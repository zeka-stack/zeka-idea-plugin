package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import lombok.Getter;

/**
 * 个人信息面板
 * <p>
 * 可复用的个人信息展示组件，用于在插件设置页面展示作者信息。
 * 包含头像、姓名、简介、社交媒体链接等信息。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.12.07
 * @since 2.2.0
 */
@SuppressWarnings("all")
public class PersonalInfoPanel {
    /** 面板内容 */
    @Getter
    private JPanel content;

    /**
     * 个人信息配置类
     */
    public static class PersonalInfo {
        /** 姓名 */
        private final String name;
        /** 职位/角色 */
        private final String role;
        /** 个人简介 */
        private final String bio;
        /** 头像图标 */
        private final ImageIcon avatar;
        /** 悬停时的头像图标 */
        private final ImageIcon hoverAvatar;
        /** GitHub 链接 */
        private final String githubUrl;
        /** 个人网站链接 */
        private final String websiteUrl;
        /** 邮箱 */
        private final String email;
        /** Twitter/X 链接 */
        private final String twitterUrl;
        /** 博客链接 */
        private final String blogUrl;
        /** NPX Card 链接 */
        private final String npxCardUrl;
        /** Chat 链接 */
        private final String chatUrl;
        /** 命令行命令 */
        private final String command;
        /** 底部提示的 GitHub 链接 */
        private final String footerGitHubUrl;

        private PersonalInfo(Builder builder) {
            this.name = builder.name;
            this.role = builder.role;
            this.bio = builder.bio;
            this.avatar = builder.avatar;
            this.hoverAvatar = builder.hoverAvatar;
            this.githubUrl = builder.githubUrl;
            this.websiteUrl = builder.websiteUrl;
            this.email = builder.email;
            this.twitterUrl = builder.twitterUrl;
            this.blogUrl = builder.blogUrl;
            this.npxCardUrl = builder.npxCardUrl;
            this.chatUrl = builder.chatUrl;
            this.command = builder.command;
            this.footerGitHubUrl = builder.footerGitHubUrl;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;
            private String role;
            private String bio;
            private ImageIcon avatar;
            private ImageIcon hoverAvatar;
            private String githubUrl;
            private String websiteUrl;
            private String email;
            private String twitterUrl;
            private String blogUrl;
            private String npxCardUrl;
            private String chatUrl;
            private String command;
            private String footerGitHubUrl;

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder role(String role) {
                this.role = role;
                return this;
            }

            public Builder bio(String bio) {
                this.bio = bio;
                return this;
            }

            public Builder avatar(ImageIcon avatar) {
                this.avatar = avatar;
                return this;
            }

            public Builder hoverAvatar(ImageIcon hoverAvatar) {
                this.hoverAvatar = hoverAvatar;
                return this;
            }

            public Builder githubUrl(String githubUrl) {
                this.githubUrl = githubUrl;
                return this;
            }

            public Builder websiteUrl(String websiteUrl) {
                this.websiteUrl = websiteUrl;
                return this;
            }

            public Builder email(String email) {
                this.email = email;
                return this;
            }

            public Builder twitterUrl(String twitterUrl) {
                this.twitterUrl = twitterUrl;
                return this;
            }

            public Builder blogUrl(String blogUrl) {
                this.blogUrl = blogUrl;
                return this;
            }

            public Builder npxCardUrl(String npxCardUrl) {
                this.npxCardUrl = npxCardUrl;
                return this;
            }

            public Builder chatUrl(String chatUrl) {
                this.chatUrl = chatUrl;
                return this;
            }

            public Builder command(String command) {
                this.command = command;
                return this;
            }

            public Builder footerGitHubUrl(String footerGitHubUrl) {
                this.footerGitHubUrl = footerGitHubUrl;
                return this;
            }

            public PersonalInfo build() {
                return new PersonalInfo(this);
            }
        }
    }

    /**
     * 构造函数
     *
     * @param info 个人信息配置
     */
    public PersonalInfoPanel(@NotNull PersonalInfo info) {
        this(info, true);
    }

