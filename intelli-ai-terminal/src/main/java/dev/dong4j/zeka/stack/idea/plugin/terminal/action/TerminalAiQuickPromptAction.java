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
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.datatransfer.StringSelection;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import dev.dong4j.zeka.stack.idea.plugin.terminal.ai.TerminalAIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.terminal.context.TerminalContextService;
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

    public TerminalAiQuickPromptAction() {
        super(
            TerminalBundle.message("action.terminal.quick.prompt.title"),
            TerminalBundle.message("action.terminal.quick.prompt.description"),
            TerminalIcons.TERMINAL_16
             );
    }

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

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean enabled = e.getProject() != null && SettingsState.getInstance().enableTerminalAI
                          && (TerminalAiGenerateAction.getTerminalView(e) != null
                              || e.getData(JBTerminalWidget.TERMINAL_DATA_KEY) != null);
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private static void showPopup(@NotNull Project project,
                                  @Nullable TerminalView terminalView,
                                  @Nullable JBTerminalWidget widget) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        JBTextField inputField = new JBTextField();
        inputField.getEmptyText().setText(TerminalBundle.message("action.terminal.quick.prompt.placeholder"));

        JBTextArea resultArea = new JBTextArea(4, 70);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setText(TerminalBundle.message("action.terminal.quick.prompt.result.placeholder"));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton generateBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.generate"));
        JButton insertBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.insert"));
        JButton runBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.run"));
        JButton copyBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.copy"));
        JButton moreBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.more"));
        JButton closeBtn = new JButton(TerminalBundle.message("action.terminal.quick.prompt.close"));

        buttons.add(generateBtn);
        buttons.add(insertBtn);
        buttons.add(runBtn);
        buttons.add(copyBtn);
        buttons.add(moreBtn);
        buttons.add(closeBtn);

        root.add(new JBLabel(TerminalBundle.message("action.terminal.quick.prompt.label")), BorderLayout.NORTH);
        root.add(inputField, BorderLayout.CENTER);
        root.add(resultArea, BorderLayout.SOUTH);

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
            resultArea.setText(TerminalBundle.message("action.terminal.quick.prompt.generating"));
            generateCommand(project, terminalView, widget, requirement, new GenerateCallback() {
                @Override
                public void onSuccess(@NotNull String command) {
                    commandRef.set(command);
                    resultArea.setText(command);
                    generateBtn.setEnabled(true);
                }

                @Override
                public void onError(@NotNull String message) {
                    resultArea.setText(message);
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

        copyBtn.addActionListener(ev -> {
            String command = commandRef.get().trim();
            if (command.isEmpty()) {
                NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.command"));
                return;
            }
            CopyPasteManager.getInstance().setContents(new StringSelection(command));
            NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.copied"));
        });

        moreBtn.addActionListener(ev -> {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem insertAtCursor = new JMenuItem(TerminalBundle.message("action.terminal.quick.prompt.more.insert.cursor"));
            JMenuItem insertToNewFile = new JMenuItem(TerminalBundle.message("action.terminal.quick.prompt.more.insert.new.file"));
            JMenuItem applyInEditor = new JMenuItem(TerminalBundle.message("action.terminal.quick.prompt.more.apply.editor"));

            insertAtCursor.addActionListener(ae -> {
                String command = commandRef.get().trim();
                if (command.isEmpty()) {
                    NotificationUtil.showWarning(project, TerminalBundle.message("action.terminal.quick.prompt.no.command"));
                    return;
                }
                if (insertCommandToCurrentEditor(project, command, false)) {
                    NotificationUtil.showInfo(project, TerminalBundle.message("action.terminal.quick.prompt.inserted.editor"));
                }
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

            menu.add(insertAtCursor);
            menu.add(insertToNewFile);
            menu.addSeparator();
            menu.add(applyInEditor);
            menu.show(moreBtn, 0, moreBtn.getHeight());
        });

        closeBtn.addActionListener(ev -> {
            JBPopup popup = popupRef.get();
            if (popup != null) {
                popup.cancel();
            }
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

    private interface GenerateCallback {
        void onSuccess(@NotNull String command);

        void onError(@NotNull String message);
    }

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

    private static void openCommandInNewEditor(@NotNull Project project, @NotNull String command) {
        LightVirtualFile file = new LightVirtualFile("terminal-command.sh", PlainTextFileType.INSTANCE, command + "\n");
        FileEditorManager.getInstance(project).openFile(file, true);
    }
}
