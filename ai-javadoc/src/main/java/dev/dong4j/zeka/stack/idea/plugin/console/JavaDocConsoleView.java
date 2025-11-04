package dev.dong4j.zeka.stack.idea.plugin.console;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * AI Javadoc Console 视图
 * <p>
 * 用于在 IDE 底部工具窗口中显示 AI 接口的请求参数和响应结果。
 * 所有日志输出方法都受 verboseLogging 配置控制，只有启用时才会输出。
 *
 * <p>功能特性：
 * <ul>
 *   <li>项目级别服务，每个项目独立的 Console 实例</li>
 *   <li>支持带/不带时间戳的正常输出</li>
 *   <li>支持成功（绿色）、警告（黄色）、错误（红色）输出</li>
 *   <li>自动显示工具窗口</li>
 *   <li>时间戳格式：[yyyy.MM.dd HH:mm:ss]</li>
 *   <li>所有方法都受 verboseLogging 配置控制</li>
 * </ul>
 *
 * @author dong4j
 * @version 2.0.0
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class JavaDocConsoleView implements Disposable {

    /** 工具窗口 ID */
    public static final String TOOL_WINDOW_ID = "AI Javadoc Console";
    
    /** 时间格式：yyyy.MM.dd HH:mm:ss */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

    /** Console 视图实例 */
    private ConsoleView consoleView;

    /** 项目实例 */
    private final Project project;

    /** 设置状态 */
    private final SettingsState settings;

    /**
     * 构造函数
     *
     * @param project 项目实例
     */
    public JavaDocConsoleView(@NotNull Project project) {
        this.project = project;

        this.settings = SettingsState.getInstance();
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
     * 检查是否启用详细日志
     *
     * @return 如果启用详细日志返回 true
     */
    public boolean verboseLoggingDisable() {
        return !settings.verboseLogging;
    }

    /**
     * 获取实例（静态方法）
     *
     * @param project 项目实例
     * @return Console 视图实例
     */
    @NotNull
    public static JavaDocConsoleView getInstance(@NotNull Project project) {
        return project.getService(JavaDocConsoleView.class);
    }

    /**
     * 输出普通信息（带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    private void printWithTimestamp(@NotNull String message) {
        if (verboseLoggingDisable()) {
            return;
        }
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
    void print(@NotNull String message) {
        if (verboseLoggingDisable()) {
            return;
        }
        printInternal(message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    /**
     * 输出成功信息（绿色，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    private void printSuccess(@NotNull String message) {
        if (verboseLoggingDisable()) {
            return;
        }
        printInternal(message + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    /**
     * 输出警告信息（黄色，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    private void printWarning(@NotNull String message) {
        if (verboseLoggingDisable()) {
            return;
        }
        printInternal(message + "\n", ConsoleViewContentType.LOG_WARNING_OUTPUT);
    }

    /**
     * 输出错误信息（红色，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param message 消息内容
     */
    private void printError(@NotNull String message) {
        if (verboseLoggingDisable()) {
            return;
        }
        printInternal(message + "\n", ConsoleViewContentType.ERROR_OUTPUT);
    }

    /**
     * 内部输出方法（实际执行输出操作）
     *
     * @param message     消息内容
     * @param contentType 内容类型
     */
    private void printInternal(@NotNull String message, @NotNull ConsoleViewContentType contentType) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ConsoleView console = getConsoleView();
            if (console != null) {
                console.print(message, contentType);
                showToolWindow();
            }
        });
    }


    /**
     * 输出信息（静态方法，带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printWithTimestamp(Project project, @NotNull String message) {
        if (project == null) {
            return;
        }
        try {
            getInstance(project).printWithTimestamp(message);
        } catch (Exception e) {
            // 忽略异常，避免影响主功能
        }
    }

    /**
     * 输出信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void print(Project project, @NotNull String message) {
        if (project == null) {
            return;
        }
        try {
            getInstance(project).print(message);
        } catch (Exception e) {
            // 忽略异常，避免影响主功能
        }
    }

    /**
     * 输出成功信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printSuccess(Project project, @NotNull String message) {
        if (project == null) {
            return;
        }
        try {
            getInstance(project).printSuccess(message);
        } catch (Exception e) {
            // 忽略异常，避免影响主功能
        }
    }

    /**
     * 输出警告信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printWarning(Project project, @NotNull String message) {
        if (project == null) {
            return;
        }
        try {
            getInstance(project).printWarning(message);
        } catch (Exception e) {
            // 忽略异常，避免影响主功能
        }
    }

    /**
     * 输出错误信息（静态方法，不带时间戳）
     * <p>
     * 仅在 verboseLogging 启用时输出。
     *
     * @param project 项目实例
     * @param message 消息内容
     */
    public static void printError(Project project, @NotNull String message) {
        if (project == null) {
            return;
        }
        try {
            getInstance(project).printError(message);
        } catch (Exception e) {
            // 忽略异常，避免影响主功能
        }
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

