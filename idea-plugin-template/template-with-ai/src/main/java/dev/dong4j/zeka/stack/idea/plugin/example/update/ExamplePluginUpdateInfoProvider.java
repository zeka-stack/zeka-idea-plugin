package dev.dong4j.zeka.stack.idea.plugin.example.update;

import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.update.PluginUpdateInfoProvider;
import dev.dong4j.zeka.stack.idea.plugin.example.PluginContents;

/**
 * 插件更新信息提供者类
 * <p> 用于提供插件的唯一标识符, 以便在插件管理系统中识别和更新该插件
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class ExamplePluginUpdateInfoProvider implements PluginUpdateInfoProvider {
    /**
     * 插件的唯一标识符
     * <p> 此常量用于表示当前插件的 ID, 确保每个插件在系统中具有唯一的标识
     *
     * @see PluginId
     */
    private static final PluginId PLUGIN_ID = PluginId.getId(PluginContents.PLUGIN_ID);

    /**
     * 获取插件的唯一标识符
     *
     * @return 插件 ID, 表示该插件的唯一身份标识
     */
    @Override
    @NotNull
    public PluginId getPluginId() {
        return PLUGIN_ID;
    }
}
