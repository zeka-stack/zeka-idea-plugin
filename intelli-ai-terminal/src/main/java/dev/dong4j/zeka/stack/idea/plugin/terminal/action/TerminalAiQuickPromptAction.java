package dev.dong4j.zeka.stack.idea.plugin.terminal.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.StringSelection;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import dev.dong4j.zeka.stack.idea.plugin.terminal.ai.TerminalAIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.terminal.context.TerminalContextService;
import dev.dong4j.zeka.stack.idea.plugin.terminal.history.TerminalHistoryState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.shell.TerminalShellDetector;
import dev.dong4j.zeka.stack.idea.plugin.terminal.shell.TerminalShellType;
import dev.dong4j.zeka.stack.idea.plugin.terminal.statistics.TerminalStatisticsReporter;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import icons.TerminalIcons;

/**
 * 终端快捷输入框动作
 * <p> 在终端中通过快捷键打开输入弹层, 输入自然语言需求后生成命令并支持插入/运行/复制.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class TerminalAiQuickPromptAction extends com.intellij.openapi.project.DumbAwareAction {
    /** 历史记录时间格式, 用于格式化时间戳为 "yyyy-MM-dd HH:mm:ss" 格式 */
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 构造函数, 创建终端快捷输入框动作实例
     * <p> 初始化动作的标题, 描述和图标 </p>
     */
    public TerminalAiQuickPromptAction() {
        super(
            TerminalBundle.message("action.terminal.quick.prompt.title"),
            TerminalBundle.message("action.terminal.quick.prompt.description"),
            TerminalIcons.TERMINAL_16
             );
    }

    /**
     * 处理终端快捷输入框动作
     * <p> 根据用户触发的动作, 显示一个弹窗, 允许用户输入自然语言需求, 生成命令并支持插入, 运行或复制.</p>
     *
     * @param e 动作事件对象
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
        TerminalView terminalView = TerminalAiGenerateAction.getTerminalView(e);
        JBTerminalWidget widget = e.getData(JBTerminalWidget.TERMINAL_DATA_KEY);
        if (terminalView == null && widget == null) {
            NotificationUtil.showWarning(project, TerminalBundle.message("error.terminal.not.found"));
            return;
        }
        showPopup(project, terminalView, widget);
    }

    /**
     * 更新操作的启用状态
     * <p> 根据当前项目是否存在, 终端 AI 功能是否启用以及是否存在终端视图或终端小部件, 动态设置该操作是否可用.</p>
     *
     * @param e 行动事件对象, 包含与操作相关的上下文信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean enabled = e.getProject() != null && SettingsState.getInstance().enableTerminalAI
                          && (TerminalAiGenerateAction.getTerminalView(e) != null
                              || e.getData(JBTerminalWidget.TERMINAL_DATA_KEY) != null);
        e.getPresentation().setEnabled(enabled);
    }

    /**
     * 获取此操作的更新线程
     * <p>此方法返回 <pre>{@code ActionUpdateThread.BGT}</pre>, 表示此操作的更新将在后台任务线程 (BGT) 上执行. 该线程适用于不直接与 UI 线程交互, 但需要在后台完成更新的场景.</p>
     *
     * @return 用于更新操作的线程类型, 始终为 {@code ActionUpdateThread.BGT}
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 在终端中显示快捷输入弹窗, 支持自然语言输入, 命令生成, 插入, 运行, 复制及历史记录查看
     * <p> 该方法创建一个包含输入框, 结果区域, 操作按钮的弹窗界面, 用户输入自然语言后可生成命令, 支持插入到终端, 运行, 复制或保存到历史记录.</p>
     *
     * @param project      当前项目上下文, 用于显示通知和操作终端
     * @param terminalView 终端视图对象, 用于获取终端组件或判断是否可插入命令
     * @param widget       终端小部件对象, 用于获取终端组件或判断是否可插入命令
     *                     <p> 若 terminalView 和 widget 均为 null, 则弹窗将无法定位锚点, 会显示在焦点中心.</p>
     * @since 1.0
     */
    private static void showPopup(@NotNull Project project,
                                  @Nullable TerminalView terminalView,
                                  @Nullable JBTerminalWidget widget) {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        JBTextField inputField = new JBTextField();
        inputField.getEmptyText().setText(TerminalBundle.message("action.terminal.quick.prompt.placeholder"));

        JBTextArea resultArea = new JBTextArea(4, 70);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        String resultPlaceholder = TerminalBundle.message("action.terminal.quick.prompt.result.placeholder");
        setResultPlaceholder(resultArea, resultPlaceholder);
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBorder(JBUI.Borders.empty(6, 8));
        inputContainer.add(inputField, BorderLayout.CENTER);

        JPanel resultContainer = new JPanel(new BorderLayout());
        resultContainer.setBorder(JBUI.Borders.empty(6, 8));
        JBScrollPane resultScroll = new JBScrollPane(resultArea);
        resultScroll.setBorder(JBUI.Borders.empty());
        resultContainer.add(resultScroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton generateBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.generate"));
        JButton insertBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.insert"));
        JButton runBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.run"));
        JButton moreBtn = new JButton("...");

        buttons.add(generateBtn);
        buttons.add(insertBtn);
        buttons.add(runBtn);
        buttons.add(moreBtn);

        JBLabel titleLabel = new JBLabel(TerminalBundle.message("action.terminal.quick.prompt.label"));
        JPanel titleContainer = new JPanel(new BorderLayout());
        titleContainer.setBorder(JBUI.Borders.empty(0, 8));
        titleContainer.add(titleLabel, BorderLayout.CENTER);
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new javax.swing.BoxLayout(contentPanel, javax.swing.BoxLayout.Y_AXIS));
        contentPanel.add(titleContainer);
        contentPanel.add(inputContainer);
        contentPanel.add(resultContainer);
        root.add(contentPanel, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        wrapper.add(root, BorderLayout.CENTER);
        wrapper.add(buttons, BorderLayout.SOUTH);

        final AtomicReference<String> commandRef = new AtomicReference<>("");
        final AtomicReference<JBPopup> popupRef = new AtomicReference<>();

        Runnable doGenerate = () -> {
            String requirement = inputField.getText().trim();
            if (requirement.isEmpty()) {
                NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.empty"));
                return;
            }
            generateBtn.setEnabled(false);
            setResultPlaceholder(resultArea, TerminalBundle.message("action.terminal.quick.prompt.generating"));
            generateCommand(project, terminalView, widget, requirement, new GenerateCallback() {
                /**
                 * 处理生成操作成功的回调
                 * <p> 当生成操作成功完成时调用, 负责保存历史记录, 更新命令引用, 设置结果区域内容并重新启用生成按钮.
                 *
                 * @param command 生成的命令内容, 不能为空
                 */
                @Override
                public void onSuccess(@NotNull String command) {
                    addHistory(project, requirement, command);
                    commandRef.set(command);
                    setResultContent(resultArea, command);
                    generateBtn.setEnabled(true);
                }

                /**
                 * 处理生成失败时的错误回调
                 * <p> 在生成失败时, 将错误信息设置到结果区域, 并重新启用生成按钮
                 *
                 * @param message 错误信息
                 */
                @Override
                public void onError(@NotNull String message) {
                    setResultContent(resultArea, message);
                    generateBtn.setEnabled(true);
                }
            });
        };

        generateBtn.addActionListener(ev -> doGenerate.run());
        inputField.addActionListener(ev -> doGenerate.run());

        insertBtn.addActionListener(ev -> {
            String command = commandRef.get().trim();
            if (command.isEmpty()) {
                NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.command"));
                return;
            }
            TerminalShellType shellType = TerminalShellDetector.detect(terminalView, widget);
            boolean applied = TerminalAiGenerateAction.applyCommandToTerminal(project, terminalView, widget, command, false, shellType);
            if (applied) {
                NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.inserted"));
                JBPopup popup = popupRef.get();
                if (popup != null) {
                    popup.cancel();
                }
            }
        });

        runBtn.addActionListener(ev -> {
            String command = commandRef.get().trim();
            if (command.isEmpty()) {
                NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.command"));
                return;
            }
            TerminalShellType shellType = TerminalShellDetector.detect(terminalView, widget);
            boolean applied = TerminalAiGenerateAction.applyCommandToTerminal(project, terminalView, widget, command, true, shellType);
            if (applied) {
                NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.executed"));
                JBPopup popup = popupRef.get();
                if (popup != null) {
                    popup.cancel();
                }
            }
        });

        moreBtn.addActionListener(ev -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem copyCommand = new JMenuItem(TerminalBundle.message("action.terminal.quick.prompt.copy"));
            JMenuItem insertToNewFile = new JMenuItem(TerminalBundle.message("action.terminal.quick.prompt.more.insert.new.file"));
            JMenuItem applyInEditor = new JMenuItem(TerminalBundle.message("action.terminal.quick.prompt.more.apply.editor"));
            JMenuItem historyItem = new JMenuItem(TerminalBundle.message("action.terminal.quick.prompt.more.history"));

            copyCommand.addActionListener(ae -> {
                String command = commandRef.get().trim();
                if (command.isEmpty()) {
                    NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.command"));
                    return;
                }
                CopyPasteManager.getInstance().setContents(new StringSelection(command));
                NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.copied"));
            });

            insertToNewFile.addActionListener(ae -> {
                String command = commandRef.get().trim();
                if (command.isEmpty()) {
                    NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.command"));
                    return;
                }
                openCommandInNewEditor(project, command);
                NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.opened.new.file"));
            });
            applyInEditor.addActionListener(ae -> {
                String command = commandRef.get().trim();
                if (command.isEmpty()) {
                    NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.command"));
                    return;
                }
                if (insertCommandToCurrentEditor(project, command, true)) {
                    NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.applied.editor"));
                }
            });
            historyItem.addActionListener(ae -> showHistoryPopup(project));

            menu.add(copyCommand);
            menu.addSeparator();
            menu.add(insertToNewFile);
            menu.add(applyInEditor);
            menu.addSeparator();
            menu.add(historyItem);
            menu.show(moreBtn, 0, moreBtn.getHeight());
        });

        JBPopup popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(wrapper, inputField)
            .setTitle(TerminalBundle.message("action.terminal.quick.prompt.popup.title"))
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(true)
            .setCancelOnOtherWindowOpen(true)
            .createPopup();
        popupRef.set(popup);

        JComponent anchor = terminalView != null
                            ? terminalView.getComponent()
                            : (widget != null ? widget.getComponent() : null);
        if (anchor != null && anchor.isShowing()) {
            popup.showInCenterOf(anchor);
        } else {
            popup.showInFocusCenter();
        }
        inputField.requestFocusInWindow();
    }

    /**
     * 根据给定的需求生成命令
     * <p> 该方法根据用户提供的需求生成相应的命令, 并通过回调函数返回结果或错误信息.
     *
     * @param project      当前项目
     * @param terminalView 终端视图对象, 可以为空
     * @param widget       终端小部件对象, 可以为空
     * @param requirement  用户需求字符串
     * @param callback     回调函数, 用于处理生成的命令或错误信息
     */
    private static void generateCommand(@NotNull Project project,
                                        @Nullable TerminalView terminalView,
                                        @Nullable JBTerminalWidget widget,
                                        @NotNull String requirement,
                                        @NotNull GenerateCallback callback) {
        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerConfig = TerminalAiGenerateAction.resolveProviderConfig(settings);
        if (providerConfig == null) {
            callback.onError(TerminalBundle.message("error.no.ai.provider"));
            return;
        }
        TerminalShellType shellType = TerminalShellDetector.detect(terminalView, widget);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, TerminalBundle.message("action.terminal.progress"), true) {
            /**
             * 执行终端命令生成任务
             * <p> 此方法用于在后台运行 AI 生成终端命令的任务, 包括准备提示信息, 调用 AI 服务并处理响应结果.
             *
             * @param indicator 进度指示器, 用于更新任务状态和显示进度信息
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(TerminalBundle.message("action.terminal.progress.preparing"));

                TerminalContextService contextService = null;
                String userContent = requirement;
                if (settings.enableTerminalContext) {
                    contextService = project.getService(TerminalContextService.class);
                    userContent = contextService.buildUserPrompt(requirement, terminalView);
                }
                String userPrompt = settings.terminalTemplate.replace("{content}", userContent);
                AIChatRequest request = new AIChatRequest(settings.systemPrompt, userPrompt);
                TerminalAIResponseListener listener = new TerminalAIResponseListener(project);
                AIService aiService = ApplicationManager.getApplication().getService(AIService.class);
                long startTimeMs = System.currentTimeMillis();

                try {
                    indicator.setText(TerminalBundle.message("action.terminal.progress.receiving.response"));
                    String result = aiService.generateContent(project, request, providerConfig, listener);
                    String command = TerminalAiGenerateAction.extractExecutableCommand(result, shellType);
                    if (command == null || command.isBlank()) {
                        ApplicationManager.getApplication().invokeLater(
                            () -> callback.onError(TerminalBundle.message("error.ai.invalid.output")));
                        return;
                    }
                    if (contextService != null) {
                        contextService.recordHistory(requirement, command);
                    }
                    long latencyMs = System.currentTimeMillis() - startTimeMs;
                    TerminalStatisticsReporter.reportSuccess(project,
                                                             providerConfig,
                                                             request,
                                                             result,
                                                             latencyMs,
                                                             listener.getPromptTokens(),
                                                             listener.getCompletionTokens(),
                                                             listener.getTotalTokens(),
                                                             StatisticsUserAction.TERMINAL_SHORTCUT);
                    ApplicationManager.getApplication().invokeLater(() -> callback.onSuccess(command));
                } catch (AIServiceException ex) {
                    String error = AIServiceException.build(ex);
                    ApplicationManager.getApplication().invokeLater(() -> callback.onError(error));
                }
            }
        });
    }

    /**
     * 回调接口, 用于处理生成命令的结果
     * <p> 该接口定义了在生成终端命令成功或失败时的回调方法, 供调用方实现具体逻辑.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.02.10
     * @since 1.0.0
     */
    private interface GenerateCallback {
        /**
         * 在操作成功时调用的回调方法
         * <p> 当某个操作成功完成时, 通过此方法通知调用者, 并传递相关命令信息
         *
         * @param command 操作成功的命令或结果信息
         */
        void onSuccess(@NotNull String command);

        /**
         * 错误回调方法
         * <p> 当操作失败时调用此方法, 传入错误信息
         *
         * @param message 错误信息
         */
        void onError(@NotNull String message);
    }

    /**
     * 将命令插入到当前编辑器中
     * <p> 在当前选中的文本编辑器中, 根据指定参数将命令插入或替换选中内容. 若编辑器未选中或已销毁, 则显示警告并返回 false.</p>
     *
     * @param project          当前项目对象, 用于获取编辑器服务和执行写入操作
     * @param command          要插入的命令字符串
     * @param replaceSelection 是否替换当前选中内容, 若为 true 且存在选中内容, 则替换; 否则在光标位置插入
     * @return 如果成功插入或替换命令, 则返回 true; 否则返回 false
     */
    private static boolean insertCommandToCurrentEditor(@NotNull Project project,
                                                        @NotNull String command,
                                                        boolean replaceSelection) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null || editor.isDisposed()) {
            NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.editor"));
            return false;
        }
        Document document = editor.getDocument();
        WriteCommandAction.runWriteCommandAction(project, () -> {
            int startOffset = editor.getSelectionModel().getSelectionStart();
            int endOffset = editor.getSelectionModel().getSelectionEnd();
            int caretOffset = editor.getCaretModel().getOffset();
            if (replaceSelection && editor.getSelectionModel().hasSelection()) {
                document.replaceString(startOffset, endOffset, command);
                editor.getCaretModel().moveToOffset(startOffset + command.length());
                editor.getSelectionModel().removeSelection();
                return;
            }
            document.insertString(caretOffset, command);
            editor.getCaretModel().moveToOffset(caretOffset + command.length());
        });
        return true;
    }

    /**
     * 在新的编辑器窗口中打开命令文件
     * <p> 创建一个虚拟文件并将指定的命令内容写入该文件, 然后在新的编辑器窗口中打开该文件
     *
     * @param project 项目对象
     * @param command 要打开的命令内容
     */
    private static void openCommandInNewEditor(@NotNull Project project, @NotNull String command) {
        LightVirtualFile file = new LightVirtualFile("terminal-command.sh", PlainTextFileType.INSTANCE, command + "\n");
        FileEditorManager.getInstance(project).openFile(file, true);
    }

    /**
     * 设置结果文本区域的占位符内容和前景色
     * <p> 将指定文本设置为结果文本区域的占位符内容, 并将前景色设置为上下文帮助颜色, 用于提示用户输入内容 </p>
     *
     * @param resultArea 结果文本区域, 用于显示占位符内容
     * @param text       占位符文本内容
     */
    private static void setResultPlaceholder(@NotNull JBTextArea resultArea, @NotNull String text) {
        resultArea.setForeground(UIUtil.getContextHelpForeground());
        resultArea.setText(text);
    }

    /**
     * 设置结果区域的内容文本
     * <p> 将指定的文本内容设置到结果区域, 并恢复正常的文本前景色.
     *
     * @param resultArea 结果文本区域组件
     * @param text       要设置的文本内容
     */
    private static void setResultContent(@NotNull JBTextArea resultArea, @NotNull String text) {
        resultArea.setForeground(UIUtil.getLabelForeground());
        resultArea.setText(text);
    }

    /**
     * 将给定的提示语和回答记录到终端历史.
     *
     * <p> 此方法会使用 {@link java.time.format.DateTimeFormatter} 格式化当前时间戳, 并将提示语, 回答以及时间戳存入 {@link TerminalHistoryState}.</p>
     *
     * @param project 当前项目实例
     * @param prompt  记录的提示语
     * @param answer  记录的回答内容
     */
    private static void addHistory(@NotNull Project project, @NotNull String prompt, @NotNull String answer) {
        TerminalHistoryState state = TerminalHistoryState.getInstance(project);
        state.add(java.time.LocalDateTime.now().format(HISTORY_TIME_FORMAT), prompt, answer);
    }

    /**
     * 显示历史记录弹窗
     * <p> 从项目的历史记录状态中获取历史记录条目, 并显示在一个弹窗中, 用户可以选择条目并查看详细答案.
     *
     * @param project 项目对象
     */
    private static void showHistoryPopup(@NotNull Project project) {
        TerminalHistoryState state = TerminalHistoryState.getInstance(project);
        List<TerminalHistoryState.HistoryEntry> persisted = state.snapshot();
        List<HistoryEntry> snapshot = new ArrayList<>(persisted.size());
        for (TerminalHistoryState.HistoryEntry entry : persisted) {
            snapshot.add(new HistoryEntry(entry.timestamp, entry.prompt, entry.answer));
        }
        if (snapshot.isEmpty()) {
            NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.history.empty"));
            return;
        }
        // 按时间倒序展示
        java.util.Collections.reverse(snapshot);
        JList<HistoryEntry> historyList = new JList<>(snapshot.toArray(new HistoryEntry[0]));
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            String text = String.format("%-19s | %s", value.timestamp(), value.prompt());
            JBLabel label = new JBLabel(text);
            label.setBorder(JBUI.Borders.empty(4, 8));
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, label.getFont().getSize()));
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(UIUtil.getListSelectionBackground(true));
                label.setForeground(UIUtil.getListSelectionForeground(true));
            } else {
                label.setOpaque(true);
                label.setBackground(UIUtil.getListBackground());
                label.setForeground(UIUtil.getLabelForeground());
            }
            return label;
        });
        if (!snapshot.isEmpty()) {
            historyList.setSelectedIndex(0);
        }
        JBScrollPane listScroll = new JBScrollPane(historyList);
        listScroll.setPreferredSize(JBUI.size(400, 260));

        JBTextArea answerArea = new JBTextArea();
        answerArea.setEditable(false);
        answerArea.setLineWrap(true);
        answerArea.setWrapStyleWord(true);
        answerArea.setBorder(JBUI.Borders.empty(8, 10));
        if (!snapshot.isEmpty()) {
            answerArea.setText(snapshot.getFirst().answer());
        }
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                HistoryEntry selected = historyList.getSelectedValue();
                answerArea.setText(selected == null ? "" : selected.answer());
            }
        });
        JBScrollPane answerScroll = new JBScrollPane(answerArea);
        answerScroll.setPreferredSize(JBUI.size(400, 150));

        JButton copySelected = new JButton(TerminalBundle.message("action.terminal.quick.prompt.history.copy.selected"));
        copySelected.addActionListener(ev -> {
            HistoryEntry selected = historyList.getSelectedValue();
            if (selected == null) {
                NotificationUtil.showWarning(project,
                                             TerminalBundle.message("action.terminal.quick.prompt.history.no.selection"));
                return;
            }
            if (!selected.answer().isBlank()) {
                CopyPasteManager.getInstance().setContents(new StringSelection(selected.answer()));
                NotificationUtil.showInfo(project,
                                          TerminalBundle.message("action.terminal.quick.prompt.history.copied.selected"));
            }
        });

        JPanel historyPanel = new JPanel(new BorderLayout(8, 8));
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(listScroll, BorderLayout.CENTER);
        center.add(answerScroll, BorderLayout.SOUTH);
        historyPanel.add(center, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        footer.add(copySelected);
        historyPanel.add(footer, BorderLayout.SOUTH);

        JBPopup historyPopup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(historyPanel, historyList)
            .setTitle(TerminalBundle.message("action.terminal.quick.prompt.history.title"))
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelOnClickOutside(false)
            .setCancelOnOtherWindowOpen(false)
            .createPopup();
        historyPopup.showInFocusCenter();
    }

    /**
     * 终端历史记录条目数据类
     * <p> 用于存储终端快捷输入框生成的历史记录, 包含时间戳, 用户输入提示和 AI 生成的命令结果
     *
     * @param timestamp 记录时间戳, 格式为 yyyy-MM-dd HH:mm:ss
     * @param prompt    用户输入的自然语言需求描述
     * @param answer    AI 生成的终端命令
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.02.10
     * @since x.x.x
     */
    private record HistoryEntry(@NotNull String timestamp,
                                @NotNull String prompt,
                                @NotNull String answer) {
    }
}
