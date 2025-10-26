package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 思考内容过滤测试
 *
 * <p>测试 AI 响应中思考内容的过滤功能。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
class ThinkingContentFilterTest {

    private CustomProvider provider;
    private SettingsState settings;

    @BeforeEach
    void setUp() {
        settings = new SettingsState();
        settings.aiProvider = AIProviderType.CUSTOM.getProviderId();
        settings.baseUrl = "https://api.openai.com/v1";
        settings.apiKey = "test-api-key";
        settings.modelName = "gpt-3.5-turbo";
        settings.configurationVerified = true;
        settings.verboseLogging = true;

        provider = new CustomProvider(settings);
    }

    @Test
    void testFilterThinkingContentWithTag() {
        // 测试包含思考标签的内容过滤
        String responseWithThinking = """
            <think>
            我需要分析这个类的功能，它看起来是一个用户服务类，
            提供了用户相关的业务逻辑处理功能。
            </think>
            
            /**
             * 用户服务类
             * <p>
             * <p>提供用户相关的业务逻辑处理，包括用户的查询、创建、更新和删除等操作
             * <p>
             * @author dong4j
             * @version 1.0.0
             */
            """;

        // 使用反射访问私有方法
        try {
            java.lang.reflect.Method method = CustomProvider.class.getSuperclass().getDeclaredMethod("filterThinkingContent", String.class);
            method.setAccessible(true);
            String filteredContent = (String) method.invoke(provider, responseWithThinking);

            assertNotNull(filteredContent);
            assertTrue(filteredContent.contains("/**"));
            assertTrue(filteredContent.contains("用户服务类"));
            assertTrue(filteredContent.contains("@author dong4j"));
            assertFalse(filteredContent.contains("<think>"));
            assertFalse(filteredContent.contains("我需要分析这个类的功能"));
        } catch (Exception e) {
            fail("Failed to test thinking content filter: " + e.getMessage());
        }
    }

    @Test
    void testFilterThinkingContentWithoutTag() {
        // 测试不包含思考标签的内容
        String responseWithoutThinking = "/**\n * 用户服务类\n */";

        try {
            java.lang.reflect.Method method = CustomProvider.class.getSuperclass().getDeclaredMethod("filterThinkingContent", String.class);
            method.setAccessible(true);
            String filteredContent = (String) method.invoke(provider, responseWithoutThinking);

            assertEquals(responseWithoutThinking, filteredContent);
        } catch (Exception e) {
            fail("Failed to test thinking content filter: " + e.getMessage());
        }
    }

    @Test
    void testFilterThinkingContentEmpty() {
        // 测试空内容
        try {
            java.lang.reflect.Method method = CustomProvider.class.getSuperclass().getDeclaredMethod("filterThinkingContent", String.class);
            method.setAccessible(true);

            String emptyResult = (String) method.invoke(provider, "");
            assertEquals("", emptyResult);

            String nullResult = (String) method.invoke(provider, (String) null);
            assertNull(nullResult);
        } catch (Exception e) {
            fail("Failed to test thinking content filter: " + e.getMessage());
        }
    }

    @Test
    void testFilterThinkingContentOnlyThinking() {
        // 测试只有思考内容的情况
        String onlyThinking = """
            <think>
            这是一个测试类的思考过程
            </think>
            """;

        try {
            java.lang.reflect.Method method = CustomProvider.class.getSuperclass().getDeclaredMethod("filterThinkingContent", String.class);
            method.setAccessible(true);
            String filteredContent = (String) method.invoke(provider, onlyThinking);

            assertNotNull(filteredContent);
            assertEquals("", filteredContent.trim());
        } catch (Exception e) {
            fail("Failed to test thinking content filter: " + e.getMessage());
        }
    }

    @Test
    void testFilterThinkingContentMultipleTags() {
        // 测试包含多个思考标签的情况 - 简化版本
        String responseWithMultipleThinking = "<think>思考内容</think>\n/**\n * 最终结果\n */";

        try {
            java.lang.reflect.Method method = CustomProvider.class.getSuperclass().getDeclaredMethod("filterThinkingContent", String.class);
            method.setAccessible(true);
            String filteredContent = (String) method.invoke(provider, responseWithMultipleThinking);

            assertNotNull(filteredContent);
            assertTrue(filteredContent.contains("/**"));
            assertTrue(filteredContent.contains("最终结果"));
            assertFalse(filteredContent.contains("思考内容"));
        } catch (Exception e) {
            fail("Failed to test thinking content filter: " + e.getMessage());
        }
    }
}
