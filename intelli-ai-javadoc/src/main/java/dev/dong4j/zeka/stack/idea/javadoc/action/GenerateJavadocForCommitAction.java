package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.git.CommitJavadocGenerator;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 提交页面 Javadoc 生成动作类
 * <p>
 * 该类继承自 AnAction, 用于在 Git 提交页面检测并生成缺失的 Javadoc 注释.
 * 提供了在提交代码时自动检测 Java 文件并为其生成 Javadoc 的功能,
 * 帮助开发者在提交代码前完善文档注释.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavadocForCommitAction extends AnAction {

    /**
     * 更新动作状态
     *
     * <p> 检查是否有 Java 文件变更, 如果有则启用按钮, 否则禁用.
     * 在后台线程中执行, 需要使用 read-action 访问 VCS 数据.
     *
     * @param e 动作事件
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
            return;
        }

        // 设置按钮文本和图标
        e.getPresentation().setText(JavadocBundle.message("commit.action.text"));
        e.getPresentation().setIcon(AIJicons.AIJ_16);

        // 检查是否有 Java 文件变更（需要在 read-action 中访问 VCS 数据）
        boolean hasJavaFiles = ApplicationManager.getApplication().runReadAction(
            (Computable<Boolean>) () -> hasJavaFileChanges(project)
                                                                                );
        e.getPresentation().setEnabled(hasJavaFiles);
        e.getPresentation().setVisible(hasJavaFiles);
    }

    /**
     * 获取更新线程
     *
     * <p> 在后台线程中执行更新操作, 避免阻塞 UI.
     *
     * @return ActionUpdateThread.BGT 后台线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 执行动作
     *
     * <p> 当用户点击按钮时, 检测提交的 Java 文件中缺少 Javadoc 的元素,
     * 并批量生成文档注释.
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 检查 AI Provider 配置
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (!AIProviderUtils.hasAIProvider(project, config, JavadocBundle.message("settings.display.name"), JavadocBundle.message(
            "settings.ai.provider.selection"))) {
            return;
        }

        log.debug("Git 提交页面：开始检测缺少 Javadoc 的代码");

        // 在 ReadAction 中获取提交的文件变更和过滤 Java 文件
        Collection<Change> changes = ApplicationManager.getApplication().runReadAction(
            (Computable<Collection<Change>>) () -> getCommittedChanges(project)
                                                                                      );
        if (changes.isEmpty()) {
            log.debug("Git 提交页面：没有找到文件变更");
            return;
        }

        // 在 ReadAction 中过滤 Java 文件
        List<VirtualFile> javaFiles = filterJavaFiles(project, changes);
        if (javaFiles.isEmpty()) {
            log.debug("Git 提交页面：没有找到 Java 文件");
            return;
        }

        log.debug("Git 提交页面：找到 {} 个 Java 文件", javaFiles.size());

        // 使用生成器检测和生成文档
        CommitJavadocGenerator generator = new CommitJavadocGenerator(project);
        generator.generateForChanges(changes, javaFiles);
    }

    /**
     * 检查是否有 Java 文件变更
     * <p> 获取项目的提交变更列表, 并从中筛选出 Java 文件变更. 如果存在 Java 文件变更, 则返回 true; 否则返回 false.
     *
     * @param project 项目对象
     * @return 如果有 Java 文件变更返回 true, 否则返回 false
     */
    private boolean hasJavaFileChanges(@NotNull Project project) {
        Collection<Change> changes = getCommittedChanges(project);
        return !filterJavaFiles(project, changes).isEmpty();
    }

    /**
     * 获取提交的文件变更
     * <p>
     * 获取所有类型的文件变更，包括：
     * <ul>
     *   <li>已暂存的变更（staged changes）</li>
     *   <li>未暂存的变更（unstaged changes）</li>
     *   <li>未版本控制的文件（unversioned files）</li>
     * </ul>
     *
     * @param project 项目对象
     * @return 文件变更列表，包括所有类型的变更
     */
    @NotNull
    private Collection<Change> getCommittedChanges(@NotNull Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);

        // 获取所有已跟踪的变更（包括已暂存和未暂存的）
        List<Change> allChanges = new ArrayList<>(changeListManager.getAllChanges());

        // 获取未版本控制的文件，并转换为 Change 对象
        List<FilePath> unversionedFiles = changeListManager.getUnversionedFilesPaths();
        for (FilePath filePath : unversionedFiles) {
            // 为未版本控制的文件创建 Change 对象（只有 afterRevision，没有 beforeRevision）
            ContentRevision revision = new CurrentContentRevision(filePath);
            allChanges.add(new Change(null, revision));
        }

        return allChanges;
    }

    /**
     * 过滤 Java 文件
     * <p>
     * 从文件变更列表中筛选出扩展名为 ".java" 的虚拟文件, 并返回这些文件的列表.
     * 同时会检查文件是否在项目范围内，排除非项目文件和 jar 中的文件.
     *
     * @param project 项目对象
     * @param changes 文件变更列表
     * @return 包含所有符合条件的 Java 文件的虚拟文件列表
     */
    @NotNull
    private List<VirtualFile> filterJavaFiles(@NotNull Project project,
                                              @NotNull Collection<Change> changes) {
        if (project.isDisposed()) {
            return new ArrayList<>();
        }

        return ApplicationManager.getApplication().runReadAction(
            (Computable<List<VirtualFile>>) () -> {
                ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
                return changes.stream()
                    .map(Change::getVirtualFile)
                    .filter(file -> file != null
                                    && PluginContents.JAVA.equalsIgnoreCase(file.getExtension())
                                    && isFileInProject(project, file, fileIndex))
                    .collect(Collectors.toList());
            }
                                                                );
    }

    /**
     * 检查文件是否在项目范围内
     * <p>
     * 该方法检查以下条件：
     * <ol>
     *   <li>文件是否在 JarFileSystem 中（jar 中的源码应该排除）</li>
     *   <li>文件是否在本地文件系统中（不是 jar 中的文件）</li>
     *   <li>文件是否在项目的源码根目录或资源根目录中</li>
     * </ol>
     * <p>
     * <b>重要：</b>该方法必须在 ReadAction 中调用，因为 {@link ProjectFileIndex#isInProject(VirtualFile)}
     * 需要访问项目文件索引，必须在 ReadAction 中执行。
     *
     * @param project   项目对象
     * @param file      虚拟文件
     * @param fileIndex 项目文件索引（已在 ReadAction 中获取）
     * @return 如果文件在项目内且不是 jar 中的源码，返回 true；否则返回 false
     */
    private boolean isFileInProject(@NotNull Project project,
                                    @NotNull VirtualFile file,
                                    @NotNull ProjectFileIndex fileIndex) {
        // 检查文件是否在 jar 中（jar 中的源码不应该处理）
        if (file.getFileSystem() instanceof JarFileSystem) {
            return false;
        }

        // 检查文件是否在本地文件系统中
        if (!(file.getFileSystem() instanceof LocalFileSystem)) {
            return false;
        }

        // 检查文件是否在项目的源码根目录或资源根目录中
        return fileIndex.isInProject(file);
    }
}
