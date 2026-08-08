package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import dev.dong4j.zeka.stack.idea.plugin.changelog.hint.CommitMessageHintService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;

/**
 * Commit Message 首行 Conventional Commit 补全（type / scope / {@code !}）。
 * <p>
 * 必须排在平台 {@code commitCompletion}（历史提交消息补全）之前，并在 TYPE/SCOPE/BREAKING
 * 段调用 {@link CompletionResultSet#stopHere()}，否则输入 {@code f} 时只会弹出历史 commit 列表。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitCompletionContributor extends CompletionContributor implements DumbAware {

    private static final double TYPE_PRIORITY = 1000.0;
    private static final double SCOPE_PRIORITY = 900.0;
    private static final double BREAKING_PRIORITY = 800.0;

    private static final InsertHandler<LookupElement> TYPE_INSERT_HANDLER = (insertionContext, item) -> {
        Editor editor = insertionContext.getEditor();
        Document document = editor.getDocument();
        int tailOffset = insertionContext.getTailOffset();
        CharSequence text = document.getImmutableCharSequence();
        char nextChar = tailOffset < text.length() ? text.charAt(tailOffset) : '\u0000';
        if (nextChar == ':' || nextChar == '(' || nextChar == '!') {
            return;
        }
        document.insertString(tailOffset, ": ");
        editor.getCaretModel().moveToOffset(tailOffset + 2);
    };

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }
        if (!SettingsState.getInstance().enableConventionalCommitAssist) {
            return;
        }

        Editor editor = parameters.getEditor();
        if (!CommitMessageHintService.isCommitMessageEditor(editor)) {
            return;
        }

        Document document = editor.getDocument();
        int offset = parameters.getOffset();
        if (document.getLineNumber(Math.min(offset, Math.max(document.getTextLength(), 0))) != 0) {
            return;
        }

        int lineStart = document.getLineStartOffset(0);
        int offsetInLine = Math.max(0, offset - lineStart);
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine(document.getCharsSequence());
        ConventionalCommitContext ctx = ConventionalCommitHeaderParser.contextAt(header, offsetInLine);

        switch (ctx) {
            case TYPE -> {
                addTypeCompletions(header, offsetInLine, result);
                // 拦住平台历史提交补全，避免盖住 feat/fix/docs
                result.stopHere();
            }
            case BREAKING -> {
                result.addElement(PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create("!").withTypeText("breaking change").bold(),
                    BREAKING_PRIORITY));
                result.stopHere();
            }
            case SCOPE -> {
                addScopeCompletions(header, offsetInLine, editor.getProject(), result);
                result.stopHere();
            }
            case SUBJECT, OTHER -> {
                // subject 不抢补全，留给用户自由输入 / 平台历史（如需要）
            }
        }
    }

    private static void addTypeCompletions(
        @NotNull ConventionalCommitHeader header,
        int offsetInLine,
        @NotNull CompletionResultSet result) {

        TextRange typeRange = header.typeRange();
        String typePrefix;
        if (typeRange == null) {
            typePrefix = "";
        } else {
            int prefixEnd = Math.min(offsetInLine, typeRange.getEndOffset());
            int prefixStart = Math.min(typeRange.getStartOffset(), prefixEnd);
            typePrefix = header.firstLine().substring(prefixStart, prefixEnd);
        }

        boolean canAutoAppendColon = !header.hasSeparator()
                                      && header.scopeRange() == null
                                      && !header.hasBreakingChange();

        CompletionResultSet rs = result.caseInsensitive().withPrefixMatcher(typePrefix);
        double priority = TYPE_PRIORITY;
        for (String type : ConventionalCommitTypes.matchesPrefix(typePrefix)) {
            LookupElementBuilder builder = LookupElementBuilder.create(type)
                .withTypeText(ConventionalCommitTypes.description(type))
                .bold();
            if (canAutoAppendColon) {
                builder = builder.withInsertHandler(TYPE_INSERT_HANDLER);
            }
            rs.addElement(PrioritizedLookupElement.withPriority(builder, priority));
            priority -= 1.0;
        }
    }

    private static void addScopeCompletions(
        @NotNull ConventionalCommitHeader header,
        int offsetInLine,
        @Nullable Project project,
        @NotNull CompletionResultSet result) {

        if (project == null) {
            return;
        }

        TextRange scopeRange = header.scopeRange();
        if (scopeRange == null) {
            return;
        }

        List<String> recentScopes = ConventionalCommitScopeProvider.getInstance(project).getRecentScopes();
        if (recentScopes.isEmpty()) {
            return;
        }

        int contentStart = scopeRange.getStartOffset() + 1;
        int prefixEnd = Math.max(contentStart, Math.min(offsetInLine, scopeRange.getEndOffset()));
        String scopePrefix = prefixEnd > contentStart
                             ? header.firstLine().substring(contentStart, prefixEnd)
                             : "";
        if (scopePrefix.endsWith(")")) {
            scopePrefix = scopePrefix.substring(0, scopePrefix.length() - 1);
        }

        String lowerPrefix = scopePrefix.toLowerCase(Locale.ROOT);
        CompletionResultSet rs = result.caseInsensitive().withPrefixMatcher(scopePrefix);
        double priority = SCOPE_PRIORITY;
        for (String scope : recentScopes) {
            if (scope.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                rs.addElement(PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create(scope).withTypeText("scope"),
                    priority));
                priority -= 1.0;
            }
        }
    }
}
