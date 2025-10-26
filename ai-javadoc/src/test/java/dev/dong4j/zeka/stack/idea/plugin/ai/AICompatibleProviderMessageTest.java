package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AICompatibleProvider 消息结构测试
 *
 * <p>测试新的 system/user 消息结构是否正确实现。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
class AICompatibleProviderMessageTest {

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
    void testBuildRequestBodyStructure() {
        String testPrompt = "请为以下代码生成 JavaDoc 注释：\n\npublic class TestClass {\n    private String name;\n}";

        JsonObject requestBody = provider.buildRequestBody(testPrompt);

        // 验证基本结构
        assertNotNull(requestBody);
        assertEquals("gpt-3.5-turbo", requestBody.get("model").getAsString());
        assertEquals(0.1, requestBody.get("temperature").getAsDouble());
        assertEquals(1000, requestBody.get("max_tokens").getAsInt());

        // 验证 messages 数组
        JsonArray messages = requestBody.getAsJsonArray("messages");
        assertNotNull(messages);
        assertEquals(2, messages.size(), "应该有 system 和 user 两个消息");

        // 验证 system 消息
        JsonObject systemMessage = messages.get(0).getAsJsonObject();
        assertEquals("system", systemMessage.get("role").getAsString());
        String systemContent = systemMessage.get("content").getAsString();
        assertNotNull(systemContent);
        assertTrue(systemContent.contains("专业的 Java 开发工程师"));
        assertTrue(systemContent.contains("JavaDoc 注释"));

        // 验证 user 消息
        JsonObject userMessage = messages.get(1).getAsJsonObject();
        assertEquals("user", userMessage.get("role").getAsString());
        assertEquals(testPrompt, userMessage.get("content").getAsString());
    }

    @Test
    void testGetSystemPrompt() {
        String systemPrompt = provider.getSystemPrompt();

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
        assertTrue(systemPrompt.contains("JavaDoc 注释"));
        assertTrue(systemPrompt.contains("中文"));
        assertTrue(systemPrompt.length() > 50, "系统提示词应该足够详细");
    }

    @Test
    void testSystemPromptConsistency() {
        // 多次调用应该返回相同的内容
        String prompt1 = provider.getSystemPrompt();
        String prompt2 = provider.getSystemPrompt();
        assertEquals(prompt1, prompt2, "系统提示词应该保持一致");
    }
}
