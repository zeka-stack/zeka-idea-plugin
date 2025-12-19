package dev.dong4j.zeka.stack.idea.plugin.common.ui.component;

import com.intellij.ui.components.JBLabel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 带前导空格的 JBLabel 组件
 * <p>
 * 继承自 JBLabel，自动在所有文本前添加一个空格，用于统一 UI 间距。
 * 重写了构造函数和 setText 方法，确保所有文本都带有前导空格。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public class SpacedJBLabel extends JBLabel {
    /**
     * 默认构造函数
     */
    public SpacedJBLabel() {
        super();
    }

    /**
     * 使用文本创建标签
     *
     * @param text 标签文本
     */
    public SpacedJBLabel(@Nullable String text) {
        super(addLeadingSpace(text));
    }

    /**
     * 使用文本和水平对齐方式创建标签
     *
     * @param text      标签文本
     * @param alignment 水平对齐方式
     */
    public SpacedJBLabel(@Nullable String text, int alignment) {
        super(addLeadingSpace(text), alignment);
    }

    /**
     * 设置标签文本
     * <p>
     * 自动在文本前添加空格，如果文本已经以空格开头则不再添加。
     *
     * @param text 标签文本
     */
    @Override
    public void setText(@Nullable String text) {
        super.setText(addLeadingSpace(text));
    }

    /**
     * 在文本前添加前导空格
     * <p>
     * 如果文本为 null 或空字符串，返回原值。
     * 如果文本是 HTML 格式，在 &lt;html&gt; 标签后添加空格。
     * 否则在文本开头添加空格。
     * <p>
     * 注意：该方法能正确处理包含表情符号（emoji）等 Unicode 字符的文本。
     *
     * @param text 原始文本
     * @return 添加了前导空格的文本
     */
    @NotNull
    private static String addLeadingSpace(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return text != null ? text : "";
        }

        // 如果已经是 HTML 格式，在 <html> 后添加空格
        String lowerText = text.toLowerCase().trim();
        if (lowerText.startsWith("<html>")) {
            // 检查 <html> 标签后是否已经有空格（跳过标签后的空白字符）
            int htmlTagEnd = 6; // "<html>" 的长度
            if (text.length() > htmlTagEnd) {
                // 跳过标签后的空白字符，检查第一个非空白字符前是否有空格
                int firstNonWhitespace = htmlTagEnd;
                while (firstNonWhitespace < text.length() &&
                       Character.isWhitespace(text.charAt(firstNonWhitespace))) {
                    firstNonWhitespace++;
                }
                // 如果第一个非空白字符前已经有空格，则不再添加
                if (firstNonWhitespace > htmlTagEnd) {
                    return text;
                }
            }
            return text.substring(0, htmlTagEnd) + " " + text.substring(htmlTagEnd);
        }

        // 检查是否已经有前导空格（包括 Unicode 空白字符）
        if (Character.isWhitespace(text.charAt(0))) {
            return text;
        }

        return " " + text;
    }
}

