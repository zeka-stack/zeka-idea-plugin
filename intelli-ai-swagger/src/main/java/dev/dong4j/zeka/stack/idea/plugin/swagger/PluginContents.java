package dev.dong4j.zeka.stack.idea.plugin.swagger;

/**
 * 插件内容常量类
 * <p> 用于定义插件的标识符和名称等静态常量信息, 确保插件在系统中具有唯一性和可识别性.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public final class PluginContents {
    /** 插件唯一标识符, 用于在插件系统中识别该插件 */
    public static final String PLUGIN_ID = "dev.dong4j.zeka.stack.idea.plugin.swagger";
    /** 插件名称, 值为 "IntelliAI Swagger" */
    public static final String PLUGIN_NAME = "IntelliAI Swagger";

    /**
     * 私有构造函数, 防止外部实例化
     * <p> 该类为工具类, 包含插件相关常量, 不允许被实例化
     */
    private PluginContents() {
    }
}
