package dev.dong4j.zeka.stack.idea.plugin.common.diagnostic;

import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 反馈上下文提供者基类
 * <p> 通过 {@link PluginAware} 自动注入插件描述符, 供子插件获取来源信息.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.28
 * @since 1.0.0
 */
public abstract class AbstractFeedbackContextProvider implements FeedbackContextProvider, PluginAware {
    /** 插件描述符, 用于自动注入插件来源信息, 供子插件获取插件标识, 名称, 版本等上下文数据. */
    private PluginDescriptor pluginDescriptor;

    /**
     * 设置插件描述符
     * <p> 将传入的插件描述符赋值给当前实例的成员变量, 用于后续获取插件相关信息.</p>
     *
     * @param pluginDescriptor 插件描述符对象, 不能为空
     */
    @Override
    public void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * 获取插件描述符
     * <p> 返回当前插件的描述符对象, 如果未设置则返回 null</p>
     *
     * @return 插件描述符对象, 如果未设置则返回 null
     */
    @Override
    public @Nullable PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    /**
     * 获取插件唯一标识符
     * <p> 从插件描述符中获取插件 ID, 若描述符为空或 ID 为空字符串或仅包含空白字符, 则返回空字符串 </p>
     *
     * @return 插件唯一标识符, 若未设置或无效则返回空字符串
     */
    @Override
    public @NotNull String getPluginId() {
        if (pluginDescriptor == null) {
            return "";
        } else {
            pluginDescriptor.getPluginId();
        }
        String id = pluginDescriptor.getPluginId().getIdString();
        return StringUtil.isEmptyOrSpaces(id) ? "" : id;
    }

    /**
     * 获取插件名称
     * <p> 根据插件描述符获取插件名称, 如果插件描述符为空, 则返回空字符串 </p>
     *
     * @return 插件名称, 如果插件描述符为空则返回空字符串
     */
    @Override
    public @NotNull String getPluginName() {
        if (pluginDescriptor == null) {
            return "";
        }
        String name = pluginDescriptor.getName();
        return StringUtil.isEmptyOrSpaces(name) ? "" : name;
    }

    /**
     * 获取插件版本
     * <p> 返回当前插件描述符中定义的版本号. 如果插件描述符为空或版本号字符串为空, 则返回 null.
     *
     * @return 插件版本字符串, 如果获取失败或为空则返回 null
     */
    @Override
    public @Nullable String getPluginVersion() {
        if (pluginDescriptor == null) {
            return null;
        }
        String version = pluginDescriptor.getVersion();
        return StringUtil.isEmptyOrSpaces(version) ? null : version;
    }
}
