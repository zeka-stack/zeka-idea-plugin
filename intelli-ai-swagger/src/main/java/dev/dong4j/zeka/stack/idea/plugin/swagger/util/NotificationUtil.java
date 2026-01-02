package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.swagger.PluginContents;

/**
 * 通知工具类
 * <p>
 * 提供统一的通知功能, 用于在项目中展示不同级别的通知信息。
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NotificationUtil {

    /**
     * 显示信息通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showInfo(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示警告通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showWarning(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示错误通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, message);
    }

}
