package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;

/**
 * 通知工具类
 *
 * @author dong4j
 * @version 1.0.0
 */
public class NotificationUtil {
    public static final String NOTIFICATION_TITLE = EngineContents.PLUGIN_NAME;
    public static final String NOTIFICATION_GROUP_ID = NOTIFICATION_TITLE + " Notifications";

    /**
     * 获取指定 ID 的通知组
     *
     * @return 通知组实例
     */
    @NotNull
    private static NotificationGroup getNotificationGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
    }

    /**
     * 显示信息通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        notify(project, NOTIFICATION_TITLE, message, NotificationType.INFORMATION);
    }

    /**
     * 显示警告通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        notify(project, NOTIFICATION_TITLE, message, NotificationType.WARNING);
    }

    /**
     * 显示错误通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        notify(project, NOTIFICATION_TITLE, message, NotificationType.ERROR);
    }

    /**
     * 发送通知
     *
     * @param project 项目对象，可为空
     * @param title   通知标题
     * @param content 通知内容
     * @param type    通知类型
     */
    private static void notify(@Nullable Project project, @NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notification notification = getNotificationGroup().createNotification(title, content, type);
        notification.notify(project);
    }

    /**
     * 为通知添加一个可配置面板的操作
     * <p>
     * 创建一个通知操作, 当用户点击时会打开指定的配置面板并使通知过期.
     *
     * @param notification 要添加操作的通知对象
     * @param project      与通知关联的项目对象
     * @throws IllegalArgumentException 如果参数为 null 时可能抛出异常
     * @since 1.0
     */
    public static void addOpenConfigurablePanelAction(Notification notification, Project project, String configurableId) {
        notification.addAction(new NotificationAction(AICommonBundle.message("settings.ai.provider.open.engine.settings")) {
            /**
             * 处理动作事件, 用于显示 JavaDoc 设置配置界面并关闭通知
             * <p>
             * 该方法在接收到动作事件时, 创建 JavaDoc 设置配置界面并打开编辑窗口, 随后关闭传入的通知
             *
             * @param e            动作事件对象, 包含触发动作的相关信息
             * @param notification 通知对象, 用于在操作完成后关闭通知
             */
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                // 打开设置面板
                ShowSettingsUtil.getInstance().editConfigurable(null, configurableId);
                notification.expire();
            }
        });
        Notifications.Bus.notify(notification, project);
    }
}

