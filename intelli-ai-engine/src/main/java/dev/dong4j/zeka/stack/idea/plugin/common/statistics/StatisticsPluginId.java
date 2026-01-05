package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

/**
 * <p> 统计插件 ID 枚举.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public enum StatisticsPluginId {

    /** Engine 插件 */
    ENGINE("engine", "Engine"),

    /** Changelog 插件 */
    CHANGELOG("changelog", "Changelog"),

    /** Javadoc 插件 */
    JAVADOC("javadoc", "Javadoc");

    /**
     * 插件的唯一标识符代码.
     * <p> 该字段用于存储枚举实例对应的代码值.</p>
     *
     * @see StatisticsPluginId
     */
    private final String code;
    /** 描述信息 */
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
     * 获取枚举的代码值
     * <p> 返回当前枚举实例的 code 字段值
     *
     * @return 枚举的代码值
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取枚举项的描述信息
     * <p> 返回该枚举项对应的描述字符串, 用于展示或日志记录等用途.
     *
     * @return 描述字符串
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 枚举代码
     * @return 对应的枚举常量
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
