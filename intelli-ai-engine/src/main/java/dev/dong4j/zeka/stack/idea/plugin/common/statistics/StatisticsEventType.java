package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import lombok.Getter;

/**
 * 统计事件类型枚举
 * <p>用于标识系统中各类统计事件的类型, 支持通过代码快速查找对应描述, 常用于日志记录, 数据统计和行为追踪场景.
 * <p>每个枚举值包含唯一代码 (code) 和对应中文描述(description), 便于前端展示或后端逻辑判断.
 * <p>提供静态方法 {@code fromCode(String code)} 用于根据代码反向查找枚举值, 若未匹配则默认返回 {@code AI_REQUEST}.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
@Getter
public enum StatisticsEventType {

    /** AI 请求事件, 表示与人工智能请求相关的统计事件类型 */
    AI_REQUEST("engine_ai_request", "AI 请求"),

    /** 自动完成事件 */
    AUTOCOMPLETE("engine_autocomplete", "自动完成"),

    /** 提交信息生成事件类型, 用于统计与提交信息生成相关的操作行为 */
    CHANGELOG_COMMIT_MESSAGE("changelog_commit_message", "提交信息生成"),

    /** Release Log 生成事件类型, 用于统计 Release Log 生成相关的操作行为 */
    CHANGELOG_RELEASE_LOG("changelog_release_log", "Release Log 生成"),

    /** 日报生成事件类型, 用于统计每日报告生成相关的操作数据 */
    CHANGELOG_DAILY_REPORT("changelog_daily_report", "日报生成"),

    /** 周报生成事件类型, 用于统计周报相关操作的事件数据 */
    CHANGELOG_WEEKLY_REPORT("changelog_weekly_report", "周报生成"),

    /** 变更日志生成事件类型, 用于统计 Changelog 生成相关操作 */
    CHANGELOG_CHANGELOG("changelog_changelog", "变更日志生成"),

    /** 类注释生成 */
    JAVADOC_CLASS("javadoc_class", "类注释生成"),

    /** 方法注释生成 */
    JAVADOC_METHOD("javadoc_method", "方法注释生成"),

    /** 字段注释生成 */
    JAVADOC_FIELD("javadoc_field", "字段注释生成"),

    /** 终端命令生成 */
    TERMINAL_COMMAND("terminal_command", "终端命令生成");


    /** 事件类型的唯一标识码 */
    private final String code;

    /** 事件类型的描述信息, 用于展示或日志记录 */
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
     * 根据事件类型标识码获取对应的枚举实例
     * <p> 遍历所有枚举值, 匹配传入的 code 字符串, 若找到则返回对应枚举; 否则返回默认值 AI_REQUEST</p>
     *
     * @param code 事件类型的唯一标识码
     * @return 对应的枚举实例, 若未找到则返回 {@link StatisticsEventType#AI_REQUEST} 默认值
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
