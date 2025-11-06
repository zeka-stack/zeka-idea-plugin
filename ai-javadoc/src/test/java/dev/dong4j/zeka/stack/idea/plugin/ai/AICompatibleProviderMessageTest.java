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
 * AICompatibleProvider 消息结构测试类
 * <p>
 * 用于验证 AI 兼容提供者的新消息结构是否正确实现，包括构建请求体和获取系统提示词的功能。
 * 测试内容涵盖消息结构的完整性、字段值的准确性以及系统提示词的一致性。
 * <p>
 * 该测试类主要针对 CustomProvider 类的 buildRequestBody 和 getSystemPrompt 方法进行验证，
 * 确保其符合预期的 AI 消息格式要求。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
class AICompatibleProviderMessageTest {

    /** 自定义提供者实例 */
    private CustomProvider provider;
    /** settings 字段用于存储应用的配置状态信息 */
    private SettingsState settings;

    /**
     * 初始化测试环境，设置自定义AI提供者的配置信息
     * <p>
     * 此方法用于在每个测试用例执行前，初始化一个包含默认配置的SettingsState对象，并创建一个CustomProvider实例。
     * 配置包括AI提供者ID、基础URL、API密钥、模型名称、温度值和最大令牌数等。
     *
     * @since 1.0
     */
    @BeforeEach
    void setUp() {
        settings = new SettingsState();
        settings.providerType = AIProviderType.CUSTOM;
        SettingsState.ProviderConfig config = settings.getDefaultProviderConfig(AIProviderType.CUSTOM);
        config.baseUrl = "https://api.openai.com/v1";
        config.modelName = "gpt-3.5-turbo";
        config.configurationVerified = true;
        SettingsState.setApiKey(config.md5, "test-api-key");
        settings.temperature = 0.1;
        settings.maxTokens = 1000;

        provider = new CustomProvider(settings, config);
    }

    /**
     * 测试构建请求体结构功能
     * <p>
     * 测试目标：验证构建的请求体是否符合预期结构
     * 测试场景：输入一个包含代码片段的提示信息
     * 预期结果：请求体应包含正确的模型类型、温度值、最大令牌数以及包含 system 和 user 消息的数组
     * <p>
     * 特别说明：测试中会验证 system 消息内容是否包含“专业的 Java 开发工程师”和“JavaDoc 注释”关键词
     */
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

    /**
     * 测试获取系统提示词功能
     * <p>
     * 测试场景：验证系统提示词是否包含指定的关键内容
     * 预期结果：系统提示词应包含 "专业的 Java 开发工程师"、"JavaDoc 注释" 和 "中文" 等关键词，并且长度应超过 50
     */
    @Test
    void testGetSystemPrompt() {
        String systemPrompt = provider.getSystemPrompt();

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("专业的 Java 开发工程师"));
        assertTrue(systemPrompt.contains("JavaDoc 注释"));
        assertTrue(systemPrompt.contains("中文"));
        assertTrue(systemPrompt.length() > 50, "系统提示词应该足够详细");
    }

    /**
     * 测试系统提示词的一致性
     * <p>
     * 预期结果：每次调用返回的内容应保持一致
     * <p>
     * 说明：该测试用于验证系统提示词在多次调用时是否保持不变，确保其稳定性
     */
    @Test
    void testSystemPromptConsistency() {
        // 多次调用应该返回相同的内容
        String prompt1 = provider.getSystemPrompt();
        String prompt2 = provider.getSystemPrompt();
        assertEquals(prompt1, prompt2, "系统提示词应该保持一致");
    }
}
