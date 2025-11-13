package dev.dong4j.zeka.stack.idea.plugin.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SettingsState 测试类
 * <p>
 * 用于验证 SettingsState 类的配置属性和相关方法的正确性，包括默认值设置、配置有效性校验、语言支持检测、配置复制、
 * 默认 Prompt 模板获取、状态持久化与加载等功能。该类主要作为单元测试使用，确保 SettingsState 的行为符合预期。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@DisplayName("SettingsState 单元测试")
public class SettingsStateTest {

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
     * 测试默认配置值
     * <p>
     * 测试场景：验证配置类在未显式设置时的默认值
     * 预期结果：所有配置项应具有预设的默认值
     * <p>
     * 说明：该测试用例用于确保配置类初始化时各项参数的默认行为符合预期，包括基础配置、功能配置、高级配置以及支持的语言列表。
     */
    @Test
    @DisplayName("测试默认配置值")
    void testDefaultValues() {
        assertThat(settings.providerType).isEqualTo(AIProviderType.QIANWEN);

        // 从 defaultProviders 获取配置
        SettingsState.ProviderConfig defaultConfig = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        assertThat(defaultConfig.modelName).isEqualTo("qwen3-8b");
        assertThat(defaultConfig.baseUrl).isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");

        // API Key 从 PasswordSafe 获取
        String apiKey = SettingsState.getApiKey(defaultConfig.md5);
        assertThat(apiKey).isNullOrEmpty();

        // 功能配置
        assertThat(settings.generateForClass).isTrue();
        assertThat(settings.generateForMethod).isTrue();
        assertThat(settings.generateForField).isTrue();
        assertThat(settings.overrideExisting).isFalse();
        // 高级配置
        assertThat(settings.maxRetries).isEqualTo(3);
        assertThat(settings.timeout).isEqualTo(30000);
        assertThat(settings.waitDuration).isEqualTo(5000);
        assertThat(settings.temperature).isEqualTo(0.1);
        assertThat(settings.maxTokens).isEqualTo(1000);
        assertThat(settings.topP).isEqualTo(0.9);
        assertThat(settings.topK).isEqualTo(50);
        assertThat(settings.presencePenalty).isEqualTo(0.0);
        assertThat(settings.performanceMode).isFalse();
        assertThat(settings.verboseLogging).isFalse();

        // 支持的语言
        assertThat(settings.supportedLanguages).containsOnly("java");
    }

    /**
     * 测试 ProviderConfig 的基本功能
     * <p>
     * 测试场景：创建和获取 ProviderConfig 配置
     * 预期结果：能够正确创建和获取配置信息
     */
    @Test
    @DisplayName("测试 ProviderConfig 配置管理")
    void testProviderConfig() {
        settings.providerType = AIProviderType.QIANWEN;

        // 获取或创建配置
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        config.modelName = "qwen-max";
        config.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

        // 设置 API Key
        SettingsState.setApiKey(config.md5, "valid-api-key");

        // 验证配置
        assertThat(config.providerType).isEqualTo(AIProviderType.QIANWEN);
        assertThat(config.modelName).isEqualTo("qwen-max");
        assertThat(config.baseUrl).isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");

        // 验证 API Key
        String retrievedApiKey = SettingsState.getApiKey(config.md5);
        assertThat(retrievedApiKey).isEqualTo("valid-api-key");
    }

    /**
     * 测试不同提供商的配置隔离
     * <p>
     * 测试场景：为不同的 AI 提供商设置不同的配置
     * 预期结果：配置应该相互独立，互不影响
     */
    @Test
    @DisplayName("测试不同提供商的配置隔离")
    void testProviderConfigIsolation() {
        // 配置 QianWen
        SettingsState.ProviderConfig qianwenConfig = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        qianwenConfig.modelName = "qwen-max";
        qianwenConfig.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        SettingsState.setApiKey(qianwenConfig.md5, "qianwen-key");

        // 配置 Ollama
        SettingsState.ProviderConfig ollamaConfig = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        ollamaConfig.modelName = "llama2";
        ollamaConfig.baseUrl = "http://localhost:11434";

        // 验证配置隔离
        assertThat(qianwenConfig.modelName).isEqualTo("qwen-max");
        assertThat(ollamaConfig.modelName).isEqualTo("llama2");
        assertThat(qianwenConfig.baseUrl).isNotEqualTo(ollamaConfig.baseUrl);

        // 验证 API Key 隔离
        assertThat(SettingsState.getApiKey(qianwenConfig.md5)).isEqualTo("qianwen-key");
        assertThat(SettingsState.getApiKey(ollamaConfig.md5)).isNullOrEmpty();
    }

