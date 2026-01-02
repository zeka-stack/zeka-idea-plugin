package dev.dong4j.zeka.stack.idea.plugin.example.util;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.example.PluginContents;

/**
 * 通知工具类
 * <p> 提供统一的通知展示功能, 用于在 IDEA 插件中显示信息, 警告和错误提示
 * <p> 支持通过指定的项目对象和消息内容展示不同类型的通知
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class NotificationUtil {
    /**
     * 显示信息级别的通知
     * <p> 在指定项目中显示一条信息级别的通知, 通知内容由参数提供.
     *
     * @param project 项目对象, 可以为 null
     * @param message 通知内容, 不能为空
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showInfo(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示警告通知
     * <p> 在指定项目上下文中显示带有警告级别的通知信息, 通知标题为插件名称
     *
     * @param project 项目对象, 可以为空, 表示全局通知
     * @param message 通知内容, 不能为空
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showWarning(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示错误通知
     * <p> 通过指定的项目对象和错误信息展示一个错误级别的通知, 通常用于提示用户发生了严重问题.
     *
     * @param project 项目对象, 可为空
     * @param message 错误通知内容, 不可为 null
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, message);
    }
}

