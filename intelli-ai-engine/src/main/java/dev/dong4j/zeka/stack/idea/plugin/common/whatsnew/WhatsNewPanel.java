package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.ide.util.TipUIUtil;
import com.intellij.ide.util.TipUIUtil.Browser;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ResourceUtil;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.JBUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

/**
 * 用于显示“新功能”或“更新日志”的面板组件
 * <p> 该类继承自 JPanel, 主要用于展示插件或应用的更新内容, 支持翻页查看不同版本的更新日志.
 * 支持通过 {@link WhatsNewProvider} 提供的页面数据动态加载 HTML 格式的更新内容.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class WhatsNewPanel extends JPanel {
    /**
     * 浏览器组件, 用于显示 "What's New" 内容
     *
     * @see TipUIUtil#createBrowser()
     */
    private final Browser browser = TipUIUtil.createBrowser();
    /**
     * 提供者对象, 用于获取更新页面列表
     *
     * @see WhatsNewProvider
     */
    private WhatsNewProvider provider;
    /**
     * 存储 WhatsNew 页面列表
     *
     * @see WhatsNewPage
     */
    private List<WhatsNewPage> pages = new ArrayList<>();
    /**
     * 当前显示的变更日志页面索引
     *
     * @see #newerChangelog()*@see #olderChangelog()
     */
    private int pageIndex;

    /**
     * 构造函数, 初始化 WhatsNewPanel 组件
     * <p> 设置布局为 BorderLayout, 并创建一个带有边框的滚动面板, 其中包含浏览器组件.
     * 滚动面板用于显示变更日志页面.
     *
     * @since hello.world
     */
    public WhatsNewPanel() {
        super(new BorderLayout());
        browser.getComponent().setBorder(JBUI.Borders.empty(8, 12));
        var scrollPane = ScrollPaneFactory.createScrollPane(browser.getComponent(), true);
        scrollPane.setBorder(JBUI.Borders.customLine(new JBColor(0xd9d9d9, 0x515151), 0, 0, 1, 0));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 获取组件的首选大小
     * <p> 此方法重写自父类, 指定组件的默认宽度为 640 像素, 高度为 360 像素.
     *
     * @return 组件的首选大小, 宽度为 640 像素, 高度为 360 像素
     */
    @Override
    public Dimension getPreferredSize() {
        return new JBDimension(640, 360);
    }

    /**
     * 设置 WhatsNewProvider 提供者
     * <p> 更新页面列表并设置初始变更日志
     *
     * @param provider WhatsNewProvider 实例
     */
    public void setProvider(WhatsNewProvider provider) {
        this.provider = provider;
        this.pages = new ArrayList<>(provider.getPages());
        setInitialChangelog();
    }

    /**
     * 判断是否有更新的日志页面
     * <p> 检查当前页面索引是否大于 0, 以确定是否存在更早的日志页面
     *
     * @return 是否存在更新的日志页面
     */
    public boolean hasNewer() {
        return pageIndex > 0;
    }

    /**
     * 判断是否存在更旧的日志页面
     * <p> 通过当前页面索引判断是否还有后续的变更日志页面
     *
     * @return 如果存在更旧的日志页面则返回 true, 否则返回 false
     */
    public boolean hasOlder() {
        return pageIndex >= 0 && pageIndex < pages.size() - 1;
    }

    /**
     * 跳转到更新的日志页面
     * <p> 如果存在更新的日志页面, 则将当前页面索引减一, 并加载对应页面内容
     *
     */
    public void newerChangelog() {
        if (hasNewer()) {
            setChangelog(pages.get(--pageIndex).fileName());
        }
    }

    /**
     * 移动到更早的日志页面并显示对应内容
     * <p> 检查是否存在更早的日志页面, 如果存在则将当前页面索引加 1, 并加载对应页面内容
     *
     */
    public void olderChangelog() {
        if (hasOlder()) {
            setChangelog(pages.get(++pageIndex).fileName());
        }
    }

    /**
     * 获取当前显示的变更日志版本号
     * <p> 返回当前页面对应的版本信息, 如果页面列表为空则返回 null
     *
     * @return 当前显示的变更日志版本号, 如果页面列表为空则返回 null
     */
    public String currentVersion() {
        if (pages.isEmpty()) {
            return null;
        }
        return pages.get(pageIndex).version();
    }

    /**
     * 获取更新的日志版本号
     * <p> 返回当前页面前一个页面的版本号, 如果不存在更新的日志页面则返回 null
     *
     * @return 更新的日志版本号, 如果不存在则返回 null
     */
    public String newerVersion() {
        if (!hasNewer()) {
            return null;
        }
        return pages.get(pageIndex - 1).version();
    }

    /**
     * 获取当前页面的旧版本号
     * <p> 如果存在更旧的日志页面, 则返回当前页面下一个页面的版本号, 否则返回 null
     *
     * @return 当前页面的旧版本号, 如果不存在则返回 null
     */
    public String olderVersion() {
        if (!hasOlder()) {
            return null;
        }
        return pages.get(pageIndex + 1).version();
    }

    /**
     * 初始化显示变更日志内容
     * <p> 如果页面列表为空, 则设置一个空的 HTML 内容; 否则将页面索引设置为 0, 并加载第一个页面的变更日志内容.
     *
     */
    private void setInitialChangelog() {
        if (pages.isEmpty()) {
            browser.setText("<html><body></body></html>");
            return;
        }
        pageIndex = 0;
        setChangelog(pages.get(pageIndex).fileName());
    }

    /**
     * 设置变更日志内容
     * <p> 根据指定的文件名加载变更日志内容并设置到浏览器组件中.
     * 如果文件名为空或仅包含空白字符, 将抛出异常.
     * 如果文件未找到, 也将抛出异常.
     * 如果 HTML 解析失败（例如包含不支持的 CSS 属性）, 将显示错误消息.
     *
     * @param fileName 变更日志文件名
     */
    private void setChangelog(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalStateException("Whats new file name cannot be blank. Provider: " + provider.getClass().getName());
        }

        ClassLoader classLoader = provider.getPluginDescriptor() != null
            ? provider.getPluginDescriptor().getPluginClassLoader()
            : getClass().getClassLoader();

        try (InputStream stream = ResourceUtil.getResourceAsStream(classLoader, provider.getBasePath(), fileName)) {
            if (stream == null) {
                throw new IllegalStateException("Whats new file not found: " + fileName);
            }
            String htmlContent = ResourceUtil.loadText(stream);
            try {
                browser.setText(htmlContent);
            } catch (Exception e) {
                // Swing 的 HTML 解析器可能不支持某些 CSS 属性，显示错误消息
                String errorHtml = "<html><body>" +
                    "<h3>无法显示内容</h3>" +
                    "<p>HTML 内容包含不支持的 CSS 属性，无法在 Swing 浏览器中正确显示。</p>" +
                    "<p>错误信息: " + e.getMessage() + "</p>" +
                    "</body></html>";
                browser.setText(errorHtml);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load whats new page: " + fileName, ex);
        }
    }
}
