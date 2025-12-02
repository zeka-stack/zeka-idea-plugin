package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

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

/**
 * 通知工具类
 * <p>
 * 提供统一的通知功能, 用于在项目中展示不同级别的通知信息。
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NotificationUtil {
    public static final String NOTIFICATION_GROUP_ID = "IntelliAI Nacos Notifications";

    @NotNull
    private static NotificationGroup getNotificationGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
    }

    public static void notifyInfo(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.INFORMATION);
    }

    public static void notifyWarning(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.WARNING);
    }

    public static void notifyError(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.ERROR);
    }

    public static void showInfo(@Nullable Project project, @NotNull String message) {
        notify(project, NacosBundle.message("notification.title"), message, NotificationType.INFORMATION);
    }

    public static void showWarning(@Nullable Project project, @NotNull String message) {
        notify(project, NacosBundle.message("notification.title"), message, NotificationType.WARNING);
    }

    public static void showError(@Nullable Project project, @NotNull String message) {
        notify(project, NacosBundle.message("notification.title"), message, NotificationType.ERROR);
    }

    private static void notify(@Nullable Project project, @NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notification notification = getNotificationGroup().createNotification(title, content, type);
        notification.notify(project);
    }

    /**
     * 向通知中添加可配置的面板操作项
     * <p>
     * 为指定的通知添加一个操作项, 点击该操作项将打开 Nacos 设置配置面板, 并使通知消失
     *
     * @param notification 要添加操作项的通知对象
     * @param project      项目对象, 用于关联配置面板和项目
     */
    public static void addOpenConfigurablePanelAction(Notification notification, Project project) {
        notification.addAction(new NotificationAction(NacosBundle.message("notification.error.message.config")) {
            /**
             * 处理动作事件, 用于显示 Nacos 设置配置界面并关闭通知
             * <p>
             * 该方法在接收到动作事件时, 创建 Nacos 设置配置界面并打开编辑窗口, 随后关闭传入的通知
             *
             * @param e            动作事件对象, 包含触发动作的相关信息
             * @param notification 通知对象, 用于在操作完成后关闭通知
             */
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                // 打开设置面板
                ShowSettingsUtil.getInstance().editConfigurable(null, "IntelliAI Nacos");
                notification.expire();
            }
        });
        Notifications.Bus.notify(notification, project);
    }
}

