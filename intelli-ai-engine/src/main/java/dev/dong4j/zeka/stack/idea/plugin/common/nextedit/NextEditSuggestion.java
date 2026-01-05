package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.InlayProperties;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;

/**
 * 提供编辑器中下一次编辑建议的功能类
 * <p> 该类用于在编辑器中显示和管理下一次编辑建议, 包括高亮范围, 添加提示内联元素以及滚动到建议位置等操作.
 * <p> 主要功能包括:
 * <ul>
 *   <li> 显示编辑建议的高亮范围 </li>
 *   <li> 在编辑器中添加提示内联元素 </li>
 *   <li> 滚动到建议位置以确保可见 </li>
 *   <li> 接受建议并执行替换操作 </li>
 *   <li> 释放资源并清理相关对象 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditSuggestion implements Disposable {
    /**
     * 编辑建议的起始偏移量.
     * <p> 表示编辑建议中需要替换文本的起始位置.
     */
    private final int startOffset;
    /** 结束偏移量 */
    private final int endOffset;
    /** 替换文本内容 */
    private final String replacement;
    /** 用于编辑器操作的文本编辑器实例 */
    private final Editor editor;
    /**
     * 用于存储高亮范围的标记器.
     * <p> 在显示编辑建议时, 使用此标记器来突出显示需要替换的文本范围.
     *
     * @see #highlightRange()
     */
    private RangeHighlighter highlighter;
    /**
     * 表示编辑器中内联元素的占位符.
     * <p> 在显示建议时添加到编辑器中, 提供额外的提示信息.
     *
     * @see Inlay
     */
    private Inlay inlay;
    /** 是否已释放资源, 用于防止重复释放或在已释放状态下执行操作 */
    private boolean disposed;

    /**
     * 构造一个编辑建议对象
     * <p> 用于创建一个编辑建议, 指定替换范围, 替换内容和编辑器实例
     *
     * @param startOffset 要替换的起始偏移量
     * @param endOffset   要替换的结束偏移量
     * @param replacement 替换内容, 不能为空
     * @param editor      编辑器实例, 不能为空
     */
    NextEditSuggestion(int startOffset, int endOffset, @NotNull String replacement, @NotNull Editor editor) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.replacement = replacement;
        this.editor = editor;
    }

    /**
     * 显示编辑建议
     * <p> 在编辑器中高亮指定范围并添加提示内联元素, 同时滚动到建议位置
     *
     */
    void show() {
        if (disposed) {
            return;
        }
        highlightRange();
        addHintInlay();
        scrollToSuggestion();
    }

    /**
     * 应用编辑建议, 替换指定范围的文本
     * <p> 如果对象已释放或编辑器没有关联项目, 则直接返回. 否则, 执行写入操作以替换文档中的指定范围文本, 并将光标移动到替换后的位置.
     *
     */
    void accept() {
        if (disposed || editor.getProject() == null) {
            return;
        }
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> {
            editor.getDocument().replaceString(startOffset, endOffset, replacement);
            editor.getCaretModel().moveToOffset(startOffset + replacement.length());
        });
        dispose();
    }

    /**
     * 获取建议编辑的起始偏移量
     * <p> 返回建议编辑的起始位置的偏移量
     *
     * @return 起始偏移量
     */
    int getStartOffset() {
        return startOffset;
    }

    /**
     * 获取建议编辑的结束偏移量
     * <p> 返回当前编辑建议所覆盖文本的结束位置偏移量
     *
     * @return 结束偏移量
     */
    int getEndOffset() {
        return endOffset;
    }

    /**
     * 获取替换文本与原文本长度的差值
     * <p> 计算替换文本长度与原文本范围长度的差值, 用于调整光标位置或更新编辑器状态
     *
     * @return 替换文本长度与原文本范围长度的差值
     */
    int getDelta() {
        return replacement.length() - (endOffset - startOffset);
    }

    /**
     * 获取替换字符串
     * <p> 返回用于替换文本的字符串
     *
     * @return 替换字符串, 不能为 null
     */
    @NotNull
    String getReplacement() {
        return replacement;
    }

    /**
     * 释放资源并清理与当前编辑建议相关的高亮和内联提示
     * <p> 此方法确保在对象被销毁时, 相关的高亮和内联提示被正确移除, 避免内存泄漏
     * <p> 如果对象已经被处置, 则直接返回, 不再进行重复操作
     *
     * @since 1.0
     */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (highlighter != null) {
            editor.getMarkupModel().removeHighlighter(highlighter);
        }
        highlighter = null;
        if (inlay != null) {
            inlay.dispose();
        }
        inlay = null;
    }

    /**
     * 高亮指定的代码范围
     * <p> 为给定的起始和结束偏移量创建一个高亮区域, 用于在编辑器中可视化标记该范围.
     *
     */
    private void highlightRange() {
        TextAttributes attrs = new TextAttributes();
        attrs.setBackgroundColor(new JBColor(new Color(255, 238, 210), new Color(70, 50, 20)));
        highlighter = editor.getMarkupModel().addRangeHighlighter(startOffset, endOffset,
                                                                  HighlighterLayer.SELECTION - 1, attrs,
                                                                  com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE);
    }

    /**
     * 在指定位置添加提示内联元素
     * <p> 根据当前编辑器中的起始偏移量获取所在行号, 并计算该行的结束偏移量, 然后使用指定的属性和渲染器在该位置添加内联提示元素.
     *
     */
    private void addHintInlay() {
        int lineNumber = editor.getDocument().getLineNumber(startOffset);
        int lineEndOffset = editor.getDocument().getLineEndOffset(lineNumber);
        InlayProperties properties = new InlayProperties();
        properties.relatesToPrecedingText(true);
        properties.disableSoftWrapping(true);
        NextEditSuggestionRenderer renderer = new NextEditSuggestionRenderer(editor, replacement);
        inlay = editor.getInlayModel().addInlineElement(lineEndOffset, properties, renderer);
    }

    /**
     * 将光标滚动到建议的位置
     * <p> 根据起始偏移量获取所在行号, 并将编辑器滚动到该行的起始位置, 以便用户查看建议内容
     *
     */
    private void scrollToSuggestion() {
        int lineNumber = editor.getDocument().getLineNumber(startOffset);
        editor.getScrollingModel().scrollTo(new LogicalPosition(lineNumber, 0), ScrollType.MAKE_VISIBLE);
    }
}
