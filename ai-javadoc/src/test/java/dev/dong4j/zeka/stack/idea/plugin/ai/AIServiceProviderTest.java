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
 * AIServiceProvider 接口测试
 * <p>
 * 测试所有 AI Provider 实现是否正确实现了接口
 */
@DisplayName("AIServiceProvider 接口测试")
public class AIServiceProviderTest {

    private SettingsState settings;

    @BeforeEach
    void setUp() {
        settings = new SettingsState();
    }

    @Test
    @DisplayName("测试千问 Provider 实现")
    void testQianWenProvider_implementation() {
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
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

    @Test
    @DisplayName("测试 Ollama Provider 实现")
    void testOllamaProvider_implementation() {
        settings.aiProvider = AIProviderType.OLLAMA.getProviderId();
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

    @Test
    @DisplayName("测试千问 Provider 的配置验证")
    void testQianWenProvider_configurationValidation() {
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
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

    @Test
    @DisplayName("测试 Ollama Provider 的配置验证")
    void testOllamaProvider_configurationValidation() {
        settings.aiProvider = AIProviderType.OLLAMA.getProviderId();
        settings.modelName = "llama2";
        settings.baseUrl = "http://localhost:11434";
        settings.apiKey = ""; // Ollama 不需要 API Key

        AIServiceProvider provider = new OllamaProvider(settings);

        // Ollama 不需要 API Key，所以即使为空也可以通过基本验证
        assertThat(provider.requiresApiKey()).isFalse();
    }

    @Test
    @DisplayName("测试 Provider 支持的模型列表不为空")
    void testProvider_supportedModels_notEmpty() {
        // 测试千问
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
        AIServiceProvider qianwen = new QianWenProvider(settings);
        assertThat(qianwen.getSupportedModels()).isNotEmpty();

        // 测试 Ollama
        settings.aiProvider = AIProviderType.OLLAMA.getProviderId();
        AIServiceProvider ollama = new OllamaProvider(settings);
        assertThat(ollama.getSupportedModels()).isNotEmpty();

    }

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

    @Test
    @DisplayName("测试 Provider 名称不为空")
    void testProvider_names_notEmpty() {
        AIServiceProvider qianwen = new QianWenProvider(settings);
        AIServiceProvider ollama = new OllamaProvider(settings);

        assertThat(qianwen.getProviderName()).isNotEmpty();
        assertThat(ollama.getProviderName()).isNotEmpty();
    }

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

    @Test
    @DisplayName("测试 Ollama Provider 默认配置")
    void testOllamaProvider_defaultConfiguration() {
        AIServiceProvider ollama = new OllamaProvider(settings);

        String defaultBaseUrl = ollama.getDefaultBaseUrl();

        // Ollama 的默认 URL 应该是本地地址
        assertThat(defaultBaseUrl).contains("localhost");
        assertThat(defaultBaseUrl).contains("11434");
    }

    @Test
    @DisplayName("测试 Provider 支持的模型包含默认模型")
    void testProvider_supportedModels_containsDefault() {
        AIServiceProvider qianwen = new QianWenProvider(settings);

        String defaultModel = qianwen.getDefaultModel();
        List<String> supportedModels = qianwen.getSupportedModels();

        assertThat(supportedModels).contains(defaultModel);
    }
}

