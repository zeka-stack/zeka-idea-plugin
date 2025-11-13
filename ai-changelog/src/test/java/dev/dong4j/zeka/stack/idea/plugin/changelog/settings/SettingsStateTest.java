package dev.dong4j.zeka.stack.idea.plugin.changelog.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SettingsState 单元测试
 */
class SettingsStateTest {

    private SettingsState settingsState;

    @BeforeEach
    void setUp() {
        settingsState = new SettingsState();
    }

    @Test
    void testDefaultValues() {
        // Then
        assertThat(settingsState.getExampleText()).isEqualTo("Hello World");
        assertThat(settingsState.isEnableFeature()).isTrue();
        assertThat(settingsState.getMaxItems()).isEqualTo(100);
        assertThat(settingsState.getCustomPath()).isEmpty();
        assertThat(settingsState.isDebugMode()).isFalse();
        assertThat(settingsState.getTimeout()).isEqualTo(5000);
    }

    @Test
    void testSettersAndGetters() {
        // When
        settingsState.setExampleText("Test Text");
        settingsState.setEnableFeature(false);
        settingsState.setMaxItems(50);
        settingsState.setCustomPath("/custom/path");
        settingsState.setDebugMode(true);
        settingsState.setTimeout(10000);

        // Then
        assertThat(settingsState.getExampleText()).isEqualTo("Test Text");
        assertThat(settingsState.isEnableFeature()).isFalse();
        assertThat(settingsState.getMaxItems()).isEqualTo(50);
        assertThat(settingsState.getCustomPath()).isEqualTo("/custom/path");
        assertThat(settingsState.isDebugMode()).isTrue();
        assertThat(settingsState.getTimeout()).isEqualTo(10000);
    }

    @Test
    void testLoadState() {
        // Given
        SettingsState newState = new SettingsState();
        newState.setExampleText("New Text");
        newState.setEnableFeature(false);

        // When
        settingsState.loadState(newState);

        // Then
        assertThat(settingsState.getExampleText()).isEqualTo("New Text");
        assertThat(settingsState.isEnableFeature()).isFalse();
    }
}
