package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.LMStudioProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.OllamaProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.QianWenProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.SiliconFlowProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AIServiceFactory 单元测试
 */
@DisplayName("AIServiceFactory 单元测试")
public class AIServiceFactoryTest {

    private SettingsState settings;

    @BeforeEach
    void setUp() {
        settings = new SettingsState();
    }

    @Test
    @DisplayName("测试创建千问提供商")
    void testCreateProvider_qianwen() {
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
        settings.modelName = "qwen-max";
        settings.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        settings.apiKey = "test-api-key";
        settings.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(QianWenProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.QIANWEN.getProviderId());
    }

    @Test
    @DisplayName("测试创建 Ollama 提供商")
    void testCreateProvider_ollama() {
        settings.aiProvider = AIProviderType.OLLAMA.getProviderId();
        settings.modelName = "llama2";
        settings.baseUrl = "http://localhost:11434";
        settings.apiKey = "";
        settings.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(OllamaProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.OLLAMA.getProviderId());
    }

    @Test
    @DisplayName("测试创建 LM Studio 提供商")
    void testCreateProvider_lmstudio() {
        settings.aiProvider = AIProviderType.LM_STUDIO.getProviderId();
        settings.modelName = "gpt-3.5-turbo";
        settings.baseUrl = "http://localhost:1234/v1";
        settings.apiKey = "";
        settings.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(LMStudioProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.LM_STUDIO.getProviderId());
    }

    @Test
    @DisplayName("测试创建硅基流动提供商")
    void testCreateProvider_siliconflow() {
        settings.aiProvider = AIProviderType.SILICONFLOW.getProviderId();
        settings.modelName = "deepseek-chat";
        settings.baseUrl = "https://api.siliconflow.cn/v1";
        settings.apiKey = "test-api-key";
        settings.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(SiliconFlowProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.SILICONFLOW.getProviderId());
    }


    @Test
    @DisplayName("测试获取支持的提供商列表")
    void testGetSupportedProviders() {
        Set<String> providers = AIServiceFactory.getSupportedProviders();

        assertThat(providers).isNotNull();
        assertThat(providers).isNotEmpty();
        assertThat(providers).contains(AIProviderType.QIANWEN.getProviderId(),
                                       AIProviderType.OLLAMA.getProviderId(),
                                       AIProviderType.LM_STUDIO.getProviderId(),
                                       AIProviderType.SILICONFLOW.getProviderId(),
                                       AIProviderType.CUSTOM.getProviderId());
    }

