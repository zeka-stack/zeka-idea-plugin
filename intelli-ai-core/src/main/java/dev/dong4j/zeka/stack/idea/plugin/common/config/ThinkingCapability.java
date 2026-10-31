package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;

/**
 * 模型对 {@code enable_thinking} 扩展字段的能力画像
 * <p>
 * 该字段并非 OpenAI 官方规范, 由兼容厂商自行实现; 能力由测试连接时的三探针探测得出.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ThinkingCapability {
    /** 传 true/false 均失败, 只能不传字段 */
    UNSUPPORTED,
    /** 必须传 true (false 会被拒绝) */
    REQUIRED_TRUE,
    /** true/false 均可; 关闭 Think 时默认不写字段 */
    OPTIONAL,
    /** 不传参数即可使用 (常默认开启思考) */
    DEFAULT_ON_NO_PARAM,
    /** 探测失败或结果矛盾, 回退到用户勾选 */
    UNKNOWN;

    /**
     * 生成面向用户的简短中文说明
     *
     * @return 说明文本
     */
    @NotNull
    public String displayLabel() {
        return switch (this) {
            case UNSUPPORTED -> "不支持思考参数";
            case REQUIRED_TRUE -> "必须启用 Think";
            case OPTIONAL -> "可选 Think（可开关）";
            case DEFAULT_ON_NO_PARAM -> "默认思考（无需传参）";
            case UNKNOWN -> "未知（请手动选择）";
        };
    }
}
