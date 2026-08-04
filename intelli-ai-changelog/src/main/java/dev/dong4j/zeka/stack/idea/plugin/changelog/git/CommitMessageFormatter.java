package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import dev.dong4j.zeka.stack.idea.plugin.kit.MessageFormatter;
import org.jetbrains.annotations.NotNull;

/**
 * Git 提交消息格式化工具类
 * <p> 在通用文本格式化的基础上，补充 Conventional Commits 对 subject 与 body 分隔空行的结构约束。
 * 该约束必须由客户端兜底，避免不同 AI 模型偶发省略必需空行，导致生成结果无法直接提交。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.08.04
 * @since 1.0.0
 */
public final class CommitMessageFormatter {
    /** 工具类不允许实例化。 */
    private CommitMessageFormatter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 格式化 AI 生成的 Git 提交消息
     * <p> subject 单独存在时保持单行；存在 body 时，subject 与 body 之间始终保留且仅保留一个空行。
     * body 内部的换行和列表结构不会被修改。
     *
     * @param text AI 生成的原始提交消息
     * @return 满足提交消息结构约束的文本
     */
    @NotNull
    public static String format(@NotNull String text) {
        String formatted = MessageFormatter.format(text)
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim();
        int firstLineEnd = formatted.indexOf('\n');
        if (firstLineEnd < 0) {
            return formatted;
        }

        String subject = formatted.substring(0, firstLineEnd).stripTrailing();
        String body = formatted.substring(firstLineEnd + 1).stripLeading();
        if (body.isBlank()) {
            return subject;
        }

        // 模型可能省略分隔空行或生成多个空行，这里统一收敛为 Conventional Commits 要求的一个空行。
        return subject + "\n\n" + body;
    }
}
