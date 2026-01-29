package dev.dong4j.zeka.stack.idea.plugin.common.chat;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 加载聊天页面 HTML 并注入 IDE 主题
 */
final class AIChatHtmlLoader {
    private static final Logger LOG = Logger.getInstance(AIChatHtmlLoader.class);
    private static final String RESOURCE_PATH = "/html/engine-chat.html";

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

    private String injectIdeTheme(@NotNull String html) {
        boolean isDark = StartupUiUtil.INSTANCE.isDarkTheme();
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

    private String fallbackHtml() {
        return "<!DOCTYPE html>" +
            "<html><head><meta charset=\"UTF-8\"><title>IntelliAI Chat</title>" +
            "<style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;" +
            "background:#1e1e1e;color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;}" +
            ".error{text-align:center;padding:40px;}h1{color:#f85149;}</style></head>" +
            "<body><div class=\"error\"><h1>无法加载聊天界面</h1><p>请检查资源文件是否存在</p></div></body></html>";
    }
}