    /**
     * 测试语言支持检测功能
     * <p>
     * 测试场景：验证 settings 对象是否能正确识别不同大小写形式的语言名称
     * 预期结果：对于 "java"、"Java"、"JAVA" 应返回 true，对于 "kotlin"、"python" 应返回 false
     * <p>
     * 说明：此测试用例用于确保语言支持检测逻辑不区分大小写，并且能正确识别支持的语言
     */
    @Test
    @DisplayName("测试语言支持检测")
    void testIsLanguageSupported() {
        assertThat(settings.isLanguageSupported("java")).isTrue();
        assertThat(settings.isLanguageSupported("Java")).isTrue();
        assertThat(settings.isLanguageSupported("JAVA")).isTrue();
        assertThat(settings.isLanguageSupported("kotlin")).isFalse();
        assertThat(settings.isLanguageSupported("python")).isFalse();
    }

    /**
     * 测试重置为默认配置功能
     * <p>
     * 测试场景：修改配置后调用重置方法
     * 预期结果：所有配置项应恢复为默认值
     * <p>
     * 特殊说明：测试需要先手动设置配置项为非默认值，再调用 resetToDefaults 方法进行验证
     */
    @Test
    @DisplayName("测试重置为默认配置")
    void testResetToDefaults() {
        // 修改配置
        settings.providerType = AIProviderType.OLLAMA;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        config.modelName = "llama2";
        config.baseUrl = "http://localhost:11434";
        SettingsState.setApiKey(config.md5, "test-key");
        settings.generateForClass = false;
        settings.generateForMethod = false;
        settings.generateForField = false;
        settings.maxRetries = 5;
        settings.temperature = 0.5;
        settings.topP = 0.5;
        settings.topK = 20;
        settings.presencePenalty = 0.5;

        // 重置
        settings.resetToDefaults();

        // 验证
        assertThat(settings.providerType).isEqualTo(AIProviderType.QIANWEN);
        SettingsState.ProviderConfig defaultConfig = settings.getDefaultProviderConfig(AIProviderType.QIANWEN);
        assertThat(defaultConfig.modelName).isEqualTo("qwen3-8b");
        assertThat(defaultConfig.baseUrl).isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
        assertThat(settings.generateForClass).isTrue();
        assertThat(settings.generateForMethod).isTrue();
        assertThat(settings.generateForField).isTrue();
        assertThat(settings.maxRetries).isEqualTo(3);
        assertThat(settings.temperature).isEqualTo(0.1);
        assertThat(settings.topP).isEqualTo(0.9);
        assertThat(settings.topK).isEqualTo(50);
        assertThat(settings.presencePenalty).isEqualTo(0.0);
    }

    /**
     * 测试配置复制功能
     * <p>
     * 测试场景：验证 SettingsState 对象的 copy 方法是否能够正确复制原始配置对象的所有属性值
     * 预期结果：复制后的对象属性值应与原始对象一致，且复制对象与原始对象为不同的实例
     * <p>
     * 特殊说明：测试中设置原始配置的多个属性值，并通过断言验证复制后的属性值是否一致
     * 同时验证修改复制对象的属性值不会影响原始对象的属性值
     */
    @Test
    @DisplayName("测试配置复制")
    void testCopy() {
        // 设置原始配置
        settings.providerType = AIProviderType.OLLAMA;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        config.modelName = "llama2";
        config.baseUrl = "http://localhost:11434";
        SettingsState.setApiKey(config.md5, "test-key");
        settings.generateForField = true;
        settings.maxRetries = 5;
        settings.temperature = 0.5;
        settings.topP = 0.5;
        settings.topK = 20;
        settings.presencePenalty = 0.5;

        // 复制
        SettingsState copy = settings.copy();

        // 验证复制的值
        assertThat(copy.providerType).isEqualTo(settings.providerType);
        SettingsState.ProviderConfig copyConfig = copy.getDefaultProviderConfig(AIProviderType.OLLAMA);
        assertThat(copyConfig.modelName).isEqualTo(config.modelName);
        assertThat(copyConfig.baseUrl).isEqualTo(config.baseUrl);
        assertThat(copy.generateForField).isEqualTo(settings.generateForField);
        assertThat(copy.maxRetries).isEqualTo(settings.maxRetries);
        assertThat(copy.temperature).isEqualTo(settings.temperature);
        assertThat(copy.topP).isEqualTo(settings.topP);
        assertThat(copy.topK).isEqualTo(settings.topK);
        assertThat(copy.presencePenalty).isEqualTo(settings.presencePenalty);

        // 验证是不同的对象
        assertThat(copy).isNotSameAs(settings);

        // 修改副本不影响原始对象
        copy.providerType = AIProviderType.QIANWEN;
        assertThat(settings.providerType).isEqualTo(AIProviderType.OLLAMA);
    }

