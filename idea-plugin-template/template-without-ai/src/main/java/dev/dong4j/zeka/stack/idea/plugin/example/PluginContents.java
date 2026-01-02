package dev.dong4j.zeka.stack.idea.plugin.example;

/**
 * 插件内容配置类
 * <p> 用于存储插件的基本标识信息, 包括插件 ID 和插件名称, 供插件系统识别和使用
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public final class PluginContents {
    /**
     * 插件的唯一标识符
     * <p>
     * 该常量用于标识插件的唯一 ID, 在整个系统中是唯一的.
     */
    public static final String PLUGIN_ID = "dev.dong4j.zeka.stack.idea.plugin.example";
    /** 插件名称 */
    public static final String PLUGIN_NAME = "Example Plugin";

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 包含插件相关常量, 不允许被实例化
     */
    private PluginContents() {
    }
}
