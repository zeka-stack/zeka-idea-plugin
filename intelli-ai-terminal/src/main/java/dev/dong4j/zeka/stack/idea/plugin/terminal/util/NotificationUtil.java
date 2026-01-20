package dev.dong4j.zeka.stack.idea.plugin.terminal.util;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.terminal.PluginContents;

/**
 * 通知工具类
 * <p> 提供在 IntelliJ IDEA 插件中统一显示信息, 警告和错误通知的功能, 封装了对 {@code NotificationUtil} 的调用, 便于在插件中统一展示提示信息.
 * <p> 支持在指定项目上下文中显示通知, 通知标题默认使用插件名称 {@code PluginContents.PLUGIN_NAME}, 确保通知来源清晰.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class NotificationUtil {
    /**
     * 显示信息级别的通知
     * <p> 在指定项目中显示一条信息级别的通知, 通知内容由参数提供.
     *
     * @param project 项目对象, 可以为 null, 表示全局通知
     * @param message 通知内容, 不能为空
     * @since 1.0.0
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
     * <p> 通过指定的项目对象和错误信息展示一个错误级别的通知, 通常用于提示用户发生了严重问题.</p>
     *
     * @param project 项目对象, 可为空, 表示全局通知
     * @param message 错误通知内容, 不可为 null
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, message);
    }
}

