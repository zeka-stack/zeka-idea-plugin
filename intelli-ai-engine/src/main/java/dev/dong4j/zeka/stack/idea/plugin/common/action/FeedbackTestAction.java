package dev.dong4j.zeka.stack.idea.plugin.common.action;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.WindowManager;
import dev.dong4j.zeka.stack.idea.plugin.common.diagnostic.EngineFeedbackSubmitter;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.Notifications;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 反馈提交流程测试 Action
 * <p> 在 IDE 中通过手动输入信息, 直接触发 EngineFeedbackSubmitter 的提交流程.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.28
 * @since 1.0.0
 */
public class FeedbackTestAction extends DumbAwareAction {
    private static final String REGISTRY_KEY = "intelliai.engine.feedback.test";

    /**
     * 构造函数, 初始化反馈测试 Action
     */
    public FeedbackTestAction() {
        super(AICommonBundle.message("action.feedback.test.text"),
              AICommonBundle.message("action.feedback.test.description"),
              com.intellij.icons.AllIcons.General.Balloon);
    }

    /**
     * 获取动作更新线程
     *
     * @return 动作更新线程类型 BGT (Background Thread)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        boolean visible = Registry.is(REGISTRY_KEY);
        event.getPresentation().setEnabledAndVisible(visible);
    }

    /**
     * 执行反馈提交测试动作
     *
     * @param event 动作事件对象, 包含触发动作的相关信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        String title = Messages.showInputDialog(
            project,
            AICommonBundle.message("action.feedback.test.title.prompt"),
            AICommonBundle.message("action.feedback.test.title"),
            Messages.getQuestionIcon(),
            "Feedback Test",
            null
        );
        if (title == null) {
            return;
        }

        String additionalInfo = Messages.showMultilineInputDialog(
            project,
            AICommonBundle.message("action.feedback.test.additional.prompt"),
            AICommonBundle.message("action.feedback.test.additional.title"),
            "Additional info for feedback test.",
            Messages.getInformationIcon(),
            null
        );
        if (additionalInfo == null) {
            return;
        }

        String throwableMessage = Messages.showInputDialog(
            project,
            AICommonBundle.message("action.feedback.test.throwable.prompt"),
            AICommonBundle.message("action.feedback.test.throwable.title"),
            Messages.getWarningIcon(),
            "Test exception for feedback submission.",
            null
        );
        if (throwableMessage == null) {
            return;
        }

        Component parent = project != null ? WindowManager.getInstance().getFrame(project) : null;
        if (parent == null) {
            parent = new JPanel();
        }

        IdeaLoggingEvent loggingEvent = new IdeaLoggingEvent(title,
                                                            new RuntimeException(throwableMessage));
        EngineFeedbackSubmitter submitter = new EngineFeedbackSubmitter();
        AtomicReference<SubmittedReportInfo> resultRef = new AtomicReference<>();

        Component finalParent = parent;
        new Task.Backgroundable(project, AICommonBundle.message("action.feedback.test.progress.title")) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText(AICommonBundle.message("action.feedback.test.progress.text"));
                boolean success = submitter.submitFeedback(new IdeaLoggingEvent[] {loggingEvent},
                                                           additionalInfo,
                    finalParent,
                                                           resultRef::set);
                SubmittedReportInfo reportInfo = resultRef.get();
                String status = reportInfo != null ? reportInfo.getStatus().toString() : "UNKNOWN";
                String linkText = reportInfo != null ? reportInfo.getLinkText() : "";
                String url = reportInfo != null ? reportInfo.getURL() : "";
                String content = AICommonBundle.message("action.feedback.test.result.content",
                                                        status,
                                                        StringUtil.notNullize(linkText),
                                                        StringUtil.notNullize(url));
                NotificationType type = success ? NotificationType.INFORMATION : NotificationType.ERROR;
                Notifications.showLogNotification(
                    AICommonBundle.message("action.feedback.test.result.title"),
                    content,
                    type,
                    project
                );
            }
        }.queue();
    }
}
