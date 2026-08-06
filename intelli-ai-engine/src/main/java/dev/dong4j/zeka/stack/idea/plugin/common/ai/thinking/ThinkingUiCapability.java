package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 设置页 / 对话框根据策略决定 Think 相关控件显隐
 *
 * @param supportsToggle  是否显示 Think 开关
 * @param supportsEffort  是否显示思考强度
 * @param supportsProbe   测连后是否执行网络探测（false 时仍可返回合成结论）
 * @param allowedEfforts  强度可选项（通常不含 AUTO，由 UI 单独加「自动」）
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public record ThinkingUiCapability(
    boolean supportsToggle,
    boolean supportsEffort,
    boolean supportsProbe,
    @NotNull List<ThinkingEffort> allowedEfforts
) {
    private static final List<ThinkingEffort> STANDARD_EFFORTS =
        List.of(ThinkingEffort.LOW, ThinkingEffort.HIGH, ThinkingEffort.MAX);

    /** 兼容网关默认：仅 enable_thinking 开关 + 三探针 */
    public static final ThinkingUiCapability ENABLE_THINKING =
        new ThinkingUiCapability(true, false, true, List.of());

    /** 通义：开关 + 强度 + 三探针 */
    public static final ThinkingUiCapability QIANWEN =
        new ThinkingUiCapability(true, true, true, STANDARD_EFFORTS);

    /** DeepSeek / 豆包 / 智谱等：开关 + 强度，无 enable_thinking 探针 */
    public static final ThinkingUiCapability TOGGLE_AND_EFFORT =
        new ThinkingUiCapability(true, true, false, STANDARD_EFFORTS);

    /** DeepSeek 别名（历史常量） */
    public static final ThinkingUiCapability DEEPSEEK = TOGGLE_AND_EFFORT;

    /** Kimi K3：始终思考，仅强度 */
    public static final ThinkingUiCapability EFFORT_ONLY =
        new ThinkingUiCapability(false, true, false, STANDARD_EFFORTS);

    /** Kimi K2.x：仅 thinking.type 开关 */
    public static final ThinkingUiCapability TOGGLE_ONLY =
        new ThinkingUiCapability(true, false, false, List.of());

    /** 当前协议不写入思考扩展字段 */
    public static final ThinkingUiCapability NONE =
        new ThinkingUiCapability(false, false, false, List.of());
}
