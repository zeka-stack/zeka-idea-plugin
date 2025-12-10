package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;

import org.jetbrains.annotations.NotNull;

/**
 * JavaDoc 单行注释格式化工具类
 * <p>
 * 提供将多行 JavaDoc 单行注释压缩为单行格式的功能。
 * 当 JavaDoc 注释只有一行内容时，将其格式化为 {@code /** comment
 */}格式。
    *
    *@author zeka.stack.team
 *@version 1.0.0
    *@since 1.0.0
    */

public final class JavaDocSingleLineFormatter {
    /**
     * 单行注释的最大长度限制（字符数）
     * <p>
     * 超过此长度的注释不压缩为单行格式
     */
    private static final int MAX_SINGLE_LINE_LENGTH = 120;

    /**
     * 私有构造函数，防止实例化
     */
    private JavaDocSingleLineFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 压缩元素的 JavaDoc 注释为单行格式（如果是单行注释）
     * <p>
     * 检查元素的 JavaDoc 注释是否为单行注释，如果是则压缩为单行格式。
     *
     * @param element  目标元素
     * @param document 文档对象
     */
    public static void compressSingleLineJavaDoc(@NotNull PsiElement element,
                                                 @NotNull Document document) {
        if (!(element instanceof PsiDocCommentOwner docCommentOwner)) {
            return;
        }

        PsiDocComment docComment = docCommentOwner.getDocComment();
        if (docComment == null) {
            return;
        }

        String commentText = docComment.getText();
        if (!isSingleLineComment(commentText)) {
            return;
        }

        String singleLineComment = compressToSingleLine(commentText);
        if (singleLineComment == null || singleLineComment.equals(commentText)) {
            return;
        }

        // 替换注释文本
        int startOffset = docComment.getTextRange().getStartOffset();
        int endOffset = docComment.getTextRange().getEndOffset();
        document.replaceString(startOffset, endOffset, singleLineComment);
    }

    /**
     * 判断 JavaDoc 注释是否为单行注释
     * <p>
     * 单行注释的判断标准：
     * <ul>
     *   <li>注释内容只有一行（不包括开始标记和结束标记）</li>
     *   <li>注释内容不包含 JavaDoc 标签（@param、@return、@throws 等）</li>
     *   <li>注释内容长度不超过限制</li>
     * </ul>
     *
     * @param commentText JavaDoc 注释文本
     * @return 如果是单行注释返回 true，否则返回 false
     */
    @SuppressWarnings("D")
    private static boolean isSingleLineComment(@NotNull String commentText) {
        if (commentText.isEmpty()) {
            return false;
        }

        // 移除开始和结束标记
        String content = commentText
            .replaceAll("^/\\*\\*", "")  // 移除 /** 开头
            .replaceAll("\\*/$", "")     // 移除 */ 结尾
            .trim();

        if (content.isEmpty()) {
            // 空注释：/** */，不压缩
            return false;
        }

        // 移除每行开头的 * 和空白字符
        String[] lines = content.split("\\n");
        StringBuilder contentBuilder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            // 移除行首的 * 和后续空白
            if (trimmed.startsWith("*")) {
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.isEmpty()) {
                if (!contentBuilder.isEmpty()) {
                    contentBuilder.append(" ");
                }
                contentBuilder.append(trimmed);
            }
        }

        String finalContent = contentBuilder.toString().trim();
        if (finalContent.isEmpty()) {
            return false;
        }

        // 检查是否包含 JavaDoc 标签
        if (containsJavaDocTags(finalContent)) {
            return false;
        }

        // 检查长度限制
        if (finalContent.length() > MAX_SINGLE_LINE_LENGTH) {
            return false;
        }

        // 检查是否只有一行内容（去除标记后）
        // 格式化后的多行单行注释通常是：
        // /**
        //  * content
        //  */
        // 所以 lines.length 通常是 3 或 4（可能有空行）
        // 但我们通过最终内容来判断，所以这里检查行数主要是排除真正的多行注释
        int nonEmptyLines = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("*")) {
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.isEmpty()) {
                nonEmptyLines++;
            }
        }
        // 只允许一行非空内容（不包括开始和结束标记）
        return nonEmptyLines <= 1;
    }

    /**
     * 检查内容是否包含 JavaDoc 标签
     *
     * @param content 注释内容
     * @return 如果包含标签返回 true，否则返回 false
     */
    private static boolean containsJavaDocTags(@NotNull String content) {
        // 常见的 JavaDoc 标签
        String[] tags = {
            "@param", "@return", "@throws", "@exception", "@see",
            "@since", "@author", "@version", "@deprecated", "@inheritDoc",
            "@apiNote", "@implSpec", "@implNote", "@serial", "@serialData",
            "@serialField", "@hidden"
        };

        String lowerContent = content.toLowerCase();
        for (String tag : tags) {
            if (lowerContent.contains(tag)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 将多行 JavaDoc 注释压缩为单行格式
     * <p>
     * 将格式如下的注释：
     * <pre>
     * /**
     *  * Comment text
     *  /
     * </pre>
     * 压缩为：
     * <pre>
     * /** Comment text /
     * </pre>
     *
     * @param commentText 原始 JavaDoc 注释文本
     * @return 压缩后的单行注释，如果无法压缩返回 null
     */
    private static String compressToSingleLine(@NotNull String commentText) {
        if (commentText.isEmpty()) {
            return null;
        }

        // 提取注释内容
        String content = commentText
            .replaceAll("^/\\*\\*", "")  // 移除 /** 开头
            .replaceAll("\\*/$", "")     // 移除 */ 结尾
            .trim();

        if (content.isEmpty()) {
            return null;
        }

        // 提取所有非空行的内容
        String[] lines = content.split("\\n");
        StringBuilder contentBuilder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            // 移除行首的 * 和后续空白
            if (trimmed.startsWith("*")) {
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.isEmpty()) {
                if (!contentBuilder.isEmpty()) {
                    contentBuilder.append(" ");
                }
                contentBuilder.append(trimmed);
            }
        }

        String finalContent = contentBuilder.toString().trim();
        if (finalContent.isEmpty()) {
            return null;
        }

        // 构建单行注释格式
        return "/** " + finalContent + " */";
    }
}

