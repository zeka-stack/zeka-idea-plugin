package dev.dong4j.zeka.stack.idea.plugin.terminal.action;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Tab 抢占策略: 只有终端内且存在有效 AI 触发输入时才占用 Tab,
 * 其余场景必须让出, 避免覆盖编辑器缩进 / Live Template / 终端补全.
 */
class TerminalAiTabClaimPolicyTest {

    @Test
    void shouldClaimTabOnlyWhenEnabledInTerminalWithAiInput() {
        assertThat(TerminalAiTabClaimPolicy.shouldClaimTab(true, true, "列出 java 文件")).isTrue();
    }

    @Test
    void shouldYieldTabWhenNotInTerminal() {
        assertThat(TerminalAiTabClaimPolicy.shouldClaimTab(true, false, "列出 java 文件")).isFalse();
    }

    @Test
    void shouldYieldTabWhenTerminalAiDisabled() {
        assertThat(TerminalAiTabClaimPolicy.shouldClaimTab(false, true, "列出 java 文件")).isFalse();
    }

    @Test
    void shouldYieldTabWhenAiInputMissing() {
        assertThat(TerminalAiTabClaimPolicy.shouldClaimTab(true, true, null)).isFalse();
        assertThat(TerminalAiTabClaimPolicy.shouldClaimTab(true, true, "   ")).isFalse();
    }

    @Test
    void shouldExtractContentAfterTriggerPrefix() {
        assertThat(TerminalAiTabClaimPolicy.extractTriggerContent("#", "# 列出 java 文件"))
            .isEqualTo("列出 java 文件");
    }

    @Test
    void shouldReturnNullWhenLineHasNoTriggerPrefix() {
        assertThat(TerminalAiTabClaimPolicy.extractTriggerContent("#", "ls -la")).isNull();
        assertThat(TerminalAiTabClaimPolicy.extractTriggerContent("#", "git status")).isNull();
    }

    @Test
    void shouldReturnNullWhenPrefixHasNoContent() {
        assertThat(TerminalAiTabClaimPolicy.extractTriggerContent("#", "#")).isNull();
        assertThat(TerminalAiTabClaimPolicy.extractTriggerContent("#", "#   ")).isNull();
    }

    @Test
    void shouldUseWholeLineWhenPrefixIsEmpty() {
        assertThat(TerminalAiTabClaimPolicy.extractTriggerContent("", "ls -la")).isEqualTo("ls -la");
        assertThat(TerminalAiTabClaimPolicy.extractTriggerContent("  ", "echo hi")).isEqualTo("echo hi");
    }
}