    /**
     * 测试获取默认 Prompt 模板功能
     * <p>
     * 测试目标：验证 SettingsState 类中默认的 Prompt 模板是否符合预期
     * 测试场景：检查默认的类模板、方法模板、字段模板和测试模板内容
     * 预期结果：所有默认模板应非空，并包含指定的关键字如 "JavaDoc"、"%s" 以及测试相关描述
     * <p>
     * 说明：测试中涉及的模板内容需确保包含必要的占位符和注释关键字，以满足生成 JavaDoc 的需求
     */
    @Test
    @DisplayName("测试获取默认 Prompt 模板")
    void testDefaultPromptTemplates() {
        String classPrompt = SettingsState.getDefaultClassPromptTemplate();
        assertThat(classPrompt).isNotEmpty();
        assertThat(classPrompt).contains("JavaDoc");
        assertThat(classPrompt).contains("%s");

        String methodPrompt = SettingsState.getDefaultMethodPromptTemplate();
        assertThat(methodPrompt).isNotEmpty();
        assertThat(methodPrompt).contains("JavaDoc");
        assertThat(methodPrompt).contains("%s");
        assertThat(methodPrompt).contains("@param");
        assertThat(methodPrompt).contains("@return");

        String fieldPrompt = SettingsState.getDefaultFieldPromptTemplate();
        assertThat(fieldPrompt).isNotEmpty();
        assertThat(fieldPrompt).contains("JavaDoc");
        assertThat(fieldPrompt).contains("%s");

        String testPrompt = SettingsState.getDefaultTestPromptTemplate();
        assertThat(testPrompt).isNotEmpty();
        assertThat(testPrompt).contains("测试");
        assertThat(testPrompt).contains("%s");
    }

    /**
     * 测试 Prompt 模板默认值
     * <p>
     * 测试场景：验证各类 Prompt 模板的默认值是否与 SettingsState 中定义的默认值一致
     * 预期结果：所有 Prompt 模板的值应等于对应的默认值
     * <p>
     * 注意：测试依赖 SettingsState 类中默认值的定义，确保其正确性
     */
    @Test
    @DisplayName("测试 Prompt 模板默认值")
    void testPromptTemplateDefaults() {
        assertThat(settings.classPromptTemplate).isEqualTo(SettingsState.getDefaultClassPromptTemplate());
        assertThat(settings.methodPromptTemplate).isEqualTo(SettingsState.getDefaultMethodPromptTemplate());
        assertThat(settings.fieldPromptTemplate).isEqualTo(SettingsState.getDefaultFieldPromptTemplate());
        assertThat(settings.testPromptTemplate).isEqualTo(SettingsState.getDefaultTestPromptTemplate());
    }

    /**
     * 测试持久化状态功能
     * <p>
     * 测试场景：修改配置后获取状态并验证是否与原对象一致
     * 预期结果：状态对象应与原对象为同一实例，且配置信息正确持久化
     * <p>
     * 特殊说明：测试需要修改配置项，包括 AI 提供商、模型名称和生成字段标志
     */
    @Test
    @DisplayName("测试持久化状态")
    void testPersistentState() {
        // 修改配置
        settings.providerType = AIProviderType.OLLAMA;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        config.modelName = "llama2";
        settings.generateForField = true;

        // 获取状态
        SettingsState state = settings.getState();

        // 验证状态是同一个对象
        assertThat(state).isSameAs(settings);
        assertThat(state.providerType).isEqualTo(AIProviderType.OLLAMA);
        SettingsState.ProviderConfig stateConfig = state.getDefaultProviderConfig(AIProviderType.OLLAMA);
        assertThat(stateConfig.modelName).isEqualTo("llama2");
        assertThat(state.generateForField).isTrue();
    }

    /**
     * 测试加载状态功能
     * <p>
     * 测试场景：验证从 {@link SettingsState} 对象加载配置参数到 {@link SettingsState} 实例的正确性
     * 预期结果：所有配置参数应被正确设置并匹配预期值
     * <p>
     * 特殊说明：测试需要创建一个包含完整配置信息的 {@link SettingsState} 实例，并调用 {@link SettingsState#loadState(SettingsState)} 方法进行加载
     */
    @Test
    @DisplayName("测试加载状态")
    void testLoadState() {
        // 创建新的状态
        SettingsState newState = new SettingsState();
        newState.providerType = AIProviderType.OLLAMA;
        SettingsState.ProviderConfig config = newState.getDefaultProviderConfig(AIProviderType.OLLAMA);
        config.modelName = "llama2";
        config.baseUrl = "http://localhost:11434";
        newState.generateForField = true;
        newState.maxRetries = 5;
        newState.topP = 0.5;
        newState.topK = 20;
        newState.presencePenalty = 0.5;

        // 加载状态
        settings.loadState(newState);

        // 验证加载的值
        assertThat(settings.providerType).isEqualTo(AIProviderType.OLLAMA);
        SettingsState.ProviderConfig loadedConfig = settings.getDefaultProviderConfig(AIProviderType.OLLAMA);
        assertThat(loadedConfig.modelName).isEqualTo("llama2");
        assertThat(loadedConfig.baseUrl).isEqualTo("http://localhost:11434");
        assertThat(settings.generateForField).isTrue();
        assertThat(settings.maxRetries).isEqualTo(5);
        assertThat(settings.topP).isEqualTo(0.5);
        assertThat(settings.topK).isEqualTo(20);
        assertThat(settings.presencePenalty).isEqualTo(0.5);
    }
}