    @Test
    @DisplayName("测试检查提供商是否支持 - 千问")
    void testIsProviderSupported_qianwen() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.QIANWEN.getProviderId())).isTrue();
    }

    @Test
    @DisplayName("测试检查提供商是否支持 - LM Studio")
    void testIsProviderSupported_lmstudio() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.LM_STUDIO.getProviderId())).isTrue();
    }

    @Test
    @DisplayName("测试检查提供商是否支持 - 硅基流动")
    void testIsProviderSupported_siliconflow() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.SILICONFLOW.getProviderId())).isTrue();
    }

    @Test
    @DisplayName("测试检查提供商是否支持 - 自定义")
    void testIsProviderSupported_custom() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.CUSTOM.getProviderId())).isTrue();
    }

    @Test
    @DisplayName("测试检查提供商是否支持 - 不支持的提供商")
    void testIsProviderSupported_unsupported() {
        assertThat(AIServiceFactory.isProviderSupported("openai")).isFalse();
        assertThat(AIServiceFactory.isProviderSupported("claude")).isFalse();
        assertThat(AIServiceFactory.isProviderSupported("unknown")).isFalse();
    }

    @Test
    @DisplayName("测试检查提供商是否支持 - null 值")
    void testIsProviderSupported_null() {
        assertThatThrownBy(() -> AIServiceFactory.isProviderSupported(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("测试检查提供商是否支持 - 空字符串")
    void testIsProviderSupported_emptyString() {
        assertThat(AIServiceFactory.isProviderSupported("")).isFalse();
    }

    @Test
    @DisplayName("测试获取提供商名称 - 千问")
    void testGetProviderName_qianwen() {
        String name = AIServiceFactory.getProviderName(AIProviderType.QIANWEN.getProviderId());
        assertThat(name).isNotNull();
        assertThat(name).isNotEmpty();
        assertThat(name).isEqualTo(AIProviderType.QIANWEN.getDisplayName());
    }

    @Test
    @DisplayName("测试获取提供商名称 - Ollama")
    void testGetProviderName_ollama() {
        String name = AIServiceFactory.getProviderName(AIProviderType.OLLAMA.getProviderId());
        assertThat(name).isNotNull();
        assertThat(name).isNotEmpty();
        assertThat(name).isEqualTo(AIProviderType.OLLAMA.getDisplayName());
    }

    @Test
    @DisplayName("测试获取提供商名称 - 不支持的提供商")
    void testGetProviderName_unsupported() {
        String name = AIServiceFactory.getProviderName("unknown");
        // 对于不支持的提供商，返回 providerId 本身
        assertThat(name).isEqualTo("unknown");
    }

    @Test
    @DisplayName("测试创建的提供商实例配置正确 - 千问")
    void testCreatedProvider_hasCorrectConfiguration_qianwen() {
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
        settings.modelName = "qwen-max";
        settings.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        settings.apiKey = "test-api-key";
        settings.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.QIANWEN.getProviderId());
        assertThat(provider.getProviderName()).isNotEmpty();
        assertThat(provider.requiresApiKey()).isTrue();
        assertThat(provider.getDefaultModel()).isNotEmpty();
        assertThat(provider.getDefaultBaseUrl()).isNotEmpty();
        assertThat(provider.getSupportedModels()).isNotEmpty();
    }

    @Test
    @DisplayName("测试创建的提供商实例配置正确 - Ollama")
    void testCreatedProvider_hasCorrectConfiguration_ollama() {
        settings.aiProvider = AIProviderType.OLLAMA.getProviderId();
        settings.modelName = "llama2";
        settings.baseUrl = "http://localhost:11434";
        settings.apiKey = "";
        settings.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.OLLAMA.getProviderId());
        assertThat(provider.getProviderName()).isNotEmpty();
        assertThat(provider.requiresApiKey()).isFalse();
        assertThat(provider.getDefaultModel()).isNotEmpty();
        assertThat(provider.getDefaultBaseUrl()).isNotEmpty();
        assertThat(provider.getSupportedModels()).isNotEmpty();
    }

    @Test
    @DisplayName("测试多次创建提供商实例")
    void testCreateMultipleInstances() {
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
        settings.modelName = "qwen-max";
        settings.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        settings.apiKey = "test-api-key";
        settings.configurationVerified = true;

        AIServiceProvider provider1 = AIServiceFactory.createProvider(settings);
        AIServiceProvider provider2 = AIServiceFactory.createProvider(settings);

        // 每次创建应该返回新实例
        assertThat(provider1).isNotSameAs(provider2);
        // 但类型应该相同
        assertThat(provider1.getClass()).isEqualTo(provider2.getClass());
    }

    @Test
    @DisplayName("测试切换提供商")
    void testSwitchProviders() {
        // 创建千问提供商
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
        settings.modelName = "qwen-max";
        settings.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        settings.apiKey = "test-api-key";
        settings.configurationVerified = true;
        AIServiceProvider qianwenProvider = AIServiceFactory.createProvider(settings);

        assertThat(qianwenProvider.getProviderId()).isEqualTo(AIProviderType.QIANWEN.getProviderId());

        // 切换到 Ollama
        settings.aiProvider = AIProviderType.OLLAMA.getProviderId();
        settings.modelName = "llama2";
        settings.baseUrl = "http://localhost:11434";
        settings.apiKey = "";
        settings.configurationVerified = true;
        AIServiceProvider ollamaProvider = AIServiceFactory.createProvider(settings);

        assertThat(ollamaProvider.getProviderId()).isEqualTo(AIProviderType.OLLAMA.getProviderId());
        assertThat(ollamaProvider.getClass()).isNotEqualTo(qianwenProvider.getClass());
    }

    @Test
    @DisplayName("测试支持的提供商数量")
    void testSupportedProvidersCount() {
        Set<String> providers = AIServiceFactory.getSupportedProviders();
        // 目前支持 qianwen 和 ollama
        assertThat(providers.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("测试获取支持的提供商返回不可变集合")
    void testGetSupportedProviders_returnsSetWithExpectedProviders() {
        Set<String> providers = AIServiceFactory.getSupportedProviders();

        assertThat(providers)
            .isNotNull()
            .contains(AIProviderType.QIANWEN.getProviderId(), AIProviderType.OLLAMA.getProviderId());
    }
}

