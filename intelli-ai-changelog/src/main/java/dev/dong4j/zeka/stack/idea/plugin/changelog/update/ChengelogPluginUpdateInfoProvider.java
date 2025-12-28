package dev.dong4j.zeka.stack.idea.plugin.changelog.update;

import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.common.update.PluginUpdateInfoProvider;

/**
 * My Plugin Update Info Provider
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-28 17:35:01
 * @since hello.world
 */
public class ChengelogPluginUpdateInfoProvider implements PluginUpdateInfoProvider {
    private static final PluginId PLUGIN_ID = PluginId.getId(PluginContents.PLUGIN_ID);

    @Override
    @NotNull
    public PluginId getPluginId() {
        return PLUGIN_ID;
    }
}
