package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import org.jetbrains.annotations.NotNull;

/**
 * 用户对「思考模式」的意图（与厂商 JSON 字段解耦）
 *
 * @param enabled 是否启用思考
 * @param effort  思考强度；关闭时策略可忽略
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public record ThinkingIntent(boolean enabled, @NotNull ThinkingEffort effort) {
}
