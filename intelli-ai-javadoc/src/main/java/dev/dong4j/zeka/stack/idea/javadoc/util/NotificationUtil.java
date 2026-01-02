package dev.dong4j.zeka.stack.idea.javadoc.util;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;

/**
 * 通知工具类
 * <p>
 * 提供统一的通知消息处理功能, 包括信息, 警告, 错误等不同类型的通知显示,
 * 以及特定业务场景的通知处理, 如任务完成通知, 索引提示等
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class NotificationUtil {

    /**
     * 显示信息通知
     *
     * @param project 项目对象, 可以为 null
     * @param message 通知内容
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showInfo(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示警告通知
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showWarning(project, PluginContents.PLUGIN_NAME, message);
    }

    /**
     * 显示错误通知
     * <p>
     * 根据提供的项目对象和错误信息, 显示一条错误类型的通知.
     *
     * @param project 项目对象, 可为空
     * @param message 通知内容
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, message);
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
        // nothing
    }

    /**
     * 通知用户项目正在索引
     * <p>
     * 向用户发送通知, 提示项目正在进行索引操作
     *
     * @param project 项目对象, 可以为 null
     */
    public static void notifyIndexing(@Nullable Project project) {
        showWarning(project, JavadocBundle.message("notification.indexing.warning"));
    }

}

