package dev.dong4j.zeka.stack.idea.plugin.terminal.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.ui.awt.RelativePoint;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.view.TerminalLineIndex;
import org.jetbrains.plugins.terminal.view.TerminalOffset;
import org.jetbrains.plugins.terminal.view.TerminalOutputModel;
import org.jetbrains.plugins.terminal.view.TerminalOutputModelSnapshot;
import org.jetbrains.plugins.terminal.view.TerminalOutputModelsSet;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.terminal.context.TerminalContextService;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import icons.TerminalIcons;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
        log.debug("Terminal view: {}, JBTerminalWidget: {}", terminalView != null, jbWidget != null);
        if (terminalView == null && jbWidget == null) {
            log.debug("No terminal found, skipping action");
            NotificationUtil.showWarning(project, TerminalBundle.message("error.terminal.not.found"));
            return;
        }

        InputInfo inputInfo = terminalView != null
                              ? getInputInfo(terminalView, settings)
                              : getInputInfo(jbWidget, settings);
        String input = inputInfo == null ? null : inputInfo.content;
        log.debug("Extracted input: {}, multiLine: {}", input, inputInfo != null && inputInfo.multiLine);
        if (input == null || input.isBlank()) {
            log.debug("Input is empty, skipping AI generation");
            return;
        }

        AIProviderConfig providerConfig = resolveProviderConfig(settings);
        log.debug("Resolved AI provider: {}", providerConfig != null ? providerConfig.providerType : null);
        if (providerConfig == null) {
            log.debug("No AI provider available");
            NotificationUtil.showError(project, TerminalBundle.message("error.no.ai.provider"));
            return;
        }

        showAiProcessingHint(terminalView, jbWidget);

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

                String userContent = input;
                if (settings.enableTerminalContext) {
                    TerminalContextService contextService = project.getService(TerminalContextService.class);
                    userContent = contextService.buildUserPrompt(input, terminalView);
                }
                String userPrompt = settings.terminalTemplate.replace("{content}", userContent);
                log.debug("Built user prompt from template, length: {}", userPrompt.length());
                AIChatRequest request = new AIChatRequest(settings.systemPrompt, userPrompt);
                AIService aiService = com.intellij.openapi.application.ApplicationManager.getApplication().getService(AIService.class);
                try {
                    log.debug("Starting AI content generation");
                    AIConsoleLoggerUtil.printWithTimestamp(project, "=== Terminal AI Request ===");
                    if (settings.enableStreamResponse) {
                        aiService.generateContentStream(project, request, providerConfig, new AIStreamResponseListener() {
                            /** 用于缓存流式响应的文本内容, 累积所有分块数据以生成完整响应 */
                            private final StringBuilder streamBuffer = new StringBuilder();

                            /**
                             * 处理流式响应的片段数据
                             * <p> 将接收到的片段内容追加到内部缓冲区中, 用于后续完整响应的拼接
                             *
                             * @param chunk 当前接收到的响应片段内容
                             */
                            @Override
                            public void onChunk(@NotNull String chunk) {
                                streamBuffer.append(chunk);
                            }

                            /**
                             * 处理 AI 流式响应完成事件
                             * <p> 当 AI 响应完整返回时, 根据是否为空判断使用完整文本或缓冲区内容作为结果, 并调用处理方法
                             *
                             * @param fullText 完整的 AI 响应文本
                             */
                            @Override
                            public void onComplete(@NotNull String fullText) {
                                String result = fullText.isBlank() ? streamBuffer.toString() : fullText;
                                handleAiResult(project, terminalView, jbWidget, inputInfo, result);
                            }

                            /**
                             * 处理 AI 流式响应错误情况
                             * <p> 当 AI 服务发生错误时, 显示失败提示并弹出错误通知
                             *
                             * @param error     错误信息字符串, 用于构建错误提示消息
                             * @param exception 可选的异常对象, 用于记录详细错误堆栈信息
                             */
                            @Override
                            public void onError(@NotNull String error, @Nullable Throwable exception) {
                                showAiFailedHint(terminalView, jbWidget);
                                NotificationUtil.showError(project, TerminalBundle.message("error.ai.failed", error));
                            }
                        });
                        return;
                    }

                    String result = aiService.generateContent(project, request, providerConfig, null);
                    log.debug("AI generation completed, result length: {}", result.length());
                    handleAiResult(project, terminalView, jbWidget, inputInfo, result);
                } catch (AIServiceException ex) {
                    String message = AIServiceException.build(ex);
                    log.debug("AI service exception: {}", message, ex);
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
        SettingsState settings = SettingsState.getInstance();
        if (!settings.enableTerminalAI) {
            e.getPresentation().setEnabled(false);
            return;
        }

        TerminalView terminalView = getTerminalView(e);
        JBTerminalWidget widget = e.getData(JBTerminalWidget.TERMINAL_DATA_KEY);

        String input = terminalView != null
                       ? getCurrentInput(terminalView, settings)
                       : (widget != null ? getCurrentInput(widget, settings) : null);

        boolean enabled = input != null && !input.isBlank();
        log.debug("Action update: enabled={}, input={}", enabled, input != null ? input.substring(0, Math.min(50, input.length())) : null);
        e.getPresentation().setEnabled(enabled);
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
            log.debug("Using terminal-specific provider config: {}", settings.providerConfig.providerType);
            return settings.providerConfig.copy();
        }
        AIProviderSettings global = AIProviderSettings.getInstance();
        List<AIProviderConfig> verified = global.getVerifiedProviders();
        log.debug("Using global provider config, verified providers count: {}", verified.size());
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
            log.debug("Found TerminalView from 'TerminalView' data key");
            return view;
        }
        DataKey<TerminalView> keyLower = DataKey.create("terminalView");
        TerminalView viewLower = e.getData(keyLower);
        log.debug("TerminalView from 'terminalView' data key: {}", viewLower != null);
        return viewLower;
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
        LogicalLine logicalLine = getLastLogicalLine(terminalView);
        String line = logicalLine == null ? null : logicalLine.line;
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
            log.debug("No trigger prefix configured, using entire line");
            return line.strip();
        }
        String extracted = extractPrompt(line, prefix);
        log.debug("Extracted prefix content: prefix='{}', extracted='{}'", prefix, extracted);
        return extracted == null || extracted.isBlank() ? null : extracted;
    }

    /**
     * 获取终端视图的最后一条逻辑输入行
     * <p> 该方法会处理以反斜杠结尾的续行输入, 将多行合并为一行.
     * 从终端输出的最后一行开始向前遍历, 在遇到反斜杠续行时继续向上合并.</p>
     *
     * @param terminalView 终端视图实例, 不能为 null
     * @return 最后一条逻辑输入行, 如果所有行都为空则返回 null
     */
    private static LogicalLine getLastLogicalLine(@NotNull TerminalView terminalView) {
        TerminalOutputModelsSet models = terminalView.getOutputModels();
        TerminalOutputModel regular = models.getRegular();
        TerminalOutputModelSnapshot snapshot = regular.takeSnapshot();
        log.debug("Terminal snapshot line count: {}", snapshot.getLineCount());
        if (snapshot.getLineCount() == 0) {
            return null;
        }
        List<String> lines = snapshotToLines(snapshot);
        LogicalLine logicalLine = getLastLogicalLine(lines);
        log.debug("Last logical line extracted: {}, multiLine: {}",
                  logicalLine != null ? logicalLine.line.substring(0, Math.min(50, logicalLine.line.length())) : null,
                  logicalLine != null && logicalLine.multiLine);
        return logicalLine;
    }

    /**
     * 从终端组件中获取当前用户输入内容
     * <p> 从终端组件中获取文本内容, 提取最后一行非空输入, 并使用设置中的前缀规则进行处理.
     * 如果文本为空或无有效输入, 则返回 null.
     *
     * @param widget   终端组件, 不能为空
     * @param settings 设置状态, 包含触发前缀等配置, 不能为空
     * @return 处理后的用户输入内容, 如果无有效输入则返回 null
     */
    private static String getCurrentInput(@NotNull JBTerminalWidget widget, @NotNull SettingsState settings) {
        String text = widget.getText();
        if (text.isBlank()) {
            return null;
        }
        String[] lines = text.replace("\r", "").split("\n");
        LogicalLine logicalLine = getLastLogicalLine(List.of(lines));
        String line = logicalLine == null ? null : logicalLine.line;
        if (line == null) {
            return null;
        }
        return getPrefix(settings, line);
    }

    /**
     * 从终端视图中提取用户输入信息并封装为 InputInfo 对象
     * <p> 该方法首先获取终端视图中的最后一条逻辑输入行, 若该行为空或不存在则返回 null; 接着根据设置中的前缀规则提取有效输入内容, 若内容为空或无效也返回 null; 否则创建并返回包含提取内容和多行标识的 InputInfo 对象.</p>
     *
     * @param terminalView 终端视图实例, 不能为空
     * @param settings     设置状态对象, 不能为空
     * @return 提取后的输入信息封装对象, 若输入无效则返回 null
     */
    private static InputInfo getInputInfo(@NotNull TerminalView terminalView, @NotNull SettingsState settings) {
        LogicalLine logicalLine = getLastLogicalLine(terminalView);
        return inputInfo(settings, logicalLine);
    }

    /**
     * 从 JBTerminalWidget 组件中提取用户输入信息
     * <p> 该方法从终端组件获取当前文本内容, 按行分割后提取最后一行逻辑输入内容, 再根据设置中的前缀规则提取有效命令内容. 若输入无效或内容为空, 则返回 null.</p>
     *
     * @param widget   终端组件实例, 不能为空
     * @param settings 设置状态对象, 包含触发前缀等配置, 不能为空
     * @return 提取后的输入信息对象, 若输入无效则返回 null
     */
    private static InputInfo getInputInfo(@NotNull JBTerminalWidget widget, @NotNull SettingsState settings) {
        String text = widget.getText();
        if (text.isBlank()) {
            return null;
        }
        String[] lines = text.replace("\r", "").split("\n");
        LogicalLine logicalLine = getLastLogicalLine(List.of(lines));
        return inputInfo(settings, logicalLine);
    }

    /**
     * 从逻辑输入行和设置中提取并封装用户输入信息
     * <p> 首先检查逻辑输入行是否为 null 或内容为空, 若满足则返回 null; 接着根据设置中的前缀规则提取有效内容, 若提取结果为 null 或空白也返回 null; 否则创建并返回包含提取内容和多行标识的 {@link InputInfo} 对象.</p>
     *
     * @param settings    当前设置状态, 不能为空
     * @param logicalLine 待处理的逻辑输入行, 不能为空
     * @return 包含提取内容和多行标识的 {@link InputInfo} 对象, 若输入无效则返回 null
     */
    private static InputInfo inputInfo(@NotNull SettingsState settings, LogicalLine logicalLine) {
        if (logicalLine == null || logicalLine.line.isBlank()) {
            log.debug("Logical line is null or blank");
            return null;
        }
        String content = getPrefix(settings, logicalLine.line);
        if (content == null || content.isBlank()) {
            log.debug("Extracted content is null or blank");
            return null;
        }
        log.debug("Created InputInfo: content length={}, multiLine={}", content.length(), logicalLine.multiLine);
        return new InputInfo(content, logicalLine.multiLine);
    }

    /**
     * 将终端输出快照中的每一行文本提取为字符串列表
     * <p> 该方法从终端输出快照的最后一条行开始, 逐行向上遍历, 提取每行的文本内容, 并将结果列表反转后返回.
     * 适用于从终端输出模型中提取所有可见行内容, 用于后续处理或显示.</p>
     *
     * @param snapshot 终端输出快照对象, 不能为空
     * @return 包含所有行文本的字符串列表, 按从上到下的顺序排列
     */
    private static List<String> snapshotToLines(@NotNull TerminalOutputModelSnapshot snapshot) {
        List<String> lines = new ArrayList<>(snapshot.getLineCount());
        TerminalLineIndex lineIndex = snapshot.getLastLineIndex();
        int remaining = snapshot.getLineCount();
        while (remaining-- > 0) {
            TerminalOffset start = snapshot.getStartOfLine(lineIndex);
            TerminalOffset end = snapshot.getEndOfLine(lineIndex, true);
            CharSequence text = snapshot.getText(start, end);
            lines.add(text.toString());
            if (remaining > 0) {
                lineIndex = lineIndex.minus(1);
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    /**
     * 从字符串列表中获取最后一行逻辑输入行
     * <p> 该方法会处理以反斜杠结尾的续行输入, 将多行合并为一行. 从列表末尾开始向前遍历, 遇到反斜杠续行时继续向上合并.</p>
     *
     * @param lines 字符串列表, 包含终端输入的多行内容, 不能为空
     * @return 最后一条逻辑输入行对象, 如果所有行都为空或无效则返回 null
     */
    @Nullable
    private static LogicalLine getLastLogicalLine(@NotNull List<String> lines) {
        int lastIndex = findLastNonBlankIndex(lines);
        if (lastIndex < 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        String current = normalizeInputLine(lines.get(lastIndex));
        if (!current.isEmpty()) {
            parts.add(current);
        }
        int index = lastIndex;
        boolean multiLine = false;
        while (true) {
            int prevIndex = findPreviousNonBlankIndex(lines, index - 1);
            if (prevIndex < 0) {
                break;
            }
            String prevRaw = lines.get(prevIndex).stripTrailing();
            if (!prevRaw.endsWith("\\")) {
                break;
            }
            String prev = normalizeInputLine(prevRaw);
            if (!prev.isEmpty()) {
                parts.addFirst(prev);
            }
            multiLine = true;
            index = prevIndex;
        }
        if (parts.isEmpty()) {
            return null;
        }
        return new LogicalLine(String.join(" ", parts).strip(), multiLine);
    }

    /**
     * 从字符串列表中查找最后一个非空白行的索引
     * <p> 从列表末尾开始向前遍历, 找到第一个非空白行的索引. 若所有行均为空白, 则返回 - 1.</p>
     *
     * @param lines 字符串列表, 不能为空
     * @return 最后一个非空白行的索引, 若无非空白行则返回 - 1
     */
    private static int findLastNonBlankIndex(@NotNull List<String> lines) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (!lines.get(i).isBlank()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 在指定索引之前查找第一个非空行的索引位置
     * <p> 从 startIndex 开始向前遍历列表, 找到第一个非空行的索引. 如果未找到, 则返回 - 1.</p>
     *
     * @param lines      用于搜索的字符串列表, 不能为空
     * @param startIndex 开始向前搜索的索引位置, 必须大于等于 0
     * @return 第一个非空行的索引, 若不存在则返回 - 1
     */
    private static int findPreviousNonBlankIndex(@NotNull List<String> lines, int startIndex) {
        for (int i = startIndex; i >= 0; i--) {
            if (!lines.get(i).isBlank()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 标准化终端输入行内容
     * <p>该方法首先移除行尾空白字符, 然后根据行首是否包含 shell 提示符 (如 {@code "$"}, {@code ">"}) 进行前缀去除处理. 若行尾以反斜杠 {@code "\\"} 结尾, 则移除该反斜杠并再次移除尾部空白.
     * 最终返回处理后的纯文本内容, 且去除首尾空白.</p>
     *
     * @param line 原始输入行文本(非空)
     * @return 标准化后的文本内容, 若处理后为空则返回空字符串
     */
    @NotNull
    private static String normalizeInputLine(@NotNull String line) {
        String trimmed = line.stripTrailing();
        boolean endsWithSlash = trimmed.endsWith("\\");
        String normalized = stripPromptPrefix(trimmed);
        if (endsWithSlash) {
            normalized = normalized.substring(0, normalized.length() - 1).stripTrailing();
        }
        return normalized.strip();
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
            log.debug("Line does not start with prefix: '{}' vs '{}'", trimmed.substring(0, Math.min(20, trimmed.length())), prefix);
            return null;
        }
        String content = trimmed.substring(prefix.length());
        if (content.startsWith(" ")) {
            content = content.substring(1);
        }
        log.debug("Extracted prompt content: '{}'", content.substring(0, Math.min(50, content.length())));
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
    private static void replaceCurrentLine(@NotNull TerminalView terminalView, @NotNull String newText, boolean multiLine) {
        log.debug("Replacing current line in TerminalView, multiLine: {}, command length: {}", multiLine, newText.length());
        if (multiLine) {
            sendText(terminalView, "\u0003");
            sendText(terminalView, newText);
            return;
        }
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
    private static void replaceCurrentLine(@NotNull JBTerminalWidget widget,
                                           @NotNull String newText,
                                           @NotNull Project project,
                                           boolean multiLine) {
        try {
            if (widget.getTtyConnector() == null || !widget.getTtyConnector().isConnected()) {
                log.debug("Terminal connector not ready");
                NotificationUtil.showWarning(project, TerminalBundle.message("error.terminal.not.ready"));
                return;
            }
            log.debug("Replacing current line in JBTerminalWidget, multiLine: {}, command length: {}", multiLine, newText.length());
            if (multiLine) {
                widget.getTtyConnector().write("\u0003");
                widget.getTtyConnector().write(newText);
                return;
            }
            widget.getTtyConnector().write("\u0015");
            widget.getTtyConnector().write(newText);
        } catch (IOException ex) {
            log.debug("Failed to replace terminal line", ex);
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
                log.debug("Code fence detected: inFence={}", inFence);
                continue;
            }
            if (inFence) {
                continue;
            }
            if (trimmed.isEmpty()) {
                continue;
            }
            candidate = trimmed;
            log.debug("Found candidate command: '{}'", candidate.substring(0, Math.min(50, candidate.length())));
            break;
        }
        String finalCommand = candidate == null ? result.trim() : candidate;
        log.debug("Final extracted command: '{}'", finalCommand.substring(0, Math.min(50, finalCommand.length())));
        return finalCommand;
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
     * 在终端界面或状态栏显示提示信息
     * <p> 该方法在 UI 线程中异步显示一个消息弹窗, 位置优先基于终端组件的右下角, 若组件不可见则显示在焦点中心. 同时, 将消息内容设置到项目状态栏中供用户查看.</p>
     *
     * @param project      当前项目对象, 用于获取状态栏
     * @param terminalView 终端视图实例, 用于获取其组件以定位弹窗位置, 可为 null
     * @param widget       终端组件实例, 用于获取其组件以定位弹窗位置, 可为 null
     * @param message      要显示的提示消息内容, 不能为空
     */
    private static void showTip(@NotNull Project project,
                                @Nullable TerminalView terminalView,
                                @Nullable JBTerminalWidget widget,
                                @NotNull String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            JComponent component = terminalView != null
                                   ? terminalView.getComponent()
                                   : (widget != null ? widget.getComponent() : null);
            if (component != null && component.isShowing()) {
                // 计算右上角位置
                int offsetX = component.getWidth() - 20; // 距离右边缘 20 像素
                int offsetY = 20; // 距离顶部 20 像素
                RelativePoint point = new RelativePoint(component, new Point(offsetX, offsetY));

                JBPopupFactory.getInstance()
                    .createMessage(message)
                    .showInCenterOf(component);
            } else {
                JBPopupFactory.getInstance()
                    .createMessage(message)
                    .showInFocusCenter();
            }

            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(message);
            }
        });
    }

    /**
     * 处理 AI 生成的结果并替换终端当前行内容
     * <p> 该方法接收 AI 服务返回的响应内容, 提取其中的命令并替换终端当前输入行. 若提取的命令为空或无效, 则显示相应错误提示.</p>
     *
     * @param project      当前项目上下文, 用于日志记录和通知显示
     * @param terminalView 终端视图实例, 用于替换当前行内容 (可为 null)
     * @param jbWidget     终端组件实例, 用于替换当前行内容 (可为 null)
     * @param inputInfo    输入信息封装对象, 包含原始输入内容和是否为多行标识
     * @param result       AI 服务返回的完整响应内容
     * @since 1.0.0
     */
    private static void handleAiResult(@NotNull Project project,
                                       @Nullable TerminalView terminalView,
                                       @Nullable JBTerminalWidget jbWidget,
                                       @NotNull InputInfo inputInfo,
                                       @NotNull String result) {
        AIConsoleLoggerUtil.printSuccess(project, "=== Terminal AI Response ===");
        AIConsoleLoggerUtil.print(project, result);

        String command = extractCommand(result);
        log.debug("Extracted command: {}", command);
        if (command.isBlank()) {
            log.debug("Extracted command is blank");
            showTip(project, terminalView, jbWidget, TerminalBundle.message("error.ai.empty"));
            return;
        }
        if (!isValidShellOutput(command)) {
            log.debug("Command validation failed: {}", command);
            showTip(project, terminalView, jbWidget, TerminalBundle.message("error.ai.invalid.output"));
            return;
        }
        log.debug("Replacing current line with command, multiLine: {}", inputInfo.multiLine);
        if (terminalView != null) {
            replaceCurrentLine(terminalView, command, inputInfo.multiLine);
        } else if (jbWidget != null) {
            replaceCurrentLine(jbWidget, command, project, inputInfo.multiLine);
        }
        log.debug("Terminal line replaced successfully");
    }

    /**
     * 显示 AI 处理中的提示信息
     * <p> 在终端视图或终端组件中显示“正在处理中”的提示状态, 用于告知用户 AI 正在生成内容.</p>
     *
     * @param terminalView 终端视图实例, 可为 null
     * @param widget       终端组件实例, 可为 null
     */
    private static void showAiProcessingHint(@Nullable TerminalView terminalView, @Nullable JBTerminalWidget widget) {
        showAiStatusHint(terminalView, widget, TerminalBundle.message("terminal.hint.processing"));
    }

    /**
     * 显示 AI 生成失败的提示信息
     * <p> 该方法用于在终端界面中显示 AI 生成失败的提示, 通过调用 {@link #showAiStatusHint(TerminalView, JBTerminalWidget, String)} 方法, 传入失败提示消息.</p>
     *
     * @param terminalView 终端视图实例, 可为 null
     * @param widget       终端组件实例, 可为 null
     */
    private static void showAiFailedHint(@Nullable TerminalView terminalView, @Nullable JBTerminalWidget widget) {
        showAiStatusHint(terminalView, widget, TerminalBundle.message("terminal.hint.failed"));
    }

    /**
     * 在终端中显示 AI 处理状态提示信息
     * <p> 该方法根据传入的终端视图或终端组件, 向终端发送清除当前行的控制字符 (<code>\u0015</code>), 然后输出指定的提示消息. 若终端视图或组件均不可用, 则不执行任何操作.</p>
     * <p> 提示消息会先经过 ANSI 转义序列去除处理 (<code>stripAnsi</code>), 确保显示内容纯净.</p>
     *
     * @param terminalView 终端视图实例, 若不为 null 则通过其发送清除和提示文本
     * @param widget       终端组件实例, 若不为 null 且连接正常, 则通过其 TTY 连接器发送清除和提示文本
     * @param message      要显示的提示消息内容, 将被清理 ANSI 序列后输出
     */
    private static void showAiStatusHint(@Nullable TerminalView terminalView,
                                         @Nullable JBTerminalWidget widget,
                                         @NotNull String message) {
        String hint = stripAnsi(message);
        if (terminalView != null) {
            sendText(terminalView, "\u0015");
            sendText(terminalView, hint);
            return;
        }
        if (widget != null) {
            try {
                if (widget.getTtyConnector() != null && widget.getTtyConnector().isConnected()) {
                    widget.getTtyConnector().write("\u0015");
                    widget.getTtyConnector().write(hint);
                }
            } catch (IOException ignored) {
                // ignore hint errors
            }
        }
    }

    /**
     * 去除字符串中的 ANSI 转义序列
     * <p>该方法用于从输入字符串中移除所有 ANSI 转义序列(如颜色, 格式化控制码), 仅保留纯文本内容.
     * ANSI 转义序列通常以 ESC 字符 (\u001B) 开头, 后跟方括号内的数字和分号组合, 以字母 'm' 结尾.
     *
     * @param text 输入的字符串, 不能为空
     * @return 去除 ANSI 转义序列后的纯文本字符串
     */
    @NotNull
    private static String stripAnsi(@NotNull String text) {
        return text.replaceAll("\u001B\\[[0-9;]*m", "");
    }

    /**
     * 输入信息封装类
     * <p>用于封装从终端中提取的用户输入内容及其多行标识, 便于后续处理和生成 AI 响应. 该类为不可变数据类, 包含两个核心属性: 内容文本 (content) 和是否为多行输入(multiLine).</p>
     * <p>此类通常用于在终端 AI 生成功能中, 将用户输入内容结构化, 以便在调用 AI 服务前进行预处理或在生成结果后进行内容替换.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private record InputInfo(String content, boolean multiLine) {
        /**
         * 初始化输入信息对象
         * <p> 构造函数用于创建包含内容和是否多行标记的输入信息对象
         *
         * @param content   输入内容, 不能为空
         * @param multiLine 是否为多行内容
         */
        private InputInfo(@NotNull String content, boolean multiLine) {
            this.content = content;
            this.multiLine = multiLine;
        }
    }

    /**
     * 逻辑行数据记录类
     * <p> 用于封装终端中的一行输入内容及其是否为多行输入的标识信息. 该类作为不可变数据记录类 (record), 主要用于在终端输入处理过程中, 将用户输入内容按逻辑行进行封装和传递, 支持处理以反斜杠结尾的续行输入场景.</p>
     * <p> 主要用途是在终端输入解析阶段, 将多行输入合并为一条逻辑行, 并标记是否为多行输入, 以便后续提取 AI 命令内容或替换终端行时正确处理.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.20
     * @since 1.0.0
     */
    private record LogicalLine(String line, boolean multiLine) {
        /**
         * 构造逻辑行对象
         * <p> 初始化逻辑行内容及是否为多行标记
         *
         * @param line      逻辑行内容, 不能为空
         * @param multiLine 是否为多行逻辑行标记
         * @since 1.0
         */
        private LogicalLine(@NotNull String line, boolean multiLine) {
            this.line = line;
            this.multiLine = multiLine;
        }
    }
}
