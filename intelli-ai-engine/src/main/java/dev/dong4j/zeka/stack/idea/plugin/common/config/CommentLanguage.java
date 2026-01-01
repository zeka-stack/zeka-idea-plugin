package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * Comment Language
 *
 * @author dong4j
 * @date 2026-01-01 04:28:45
 * @version hello.world
 * @since hello.world
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
     * 获取语言显示描述（国际化）
     *
     * <p>返回国际化后的语言显示描述，用于 UI 显示。
     *
     * @return 语言显示描述（如 "中文" 或 "English"）
     */
    @NotNull
    public String getDesc() {
        return this == ZH
               ? AICommonBundle.message("settings.comment.language.chinese")
               : AICommonBundle.message("settings.comment.language.english");
    }

    /**
     * 获取语言显示描述（原始值，用于提示词）
     *
     * <p>提示词模板使用中文，因此返回中文文本。
     *
     * @return 语言显示描述（"中文" 或 "英文"）
     */
    @NotNull
    public String getDescForPrompt() {
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
