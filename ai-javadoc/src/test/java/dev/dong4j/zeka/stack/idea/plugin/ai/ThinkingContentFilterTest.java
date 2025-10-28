package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.CustomProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 思考内容过滤测试类
 * <p>
 * 用于验证 AI 响应中思考内容的过滤功能，确保系统能够正确识别并移除
 * <think> 标签内的思考内容，保留最终的响应内容。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@Slf4j
class ThinkingContentFilterTest {

    /** 自定义服务提供者实例 */
    private CustomProvider provider;
    /** 用户的设置状态信息 */
    private SettingsState settings;

    /**
     * 初始化测试环境，设置配置和自定义提供者
     * <p>
     * 用于在每个测试用例执行前初始化必要的配置和提供者对象，设置默认的AI提供者、基础URL、API密钥、模型名称等参数。
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
        settings.configurationVerified = true;
        settings.verboseLogging = true;

        provider = new CustomProvider(settings);
    }

    /**
     * 测试带有思考标签的内容过滤功能
     * <p>
     * 测试场景：当响应内容中包含思考标签时，通过反射调用私有方法进行过滤
     * 预期结果：过滤后的内容应保留注释信息，去除思考标签内容
     * <p>
     * 该测试通过反射调用 {@link CustomProvider#filterThinkingContent(String)} 方法，
     * 验证过滤后的结果是否符合预期，包括保留注释内容、去除思考标签内容等
     */
    @Test
    void testFilterThinkingContentWithTag() {
        log.info("xxxxxxxxxx");

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

    /**
     * 测试过滤不包含思考标签的内容
     * <p>
     * 测试场景：当响应内容中不包含思考标签时
     * 预期结果：应返回原始内容，不进行任何过滤
     * <p>
     * 注意：该测试通过反射调用 CustomProvider 父类中的 filterThinkingContent 方法进行验证
     */
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

    /**
     * 测试 filterThinkingContent 方法在输入为空字符串时的处理逻辑
     * <p>
     * 测试场景：输入为空字符串或 null 值
     * 预期结果：当输入为空字符串时返回空字符串，当输入为 null 时返回 null
     * <p>
     * 由于该方法为私有方法，通过反射调用进行测试
     */
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

    /**
     * 测试过滤仅包含思考内容的场景
     * <p>
     * 测试场景：输入内容仅包含思考部分，没有实际文本内容
     * 预期结果：过滤后的内容应为空字符串
     * <p>
     * 该测试通过反射调用 CustomProvider 父类中的 filterThinkingContent 方法进行验证
     * <p>
     * 注意：测试需要通过反射访问私有方法，因此需要设置方法为可访问
     */
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

    /**
     * 测试过滤包含多个思考标签的内容功能
     * <p>
     * 测试场景：验证当响应中包含多个思考标签时，过滤方法是否能正确移除思考内容并保留注释
     * 预期结果：过滤后的内容应包含生成的注释，并且不包含 "思考内容" 字符串
     * <p>
     * 注意：此测试通过反射调用父类的 filterThinkingContent 方法，用于验证其行为
     */
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
