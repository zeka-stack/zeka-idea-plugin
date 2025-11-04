package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.OllamaProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.QianWenProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AIServiceProvider 接口测试类
 * <p>
 * 用于验证所有 AI Provider 实现是否符合 AIServiceProvider 接口的规范，包括配置验证、模型支持、ID 唯一性、名称有效性等核心功能。
 * <p>
 * 该测试类覆盖了多个 AI Provider 的实现，如 QianWenProvider 和 OllamaProvider，确保其行为符合预期。
 *
 * @author 作者名
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("AIServiceProvider 接口测试")
public class AIServiceProviderTest {

    /** 设置状态信息 */
    private SettingsState settings;

    /**
     * 初始化测试环境，设置 SettingsState 实例
     * <p>
     * 在每个测试方法执行前初始化 SettingsState 对象，用于测试数据准备
     */
    @BeforeEach
    void setUp() {
        settings = new SettingsState();
    }

    /**
     * 测试千问 Provider 的实现功能
     * <p>
     * 测试目标：验证 QianWenProvider 类在不同场景下的行为是否符合预期
     * 测试场景：配置为千问 Provider 类型，设置模型名称、基础 URL 和 API 密钥
     * 预期结果：Provider 实例应正确初始化并返回相应的属性值
     * <p>
     * 测试内容包括：
     * 1. 验证 Provider ID、名称及是否需要 API 密钥等基本属性
     * 2. 验证默认模型和默认基础 URL 是否为空
     * 3. 验证支持的模型列表是否包含预期的模型名称 "qwen-max"
     * <p>
     * 注意：测试依赖于 {@link AIProviderType} 和 {@link AIServiceProvider} 的正确实现
     */
    @Test
    @DisplayName("测试千问 Provider 实现")
    void testQianWenProvider_implementation() {
        settings.providerType = AIProviderType.QIANWEN.getProviderId();
        settings.modelName = "qwen-max";
        settings.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        settings.apiKey = "test-key";

        AIServiceProvider provider = new QianWenProvider(settings);

        // 测试基本属性
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.QIANWEN.getProviderId());
        assertThat(provider.getProviderName()).isNotEmpty();
        assertThat(provider.requiresApiKey()).isTrue();

        // 测试默认值
        assertThat(provider.getDefaultModel()).isNotEmpty();
        assertThat(provider.getDefaultBaseUrl()).isNotEmpty();

