package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class AutocompleteSuggestion implements Disposable {
    private long shownTime;
    private long disposedTime;
    private Runnable onDispose = () -> {
    };

    @NotNull
    abstract String getContent();

    abstract int getStartOffset();

    abstract int getEndOffset();

    @NotNull
    abstract String getAutocompleteId();

    void show(@NotNull Editor editor) {
        show(editor, false);
    }

    abstract void show(@NotNull Editor editor, boolean isPostJumpSuggestion);

    @Nullable
    abstract Disposable accept(@NotNull Editor editor);

    long getShownTime() {
        return shownTime;
    }

    void setShownTime(long shownTime) {
        this.shownTime = shownTime;
    }

    long getDisposedTime() {
        return disposedTime;
    }

    void setDisposedTime(long disposedTime) {
        this.disposedTime = disposedTime;
    }

    @NotNull
    Runnable getOnDispose() {
        return onDispose;
    }

    void setOnDispose(@NotNull Runnable onDispose) {
        this.onDispose = onDispose;
    }

    @NotNull
    String rejectionCacheKey() {
        return getContent();
    }

    void markDisposed() {
        setDisposedTime(System.currentTimeMillis());
        onDispose.run();
    }
}
