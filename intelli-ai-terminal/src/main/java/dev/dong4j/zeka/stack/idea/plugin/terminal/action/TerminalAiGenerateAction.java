package dev.dong4j.zeka.stack.idea.plugin.terminal.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.frontend.view.TerminalView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.view.TerminalLineIndex;
import org.jetbrains.plugins.terminal.view.TerminalOffset;
import org.jetbrains.plugins.terminal.view.TerminalOutputModel;
import org.jetbrains.plugins.terminal.view.TerminalOutputModelSnapshot;
import org.jetbrains.plugins.terminal.view.TerminalOutputModelsSet;

import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import icons.TerminalIcons;

/**
 * 终端 AI 生成动作类
 * <p> 该类用于在 IDEA 终端中执行 AI 生成命令的功能, 支持从终端输入中提取内容, 并通过指定的 AI 服务生成响应. 生成的结果会替换当前终端行, 供用户直接使用.
 * <p> 主要功能包括: 检查 AI 功能是否启用, 获取终端视图或 JBTerminalWidget 实例, 提取用户输入内容, 调用 AI 服务生成内容, 验证输出格式并更新终端内容等.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
@SuppressWarnings("UnstableApiUsage")
public class TerminalAiGenerateAction extends com.intellij.openapi.project.DumbAwareAction {

    /**
     * 构造函数, 用于创建 TerminalAiGenerateAction 实例
     * <p> 初始化终端 AI 生成操作的标题, 描述和图标
     */
    public TerminalAiGenerateAction() {
        super(
            TerminalBundle.message("action.terminal.title"),
            TerminalBundle.message("action.terminal.description"),
            TerminalIcons.TERMINAL_16
             );
    }

    /**
     * 执行终端 AI 生成动作, 根据当前终端输入内容调用 AI 服务生成命令并替换当前行
     * <p> 该方法会在终端中获取用户输入内容, 通过 AI 服务生成对应的命令, 并将结果替换到终端当前行.
     * 如果 AI 服务不可用或输入为空, 则提示相应错误信息.
     *
     * @param e 表示动作事件的对象, 包含项目, 终端视图等上下文信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        SettingsState settings = SettingsState.getInstance();
        if (!settings.enableTerminalAI) {
            NotificationUtil.showWarning(project, TerminalBundle.message("error.terminal.disabled"));
            return;
        }

        TerminalView terminalView = getTerminalView(e);
        JBTerminalWidget jbWidget = e.getData(JBTerminalWidget.TERMINAL_DATA_KEY);
        if (terminalView == null && jbWidget == null) {
            NotificationUtil.showWarning(project, TerminalBundle.message("error.terminal.not.found"));
            return;
        }

        String input = terminalView != null
                       ? getCurrentInput(terminalView, settings)
                       : getCurrentInput(jbWidget, settings);
        if (input == null || input.isBlank()) {
            return;
        }

        AIProviderConfig providerConfig = resolveProviderConfig(settings);
        if (providerConfig == null) {
            NotificationUtil.showError(project, TerminalBundle.message("error.no.ai.provider"));
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(
            project,
            TerminalBundle.message("action.terminal.progress"),
            true
        ) {
            /**
             * 执行 AI 生成并替换终端命令行内容
             * <p> 该方法设置进度提示, 构建用户指令, 并通过 AI 服务生成响应结果.
             * 随后提取命令并替换当前终端输入行. 若生成失败或输出无效, 将显示错误提示.</p>
             *
             * @param indicator 进度指示器, 用于展示执行状态
             *
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(TerminalBundle.message("action.terminal.progress"));

                String userPrompt = settings.terminalTemplate.replace("{content}", input);
                AIChatRequest request = new AIChatRequest(settings.systemPrompt, userPrompt);
                AIService aiService = com.intellij.openapi.application.ApplicationManager.getApplication().getService(AIService.class);
                try {
                    AIConsoleLoggerUtil.printWithTimestamp(project, "=== Terminal AI Request ===");
                    String result = aiService.generateContent(project, request, providerConfig, null);
                    AIConsoleLoggerUtil.printSuccess(project, "=== Terminal AI Response ===");
                    AIConsoleLoggerUtil.print(project, result);

                    String command = extractCommand(result);
                    if (command.isBlank()) {
                        showTip(terminalView, jbWidget, TerminalBundle.message("error.ai.empty"));
                        return;
                    }
                    if (!isValidShellOutput(command)) {
                        showTip(terminalView, jbWidget, TerminalBundle.message("error.ai.invalid.output"));
                        return;
                    }
                    if (terminalView != null) {
                        replaceCurrentLine(terminalView, command);
                    } else {
                        replaceCurrentLine(jbWidget, command, project);
                    }
                } catch (AIServiceException ex) {
                    String message = AIServiceException.build(ex);
                    AIConsoleLoggerUtil.printError(project, message);
                    NotificationUtil.showError(project, TerminalBundle.message("error.ai.failed", message));
                }
            }
        });
    }

    /**
     * 更新动作的可用状态, 根据终端是否存在以及输入内容是否有效来决定是否启用该动作
     * <p> 此方法用于在用户界面中动态更新该动作的可用性. 如果终端未启用 AI 功能或没有有效的输入内容, 则禁用该动作.
     *
     * @param e 代表当前动作事件, 包含项目信息和终端视图数据
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        TerminalView terminalView = getTerminalView(e);
        JBTerminalWidget widget = e.getData(JBTerminalWidget.TERMINAL_DATA_KEY);
        SettingsState settings = SettingsState.getInstance();
        if (!settings.enableTerminalAI) {
            e.getPresentation().setEnabled(false);
            return;
        }
        String input = terminalView != null
                       ? getCurrentInput(terminalView, settings)
                       : (widget != null ? getCurrentInput(widget, settings) : null);
        e.getPresentation().setEnabled(input != null && !input.isBlank());
    }

    /**
     * 返回操作更新线程
     * <p> 此方法返回一个后台线程 (BGT), 用于在后台执行操作更新.
     *
     * @return 操作更新线程, 固定返回 ActionUpdateThread.BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 解析并返回 AI 提供商配置
     * <p> 根据设置中的提供商配置解析并返回 AI 提供商配置对象. 如果设置了具体的提供商配置, 则直接返回其副本;
     * 否则, 从全局 AI 提供商设置中获取已验证的提供商列表的第一个配置并返回其副本.
     *
     * @param settings 当前设置状态
     * @return AIProviderConfig 对象的副本, 如果没有可用的提供商配置则返回 null
     */
    private static AIProviderConfig resolveProviderConfig(@NotNull SettingsState settings) {
        if (settings.providerConfig != null) {
            return settings.providerConfig.copy();
        }
        AIProviderSettings global = AIProviderSettings.getInstance();
        List<AIProviderConfig> verified = global.getVerifiedProviders();
        if (verified.isEmpty()) {
            return null;
        }
        return verified.getFirst().copy();
    }

    /**
     * 从动作事件中获取终端视图实例
     * <p> 优先从数据键 "TerminalView" 获取终端视图, 若不存在则尝试从数据键 "terminalView" 获取 </p>
     *
     * @param e 动作事件对象, 用于获取数据上下文
     * @return 终端视图实例, 若未找到则返回 null
     */
    private static TerminalView getTerminalView(@NotNull AnActionEvent e) {
        DataKey<TerminalView> key = DataKey.create("TerminalView");
        TerminalView view = e.getData(key);
        if (view != null) {
            return view;
        }
        DataKey<TerminalView> keyLower = DataKey.create("terminalView");
        return e.getData(keyLower);
    }

    /**
     * 根据终端视图和设置状态获取当前输入内容
     * <p> 从终端视图中获取最后一行文本, 若该行非空, 则根据设置前缀提取有效输入内容; 若无有效输入则返回 null
     *
     * @param terminalView 终端视图对象, 用于获取终端输出内容
     * @param settings     设置状态对象, 包含触发前缀等配置
     * @return 提取后的有效输入内容, 若无有效输入则返回 null
     */
    private static String getCurrentInput(@NotNull TerminalView terminalView, @NotNull SettingsState settings) {
        String line = getLastLine(terminalView);
        if (line == null || line.isBlank()) {
            return null;
        }
        return getPrefix(settings, line);
    }

    /**
     * 从终端输入行中提取触发前缀后的内容
     * <p> 根据配置的前缀触发词, 从当前行中提取 AI 命令的内容部分.
     * 如果未配置前缀触发词, 则直接返回去除首尾空白的原始行内容.
     *
     * @param settings 设置状态对象, 包含触发前缀配置
     * @param line     终端当前输入行
     * @return 提取后的命令内容, 如果内容为空或仅包含空白字符则返回 null
     */
    @Nullable
    private static String getPrefix(@NotNull SettingsState settings, String line) {
        String prefix = settings.triggerPrefix == null ? "" : settings.triggerPrefix.trim();
        if (prefix.isEmpty()) {
            return line.strip();
        }
        String extracted = extractPrompt(line, prefix);
        return extracted == null || extracted.isBlank() ? null : extracted;
    }

    /**
     * 获取终端视图的最后一行非空内容
     * <p> 从终端输出的最后一行开始向前遍历, 查找并返回第一个非空行内容.
     * 如果所有行都为空, 则返回 null</p>
     *
     * @param terminalView 终端视图实例, 不能为 null
     * @return 最后一行非空内容, 如果所有行都为空则返回 null
     */
    private static String getLastLine(@NotNull TerminalView terminalView) {
        TerminalOutputModelsSet models = terminalView.getOutputModels();
        TerminalOutputModel regular = models.getRegular();
        TerminalOutputModelSnapshot snapshot = regular.takeSnapshot();
        if (snapshot.getLineCount() == 0) {
            return null;
        }
        TerminalLineIndex lineIndex = snapshot.getLastLineIndex();
        int remaining = snapshot.getLineCount();
        while (remaining-- > 0) {
            TerminalOffset start = snapshot.getStartOfLine(lineIndex);
            TerminalOffset end = snapshot.getEndOfLine(lineIndex, true);
            CharSequence text = snapshot.getText(start, end);
            String line = text.toString().stripTrailing();
            if (!line.isEmpty()) {
                return line;
            }
            lineIndex = lineIndex.minus(1);
        }
        return null;
    }

    /**
     * 根据 JBTerminalWidget 和设置状态获取当前终端输入内容
     * <p> 从 JBTerminalWidget 的文本内容中提取最后一行非空行, 并根据设置中的触发前缀提取有效命令内容. 若无有效输入则返回 null</p>
     *
     * @param widget   JBTerminalWidget 实例, 用于获取终端文本内容
     * @param settings 设置状态对象, 包含触发前缀等配置
     * @return 提取后的有效输入内容, 若无有效输入则返回 null
     */
    private static String getCurrentInput(@NotNull JBTerminalWidget widget, @NotNull SettingsState settings) {
        String text = widget.getText();
        if (text.isBlank()) {
            return null;
        }
        String[] lines = text.replace("\r", "").split("\n");
        String line = null;
        for (int i = lines.length - 1; i >= 0; i--) {
            String l = lines[i].stripTrailing();
            if (!l.isEmpty()) {
                line = l;
                break;
            }
        }
        if (line == null) {
            return null;
        }
        return getPrefix(settings, line);
    }

    /**
     * 从当前输入行中提取 AI 命令内容
     * <p> 该方法首先去除行首空白字符, 然后检查是否以指定前缀开头. 若不匹配前缀, 则返回 null; 若匹配, 则提取前缀后的剩余内容, 并移除首部空格后返回处理后的命令内容.</p>
     *
     * @param currentLine 当前输入行文本 (非空)
     * @param prefix      指定的触发前缀 (非空)
     * @return 提取后的命令内容, 若不匹配前缀或内容为空则返回 null
     */
    private static String extractPrompt(@NotNull String currentLine, @NotNull String prefix) {
        String trimmed = stripPromptPrefix(currentLine.stripLeading());
        if (!trimmed.startsWith(prefix)) {
            return null;
        }
        String content = trimmed.substring(prefix.length());
        if (content.startsWith(" ")) {
            content = content.substring(1);
        }
        return content.strip();
    }

    /**
     * 去除字符串行首的 shell 提示符 (如 {@code "$"},{@code "$"},{@code ">"},{@code ">"}) 并返回处理后的结果.
     *
     * <p>该方法首先移除 {@code line} 前导的空白字符, 然后根据首字符判断是否包含上述提示符, 并相应地删除.
     * 如果行首不匹配任何提示符, 则保持字符串不变, 只去除了前导空白.
     *
     * @param line 原始行文本(不为 {@code null})
     * @return 去除提示符及前导空白后的纯文本; 若处理后为空字符串则返回空字符串
     */
    private static String stripPromptPrefix(@NotNull String line) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("$ ")) {
            return trimmed.substring(2);
        }
        if (trimmed.startsWith("$")) {
            return trimmed.substring(1).stripLeading();
        }
        if (trimmed.startsWith("> ")) {
            return trimmed.substring(2);
        }
        if (trimmed.startsWith(">")) {
            return trimmed.substring(1).stripLeading();
        }
        return trimmed;
    }

    /**
     * 将终端当前行替换为指定文本.
     * <p> 该方法先发送 ctrl‑U(`\u0015`) 控制字符以清除当前行,
     * 再将 {@code newText} 写入终端, 相当于覆盖当前行的内容.
     *
     * @param terminalView 需要操作的 {@link TerminalView} 实例
     * @param newText      用于替换当前行的文本
     */
    private static void replaceCurrentLine(@NotNull TerminalView terminalView, @NotNull String newText) {
        sendText(terminalView, "\u0015");
        sendText(terminalView, newText);
    }

    /**
     * 使用新文本替换终端组件中的当前行
     * <p> 该方法首先检查终端连接器是否已连接并准备就绪, 如果是, 则发送擦除行字符并写入新文本.
     *
     * @param widget  终端组件 {@link JBTerminalWidget}
     * @param newText 要写入的新文本内容
     * @param project 当前项目上下文, 用于显示错误通知
     */
    private static void replaceCurrentLine(@NotNull JBTerminalWidget widget, @NotNull String newText, @NotNull Project project) {
        try {
            if (widget.getTtyConnector() == null || !widget.getTtyConnector().isConnected()) {
                NotificationUtil.showWarning(project, TerminalBundle.message("error.terminal.not.ready"));
                return;
            }
            widget.getTtyConnector().write("\u0015");
            widget.getTtyConnector().write(newText);
        } catch (IOException ex) {
            NotificationUtil.showError(project, TerminalBundle.message("error.terminal.write.failed", ex.getMessage()));
        }
    }

    /**
     * 向终端视图发送指定文本
     *
     * @param terminalView 终端视图实例, 用于接收文本
     * @param text         要发送的文本内容
     */
    private static void sendText(@NotNull TerminalView terminalView, @NotNull String text) {
        terminalView.sendText(text);
    }

    /**
     * 从 AI 响应文本中提取第一行有效命令
     * <p>遍历响应文本的每一行, 跳过代码块 (以 ``` 开头的行) 和空行, 找到第一个非空且不在代码块内的行作为候选命令, 若未找到则返回整个文本的首行(去除首尾空格)</p>
     *
     * @param result AI 响应文本内容
     * @return 提取到的第一行有效命令, 若未找到则返回原始文本的首行(去除首尾空格)
     */
    private static String extractCommand(@NotNull String result) {
        String[] lines = result.replace("\r", "").split("\n");
        String candidate = null;
        boolean inFence = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                inFence = !inFence;
                continue;
            }
            if (inFence) {
                continue;
            }
            if (trimmed.isEmpty()) {
                continue;
            }
            candidate = trimmed;
            break;
        }
        return candidate == null ? result.trim() : candidate;
    }

    /**
     * 验证给定的字符串是否为有效的 Shell 命令输出格式
     * <p>该方法检查字符串是否满足以下条件:
     * <ul>
     *   <li>以字母, 数字或特殊字符 (如 /, ., $) 开头</li>
     *   <li>不包含代码块标记(即 ``` 字符串)</li>
     *   <li>不包含连续换行(\n\n)</li>
     * </ul>
     *
     * @param output 要验证的输出字符串
     * @return 如果字符串是合法的 Shell 输出格式, 则返回 true; 否则返回 false
     */
    private static boolean isValidShellOutput(@NotNull String output) {
        return output.matches("^[a-zA-Z0-9/.$].*")
               && !output.contains("```")
               && !output.contains("\n\n");
    }

    /**
     * 在指定的组件上显示提示信息
     * <p> 根据传入的终端视图或 JBTerminalWidget 组件, 选择合适的位置显示提示信息.
     * 如果组件不可见, 则在焦点中心显示.
     *
     * @param terminalView 终端视图对象, 可以为 null
     * @param widget       JBTerminalWidget 对象, 可以为 null
     * @param message      要显示的提示信息
     */
    private static void showTip(@Nullable TerminalView terminalView,
                                @Nullable JBTerminalWidget widget,
                                @NotNull String message) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            JComponent component = terminalView != null
                                   ? terminalView.getComponent()
                                   : (widget != null ? widget.getComponent() : null);
            if (component != null && component.isShowing()) {
                JBPopupFactory.getInstance()
                    .createMessage(message)
                    .showInCenterOf(component);
            } else {
                JBPopupFactory.getInstance()
                    .createMessage(message)
                    .showInFocusCenter();
            }
        });
    }
}
