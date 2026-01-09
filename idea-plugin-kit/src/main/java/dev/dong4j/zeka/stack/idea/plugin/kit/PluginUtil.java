package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

import org.jetbrains.annotations.Nullable;

import lombok.extern.slf4j.Slf4j;


/**
 * 插件工具类
 * <p> 提供获取指定插件版本号的功能, 通过插件 ID 查询 IntelliJ IDEA 插件管理器中的插件描述符并返回其版本信息.
 * <p> 若插件不存在或查询过程中发生异常, 将返回 null, 并在日志中记录错误信息.
 * <p> 使用示例:
 * <pre>{@code
 * String version = PluginUtil.getVersion("com.intellij.java");
 * if (version != null) {*     System.out.println("插件版本:" + version);
 * }
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.10
 * @since 1.0.0
 */
@Slf4j
public class PluginUtil {

    /**
     * 获取当前插件版本号
     * <p>
     * 通过 PluginManagerCore 获取插件的版本信息。
     *
     * @return 插件版本号，如果获取失败则返回 null
     */
    @Nullable
    public static String getVersion(String id) {
        try {
            PluginId pluginId = PluginId.getId(id);
            IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);
            if (pluginDescriptor != null) {
                return pluginDescriptor.getVersion();
            }
        } catch (Exception e) {
            log.trace("获取插件版本号失败", e);
        }
        return null;
    }
}
