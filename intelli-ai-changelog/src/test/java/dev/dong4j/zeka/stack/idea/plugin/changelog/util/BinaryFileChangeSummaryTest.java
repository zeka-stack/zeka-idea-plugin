package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.changelog.model.CodeDiff;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 二进制变更英文路径摘要测试
 *
 * @author dong4j
 * @version 2026.2.1002
 * @since 2026.2.1002
 */
class BinaryFileChangeSummaryTest {

    @Test
    void shouldAppendEnglishPathOnlyLines() {
        CodeDiff added = new CodeDiff("assets/logo.png", CodeDiff.ChangeType.ADD, 0, 0, null, null, null);
        added.binary = true;
        CodeDiff modified = new CodeDiff("docs/manual.pdf", CodeDiff.ChangeType.MODIFY, 0, 0, null, null, null);
        modified.binary = true;
        CodeDiff deleted = new CodeDiff("images/old.jpg", CodeDiff.ChangeType.DELETE, 0, 0, null, null, null);
        deleted.binary = true;
        CodeDiff text = new CodeDiff("src/Main.java", CodeDiff.ChangeType.MODIFY, 1, 1, "+x", null, null);

        StringBuilder summary = new StringBuilder();
        BinaryFileChangeSummary.append(summary, List.of(added, modified, deleted, text), 10_000);

        assertThat(summary.toString()).isEqualTo(
            """
                Binary file changes (paths only, no content):
                - Added: assets/logo.png
                - Modified: docs/manual.pdf
                - Deleted: images/old.jpg

                """);
    }

    @Test
    void shouldSkipWhenNoBinaryDiffs() {
        CodeDiff text = new CodeDiff("src/Main.java", CodeDiff.ChangeType.MODIFY, 1, 0, "+x", null, null);
        StringBuilder summary = new StringBuilder();
        BinaryFileChangeSummary.append(summary, List.of(text), 10_000);
        assertThat(summary).isEmpty();
    }
}
