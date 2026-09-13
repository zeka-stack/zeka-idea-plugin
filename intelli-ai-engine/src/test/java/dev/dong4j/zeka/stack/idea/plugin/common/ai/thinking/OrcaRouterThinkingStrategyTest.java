/*
 * OrcaRouter 思考参数回归测试：确保聚合路由只接收官方 reasoning_effort 字段。
 */
package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OrcaRouter 思考参数策略测试
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
class OrcaRouterThinkingStrategyTest {

    private static final ThinkingContext CONTEXT =
        new ThinkingContext(AIProviderType.ORCAROUTER, "orcarouter/auto", null);

    /** 开启思考且选择 AUTO 时，应只发送默认 high 强度。 */
    @Test
    void shouldApplyOnlyReasoningEffortWhenEnabled() {
        JsonObject body = new JsonObject();

        OrcaRouterThinkingStrategy.INSTANCE.apply(body, new ThinkingIntent(true, ThinkingEffort.AUTO), CONTEXT);

        assertEquals("high", body.get("reasoning_effort").getAsString());
        assertFalse(body.has("enable_thinking"));
        assertFalse(body.has("thinking"));
    }

    /** 关闭思考时不应写入任何厂商扩展字段。 */
    @Test
    void shouldOmitThinkingFieldsWhenDisabled() {
        JsonObject body = new JsonObject();

        OrcaRouterThinkingStrategy.INSTANCE.apply(body, new ThinkingIntent(false, ThinkingEffort.MAX), CONTEXT);

        assertFalse(body.has("reasoning_effort"));
        assertFalse(body.has("enable_thinking"));
        assertFalse(body.has("thinking"));
    }

    /** 注册表必须优先返回 OrcaRouter 专属策略，避免落入 enable_thinking 默认策略。 */
    @Test
    void shouldResolveDedicatedStrategy() {
        AIProviderConfig config = new AIProviderConfig();
        config.providerType = AIProviderType.ORCAROUTER;

        ThinkingParamStrategy strategy = ThinkingParamStrategyRegistry.resolve(config);

        assertSame(OrcaRouterThinkingStrategy.INSTANCE, strategy);
        assertTrue(strategy.uiCapability(CONTEXT).supportsEffort());
        assertFalse(strategy.uiCapability(CONTEXT).supportsProbe());
    }
}
