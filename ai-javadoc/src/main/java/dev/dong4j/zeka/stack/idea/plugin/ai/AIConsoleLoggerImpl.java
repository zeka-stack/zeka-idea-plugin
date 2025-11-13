package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.console.JavaDocConsoleView;

/**
 * AI 控制台日志实现
 * <p>
 * 将通用的日志接口适配到 JavaDocConsoleView
 */
public class AIConsoleLoggerImpl implements AIConsoleLogger {
    /**
     * 当前操作的项目对象
     * <p>
     * 用于存储和操作与当前任务或功能相关的项目数据
     */
    private final Project project;

    /**
     * 构造函数, 用于初始化 AIConsoleLoggerImpl 实例
     * <p>
     * 将传入的 Project 对象赋值给当前实例的 project 属性
     *
     * @param project 项目对象, 不能为空
     */
    public AIConsoleLoggerImpl(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 打印指定的消息到控制台视图
     * <p>
     * 将传入的消息内容输出到 JavaDoc 控制台视图中
     *
     * @param message 要打印的消息内容
     */
    @Override
    public void print(@NotNull String message) {
        JavaDocConsoleView.print(project, message);
    }

    /**
     * 带时间戳打印消息
     * <p>
     * 在控制台中以带时间戳的方式输出指定消息
     *
     * @param message 要打印的消息内容
     * @throws NullPointerException 如果 message 为 null(由 @NotNull 注解保证)
     */
    @Override
    public void printWithTimestamp(@NotNull String message) {
        JavaDocConsoleView.printWithTimestamp(project, message);
    }

    /**
     * 打印成功信息
     * <p>
     * 将指定的成功消息输出到 {@link JavaDocConsoleView} 控制台视图.
     *
     * @param message 要打印的成功消息, 不能为空
     */
    @Override
    public void printSuccess(@NotNull String message) {
        JavaDocConsoleView.printSuccess(project, message);
    }

    /**
     * 打印警告信息到控制台视图
     * <p>
     * 将指定的警告信息输出到 JavaDoc 控制台视图中
     *
     * @param message 要打印的警告信息
     */
    @Override
    public void printWarning(@NotNull String message) {
        JavaDocConsoleView.printWarning(project, message);
    }

    /**
     * 输出错误信息到控制台视图
     * <p>
     * 将指定的错误信息打印到 JavaDoc 控制台视图中
     *
     * @param message 错误信息内容
     */
    @Override
    public void printError(@NotNull String message) {
        JavaDocConsoleView.printError(project, message);
    }

    /**
     * 打印超链接到控制台
     * <p>
     * 在指定的虚拟文件和行号处, 将给定消息作为超链接输出到控制台.
     *
     * @param message     要显示的超链接文本消息
     * @param virtualFile 虚拟文件对象, 用于定位超链接位置
     * @param line        超链接所在的行号
     * @throws NullPointerException 如果 message,virtualFile 为 null(由 @NotNull 注解保证)
     */
    @Override
    public void printHyperlink(@NotNull String message, @NotNull VirtualFile virtualFile, int line) {
        JavaDocConsoleView.printHyperlink(project, message, virtualFile, line);
    }

    /**
     * 根据消息和虚拟文件打印带时间戳的超链接
     * <p>
     * 该方法用于在控制台中打印带有时间戳的超链接信息, 通常用于调试或日志记录.
     *
     * @param message     要打印的消息内容
     * @param virtualFile 虚拟文件对象, 用于获取文件路径信息
     * @param line        行号, 表示消息所在的代码行
     */
    @Override
    public void printHyperlinkWithTimestamp(@NotNull String message, @NotNull VirtualFile virtualFile, int line) {
        JavaDocConsoleView.printHyperlinkWithTimestamp(project, message, virtualFile, line);
    }
}

