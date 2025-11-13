package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogResultDialog;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;

/**
 * Git Log 工具窗口中生成 Changelog 的 Action
 * <p>
 * 在 Git Log 工具窗口中，用户可以选择多条提交记录，然后右键选择此 Action 生成 Changelog
 */
public class GenerateChangelogForGitLogAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 获取选中的提交记录
        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        if (log == null) {
            NotificationUtil.showError(project, "Please select commits in Git Log tool window");
            return;
        }

        // 获取选中的提交记录 hash
        List<String> selectedHashes = new ArrayList<>();
        List<VcsFullCommitDetails> selectedCommits = log.getSelectedDetails();
        if (selectedCommits.isEmpty()) {
            NotificationUtil.showError(project, "Please select at least one commit");
            return;
        }

        for (VcsFullCommitDetails commit : selectedCommits) {
            selectedHashes.add(commit.getId().asString());
        }


        // 在后台任务中生成 Changelog
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Changelog with AI", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Reading commits and generating changelog...");

                try {
                    ChangelogService service = ChangelogService.getInstance(project);
                    String changelog = service.generateChangelog(selectedHashes);

                    // 在 EDT 中显示结果对话框
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        ChangelogResultDialog dialog = new ChangelogResultDialog(project, changelog);
                        dialog.show();
                    });
                } catch (Exception ex) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project, "Failed to generate changelog: " + ex.getMessage());
                    });
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);

        // 只有在 Git Log 工具窗口中有选中提交时才启用
        boolean enabled = project != null && log != null;
        if (enabled) {
            List<VcsFullCommitDetails> selectedCommits = log.getSelectedDetails();
            enabled = !selectedCommits.isEmpty();
        }

        e.getPresentation().setEnabled(enabled);
    }
}

