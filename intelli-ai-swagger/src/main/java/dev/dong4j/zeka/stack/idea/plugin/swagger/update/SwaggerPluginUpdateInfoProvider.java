package dev.dong4j.zeka.stack.idea.plugin.swagger.update;

import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.update.PluginUpdateInfoProvider;
import dev.dong4j.zeka.stack.idea.plugin.swagger.PluginContents;

/**
 * Swagger 插件更新信息提供者类
 * <p> 实现插件更新信息的获取功能, 用于标识并提供 Swagger 插件的基本信息
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class SwaggerPluginUpdateInfoProvider implements PluginUpdateInfoProvider {
    /** 插件唯一标识符, 用于标识当前插件的 ID */
    private static final PluginId PLUGIN_ID = PluginId.getId(PluginContents.PLUGIN_ID);

    /**
     * 获取插件 ID
     * <p> 返回当前插件的唯一标识符
     *
     * @return 插件 ID, 不能为 null
     */
    @Override
    @NotNull
    public PluginId getPluginId() {
        return PLUGIN_ID;
    }
}
