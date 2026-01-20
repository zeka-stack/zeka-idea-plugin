package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

/**
 * FIX 响应验证工具类
 * <p>提供对响应文本的标准化处理功能, 主要用于去除代码块标记 (如 ```) 并提取有效内容.
 * 该类通过静态方法实现文本清洗逻辑, 适用于需要净化用户输入或解析结构化文本的场景.
 *
 * @author dong4j
 * @version 1.0.0
 * @email mailto:dong4j@gmail.com
 * @date 2026.01.20
 * @since 1.0.0
 */
public final class FixResponseValidator {
    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 仅提供静态方法, 不允许外部创建实例
     */
    private FixResponseValidator() {
    }

    /**
     * 清洗并标准化输入的响应字符串
     * <p> 该方法会移除输入字符串中的代码块标记 (如 ```), 并尝试提取和清理代码块内部内容.
     * 如果处理后的内容仍包含特殊标记 (如 ```,<<<,>>>), 则返回空字符串.
     *
     * @param raw 原始输入字符串, 可能为 null
     * @return 处理后的标准化字符串, 若包含非法标记则返回空字符串; 若输入为 null 则返回空字符串
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int fenceStart = trimmed.indexOf("```");
        if (fenceStart >= 0) {
            int fenceEnd = trimmed.indexOf("```", fenceStart + 3);
            if (fenceEnd > fenceStart) {
                String inside = trimmed.substring(fenceStart + 3, fenceEnd);
                int firstLineBreak = inside.indexOf('\n');
                if (firstLineBreak >= 0) {
                    String firstLine = inside.substring(0, firstLineBreak).trim();
                    if (!firstLine.isEmpty() && firstLine.length() <= 10 && firstLine.matches("[a-zA-Z]+")) {
                        inside = inside.substring(firstLineBreak + 1);
                    }
                }
                trimmed = inside.trim();
            }
        }
        if (trimmed.contains("```") || trimmed.contains("<<<") || trimmed.contains(">>>")) {
            return "";
        }
        return trimmed;
    }
}
