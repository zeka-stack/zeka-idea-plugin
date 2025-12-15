package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

/**
 * AI 控制台日志记录器接口
 * <p>
 * 定义了 AI 相关控制台日志输出的统一接口规范, 提供多种日志输出方式,
 * 包括普通消息, 带时间戳消息, 成功 / 警告 / 错误消息以及超链接格式的日志输出
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public interface AIConsoleLogger {
    /**
     * 输出普通日志
     *
     * @param message 消息内容
     */
    void print(String message);

    /**
     * 输出带时间戳的日志
     *
     * @param message 消息内容
     */
    void printWithTimestamp(String message);

    /**
     * 输出成功日志
     *
     * @param message 消息内容
     */
    void printSuccess(String message);

    /**
     * 输出警告日志
     *
     * @param message 消息内容
     */
    void printWarning(String message);

    /**
     * 输出错误日志
     *
     * @param message 消息内容
     */
    void printError(String message);

    /**
     * 输出可点击的超链接（跳转到代码位置）
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    void printHyperlink(String message, @NotNull VirtualFile virtualFile, int line);

    /**
     * 输出带时间戳的可点击超链接
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    void printHyperlinkWithTimestamp(String message, @NotNull VirtualFile virtualFile, int line);
}

