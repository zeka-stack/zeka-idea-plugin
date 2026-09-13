/*
 * OrcaRouter Provider 回归测试：固定模型能力过滤与工厂路由边界。
 */
package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.openai;

import com.intellij.openapi.project.Project;

import org.junit.jupiter.api.Test;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/**
 * OrcaRouter 提供商测试
 * <p>
 * 验证模型目录仅排除服务端明确标记为不兼容的条目，并确保工厂不会退回通用 Provider。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
class OrcaRouterProviderTest {

    /**
     * OpenAI 文本模型与缺少元数据的兼容条目应保留，明确的 Anthropic-only / image-only 模型应排除。
     */
    @Test
    void shouldFilterModelsByEndpointAndOutputModality() {
        OrcaRouterProvider provider = createProvider();
        String response = """
            {
              "data": [
                {
                  "id": "openai/gpt-4o-mini",
                  "supported_endpoint_types": ["openai"],
                  "architecture": {"output_modalities": ["text"]}
                },
                {
                  "id": "multimodal/chat",
                  "supported_endpoint_types": ["anthropic", "openai"],
                  "architecture": {"output_modalities": ["text", "image"]}
                },
                {
                  "id": "anthropic/claude-only",
                  "supported_endpoint_types": ["anthropic"],
                  "architecture": {"output_modalities": ["text"]}
                },
                {
                  "id": "image/generator",
                  "supported_endpoint_types": ["openai"],
                  "architecture": {"output_modalities": ["image"]}
                },
                {"id": "orcarouter/auto"}
              ]
            }
            """;

        assertEquals(List.of("openai/gpt-4o-mini", "multimodal/chat", "orcarouter/auto"),
                     provider.parseModelsResponse(response));
    }

    /** 模型目录格式损坏时应安全返回空列表。 */
    @Test
    void shouldReturnEmptyListForMalformedResponse() {
        assertEquals(List.of(), createProvider().parseModelsResponse("not-json"));
    }

    /** 工厂应创建 OrcaRouter 专属 Provider，以启用其模型目录过滤。 */
    @Test
    void shouldCreateDedicatedProviderFromFactory() {
        AIProviderConfig config = createConfig();
        AIServiceProvider provider = AIServiceFactory.createProvider(mock(Project.class), config);

        assertInstanceOf(OrcaRouterProvider.class, provider);
    }

    /** 构造测试 Provider，避免测试依赖网络与 IDE 项目状态。 */
    private static OrcaRouterProvider createProvider() {
        AIProviderConfig config = createConfig();
        return new OrcaRouterProvider(mock(Project.class), config, new AIModelParameters(), new AIRuntimeSettings());
    }

    /** 创建与枚举默认值一致的 OrcaRouter 配置。 */
    private static AIProviderConfig createConfig() {
        AIProviderConfig config = new AIProviderConfig();
        config.providerType = AIProviderType.ORCAROUTER;
        config.baseUrl = AIProviderType.ORCAROUTER.getDefaultBaseUrl();
        config.modelName = AIProviderType.ORCAROUTER.getDefaultModel();
        return config;
    }
}
