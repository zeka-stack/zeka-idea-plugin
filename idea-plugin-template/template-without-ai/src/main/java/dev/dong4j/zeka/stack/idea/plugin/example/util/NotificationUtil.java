package dev.dong4j.zeka.stack.idea.plugin.example.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
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
    public static final String NOTIFICATION_GROUP_ID = "Example Plugin Notifications";

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
        notify(project, ExampleBundle.message("notification.title"), message, NotificationType.INFORMATION);
    }

    public static void showWarning(@Nullable Project project, @NotNull String message) {
        notify(project, ExampleBundle.message("notification.title"), message, NotificationType.WARNING);
    }

    public static void showError(@Nullable Project project, @NotNull String message) {
        notify(project, ExampleBundle.message("notification.title"), message, NotificationType.ERROR);
    }

    private static void notify(@Nullable Project project, @NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notification notification = getNotificationGroup().createNotification(title, content, type);
        notification.notify(project);
    }
}

