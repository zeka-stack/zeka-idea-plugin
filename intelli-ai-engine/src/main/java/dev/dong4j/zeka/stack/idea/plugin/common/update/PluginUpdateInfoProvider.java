package dev.dong4j.zeka.stack.idea.plugin.common.update;

import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.NotNull;

/**
 * 插件更新信息提供者接口
 * <p>
 * 用于子插件向 engine 插件注册插件信息，以便 engine 插件能够检查和管理子插件的更新。
 * 子插件需要实现此接口，并在 plugin.xml 中注册到扩展点。
 * <p>
 * <b>使用示例：</b>
 * <p>
 * 1. 在子插件中创建实现类：
 * <pre>{@code
 * public class MyPluginUpdateInfoProvider implements PluginUpdateInfoProvider {
 *     private static final PluginId PLUGIN_ID = PluginId.getId("com.example.myplugin");
 *
 *     @Override
 *     @NotNull
 *     public PluginId getPluginId() {
 *         return PLUGIN_ID;
 *     }
 * }
 * }</pre>
 * <p>
 * 2. 在子插件的 plugin.xml 中注册：
 * <pre>{@code
 * <extensions defaultExtensionNs="dev.dong4j.zeka.stack.idea.plugin.common.ai">
 *     <pluginUpdateInfoProvider implementation="com.example.MyPluginUpdateInfoProvider"/>
 * </extensions>
 * }</pre>
 * <p>
 * 注册后，engine 插件在检查更新时会自动包含该子插件。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.27
 * @since 1.0.0
 */
public interface PluginUpdateInfoProvider {
    /**
     * 获取插件 ID
     * <p>
     * 返回需要检查更新的插件唯一标识符。该 ID 必须与子插件在 plugin.xml 中定义的 &lt;id&gt; 一致。
     *
     * @return 插件 ID
     * @see PluginId
     */
    @NotNull
    PluginId getPluginId();
}

