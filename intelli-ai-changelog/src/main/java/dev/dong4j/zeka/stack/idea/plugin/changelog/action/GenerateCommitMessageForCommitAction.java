package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.CommitMessageGenerator;
import dev.dong4j.zeka.stack.idea.plugin.changelog.icons.ChangelogIcons;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 提交工具栏中的提交记录生成操作
 * <p>
 * 在 Git 提交工具栏中添加一个按钮，允许用户根据代码变更（diff）生成提交记录。
 *
 * <p>功能特性：
 * <ul>
 *   <li>仅在提交面板打开时显示</li>
 *   <li>仅在存在代码变更时可用</li>
 *   <li>基于代码实际改动生成提交记录</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class GenerateCommitMessageForCommitAction extends AnAction {

    /**
     * 更新动作状态
     *
     * <p>检查是否有代码变更，如果有则启用按钮，否则禁用。
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
        e.getPresentation().setText(ChangelogBundle.message("commit.action.text"));
        e.getPresentation().setIcon(ChangelogIcons.COMMIT_MESSAGE_16);

        // 检查是否有代码变更（需要在 read-action 中访问 VCS 数据）
        boolean hasChanges = ApplicationManager.getApplication().runReadAction(
            (Computable<Boolean>) () -> hasChanges(project)
                                                                              );
        e.getPresentation().setEnabled(hasChanges);
        e.getPresentation().setVisible(hasChanges);
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
     * <p>当用户点击按钮时，根据代码变更生成提交记录。
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        log.info("Git 提交页面：开始生成提交记录");

        // 获取提交的文件变更
        Collection<Change> changes = getCommittedChanges(project);
        if (changes.isEmpty()) {
            log.warn("Git 提交页面：没有找到文件变更");
            return;
        }

        log.info("Git 提交页面：找到 {} 个文件变更", changes.size());

        // 使用生成器生成提交记录
        CommitMessageGenerator generator = new CommitMessageGenerator(project);
        generator.generateForChanges(changes);
    }

    /**
     * 检查是否有代码变更
     *
     * @param project 项目对象
     * @return 如果有代码变更返回 true
     */
    private boolean hasChanges(@NotNull Project project) {
        Collection<Change> changes = getCommittedChanges(project);
        return !changes.isEmpty();
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
}