    /**
     * 构造函数
     *
     * @param info        个人信息配置
     * @param collapsible 是否使用折叠面板
     */
    public PersonalInfoPanel(@NotNull PersonalInfo info, boolean collapsible) {
        createPersonalInfoPanel(info, collapsible);
    }

    /**
     * 创建个人信息面板
     *
     * @param info 个人信息配置
     */
    private void createPersonalInfoPanel(@NotNull PersonalInfo info, boolean collapsible) {
        content = collapsible ? createCollapsiblePanel(info) : createContentOnlyPanel(info);
    }

    /**
     * 创建折叠式个人信息面板
     *
     * @param info 个人信息配置
     * @return 折叠式面板
     */
    @NotNull
    private JPanel createCollapsiblePanel(@NotNull PersonalInfo info) {
        JPanel container = new JPanel(new BorderLayout());

        // 创建标题文本
        final String titleText = "👨‍💻 About Me";

        // 创建标题面板（不带边框，因为边框在容器上）
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(JBUI.Borders.empty(5));
        titlePanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        titlePanel.setOpaque(false);

        // 主内容面板（居中布局）
        JPanel mainPanel = buildContentPanel(info);
        // 默认折叠：隐藏内容面板
        mainPanel.setVisible(false);

        // 使用包装面板确保内容居中
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(mainPanel, BorderLayout.NORTH);
        contentWrapper.setOpaque(false);

        // 将标题栏和内容面板添加到主面板
        container.add(titlePanel, BorderLayout.NORTH);
        container.add(contentWrapper, BorderLayout.CENTER);

        // 为容器设置 TitledBorder（边框会包围整个区域）
        TitledBorder containerBorder = BorderFactory.createTitledBorder("▶ " + titleText);
        configureTitledBorder(containerBorder);
        container.setBorder(BorderFactory.createCompoundBorder(
            containerBorder,
            JBUI.Borders.empty(5)
                                                            ));
        container.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        container.setOpaque(true);
        container.setBackground(UIUtil.getPanelBackground());

        // 存储内容面板的引用，以便在点击时切换显示
        final JPanel contentPanel = mainPanel;

        // 为容器添加点击事件（整个容器都可以点击）
        container.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = contentPanel.isVisible();
                contentPanel.setVisible(!isVisible);

                // 更新容器的 TitledBorder（因为边框在容器上）
                String arrow = !isVisible ? "▼ " : "▶ ";
                TitledBorder containerBorder = BorderFactory.createTitledBorder(arrow + titleText);
                configureTitledBorder(containerBorder);
                container.setBorder(BorderFactory.createCompoundBorder(
                    containerBorder,
                    JBUI.Borders.empty(5)
                                                                    ));

                container.revalidate();
                container.repaint();
            }
        });

        return container;
    }

    /**
     * 创建仅内容的个人信息面板
     *
     * @param info 个人信息配置
     * @return 内容面板
     */
    @NotNull
    private JPanel createContentOnlyPanel(@NotNull PersonalInfo info) {
        JPanel mainPanel = buildContentPanel(info);
        mainPanel.setVisible(true);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(mainPanel, BorderLayout.NORTH);
        contentWrapper.setOpaque(false);
        return contentWrapper;
    }

    /**
     * 创建个人信息内容面板
     *
     * @param info 个人信息配置
     * @return 内容面板
     */
    @NotNull
    private JPanel buildContentPanel(@NotNull PersonalInfo info) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        mainPanel.setBorder(JBUI.Borders.empty(15));

        // 头像（圆形，居中）
        if (info.avatar != null) {
            JComponent avatarComponent = createCircularAvatarLabel(info.avatar, info.hoverAvatar);

            // 使用容器确保头像完全居中，并限制容器尺寸与头像一致
            JPanel avatarContainer = new JPanel(new BorderLayout());
            avatarContainer.setOpaque(false);
            int iconWidth = info.avatar.getIconWidth();
            int iconHeight = info.avatar.getIconHeight();
            avatarContainer.setPreferredSize(new Dimension(iconWidth, iconHeight));
            avatarContainer.setMaximumSize(new Dimension(iconWidth, iconHeight));
            avatarContainer.setMinimumSize(new Dimension(iconWidth, iconHeight));
            avatarContainer.add(avatarComponent, BorderLayout.CENTER);
            avatarContainer.setAlignmentX(JPanel.CENTER_ALIGNMENT);
            mainPanel.add(avatarContainer);
            mainPanel.add(Box.createVerticalStrut(15));
        }

        // 姓名（大字体，居中）
        if (info.name != null) {
            JBLabel nameLabel = new JBLabel(info.name, SwingConstants.CENTER);
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 24f));
            nameLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            mainPanel.add(nameLabel);
            mainPanel.add(Box.createVerticalStrut(8));
        }

        // 角色（灰色，居中）
        if (info.role != null) {
            JBLabel roleLabel = new JBLabel(info.role, SwingConstants.CENTER);
            roleLabel.setFont(roleLabel.getFont().deriveFont(Font.PLAIN, 14f));
            roleLabel.setForeground(UIUtil.getLabelDisabledForeground());
            roleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            mainPanel.add(roleLabel);
            mainPanel.add(Box.createVerticalStrut(15));
        }

        // 简介（居中，多行）
        if (info.bio != null) {
            JBLabel bioLabel = new JBLabel("<html><div style='text-align: center; max-width: 500px;'>" +
                                           info.bio + "</div></html>", SwingConstants.CENTER);
            bioLabel.setFont(bioLabel.getFont().deriveFont(Font.PLAIN, 13f));
            bioLabel.setForeground(UIUtil.getLabelForeground());
            bioLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            mainPanel.add(bioLabel);
            mainPanel.add(Box.createVerticalStrut(15));
        }

        // 命令行代码块
        if (info.command != null) {
            JPanel codeBlockPanel = createCodeBlockPanel(info.command);
            codeBlockPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
            mainPanel.add(codeBlockPanel);
            mainPanel.add(Box.createVerticalStrut(20));
        }

        // 社交媒体链接（居中，图标形式）
        JPanel linksPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        linksPanel.setOpaque(false);
        linksPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);

        if (info.websiteUrl != null) {
            linksPanel.add(createIconLink(loadSvgIcon("/icons/personal/home.svg"), "Home", info.websiteUrl));
        }
        if (info.githubUrl != null) {
            linksPanel.add(createIconLink(loadSvgIcon("/icons/personal/github.svg"), "GitHub", info.githubUrl));
        }
        if (info.blogUrl != null) {
            linksPanel.add(createIconLink(loadSvgIcon("/icons/personal/blog.svg"), "Blog", info.blogUrl));
        }
        if (info.npxCardUrl != null) {
            linksPanel.add(createIconLink(loadSvgIcon("/icons/personal/card.svg"), "Card", info.npxCardUrl));
        }
        if (info.chatUrl != null) {
            linksPanel.add(createIconLink(loadSvgIcon("/icons/personal/chat.svg"), "Chat", info.chatUrl));
        }
        if (info.email != null) {
            linksPanel.add(createIconLink(loadSvgIcon("/icons/personal/email.svg"), "Email", "mailto:" + info.email));
        }
        if (info.twitterUrl != null) {
            // Twitter 图标暂时使用 GitHub 图标作为占位符，或者可以添加 twitter.svg
            linksPanel.add(createIconLink(loadSvgIcon("/icons/personal/github.svg"), "Twitter", info.twitterUrl));
        }

        mainPanel.add(linksPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 底部提示
        if (info.footerGitHubUrl != null) {
            JBLabel footerLabel = new JBLabel(
                "<html><div style='text-align: center; color: #999; font-size: 11px;'>" +
                "If you find this plugin helpful, please give it a ⭐ on " +
                "<a href='" + info.footerGitHubUrl + "'>GitHub</a>" +
                "</div></html>",
                SwingConstants.CENTER
            );
            footerLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            footerLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            // 添加鼠标监听器来处理链接点击
            footerLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    BrowserUtil.browse(info.footerGitHubUrl);
                }
            });

            mainPanel.add(footerLabel);
            mainPanel.add(Box.createVerticalStrut(12));
        }

        // Buy Me a Coffee 链接
        final String buyMeACoffeeUrl = "https://ideaplugin.dong4j.site/buy-me-a-coffee";
        JBLabel buyMeACoffeeLabel = new JBLabel(
            "<html><div style='text-align: center; color: #999; font-size: 11px;'>" +
            "<a href='" + buyMeACoffeeUrl + "'>Buy Me a Coffee</a>" +
            "</div></html>",
            SwingConstants.CENTER
        );
        buyMeACoffeeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        buyMeACoffeeLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        // 添加鼠标监听器来处理链接点击
        buyMeACoffeeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse(buyMeACoffeeUrl);
            }
        });

        mainPanel.add(buyMeACoffeeLabel);

        return mainPanel;
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p>
     * 显式设置字体和颜色，确保在 2025 版本中正常显示。
     * 使用 UIUtil 获取主题感知的文本颜色，自动适配浅色和深色主题。
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 创建可折叠的标题栏
     *
     * @param title 标题文本
     * @return 标题栏面板
     */
    private JPanel createCollapsibleTitle(@NotNull String title) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        // 默认折叠状态，使用右箭头
        TitledBorder titledBorder = BorderFactory.createTitledBorder("▶ " + title);
        configureTitledBorder(titledBorder);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            JBUI.Borders.empty(5)
                                                               ));
        titlePanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        titlePanel.setOpaque(true);
        titlePanel.setBackground(UIUtil.getPanelBackground());
        return titlePanel;
    }

    /**
     * 更新可折叠标题栏的箭头图标
     *
     * @param titlePanel 标题栏面板
     * @param title      标题文本
     * @param expanded   是否展开
     */
    private void updateCollapsibleTitle(@NotNull JPanel titlePanel, @NotNull String title, boolean expanded) {
        String arrow = expanded ? "▼ " : "▶ ";
        TitledBorder titledBorder = BorderFactory.createTitledBorder(arrow + title);
        configureTitledBorder(titledBorder);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            JBUI.Borders.empty(5)
                                                               ));
    }

    /**
     * 创建头像标签（直接使用原始图片，不进行缩放）
     * 支持鼠标悬停时切换图片，带淡入淡出过渡效果
     *
     * @param icon      原始图标（已经是 120x120 圆形）
     * @param hoverIcon 悬停时的图标（可选）
     * @return 头像组件
     */
    private JComponent createCircularAvatarLabel(@NotNull ImageIcon icon, ImageIcon hoverIcon) {
        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();

        // 如果提供了悬停图标，使用带过渡效果的组件
        if (hoverIcon != null) {
            return new FadingAvatarComponent(icon, hoverIcon, iconWidth, iconHeight);
        } else {
            // 如果没有悬停图标，使用普通 JLabel
            JLabel avatarLabel = new JLabel(icon);
            avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
            avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
            avatarLabel.setPreferredSize(new Dimension(iconWidth, iconHeight));
            avatarLabel.setMaximumSize(new Dimension(iconWidth, iconHeight));
            avatarLabel.setMinimumSize(new Dimension(iconWidth, iconHeight));
            return avatarLabel;
        }
    }

    /**
     * 带淡入淡出过渡效果的头像组件
     */
    private static class FadingAvatarComponent extends JComponent {
        private static final int ANIMATION_DURATION_MS = 200; // 动画持续时间（毫秒）
        private static final int TIMER_DELAY_MS = 16; // 每帧延迟（约 60fps）

        private final ImageIcon normalIcon;
        private final ImageIcon hoverIcon;
        private float hoverAlpha = 0.0f; // 悬停图标的透明度，0.0 = 完全透明，1.0 = 完全不透明
        private boolean isHovering = false;
        private Timer animationTimer;

        public FadingAvatarComponent(@NotNull ImageIcon normalIcon, @NotNull ImageIcon hoverIcon,
                                     int width, int height) {
            this.normalIcon = normalIcon;
            this.hoverIcon = hoverIcon;

            setPreferredSize(new Dimension(width, height));
            setMaximumSize(new Dimension(width, height));
            setMinimumSize(new Dimension(width, height));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

            // 创建动画定时器
            animationTimer = new Timer(TIMER_DELAY_MS, e -> {
                float targetAlpha = isHovering ? 1.0f : 0.0f;
                float diff = targetAlpha - hoverAlpha;
                float step = (float) TIMER_DELAY_MS / ANIMATION_DURATION_MS;

                if (Math.abs(diff) < step) {
                    hoverAlpha = targetAlpha;
                    animationTimer.stop();
                } else {
                    hoverAlpha += diff > 0 ? step : -step;
                }

                repaint();
            });

            // 添加鼠标监听器
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovering = true;
                    if (!animationTimer.isRunning()) {
                        animationTimer.start();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovering = false;
                    if (!animationTimer.isRunning()) {
                        animationTimer.start();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int x = (getWidth() - normalIcon.getIconWidth()) / 2;
                int y = (getHeight() - normalIcon.getIconHeight()) / 2;

                // 绘制原始图标（作为背景）
                if (hoverAlpha < 1.0f) {
                    float normalAlpha = 1.0f - hoverAlpha;
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, normalAlpha));
                    normalIcon.paintIcon(this, g2, x, y);
                }

                // 绘制悬停图标（叠加在原始图标上）
                if (hoverAlpha > 0.0f) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hoverAlpha));
                    hoverIcon.paintIcon(this, g2, x, y);
                }
            } finally {
                g2.dispose();
            }
        }

        /**
         * 清理资源
         */
        public void dispose() {
            if (animationTimer != null) {
                animationTimer.stop();
                animationTimer = null;
            }
        }
    }

    /**
     * 创建命令行代码块面板
     *
     * @param command 命令文本
     * @return 代码块面板
     */
    private JPanel createCodeBlockPanel(@NotNull String command) {
        JPanel codePanel = new JPanel(new BorderLayout(10, 0));
        codePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1),
            JBUI.Borders.empty(10, 15)
                                                              ));
        codePanel.setBackground(UIUtil.getPanelBackground());
        codePanel.setMaximumSize(new Dimension(300, 50));

        // 命令文本
        JBLabel codeLabel = new JBLabel(command);
        codeLabel.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeLabel.setForeground(UIUtil.getLabelForeground());
        codePanel.add(codeLabel, BorderLayout.CENTER);

        // 复制按钮
        JButton copyButton = new JButton("Copy");
        copyButton.setFont(copyButton.getFont().deriveFont(Font.PLAIN, 11f));
        copyButton.setPreferredSize(new Dimension(60, 25));
        copyButton.addActionListener(e -> {
            java.awt.datatransfer.StringSelection selection =
                new java.awt.datatransfer.StringSelection(command);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(selection, null);
            copyButton.setText("Copied!");
            javax.swing.Timer timer = new javax.swing.Timer(2000, evt -> copyButton.setText("Copy"));
            timer.setRepeats(false);
            timer.start();
        });
        codePanel.add(copyButton, BorderLayout.EAST);

        return codePanel;
    }

    /**
     * 加载 SVG 图标
     *
     * @param resourcePath 资源路径，例如 "/icons/personal/github.svg"
     * @return Icon 对象，如果加载失败则返回 null
     */
    @Nullable
    private Icon loadSvgIcon(@NotNull String resourcePath) {
        try {
            return IconLoader.getIcon(resourcePath, PersonalInfoPanel.class);
        } catch (Exception e) {
            // 如果加载失败，返回 null，createIconLink 会处理
            return null;
        }
    }

    /**
     * 创建图标链接
     *
     * @param icon    图标（Icon 对象）
     * @param tooltip 提示文本
     * @param url     链接地址
     * @return 链接标签
     */
    private JBLabel createIconLink(@Nullable Icon icon, @NotNull String tooltip, @NotNull String url) {
        JBLabel label;
        final Icon originalIcon = icon;

        if (icon != null) {
            // 使用 SVG 图标
            label = new JBLabel(icon);
        } else {
            // 如果图标加载失败，使用文本占位符
            label = new JBLabel("🔗");
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 20f));
        }

        label.setForeground(UIUtil.getLabelForeground());
        label.setToolTipText(tooltip);
        label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse(url);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (originalIcon != null) {
                    // 对于 SVG 图标，不改变图标本身，只改变前景色（可能影响某些图标）
                    // SVG 图标会根据主题自动调整颜色，这里我们通过改变前景色来提供视觉反馈
                    label.setForeground(new JBColor(new java.awt.Color(102, 126, 234), new java.awt.Color(102, 126, 234)));
                } else {
                    // 对于文本占位符，改变文本颜色
                    label.setForeground(new JBColor(new java.awt.Color(102, 126, 234), new java.awt.Color(102, 126, 234)));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 恢复文本颜色
                label.setForeground(UIUtil.getLabelForeground());
            }
        });

        return label;
    }

    /**
     * 加载图像图标
     * <p> 根据给定的资源路径加载图像, 并返回一个 ImageIcon 对象. 如果加载失败, 则返回 null.
     *
     * @param resourcePath 资源路径, 例如 "/icons/personal/avatar.png"
     * @return 加载成功的 ImageIcon 对象, 如果加载失败则返回 null
     */
    @Nullable
    private static ImageIcon loadImageIcon(@NotNull String resourcePath) {
        try {
            java.net.URL imageUrl = PersonalInfoPanel.class.getResource(resourcePath);
            if (imageUrl != null) {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(imageUrl);
                if (image != null) {
                    return new ImageIcon(image);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 返回默认的个人信息配置对象
     * <p> 此方法提供了一个预定义的个人信息配置, 包含了姓名, 职位, 简介, 头像, 社交媒体链接等信息.
     *
     * @return 默认的个人信息配置对象
     */
    @NotNull
    public static PersonalInfoPanel.PersonalInfo defaultInfo() {
        ImageIcon avatar = loadImageIcon("/icons/personal/avatar.png");
        ImageIcon hoverAvatar = loadImageIcon("/icons/personal/avatar2.png");

        return PersonalInfoPanel.PersonalInfo.builder()
            .name("dong4j")
            .role("✨ Gifted Web Developer | 🛠️ Tool Creator")
            .bio("Passionate about creating useful developer tools and plugins that make developers' lives easier.<br><br>" +
                 "💡 Try running this in your terminal to connect with me:")
            .avatar(avatar)
            .hoverAvatar(hoverAvatar)
            .command("npx dong4j --no")
            .githubUrl("https://github.com/dong4j")
            .blogUrl("https://blog.dong4j.site")
            .websiteUrl("https://home.dong4j.site")
            .npxCardUrl("https://npx-card.dong4j.site")
            .chatUrl("https://chat.dong4j.site")
            .email("dong4j@gmail.com")
            .footerGitHubUrl("https://github.com/zeka-stack/zeka-idea-plugin")
            .build();
    }

    /**
     * 创建一个默认的个人信息面板实例
     * <p> 使用默认的个人信息配置创建并返回一个个人信息面板实例
     *
     * @return 默认的个人信息面板实例
     */
    public static PersonalInfoPanel create() {
        return new PersonalInfoPanel(defaultInfo());
    }

    /**
     * 创建一个仅内容的个人信息面板实例
     * <p> 用于对话框等场景，不包含折叠标题 </p>
     *
     * @return 内容面板实例
     */
    public static PersonalInfoPanel createContentOnly() {
        return new PersonalInfoPanel(defaultInfo(), false);
    }
}
