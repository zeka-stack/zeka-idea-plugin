package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.swagger.PluginContents;

/**
 * 通知工具类
 * <p> 提供在 IntelliJ IDEA 插件中显示信息, 警告和错误通知的静态方法
 * <p> 该工具类封装了通用的通知逻辑, 简化插件开发中的用户交互操作
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
     * <p> 用于在项目中展示一般级别的通知信息, 通常用于提示用户非错误类的信息
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
     * <p> 在指定项目上下文中显示带有警告级别的通知信息, 通知标题为插件名称
     *
     * @param project 项目对象, 可以为 null, 表示全局通知
     * @param message 通知内容, 不能为空
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showWarning(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示错误通知
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容, 不可为空
     * @since 1.0.0
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, message);
    }

}
