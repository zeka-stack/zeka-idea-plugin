package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLog;
import com.intellij.vcs.log.VcsLogDataKeys;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogResultDialog;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import icons.ChangelogIcons;

/**
 * Git Log 相关 Action 的抽象基类
 * <p>
 * 提供统一的 update() 方法实现，减少代码重复。
 * 子类只需要提供文本、描述和图标的信息即可。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class AbstractGitLogAction extends AnAction {

    /**
     * 获取 Action 的文本资源键
     *
     * @return 文本资源键
     */
    @NotNull
    protected abstract String getTextKey();

    /**
     * 获取 Action 的描述资源键
     *
     * @return 描述资源键
     */
    @NotNull
    protected abstract String getDescriptionKey();

    /**
     * 获取 Action 的图标
     *
     * @return 图标对象
     */
    @NotNull
    protected Icon getIcon() {
        return ChangelogIcons.CHANGELOG_16;
    }

    /**
     * 获取进度标题的资源键
     *
     * @return 进度标题的资源键
     */
    @NotNull
    protected String getProgressTitleKey() {
        return "";
    }

    /**
     * 获取进度文本的资源键
     *
     * @return 进度文本的资源键
     */
    @NotNull
    protected String getProgressTextKey() {
        return "";
    }

    /**
     * 获取错误消息的资源键
     *
     * @return 错误消息的资源键
     */
    @NotNull
    protected String getErrorKey() {
        return "";
    }

    /**
     * 生成内容
     * <p>
     * 子类实现此方法来调用 ChangelogService 的相应方法生成内容。
     *
     * @param service      ChangelogService 实例
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的内容
     * @throws Exception 生成过程中可能发生的异常
     */
    @NotNull
    protected String generateContent(@NotNull ChangelogService service, @NotNull List<String> commitHashes) throws Exception {
        return "";
    }

    /**
     * 更新动作状态
     * <p>
     * 检查是否有选中的提交记录，如果有则启用按钮，否则禁用。
     * 同时设置按钮的文本、描述和图标。
     *
     * @param e 动作事件
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);

        // 设置按钮文本、描述和图标
        e.getPresentation().setText(ChangelogBundle.message(getTextKey()));
        e.getPresentation().setDescription(ChangelogBundle.message(getDescriptionKey()));
        e.getPresentation().setIcon(getIcon());

        // 只有在 Git Log 工具窗口中有选中提交时才启用
        boolean enabled = project != null && log != null;
        if (enabled) {
            List<VcsFullCommitDetails> selectedCommits = log.getSelectedDetails();
            enabled = !selectedCommits.isEmpty();
        }

        e.getPresentation().setEnabled(enabled);
    }

    /**
     * 执行动作
     * <p>
     * 模板方法，提供统一的执行流程：
     * 1. 获取选中的提交记录
     * 2. 在后台任务中生成内容
     * 3. 显示结果对话框或错误提示
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 获取选中的提交记录
        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        if (log == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.git.log"));
            return;
        }

        List<VcsFullCommitDetails> selectedCommits = log.getSelectedDetails();
        if (selectedCommits.isEmpty()) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.commits.selected"));
            return;
        }

        // 获取选中的提交记录 hash
        List<String> selectedHashes = new ArrayList<>();
        for (VcsFullCommitDetails commit : selectedCommits) {
            selectedHashes.add(commit.getId().asString());
        }

        // 在后台任务中生成内容
        String progressTitle = ChangelogBundle.message(getProgressTitleKey());
        ProgressManager.getInstance().run(new Task.Backgroundable(project, progressTitle, true) {
            /**
             * 执行变更日志生成任务.
             * <p>
             * 该方法在进度指示器上设置为不确定进度, 并显示相应文本. 随后尝试使用 {@link ChangelogService} 生成变更日志内容,
             * 并在 UI 线程中弹出 {@link ChangelogResultDialog} 显示结果. 如果生成过程中出现异常, 则在 UI 线程中通过 {@link NotificationUtil}
             * 显示错误通知.
             *
             * @param indicator 用于显示任务进度的进度指示器
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(ChangelogBundle.message(getProgressTextKey()));

                try {
                    ChangelogService service = ChangelogService.getInstance(project);
                    String content = generateContent(service, selectedHashes);

                    // 在 EDT 中显示结果对话框
                    ApplicationManager.getApplication().invokeLater(() -> {
                        ChangelogResultDialog dialog = new ChangelogResultDialog(project, content);
                        dialog.show();
                    });
                } catch (Exception ex) {
                    // 在 EDT 中显示错误提示
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project,
                                                   ChangelogBundle.message(getErrorKey(), ex.getMessage()));
                    });
                }
            }
        });
    }

    /**
     * 获取动作更新线程
     *
     * <p>在后台线程中执行更新操作，避免阻塞事件调度线程(EDT)。
     * 因为需要访问 VCS 数据（VcsLog），在后台线程中执行更安全。
     *
     * @return ActionUpdateThread.BGT 后台线程
     * @see ActionUpdateThread#BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 在后台线程中执行 update，避免阻塞 EDT
        return ActionUpdateThread.BGT;
    }
}

