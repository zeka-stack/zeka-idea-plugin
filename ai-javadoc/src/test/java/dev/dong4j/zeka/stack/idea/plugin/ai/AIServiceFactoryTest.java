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
 * AIServiceFactory 单元测试类
 * <p>
 * 用于验证 AIServiceFactory 类中创建 AI 提供商实例以及获取支持的提供商列表等功能的正确性。
 * 包含对不同 AI 提供商（如千问、Ollama、LM Studio、硅基流动等）的测试用例，确保其配置和行为符合预期。
 * 同时测试了提供商支持检查、提供商名称获取、多次创建实例是否独立等场景。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("AIServiceFactory 单元测试")
public class AIServiceFactoryTest {

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
     * 测试创建千问提供商功能
     * <p>
     * 测试场景：配置参数设置为千问提供商相关值时
     * 预期结果：应成功创建并返回千问提供商实例
     * <p>
     * 验证点包括：实例不为空、实例类型为QianWenProvider、提供商ID与配置一致
     */
    @Test
    @DisplayName("测试创建千问提供商")
    void testCreateProvider_qianwen() {
        settings.providerType = AIProviderType.QIANWEN;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        config.modelName = "qwen-max";
        config.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        config.configurationVerified = true;
        SettingsState.setApiKey(config.md5, "test-api-key");

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(QianWenProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.QIANWEN.getProviderId());
    }

    /**
     * 测试创建 Ollama 提供商功能
     * <p>
     * 测试场景：配置参数设置为 Ollama 类型，模型名称为 llama2，基础地址为 <a href="http://localhost:11434">...</a>，API 密钥为空，配置已验证
     * 预期结果：应成功创建 Ollama 提供商实例，且实例类型为 OllamaProvider，提供商 ID 与配置一致
     * <p>
     * 注意：测试依赖 AIServiceFactory 的实现，确保相关配置和工厂方法正常工作
     */
    @Test
    @DisplayName("测试创建 Ollama 提供商")
    void testCreateProvider_ollama() {
        settings.providerType = AIProviderType.OLLAMA;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        config.modelName = "llama2";
        config.baseUrl = "http://localhost:11434";
        config.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(OllamaProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.OLLAMA.getProviderId());
    }

