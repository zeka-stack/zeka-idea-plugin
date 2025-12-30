package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.console.AIConsoleView;

/**
 * AI 控制台日志工具类
 * <p>
 * 提供简单的静态方法接口, 供第三方插件输出日志到 Engine 的统一控制台.
 * 所有基于 IntelliAI Engine 的插件都应该使用此工具类的静态方法来输出日志.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AIConsoleLoggerUtil {

    /**
     * 私有构造函数, 防止实例化
     */
    private AIConsoleLoggerUtil() {
        // 工具类, 禁止实例化
    }

    /**
     * 输出信息（静态方法，带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printWithTimestamp(Project project, String message) {
        safeLog(project, console -> console.printWithTimestamp(message));
    }

    /**
     * 输出信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void print(Project project, String message) {
        safeLog(project, console -> console.print(message));
    }

    /**
     * 输出成功信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printSuccess(Project project, String message) {
        safeLog(project, console -> console.printSuccess(message));
    }

    /**
     * 输出警告信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printWarning(Project project, String message) {
        safeLog(project, console -> console.printWarning(message));
    }

    /**
     * 输出错误信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printError(Project project, String message) {
        safeLog(project, console -> console.printError(message));
    }

    /**
     * 输出可点击的超链接（静态方法）
     * <p>
     * 输出一个可点击的超链接，点击后跳转到指定文件的指定行。
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project     项目实例
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    public static void printHyperlink(Project project, String message,
                                      @NotNull VirtualFile virtualFile, int line) {
        safeLog(project, console -> console.printHyperlink(message, virtualFile, line));
    }

    /**
     * 输出带时间戳的可点击超链接（静态方法）
     * <p>
     * 输出一个带时间戳的可点击超链接，点击后跳转到指定文件的指定行。
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project     项目实例
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    public static void printHyperlinkWithTimestamp(Project project, String message,
                                                   @NotNull VirtualFile virtualFile, int line) {
        safeLog(project, console -> console.printHyperlinkWithTimestamp(message, virtualFile, line));
    }

    /**
     * 流式输出内容（追加，不换行）
     *
     * @param project 项目实例
     * @param chunk   增量内容块
     */
    public static void printStream(Project project, String chunk) {
        safeLog(project, console -> console.printStream(chunk));
    }

    /**
     * 完成流式输出（收口与换行）
     *
     * @param project 项目实例
     */
    public static void completeStream(Project project) {
        safeLog(project, AIConsoleView::completeStream);
    }

    /**
     * 流式输出内容（不触发欢迎信息，不自动追加换行）
     *
     * @param project 项目实例
     * @param chunk   增量内容块
     */
    public static void printStreamPlain(Project project, String chunk) {
        safeLog(project, console -> console.printStreamPlain(chunk));
    }

    /**
     * 完成流式输出（不追加换行）
     *
     * @param project 项目实例
     */
    public static void completeStreamPlain(Project project) {
        safeLog(project, AIConsoleView::completeStreamPlain);
    }

    /**
     * 安全地输出日志（带空值检查）
     * <p>
     * 如果控制台不可用, 则静默忽略, 避免影响主功能.
     *
     * @param project 项目对象
     * @param action  日志输出操作
     */
    private static void safeLog(Project project, @NotNull LogAction action) {
        if (project == null) {
            return;
        }

        // 检查全局 verboseLogging 设置
        final boolean verboseLogging = AIProviderSettings.getInstance().verboseLogging;
        if (!verboseLogging) {
            return;
        }
        try {
            AIConsoleView consoleView = AIConsoleView.getInstance(project);
            action.execute(consoleView);
        } catch (Exception e) {
            // 静默忽略异常, 避免影响主功能
        }
    }

    /**
     * 日志输出操作接口
     */
    @FunctionalInterface
    private interface LogAction {
        /**
         * 执行日志输出操作
         *
         * @param consoleView 控制台视图
         */
        void execute(@NotNull AIConsoleView consoleView);
    }
}
