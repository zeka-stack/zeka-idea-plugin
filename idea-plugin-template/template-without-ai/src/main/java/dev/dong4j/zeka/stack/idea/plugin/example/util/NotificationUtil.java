package dev.dong4j.zeka.stack.idea.plugin.example.util;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.example.PluginContents;

/**
 * 通知工具类
 * <p> 提供显示信息, 警告和错误通知的功能, 主要用于在 IntelliJ IDEA 插件中向用户展示不同类型的提示信息.
 * <p> 该类通过调用底层的 NotificationUtil 方法来显示不同类型的通知, 包括普通信息, 警告和错误.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class NotificationUtil {
    /**
     * 显示信息通知
     * <p> 用于在指定项目上下文中显示一条普通的信息通知, 通知标题为插件名称
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不可为空
     * @since 1.0.0
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showInfo(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示警告通知
     * <p> 调用底层通知工具类显示警告信息
     *
     * @param project 项目对象, 可以为空
     * @param message 要显示的警告内容, 不能为空
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showWarning(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示错误通知
     * <p> 在指定的项目中显示错误级别的通知信息
     *
     * @param project 项目对象, 可以为空
     * @param message 错误通知的内容, 不能为空
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, message);
    }
}

