package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.git.CommitJavaDocGenerator;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 提交工具栏中的 JavaDoc 生成操作
 *
 * <p>在 Git 提交工具栏中添加一个按钮，允许用户在提交代码前
 * 为缺少 JavaDoc 的代码元素生成文档注释。
 *
 * <p>功能特性：
 * <ul>
 *   <li>仅在提交面板打开时显示</li>
 *   <li>仅在存在 Java 文件变更时可用</li>
 *   <li>自动检测缺少 JavaDoc 的元素</li>
 *   <li>批量生成文档</li>
 *   <li>非覆盖模式（只为没有 JavaDoc 的元素生成）</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.4.0
 * @since 1.4.0
 */
@Slf4j
public class GenerateJavaDocForCommitAction extends AnAction {

    /**
     * 更新动作状态
     *
     * <p>检查是否有 Java 文件变更，如果有则启用按钮，否则禁用。
     * 在后台线程中执行，需要使用 read-action 访问 VCS 数据。
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
        e.getPresentation().setText(JavaDocBundle.message("commit.action.text"));
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
     * <p>在后台线程中执行更新操作，避免阻塞 UI。
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
     * <p>当用户点击按钮时，检测提交的 Java 文件中缺少 JavaDoc 的元素，
     * 并批量生成文档。
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        log.info("Git 提交页面：开始检测缺少 JavaDoc 的代码");

        // 获取提交的文件变更
        Collection<Change> changes = getCommittedChanges(project);
        if (changes.isEmpty()) {
            log.warn("Git 提交页面：没有找到文件变更");
            return;
        }

        // 过滤 Java 文件
        List<VirtualFile> javaFiles = filterJavaFiles(changes);
        if (javaFiles.isEmpty()) {
            log.warn("Git 提交页面：没有找到 Java 文件");
            return;
        }

        log.info("Git 提交页面：找到 {} 个 Java 文件", javaFiles.size());

        // 使用生成器检测和生成文档
        CommitJavaDocGenerator generator = new CommitJavaDocGenerator(project);
        generator.generateForChanges(changes, javaFiles);
    }

    /**
     * 检查是否有 Java 文件变更
     *
     * @param project 项目对象
     * @return 如果有 Java 文件变更返回 true
     */
    private boolean hasJavaFileChanges(@NotNull Project project) {
        Collection<Change> changes = getCommittedChanges(project);
        return !filterJavaFiles(changes).isEmpty();
    }

    /**
     * 获取提交的文件变更
     *
     * @param project 项目对象
     * @return 文件变更列表
     */
    @NotNull
    private Collection<Change> getCommittedChanges(@NotNull Project project) {
        ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        return changeListManager.getDefaultChangeList().getChanges();
    }

    /**
     * 过滤 Java 文件
     *
     * @param changes 文件变更列表
     * @return Java 文件列表
     */
    @NotNull
    private List<VirtualFile> filterJavaFiles(@NotNull Collection<Change> changes) {
        return changes.stream()
            .map(Change::getVirtualFile)
            .filter(file -> file != null && "java".equalsIgnoreCase(file.getExtension()))
            .collect(Collectors.toList());
    }
}

