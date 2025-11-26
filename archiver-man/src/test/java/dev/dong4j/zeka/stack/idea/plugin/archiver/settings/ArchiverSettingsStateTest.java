package dev.dong4j.zeka.stack.idea.plugin.archiver.settings;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchiverSettingsStateTest {

    @Test
    void maxEditableBytesRespectsMinimum() {
        ArchiverSettingsState state = new ArchiverSettingsState();
        state.maxEditableFileSizeMb = 0;
        assertThat(state.maxEditableBytes()).isEqualTo(1024L * 1024L);
        state.maxEditableFileSizeMb = 8;
        assertThat(state.maxEditableBytes()).isEqualTo(8 * 1024L * 1024L);
    }
}

