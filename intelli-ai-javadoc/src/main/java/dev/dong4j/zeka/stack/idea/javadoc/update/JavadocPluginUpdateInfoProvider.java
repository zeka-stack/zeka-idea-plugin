package dev.dong4j.zeka.stack.idea.javadoc.update;

import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.common.update.PluginUpdateInfoProvider;

/**
 * My Plugin Update Info Provider
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-28 17:35:01
 * @since hello.world
 */
public class JavadocPluginUpdateInfoProvider implements PluginUpdateInfoProvider {
    /** 插件 ID, 用于标识当前插件的唯一标识符 */
    private static final PluginId PLUGIN_ID = PluginId.getId(PluginContents.PLUGIN_ID);

    /**
     * 获取插件的唯一标识符
     *
     * @return 插件 ID, 表示该插件的唯一标识, 不会为 null
     */
    @Override
    @NotNull
    public PluginId getPluginId() {
        return PLUGIN_ID;
    }
}
