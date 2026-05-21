package dev.dong4j.zeka.stack.idea.plugin.kit;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
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
     * 获取插件描述符
     * <p>
     * 将 IntelliJ 插件管理公开 API 集中封装在工具类中, 避免各业务模块直接依赖具体查询实现。
     *
     * @param id 插件 ID
     * @return 插件描述符, 如果插件未启用或获取失败则返回 null
     */
    @Nullable
    public static IdeaPluginDescriptor getPluginDescriptor(String id) {
        try {
            PluginId pluginId = PluginId.getId(id);
            return PluginManager.getInstance().findEnabledPlugin(pluginId);
        } catch (Exception e) {
            log.debug("获取插件描述符失败", e);
        }
        return null;
    }

    /**
     * 获取当前插件版本号
     * <p>
     * 复用插件描述符查询逻辑, 保证所有插件元信息都通过同一个公开 API 入口获取。
     *
     * @param id 插件 ID
     * @return 插件版本号, 如果获取失败则返回 null
     */
    @Nullable
    public static String getVersion(String id) {
        IdeaPluginDescriptor pluginDescriptor = getPluginDescriptor(id);
        return pluginDescriptor != null ? pluginDescriptor.getVersion() : null;
    }
}