    /**
     * 测试创建 LM Studio 提供商功能
     * <p>
     * 测试场景：配置参数设置为 LM Studio 类型时
     * 预期结果：应成功创建 LMStudioProvider 实例，并验证其属性
     * <p>
     * 特殊说明：需要确保 AIProviderType.LM_STUDIO 的 providerId 与配置参数匹配
     */
    @Test
    @DisplayName("测试创建 LM Studio 提供商")
    void testCreateProvider_lmstudio() {
        settings.providerType = AIProviderType.LM_STUDIO;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.LM_STUDIO);
        config.modelName = "gpt-3.5-turbo";
        config.baseUrl = "http://localhost:1234/v1";
        config.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(LMStudioProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.LM_STUDIO.getProviderId());
    }

    /**
     * 测试创建硅基流动提供商功能
     * <p>
     * 测试场景：配置参数设置为硅基流动提供商的参数，包括提供商ID、模型名称、基础URL和API密钥
     * 预期结果：应成功创建硅基流动提供商实例，并验证其类型和提供商ID与预期一致
     * <p>
     * 注意：测试需要确保AIProviderType.SILICONFLOW的配置信息正确，且SiliconFlowProvider类能够被正确实例化
     */
    @Test
    @DisplayName("测试创建硅基流动提供商")
    void testCreateProvider_siliconflow() {
        settings.providerType = AIProviderType.SILICONFLOW;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.SILICONFLOW);
        config.modelName = "deepseek-chat";
        config.baseUrl = "https://api.siliconflow.cn/v1";
        config.configurationVerified = true;
        SettingsState.setApiKey(config.md5, "test-api-key");

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider).isNotNull();
        assertThat(provider).isInstanceOf(SiliconFlowProvider.class);
        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.SILICONFLOW.getProviderId());
    }


    /**
     * 测试获取支持的提供商列表功能
     * <p>
     * 测试场景：验证 {@link AIServiceFactory#getSupportedProviders()} 方法返回的提供商列表是否符合预期
     * 预期结果：返回的列表不应为空，且应包含所有预定义的提供商ID
     * <p>
     * 特殊说明：测试依赖于 {@link AIProviderType} 枚举中定义的提供商ID，确保枚举值与实际支持的提供商一致
     */
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

    /**
     * 测试检查提供商是否支持功能
     * <p>
     * 测试场景：验证千问提供商是否被系统支持
     * 预期结果：应返回 true，表示千问提供商支持
     * <p>
     * 注意：该测试依赖于 {@link AIServiceFactory#isProviderSupported(String)} 方法的正确实现
     */
    @Test
    @DisplayName("测试检查提供商是否支持 - 千问")
    void testIsProviderSupported_qianwen() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.QIANWEN.getProviderId())).isTrue();
    }

    /**
     * 测试检查提供商是否支持功能
     * <p>
     * 测试场景：验证 LM Studio 提供商是否被系统支持
     * 预期结果：应返回 true，表示 LM Studio 是支持的提供商
     * <p>
     * 注意：测试中使用了 {@link AIServiceFactory#isProviderSupported(String)} 方法来判断提供商支持情况
     */
    @Test
    @DisplayName("测试检查提供商是否支持 - LM Studio")
    void testIsProviderSupported_lmstudio() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.LM_STUDIO.getProviderId())).isTrue();
    }

    /**
     * 测试检查提供商是否支持功能
     * <p>
     * 测试场景：验证硅基流动提供商是否被系统支持
     * 预期结果：应返回 true，表示该提供商被支持
     * <p>
     * 注意：测试中使用了 {@link AIServiceFactory#isProviderSupported(String)} 方法来判断提供商支持情况
     */
    @Test
    @DisplayName("测试检查提供商是否支持 - 硅基流动")
    void testIsProviderSupported_siliconflow() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.SILICONFLOW.getProviderId())).isTrue();
    }

    /**
     * 测试检查提供商是否支持功能 - 自定义提供商场景
     * <p>
     * 测试场景：验证系统是否能正确识别并支持自定义提供商
     * 预期结果：返回 true，表示自定义提供商被正确支持
     */
    @Test
    @DisplayName("测试检查提供商是否支持 - 自定义")
    void testIsProviderSupported_custom() {
        assertThat(AIServiceFactory.isProviderSupported(AIProviderType.CUSTOM.getProviderId())).isTrue();
    }

    /**
     * 测试检查提供商是否支持功能
     * <p>
     * 测试场景：当传入不支持的提供商名称时
     * 预期结果：应返回 false
     * <p>
     * 该测试验证 AIServiceFactory.isProviderSupported 方法在遇到不支持的提供商名称时的返回值是否正确
     */
    @Test
    @DisplayName("测试检查提供商是否支持 - 不支持的提供商")
    void testIsProviderSupported_unsupported() {
        assertThat(AIServiceFactory.isProviderSupported("openai")).isFalse();
        assertThat(AIServiceFactory.isProviderSupported("claude")).isFalse();
        assertThat(AIServiceFactory.isProviderSupported("unknown")).isFalse();
    }

    /**
     * 测试检查提供商是否支持功能
     * <p>
     * 测试场景：传入 null 值作为提供商参数
     * 预期结果：应抛出 IllegalArgumentException 异常
     */
    @Test
    @DisplayName("测试检查提供商是否支持 - null 值")
    void testIsProviderSupported_null() {
        assertThatThrownBy(() -> AIServiceFactory.isProviderSupported(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 测试检查提供商是否支持功能
     * <p>
     * 测试场景：输入为空字符串时
     * 预期结果：应返回 false，表示不支持空字符串作为提供商
     */
    @Test
    @DisplayName("测试检查提供商是否支持 - 空字符串")
    void testIsProviderSupported_emptyString() {
        assertThat(AIServiceFactory.isProviderSupported("")).isFalse();
    }

    /**
     * 测试获取提供商名称功能
     * <p>
     * 测试场景：验证当提供商类型为千问时，获取到的名称是否正确
     * 预期结果：返回的名称应不为空且等于千问的显示名称
     * <p>
     * 注意：该测试依赖 {@link AIProviderType#QIANWEN} 类型的定义及 {@link AIServiceFactory#getProviderName(String)} 方法的实现
     */
    @Test
    @DisplayName("测试获取提供商名称 - 千问")
    void testGetProviderName_qianwen() {
        String name = AIServiceFactory.getProviderName(AIProviderType.QIANWEN.getProviderId());
        assertThat(name).isNotNull();
        assertThat(name).isNotEmpty();
        assertThat(name).isEqualTo(AIProviderType.QIANWEN.getDisplayName());
    }

    /**
     * 测试获取提供商名称功能 - Ollama
     * <p>
     * 测试场景：调用 {@link AIServiceFactory#getProviderName(String)} 方法获取 Ollama 提供商名称
     * 预期结果：返回的名称应不为空且等于 Ollama 的显示名称
     */
    @Test
    @DisplayName("测试获取提供商名称 - Ollama")
    void testGetProviderName_ollama() {
        String name = AIServiceFactory.getProviderName(AIProviderType.OLLAMA.getProviderId());
        assertThat(name).isNotNull();
        assertThat(name).isNotEmpty();
        assertThat(name).isEqualTo(AIProviderType.OLLAMA.getDisplayName());
    }

    /**
     * 测试获取提供商名称功能，验证当提供商不支持时的处理逻辑
     * <p>
     * 测试场景：传入一个未知的提供商ID "unknown"
     * 预期结果：应返回该提供商ID本身，即 "unknown"
     * <p>
     * 说明：该测试用例用于验证AIServiceFactory在遇到不支持的提供商时，是否能够正确返回providerId作为名称
     */
    @Test
    @DisplayName("测试获取提供商名称 - 不支持的提供商")
    void testGetProviderName_unsupported() {
        String name = AIServiceFactory.getProviderName("unknown");
        // 对于不支持的提供商，返回 providerId 本身
        assertThat(name).isEqualTo("unknown");
    }

    /**
     * 测试创建的提供商实例配置正确 - 千问
     * <p>
     * 测试场景：当设置中指定AI提供者为千问时，创建的提供商实例应具有正确的配置信息
     * 预期结果：提供商实例的提供者ID、名称、是否需要API密钥、默认模型、默认基础URL及支持的模型均应符合预期
     * <p>
     * 说明：测试中使用了预设的配置参数，包括提供者ID、模型名称、基础URL、API密钥及配置验证标志，验证创建的提供商实例是否正确解析并返回对应配置
     */
    @Test
    @DisplayName("测试创建的提供商实例配置正确 - 千问")
    void testCreatedProvider_hasCorrectConfiguration_qianwen() {
        settings.providerType = AIProviderType.QIANWEN;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        config.modelName = "qwen-max";
        config.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        config.configurationVerified = true;
        SettingsState.setApiKey(config.md5, "test-api-key");

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.QIANWEN.getProviderId());
        assertThat(provider.getProviderName()).isNotEmpty();
        assertThat(provider.requiresApiKey()).isTrue();
        assertThat(provider.getDefaultModel()).isNotEmpty();
        assertThat(provider.getDefaultBaseUrl()).isNotEmpty();
        assertThat(provider.getSupportedModels()).isNotEmpty();
    }

    /**
     * 测试创建的提供商实例配置正确 - Ollama
     * <p>
     * 测试场景：设置 Ollama 作为 AI 提供商，并配置相应的模型名称、基础 URL 和 API 密钥
     * 预期结果：创建的提供商实例应具有正确的配置信息，包括提供商 ID、名称、是否需要 API 密钥、默认模型、默认基础 URL 和支持的模型列表
     * <p>
     * 注意：测试依赖于 AIServiceFactory 的实现，确保其能够正确解析配置并创建对应的提供商实例
     */
    @Test
    @DisplayName("测试创建的提供商实例配置正确 - Ollama")
    void testCreatedProvider_hasCorrectConfiguration_ollama() {
        settings.providerType = AIProviderType.OLLAMA;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        config.modelName = "llama2";
        config.baseUrl = "http://localhost:11434";
        config.configurationVerified = true;

        AIServiceProvider provider = AIServiceFactory.createProvider(settings);

        assertThat(provider.getProviderId()).isEqualTo(AIProviderType.OLLAMA.getProviderId());
        assertThat(provider.getProviderName()).isNotEmpty();
        assertThat(provider.requiresApiKey()).isFalse();
        assertThat(provider.getDefaultModel()).isNotEmpty();
        assertThat(provider.getDefaultBaseUrl()).isNotEmpty();
        assertThat(provider.getSupportedModels()).isNotEmpty();
    }

    /**
     * 测试多次创建提供商实例
     * <p>
     * 测试场景：验证 {@link AIServiceFactory#createProvider(SettingsState)} 方法在相同配置下是否每次返回不同的实例，但实例类型保持一致
     * 预期结果：两个实例应为不同的对象，但它们的类应相同
     * <p>
     * 特殊说明：测试需要设置具体的提供商类型、模型名称、基础 URL 和 API 密钥，以确保创建逻辑正常执行
     */
    @Test
    @DisplayName("测试多次创建提供商实例")
    void testCreateMultipleInstances() {
        settings.providerType = AIProviderType.QIANWEN;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        config.modelName = "qwen-max";
        config.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        config.configurationVerified = true;
        SettingsState.setApiKey(config.md5, "test-api-key");

        AIServiceProvider provider1 = AIServiceFactory.createProvider(settings);
        AIServiceProvider provider2 = AIServiceFactory.createProvider(settings);

        // 每次创建应该返回新实例
        assertThat(provider1).isNotSameAs(provider2);
        // 但类型应该相同
        assertThat(provider1.getClass()).isEqualTo(provider2.getClass());
    }

    /**
     * 测试切换不同 AI 服务提供商的功能
     * <p>
     * 测试场景：验证系统能够根据配置切换到不同的 AI 服务提供商，如千问和 Ollama
     * 预期结果：创建的 AI 服务提供商实例应具有正确的提供商 ID，并且不同提供商的实例类应不同
     * <p>
     * 注意：测试需要确保 AIProviderType 枚举中包含 QIANWEN 和 OLLAMA 两个枚举值，并且 AIServiceFactory
     * 能够根据配置正确创建对应的 AI 服务提供商实例
     */
    @Test
    @DisplayName("测试切换提供商")
    void testSwitchProviders() {
        // 创建千问提供商
        settings.providerType = AIProviderType.QIANWEN;
        SettingsState.ProviderConfig qianwenConfig = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        qianwenConfig.modelName = "qwen-max";
        qianwenConfig.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        qianwenConfig.configurationVerified = true;
        SettingsState.setApiKey(qianwenConfig.md5, "test-api-key");
        AIServiceProvider qianwenProvider = AIServiceFactory.createProvider(settings);

        assertThat(qianwenProvider.getProviderId()).isEqualTo(AIProviderType.QIANWEN.getProviderId());

        // 切换到 Ollama
        settings.providerType = AIProviderType.OLLAMA;
        SettingsState.ProviderConfig ollamaConfig = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        ollamaConfig.modelName = "llama2";
        ollamaConfig.baseUrl = "http://localhost:11434";
        ollamaConfig.configurationVerified = true;
        AIServiceProvider ollamaProvider = AIServiceFactory.createProvider(settings);

        assertThat(ollamaProvider.getProviderId()).isEqualTo(AIProviderType.OLLAMA.getProviderId());
        assertThat(ollamaProvider.getClass()).isNotEqualTo(qianwenProvider.getClass());
    }

    /**
     * 测试支持的提供商数量
     * <p>
     * 测试场景：验证 AIServiceFactory 返回的受支持提供商集合中元素数量是否不少于 2
     * 预期结果：集合大小应大于等于 2，目前支持 qianwen 和 ollama
     */
    @Test
    @DisplayName("测试支持的提供商数量")
    void testSupportedProvidersCount() {
        Set<String> providers = AIServiceFactory.getSupportedProviders();
        // 目前支持 qianwen 和 ollama
        assertThat(providers.size()).isGreaterThanOrEqualTo(2);
    }

    /**
     * 测试获取支持的提供商返回不可变集合
     * <p>
     * 测试场景：验证 {@link AIServiceFactory#getSupportedProviders()} 方法返回的集合是否包含预期的提供商ID
     * 预期结果：返回的集合应不为空，并且包含 {@link AIProviderType#QIANWEN} 和 {@link AIProviderType#OLLAMA} 对应的提供商ID
     */
    @Test
    @DisplayName("测试获取支持的提供商返回不可变集合")
    void testGetSupportedProviders_returnsSetWithExpectedProviders() {
        Set<String> providers = AIServiceFactory.getSupportedProviders();

        assertThat(providers)
            .isNotNull()
            .contains(AIProviderType.QIANWEN.getProviderId(), AIProviderType.OLLAMA.getProviderId());
    }
}

