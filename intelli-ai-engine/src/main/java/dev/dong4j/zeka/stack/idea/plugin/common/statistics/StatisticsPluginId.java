package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import lombok.Getter;

/**
 * 统计插件标识枚举类
 * <p> 用于标识系统中不同统计插件的唯一编码及其描述信息, 支持通过编码快速查找对应的枚举值.
 * 适用于插件系统中对统计功能模块的区分与管理.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
@Getter
public enum StatisticsPluginId {

    /** Engine 插件标识, 代码为 "engine", 描述为 "Engine" */
    ENGINE("engine", "Engine"),

    /** Changelog 插件标识代码及描述 */
    CHANGELOG("changelog", "Changelog"),

    /** Javadoc 插件标识代码及描述 */
    JAVADOC("javadoc", "Javadoc");

    /** 插件标识代码, 不能为空 */
    private final String code;

    /** 插件描述信息, 不能为空 */
    private final String description;

    /**
     * 构造函数, 初始化插件 ID 枚举项的代码和描述
     *
     * @param code        插件标识代码, 不能为空
     * @param description 插件描述信息, 不能为空
     */
    StatisticsPluginId(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据插件标识代码获取对应的枚举常量
     * <p> 遍历所有枚举值, 匹配传入的代码, 若找到则返回对应枚举, 否则默认返回 ENGINE 枚举常量 </p>
     *
     * @param code 插件标识代码, 不能为空
     * @return 对应的 StatisticsPluginId 枚举常量, 若未找到则返回 ENGINE
     */
    public static StatisticsPluginId fromCode(String code) {
        for (StatisticsPluginId id : values()) {
            if (id.code.equals(code)) {
                return id;
            }
        }
        return ENGINE;
    }
}
