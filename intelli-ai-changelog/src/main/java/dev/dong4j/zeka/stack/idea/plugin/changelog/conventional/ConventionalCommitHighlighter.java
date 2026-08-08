package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Conventional Commit 首行分段高亮实现。
 * <p>
 * 使用 {@link MarkupModel#addRangeHighlighter(int, int, int, TextAttributes, HighlighterTargetArea)}
 * 直接写入 {@link TextAttributes}，避免仅注册 {@code TextAttributesKey} 却未进入 Color Scheme 时“看不见颜色”。
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitHighlighter implements Disposable {

    private final Editor editor;
    private final List<RangeHighlighter> highlighters = new ArrayList<>();

    /**
     * @param editor 需要高亮的编辑器
     */
    public ConventionalCommitHighlighter(@NotNull Editor editor) {
        this.editor = editor;
    }

    /**
     * 重新解析首行并刷新高亮。必须在 EDT 调用。
     */
    public void refresh() {
        clear();
        if (editor.isDisposed()) {
            return;
        }

        Document document = editor.getDocument();
        ConventionalCommitHeader header = ConventionalCommitHeaderParser.parseFirstLine(document.getCharsSequence());
        MarkupModel model = editor.getMarkupModel();

        addHighlighter(model, header.typeRange(), resolveTypeAttributes(header.type()));
        addHighlighter(model, header.scopeRange(), ConventionalCommitColors.SCOPE_ATTR);
        addHighlighter(model, header.breakingRange(), ConventionalCommitColors.BREAKING_ATTR);
        addHighlighter(model, header.separatorRange(), ConventionalCommitColors.SEPARATOR_ATTR);
        addHighlighter(model, header.subjectRange(), ConventionalCommitColors.SUBJECT_ATTR);
    }

    @NotNull
    private static TextAttributes resolveTypeAttributes(@Nullable String type) {
        if (type != null && ConventionalCommitTypes.isStandard(type)) {
            return ConventionalCommitColors.TYPE_ATTR;
        }
        return ConventionalCommitColors.TYPE_UNKNOWN_ATTR;
    }

    private void addHighlighter(@NotNull MarkupModel model,
                                @Nullable TextRange range,
                                @NotNull TextAttributes attributes) {
        if (range == null || range.isEmpty()) {
            return;
        }
        // 首行 ranges 相对文档 offset 0 起算；EXACT_RANGE 保证只染对应 token
        RangeHighlighter highlighter = model.addRangeHighlighter(
            range.getStartOffset(),
            range.getEndOffset(),
            HighlighterLayer.SYNTAX + 1,
            attributes,
            HighlighterTargetArea.EXACT_RANGE);
        highlighters.add(highlighter);
    }

    private void clear() {
        for (RangeHighlighter highlighter : highlighters) {
            highlighter.dispose();
        }
        highlighters.clear();
    }

    @Override
    public void dispose() {
        clear();
    }
}
