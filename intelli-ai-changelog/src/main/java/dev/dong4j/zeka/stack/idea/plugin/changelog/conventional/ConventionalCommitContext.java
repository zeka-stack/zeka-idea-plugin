package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

/**
 * Conventional Commit 首行内光标所在语义片段。
 * <p>
 * 后续高亮与补全仅针对提交消息第一行（header）；body 不参与这些片段划分。
 *
 * @author dong4j
 * @since 1.0.0
 */
public enum ConventionalCommitContext {
    TYPE,
    SCOPE,
    BREAKING,
    SUBJECT,
    OTHER
}
