package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import com.intellij.notification.Notification;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.nacos.PluginContents;

/**
 * 通知工具类
 * <p> 提供显示信息, 警告和错误通知的功能, 支持在 IntelliJ IDEA 插件中集成.
 * <p> 该类主要用于向用户展示不同类型的系统消息, 包括信息, 警告和错误通知.
 * <p> 此外, 还提供了添加可配置面板操作的功能, 允许用户通过点击通知进入相关设置界面.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class NotificationUtil {
    /**
     * 通知组的唯一标识符
     * <p> 用于区分不同的通知来源, 确保通知能够正确分组和显示
     *
     * @see #showInfo(Project, String)
     * @see #showWarning(Project, String)
     * @see #showError(Project, String)
     */
    public static final String NOTIFICATION_GROUP_ID = "IntelliAI Nacos Notifications";

    /**
     * 显示信息通知
     * <p> 在指定的项目中显示一条信息级别的通知消息
     *
     * @param project 项目对象, 可以为 null, 表示全局通知
     * @param message 通知消息, 不能为空
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showInfo(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示警告通知
     * <p> 在指定的项目中显示带有警告级别的通知信息
     *
     * @param project 项目对象, 可以为 null
     * @param message 警告消息, 不能为空
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showWarning(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示错误通知
     * <p> 在指定的项目中显示一条错误级别的通知信息
     *
     * @param project 项目对象, 可以为 null
     * @param message 错误信息, 不能为空
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 向通知中添加可配置的面板操作项
     * <p> 为指定的通知添加一个操作项, 点击该操作项将打开 Nacos 设置配置面板, 并使通知消失
     *
     * @param notification 要添加操作项的通知对象
     * @param project      项目对象, 用于关联配置面板和项目
     */
    public static void addOpenConfigurablePanelAction(Notification notification, Project project) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.addOpenConfigurablePanelAction(
            project,
            notification,
            NacosBundle.message("settings.display.name"),
            NacosBundle.message("notification.error.message.config"));

    }
}

