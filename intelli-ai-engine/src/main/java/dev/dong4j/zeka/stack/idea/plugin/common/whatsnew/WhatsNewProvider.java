package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;

import java.util.List;

/**
 * 提供插件相关的“新功能”信息的接口
 * <p> 该接口用于定义插件提供“新功能”页面的规范, 包含显示名称, 基础路径, 页面列表, 是否在启动时显示以及插件描述等信息
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.24
 * @since 1.0.0
 */
public interface WhatsNewProvider extends PluginAware {
    /**
     * 返回在对话框选项卡中显示的名称.
     *
     * @return 提供者显示名称
     */
    String getDisplayName();

    /**
     * Returns the base path for HTML resources.
     *
     * @return base path
     */
    String getBasePath();

    /**
     * Returns the list of what's new pages.
     *
     * @return 页面列表
     */
    List<WhatsNewPage> getPages();

    /**
     * 是否应在启动时显示对话框.
     *
     * @return true 表示在启动时显示,false 表示不显示
     */
    boolean shouldDisplayAtStartup();

    /**
     * 获取插件描述符
     * <p> 返回插件的描述信息, 用于插件管理和展示
     *
     * @return 插件描述符对象
     */
    PluginDescriptor getPluginDescriptor();
}
