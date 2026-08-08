package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.CommitMessageGenerator;

/**
 * 在 Commit Message 首行 type/scope 段，仅在「用户正在键入」时主动拉起 BASIC 补全弹层。
 * <p>
 * 不依赖 {@code AutoPopupController}：提交框是 EditorTextField + Plain Text，
 * 平台默认常抑制 autopopup；直接 {@link CodeCompletionHandlerBase#invokeCompletion} 更可靠。
 * <p>
 * 约束（避免误弹）：
 * <ul>
 *   <li>空文档不自动弹（用户未输入）</li>
 *   <li>AI 生成写入 / 大段替换不自动弹</li>
 *   <li>生成任务进行中不自动弹</li>
 * </ul>
 * 手动 {@code Ctrl+Space} 仍由 {@link ConventionalCommitCompletionContributor} 提供候选。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitCompletionInvoker {

    /** 超过该插入长度视为粘贴 / AI 流式块，不主动弹补全 */
    private static final int MAX_TYPING_INSERT_CHARS = 2;

    private ConventionalCommitCompletionInvoker() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 文档变更后按需触发补全：仅用户小幅键入，且光标落在 TYPE / SCOPE / BREAKING。
     */
    public static void invokeIfNeeded(@NotNull Project project,
                                      @NotNull Editor editor,
                                      @NotNull DocumentEvent event) {
        if (!isLikelyUserTyping(event)) {
            return;
        }
        invokeIfNeeded(project, editor);
    }

    /**
     * 若光标位于 TYPE / SCOPE / BREAKING 段、已有输入前缀、且当前没有活跃 Lookup，则触发补全。
     */
    public static void invokeIfNeeded(@NotNull Project project, @NotNull Editor editor) {
        if (project.isDisposed() || editor.isDisposed()) {
            return;
        }
        if (CommitMessageGenerator.isRunning(project)) {
            return;
        }
        if (LookupManager.getInstance(project).getActiveLookup() != null) {
            return;
        }

        Document document = editor.getDocument();
        // 空文档不自动弹：避免打开 Commit 框或清空后误出 type 列表
        if (document.getTextLength() == 0) {
            return;
        }

        int offset = editor.getCaretModel().getOffset();
        if (document.getLineNumber(Math.min(offset, document.getTextLength())) != 0) {
            return;
        }

        int offsetInLine = offset - document.getLineStartOffset(0);
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine(document.getCharsSequence());
        ConventionalCommitContext ctx = ConventionalCommitHeaderParser.contextAt(header, offsetInLine);
        if (ctx != ConventionalCommitContext.TYPE
            && ctx != ConventionalCommitContext.SCOPE
            && ctx != ConventionalCommitContext.BREAKING) {
            return;
        }

        // TYPE 段：至少已键入 1 个字符才自动弹（未输入时不弹全量 type 列表）
        if (ctx == ConventionalCommitContext.TYPE && offsetInLine <= 0) {
            return;
        }

        new CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(project, editor);
    }

    /**
     * 判断是否像用户键盘输入（单字符/退格），而非 AI 整段写入或粘贴。
     */
    static boolean isLikelyUserTyping(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        if (document.getTextLength() == 0) {
            return false;
        }
        // 大段删除或大段插入：生成 / 粘贴 / 整框替换
        if (event.getOldLength() > MAX_TYPING_INSERT_CHARS || event.getNewLength() > MAX_TYPING_INSERT_CHARS) {
            return false;
        }
        // 无实际变更
        return event.getOldLength() > 0 || event.getNewLength() > 0;
    }
}
