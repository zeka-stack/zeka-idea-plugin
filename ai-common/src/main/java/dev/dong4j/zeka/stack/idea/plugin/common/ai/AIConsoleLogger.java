package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

/**
 * AI 控制台日志接口
 * <p>
 * 用于输出详细的过程日志，包括可点击的代码链接。
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

