package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * Project 控制台日志适配器
 * <p>
 * 将 IntelliJ {@link Project} 适配为 core 执行链所需的 {@link AIConsoleLogger},
 * 全部输出委托 {@link AIConsoleLoggerUtil}, 复用其 verboseLogging 开关检查与
 * 项目级控制台(AIConsoleView)定位逻辑.
 * <p>
 * 存在背景: core 模块不允许依赖 Project, 执行链(AIServiceFactory/Provider/Executor)
 * 统一以 {@link AIConsoleLogger} 记录日志; IDE 宿主在入口处用本类完成类型适配,
 * MCP Server 宿主则提供输出到 stdout/文件的实现.
 * <p>
 * 使用示例:
 * <pre>{@code
 * AIServiceProvider provider = AIServiceFactory.createProvider(
 *     new ProjectAIConsoleLogger(project), config);
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ProjectAIConsoleLogger implements AIConsoleLogger {

    /** 当前项目上下文, 用于定位日志控制台 */
    private final Project project;

    /**
     * 初始化适配器
     *
     * @param project 项目实例, 不能为 null
     */
    public ProjectAIConsoleLogger(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 输出普通日志
     *
     * @param message 消息内容
     */
    @Override
    public void print(@NotNull String message) {
        AIConsoleLoggerUtil.print(project, message);
    }

    /**
     * 输出带时间戳的日志
     *
     * @param message 消息内容
     */
    @Override
    public void printWithTimestamp(@NotNull String message) {
        AIConsoleLoggerUtil.printWithTimestamp(project, message);
    }

    /**
     * 输出成功日志
     *
     * @param message 消息内容
     */
    @Override
    public void printSuccess(@NotNull String message) {
        AIConsoleLoggerUtil.printSuccess(project, message);
    }

    /**
     * 输出警告日志
     *
     * @param message 消息内容
     */
    @Override
    public void printWarning(@NotNull String message) {
        AIConsoleLoggerUtil.printWarning(project, message);
    }

    /**
     * 输出错误日志
     *
     * @param message 消息内容
     */
    @Override
    public void printError(@NotNull String message) {
        AIConsoleLoggerUtil.printError(project, message);
    }

    /**
     * 输出可点击的超链接
     * <p>
     * core 侧传入的是文件路径, 这里解析回 {@link VirtualFile} 后委托原工具方法;
     * 路径无法解析(文件已被删除/外部路径)时降级为普通日志输出.
     *
     * @param message  消息内容
     * @param filePath 目标文件路径
     * @param line     目标行号（从 0 开始）
     */
    @Override
    public void printHyperlink(@NotNull String message, @NotNull String filePath, int line) {
        VirtualFile virtualFile = findFile(filePath);
        if (virtualFile != null) {
            AIConsoleLoggerUtil.printHyperlink(project, message, virtualFile, line);
        } else {
            AIConsoleLoggerUtil.print(project, message);
        }
    }

    /**
     * 输出带时间戳的可点击超链接
     * <p>
     * 路径解析逻辑同 {@link #printHyperlink(String, String, int)}.
     *
     * @param message  消息内容
     * @param filePath 目标文件路径
     * @param line     目标行号（从 0 开始）
     */
    @Override
    public void printHyperlinkWithTimestamp(@NotNull String message, @NotNull String filePath, int line) {
        VirtualFile virtualFile = findFile(filePath);
        if (virtualFile != null) {
            AIConsoleLoggerUtil.printHyperlinkWithTimestamp(project, message, virtualFile, line);
        } else {
            AIConsoleLoggerUtil.printWithTimestamp(project, message);
        }
    }

    /**
     * 流式输出内容（追加，不换行）
     *
     * @param chunk 增量内容块
     */
    @Override
    public void printStream(@NotNull String chunk) {
        AIConsoleLoggerUtil.printStream(project, chunk);
    }

    /**
     * 完成流式输出（收口与换行）
     */
    @Override
    public void completeStream() {
        AIConsoleLoggerUtil.completeStream(project);
    }

    /**
     * 流式输出内容（不自动追加换行）
     *
     * @param chunk 增量内容块
     */
    @Override
    public void printStreamPlain(@NotNull String chunk) {
        AIConsoleLoggerUtil.printStreamPlain(project, chunk);
    }

    /**
     * 完成流式输出（不追加换行）
     */
    @Override
    public void completeStreamPlain() {
        AIConsoleLoggerUtil.completeStreamPlain(project);
    }

    /**
     * 按路径查找虚拟文件
     *
     * @param filePath 文件路径
     * @return 对应的 VirtualFile, 找不到时返回 null
     */
    @Nullable
    private static VirtualFile findFile(@NotNull String filePath) {
        return LocalFileSystem.getInstance().findFileByPath(filePath);
    }
}
