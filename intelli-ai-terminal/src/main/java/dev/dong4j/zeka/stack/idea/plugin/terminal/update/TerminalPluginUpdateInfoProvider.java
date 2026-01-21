package dev.dong4j.zeka.stack.idea.plugin.terminal.update;

import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.update.PluginUpdateInfoProvider;
import dev.dong4j.zeka.stack.idea.plugin.terminal.PluginContents;

/**
 * 终端插件更新信息提供者
 * <p> 实现了 {@code PluginUpdateInfoProvider} 接口,
 * 负责提供终端插件的唯一标识符 (PluginId), 用于插件的更新检查和管理.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 2025.3.1200
 */
public class TerminalPluginUpdateInfoProvider implements PluginUpdateInfoProvider {
    /**
     * 插件的唯一标识符
     * <p> 此常量用于表示当前插件在系统中的 ID, 确保每个插件具有唯一性
     *
     * @see PluginId
     * @see PluginContents#PLUGIN_ID
     */
    private static final PluginId PLUGIN_ID = PluginId.getId(PluginContents.PLUGIN_ID);

    /**
     * 获取插件的唯一标识符
     * <p> 此方法返回插件的唯一标识符, 确保每个插件在系统中具有唯一的身份标识
     *
     * @return 插件 ID, 表示该插件的唯一身份标识
     */
    @Override
    @NotNull
    public PluginId getPluginId() {
        return PLUGIN_ID;
    }
}
