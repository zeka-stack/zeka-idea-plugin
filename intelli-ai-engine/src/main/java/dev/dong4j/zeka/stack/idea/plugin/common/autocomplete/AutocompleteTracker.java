package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

final class AutocompleteTracker implements Disposable {
    private static final String DEBOUNCE_KEY = "autocomplete";

    private final Project project;
    private final Editor editor;
    private final AutocompleteDebouncer debouncer = new AutocompleteDebouncer();
    private final AutocompleteRequestBuilder requestBuilder = new AutocompleteRequestBuilder();
    private final AutocompleteCompletionDispatcher dispatcher;
    private final AutocompleteSuggestionFactory suggestionFactory = new AutocompleteSuggestionFactory();
    private final AutocompleteSuggestionCache cache = new AutocompleteSuggestionCache();
    private final TriggerEngine triggerEngine;

    private AutocompleteSuggestion currentSuggestion;
    private int lastSuggestionOffset = -1;
    private volatile boolean applyingSuggestion;

    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            if (applyingSuggestion) {
                return;
            }
            rejectSuggestion();
            if (!AutocompleteSettings.getInstance().autoTrigger) {
                return;
            }
            CharSequence newText = event.getNewFragment();
            if (newText.isEmpty()) {
                return;
            }
            if (newText.toString().trim().isEmpty() && !newText.toString().contains("\n")) {
                return;
            }
            scheduleTrigger(AutocompleteTriggerMode.AUTO);
        }
    };

    private final CaretListener caretListener = new CaretListener() {
        @Override
        public void caretPositionChanged(@NotNull CaretEvent event) {
            if (currentSuggestion != null) {
                event.getCaret();
                if (event.getCaret().getOffset() != lastSuggestionOffset) {
                    rejectSuggestion();
                }
            }
        }
    };

    AutocompleteTracker(@NotNull Project project, @NotNull Editor editor) {
        this.project = project;
        this.editor = editor;
        this.dispatcher = new AutocompleteCompletionDispatcher(project);
        this.triggerEngine = new TriggerEngine(List.of(
            new EditorAvailableRule(),
            new MultiCaretRule(),
            new LargeFileRule(),
            new LookupActiveRule(),
            new LiveTemplateRule(),
            new IMEChineseRule()
                                                      ));
        attachListeners();
    }

    void trigger(@NotNull AutocompleteTriggerMode mode) {
        TriggerResult result = ReadAction.compute(() -> buildTriggerResult(mode));
        if (result == null) {
            return;
        }
        AutocompleteCompletionRequest request = result.request;
        int requestOffset = result.requestOffset;
        AIConsoleLoggerUtil.print(project, "Autocomplete 触发: " + mode);
        dispatcher.request(request).thenAccept(response -> {
            if (response == null || response.content().isBlank()) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (editor.isDisposed()) {
                    return;
                }
                if (editor.getCaretModel().getOffset() != requestOffset) {
                    return;
                }
                AutocompleteSuggestion suggestion = suggestionFactory.build(editor, response.content());
                if (suggestion == null) {
                    return;
                }
                if (cache.isDuplicate(response.content())) {
                    return;
                }
                cache.update(response.content());
                renderSuggestion(suggestion);
            });
        });
    }

    private TriggerResult buildTriggerResult(@NotNull AutocompleteTriggerMode mode) {
        if (!AutocompleteSettings.getInstance().enabled) {
            return null;
        }
        Document document = editor.getDocument();
        if (document.isInBulkUpdate()) {
            return null;
        }
        TriggerContext context = new TriggerContext(editor, mode);
        if (!triggerEngine.shouldTrigger(context)) {
            return null;
        }
        AutocompleteCompletionRequest request = requestBuilder.build(editor, mode);
        int requestOffset = editor.getCaretModel().getOffset();
        return new TriggerResult(request, requestOffset);
    }

    void acceptSuggestion() {
        if (currentSuggestion == null) {
            return;
        }
        applyingSuggestion = true;
        try {
            currentSuggestion.accept(editor);
            AIConsoleLoggerUtil.printSuccess(project, "Autocomplete 已采纳");
        } finally {
            applyingSuggestion = false;
            currentSuggestion = null;
        }
    }

    void rejectSuggestion() {
        if (currentSuggestion != null) {
            currentSuggestion.dispose();
            currentSuggestion = null;
            lastSuggestionOffset = -1;
            AIConsoleLoggerUtil.printWarning(project, "Autocomplete 已取消");
        }
    }

    boolean hasSuggestion() {
        return currentSuggestion != null;
    }

    private void renderSuggestion(@NotNull AutocompleteSuggestion suggestion) {
        rejectSuggestion();
        currentSuggestion = suggestion;
        lastSuggestionOffset = suggestion.getStartOffset();
        suggestion.show(editor);
        AIConsoleLoggerUtil.print(project, "Autocomplete 已展示");
    }

    private void scheduleTrigger(@NotNull AutocompleteTriggerMode mode) {
        long delay = AutocompleteSettings.getInstance().debounceMs;
        debouncer.debounce(DEBOUNCE_KEY, delay, () -> trigger(mode));
    }

    private void attachListeners() {
        editor.getDocument().addDocumentListener(documentListener, this);
        editor.getCaretModel().addCaretListener(caretListener);
    }

    @Override
    public void dispose() {
        rejectSuggestion();
        editor.getCaretModel().removeCaretListener(caretListener);
    }

    private record TriggerResult(AutocompleteCompletionRequest request, int requestOffset) {
            private TriggerResult(@NotNull AutocompleteCompletionRequest request, int requestOffset) {
                this.request = request;
                this.requestOffset = requestOffset;
            }
        }
}
