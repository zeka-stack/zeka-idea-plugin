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
 * Git Log 工具窗口中生成工作日报的 Action
 */
public class GenerateDailyReportForGitLogAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        if (log == null) {
            NotificationUtil.showError(project, "请在 Git Log 工具窗口中选择提交记录");
            return;
        }

        List<String> selectedHashes = new ArrayList<>();
        List<VcsFullCommitDetails> selectedCommits = log.getSelectedDetails();
        if (selectedCommits.isEmpty()) {
            NotificationUtil.showError(project, "请至少选择一条提交记录");
            return;
        }

        for (VcsFullCommitDetails commit : selectedCommits) {
            selectedHashes.add(commit.getId().asString());
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "使用 AI 生成工作日报", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("读取提交记录并生成工作日报...");

                try {
                    ChangelogService service = ChangelogService.getInstance(project);
                    String report = service.generateDailyReport(selectedHashes);

                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        ChangelogResultDialog dialog = new ChangelogResultDialog(project, report);
                        dialog.show();
                    });
                } catch (Exception ex) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project, "生成工作日报失败: " + ex.getMessage());
                    });
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);

        boolean enabled = project != null && log != null;
        if (enabled) {
            List<VcsFullCommitDetails> selectedCommits = log.getSelectedDetails();
            enabled = !selectedCommits.isEmpty();
        }

        e.getPresentation().setEnabled(enabled);
    }
}

