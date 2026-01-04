package dev.dong4j.zeka.stack.idea.plugin.common.ui.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import dev.dong4j.zeka.stack.idea.plugin.common.ui.listener.HyperLinkListenerImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.panel.HyperLinkJBLabel;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.HtmlConstant;
import dev.dong4j.zeka.stack.idea.plugin.common.util.UrlType;
import dev.dong4j.zeka.stack.idea.plugin.common.util.Urls;
import icons.AICommonIcons;

/**
 * 支持对话框类
 * <p> 此对话框用于显示项目的支持信息, 包括支持内容, 捐赠信息及支付二维码等.
 * <p> 用户可以通过点击对话框中的链接来访问项目的 GitHub 页面, 市场评论页面等, 也可以通过扫描二维码进行捐赠.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class SupportDialog extends DialogWrapper {

    /** 主面板, 用于承载对话框的所有内容组件 */
    private JPanel rootPanel;
    /** 支持信息标题标签, 用于显示插件支持相关的头部文字内容 */
    private JBLabel supportHeader;
    /** 支持内容标签, 用于显示支持信息的文本 */
    private JBLabel supportContent;
    /** 捐赠信息标题标签 */
    private JBLabel donateHeader;
    /**
     * 捐赠内容显示的 JLabel 组件
     * <p>
     * 用于在对话框中显示捐赠相关的文本内容.
     */
    private JBLabel donateContent;
    /**
     * 微信支付图标标签
     * <p> 用于显示微信支付二维码或相关信息
     */
    private JLabel wechatLabel;
    /** 支付宝支付二维码标签 */
    private JLabel alipayLabel;
    /** 捐赠说明链接 */
    private ActionLink donateNote;

    /**
     * 构造支持对话框
     * <p> 创建一个用于显示插件支持和捐赠信息的对话框, 不具有模态性且不可调整大小.
     * <p> 对话框标题设置为支持功能的标题, 确认按钮文本设置为“确定”.
     *
     * @since 1.0.0
     */
    public SupportDialog() {
        super((Project) null);
        setModal(false);
        setResizable(false);
        setTitle(AICommonBundle.message("dialog.support.support.header"));
        setOKButtonText(AICommonBundle.message("dialog.support.ok"));
        init();
    }

    /**
     * 创建对话框的中心面板
     * <p> 初始化支持对话框的界面组件, 包括支持信息, 捐赠信息, 二维码支付选项和链接提示
     * <p> 该方法负责设置对话框的布局和内容, 包括文本, 图标, 链接和交互元素
     *
     * @return 中心面板组件, 返回 rootPanel 作为对话框的主内容区域
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        initComponents();
        initCopyable();
        initFocusable();
        initLinkListener();

        // 设置支持部分
        supportHeader.setIcon(null);
        supportHeader.setText(HtmlConstant.wrapBoldHtml("💡 " + AICommonBundle.message("dialog.support.support.header")));
        supportContent.setText(HtmlConstant.wrapBody(AICommonBundle.message("dialog.support.support.content",
                                                                            Urls.GITHUB_LINK,
                                                                            Urls.MARKETPLACE_REVIEWS_LINK,
                                                                            UrlType.SHARE.getId())));

        // 设置捐赠部分
        donateHeader.setIcon(null);
        donateHeader.setText(HtmlConstant.wrapBoldHtml("❤️ " + AICommonBundle.message("dialog.support.donate.header")));
        donateContent.setText(HtmlConstant.wrapBody(AICommonBundle.message("dialog.support.donate.content")));

        int qrSize = JBUI.scale(170);
        wechatLabel.setIcon(scaleIcon(AICommonIcons.WECHAT_PAY, qrSize, qrSize));
        wechatLabel.setText(HtmlConstant.wrapBoldHtml(AICommonBundle.message("dialog.support.wechat")));

        alipayLabel.setIcon(scaleIcon(AICommonIcons.ALIPAY, qrSize, qrSize));
        alipayLabel.setText(HtmlConstant.wrapBoldHtml(AICommonBundle.message("dialog.support.alipay")));

        donateNote.setIcon(null);
        donateNote.setText("📗 " + AICommonBundle.message("dialog.support.donate.link"));

        return rootPanel;
    }

    /**
     * 初始化对话框组件
     * <p> 创建并配置支持对话框的 UI 结构, 包括支持信息, 捐赠说明和支付二维码等部分
     * <p> 该方法构建了整个面板布局, 并将各个标签, 按钮和图标按照指定格式排列
     */
    private void initComponents() {
        rootPanel = new JPanel(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));

        JPanel contentPanel = new JPanel(new GridLayoutManager(9, 3, JBUI.emptyInsets(), -1, -1));
        rootPanel.add(contentPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
                                                        GridConstraints.FILL_BOTH,
                                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null
            , null, null, 0, false));

        // 支持标题
        JPanel supportHeaderPanel = new JPanel(new GridLayoutManager(1, 1, JBUI.insetsTop(10), -1, -1));
        supportHeaderPanel.setOpaque(false);
        supportHeader = new JBLabel();
        supportHeader.setFont(supportHeader.getFont().deriveFont(Font.BOLD));
        supportHeaderPanel.add(supportHeader, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                                                                  GridConstraints.FILL_HORIZONTAL,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                  GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentPanel.add(supportHeaderPanel, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                                 GridConstraints.FILL_BOTH,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        // 支持分割线
        JPanel supportSeparatorPanel = new JPanel(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        supportSeparatorPanel.add(new JSeparator(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
                                                                        GridConstraints.FILL_HORIZONTAL,
                                                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentPanel.add(supportSeparatorPanel, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                                    GridConstraints.FILL_BOTH,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        // 支持内容
        JPanel supportContentPanel = new JPanel(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        supportContent = new HyperLinkJBLabel();
        supportContentPanel.add(supportContent, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                                                                    GridConstraints.FILL_HORIZONTAL,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        contentPanel.add(supportContentPanel, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                                  GridConstraints.FILL_BOTH,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        // 捐赠标题
        JPanel donateHeaderPanel = new JPanel(new GridLayoutManager(1, 1, JBUI.insetsTop(20), -1, -1));
        donateHeaderPanel.setOpaque(false);
        donateHeader = new JBLabel();
        donateHeader.setFont(donateHeader.getFont().deriveFont(Font.BOLD));
        donateHeaderPanel.add(donateHeader, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                                                                GridConstraints.FILL_HORIZONTAL,
                                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentPanel.add(donateHeaderPanel, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                                GridConstraints.FILL_BOTH,
                                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        // 捐赠分割线
        JPanel donateSeparatorPanel = new JPanel(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        donateSeparatorPanel.add(new JSeparator(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
                                                                       GridConstraints.FILL_HORIZONTAL,
                                                                       GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                       GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentPanel.add(donateSeparatorPanel, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                                   GridConstraints.FILL_BOTH,
                                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                   GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        // 捐赠内容
        JPanel donateContentPanel = new JPanel(new GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1));
        donateContent = new JBLabel();
        donateContentPanel.add(donateContent, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                                                                  GridConstraints.FILL_HORIZONTAL,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                  GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        contentPanel.add(donateContentPanel, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                                 GridConstraints.FILL_BOTH,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                 GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        // 二维码区域
        JPanel paymentPanel = new JPanel(new GridLayoutManager(1, 2, JBUI.insets(30, 10, 0, 0), -1, -1));
        wechatLabel = new JLabel();
        wechatLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wechatLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        wechatLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        paymentPanel.add(wechatLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
                                                          GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                                          GridConstraints.SIZEPOLICY_FIXED,
                                                          null, null, null, 0, false));

        alipayLabel = new JLabel();
        alipayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        alipayLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        alipayLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        paymentPanel.add(alipayLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER,
                                                          GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                                                          GridConstraints.SIZEPOLICY_FIXED,
                                                          null, null, null, 0, false));

        contentPanel.add(paymentPanel, new GridConstraints(6, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                           GridConstraints.FILL_BOTH,
                                                           GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                           GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                           null, null, null, 0, false));

        // 捐赠指南链接
        JPanel linkPanel = new JPanel(new GridLayoutManager(1, 2, JBUI.insets(30, 10, 0, 0), -1, -1));
        donateNote = new ActionLink();
        linkPanel.add(donateNote, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST,
                                                      GridConstraints.FILL_NONE,
                                                      GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                      GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        linkPanel.add(new Spacer(), new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER,
                                                        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
                                                        GridConstraints.SIZEPOLICY_FIXED,
                                                        null, null, null, 0, false));
        contentPanel.add(linkPanel, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                        GridConstraints.FILL_BOTH,
                                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null
            , null, null, 0, false));

        // 底部占位
        contentPanel.add(new Spacer(), new GridConstraints(8, 0, 1, 3, GridConstraints.ANCHOR_CENTER,
                                                           GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED,
                                                           GridConstraints.SIZEPOLICY_WANT_GROW,
                                                           null, null, null, 0, false));
    }

    /**
     * 初始化组件不可聚焦状态
     * <p> 将对话框中的所有主要组件设置为不可获取焦点, 以提升用户体验和界面交互的一致性
     */
    private void initFocusable() {
        rootPanel.setFocusable(false);
        supportHeader.setFocusable(false);
        supportContent.setFocusable(false);
        donateHeader.setFocusable(false);
        donateContent.setFocusable(false);
        wechatLabel.setFocusable(false);
        alipayLabel.setFocusable(false);
        donateNote.setFocusable(false);
    }

    /**
     * 缩放图标到指定尺寸
     * <p> 将传入的图标按照指定的宽度和高度进行缩放, 并返回缩放后的图标. 如果原始图标为 null, 则直接返回 null.
     *
     * @param icon   原始图标, 可以为 null
     * @param width  目标宽度
     * @param height 目标高度
     * @return 缩放后的图标, 如果原始图标为 null 则返回 null
     */
    private Icon scaleIcon(Icon icon, int width, int height) {
        if (icon == null) {
            return null;
        }
        Image image = iconToImage(icon);
        if (image != null) {
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        }
        return icon;
    }

    /**
     * 将 Icon 转换为 Image
     * <p> 该方法用于将给定的 Icon 对象转换为 Image 对象, 以便进行后续处理或显示.
     *
     * @param icon 图标对象, 不能为 null
     * @return 转换后的图像对象, 如果图标为空则返回 null
     */
    private Image iconToImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
    }

    /**
     * 初始化链接监听器
     * <p> 为捐赠说明链接添加点击事件监听器, 当用户点击链接时, 弹出一个带有捐赠信息的气泡提示.
     * <p> 气泡提示的位置根据链接所在组件的中心位置计算, 并设置在组件上方.
     * <p> 气泡提示的内容包括捐赠者的名单, 联系邮箱等信息.
     *
     * @since 1.0.0
     */
    private void initLinkListener() {
        donateNote.addActionListener(new AbstractAction() {
            /**
             * 处理动作事件, 显示捐赠提示弹窗
             * <p> 从事件中获取组件来源, 并在组件的相对位置上方显示一个 HTML 格式的气球弹窗, 包含捐赠信息和超链接
             * <p> 弹窗设置为点击, 点击外部, 窗口调整大小, 按键按下时自动隐藏
             *
             * @param e 动作事件对象
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                Component source = (Component) e.getSource();
                RelativePoint relativePoint = new RelativePoint(source, new Point(source.getWidth() / 2, source.getHeight() - 25));

                JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(
                        AICommonBundle.message("dialog.support.donate.note",
                                               Urls.DONORS_LIST_LINK, UrlType.MAIL.getId(), Urls.EMAIL_LINK),
                        null,
                        JBUI.CurrentTheme.NotificationInfo.backgroundColor(),
                        new HyperLinkListenerImpl())
                    .setShadow(true)
                    .setHideOnAction(true)
                    .setHideOnClickOutside(true)
                    .setHideOnFrameResize(true)
                    .setHideOnKeyOutside(true)
                    .setHideOnLinkClick(true)
                    .setContentInsets(JBUI.insets(10))
                    .createBalloon()
                    .show(relativePoint, Balloon.Position.above);
            }
        });
    }

    /**
     * 初始化支持和捐赠信息文本的可复制功能
     * <p>为对话框中的支持和捐赠信息文本 (包括标题和内容) 启用复制功能, 方便用户复制链接或文本信息
     *
     * @author dong4j
     * @version 1.0.0
     * @since 1.0.0
     */
    private void initCopyable() {
        supportHeader.setCopyable(true);
        supportContent.setCopyable(true);
        donateHeader.setCopyable(true);
        donateContent.setCopyable(true);
    }

    /**
     * 创建对话框操作按钮数组
     * <p> 该方法返回对话框底部显示的操作按钮列表, 当前仅包含确认按钮
     *
     * @return 包含确认按钮的操作数组
     */
    @Override
    protected Action @NotNull [] createActions() {
        List<Action> actions = new ArrayList<>();
        actions.add(getOKAction());
        return actions.toArray(new Action[0]);
    }

    /**
     * 获取首选聚焦组件
     * <p> 返回 null 表示没有首选聚焦组件
     *
     * @return 首选聚焦组件, 若无则返回 null
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    /**
     * 显示对话框
     * <p> 在事件调度线程中显示对话框, 确保界面更新的线程安全
     *
     * @since 1.0.0
     */
    @Override
    public void show() {
        ApplicationManager.getApplication().invokeLater(super::show);
    }
}
