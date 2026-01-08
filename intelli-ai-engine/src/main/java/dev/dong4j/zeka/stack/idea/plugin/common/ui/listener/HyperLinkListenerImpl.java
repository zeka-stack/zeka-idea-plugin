package dev.dong4j.zeka.stack.idea.plugin.common.ui.listener;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Point;
import java.net.URI;
import java.util.Objects;

import javax.swing.event.HyperlinkEvent;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.PlatformUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.util.UrlType;

/**
 * 超链接监听器实现类
 * <p> 继承自 HyperlinkAdapter, 用于处理超链接激活事件. 根据不同的 URL 类型执行相应的操作, 例如分享链接和邮件链接.
 * <p> 具体功能如下:
 * <ul>
 * <li> 当 URL 类型为 SHARE 时, 执行分享链接操作, 并将链接地址复制到剪贴板, 同时显示一个通知气泡.</li>
 * <li> 当 URL 类型为 MAIL 时, 尝试打开默认邮件客户端发送邮件.</li>
 * <li> 对于其他类型的 URL, 使用浏览器打开.</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class HyperLinkListenerImpl extends HyperlinkAdapter {

    /**
     * 静态常量, 用于记录日志
     * <p> 获取一个与 HyperLinkListenerImpl 类相关的 Logger 实例
     *
     * @see Logger
     */
    private static final Logger LOG = Logger.getInstance(HyperLinkListenerImpl.class);

    /**
     * 处理超链接激活事件
     * <p> 根据超链接的描述判断其类型, 并执行相应的操作. 支持分享链接, 邮件链接以及其他普通链接的打开.
     *
     * @param e 超链接事件, 不能为 null
     */
    @Override
    protected void hyperlinkActivated(@NotNull HyperlinkEvent e) {
        String url = e.getDescription();
        if (Objects.equals(UrlType.SHARE.getId(), url)) {
            execShareLinkAction(e, UrlType.SHARE.getUrl());
        } else if (Objects.equals(UrlType.MAIL.getId(), url)) {
            execEmailLinkAction(UrlType.MAIL.getUrl());
        } else {
            BrowserUtil.browse(url);
        }
    }

    /**
     * 执行邮件链接操作
     * <p> 尝试使用系统默认的邮件客户端打开指定的邮件 URL
     * <p> 如果打开失败, 记录错误日志
     *
     * @param url 邮件 URL
     */
    private void execEmailLinkAction(String url) {
        try {
            Desktop.getDesktop().mail(new URI(url));
        } catch (Exception ex) {
            LOG.debug("Failed to open email", ex);
        }
    }

    /**
     * 执行分享链接操作
     * <p> 将分享链接复制到剪贴板, 并显示一个通知气泡提示用户已成功分享
     * <p> 通知气泡会在 5 秒后自动消失, 或者在用户点击外部区域, 窗口调整大小或按下外部键时消失
     *
     * @param e   超链接事件, 不能为 null
     * @param url 分享链接 URL, 不能为 null
     */
    private void execShareLinkAction(@NotNull HyperlinkEvent e, String url) {
        PlatformUtil.setClipboard(url);
        Component component = e.getInputEvent().getComponent();
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                AICommonBundle.message("dialog.support.share"),
                null,
                JBUI.CurrentTheme.NotificationInfo.backgroundColor(),
                null)
            .setShadow(true)
            .setHideOnAction(true)
            .setHideOnClickOutside(true)
            .setHideOnFrameResize(true)
            .setHideOnKeyOutside(true)
            .setHideOnLinkClick(true)
            .setFadeoutTime(5000L)
            .createBalloon()
            .show(new RelativePoint(component, new Point(component.getWidth() / 4, component.getHeight())), Balloon.Position.below);
    }
}

