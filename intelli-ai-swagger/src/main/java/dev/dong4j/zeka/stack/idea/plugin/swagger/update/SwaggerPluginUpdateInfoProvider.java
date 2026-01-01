package dev.dong4j.zeka.stack.idea.plugin.swagger.update;

import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.update.PluginUpdateInfoProvider;
import dev.dong4j.zeka.stack.idea.plugin.swagger.PluginContents;

/**
 * Swagger Plugin Update Info Provider
 *
 * @author dong4j
 * @since 1.0.0
 */
public class SwaggerPluginUpdateInfoProvider implements PluginUpdateInfoProvider {
    private static final PluginId PLUGIN_ID = PluginId.getId(PluginContents.PLUGIN_ID);

    @Override
    @NotNull
    public PluginId getPluginId() {
        return PLUGIN_ID;
    }
}
