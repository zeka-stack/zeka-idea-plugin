package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogResultDialog;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import lombok.extern.slf4j.Slf4j;

/**
 * 提交记录生成器
 * <p>
 * 用于在 Git 提交时根据代码变更（diff）生成提交记录。
 * 参考 CommitJavaDocGenerator 的实现方式。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class CommitMessageGenerator {
    /** 项目对象 */
    private final Project project;

    /**
     * 初始化 CommitMessageGenerator 实例
     *
     * @param project 项目对象
     */
    public CommitMessageGenerator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理代码变更，生成提交记录
     * <p>
     * 该方法用于处理提交的代码变更，根据代码的实际改动生成提交记录。
     *
     * @param changes 变更集合
     */
    public void generateForChanges(@NotNull Collection<Change> changes) {
        if (changes.isEmpty()) {
            log.warn("Git 提交页面：没有代码变更需要处理");
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.changes"));
            return;
        }

        // 在后台任务中执行生成
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, ChangelogBundle.message("commit.generating.progress"), true) {
                /**
                 * 执行提交信息生成任务
                 * <p>
                 * 设置进度指示器为不确定状态, 并显示分析更改中的提示信息. 调用 ChangelogService 生成提交信息, 并在 UI 线程中显示结果或错误信息.
                 *
                 * @param indicator 进度指示器, 用于显示任务进度和状态信息
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(ChangelogBundle.message("commit.analyzing.changes"));

                    try {
                        ChangelogService service = ChangelogService.getInstance(project);
                        String commitMessage = service.generateCommitMessageFromDiff(changes);

                        // 在 EDT 中显示结果
                        ApplicationManager.getApplication().invokeLater(() -> {
                            showCommitMessageDialog(commitMessage);
                        });

                        log.info("Git 提交页面：提交记录生成成功");
                    } catch (AIServiceException e) {
                        log.error("Git 提交页面：生成提交记录失败", e);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NotificationUtil.showError(project,
                                                       ChangelogBundle.message("commit.generation.failed", e.getMessage()));
                        });
                    } catch (Exception e) {
                        log.error("Git 提交页面：生成提交记录时发生异常", e);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NotificationUtil.showError(project,
                                                       ChangelogBundle.message("commit.generation.error", e.getMessage()));
                        });
                    }
                }
            }
                                         );
    }

    /**
     * 显示提交记录对话框
     *
     * @param commitMessage 生成的提交记录
     */
    private void showCommitMessageDialog(@NotNull String commitMessage) {
        // 使用 ChangelogResultDialog 显示提交记录对话框
        ChangelogResultDialog dialog = new ChangelogResultDialog(
            project,
            commitMessage,
            ChangelogBundle.message("commit.message.title")
        );

        if (dialog.showAndGet()) {
            // 用户点击了 OK 按钮
            String result = dialog.getText();
            if (!result.trim().isEmpty()) {
                // 用户确认后，可以在这里将提交记录设置到提交消息框中
                // 注意：这需要访问提交面板的 API，可能需要进一步实现
                log.info("用户确认了提交记录：{}", result);
                NotificationUtil.showInfo(project, ChangelogBundle.message("commit.message.copied"));
            }
        }
    }
}

