package dev.dong4j.zeka.stack.idea.plugin.terminal.settings;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.StringSelection;

import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 提示模板版本通知器
 * <p>实现了 {@code ProjectActivity} 接口, 负责管理并在提示模板版本更新时通知用户.
 * <p>当插件检测到新的提示模板版本时, 该组件会评估用户的当前配置状态. 如果用户正在使用过时的自定义模板,
 * 它将显示一个通知, 提供便捷的操作以帮助用户更新配置或备份当前设置.
 * <p>主要功能包括:
 * <ul>
 * <li>检测过时的提示模板版本.</li>
 * <li>生成包含操作按钮 (复制提示, 打开设置) 的通知.</li>
 * <li>更新通知版本状态以防止重复提示.</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class PromptTemplateVersionNotifier implements ProjectActivity {

    /**
     * 复制系统提示词和终端模板到剪贴板
     * <p> 将系统提示词和终端模板的内容拼接成一个字符串, 并设置到系统的剪贴板中, 以便用户可以快速粘贴
     *
     * @param state 包含系统提示词和终端模板的设置状态对象
     */
    private static void copyPrompts(@NotNull SettingsState state) {
        String content = "systemPrompt:\n" + state.systemPrompt + "\n\nterminalTemplate:\n" + state.terminalTemplate;
        CopyPasteManager.getInstance().setContents(new StringSelection(content));
    }

    /**
     * 执行提示模板版本通知检查
     * <p> 检查当前提示模板版本, 如果需要更新则显示通知提示用户.
     * 该方法会检查版本号和通知次数, 避免重复提示用户
     *
     * @param project      当前项目实例, 用于显示通知和设置对话框
     * @param continuation 协程继续对象, 用于支持挂起函数
     * @return 执行结果, 通常返回 {@code Unit.INSTANCE}, 在特定条件下可能返回 null
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        SettingsState state = SettingsState.getInstance();
        int currentVersion = SettingsState.PROMPT_TEMPLATE_VERSION;
        int storedVersion = PromptTemplateVersionStore.getPromptTemplateVersion();
        int noticeVersion = PromptTemplateVersionStore.getPromptTemplateNoticeVersion();

        if (storedVersion >= currentVersion) {
            return Unit.INSTANCE;
        }
        if (noticeVersion >= currentVersion) {
            return Unit.INSTANCE;
        }
        if (state.isUsingDefaultPrompts()) {
            state.promptTemplateVersion = currentVersion;
            state.promptTemplateNoticeVersion = currentVersion;
            PromptTemplateVersionStore.setPromptTemplateVersion(currentVersion);
            PromptTemplateVersionStore.setPromptTemplateNoticeVersion(currentVersion);
            return Unit.INSTANCE;
        }

        String message = TerminalBundle.message("notification.prompt.update");
        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("IntelliAI Terminal Notifications")
            .createNotification(message, NotificationType.INFORMATION);

        notification.addAction(NotificationAction.createSimple(
            TerminalBundle.message("notification.prompt.copy"),
            () -> copyPrompts(state)
                                                              ));

        notification.addAction(NotificationAction.createSimple(
            TerminalBundle.message("notification.prompt.open.settings"),
            () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, TerminalSettingsConfigurable.class)
                                                              ));

        notification.notify(project);
        state.promptTemplateNoticeVersion = currentVersion;
        PromptTemplateVersionStore.setPromptTemplateNoticeVersion(currentVersion);

        return Unit.INSTANCE;
    }
}
