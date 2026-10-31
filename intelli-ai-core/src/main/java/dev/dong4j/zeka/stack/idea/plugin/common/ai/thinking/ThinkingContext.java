package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingProbeResult;

/**
 * 策略解析与 apply 时的上下文
 *
 * @param providerType 服务商类型
 * @param modelName    模型名
 * @param probeResult  既有探测结果（可为 null）
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public record ThinkingContext(
    @NotNull AIProviderType providerType,
    @NotNull String modelName,
    @Nullable ThinkingProbeResult probeResult
) {
    /**
     * 从配置构建上下文
     *
     * @param config 服务商配置
     * @return 上下文
     */
    @NotNull
    public static ThinkingContext from(@NotNull AIProviderConfig config) {
        AIProviderType type = config.providerType != null ? config.providerType : AIProviderType.OPENAI;
        String model = config.modelName != null ? config.modelName : "";
        return new ThinkingContext(type, model, config.thinkingProbeResult);
    }
}
