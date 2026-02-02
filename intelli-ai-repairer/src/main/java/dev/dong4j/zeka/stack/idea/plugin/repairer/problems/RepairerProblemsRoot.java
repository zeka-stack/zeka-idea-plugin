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
import java.util.TreeMap;
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
        List<Problem> problems = null;
        RepairerToolNode toolNode = fileNode.getParent(RepairerToolNode.class);
        if (toolNode != null) {
            Map<VirtualFile, List<Problem>> toolFiles =
                getProblemsIndex().problemsByTool.getOrDefault(toolNode.getKey(), Map.of());
            problems = toolFiles.get(fileNode.getVirtualFile());
        }
        if (problems == null) {
            problems = getProblemsIndex().problemsByFile.getOrDefault(fileNode.getVirtualFile(), List.of());
        }
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
        Map<String, Map<VirtualFile, List<Problem>>> problemsByTool = new TreeMap<>(String::compareToIgnoreCase);

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
            String toolName = violation.tool == null || violation.tool.isBlank() ? "Unknown" : violation.tool.trim();
            problemsByTool
                .computeIfAbsent(toolName, ignored -> new HashMap<>())
                .computeIfAbsent(file, ignored -> new ArrayList<>())
                .add(problem);
        }

        for (List<Problem> problems : problemsByFile.values()) {
            problems.sort(Comparator.comparingInt(RepairerProblemsRoot::problemLine)
                                    .thenComparingInt(RepairerProblemsRoot::problemColumn));
        }
        for (Map<VirtualFile, List<Problem>> toolFiles : problemsByTool.values()) {
            for (List<Problem> problems : toolFiles.values()) {
                problems.sort(Comparator.comparingInt(RepairerProblemsRoot::problemLine)
                                  .thenComparingInt(RepairerProblemsRoot::problemColumn));
            }
        }

        List<Node> toolNodes = new ArrayList<>();
        for (Map.Entry<String, Map<VirtualFile, List<Problem>>> toolEntry : problemsByTool.entrySet()) {
            List<Node> fileNodes = new ArrayList<>();
            List<VirtualFile> sortedFiles = new ArrayList<>(toolEntry.getValue().keySet());
            sortedFiles.sort(Comparator.comparing(VirtualFile::getName, String::compareToIgnoreCase));
            int toolProblemCount = countProblems(toolEntry.getValue());
            String toolName = formatToolName(toolEntry.getKey(), toolProblemCount);
            RepairerToolNode toolNode = new RepairerToolNode(project, toolEntry.getKey(), toolName, fileNodes);
            toolNodes.add(toolNode);

            for (VirtualFile file : sortedFiles) {
                List<Problem> problems = toolEntry.getValue().getOrDefault(file, List.of());
                Node fileNode = new RepairerFileNode(this, toolNode, file, problems);
                fileNodes.add(fileNode);

            }
        }

        int problemCount = problemsByFile.values().stream().mapToInt(Collection::size).sum();
        int fileCount = problemsByFile.size();
        int checkstyleCount = countProblems(problemsByTool.get("CHECKSTYLE"));
        int pmdCount = countProblems(problemsByTool.get("PMD"));
        String summary = RepairerBundle.message("problems.root.summary.detail",
                                                problemCount,
                                                fileCount,
                                                checkstyleCount,
                                                pmdCount);

        List<Node> rootNodes = new ArrayList<>();
        rootNodes.add(new RepairerSummaryNode(project, summary, toolNodes));

        return new ProblemsIndex(currentVersion, rootNodes, problemsByFile, problemsByTool);
    }

    /**
     * 统计指定工具分组下所有文件的问题总数
     * <p> 遍历工具分组映射中的每个文件及其对应的问题列表, 对每个文件的问题数量求和, 返回总问题数.
     * 如果输入的映射为 null 或为空, 则返回 0.
     *
     * @param toolFiles 工具分组映射,Key 为虚拟文件,Value 为该文件的问题列表集合, 不能为 null
     * @return 该工具分组下所有文件的问题总数
     */
    private static int countProblems(Map<VirtualFile, List<Problem>> toolFiles) {
        if (toolFiles == null || toolFiles.isEmpty()) {
            return 0;
        }
        return toolFiles.values().stream().mapToInt(Collection::size).sum();
    }

    /**
     * 格式化工具名称并附加问题数量
     * <p> 将工具键转换为友好的工具名称, 并在名称后附加括号中的问题数量, 用于显示在用户界面中.
     *
     * @param toolKey 工具的键, 用于标识不同的代码检查工具
     * @param count   与该工具相关的问题总数
     * @return 格式化后的工具名称, 格式为 "工具名称 (问题数量)"
     */
    private static String formatToolName(String toolKey, int count) {
        String name = friendlyToolName(toolKey);
        return name + " (" + count + ")";
    }

    /**
     * 将工具名称转换为友好的显示名称
     * <p> 根据传入的工具键名, 返回对应的友好显示名称. 若工具键为 null 或空白, 则返回 "Unknown"; 若为 "CHECKSTYLE", 则返回 "Checkstyle"; 若为 "PMD", 则返回 "PMD"; 否则返回去除首尾空格后的原始名称.
     *
     * @param toolKey 工具键名, 可能为 null 或空白字符串
     * @return 友好显示的工具名称, 如 "Checkstyle","PMD" 或 "Unknown"
     */
    private static String friendlyToolName(String toolKey) {
        if (toolKey == null || toolKey.isBlank()) {
            return "Unknown";
        }
        if ("CHECKSTYLE".equalsIgnoreCase(toolKey)) {
            return "Checkstyle";
        }
        if ("PMD".equalsIgnoreCase(toolKey)) {
            return "PMD";
        }
        return toolKey.trim();
    }

    /**
     * File node with attached problems for tool grouping.
     */
    private static final class RepairerFileNode extends Node {
        /** 用于存储与该文件节点相关的修复问题根对象 */
        private final RepairerProblemsRoot root;
        /** 委托的文件节点 */
        private final FileNode delegate;
        /**
         * 存储与文件关联的问题列表
         * <p> 这些问题用于工具分组和显示
         */
        private final List<Problem> problems;
        /** 与当前节点关联的虚拟文件对象, 用于表示文件系统中的实际文件. */
        private final VirtualFile file;

        /**
         * 构造函数, 初始化 RepairerFileNode 对象
         * <p> 根据传入的参数初始化对象属性, 并调用父类构造函数
         *
         * @param root     问题根对象
         * @param parent   父节点
         * @param file     虚拟文件对象
         * @param problems 问题列表
         */
        private RepairerFileNode(@NotNull RepairerProblemsRoot root,
                                 @NotNull Node parent,
                                 @NotNull VirtualFile file,
                                 @NotNull List<Problem> problems) {
            super(parent);
            this.root = root;
            this.file = file;
            this.delegate = new FileNode(this, file);
            this.problems = problems;
        }

        /**
         * 获取子节点集合
         * <p> 根据委托节点和问题列表, 从根节点获取对应的子节点集合
         *
         * @return 子节点集合, 类型为 {@code Collection<Node>}
         * @since 1.0
         */
        @Override
        public @NotNull Collection<Node> getChildren() {
            return root.getNodesForProblems(delegate, problems);
        }

        /**
         * 获取当前节点的叶子状态
         * <p> 根据问题列表是否为空来决定节点的叶子状态. 如果问题列表为空, 则返回 ALWAYS, 否则返回 NEVER
         *
         * @return 节点的叶子状态
         */
        @Override
        public @NotNull com.intellij.ui.tree.LeafState getLeafState() {
            return problems.isEmpty() ? com.intellij.ui.tree.LeafState.ALWAYS : com.intellij.ui.tree.LeafState.NEVER;
        }

        /**
         * 获取并返回文件节点对应文件的名称.
         *
         * <p> 此方法重写自 {@link Node#getName}, 返回存储在 {@link VirtualFile} 中的文件名.
         *
         * @return 文件名称, 不能为空
         */
        @Override
        public @NotNull String getName() {
            return file.getName();
        }

        /**
         * 更新节点的显示表示数据
         * <p> 设置节点的可显示文本为文件名, 并设置节点的图标为文件类型对应的图标
         *
         * @param presentation 项目的展示数据对象
         */
        @Override
        protected void update(@NotNull com.intellij.ide.projectView.PresentationData presentation) {
            presentation.setPresentableText(file.getName());
            presentation.setIcon(file.getFileType().getIcon());
        }

        /**
         * 更新节点的展示信息
         * <p> 根据当前项目和展示数据对象更新节点的显示状态, 调用无参数的 update 方法实现具体逻辑.
         *
         * @param project          当前项目对象
         * @param presentationData 展示数据对象, 用于设置节点的显示信息
         */
        @Override
        protected void update(@NotNull Project project, @NotNull com.intellij.ide.projectView.PresentationData presentationData) {
            update(presentationData);
        }
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
    private record ProblemsIndex(long version,
                                 List<Node> rootNodes,
                                 Map<VirtualFile, List<Problem>> problemsByFile,
                                 Map<String, Map<VirtualFile, List<Problem>>> problemsByTool) {
        /**
         * 构造 ProblemsIndex 实例
         * <p> 初始化问题索引, 包含版本号, 模块节点列表以及按文件分组的问题映射
         *
         * @param version        索引版本号
         * @param rootNodes      模块节点列表, 构造后将被封装为不可修改列表
         * @param problemsByFile 按虚拟文件分组的问题映射,Key 为文件,Value 为该文件的问题列表
         * @param problemsByTool 按工具分组的问题映射,Key 为工具名称,Value 为该工具在各文件中的问题列表映射
         */
        private ProblemsIndex(long version,
                              @NotNull List<Node> rootNodes,
                              @NotNull Map<VirtualFile, List<Problem>> problemsByFile,
                              @NotNull Map<String, Map<VirtualFile, List<Problem>>> problemsByTool) {
            this.version = version;
            this.rootNodes = Collections.unmodifiableList(rootNodes);
            this.problemsByFile = problemsByFile;
            this.problemsByTool = problemsByTool;
        }
    }
}
