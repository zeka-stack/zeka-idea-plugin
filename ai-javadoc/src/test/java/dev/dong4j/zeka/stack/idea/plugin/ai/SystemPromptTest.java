package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 系统提示词功能测试
 *
 * <p>测试系统提示词的配置和使用功能。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
class SystemPromptTest {

    private CustomProvider provider;
    private SettingsState settings;

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

    @Test
    void testCustomSystemPrompt() {
        // 测试自定义系统提示词
        String customSystemPrompt = "你是一个专业的代码文档生成助手，专门为 Java 代码生成高质量的注释。";
        settings.systemPromptTemplate = customSystemPrompt;

        String systemPrompt = provider.getSystemPrompt();
        assertEquals(customSystemPrompt, systemPrompt);
    }

    @Test
    void testEmptySystemPrompt() {
        // 测试空系统提示词时使用默认值
        settings.systemPromptTemplate = "";

        String systemPrompt = provider.getSystemPrompt();
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
    }

    @Test
    void testNullSystemPrompt() {
        // 测试 null 系统提示词时使用默认值
        settings.systemPromptTemplate = null;

        String systemPrompt = provider.getSystemPrompt();
        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
    }

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
