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

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

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
    private TabHintManager tabHintManager;
    private int lastSuggestionOffset = -1;
    private volatile boolean applyingSuggestion;
    private String lastDocumentText;
    private EditRecord lastEdit;
    private final Queue<NextEditAutocompletion> suggestionQueue = new ArrayDeque<>();

    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            if (applyingSuggestion) {
                return;
            }
            rejectSuggestion();
            trackEdit(event);
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
        this.lastDocumentText = editor.getDocument().getText();
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
        EditorState requestState = result.requestState;
        AIConsoleLoggerUtil.print(project, "Autocomplete 触发: " + mode);
        dispatcher.request(request).thenAccept(response -> {
            if (response == null || response.completions().isEmpty()) {
                return;
            }
            ApplicationManager.getApplication().invokeLater(() -> {
                if (editor.isDisposed()) {
                    return;
                }
                if (!isSameState(requestState)) {
                    return;
                }
                suggestionQueue.clear();
                for (NextEditAutocompletion completion : response.completions()) {
                    if (shouldFilterCompletion(completion)) {
                        AIConsoleLoggerUtil.printWarning(project, "Autocomplete 忽略: 与最近编辑重叠 start="
                                                                  + completion.getStartIndex() + ", end=" + completion.getEndIndex());
                        continue;
                    }
                    suggestionQueue.add(completion);
                }
                if (suggestionQueue.isEmpty()) {
                    AIConsoleLoggerUtil.printWarning(project, "Autocomplete 无有效建议");
                }
                showNextSuggestion(false);
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
        if (lastEdit == null) {
            return null;
        }
        AutocompleteCompletionRequest request = requestBuilder.build(editor, mode, lastEdit);
        EditorState state = new EditorState(editor.getCaretModel().getOffset(), document.getText());
        return new TriggerResult(request, state);
    }

    void acceptSuggestion() {
        if (currentSuggestion == null) {
            return;
        }
        applyingSuggestion = true;
        try {
            AutocompleteSuggestion suggestion = currentSuggestion;
            currentSuggestion = null;
            suggestion.accept(editor);
            AIConsoleLoggerUtil.printSuccess(project, "Autocomplete 已采纳");
            if (suggestion instanceof JumpToEditSuggestion jumpToEditSuggestion) {
                AutocompleteSuggestion postJump = suggestionFactory.build(editor, jumpToEditSuggestion.getOriginalCompletion());
                if (postJump != null) {
                    renderSuggestion(postJump, true);
                }
            } else {
                adjustQueueOffsets(suggestion);
                showNextSuggestion(false);
            }
        } finally {
            applyingSuggestion = false;
        }
    }

    void rejectSuggestion() {
        if (currentSuggestion != null) {
            currentSuggestion.dispose();
            currentSuggestion = null;
            lastSuggestionOffset = -1;
            if (tabHintManager != null) {
                tabHintManager.dispose();
                tabHintManager = null;
            }
            AIConsoleLoggerUtil.printWarning(project, "Autocomplete 已取消");
        }
    }

    boolean hasSuggestion() {
        return currentSuggestion != null;
    }

    private void renderSuggestion(@NotNull AutocompleteSuggestion suggestion) {
        renderSuggestion(suggestion, false);
    }

    private void renderSuggestion(@NotNull AutocompleteSuggestion suggestion, boolean isPostJumpSuggestion) {
        rejectSuggestion();
        String key = suggestion.rejectionCacheKey();
        if (cache.isDuplicate(key)) {
            suggestion.dispose();
            return;
        }
        currentSuggestion = suggestion;
        lastSuggestionOffset = suggestion.getStartOffset();
        suggestion.show(editor, isPostJumpSuggestion);
        int lineNumber = editor.getDocument().getLineNumber(editor.getCaretModel().getOffset());
        int lineEndOffset = editor.getDocument().getLineEndOffset(lineNumber);
        tabHintManager = new TabHintManager(editor, lineEndOffset, suggestion);
        tabHintManager.show();
        cache.update(key);
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

    private void trackEdit(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        if (lastDocumentText == null) {
            lastDocumentText = document.getText();
            return;
        }
        String oldText = event.getOldFragment().toString();
        String newText = event.getNewFragment().toString();
        int startOffset = event.getOffset();
        int endOffset = startOffset + event.getNewLength();
        lastEdit = new EditRecord(startOffset, endOffset, oldText, newText, System.currentTimeMillis());
        lastDocumentText = document.getText();
    }

    private void showNextSuggestion(boolean isPostJumpSuggestion) {
        NextEditAutocompletion next = suggestionQueue.poll();
        if (next == null) {
            return;
        }
        AIConsoleLoggerUtil.print(project, "Autocomplete 处理建议: start=" + next.getStartIndex()
                                           + ", end=" + next.getEndIndex()
                                           + ", confidence=" + next.getConfidence()
                                           + ", id=" + next.getAutocompleteId());
        AutocompleteSuggestion suggestion = suggestionFactory.build(editor, next);
        if (suggestion == null) {
            AIConsoleLoggerUtil.printWarning(project, "Autocomplete 建议被丢弃: suggestion=null");
            showNextSuggestion(isPostJumpSuggestion);
            return;
        }
        renderSuggestion(suggestion, isPostJumpSuggestion);
    }

    private boolean isSameState(@NotNull EditorState state) {
        Document document = editor.getDocument();
        return editor.getCaretModel().getOffset() == state.caretOffset && document.getText().equals(state.documentText);
    }

    private void adjustQueueOffsets(@NotNull AutocompleteSuggestion suggestion) {
        int delta = 0;
        int changeEnd = suggestion.getEndOffset();
        if (suggestion instanceof GhostTextSuggestion) {
            delta = suggestion.getContent().length();
        } else if (suggestion instanceof PopupSuggestion popupSuggestion) {
            delta = suggestion.getContent().length() - (popupSuggestion.getEndOffset() - popupSuggestion.getStartOffset());
        } else if (suggestion instanceof MultipleGhostTextSuggestion multipleGhostTextSuggestion) {
            delta = multipleGhostTextSuggestion.getTotalInsertionLength();
            changeEnd = multipleGhostTextSuggestion.getMaxStartOffset();
        }
        if (delta == 0) {
            return;
        }
        for (NextEditAutocompletion completion : suggestionQueue) {
            if (completion.getStartIndex() >= changeEnd) {
                completion.adjustOffsets(delta);
            }
        }
    }

    private record TriggerResult(AutocompleteCompletionRequest request, EditorState requestState) {
        private TriggerResult(@NotNull AutocompleteCompletionRequest request, @NotNull EditorState requestState) {
            this.request = request;
            this.requestState = requestState;
        }
    }

    private record EditorState(int caretOffset, @NotNull String documentText) {
    }

    private boolean shouldFilterCompletion(@NotNull NextEditAutocompletion completion) {
        if (lastEdit == null) {
            return false;
        }
        int start = completion.getStartIndex();
        int end = completion.getEndIndex();
        int lastStart = lastEdit.startOffset();
        int lastEnd = lastEdit.endOffset();
        return start < lastEnd && end > lastStart;
    }
}
