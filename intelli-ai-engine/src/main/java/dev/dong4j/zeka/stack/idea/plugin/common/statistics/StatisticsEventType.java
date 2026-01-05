package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

/**
 * <p>Description : 统计事件类型枚举.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public enum StatisticsEventType {

    /**
     * AI 请求事件
     * 表示与人工智能请求相关的统计事件类型
     */
    AI_REQUEST("ai_request", "AI 请求"),

    /** 自动完成事件 */
    AUTOCOMPLETE("autocomplete", "自动完成"),

    /** 生成事件 -changelog */
    COMMIT_MESSAGE("commit_message", "提交信息生成"),

    /** 生成事件 -javadoc 类注释 */
    JAVADOC_CLASS("javadoc_class", "类注释生成"),

    /** 生成事件 -javadoc 方法注释 */
    JAVADOC_METHOD("javadoc_method", "方法注释生成"),

    /** 生成事件 -javadoc 字段注释 */
    JAVADOC_FIELD("javadoc_field", "字段注释生成"),

    /** Release log 生成 */
    RELEASE_LOG("release_log", "Release Log 生成"),

    /** 日报生成 */
    DAILY_REPORT("daily_report", "日报生成"),

    /** 周报生成 */
    WEEKLY_REPORT("weekly_report", "周报生成");

    /** 枚举代码值, 用于标识统计事件类型 */
    private final String code;
    /**
     * 事件类型的描述信息
     * <p> 用于表示事件的具体含义 </p>
     *
     * @see StatisticsEventType
     */
    private final String description;

    /**
     * 构造函数, 用于初始化统计事件类型的枚举实例
     *
     * @param code        事件类型的唯一标识码
     * @param description 事件类型的描述信息
     */
    StatisticsEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取枚举对应的代码标识
     * <p> 返回该枚举实例对应的代码字符串, 用于唯一标识该事件类型.
     *
     * @return 事件类型的代码标识
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取枚举项的描述信息
     * <p> 返回该枚举常量对应的描述字符串, 用于展示或日志记录等场景
     *
     * @return 枚举项的描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 枚举的唯一标识码
     * @return 对应的枚举实例, 若未找到则返回 AI_REQUEST 默认值
     */
    public static StatisticsEventType fromCode(String code) {
        for (StatisticsEventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return AI_REQUEST;
    }
}
