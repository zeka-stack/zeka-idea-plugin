package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

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
 * 支持根据不同的任务状态生成对应的通知内容, 并通过指定的项目上下文进行展示.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class NotificationUtil {
    /**
     * 通知分组的 ID
     * <p>
     * 用于标识一组与插件相关的通知
     */
    public static final String NOTIFICATION_GROUP_ID = "IntelliAI Changelog Notifications";

    /**
     * 获取指定 ID 的通知组
     * <p>
     * 从通知组管理器中获取与给定 ID 关联的预定义通知组
     *
     * @return 与 NOTIFICATION_GROUP_ID 关联的 NotificationGroup 实例
     * @since 1.0.0
     */
    @NotNull
    private static NotificationGroup getNotificationGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
    }

    /**
     * 发送信息通知
     * <p>
     * 根据指定的项目, 标题和内容发送信息类型的通知
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
     * <p>
     * 向指定项目发送带有指定标题和内容的警告类型通知.
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
     * <p>
     * 向指定项目发送一条错误类型的通知, 包含指定的标题和内容
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
     * <p>
     * 使用默认标题发送信息类型的通知
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不能为空
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Changelog", message, NotificationType.INFORMATION);
    }

    /**
     * 显示警告通知（简化版本，使用默认标题）
     * <p>
     * 使用默认标题发送警告类型的通知
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不能为空
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Changelog", message, NotificationType.WARNING);
    }

    /**
     * 显示错误通知（简化版本，使用默认标题）
     * <p>
     * 使用默认标题发送错误类型的通知
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不能为空
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        notify(project, "IntelliAI Changelog", message, NotificationType.ERROR);
    }

    /**
     * 向指定项目发送通知
     * <p>
     * 创建一个通知对象并发送至对应的项目
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
