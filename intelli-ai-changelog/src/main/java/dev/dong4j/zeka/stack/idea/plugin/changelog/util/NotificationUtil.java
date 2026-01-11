package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;

/**
 * 通知工具类
 * <p>
 * 提供统一的通知消息处理功能, 支持信息, 警告和错误类型的通知显示,
 * 主要用于 IntelliAI Changelog 相关的消息通知展示
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class NotificationUtil {

    public static void noShow(@Nullable Project project, @NotNull String message) {

    }

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
