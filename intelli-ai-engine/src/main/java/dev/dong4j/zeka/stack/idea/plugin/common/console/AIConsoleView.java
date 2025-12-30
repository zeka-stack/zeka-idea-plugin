package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * AI 控制台视图服务类
 * <p>
 * 该类实现了 IntelliJ IDEA 插件中的统一控制台日志输出功能, 提供带时间戳的消息打印,
 * 不同类型日志输出 (成功, 警告, 错误), 超链接支持等功能.
 * 作为项目级别的服务, 负责管理所有使用 IntelliAI Engine 的插件的日志输出.
 * <p>
 * 这是 IntelliAI Engine 提供的统一控制台, 所有基于 Engine 的插件都可以使用此控制台
 * 来输出 AI 相关的日志信息, 实现日志的统一管理和查看.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class AIConsoleView implements Disposable, AIConsoleLogger {

    /** 工具窗口 ID */
    public static final String TOOL_WINDOW_ID = "IntelliAI Engine Console";

    /** 时间格式：yyyy.MM.dd HH:mm:ss */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    /** Console 视图实例 */
    private ConsoleView consoleView;

    /** 项目实例 */
    private final Project project;

    /** 是否已显示欢迎信息 */
    private boolean welcomeMessageShown = false;

    private final StringBuilder streamBuffer = new StringBuilder();

    /**
     * 构造函数
     *
     * @param project 项目实例
     */
    public AIConsoleView(Project project) {
        this.project = project;
    }

    /**
     * 初始化 Console
     *
     * @return Console 视图实例
     */
    public ConsoleView initConsole() {
        if (consoleView == null) {
            consoleView = TextConsoleBuilderFactory.getInstance()
                .createBuilder(project)
                .getConsole();
        }
        return consoleView;
    }

    /**
     * 获取 Console 视图
     *
     * @return Console 视图实例
     */
    public ConsoleView getConsoleView() {
        if (consoleView == null) {
            initConsole();
        }
        return consoleView;
    }

    /**
     * 显示工具窗口
     */
    private void showToolWindow() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
            if (toolWindow != null && !toolWindow.isVisible()) {
                toolWindow.show(null);
            }
        });
    }

    /**
     * 获取实例（静态方法）
     *
     * @param project 项目实例
     * @return Console 视图实例
     */
    @NotNull
    public static AIConsoleView getInstance(@NotNull Project project) {
        return project.getService(AIConsoleView.class);
    }

    /**
     * 输出普通信息（带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    @Override
    public void printWithTimestamp(String message) {
        String timestamp = "[" + TIME_FORMAT.format(new Date()) + "] ";
        printInternal(timestamp + message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    /**
     * 输出普通信息（不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    @Override
    public void print(String message) {
        printInternal(message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    /**
     * 输出成功信息（绿色，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    @Override
    public void printSuccess(String message) {
        printInternal(message + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    /**
     * 输出警告信息（黄色，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    @Override
    public void printWarning(String message) {
        printInternal(message + "\n", ConsoleViewContentType.LOG_WARNING_OUTPUT);
    }

    /**
     * 输出错误信息（红色，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    @Override
    public void printError(String message) {
        printInternal(message + "\n", ConsoleViewContentType.ERROR_OUTPUT);
    }

    /**
     * 内部输出方法（实际执行输出操作）
     *
     * @param message     消息内容
     * @param contentType 内容类型
     */
    private void printInternal(String message, @NotNull ConsoleViewContentType contentType) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                // 首次输出日志时，先显示欢迎信息
                if (!welcomeMessageShown) {
                    printWelcomeMessage();
                    welcomeMessageShown = true;
                }
                console.print(message, contentType);
                showToolWindow();
            }
        });
    }

    /**
     * 输出欢迎信息（不受 verboseLogging 控制）
     * <p>
     * 用于在控制台初始化时显示欢迎信息和使用说明。
     *
     * @param message 消息内容
     */
    private void printWelcome(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                console.print(message, ConsoleViewContentType.NORMAL_OUTPUT);
                // 注意：printWelcome 不调用 showToolWindow()，由调用者控制
            }
        });
    }

    /**
     * 输出欢迎信息和使用说明（不受 verboseLogging 控制）
     * <p>
     * 在首次有日志输出时自动显示 IntelliAI Engine 的欢迎信息、使用方式和提示。
     * 该方法会在首次调用时显示欢迎信息，后续调用不会重复显示。
     */
    @SuppressWarnings("DuplicatedCode")
    private void printWelcomeMessage() {
        printWelcome("╔════════════════════════════════════════════════════════════════╗\n");
        printWelcome("║          " + AICommonBundle.message("console.welcome.title") + "             ║\n");
        printWelcome("╚════════════════════════════════════════════════════════════════╝\n");
        printWelcome("");
        printWelcome(AICommonBundle.message("console.welcome.description") + "\n");
        printWelcome("");
        printWelcome(AICommonBundle.message("console.welcome.tips.title") + "\n");
        printWelcome(AICommonBundle.message("console.welcome.tips.verbose.logging") + "\n");
        printWelcome(AICommonBundle.message("console.welcome.tips.code.location") + "\n");
        printWelcome("────────────────────────────────────────────────────────────────\n");
    }

    /**
     * 输出可点击的超链接（跳转到代码位置）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    @Override
    public void printHyperlink(String message, @NotNull VirtualFile virtualFile, int line) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                // 创建超链接信息
                HyperlinkInfo hyperlinkInfo = project1 -> {
                    // 打开文件并跳转到指定行
                    new OpenFileDescriptor(project, virtualFile, line, 0).navigate(true);
                };

                // 输出带超链接的消息
                console.printHyperlink(message, hyperlinkInfo);
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                showToolWindow();
            }
        });
    }

    /**
     * 输出带时间戳的可点击超链接（跳转到代码位置）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message     消息内容
     * @param virtualFile 目标文件
     * @param line        目标行号（从 0 开始）
     */
    @Override
    public void printHyperlinkWithTimestamp(String message, @NotNull VirtualFile virtualFile, int line) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                // 输出时间戳
                String timestamp = "[" + TIME_FORMAT.format(new Date()) + "] ";
                console.print(timestamp, ConsoleViewContentType.NORMAL_OUTPUT);

                // 创建超链接信息
                HyperlinkInfo hyperlinkInfo = project1 -> {
                    // 打开文件并跳转到指定行
                    new OpenFileDescriptor(project, virtualFile, line, 0).navigate(true);
                };

                // 输出带超链接的消息
                console.printHyperlink(message, hyperlinkInfo);
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                showToolWindow();
            }
        });
    }

    @Override
    public void printStream(@NotNull String chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        streamBuffer.append(chunk);
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                if (!welcomeMessageShown) {
                    printWelcomeMessage();
                    welcomeMessageShown = true;
                }
                console.print(chunk, ConsoleViewContentType.NORMAL_OUTPUT);
                showToolWindow();
            }
        });
    }

    @Override
    public void completeStream() {
        streamBuffer.setLength(0);
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                if (!welcomeMessageShown) {
                    printWelcomeMessage();
                    welcomeMessageShown = true;
                }
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                showToolWindow();
            }
        });
    }

    @Override
    public void printStreamPlain(@NotNull String chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                console.print(chunk, ConsoleViewContentType.NORMAL_OUTPUT);
                showToolWindow();
            }
        });
    }

    @Override
    public void completeStreamPlain() {
        streamBuffer.setLength(0);
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
                showToolWindow();
            }
        });
    }

    /**
     * 释放资源
     * <p>
     * 由 IntelliJ Platform 在项目关闭时自动调用。
     * 清理 Console 视图资源，避免内存泄漏。
     *
     * @see Disposable
     */
    @Override
    public void dispose() {
        if (consoleView != null) {
            consoleView.dispose();
            consoleView = null;
        }
    }
}
