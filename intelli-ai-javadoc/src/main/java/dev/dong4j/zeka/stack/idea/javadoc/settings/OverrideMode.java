package dev.dong4j.zeka.stack.idea.javadoc.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 覆写模式枚举
 * <p>
 * 用于表示当覆盖已有注释时使用的模式类型。
 *
 * @author zeka.stack.team
 * @version 2.7.0
 * @since 2.7.0
 */
public enum OverrideMode {
    /**
     * 仅修复错误注释
     * <p>
     * 保留原有注释, 只修复其中的错误部分.
     */
    FIX("fix", "仅修复错误注释"),

    /**
     * 删除原注释并重新生成
     * <p>
     * 完全替换原有注释, 重新生成新的注释.
     *
     */
    REPLACE("replace", "删除原注释并重新生成");

    /** 模式代码值 */
    private final String value;

    /** 模式显示描述 */
    private final String desc;

    /**
     * 构造函数
     *
     * @param value 模式代码值
     * @param desc  模式显示描述
     */
    OverrideMode(@NotNull String value, @NotNull String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 获取模式代码值
     *
     * @return 模式代码值 (如 "fix" 或 "replace")
     */
    @NotNull
    public String getValue() {
        return value;
    }

    /**
     * 获取模式显示描述
     *
     * @return 模式显示描述 (如 "仅修复错误注释" 或 "删除原注释并重新生成")
     */
    @NotNull
    public String getDesc() {
        return desc;
    }

    /**
     * 根据模式代码值查找对应的枚举值
     *
     * @param value 模式代码值 (如 "fix" 或 "replace")
     * @return 对应的枚举值, 如果未找到则返回默认值 REPLACE
     */
    @NotNull
    public static OverrideMode fromValue(@NotNull String value) {
        for (OverrideMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        // 默认返回 REPLACE
        return REPLACE;
    }

    /**
     * 根据模式代码值查找对应的枚举值 (可空版本)
     *
     * @param value 模式代码值 (如 "fix" 或 "replace"), 可以为 null
     * @return 对应的枚举值, 如果未找到或 value 为 null 则返回默认值 REPLACE
     */
    @NotNull
    public static OverrideMode fromValueOrNull(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return REPLACE;
        }
        return fromValue(value);
    }
}

