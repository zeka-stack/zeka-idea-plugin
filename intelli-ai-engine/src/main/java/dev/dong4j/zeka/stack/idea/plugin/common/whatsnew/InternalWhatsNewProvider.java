package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.extensions.PluginDescriptor;

import java.util.List;

/**
 * 内部 What's New 提供器类
 * <p> 实现了 WhatsNewProvider 接口, 用于提供插件的更新信息页面. 该类定义了一个默认的更新页面列表,
 * 并提供了获取显示名称, 基础路径, 更新页面列表, 启动时是否显示以及插件描述符的方法.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class InternalWhatsNewProvider implements WhatsNewProvider {
    /**
     * 内部 What's New 页面列表, 包含初始版本和对应的 HTML 文件名.
     * <p> 此列表用于定义应用程序启动时显示的 What's New 页面.
     *
     * @see DefaultWhatsNewPage
     */
    private static final List<WhatsNewPage> PAGES = List.of(
        // version mark
        new DefaultWhatsNewPage("2025.3.1", "2025_3_1.html"),
        new DefaultWhatsNewPage("1.8.0", "1_8_0.html"),
        new DefaultWhatsNewPage("1.7.0", "1_7_0.html"),
        new DefaultWhatsNewPage("1.6.0", "1_6_0.html"),
        new DefaultWhatsNewPage("1.5.0", "1_5_0.html"),
        new DefaultWhatsNewPage("1.4.0", "1_4_0.html"),
        new DefaultWhatsNewPage("1.3.1", "1_3_1.html"),
        new DefaultWhatsNewPage("1.3.0", "1_3_0.html"),
        new DefaultWhatsNewPage("1.2.0", "1_2_0.html"),
        new DefaultWhatsNewPage("1.1.0", "1_1_0.html"),
        new DefaultWhatsNewPage("1.0.0", "1_0_0.html")
                                                           );
    /** 插件描述信息 */
    private PluginDescriptor pluginDescriptor;

    /**
     * 获取插件的显示名称
     * <p> 返回插件在界面上显示的名称, 用于标识该插件
     *
     * @return 插件的显示名称
     */
    @Override
    public String getDisplayName() {
        return "Engine";
    }

    /**
     * 获取基础路径
     * <p> 返回用于访问版本更新页面的基础 URL 路径
     *
     * @return 基础路径字符串, 格式为 "/whatsnew/"
     */
    @Override
    public String getBasePath() {
        return "/whatsnew/";
    }

    /**
     * 获取插件的更新页面列表
     * <p> 返回预定义的更新页面列表, 用于展示插件的版本更新信息.
     *
     * @return 更新页面列表
     */
    @Override
    public List<WhatsNewPage> getPages() {
        return PAGES;
    }

    /**
     * 判断是否应在启动时显示更新信息
     * <p> 该方法返回 true 表示应在应用程序启动时显示更新信息,false 表示不显示.
     *
     * @return 是否应在启动时显示更新信息
     */
    @Override
    public boolean shouldDisplayAtStartup() {
        return true;
    }

    /**
     * 获取插件描述信息
     * <p> 返回当前插件的描述信息对象, 用于获取插件的元数据信息.
     *
     * @return 插件描述信息对象, 若未设置则返回 null
     */
    @Override
    public PluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    /**
     * 设置插件描述符
     * <p> 用于设置当前插件的描述信息, 通常在插件初始化时调用
     *
     * @param pluginDescriptor 插件描述符对象
     */
    @Override
    public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    /**
     * 默认的 WhatsNewPage 实现类
     * <p> 提供 WhatsNewPage 接口的默认实现, 用于存储和获取版本号及文件名信息
     *
     * @param version  软件版本号
     *                 <p> 表示当前 What's New 页面对应的软件版本
     * @param fileName 文件名
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.10.24
     * @since 1.0.0
     */
    private record DefaultWhatsNewPage(String version, String fileName) implements WhatsNewPage {
        /**
         * 初始化一个新的 DefaultWhatsNewPage 实例
         * <p> 构造函数用于创建一个 DefaultWhatsNewPage 对象, 设置版本号和文件名
         *
         * @param version  版本号
         * @param fileName 文件名
         */
        private DefaultWhatsNewPage {
        }

        /**
         * 获取当前版本号
         *
         * @return 返回版本字符串
         */
        @Override
        public String version() {
            return version;
        }

        /**
         * 获取文件名
         * <p> 返回与当前对象关联的文件名
         *
         * @return 文件名
         */
        @Override
        public String fileName() {
            return fileName;
        }
    }
}
