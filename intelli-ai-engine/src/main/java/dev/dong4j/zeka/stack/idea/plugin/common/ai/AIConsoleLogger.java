package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

/**
 * 控制台日志记录接口
 * <p>
 * 提供多种日志输出方法, 支持带时间戳, 成功信息, 警告信息, 错误信息以及超链接的日志打印, 适用于在控制台或 IDE 控制台中展示不同类型的日志信息, 增强日志的可读性和可交互性.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public interface AIConsoleLogger {
    /**
     * 输出普通日志
     *
     * @param message 消息内容
     */
    void print(@NotNull String message);

    /**
     * 输出带时间戳的日志
     *
     * @param message 消息内容
     */
    void printWithTimestamp(@NotNull String message);

    /**
     * 输出成功日志
     *
     * @param message 消息内容
     */
    void printSuccess(@NotNull String message);

    /**
     * 输出警告日志
     *
     * @param message 消息内容
     */
    void printWarning(@NotNull String message);

    /**
     * 输出错误日志
     *
     * @param message 消息内容
     */
    void printError(@NotNull String message);

    /**
     * 输出可点击的超链接（跳转到代码位置）
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    void printHyperlink(@NotNull String message, @NotNull VirtualFile virtualFile, int line);

    /**
     * 输出带时间戳的可点击超链接
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    void printHyperlinkWithTimestamp(@NotNull String message, @NotNull VirtualFile virtualFile, int line);
}

