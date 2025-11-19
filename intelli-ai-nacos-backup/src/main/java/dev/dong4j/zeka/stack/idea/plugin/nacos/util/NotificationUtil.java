package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

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
 * 提供统一的通知功能, 用于在项目中展示不同级别的通知信息, 如信息, 警告和错误等.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class NotificationUtil {
    /**
     * 通知分组的 ID
     */
    public static final String NOTIFICATION_GROUP_ID = "IntelliAI Nacos Notifications";

    /**
     * 获取指定 ID 的通知组
     *
     * @return 与 NOTIFICATION_GROUP_ID 关联的 NotificationGroup 实例
     */
    @NotNull
    private static NotificationGroup getNotificationGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
    }

    /**
     * 发送信息通知
     *
     * @param project 项目对象, 可为空
     * @param title   通知标题, 不能为空
     * @param content 通知内容, 不能为空
     */
    public static void notifyInfo(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.INFORMATION);
    }

    /**
     * 发送警告通知
     *
     * @param project 可能为 null 的项目对象
     * @param title   通知标题, 不可为 null
     * @param content 通知内容, 不可为 null
     */
    public static void notifyWarning(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.WARNING);
    }

    /**
     * 发送错误通知
     *
     * @param project 项目对象, 可以为 null
     * @param title   通知标题, 不能为空
     * @param content 通知内容, 不能为空
     */
    public static void notifyError(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.ERROR);
    }

    /**
     * 显示信息通知（简化版本，使用默认标题）
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不能为空
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Nacos", message, NotificationType.INFORMATION);
    }

    /**
     * 显示警告通知（简化版本，使用默认标题）
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不能为空
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Nacos", message, NotificationType.WARNING);
    }

    /**
     * 显示错误通知（简化版本，使用默认标题）
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不能为空
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Nacos", message, NotificationType.ERROR);
    }

    /**
     * 向指定项目发送通知
     *
     * @param project 项目对象, 可为空
     * @param title   通知标题
     * @param content 通知内容
     * @param type    通知类型
     */
    private static void notify(@Nullable Project project, @NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notification notification = getNotificationGroup().createNotification(title, content, type);
        notification.notify(project);
    }
}

