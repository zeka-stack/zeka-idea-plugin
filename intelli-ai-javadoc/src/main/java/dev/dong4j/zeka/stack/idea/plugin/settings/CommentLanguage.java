package dev.dong4j.zeka.stack.idea.plugin.settings;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 注释生成语言枚举
 * <p>
 * 用于表示 Javadoc 注释生成时使用的语言类型。
 * 包含语言代码（value）和显示描述（desc）两个字段。
 *
 * @author zeka.stack.team
 * @version 2.1.0
 * @since 2.1.0
 */
public enum CommentLanguage {
    /**
     * 中文
     */
    ZH("zh", "中文"),

    /**
     * 英文
     */
    EN("en", "英文");

    /**
     * 语言代码值
     */
    private final String value;

    /**
     * 语言显示描述
     */
    private final String desc;

    /**
     * 构造函数
     *
     * @param value 语言代码值
     * @param desc  语言显示描述
     */
    CommentLanguage(@NotNull String value, @NotNull String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 获取语言代码值
     *
     * @return 语言代码值（如 "zh" 或 "en"）
     */
    @NotNull
    public String getValue() {
        return value;
    }

    /**
     * 获取语言显示描述
     *
     * @return 语言显示描述（如 "中文" 或 "英文"）
     */
    @NotNull
    public String getDesc() {
        return desc;
    }

    /**
     * 根据语言代码值查找对应的枚举值
     *
     * @param value 语言代码值（如 "zh" 或 "en"）
     * @return 对应的枚举值，如果未找到则返回默认值 ZH
     */
    @NotNull
    public static CommentLanguage fromValue(@NotNull String value) {
        for (CommentLanguage language : values()) {
            if (language.value.equals(value)) {
                return language;
            }
        }
        // 默认返回中文
        return ZH;
    }

    /**
     * 根据语言代码值查找对应的枚举值（可空版本）
     *
     * @param value 语言代码值（如 "zh" 或 "en"），可以为 null
     * @return 对应的枚举值，如果未找到或 value 为 null 则返回默认值 ZH
     */
    @NotNull
    public static CommentLanguage fromValueOrNull(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return ZH;
        }
        return fromValue(value);
    }
}

