package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class AutocompleteSuggestionFactory {
    @Nullable
    AutocompleteSuggestion build(@NotNull Editor editor, @NotNull NextEditAutocompletion response) {
        Document document = editor.getDocument();
        int docLength = document.getTextLength();
        if (response.getStartIndex() < 0 || response.getEndIndex() > docLength || response.getStartIndex() > response.getEndIndex()) {
            dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil.printWarning(
                editor.getProject(), "Autocomplete 忽略: 索引越界 start=" + response.getStartIndex()
                                     + ", end=" + response.getEndIndex() + ", docLength=" + docLength);
            return null;
        }
        int caretOffset = editor.getCaretModel().getOffset();
        String oldContent = com.intellij.openapi.application.ReadAction.compute(() ->
                                                                                    document.getCharsSequence().subSequence(response.getStartIndex(), response.getEndIndex()).toString());
        if (oldContent.trim().equals(response.getCompletion().trim())) {
            dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil.printWarning(
                editor.getProject(), "Autocomplete 忽略: 新旧内容一致 old='" + oldContent + "'");
            return null;
        }
        int caretLine = document.getLineNumber(caretOffset);
        int editStartLine = document.getLineNumber(response.getStartIndex());
        int lineDifference = Math.abs(caretLine - editStartLine);
        if (lineDifference >= 6) {
            return new JumpToEditSuggestion(response.getCompletion(),
                                            response.getStartIndex(),
                                            response.getEndIndex(),
                                            response,
                                            response.getAutocompleteId(),
                                            oldContent,
                                            editor);
        }

        int relativeCaret = caretOffset - response.getStartIndex();
        Pair ghostText = getGhostTextOrNull(oldContent, response.getCompletion(), relativeCaret, document.getTextLength() == caretOffset);
        if (ghostText != null) {
            return new GhostTextSuggestion(ghostText.text, response.getStartIndex() + ghostText.insertOffset,
                                           response.getAutocompleteId(), document);
        }

        List<GhostTextSuggestion> multiple = getMultipleGhostTextOrNull(oldContent, response.getCompletion(), response.getStartIndex(),
                                                                        response.getAutocompleteId(), document);
        if (multiple != null && multiple.size() > 1) {
            return new MultipleGhostTextSuggestion(response.getCompletion(),
                                                   response.getStartIndex(),
                                                   response.getEndIndex(),
                                                   response.getAutocompleteId(),
                                                   multiple);
        }

        return new PopupSuggestion(response.getCompletion(),
                                   response.getStartIndex(),
                                   response.getEndIndex(),
                                   response.getAutocompleteId(),
                                   oldContent,
                                   editor);
    }

    @Nullable
    private Pair getGhostTextOrNull(@NotNull String oldContent,
                                    @NotNull String newContent,
                                    int caretOffset,
                                    boolean atEndOfDocument) {
        boolean caretInSpan = caretOffset < oldContent.length();
        if (caretOffset >= 0 && caretInSpan) {
            String prefix = oldContent.substring(0, caretOffset);
            String suffix = oldContent.substring(caretOffset);
            boolean newContentContainsPrefixAndSuffix = newContent.startsWith(prefix) && newContent.endsWith(suffix);
            boolean newContentIsLonger = newContent.length() > prefix.length() + suffix.length();
            if (newContentContainsPrefixAndSuffix && newContentIsLonger) {
                String addedText = newContent.substring(prefix.length(), newContent.length() - suffix.length());
                if (!addedText.contains("\n") && !addedText.isBlank()) {
                    return new Pair(addedText, caretOffset);
                }
            }
        }

        if (oldContent.length() < newContent.length()) {
            for (int i = 0; i <= oldContent.length(); i++) {
                String testPrefix = oldContent.substring(0, i);
                String testSuffix = oldContent.substring(i);
                if (newContent.startsWith(testPrefix) && newContent.endsWith(testSuffix)) {
                    String testAddedText = newContent.substring(testPrefix.length(), newContent.length() - testSuffix.length());
                    boolean caretAtNewline = testPrefix.isEmpty() || testPrefix.charAt(testPrefix.length() - 1) == '\n';
                    if (testAddedText.contains("\n") && !caretAtNewline && !atEndOfDocument) {
                        return null;
                    }
                    if (!testAddedText.isBlank()) {
                        return new Pair(testAddedText, i);
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private List<GhostTextSuggestion> getMultipleGhostTextOrNull(@NotNull String oldContent,
                                                                 @NotNull String newContent,
                                                                 int startOffset,
                                                                 @NotNull String autocompleteId,
                                                                 @NotNull Document document) {
        if (oldContent.isEmpty() || newContent.isEmpty()) {
            return null;
        }
        int commonPrefixLength = oldContent.contains("\n") ? 0 : commonPrefix(oldContent, newContent);
        List<DiffGroup> groups = DiffUtils.computeDiffGroups(oldContent.substring(commonPrefixLength),
                                                             newContent.substring(commonPrefixLength));
        boolean allAdditions = groups.stream().allMatch(group -> !group.hasDeletions());
        if (!allAdditions || groups.size() <= 1) {
            return null;
        }
        List<GhostTextSuggestion> suggestions = new ArrayList<>();
        for (DiffGroup group : groups) {
            if (group.hasAdditions()) {
                int insertionOffset = startOffset + group.index() + commonPrefixLength;
                GhostTextSuggestion ghost = new GhostTextSuggestion(group.additions(), insertionOffset, autocompleteId, document);
                suggestions.add(ghost);
            }
        }
        return suggestions;
    }

    private int commonPrefix(@NotNull String a, @NotNull String b) {
        int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    private record Pair(@NotNull String text, int insertOffset) {
    }
}
