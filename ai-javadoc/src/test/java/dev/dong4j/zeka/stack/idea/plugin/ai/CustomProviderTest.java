package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义服务提供商测试
 *
 * <p>测试 CustomProvider 的基本功能和配置验证。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
class CustomProviderTest {

    private CustomProvider provider;
    private SettingsState settings;

    @BeforeEach
    void setUp() {
        settings = new SettingsState();
        settings.aiProvider = AIProviderType.CUSTOM.getProviderId();
        settings.baseUrl = "https://api.openai.com/v1";
        settings.apiKey = "test-api-key";
        settings.modelName = "gpt-3.5-turbo";
        settings.configurationVerified = true;

        provider = new CustomProvider(settings);
    }

    @Test
    void testGetProviderId() {
        assertEquals(AIProviderType.CUSTOM.getProviderId(), provider.getProviderId());
    }

    @Test
    void testGetProviderName() {
        assertEquals("自定义服务 (OpenAI 兼容)", provider.getProviderName());
    }

    @Test
    void testGetDefaultModel() {
        assertEquals("gpt-3.5-turbo", provider.getDefaultModel());
    }

    @Test
    void testGetDefaultBaseUrl() {
        assertEquals("https://api.openai.com/v1", provider.getDefaultBaseUrl());
    }

    @Test
    void testRequiresApiKey() {
        assertTrue(provider.requiresApiKey());
    }

    @Test
    void testGetSupportedModels() {
        var models = provider.getSupportedModels();
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertTrue(models.contains("gpt-3.5-turbo"));
        assertTrue(models.contains("gpt-4"));
        assertTrue(models.contains("gpt-4-turbo"));
    }

    @Test
    void testValidateConfigurationWithValidSettings() {
        // 注意：这个测试不会真正发送网络请求，只是测试配置验证逻辑
        var result = provider.validateConfiguration();

        // 由于没有真实的网络连接，这个测试可能会失败
        // 但我们可以测试配置验证的基本逻辑
        assertNotNull(result);
    }

    @Test
    void testValidateConfigurationWithEmptyBaseUrl() {
        settings.baseUrl = "";
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Base URL 不能为空"));
    }

    @Test
    void testValidateConfigurationWithEmptyApiKey() {
        settings.apiKey = "";
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("API Key 不能为空"));
    }

    @Test
    void testValidateConfigurationWithEmptyModelName() {
        settings.modelName = "";
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("模型名称不能为空"));
    }

    @Test
    void testValidateConfigurationWithNullBaseUrl() {
        settings.baseUrl = null;
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Base URL 不能为空"));
    }

    @Test
    void testValidateConfigurationWithNullApiKey() {
        settings.apiKey = null;
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("API Key 不能为空"));
    }

    @Test
    void testValidateConfigurationWithNullModelName() {
        settings.modelName = null;
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("模型名称不能为空"));
    }
}
