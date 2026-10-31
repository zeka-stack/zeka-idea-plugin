package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 思考强度（用户意图层，与具体 HTTP 字段无关）
 * <p>
 * {@link #AUTO} 表示交给策略使用其 {@link ThinkingParamStrategy#defaultEffort()}。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ThinkingEffort {
    /** 使用策略默认强度 */
    AUTO,
    /** 低强度 */
    LOW,
    /** 高强度 */
    HIGH,
    /** 最大强度 */
    MAX;

    /**
     * 解析持久化字符串；未知或空值回退 {@link #AUTO}
     *
     * @param value 配置中的字符串
     * @return 枚举值
     */
    @NotNull
    public static ThinkingEffort fromConfig(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return ThinkingEffort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return AUTO;
        }
    }

    /**
     * 写入请求体的 effort 字面量（DeepSeek / Kimi / 智谱等：low/high/max）
     *
     * @return low / high / max
     */
    @NotNull
    public String toApiValue() {
        return switch (this) {
            case LOW -> "low";
            case MAX -> "max";
            case HIGH, AUTO -> "high";
        };
    }

    /**
     * 通义 DashScope {@code reasoning_effort} 字面量（low / medium / xhigh）
     *
     * @return DashScope 强度值
     */
    @NotNull
    public String toDashScopeReasoningEffort() {
        return switch (this) {
            case LOW -> "low";
            case HIGH -> "medium";
            case MAX, AUTO -> "xhigh";
        };
    }
}
