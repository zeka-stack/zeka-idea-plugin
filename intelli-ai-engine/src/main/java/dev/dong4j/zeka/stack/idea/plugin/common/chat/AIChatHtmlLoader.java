package dev.dong4j.zeka.stack.idea.plugin.common.chat;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * AI 聊天页面 HTML 加载器
 * <p>负责从资源文件中加载聊天界面的 HTML 模板, 并注入当前 IDE 主题样式和背景色, 支持暗色 / 亮色主题自动切换.
 * 若资源文件缺失或加载失败, 将返回默认的错误提示 HTML 页面.
 * 该类主要用于在 IntelliJ 平台中动态渲染 AI 聊天界面, 确保与当前 IDE 环境视觉风格一致.
 * </p>
 * <p>核心功能包括:</p>
 * <ul>
 *   <li>从类路径加载 <code>/html/engine-chat.html</code> 资源文件</li>
 *   <li>根据当前 IDE 主题 (暗色 / 亮色) 注入对应 CSS 样式和主题变量</li>
 *   <li>自动注入背景颜色以匹配当前 IDE 面板背景</li>
 *   <li>若资源加载失败, 提供兜底的错误提示 HTML 页面</li>
 * </ul>
 * <pre>{@code
 * String html = new AIChatHtmlLoader().loadHtml();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.29
 * @since 1.0.0
 */
final class AIChatHtmlLoader {
    /** 日志记录器, 用于记录 AI 聊天页面加载过程中的调试和警告信息 */
    private static final Logger LOG = Logger.getInstance(AIChatHtmlLoader.class);
    /** 聊天页面 HTML 资源文件路径 */
    private static final String RESOURCE_PATH = "/html/engine-chat.html";

    /**
     * 加载聊天页面 HTML 并注入 IDE 主题
     * <p> 首先尝试从指定资源路径加载 HTML 文件, 如果加载失败则记录警告日志并返回备用 HTML.
     * 成功加载后, 调用注入 IDE 主题的方法并返回处理后的 HTML 内容.
     *
     * @return 处理后的 HTML 字符串
     */
    @NotNull
    String loadHtml() {
        try (InputStream is = AIChatHtmlLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                LOG.warn("无法找到聊天页面资源: " + RESOURCE_PATH);
                return fallbackHtml();
            }
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return injectIdeTheme(html);
        } catch (Exception e) {
            LOG.warn("加载聊天页面失败: " + e.getMessage(), e);
            return fallbackHtml();
        }
    }

    /**
     * 向 HTML 内容中注入当前 IDE 的主题样式
     * <p> 根据当前 IDE 的深色 / 浅色主题设置背景颜色, 并在页面头部插入主题标识脚本 </p>
     *
     * @param html 原始 HTML 内容
     * @return 注入了主题样式的 HTML 内容
     */
    private String injectIdeTheme(@NotNull String html) {
        // 使用公共 API 检测主题: 通过面板背景色判断是否为深色主题
        boolean isDark = ColorUtil.isDark(UIUtil.getPanelBackground());
        String theme = isDark ? "dark" : "light";
        Color bg = UIUtil.getPanelBackground();
        String bgColor = StringUtil.toUpperCase(String.format("#%02x%02x%02x", bg.getRed(), bg.getGreen(), bg.getBlue()));

        // html/body 内联背景，避免首帧闪烁
        html = html.replaceFirst(
            "<html([^>]*)>",
            "<html$1 style=\"background-color:" + bgColor + ";\">"
        );
        html = html.replaceFirst(
            "<body([^>]*)>",
            "<body$1 style=\"background-color:" + bgColor + ";\">"
        );

        String scriptInjection = "\n    <script>window.__INITIAL_IDE_THEME__ = '" + theme + "';</script>";
        int headIndex = html.indexOf("<head>");
        if (headIndex != -1) {
            int insertPos = headIndex + "<head>".length();
            html = html.substring(0, insertPos) + scriptInjection + html.substring(insertPos);
        }

        return html;
    }

    /**
     * 返回默认的聊天页面 HTML 内容
     * <p> 当无法加载聊天页面资源时, 返回一个静态的错误页面 HTML 字符串, 提示用户无法加载聊天界面并建议检查资源文件是否存在.</p>
     *
     * @return 默认的错误页面 HTML 字符串
     */
    private String fallbackHtml() {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"><title>IntelliAI Chat</title>" +
            "<style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;" +
            "background:#1e1e1e;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}" +
            ".error{text-align:center;padding:40px;}h1{color:#f85149;}</style></head>" +
            "<body><div class=\"error\"><h1>无法加载聊天界面</h1><p>请检查资源文件是否存在</p></div></body></html>";
    }
}