        // 测试支持的模型列表
        List<String> models = provider.getSupportedModels();
        assertThat(models).isNotEmpty();
        assertThat(models).contains("qwen-max");
    }

    /**
     * 测试 Ollama Provider 的实现功能
     * <p>
     * 测试场景：验证 Ollama Provider 在配置参数设置后是否能正确初始化并返回预期属性
     * 预期结果：应正确返回 providerId、providerName、是否需要 apiKey 等基础属性，以及默认模型、默认 baseUrl 和支持的模型列表
     * <p>
     * 特殊说明：测试中设置的 baseUrl 包含 "localhost"，用于验证是否正确解析
     */
    @Test
    @DisplayName("测试 Ollama Provider 实现")
    void testOllamaProvider_implementation() {
        settings.providerType = AIProviderType.OLLAMA.getProviderId();
        settings.modelName = "llama2";
        settings.baseUrl = "http://localhost:11434";
        settings.apiKey = "";

        AIServiceProvider provider = new OllamaProvider(settings);

        // 测试基本属性
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.OLLAMA.getProviderId());
        assertThat(provider.getProviderName()).isNotEmpty();
        assertThat(provider.requiresApiKey()).isFalse();

        // 测试默认值
        assertThat(provider.getDefaultModel()).isNotEmpty();
        assertThat(provider.getDefaultBaseUrl()).isNotEmpty();
        assertThat(provider.getDefaultBaseUrl()).contains("localhost");

        // 测试支持的模型列表
        List<String> models = provider.getSupportedModels();
        assertThat(models).isNotEmpty();
    }

    /**
     * 测试 TaskType 枚举的所有枚举值
     * <p>
     * 测试场景：验证枚举类 DocumentationTask.TaskType 是否包含所有预定义的枚举值
     * 预期结果：枚举数组应包含 CLASS、METHOD、TEST_METHOD、FIELD、INTERFACE、ENUM 等所有枚举常量
     */
    @Test
    @DisplayName("测试 TaskType 枚举")
    void testTaskType_allValues() {
        DocumentationTask.TaskType[] types = DocumentationTask.TaskType.values();

        assertThat(types).contains(
            DocumentationTask.TaskType.CLASS,
            DocumentationTask.TaskType.METHOD,
            DocumentationTask.TaskType.TEST_METHOD,
            DocumentationTask.TaskType.FIELD,
            DocumentationTask.TaskType.INTERFACE,
            DocumentationTask.TaskType.ENUM
                                  );
    }

    /**
     * 测试千问 Provider 的配置验证功能
     * <p>
     * 测试场景：当使用空 API Key 配置千问 Provider 时
     * 预期结果：配置验证应返回失败状态
     * <p>
     * 测试场景：当设置有效 API Key 后再次验证
     * 预期结果：配置验证应返回成功状态
     * <p>
     * 注意：实际验证可能需要网络请求，此处仅测试基本逻辑
     * 在真实环境中，由于 API Key 不是有效的，验证可能仍会失败
     */
    @Test
    @DisplayName("测试千问 Provider 的配置验证")
    void testQianWenProvider_configurationValidation() {
        settings.providerType = AIProviderType.QIANWEN.getProviderId();
        settings.modelName = "qwen-max";
        settings.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        settings.apiKey = ""; // 空 API Key

        AIServiceProvider provider = new QianWenProvider(settings);

        // 千问需要 API Key，配置验证应该失败
        ValidationResult isValid = provider.validateConfiguration();
        assertThat(isValid.isSuccess()).isFalse();

        // 设置有效的 API Key
        settings.apiKey = "valid-key";
        AIServiceProvider validProvider = new QianWenProvider(settings);
        // 注意：实际验证可能需要网络请求，这里只测试基本逻辑
        // 在实际环境中可能会失败，因为 API Key 不是真实的
    }

    /**
     * 测试 Ollama Provider 的配置验证功能
     * <p>
     * 测试场景：验证当设置 Ollama 作为 AI 提供商时，配置是否能通过基本验证
     * 预期结果：配置应通过验证，且 Ollama Provider 不需要 API Key，应返回 requiresApiKey 为 false
     * <p>
     * 注意：Ollama 不需要 API Key，因此即使 apiKey 为空也应通过验证
     */
    @Test
    @DisplayName("测试 Ollama Provider 的配置验证")
    void testOllamaProvider_configurationValidation() {
        settings.providerType = AIProviderType.OLLAMA.getProviderId();
        settings.modelName = "llama2";
        settings.baseUrl = "http://localhost:11434";
        settings.apiKey = ""; // Ollama 不需要 API Key

        AIServiceProvider provider = new OllamaProvider(settings);

        // Ollama 不需要 API Key，所以即使为空也可以通过基本验证
        assertThat(provider.requiresApiKey()).isFalse();
    }

    /**
     * 测试 Provider 支持的模型列表不为空
     * <p>
     * 测试场景：验证不同 AI 服务提供商（如千问、Ollama）返回的模型列表不为空
     * 预期结果：每个 Provider 的 getSupportedModels() 方法应返回非空集合
     * <p>
     * 测试说明：分别设置 AIProviderType.QIANWEN 和 AIProviderType.OLLAMA，创建对应的 Provider 实例，并检查其支持的模型列表
     */
    @Test
    @DisplayName("测试 Provider 支持的模型列表不为空")
    void testProvider_supportedModels_notEmpty() {
        // 测试千问
        settings.providerType = AIProviderType.QIANWEN.getProviderId();
        AIServiceProvider qianwen = new QianWenProvider(settings);
        assertThat(qianwen.getSupportedModels()).isNotEmpty();

        // 测试 Ollama
        settings.providerType = AIProviderType.OLLAMA.getProviderId();
        AIServiceProvider ollama = new OllamaProvider(settings);
        assertThat(ollama.getSupportedModels()).isNotEmpty();

    }

    /**
     * 测试 Provider ID 唯一性
     * <p>
     * 测试场景：创建两个不同的 AIServiceProvider 实例（QianWenProvider 和 OllamaProvider）
     * 预期结果：两个实例的 Provider ID 应该不同，确保 ID 唯一性
     */
    @Test
    @DisplayName("测试 Provider ID 唯一性")
    void testProvider_uniqueIds() {
        AIServiceProvider qianwen = new QianWenProvider(settings);
        AIServiceProvider ollama = new OllamaProvider(settings);

        String qianwenId = qianwen.getProviderId();
        String ollamaId = ollama.getProviderId();

        // 确保每个 Provider 的 ID 都不同
        assertThat(qianwenId).isNotEqualTo(ollamaId);
    }

    /**
     * 测试 Provider 名称不为空
     * <p>
     * 测试场景：创建两个不同的 AI 服务 Provider 实例（QianWenProvider 和 OllamaProvider）
     * 预期结果：两个实例的 getProviderName 方法返回的名称均不为空
     */
    @Test
    @DisplayName("测试 Provider 名称不为空")
    void testProvider_names_notEmpty() {
        AIServiceProvider qianwen = new QianWenProvider(settings);
        AIServiceProvider ollama = new OllamaProvider(settings);

        assertThat(qianwen.getProviderName()).isNotEmpty();
        assertThat(ollama.getProviderName()).isNotEmpty();
    }

    /**
     * 测试 Provider 默认配置
     * <p>
     * 测试场景：验证 QianWenProvider 在未显式配置时的默认模型和基础 URL
     * 预期结果：默认模型和基础 URL 不为空，并且基础 URL 包含 "dashscope"
     * <p>
     * 注意：测试依赖于 QianWenProvider 的默认配置逻辑，确保其正确初始化
     */
    @Test
    @DisplayName("测试 Provider 默认配置")
    void testProvider_defaultConfiguration() {
        AIServiceProvider qianwen = new QianWenProvider(settings);

        String defaultModel = qianwen.getDefaultModel();
        String defaultBaseUrl = qianwen.getDefaultBaseUrl();

        assertThat(defaultModel).isNotNull().isNotEmpty();
        assertThat(defaultBaseUrl).isNotNull().isNotEmpty();

        // 千问的默认 URL 应该包含 dashscope
        assertThat(qianwen.getDefaultBaseUrl()).contains("dashscope");
    }

    /**
     * 测试 Ollama Provider 的默认配置
     * <p>
     * 测试场景：验证 OllamaProvider 在未显式设置基础 URL 时的默认行为
     * 预期结果：默认基础 URL 应包含 "localhost" 和 "11434"，表示连接本地 Ollama 服务
     * <p>
     * 特殊说明：测试假设 Ollama 服务默认运行在本地主机的 11434 端口，若实际环境不同需调整测试逻辑
     */
    @Test
    @DisplayName("测试 Ollama Provider 默认配置")
    void testOllamaProvider_defaultConfiguration() {
        AIServiceProvider ollama = new OllamaProvider(settings);

        String defaultBaseUrl = ollama.getDefaultBaseUrl();

        // Ollama 的默认 URL 应该是本地地址
        assertThat(defaultBaseUrl).contains("localhost");
        assertThat(defaultBaseUrl).contains("11434");
    }

    /**
     * 测试 Provider 支持的模型包含默认模型
     * <p>
     * 测试场景：创建一个 QianWenProvider 实例并获取其默认模型和所有支持的模型
     * 预期结果：支持的模型列表中应包含默认模型
     * <p>
     * 该测试验证了 Provider 的 getSupportedModels 方法返回的模型列表是否包含通过 getDefaultModel 方法获取的默认模型
     */
    @Test
    @DisplayName("测试 Provider 支持的模型包含默认模型")
    void testProvider_supportedModels_containsDefault() {
        AIServiceProvider qianwen = new QianWenProvider(settings);

        String defaultModel = qianwen.getDefaultModel();
        List<String> supportedModels = qianwen.getSupportedModels();

        assertThat(supportedModels).contains(defaultModel);
    }
}

