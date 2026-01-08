package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * 通知工具类
 * <p>
 * 提供统一的通知功能，用于在插件中展示不同级别的通知信息。
 * 这是一个通用工具类，可以被所有插件使用。
 *
 * @author dong4j
 * @version 1.0.0
 */
@Slf4j
public class NotificationUtil {

    /**
     * 显示信息通知
     *
     * @param project    项目对象，可为空
     * @param pluginName 插件名称
     * @param message    通知内容
     */
    public static void showInfo(@Nullable Project project, @NotNull String pluginName, @NotNull String message) {
        notify(project, pluginName, message, NotificationType.INFORMATION);
    }

    /**
     * 显示警告通知
     *
     * @param project    项目对象，可为空
     * @param pluginName 插件名称
     * @param message    通知内容
     */
    public static void showWarning(@Nullable Project project, @NotNull String pluginName, @NotNull String message) {
        notify(project, pluginName, message, NotificationType.WARNING);
    }

    /**
     * 显示错误通知
     *
     * @param project    项目对象，可为空
     * @param pluginName 插件名称
     * @param message    通知内容
     */
    public static void showError(@Nullable Project project, @NotNull String pluginName, @NotNull String message) {
        notify(project, pluginName, message, NotificationType.ERROR);
    }

    /**
     * 发送通知
     * <p> 根据插件名称, 通知内容和通知类型创建并发送通知
     * <p> 首先根据插件名称获取对应的 NotificationGroup 对象, 如果找不到对应的 NotificationGroup, 则直接返回
     * <p> 否则, 创建一个新的通知对象并发送给指定的项目
     *
     * @param project    项目对象, 可为空
     * @param pluginName 插件名称, 用于标识通知组
     * @param content    通知内容
     * @param type       通知类型
     * @since 1.0
     */
    private static void notify(@Nullable Project project,
                               @NotNull String pluginName,
                               @NotNull String content,
                               @NotNull NotificationType type) {
        NotificationGroup notificationGroup = getNotificationGroup(pluginName);
        if (notificationGroup == null) {
            return;
        }
        Notification notification = notificationGroup.createNotification(pluginName, content, type);
        notification.notify(project);
        notification.expire();
    }

    /**
     * 获取通知组的唯一标识符
     * <p> 根据插件名称生成对应的 NotificationGroup 的 ID
     * <p>ID 格式为 "插件名称 Notifications"
     *
     * @param pluginName 插件名称, 用于标识通知组
     * @return 通知组的唯一标识符
     */
    public static String getGroupId(String pluginName) {
        return cleanPluginName(pluginName) + " Notifications";
    }

    /**
     * 清理插件名称
     * <p> 去除插件名称中的补充字符和其他符号, 保留有效的字符
     * <p> 主要用于生成通知组的唯一标识符时清理插件名称
     *
     * @param pluginName 插件名称, 可以为 null
     * @return 清理后的插件名称, 如果原始名称为 null 则返回 null
     */
    public static String cleanPluginName(String pluginName) {
        if (pluginName == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        pluginName.codePoints()
            .filter(cp -> !Character.isSupplementaryCodePoint(cp)
                          || Character.getType(cp) != Character.OTHER_SYMBOL)
            .forEach(sb::appendCodePoint);

        return sb.toString().trim();
    }

    /**
     * 获取通知组
     * <p> 根据插件名称获取对应的 NotificationGroup 对象
     * <p> 如果插件名称对应的 NotificationGroup 不存在, 则会返回 null
     *
     * @param pluginName 插件名称, 用于标识通知组
     * @return 对应插件名称的 NotificationGroup 对象
     */
    public static @Nullable NotificationGroup getNotificationGroup(String pluginName) {
        String notificationGroupId = getGroupId(pluginName);
        NotificationGroup notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(notificationGroupId);
        if (notificationGroup == null) {
            log.debug("Notification group not found: {}", notificationGroupId);
            return null; // or throw an exception, depending on your use case
        }
        return notificationGroup;
    }

    public static void showInfo(@NotNull String message) {

    }

}

