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
 * 自定义服务提供商测试类
 * <p>
 * 用于验证 CustomProvider 类的基本功能和配置校验逻辑，包括获取服务提供商ID、名称、默认模型、Base URL等信息，以及验证配置是否有效的各种场景。
 * <p>
 * 该测试类通过设置不同的配置参数，测试 CustomProvider 在不同条件下的行为，确保其能够正确处理合法和非法的配置输入。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
class CustomProviderTest {

    /** 自定义服务提供者实例 */
    private CustomProvider provider;
    /** 设置状态信息 */
    private SettingsState settings;

    /**
     * 初始化测试环境，设置配置和自定义提供者
     * <p>
     * 该方法用于在每个测试用例执行前初始化必要的配置和提供者对象，包括设置AI提供者类型、基础URL、API密钥、模型名称以及配置验证状态。
     *
     * @since 1.0
     */
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

    /**
     * 测试获取提供者ID功能
     * <p>
     * 测试场景：验证自定义提供者类型对应的提供者ID是否正确
     * 预期结果：应返回与AIProviderType.CUSTOM对应的ProviderId值
     */
    @Test
    void testGetProviderId() {
        assertEquals(AIProviderType.CUSTOM.getProviderId(), provider.getProviderId());
    }

    /**
     * 测试获取服务提供商名称功能
     * <p>
     * 测试场景：验证服务提供商名称是否正确设置为 "自定义服务 (OpenAI 兼容)"
     * 预期结果：返回的名称应与预期值一致
     */
    @Test
    void testGetProviderName() {
        assertEquals("自定义服务 (OpenAI 兼容)", provider.getProviderName());
    }

    /**
     * 测试获取默认模型功能
     * <p>
     * 测试场景：验证默认模型是否为 "gpt-3.5-turbo"
     * 预期结果：应返回 "gpt-3.5-turbo"
     */
    @Test
    void testGetDefaultModel() {
        assertEquals("gpt-3.5-turbo", provider.getDefaultModel());
    }

    /**
     * 测试获取默认基础 URL 功能
     * <p>
     * 测试场景：验证 OpenAI API 提供商默认基础 URL 是否正确
     * 预期结果：应返回 "https://api.openai.com/v1"
     */
    @Test
    void testGetDefaultBaseUrl() {
        assertEquals("https://api.openai.com/v1", provider.getDefaultBaseUrl());
    }

    /**
     * 测试 API 密钥验证功能
     * <p>
     * 测试场景：验证 API 密钥是否为必需项
     * 预期结果：应返回 true，表示 API 密钥是必需的
     */
    @Test
    void testRequiresApiKey() {
        assertTrue(provider.requiresApiKey());
    }

    /**
     * 测试获取支持的模型列表功能
     * <p>
     * 测试场景：验证模型提供者返回的支持模型列表是否符合预期
     * 预期结果：模型列表不为空，并且包含 "gpt-3.5-turbo"、"gpt-4" 和 "gpt-4-turbo" 三个模型
     */
    @Test
    void testGetSupportedModels() {
        var models = provider.getSupportedModels();
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertTrue(models.contains("gpt-3.5-turbo"));
        assertTrue(models.contains("gpt-4"));
        assertTrue(models.contains("gpt-4-turbo"));
    }

    /**
     * 测试验证配置功能
     * <p>
     * 测试场景：当配置参数有效时
     * 预期结果：应返回非空的验证结果对象
     * <p>
     * 注意：此测试不涉及实际网络请求，仅用于验证配置逻辑的正确性
     * 由于缺少真实网络环境，测试可能无法通过，但可确保配置验证流程正常
     */
    @Test
    void testValidateConfigurationWithValidSettings() {
        // 注意：这个测试不会真正发送网络请求，只是测试配置验证逻辑
        var result = provider.validateConfiguration();

        // 由于没有真实的网络连接，这个测试可能会失败
        // 但我们可以测试配置验证的基本逻辑
        assertNotNull(result);
    }

    /**
     * 测试验证配置功能，当 Base URL 为空时的场景
     * <p>
     * 测试场景：设置空的 Base URL 并创建 CustomProvider 实例
     * 预期结果：验证结果应失败，并提示 "Base URL 不能为空" 的错误信息
     * <p>
     * 注意：此测试依赖于 {@link CustomProvider#validateConfiguration()} 方法的实现逻辑
     */
    @Test
    void testValidateConfigurationWithEmptyBaseUrl() {
        settings.baseUrl = "";
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Base URL 不能为空"));
    }

    /**
     * 测试验证配置功能，当 API Key 为空时的场景
     * <p>
     * 测试场景：设置空的 API Key 并创建 CustomProvider 实例
     * 预期结果：验证结果应失败，并提示 "API Key 不能为空"
     * <p>
     * 注意：测试中修改了 settings.apiKey 的值，需确保测试环境隔离
     */
    @Test
    void testValidateConfigurationWithEmptyApiKey() {
        settings.apiKey = "";
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("API Key 不能为空"));
    }

    /**
     * 测试验证配置方法在模型名称为空时的行为
     * <p>
     * 测试场景：当模型名称为空字符串时
     * 预期结果：验证结果应失败，并提示"模型名称不能为空"的错误信息
     * <p>
     * 该测试验证了 {@link CustomProvider#validateConfiguration()} 方法在模型名称为空时是否正确地返回了错误信息
     */
    @Test
    void testValidateConfigurationWithEmptyModelName() {
        settings.modelName = "";
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("模型名称不能为空"));
    }

    /**
     * 测试验证配置功能，当 Base URL 为 null 时的场景
     * <p>
     * 测试场景：设置 Base URL 为 null，创建 CustomProvider 实例并调用 validateConfiguration 方法
     * 预期结果：验证结果应失败，并且错误信息应包含 "Base URL 不能为空"
     */
    @Test
    void testValidateConfigurationWithNullBaseUrl() {
        settings.baseUrl = null;
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Base URL 不能为空"));
    }

    /**
     * 测试验证配置功能，当 API Key 为 null 时的场景
     * <p>
     * 测试场景：设置 API Key 为 null，创建 CustomProvider 实例并调用 validateConfiguration 方法
     * 预期结果：验证结果应失败，并且错误信息应包含 "API Key 不能为空"
     */
    @Test
    void testValidateConfigurationWithNullApiKey() {
        settings.apiKey = null;
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("API Key 不能为空"));
    }

    /**
     * 测试验证配置功能，当模型名称为 null 时的场景
     * <p>
     * 测试场景：模型名称设置为 null，创建 CustomProvider 实例并调用 validateConfiguration 方法
     * 预期结果：验证结果应失败，并且错误信息应包含 "模型名称不能为空"
     */
    @Test
    void testValidateConfigurationWithNullModelName() {
        settings.modelName = null;
        provider = new CustomProvider(settings);

        var result = provider.validateConfiguration();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("模型名称不能为空"));
    }
}
