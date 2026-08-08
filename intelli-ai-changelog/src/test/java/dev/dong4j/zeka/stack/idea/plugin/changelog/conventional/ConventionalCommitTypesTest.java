package dev.dong4j.zeka.stack.idea.plugin.changelog.conventional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConventionalCommitTypesTest {

    @Test
    void shouldExposePromptAlignedWhitelist() {
        assertThat(ConventionalCommitTypes.ALL).containsExactly(
            "feat", "fix", "refactor", "perf", "docs",
            "test", "build", "chore", "style", "revert"
        );
        assertThat(ConventionalCommitTypes.isStandard("feat")).isTrue();
        assertThat(ConventionalCommitTypes.isStandard("ci")).isFalse();
        assertThat(ConventionalCommitTypes.matchesPrefix("f"))
            .contains("feat", "fix");
        assertThat(ConventionalCommitTypes.description("docs")).isNotBlank();
        assertThat(ConventionalCommitTypes.description("feat")).isNotBlank();
    }
}