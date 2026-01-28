package dev.dong4j.zeka.stack.idea.plugin.common.diagnostic;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 反馈上下文提供者
 * <p> 子插件通过扩展点注册, 提供来源插件信息与标题前缀.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.28
 * @since 1.0.0
 */
public interface FeedbackContextProvider {
    /**
     * 扩展点名称
     */
    ExtensionPointName<FeedbackContextProvider> EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.ai.feedbackContextProvider");

    /**
     * 获取插件描述符
     * <p> 默认返回 null, 建议继承 {@link AbstractFeedbackContextProvider} 获取自动注入的描述符.</p>
     *
     * @return 插件描述符, 可能为 null
     */
    default @Nullable PluginDescriptor getPluginDescriptor() {
        return null;
    }

    /**
     * 获取插件 ID
     *
     * @return 插件 ID, 可能为空字符串
     */
    default @NotNull String getPluginId() {
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        if (pluginDescriptor == null) {
            return "";
        } else {
            pluginDescriptor.getPluginId();
        }
        String id = pluginDescriptor.getPluginId().getIdString();
        return id.isBlank() ? "" : id;
    }

    /**
     * 获取插件名称
     *
     * @return 插件名称, 可能为空字符串
     */
    default @NotNull String getPluginName() {
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        if (pluginDescriptor == null) {
            return "";
        }
        String name = pluginDescriptor.getName();
        return name == null || name.isBlank() ? "" : name;
    }

    /**
     * 获取插件版本
     *
     * @return 插件版本, 可能为 null
     */
    default @Nullable String getPluginVersion() {
        PluginDescriptor pluginDescriptor = getPluginDescriptor();
        if (pluginDescriptor == null) {
            return null;
        }
        String version = pluginDescriptor.getVersion();
        return version == null || version.isBlank() ? null : version;
    }

    /**
     * 标题前缀
     * <p> 返回空表示使用插件名称作为前缀.</p>
     *
     * @return 标题前缀, 可能为 null
     */
    default @Nullable String getTitlePrefix() {
        return null;
    }
}
