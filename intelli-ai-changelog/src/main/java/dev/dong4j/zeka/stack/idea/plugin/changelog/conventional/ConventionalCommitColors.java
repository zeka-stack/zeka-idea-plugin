package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;

import java.awt.Font;

/**
 * Conventional Commit 首行高亮的颜色定义。
 * <p>
 * 同时提供：
 * <ul>
 *     <li>{@link TextAttributesKey}：挂到标准高亮 fallback，便于以后做 Color Scheme；</li>
 *     <li>{@link TextAttributes}：直接交给 {@code MarkupModel}，避免 key 未写入 scheme 时“有 Highlighter 但看不见颜色”。</li>
 * </ul>
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConventionalCommitColors {

    /** 标准 type。 */
    public static final TextAttributesKey TYPE = TextAttributesKey.createTextAttributesKey(
        "CHANGELOG_CC_TYPE", DefaultLanguageHighlighterColors.KEYWORD);

    /** 非标准 type。 */
    public static final TextAttributesKey TYPE_UNKNOWN = TextAttributesKey.createTextAttributesKey(
        "CHANGELOG_CC_TYPE_UNKNOWN", DefaultLanguageHighlighterColors.LINE_COMMENT);

    /** scope。 */
    public static final TextAttributesKey SCOPE = TextAttributesKey.createTextAttributesKey(
        "CHANGELOG_CC_SCOPE", DefaultLanguageHighlighterColors.PARAMETER);

    /** breaking {@code !}。 */
    public static final TextAttributesKey BREAKING = TextAttributesKey.createTextAttributesKey(
        "CHANGELOG_CC_BREAKING", DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);

    /** 分隔符 {@code :}。 */
    public static final TextAttributesKey SEPARATOR = TextAttributesKey.createTextAttributesKey(
        "CHANGELOG_CC_SEPARATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    /** subject。 */
    public static final TextAttributesKey SUBJECT = TextAttributesKey.createTextAttributesKey(
        "CHANGELOG_CC_SUBJECT", DefaultLanguageHighlighterColors.STRING);

    /** 标准 type 的显式属性（蓝/亮蓝，加粗）。 */
    public static final TextAttributes TYPE_ATTR =
        new TextAttributes(new JBColor(0x1750EB, 0x6C9DFF), null, null, null, Font.BOLD);

    /** 未知 type 的显式属性（灰）。 */
    public static final TextAttributes TYPE_UNKNOWN_ATTR =
        new TextAttributes(new JBColor(0x8C8C8C, 0x9A9A9A), null, null, null, Font.PLAIN);

    /** scope 的显式属性（青绿）。 */
    public static final TextAttributes SCOPE_ATTR =
        new TextAttributes(new JBColor(0x0E8C7A, 0x4FD1C5), null, null, null, Font.PLAIN);

    /** breaking 的显式属性（红/橙，加粗）。 */
    public static final TextAttributes BREAKING_ATTR =
        new TextAttributes(new JBColor(0xC75450, 0xFF6B68), null, null, null, Font.BOLD);

    /** 分隔符的显式属性（灰）。 */
    public static final TextAttributes SEPARATOR_ATTR =
        new TextAttributes(new JBColor(0x8C8C8C, 0xA0A0A0), null, null, null, Font.PLAIN);

    /** subject 的显式属性（接近默认正文）。 */
    public static final TextAttributes SUBJECT_ATTR =
        new TextAttributes(new JBColor(0x1F1F1F, 0xD4D4D4), null, null, null, Font.PLAIN);

    private ConventionalCommitColors() {
        throw new UnsupportedOperationException("Utility class");
    }
}
