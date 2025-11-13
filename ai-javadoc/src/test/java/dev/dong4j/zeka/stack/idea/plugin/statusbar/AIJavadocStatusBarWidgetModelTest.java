package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidgetModel;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link AIProviderStatusBarWidgetModel} 单元测试。
 *
 * <p>覆盖以下核心场景：
 * <ul>
 *   <li>当前默认服务商名称获取</li>
 *   <li>候选服务商列表构建与去重</li>
 *   <li>默认服务商切换逻辑</li>
 * </ul>
 */
class AIJavadocStatusBarWidgetModelTest {

    private SettingsState settings;

    @BeforeEach
    void setUp() {
        settings = new SettingsState();
        settings.availableProviders.clear();
        settings.defaultProviders.clear();
        settings.providerType = AIProviderType.QIANWEN;
        settings.defaultProviders.put(AIProviderType.QIANWEN,
                                      createProviderConfig("default-qwen", AIProviderType.QIANWEN, true));
    }

    @Test
    void shouldReturnCurrentProviderDisplayName() {
        String displayName = AIProviderStatusBarWidgetModel.getCurrentProviderDisplayName(settings);
        String expected = AIProviderType.QIANWEN.getDisplayName() + ":" + AIProviderType.QIANWEN.getDefaultModel();
        assertEquals(expected, displayName);
    }

    @Test
    void shouldBuildVerifiedProviderItemsIncludingDuplicates() {
        settings.availableProviders.add(createProviderConfig("id-1", AIProviderType.QIANWEN, true));
        settings.availableProviders.add(createProviderConfig("id-2", AIProviderType.OLLAMA, true));
        settings.availableProviders.add(createProviderConfig("id-3", AIProviderType.QIANWEN, true));
        settings.availableProviders.add(createProviderConfig("id-4", AIProviderType.SILICONFLOW, false));

        List<SettingsState.ProviderConfig> items = AIProviderStatusBarWidgetModel.buildProviderItems(settings);

        assertEquals(3, items.size(), "All verified providers should remain even for the same provider type");
        assertSame(AIProviderType.QIANWEN, items.get(0).providerType);
        assertSame(AIProviderType.OLLAMA, items.get(1).providerType);
        assertSame(AIProviderType.QIANWEN, items.get(2).providerType);
    }

    @Test
    void shouldSwitchDefaultProviderAndCloneConfig() {
        SettingsState.ProviderConfig providerConfig = createProviderConfig("id-5", AIProviderType.OLLAMA, true);

        AIProviderStatusBarWidgetModel.switchDefaultProvider(settings, providerConfig);

        assertSame(AIProviderType.OLLAMA, settings.providerType);

        SettingsState.ProviderConfig storedConfig = settings.defaultProviders.get(AIProviderType.OLLAMA);
        assertNotNull(storedConfig, "Default provider config should be stored");
        assertEquals(providerConfig.md5, storedConfig.md5);
        assertEquals(providerConfig.modelName, storedConfig.modelName);
        assertNotSame(providerConfig, storedConfig, "Stored config must be a deep copy");
    }

    @Test
    void shouldLocateIndexByMatchingMd5WhenDuplicatesExist() {
        SettingsState.ProviderConfig first = createProviderConfig("id-1", AIProviderType.QIANWEN, true);
        SettingsState.ProviderConfig second = createProviderConfig("id-2", AIProviderType.QIANWEN, true);

        List<SettingsState.ProviderConfig> items = List.of(first, second);

        settings.defaultProviders.put(AIProviderType.QIANWEN, second);
        int index = AIProviderStatusBarWidgetModel.findCurrentProviderIndex(items, settings);

        assertEquals(1, index, "Should match default config by md5 when duplicates exist");
    }

    @Test
    void shouldFallbackToFirstMatchedProviderWhenMd5NotFound() {
        SettingsState.ProviderConfig first = createProviderConfig("id-1", AIProviderType.QIANWEN, true);
        SettingsState.ProviderConfig second = createProviderConfig("id-2", AIProviderType.QIANWEN, true);

        List<SettingsState.ProviderConfig> items = List.of(first, second);

        settings.defaultProviders.put(AIProviderType.QIANWEN,
                                      createProviderConfig("other", AIProviderType.QIANWEN, true));
        int index = AIProviderStatusBarWidgetModel.findCurrentProviderIndex(items, settings);

        assertEquals(0, index, "Should fallback to the first provider with the same type when md5 differs");
    }

    private static SettingsState.ProviderConfig createProviderConfig(String md5,
                                                                     AIProviderType providerType,
                                                                     boolean verified) {
        SettingsState.ProviderConfig config = new SettingsState.ProviderConfig();
        config.md5 = md5;
        config.providerType = providerType;
        config.modelName = providerType.getDefaultModel();
        config.baseUrl = providerType.getDefaultBaseUrl();
        config.configurationVerified = verified;
        return config;
    }
}
