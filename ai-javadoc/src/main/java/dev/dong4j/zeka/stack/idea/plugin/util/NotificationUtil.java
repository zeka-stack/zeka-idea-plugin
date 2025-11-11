package dev.dong4j.zeka.stack.idea.plugin.util;

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

import dev.dong4j.zeka.stack.idea.plugin.settings.JavaDocSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * 通知工具类
 * <p>
 * 提供统一的通知功能, 用于在项目中展示不同级别的通知信息, 如信息, 警告和错误等.
 * 支持根据不同的任务状态生成对应的通知内容, 并通过指定的项目上下文进行展示.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public class NotificationUtil {
    /**
     * 通知分组的 ID
     * <p>
     * 用于标识一组与 JavaDoc 生成相关的通知
     */
    public static final String NOTIFICATION_GROUP_ID = "AI Javadoc Notifications";

    /**
     * 获取指定 ID 的通知组
     * <p>
     * 从通知组管理器中获取与给定 ID 关联的预定义通知组
     *
     * @return 与 NOTIFICATION_GROUP_ID 关联的 NotificationGroup 实例
     * @since 1.0
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

    /**
     * 根据完成和失败的数量确定通知类型
     * <p>
     * 根据传入的完成任务数和失败任务数判断应使用哪种通知类型. 如果存在失败任务, 则返回警告类型; 如果只有完成任务, 则返回信息类型; 如果两者都为零, 则默认返回警告类型.
     *
     * @param completed 完成的任务数量
     * @param failed    失败的任务数量
     * @return 返回对应的通知类型, 可能是 {@link NotificationType#WARNING} 或 {@link NotificationType#INFORMATION}
     */
    @NotNull
    private static NotificationType getNotificationType(int completed, int failed) {
        NotificationType type;
        if (failed > 0) {
            type = NotificationType.WARNING;
        } else if (completed > 0) {
            type = NotificationType.INFORMATION;
        } else {
            type = NotificationType.WARNING;
        }
        return type;
    }

    /**
     * 通知目标完成状态
     * <p>
     * 根据项目, 目标名称以及完成, 失败, 跳过数量生成通知内容, 并发送通知.
     *
     * @param project   项目对象, 可以为 null
     * @param target    目标名称
     * @param completed 完成数量
     * @param failed    失败数量
     * @param skipped   跳过数量
     */
    public static void notifyTargetCompletion(@Nullable Project project, @NotNull String target,
                                              int completed, int failed, int skipped) {
        if (SettingsState.getInstance().verboseLogging) {
            String content = JavaDocBundle.message("notification.target.completion.format", target, completed, failed, skipped);
            final NotificationType type = getNotificationType(completed, failed);
            notify(project, JavaDocBundle.message("notification.generation.complete"), content, type);
        }
    }

    /**
     * 通知用户无任务可执行
     * <p>
     * 该方法会弹出一个信息类型的通知, 标题为 {@code JavaDocBundle.message("notification.title")}, 内容为传入的 {@code message}. 如果 {@code project} 为 {@code null
     * }, 则通知不绑定到任何项目.
     *
     * @param project 可为空的项目对象, 若为 {@code null} 则通知不关联任何项目
     * @param message 通知内容, 不能为空
     */
    public static void notifyNoTask(@Nullable Project project, @NotNull String message) {
        notify(project, JavaDocBundle.message("notification.title"), message, NotificationType.INFORMATION);
    }

    /**
     * 显示错误通知消息
     * <p>
     * 在指定的项目上下文中显示一条带有错误标题的通知消息
     *
     * @param project 可为空的项目对象, 用于确定通知的显示上下文
     * @param message 要显示的错误消息内容
     */
    public static void notifyErrorMessage(@Nullable Project project, @NotNull String message) {
        notify(project, JavaDocBundle.message("notification.error.title"), message, NotificationType.ERROR);
    }

    /**
     * 通知用户项目正在索引
     * <p>
     * 向用户发送通知, 提示项目正在进行索引操作
     *
     * @param project 项目对象, 可以为 null
     */
    public static void notifyIndexing(@Nullable Project project) {
        notify(project, JavaDocBundle.message("notification.title"),
               JavaDocBundle.message("notification.indexing.warning"),
               NotificationType.WARNING);
    }

    /**
     * 向通知中添加可配置的面板操作项
     * <p>
     * 为指定的通知添加一个操作项, 点击该操作项将打开 JavaDoc 设置配置面板, 并使通知消失
     *
     * @param notification 要添加操作项的通知对象
     * @param project      项目对象, 用于关联配置面板和项目
     */
    public static void addOpenConfigurablePanelAction(Notification notification, Project project) {
        notification.addAction(new NotificationAction(JavaDocBundle.message("notification.error.message.config")) {
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
                JavaDocSettingsConfigurable configurable = new JavaDocSettingsConfigurable();
                // 打开设置面板
                ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
                notification.expire();
            }
        });
        Notifications.Bus.notify(notification, project);
    }
}

