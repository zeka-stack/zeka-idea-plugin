package dev.dong4j.zeka.stack.idea.plugin.terminal;

/**
 * 插件内容定义类
 * <p> 该类用于定义插件的元数据信息, 包括插件 ID 和名称等常量.
 * <p> 此类为 final 类型, 并通过私有构造函数防止实例化.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public final class PluginContents {
    /** 插件唯一标识符, 用于在插件系统中识别该插件 */
    public static final String PLUGIN_ID = "dev.dong4j.zeka.stack.idea.plugin.terminal";
    /** 插件名称 */
    public static final String PLUGIN_NAME = "IntelliAI Terminal";

    /**
     * 私有构造函数, 防止外部实例化 {@link PluginContents}.
     *
     * <p> 由于该类仅包含静态成员, 构造函数被声明为私有以阻止对象创建, 保证类的工具性质不被破坏.
     *
     * @since 1.0.0
     */
    private PluginContents() {
    }
}
