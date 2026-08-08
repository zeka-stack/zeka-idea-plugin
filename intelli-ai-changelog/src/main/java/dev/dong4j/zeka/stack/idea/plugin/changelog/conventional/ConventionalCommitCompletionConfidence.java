package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.changelog.hint.CommitMessageHintService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;

/**
 * 允许在 Commit Message 的 type/scope/! 段自动弹出补全。
 * <p>
 * 平台对 Plain Text 默认常跳过 autopopup；若不覆盖，输入 {@code f} 时不会出现补全弹层。
 * 空文档、subject 段仍跳过 autopopup，避免未输入或 AI 生成后误弹。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitCompletionConfidence extends CompletionConfidence {

    @Override
    public @NotNull ThreeState shouldSkipAutopopup(@NotNull PsiElement contextElement,
                                                   @NotNull PsiFile psiFile,
                                                   int offset) {
        if (!SettingsState.getInstance().enableConventionalCommitAssist) {
            return ThreeState.UNSURE;
        }

        Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
        if (document == null) {
            return ThreeState.UNSURE;
        }

        if (!isCommitMessageDocument(document)) {
            return ThreeState.UNSURE;
        }

        // 空框：跳过平台 autopopup（手动 Ctrl+Space 仍可用）
        if (document.getTextLength() == 0 || offset <= 0) {
            return ThreeState.YES;
        }

        if (document.getLineNumber(Math.min(offset, document.getTextLength())) != 0) {
            return ThreeState.YES;
        }

        int offsetInLine = offset - document.getLineStartOffset(0);
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine(document.getCharsSequence());
        ConventionalCommitContext ctx = ConventionalCommitHeaderParser.contextAt(header, offsetInLine);
        if (ctx == ConventionalCommitContext.TYPE
            || ctx == ConventionalCommitContext.SCOPE
            || ctx == ConventionalCommitContext.BREAKING) {
            // 不跳过：允许平台在用户键入后 autopopup
            return ThreeState.NO;
        }
        // subject / 其它：跳过，避免历史提交列表抢焦点
        return ThreeState.YES;
    }

    private static boolean isCommitMessageDocument(@NotNull Document document) {
        if (document.getUserData(CommitMessage.DATA_KEY) != null) {
            return true;
        }
        for (Editor editor : EditorFactory.getInstance().getEditors(document)) {
            if (CommitMessageHintService.isCommitMessageEditor(editor)) {
                return true;
            }
        }
        return false;
    }
}
