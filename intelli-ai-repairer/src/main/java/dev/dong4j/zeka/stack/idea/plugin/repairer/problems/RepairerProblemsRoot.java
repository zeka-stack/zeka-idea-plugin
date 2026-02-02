package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.toolWindow.FileNode;
import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.Root;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.tree.TreePath;

import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCacheListener;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Root node for the IntelliAI Repairer Problems tab.
 */
public final class RepairerProblemsRoot extends Root implements ViolationCacheListener {

    /**
     * 当前项目实例
     * <p> 用于访问与项目相关的各种服务和组件
     */
    private final Project project;
    /**
     * 违规问题缓存实例
     * <p> 用于存储和管理当前项目中的所有违规问题数据
     */
    private final ViolationCache cache;
    /** 提供问题数据的提供器 */
    private final RepairerProblemsProvider provider;
    /** 用于缓存 ProblemsIndex 的版本号, 当代码违规更新时递增 */
    private final AtomicLong version = new AtomicLong();
    /** 缓存的问题索引, 用于优化性能, 支持并发访问 */
    private volatile ProblemsIndex cachedIndex;

    /**
     * 构造 RepairerProblemsRoot 实例
     * <p> 初始化问题根节点, 关联指定的项目和视图面板, 并注册违规缓存监听器
     *
     * @param panel   问题视图面板, 不能为 null
     * @param project 当前项目, 不能为 null
     */
    public RepairerProblemsRoot(@NotNull ProblemsViewPanel panel, @NotNull Project project) {
        super(panel);
        this.project = project;
        this.cache = ViolationCache.getInstance(project);
        this.provider = new RepairerProblemsProvider(project);
        this.cache.addListener(this);
    }

    /**
     * 获取当前节点的子节点集合
     * <p> 返回表示模块节点的集合, 这些节点由 {@link #getProblemsIndex()} 方法生成.
     *
     * @return 当前节点的子节点集合, 包含所有模块节点
     */
    @Override
    public @NotNull Collection<Node> getChildren() {
        return getProblemsIndex().rootNodes;
    }

    /**
     * 获取指定文件节点的所有子节点
     * <p> 根据给定的文件节点, 查找其关联的问题列表, 并返回这些问题对应的节点集合
     *
     * @param fileNode 文件节点
     * @return 子节点集合
     */
    @Override
    public @NotNull Collection<Node> getChildren(@NotNull FileNode fileNode) {
        List<Problem> problems = getProblemsIndex().problemsByFile.getOrDefault(fileNode.getVirtualFile(), List.of());
        return super.getNodesForProblems(fileNode, problems);
    }

    /**
     * 获取指定文件的所有问题节点
     * <p> 根据给定的虚拟文件, 查找其关联的问题列表, 并返回这些问题对应的节点集合
     *
     * @param file 虚拟文件对象
     * @return 问题节点集合
     */
    @Override
    public @NotNull Collection<Node> getChildren(@NotNull VirtualFile file) {
        List<Problem> problems = getProblemsIndex().problemsByFile.getOrDefault(file, List.of());
        return super.getNodesForProblems(new FileNode(this, file), problems);
    }

    /**
     * 获取其他问题
     * <p> 返回一个空列表, 表示当前节点没有其他问题.
     *
     * @return 一个不可变的空列表, 表示没有其他问题
     */
    @Override
    public @NotNull Collection<Problem> getOtherProblems() {
        return List.of();
    }

    /**
     * 获取指定文件的代码问题列表
     * <p> 返回指定虚拟文件中的所有代码问题, 如果文件没有问题则返回空列表
     *
     * @param file 虚拟文件, 不能为 null
     * @return 代码问题集合, 如果文件没有问题则返回空列表, 不会返回 null
     */
    @Override
    public @NotNull Collection<Problem> getFileProblems(@NotNull VirtualFile file) {
        return getProblemsIndex().problemsByFile.getOrDefault(file, List.of());
    }

    /**
     * 获取指定文件的问题数量
     *
     * @param file 虚拟文件对象, 不能为空
     * @return 该文件中包含的问题数量
     */
    @Override
    public int getFileProblemCount(@NotNull VirtualFile file) {
        return getFileProblems(file).size();
    }

    /**
     * 获取包含问题的所有文件集合
     *
     * @return 包含问题的虚拟文件集合, 不可为 null
     */
    @Override
    public @NotNull Collection<VirtualFile> getProblemFiles() {
        return getProblemsIndex().problemsByFile.keySet();
    }

    /**
     * 获取当前所有问题的总数.
     * <p> 通过访问 {@link #getProblemsIndex()} 获取缓存索引,
     * 然后统计 {@code problemsByFile} 中每个文件的问题数量之和.
     *
     * @return 所有文件中问题的总数
     */
    @Override
    public int getProblemCount() {
        return getProblemsIndex().problemsByFile.values().stream()
            .mapToInt(Collection::size)
            .sum();
    }

