package dev.dong4j.zeka.stack.idea.plugin.archiver.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EditableArchiveEntryTest {

    @Test
    void withUpdatedStateReturnsNewInstance() {
        EditableArchiveEntry entry = new EditableArchiveEntry(
            Path.of("/tmp/demo.zip"),
            "demo.txt",
            StandardCharsets.UTF_8,
            1L,
            2L,
            ArchiveFormat.ZIP,
            null,
            null
        );

        EditableArchiveEntry updated = entry.withUpdatedState(10L, 20L);
        assertThat(updated.archiveTimestamp()).isEqualTo(10L);
        assertThat(updated.crc()).isEqualTo(20L);
        assertThat(updated.archivePath()).isEqualTo(entry.archivePath());
        assertThat(updated.entryPath()).isEqualTo(entry.entryPath());
        assertThat(updated.format()).isEqualTo(entry.format());
    }
}

