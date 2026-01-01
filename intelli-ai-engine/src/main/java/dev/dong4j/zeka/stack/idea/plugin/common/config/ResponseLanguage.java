package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 注释语言枚举
 * <p>
 * 定义支持的注释生成语言类型，包括：
 * <ul>
 *   <li>中文 (ZH) - 简体中文</li>
 *   <li>英文 (EN) - English</li>
 *   <li>日文 (JA) - 日本語</li>
 *   <li>韩文 (KO) - 한국어</li>
 *   <li>法文 (FR) - Français</li>
 *   <li>德文 (DE) - Deutsch</li>
 *   <li>西班牙文 (ES) - Español</li>
 *   <li>俄文 (RU) - Русский</li>
 *   <li>葡萄牙文 (PT) - Português</li>
 *   <li>意大利文 (IT) - Italiano</li>
 * </ul>
 *
 * @author dong4j
 * @date 2026-01-01 04:28:45
 * @version hello.world
 * @since hello.world
 */
public enum ResponseLanguage {
    /**
     * 中文
     */
    ZH("zh", "简体中文"),
    /**
     * 繁体中文
     */
    ZH_TRADITIONAL("zh-TW", "繁体中文"),

    /**
     * 英文
     */
    EN("en", "英文"),

    /**
     * 日文
     */
    JA("ja", "日文"),

    /**
     * 韩文
     */
    KO("ko", "韩文"),

    /**
     * 法文
     */
    FR("fr", "法文"),

    /**
     * 德文
     */
    DE("de", "德文"),

    /**
     * 西班牙文
     */
    ES("es", "西班牙文"),

    /**
     * 俄文
     */
    RU("ru", "俄文"),

    /**
     * 葡萄牙文
     */
    PT("pt", "葡萄牙文"),

    /**
     * 意大利文
     */
    IT("it", "意大利文");

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
    ResponseLanguage(@NotNull String value, @NotNull String desc) {
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
        return switch (this) {
            case ZH -> AICommonBundle.message("settings.comment.language.chinese.simplified");
            case ZH_TRADITIONAL -> AICommonBundle.message("settings.comment.language.chinese.traditional");
            case EN -> AICommonBundle.message("settings.comment.language.english");
            case JA -> AICommonBundle.message("settings.comment.language.japanese");
            case KO -> AICommonBundle.message("settings.comment.language.korean");
            case FR -> AICommonBundle.message("settings.comment.language.french");
            case DE -> AICommonBundle.message("settings.comment.language.german");
            case ES -> AICommonBundle.message("settings.comment.language.spanish");
            case RU -> AICommonBundle.message("settings.comment.language.russian");
            case PT -> AICommonBundle.message("settings.comment.language.portuguese");
            case IT -> AICommonBundle.message("settings.comment.language.italian");
        };
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
    public static ResponseLanguage fromValue(@NotNull String value) {
        for (ResponseLanguage language : values()) {
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
    public static ResponseLanguage fromValueOrNull(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return ZH;
        }
        return fromValue(value);
    }
}