    /**
     * 当违规信息更新时触发的方法
     * <p> 在检测到新的或更改的代码违规时, 此方法会增加版本号, 并在稍后调用 {@code structureChanged} 方法以通知视图更新.
     *
     * @param violations 包含最新违规信息的列表
     */
    @Override
    public void violationsUpdated(@NotNull List<CodeViolation> violations) {
        version.incrementAndGet();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            structureChanged(new TreePath(this));
        });
    }

    /**
     * 释放资源并清理监听器
     * <p> 移除当前实例作为缓存的监听器, 然后调用父类的 dispose 方法完成资源释放
     *
     */
    @Override
    public void dispose() {
        cache.removeListener(this);
        super.dispose();
    }

    /**
     * 获取当前问题索引
     * <p> 检查缓存中的问题索引是否与当前版本一致, 如果一致则返回缓存索引, 否则重新构建并更新缓存.
     *
     * @return 当前的问题索引对象
     */
    private ProblemsIndex getProblemsIndex() {
        long currentVersion = version.get();
        ProblemsIndex index = cachedIndex;
        if (index != null && index.version == currentVersion) {
            return index;
        }
        ProblemsIndex rebuilt = buildIndex(currentVersion);
        cachedIndex = rebuilt;
        return rebuilt;
    }

    /**
     * 获取其他问题的数量
     * <p> 当前实现不包含其他问题, 因此始终返回 0
     *
     * @return 其他问题的数量, 固定为 0
     */
    @Override
    public int getOtherProblemCount() {
        return 0;
    }

    /**
     * 根据当前版本构建问题索引结构
     * <p> 遍历缓存中的所有代码违规项, 按文件, 模块, 包层级组织问题, 并构建可展示的树形结构索引
     * <p> 最终返回包含模块节点, 文件问题映射的不可变索引对象, 用于后续界面展示
     *
     * @param currentVersion 当前版本号, 用于缓存一致性校验
     * @return 构建完成的问题索引对象, 包含模块节点列表和文件到问题列表的映射
     */
    private ProblemsIndex buildIndex(long currentVersion) {
        Map<VirtualFile, List<Problem>> problemsByFile = new HashMap<>();

        for (CodeViolation violation : cache.getAll()) {
            if (violation.filePath == null || violation.filePath.isBlank()) {
                continue;
            }
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(violation.filePath);
            if (file == null) {
                continue;
            }
            RepairerProblem problem = new RepairerProblem(provider, violation, file);
            problemsByFile.computeIfAbsent(file, ignored -> new ArrayList<>()).add(problem);
        }

        for (List<Problem> problems : problemsByFile.values()) {
            problems.sort(Comparator.comparingInt(RepairerProblemsRoot::problemLine)
                                    .thenComparingInt(RepairerProblemsRoot::problemColumn));
        }

        List<VirtualFile> sortedFiles = new ArrayList<>(problemsByFile.keySet());
        sortedFiles.sort(Comparator.comparing(VirtualFile::getName, String::compareToIgnoreCase));

        List<Node> fileNodes = new ArrayList<>();
        for (VirtualFile file : sortedFiles) {
            fileNodes.add(new FileNode(this, file));
        }

        int problemCount = problemsByFile.values().stream().mapToInt(Collection::size).sum();
        int fileCount = problemsByFile.size();
        String summary = RepairerBundle.message("problems.root.summary", problemCount, fileCount);

        List<Node> rootNodes = new ArrayList<>();
        rootNodes.add(new RepairerSummaryNode(project, summary, fileNodes));

        return new ProblemsIndex(currentVersion, rootNodes, problemsByFile);
    }

    /**
     * 获取问题所在的行号
     * <p> 如果问题是 RepairerProblem 类型, 则返回其行号; 否则返回 0
     *
     * @param problem 问题对象
     * @return 问题所在的行号, 如果不是 RepairerProblem 类型则返回 0
     */
    private static int problemLine(@NotNull Problem problem) {
        if (problem instanceof RepairerProblem) {
            return ((RepairerProblem) problem).getLine();
        }
        return 0;
    }

    /**
     * 获取问题的列号
     * <p> 如果问题实例是 RepairerProblem 类型, 则返回其列号; 否则返回 0
     *
     * @param problem 问题对象
     * @return 列号, 如果问题不是 RepairerProblem 类型则返回 0
     */
    private static int problemColumn(@NotNull Problem problem) {
        if (problem instanceof RepairerProblem) {
            return ((RepairerProblem) problem).getColumn();
        }
        return 0;
    }

    /**
     * 问题索引数据类, 用于存储和管理问题相关的结构化数据
     * <p> 该类主要用于缓存和组织由 {@link RepairerProblemsRoot} 构建的问题信息, 包括模块节点, 文件问题映射等.
     * 它封装了问题数据的版本信息以及按模块和文件分类的问题列表, 以便于在 UI 中高效展示和更新.
     *
     * @param version        当前问题索引的版本号
     * @param rootNodes      模块节点列表
     *                       <p> 包含各个模块的相关节点信息
     * @param problemsByFile 文件与问题列表之间的映射
     *                       <p> 存储每个文件对应的检查问题列表
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.29
     * @since 1.0.0
     */
    private record ProblemsIndex(long version, List<Node> rootNodes, Map<VirtualFile, List<Problem>> problemsByFile) {
        /**
         * 构造 ProblemsIndex 实例
         * <p> 初始化问题索引, 包含版本号, 模块节点列表以及按文件分组的问题映射
         *
         * @param version        索引版本号
         * @param moduleNodes    模块节点列表, 构造后将被封装为不可修改列表
         * @param problemsByFile 按虚拟文件分组的问题映射,Key 为文件,Value 为该文件的问题列表
         */
        private ProblemsIndex(long version,
                              @NotNull List<Node> rootNodes,
                              @NotNull Map<VirtualFile, List<Problem>> problemsByFile) {
            this.version = version;
            this.rootNodes = Collections.unmodifiableList(rootNodes);
            this.problemsByFile = problemsByFile;
        }
    }
}
