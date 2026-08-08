package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 从提交消息文档中解析出的 Conventional Commit 首行（header）结构。
 * <p>
 * 只描述第一行：IDE 提交框中 subject 与 body 由空行分隔，高亮/补全仅依赖 header，
 * 因此 {@link ConventionalCommitHeaderParser}  intentionally 忽略 {@code \n} 之后的内容。
 *
 * @param firstLine            首行文本（不含换行符）
 * @param firstLineEndExclusive 文档中首行结束位置（换行符下标，或无换行时为文档长度）
 * @param type                 type 段文本，可能为未完成的输入
 * @param scope                scope 括号内文本；无 scope 或未进入括号时为 {@code null}
 * @param subject              {@code :} 之后到首行末尾的文本（常含 leading space）
 * @param hasBreakingChange    是否出现 breaking {@code !}
 * @param hasSeparator         是否出现 subject 前的 {@code :}
 * @param typeRange            type 在首行内的范围
 * @param scopeRange           含括号的 scope 段范围；无 scope 时为 {@code null}
 * @param breakingRange        {@code !} 的范围；无 breaking 时为 {@code null}
 * @param separatorRange       {@code :} 的范围；无分隔符时为 {@code null}
 * @param subjectRange         subject 在首行内的范围；无分隔符时为 {@code null}
 * @author dong4j
 * @since 1.0.0
 */
public record ConventionalCommitHeader(
    @NotNull String firstLine,
    int firstLineEndExclusive,
    @Nullable String type,
    @Nullable String scope,
    @Nullable String subject,
    boolean hasBreakingChange,
    boolean hasSeparator,
    @Nullable TextRange typeRange,
    @Nullable TextRange scopeRange,
    @Nullable TextRange breakingRange,
    @Nullable TextRange separatorRange,
    @Nullable TextRange subjectRange
) {
}
