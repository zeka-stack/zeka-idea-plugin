package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Git 提交消息格式化测试
 * <p> 验证模型输出缺少或包含多余分隔空行时，最终提交消息仍满足 Conventional Commits 的结构约束。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.08.04
 * @since 1.0.0
 */
class CommitMessageFormatterTest {
    /** 缺少分隔空行时应自动补齐。 */
    @Test
    void shouldInsertBlankLineBeforeBody() {
        String input = "fix(changelog): 修复提交消息格式\n- 补齐 subject 与 body 之间的空行\n- 保留详细列表";

        String result = CommitMessageFormatter.format(input);

        assertThat(result).isEqualTo(
            "fix(changelog): 修复提交消息格式\n\n- 补齐 subject 与 body 之间的空行\n- 保留详细列表");
    }

    /** 已有正确分隔空行时不应改变结构。 */
    @Test
    void shouldKeepSingleBlankLineBeforeBody() {
        String input = "fix(changelog): 修复提交消息格式\n\n- 保留详细列表";

        assertThat(CommitMessageFormatter.format(input)).isEqualTo(input);
    }

    /** 多个分隔空行应收敛为一个。 */
    @Test
    void shouldCollapseExtraBlankLinesBeforeBody() {
        String input = "fix(changelog): 修复提交消息格式\n\n\n\n- 保留详细列表";

        assertThat(CommitMessageFormatter.format(input))
            .isEqualTo("fix(changelog): 修复提交消息格式\n\n- 保留详细列表");
    }

    /** 只有 subject 时不应追加空行。 */
    @Test
    void shouldKeepSubjectOnlyMessageOnOneLine() {
        String input = "fix(changelog): 修复提交消息格式";

        assertThat(CommitMessageFormatter.format(input)).isEqualTo(input);
    }

    /** Windows 换行符应转换为 IDE 提交框使用的统一换行符。 */
    @Test
    void shouldNormalizeCrLfLineEndings() {
        String input = "fix(changelog): 修复提交消息格式\r\n- 保留详细列表";

        assertThat(CommitMessageFormatter.format(input))
            .isEqualTo("fix(changelog): 修复提交消息格式\n\n- 保留详细列表");
    }

    /** 模型意外添加代码围栏时仍应正确补齐分隔空行。 */
    @Test
    void shouldFormatMessageWrappedInCodeFence() {
        String input = "```text\nfix(changelog): 修复提交消息格式\n- 保留详细列表\n```";

        assertThat(CommitMessageFormatter.format(input))
            .isEqualTo("fix(changelog): 修复提交消息格式\n\n- 保留详细列表");
    }
}
