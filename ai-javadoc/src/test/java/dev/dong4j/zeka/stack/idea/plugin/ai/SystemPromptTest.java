package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AICompatibleProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 系统提示词功能测试类
 * <p>
 * 用于验证系统提示词的配置和使用逻辑，包括默认提示词、自定义提示词、空提示词和 null 提示词的处理方式。
 * 测试覆盖提示词的生成、验证以及长度限制等功能。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
class SystemPromptTest {

    /** 自定义提供者实例 */
    private CustomProvider provider;
    /** 设置状态信息 */
    private SettingsState settings;

    /**
     * 初始化测试环境，设置自定义AI提供者的配置信息
     * <p>
     * 该方法用于在每个测试用例执行前初始化必要的配置和对象，包括设置AI提供者类型、基础URL、API密钥、模型名称等参数，并创建自定义AI提供者实例。
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
        settings.temperature = 0.1;
        settings.maxTokens = 1000;
        settings.configurationVerified = true;

        provider = new CustomProvider(settings);
    }

    /**
     * 测试默认系统提示词的生成
     * <p>
     * 测试场景：验证系统提示词是否包含指定的关键内容
     * 预期结果：系统提示词应不为空，并且包含 "专业的 Java 开发工程师"、"JavaDoc 注释" 和 "中文" 等关键词，长度应超过50个字符
     */
    @Test
    void testDefaultSystemPrompt() {
        // 测试默认系统提示词
        String systemPrompt = provider.getSystemPrompt();

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
        assertTrue(systemPrompt.contains("JavaDoc 注释"));
        assertTrue(systemPrompt.contains("中文"));
        assertTrue(systemPrompt.length() > 50, "系统提示词应该足够详细");
    }

    /**
     * 测试自定义系统提示词功能
     * <p>
     * 测试场景：设置自定义系统提示词后获取系统提示词
     * 预期结果：应返回与设置的自定义系统提示词一致
     * <p>
     * 说明：测试中通过修改 settings.systemPromptTemplate 属性来验证 provider.getSystemPrompt() 方法是否正确返回自定义值
     */
    @Test
    void testCustomSystemPrompt() {
        // 测试自定义系统提示词
        String customSystemPrompt = "你是一个专业的代码文档生成助手，专门为 Java 代码生成高质量的注释。";
        settings.systemPromptTemplate = customSystemPrompt;

        String systemPrompt = provider.getSystemPrompt();
        assertEquals(customSystemPrompt, systemPrompt);
    }

    /**
     * 测试空系统提示词时使用默认值的场景
     * <p>
     * 测试场景：当系统提示词模板为空字符串时
     * 预期结果：应返回包含"专业的 Java 开发工程师"的默认提示词
     */
    @Test
    void testEmptySystemPrompt() {
        // 测试空系统提示词时使用默认值
        settings.systemPromptTemplate = "";

        String systemPrompt = provider.getSystemPrompt();
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
    }

    /**
     * 测试系统提示词为 null 时的默认值处理逻辑
     * <p>
     * 测试场景：当系统提示词模板设置为 null 时
     * 预期结果：应使用默认提示词，并且该提示词包含 "专业的 Java 开发工程师" 字符串
     * <p>
     * 该测试验证了 {@link AICompatibleProvider#getSystemPrompt()} 方法在系统提示词为 null 时是否正确地返回默认值。
     */
    @Test
    void testNullSystemPrompt() {
        // 测试 null 系统提示词时使用默认值
        settings.systemPromptTemplate = null;

        String systemPrompt = provider.getSystemPrompt();
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
    }

    /**
     * 测试系统提示词的生成功能
     * <p>
     * 测试场景：验证系统提示词是否包含必要的内容并符合长度要求
     * 预期结果：系统提示词应包含"专业的 Java 开发工程师"、"JavaDoc 注释"和"中文"等关键词，且长度在50到1000字符之间
     * <p>
     * 注意：测试中使用了 {@link AICompatibleProvider#getSystemPrompt()} 方法获取系统提示词
     */
    @Test
    void testSystemPromptInRequest() {
        // 测试系统提示词的基本功能
        String systemPrompt = provider.getSystemPrompt();

        // 验证系统提示词包含必要的内容
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
        assertTrue(systemPrompt.contains("JavaDoc 注释"));
        assertTrue(systemPrompt.contains("中文"));

        // 验证系统提示词长度合理
        assertTrue(systemPrompt.length() > 50, "系统提示词应该足够详细");
        assertTrue(systemPrompt.length() < 1000, "系统提示词不应该过长");
    }

    /**
     * 测试默认系统提示词模板功能
     * <p>
     * 测试场景：验证默认系统提示词模板是否包含指定的关键内容
     * 预期结果：默认模板应包含 "专业的 Java 开发工程师"、"JavaDoc 注释"、"中文" 和 "重要要求" 等关键词
     * <p>
     * 注意：测试依赖 {@link SettingsState#getDefaultSystemPromptTemplate()} 方法获取默认模板
     */
    @Test
    void testDefaultSystemPromptTemplate() {
        // 测试默认系统提示词模板
        String defaultTemplate = SettingsState.getDefaultSystemPromptTemplate();

        assertNotNull(defaultTemplate);
        assertTrue(defaultTemplate.contains("专业的 Java 开发工程师"));
        assertTrue(defaultTemplate.contains("JavaDoc 注释"));
        assertTrue(defaultTemplate.contains("中文"));
        assertTrue(defaultTemplate.contains("重要要求"));
    }
}
