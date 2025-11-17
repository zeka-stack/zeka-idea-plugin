package dev.dong4j.zeka.stack.idea.plugin.workflow.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 通知工具类
 *
 * @author dong4j
 * @version 1.0.0
 */
public class NotificationUtil {
    /**
     * 通知分组的 ID
     */
    public static final String NOTIFICATION_GROUP_ID = "IntelliAI Tracer Notifications";

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
        notify(project, "IntelliAI Tracer", message, NotificationType.INFORMATION);
    }

    /**
     * 显示警告通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Tracer", message, NotificationType.WARNING);
    }

    /**
     * 显示错误通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Tracer", message, NotificationType.ERROR);
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
}

