package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import lombok.extern.slf4j.Slf4j;

/**
 * 为单个 Commit Message 编辑器挂载 Conventional Commit 首行高亮，并在 type/scope 段主动拉起补全。
 *
 * @author dong4j
 * @since 1.0.0
 */
@Slf4j
public final class ConventionalCommitEditorSupport implements Disposable {

    private static final int DEBOUNCE_MS = 80;

    private final Editor editor;
    private final ConventionalCommitHighlighter highlighter;
    private final Alarm alarm;

    private ConventionalCommitEditorSupport(@NotNull Editor editor) {
        this.editor = editor;
        this.highlighter = new ConventionalCommitHighlighter(editor);
        this.alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
    }

    /**
     * 挂载高亮与文档监听；功能关闭时返回 {@code null}。
     */
    @Nullable
    public static ConventionalCommitEditorSupport attach(@NotNull Editor editor, @NotNull Disposable parent) {
        if (!SettingsState.getInstance().enableConventionalCommitAssist) {
            log.info("Conventional Commit Assist 已关闭，跳过挂载");
            return null;
        }

        ConventionalCommitEditorSupport support = new ConventionalCommitEditorSupport(editor);
        Disposer.register(parent, support);
        Disposer.register(support, support.highlighter);
        support.setupListener();

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!editor.isDisposed()) {
                support.highlighter.refresh();
                log.info("Conventional Commit 高亮已挂载并完成首次刷新");
            }
        });

        Project project = editor.getProject();
        if (project != null) {
            ConventionalCommitScopeProvider.getInstance(project).refreshAsync();
        }

        return support;
    }

    private void setupListener() {
        Document document = editor.getDocument();
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (alarm.isDisposed()) {
                    return;
                }
                alarm.cancelAllRequests();
                // 捕获本次变更：仅用户小幅键入才尝试弹补全，避免空框/AI 生成后误弹
                final DocumentEvent change = event;
                final boolean typingLike = ConventionalCommitCompletionInvoker.isLikelyUserTyping(change);
                alarm.addRequest(() -> {
                    if (editor.isDisposed()) {
                        return;
                    }
                    highlighter.refresh();
                    Project project = editor.getProject();
                    if (project == null || project.isDisposed()) {
                        return;
                    }
                    if (!typingLike) {
                        // AI / 粘贴整段写入：收起可能残留的补全弹层
                        LookupManager.getInstance(project).hideActiveLookup();
                        return;
                    }
                    ConventionalCommitCompletionInvoker.invokeIfNeeded(project, editor, change);
                }, DEBOUNCE_MS);
            }
        };
        document.addDocumentListener(documentListener, this);
    }

    @Override
    public void dispose() {
        alarm.cancelAllRequests();
    }
}
