package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;

import dev.dong4j.zeka.stack.idea.plugin.common.codefree.CodefreeAgentManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.CodefreeAgentSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.BreathingDotIcon;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.SpacedJBLabel;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * Codefree 代理配置面板
 * <p>
 * 独立的 Codefree 代理配置面板，提供高内聚低耦合的设计。
 * 包含下载地址、自动启动选项、状态显示和操作按钮等功能。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CodefreePanel {
    /** 日志记录器 */
    private static final Logger LOG = Logger.getInstance(CodefreePanel.class);
    /** 红色指示灯的颜色, 用于显示错误或警告状态 */
    private static final JBColor DOT_RED = new JBColor(new Color(239, 68, 68), new Color(255, 82, 82));
    /** Codefree 代理配置面板使用的绿色 */
    private static final JBColor CODEFREE_GREEN = new JBColor(new Color(76, 175, 80), new Color(76, 175, 80));
    private static final JBColor CODEFREE_RED = new JBColor(new Color(244, 67, 54), new Color(244, 67, 54));
    private static final JBColor CODEFREE_YELLOW = new JBColor(new Color(255, 193, 7), new Color(255, 193, 7));
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.00");
    /** 主面板, 承载整个代理配置面板的 UI 组件 */
    @NotNull
    private final JPanel content;
    /** 控制是否自动启动 Codefree 代理服务的复选框 */
    @NotNull
    private final JBCheckBox autoStartCheckBox;
    /** 下载地址输入框 */
    @NotNull
    private final JBTextField downloadUrlField;
    /** 状态标签 */
    @NotNull
    private final JBLabel statusLabel;
    /** 最新版本标签 */
    @NotNull
    private final JBLabel latestVersionLabel;
    /** 本地 jar 标签 */
    @NotNull
    private final JBLabel localJarLabel;
    /**
     * 下载进度条
     * <p> 用于显示 Codefree jar 文件的下载进度
     */
    @NotNull
    private final JProgressBar downloadProgressBar;
    /** 下载按钮 */
    @NotNull
    private final JButton downloadButton;
    /** 启动 / 停止按钮 */
    @NotNull
    private final JButton startButton;
    /** 启动状态指示灯 */
    @NotNull
    private final BreathingDotIcon startStatusIcon;
    /** 下载状态指示灯 */
    @NotNull
    private final BreathingDotIcon downloadStatusIcon;
    /** 内容面板（可折叠） */
    @NotNull
    private final JPanel mainPanel;
    /** 状态更新定时器 */
    @Nullable
    private Timer statusUpdateTimer;
    /** 状态更新回调 */
    @Nullable
    private Runnable statusUpdateCallback;
    /** 远端 jar 名称 */
    @Nullable
    private String latestJarName;
    /** 当前本地 jar 名称 */
    @Nullable
    private String currentJarName;
    /** Codefree 代理管理器 */
    @NotNull
    private final CodefreeAgentManager codefreeAgentManager = CodefreeAgentManager.getInstance();
    /** 用于显示对话框的父面板 */
    @Nullable
    private JPanel parentPanel;
    /** 上次进度更新时间（用于节流） */
    private long lastProgressUpdateTime = 0;
    /** 上次进度百分比（用于节流） */
    private int lastProgressPercent = -1;
    /** 进度更新最小间隔（毫秒） */
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 100;
    /**
     * 进度更新最小百分比间隔
     * <p> 用于控制进度更新的频率, 避免过于频繁的 UI 更新 </p>
     */
    private static final int PROGRESS_UPDATE_PERCENT_INTERVAL = 1;
    /** 是否正在下载 */
    private volatile boolean isDownloading = false;

    /**
     * 构造函数
     */
    public CodefreePanel() {
        // 初始化组件
        autoStartCheckBox = new JBCheckBox(AICommonBundle.message("settings.codefree.auto.start"));
        downloadUrlField = new JBTextField();
        downloadUrlField.setToolTipText(AICommonBundle.message("settings.codefree.download.url.hint"));
        // 限制输入框宽度，防止超长 URL 拉长界面
        Dimension urlFieldSize = new Dimension(500, downloadUrlField.getPreferredSize().height);
        downloadUrlField.setPreferredSize(urlFieldSize);
        downloadUrlField.setMaximumSize(new Dimension(600, downloadUrlField.getPreferredSize().height));

        statusLabel = new SpacedJBLabel(AICommonBundle.message("settings.codefree.status.not.ready"));
        statusLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        latestVersionLabel = new SpacedJBLabel(AICommonBundle.message("settings.codefree.version.checking"));
        localJarLabel = new SpacedJBLabel(AICommonBundle.message("settings.codefree.version.local.empty"));
        downloadProgressBar = new JProgressBar(0, 100);
        downloadProgressBar.setStringPainted(false);
        downloadProgressBar.setVisible(false);
        downloadProgressBar.setPreferredSize(new Dimension(420, JBUI.scale(3)));

        downloadButton = new JButton(AICommonBundle.message("settings.codefree.download"));
        downloadButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        downloadButton.setIconTextGap(JBUI.scale(6));
        downloadStatusIcon = new BreathingDotIcon(downloadButton, DOT_RED);
        downloadButton.setIcon(downloadStatusIcon);
        downloadButton.setDisabledIcon(downloadStatusIcon);

        startButton = new JButton(AICommonBundle.message("settings.codefree.start"));
        startButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        startButton.setIconTextGap(JBUI.scale(6));
        startStatusIcon = new BreathingDotIcon(startButton, DOT_RED);
        startButton.setIcon(startStatusIcon);
        startButton.setDisabledIcon(startStatusIcon);

        downloadUrlField.getDocument().addDocumentListener(new DocumentAdapter() {
            /**
             * 当文档内容发生变化时调用
             * <p> 重写父类方法, 在文档内容改变时更新下载按钮的状态
             *
             * @param e 文档变化事件对象, 包含文档变化的详细信息
             */
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                updateDownloadButtonState();
            }
        });

        // 创建按钮面板
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonsPanel.add(downloadButton);
        buttonsPanel.add(startButton);

        JPanel progressPanel = new JPanel(new BorderLayout(0, 0));
        progressPanel.add(downloadProgressBar, BorderLayout.CENTER);

        // 创建主内容面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(autoStartCheckBox, "settings.codefree.auto.start.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.codefree.download.url")), downloadUrlField)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.codefree.version.latest")), latestVersionLabel)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.codefree.version.local")), localJarLabel)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.codefree.status")), statusLabel)
            .addComponent(progressPanel)
            .addComponent(buttonsPanel)
            .getPanel();

        // 创建可折叠容器
        content = createCollapsiblePanel();

        updateDownloadButtonState();

        // 启动状态更新定时器（每 1 秒检查一次服务状态）
        startStatusUpdateTimer();
    }

    /**
     * 启动状态更新定时器
     * <p> 创建并启动一个定时器, 每隔 3 秒执行状态更新回调函数, 用于定期检查 Codefree 代理服务状态并更新 UI.
     * 如果定时器已存在, 会先停止之前的定时器再重新创建.
     */
    private void startStatusUpdateTimer() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
        }
        statusUpdateTimer = new Timer(3000, e -> {
            if (statusUpdateCallback != null) {
                statusUpdateCallback.run();
            }
        });
        statusUpdateTimer.setRepeats(true);
        statusUpdateTimer.start();
    }

    /**
     * 设置状态更新回调
     *
     * @param callback 状态更新回调
     */
    public void setStatusUpdateCallback(@Nullable Runnable callback) {
        this.statusUpdateCallback = callback;
    }

    /**
     * 设置父面板，用于显示对话框
     *
     * @param parentPanel 父面板
     */
    public void setParentPanel(@Nullable JPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    /**
     * 创建可折叠面板
     *
     * @return 可折叠面板
     */
    @NotNull
    private JPanel createCollapsiblePanel() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        // 创建标题文本
        String titleText = AICommonBundle.message("settings.codefree.title");

        // 创建标题面板（不带边框，因为边框在容器上）
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(JBUI.Borders.empty(5));
        titlePanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        titlePanel.setOpaque(false);

        // 默认折叠：隐藏内容面板
        mainPanel.setVisible(false);

        // 使用包装面板确保内容居中
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(mainPanel, BorderLayout.NORTH);
        contentWrapper.setOpaque(false);

        // 将标题栏和内容面板添加到主面板
        content.add(titlePanel, BorderLayout.NORTH);
        content.add(contentWrapper, BorderLayout.CENTER);

        // 为容器设置 TitledBorder（边框会包围整个区域）
        TitledBorder containerBorder = BorderFactory.createTitledBorder("▶ " + titleText);
        configureTitledBorder(containerBorder);
        content.setBorder(BorderFactory.createCompoundBorder(
            containerBorder,
            JBUI.Borders.empty(5)
                                                            ));
        content.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        content.setOpaque(true);
        content.setBackground(UIUtil.getPanelBackground());

        // 为容器添加点击事件（整个容器都可以点击）
        content.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean isVisible = mainPanel.isVisible();
                mainPanel.setVisible(!isVisible);

                // 更新容器的 TitledBorder（因为边框在容器上）
                String arrow = !isVisible ? "▼ " : "▶ ";
                TitledBorder containerBorder = BorderFactory.createTitledBorder(arrow + titleText);
                configureTitledBorder(containerBorder);
                content.setBorder(BorderFactory.createCompoundBorder(
                    containerBorder,
                    JBUI.Borders.empty(5)
                                                                    ));

                content.revalidate();
                content.repaint();
            }
        });

        return content;
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 创建一个包含复选框和提示标签的面板
     *
     * @param checkBox 要添加到面板中的复选框
     * @param hintKey  用于获取提示信息的键
     * @return 包含复选框和提示标签的面板
     */
    @NotNull
    private JPanel createCheckBoxWithHint(@NotNull JBCheckBox checkBox, @NotNull String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(checkBox, BorderLayout.WEST);

        JBLabel hintLabel = new SpacedJBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        // 根据复选框状态更新提示标签颜色
        updateHintLabelColor(hintLabel, checkBox.isSelected());
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        return panel;
    }

    /**
     * 更新提示标签的字体颜色
     *
     * @param hintLabel 提示标签对象
     * @param selected  是否选中状态
     */
    private void updateHintLabelColor(@NotNull JBLabel hintLabel, boolean selected) {
        if (selected) {
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    /**
     * 根据 URL 更新下载按钮状态
     */
    private void updateDownloadButtonState() {
        String url = downloadUrlField.getText().trim().toLowerCase();
        boolean remote = url.startsWith("http://") || url.startsWith("https://");
        downloadButton.setEnabled(remote);
        if (!remote) {
            setDownloadStatusColor(CODEFREE_GREEN);
        }
    }

    public void setLatestJarName(@Nullable String jarName) {
        this.latestJarName = jarName;
        if (jarName == null || jarName.isBlank()) {
            latestVersionLabel.setText(AICommonBundle.message("settings.codefree.version.unknown"));
        } else {
            latestVersionLabel.setText(jarName);
        }
    }

    public void setLocalJarName(@Nullable String jarName, long size) {
        this.currentJarName = jarName;
        if (jarName == null || jarName.isBlank()) {
            localJarLabel.setText(AICommonBundle.message("settings.codefree.version.local.empty"));
        } else {
            String sizeText = size > 0 ? formatSize(size) : AICommonBundle.message("settings.codefree.download.size.unknown.short");
            localJarLabel.setText(AICommonBundle.message("settings.codefree.version.local.value", jarName, sizeText));
        }
    }

    public void setDownloadSize(long totalBytes) {
        // 仅用于保留调用点，不在 UI 上显示大小
    }

    /**
     * 更新下载进度
     * <p>
     * 根据已下载字节数和总字节数更新下载进度条和进度文本.
     *
     * @param downloaded 已下载的字节数
     * @param totalBytes 总字节数
     */
    public void updateDownloadProgress(long downloaded, long totalBytes) {
        if (totalBytes > 0) {
            downloadProgressBar.setVisible(true);
            int percent = (int) Math.min(100, Math.round(downloaded * 100.0 / totalBytes));
            downloadProgressBar.setIndeterminate(false);
            downloadProgressBar.setValue(percent);
            downloadProgressBar.setStringPainted(false);
        } else {
            downloadProgressBar.setVisible(true);
            downloadProgressBar.setIndeterminate(true);
            downloadProgressBar.setStringPainted(false);
        }
        downloadProgressBar.repaint();
    }

    public void resetDownloadProgress() {
        downloadProgressBar.setVisible(false);
        downloadProgressBar.setIndeterminate(false);
        downloadProgressBar.setValue(0);
        downloadProgressBar.setStringPainted(false);
        downloadProgressBar.setString("");
        downloadProgressBar.repaint();
    }

    public void setStartStatusColor(@NotNull Color color) {
        startStatusIcon.setColor(color);
    }

    public void setDownloadStatusColor(@NotNull Color color) {
        downloadStatusIcon.setColor(color);
    }

    public void setDownloadButtonText(@NotNull String text) {
        downloadButton.setText(text);
    }

    public void setStatusText(@NotNull String text) {
        statusLabel.setText(text);
    }

    /**
     * 设置状态文本，支持可点击的链接
     * <p> 如果文本包含端点地址，将其格式化为可点击的链接样式
     *
     * @param text     状态文本
     * @param endpoint 端点地址（可选），如果提供则会被格式化为可点击链接
     */
    public void setStatusTextWithLink(@NotNull String text, @Nullable String endpoint) {
        if (endpoint != null && !endpoint.isEmpty()) {
            // 从文本中提取端点地址前后的部分
            String prefix = text.substring(0, text.indexOf(endpoint));
            String suffix = text.substring(text.indexOf(endpoint) + endpoint.length());

            // 使用 HTML 格式化链接样式，使用主题感知的颜色
            Color linkColor = new JBColor(new Color(74, 144, 226), new Color(100, 149, 237));
            String linkText = String.format(
                "<html>%s<a href='%s' style='color: rgb(%d,%d,%d); text-decoration: underline;'>%s</a>%s</html>",
                prefix,
                endpoint,
                linkColor.getRed(),
                linkColor.getGreen(),
                linkColor.getBlue(),
                endpoint,
                suffix
                                           );
            statusLabel.setText(linkText);

            // 移除旧的鼠标监听器
            for (java.awt.event.MouseListener listener : statusLabel.getMouseListeners()) {
                if (listener instanceof MouseAdapter) {
                    statusLabel.removeMouseListener(listener);
                }
            }

            // 添加点击事件来复制到剪贴板
            statusLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    copyToClipboard(endpoint);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    statusLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    statusLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            });
        } else {
            statusLabel.setText(text);
        }
    }

    /**
     * 复制文本到剪贴板（跨平台支持）
     *
     * @param text 要复制的文本
     */
    private void copyToClipboard(@NotNull String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            // 可以显示一个提示，但这里暂时不显示，避免干扰
        } catch (Exception e) {
            LOG.warn("复制到剪贴板失败: " + text, e);
        }
    }

    @NotNull
    public String getLatestJarName() {
        return latestJarName != null ? latestJarName : "";
    }

    @NotNull
    public String getCurrentJarName() {
        return currentJarName != null ? currentJarName : "";
    }

    @NotNull
    private String formatSize(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return AICommonBundle.message("settings.codefree.download.size.unknown.short");
        }
        double mb = sizeInBytes / 1024.0 / 1024.0;
        return SIZE_FORMAT.format(mb) + " MB";
    }

    // ==================== Getter 方法 ====================

    /**
     * 获取主面板
     *
     * @return 主面板
     */
    @NotNull
    public JPanel getContent() {
        return content;
    }

    /**
     * 获取自动启动复选框
     *
     * @return 自动启动复选框
     */
    @NotNull
    public JBCheckBox getAutoStartCheckBox() {
        return autoStartCheckBox;
    }

    /**
     * 获取下载地址输入框
     *
     * @return 下载地址输入框
     */
    @NotNull
    public JBTextField getDownloadUrlField() {
        return downloadUrlField;
    }

    /**
     * 获取状态标签
     *
     * @return 状态标签
     */
    @NotNull
    public JBLabel getStatusLabel() {
        return statusLabel;
    }

    /**
     * 获取下载按钮
     *
     * @return 下载按钮
     */
    @NotNull
    public JButton getDownloadButton() {
        return downloadButton;
    }

    /**
     * 获取启动/停止按钮
     *
     * @return 启动/停止按钮
     */
    @NotNull
    public JButton getStartButton() {
        return startButton;
    }

    // ==================== Codefree 业务逻辑方法 ====================

    /**
     * 更新 Codefree 代理状态
     * <p>
     * 检查 Codefree 代理服务是否正在运行，并更新按钮状态和状态标签。
     *
     * @param settings Codefree 代理设置
     */
    public void updateCodefreeStatus(@NotNull CodefreeAgentSettings settings) {
        CodefreeAgentManager.JarInfo jarInfo = codefreeAgentManager.resolveLocalJarInfo(settings);
        boolean jarReady = jarInfo != null && Files.exists(jarInfo.path());
        boolean running = codefreeAgentManager.isRunning();

        String status;
        String buttonText;
        // 如果正在下载，强制禁用启动按钮
        boolean buttonEnabled = !isDownloading && (running || jarReady);
        JBColor startColor;

        String endpoint = null;
        if (running) {
            // 服务正在运行
            endpoint = codefreeAgentManager.getLocalOpenAiEndpoint();
            status = AICommonBundle.message("settings.codefree.status.running.endpoint", endpoint);
            buttonText = AICommonBundle.message("settings.codefree.stop");
            startColor = CODEFREE_GREEN;
        } else if (jarReady) {
            // Jar 文件存在，可以启动
            status = AICommonBundle.message("settings.codefree.status.ready");
            buttonText = AICommonBundle.message("settings.codefree.start");
            startColor = CODEFREE_YELLOW;
        } else {
            // Jar 文件不存在，需要先下载
            status = AICommonBundle.message("settings.codefree.status.not.ready");
            buttonText = AICommonBundle.message("settings.codefree.start");
            startColor = CODEFREE_RED;
        }

        // 如果服务正在运行，使用带链接的状态文本
        if (running && endpoint != null) {
            setStatusTextWithLink(status, endpoint);
        } else {
            setStatusText(status);
        }
        getStatusLabel().setToolTipText(jarInfo != null ? jarInfo.path().toString() : null);
        getStartButton().setText(buttonText);
        getStartButton().setEnabled(buttonEnabled);
        setStartStatusColor(startColor);
        setLocalJarName(jarInfo != null ? jarInfo.fileName() : null, jarInfo != null ? jarInfo.size() : -1);
        if (jarInfo != null) {
            settings.jarFileName = jarInfo.fileName();
        }
        updateDownloadIndicator(jarReady, getLatestJarName(), jarInfo != null ? jarInfo.fileName() : null);
    }

    /**
     * 刷新 Codefree 版本信息
     *
     * @param settings Codefree 代理设置
     */
    public void refreshCodefreeVersionInfo(@NotNull CodefreeAgentSettings settings) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String latestName = null;
            long remoteSize = -1;
            String baseUrl = downloadUrlField.getText().trim();
            boolean remote = baseUrl.startsWith("http://") || baseUrl.startsWith("https://");
            try {
                if (remote) {
                    latestName = codefreeAgentManager.fetchLatestJarName(baseUrl);
                    if (!latestName.isBlank()) {
                        remoteSize = codefreeAgentManager.fetchRemoteJarSize(baseUrl, latestName);
                    }
                }
            } catch (Exception e) {
                LOG.warn("获取 Codefree 最新版本失败", e);
            }
            CodefreeAgentManager.JarInfo jarInfo = codefreeAgentManager.resolveLocalJarInfo(settings);
            boolean jarReady = jarInfo != null && Files.exists(jarInfo.path());
            String localName = jarInfo != null ? jarInfo.fileName() : null;
            long localSize = jarInfo != null ? jarInfo.size() : -1;
            String finalLatestName = latestName;
            long finalRemoteSize = remoteSize;
            ApplicationManager.getApplication().invokeLater(() -> {
                setLatestJarName(finalLatestName);
                setDownloadSize(remote ? finalRemoteSize : -1);
                setLocalJarName(localName, localSize);
                updateDownloadIndicator(jarReady, finalLatestName, localName);
            });
        });
    }

    /**
     * 更新下载指示器
     *
     * @param jarReady      jar 文件是否就绪
     * @param latestJarName 最新 jar 文件名
     * @param localJarName  本地 jar 文件名
     */
    private void updateDownloadIndicator(boolean jarReady,
                                         @Nullable String latestJarName,
                                         @Nullable String localJarName) {
        if (!jarReady) {
            setDownloadStatusColor(CODEFREE_RED);
            setDownloadButtonText(AICommonBundle.message("settings.codefree.download"));
            return;
        }
        if (latestJarName != null && !latestJarName.isBlank() && !latestJarName.equals(localJarName)) {
            setDownloadStatusColor(CODEFREE_YELLOW);
            setDownloadButtonText(AICommonBundle.message("settings.codefree.update"));
        } else {
            setDownloadStatusColor(CODEFREE_GREEN);
            setDownloadButtonText(AICommonBundle.message("settings.codefree.download"));
        }
    }

    /**
     * 下载 Codefree jar 文件
     * <p>
     * 根据提供的设置下载 Codefree 代理的 jar 文件, 并更新 UI 状态和进度.
     *
     * @param settings Codefree 代理设置, 包含下载地址等信息
     */
    public void downloadCodefreeJar(@NotNull CodefreeAgentSettings settings) {
        String downloadUrl = settings.downloadUrl != null ? settings.downloadUrl.trim() : "";
        if (downloadUrl.isBlank()) {
            JOptionPane.showMessageDialog(parentPanel != null ? parentPanel : getContent(),
                                          AICommonBundle.message("settings.codefree.error.no.url"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean remote = downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://");
        if (!remote) {
            JOptionPane.showMessageDialog(parentPanel != null ? parentPanel : getContent(),
                                          AICommonBundle.message("settings.codefree.download.local.path"),
                                          AICommonBundle.message("settings.codefree.title"),
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String jarFileName = getLatestJarName();
        if (jarFileName.isBlank()) {
            jarFileName = codefreeAgentManager.fetchLatestJarName(downloadUrl);
        }

        if (jarFileName.isBlank()) {
            JOptionPane.showMessageDialog(parentPanel != null ? parentPanel : getContent(),
                                          AICommonBundle.message("settings.codefree.error.no.jar"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        setStartedDownloadState();

        String finalJarFileName = jarFileName;
        String jarDownloadUrl = codefreeAgentManager.buildDownloadUrl(downloadUrl, finalJarFileName);
        CodefreeAgentSettings downloadSettings = settings.copy();
        downloadSettings.downloadUrl = jarDownloadUrl;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressIndicator indicator = new EmptyProgressIndicator();
            try {
                long remoteSize = codefreeAgentManager.fetchRemoteJarSize(downloadUrl, finalJarFileName);
                SwingUtilities.invokeLater(() -> {
                    if (remoteSize > 0) {
                        downloadProgressBar.setIndeterminate(false);
                        downloadProgressBar.setValue(0);
                        downloadProgressBar.setStringPainted(false);
                    } else {
                        downloadProgressBar.setIndeterminate(true);
                        downloadProgressBar.setStringPainted(false);
                    }
                });
                // 重置进度更新节流状态
                lastProgressUpdateTime = System.currentTimeMillis();
                lastProgressPercent = -1;

                Path savedPath = codefreeAgentManager.downloadJar(
                    downloadSettings,
                    finalJarFileName,
                    indicator,
                    (downloaded, total) -> {
                        // 优先使用回调中的 total，如果没有则使用远程大小
                        long progressTotal = total > 0 ? total : remoteSize;

                        // 节流：限制更新频率，避免 EDT 阻塞
                        if (progressTotal > 0) {
                            int currentPercent = (int) Math.min(100, Math.round(downloaded * 100.0 / progressTotal));
                            long currentTime = System.currentTimeMillis();

                            // 首次更新或百分比变化超过阈值或时间间隔超过阈值时才更新
                            boolean shouldUpdate = lastProgressPercent == -1 || // 首次更新
                                                   (currentPercent != lastProgressPercent &&
                                                    Math.abs(currentPercent - lastProgressPercent) >= PROGRESS_UPDATE_PERCENT_INTERVAL) ||
                                                   (currentTime - lastProgressUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS);

                            if (shouldUpdate) {
                                lastProgressPercent = currentPercent;
                                lastProgressUpdateTime = currentTime;

                                // 使用 SwingUtilities.invokeLater 确保在 EDT 中执行，避免被阻塞
                                SwingUtilities.invokeLater(() -> updateDownloadProgress(downloaded, progressTotal));
                            }
                        } else {
                            // 不确定模式：按时间节流，但首次更新立即执行
                            long currentTime = System.currentTimeMillis();
                            if (lastProgressUpdateTime == 0 ||
                                currentTime - lastProgressUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                                lastProgressUpdateTime = currentTime;
                                SwingUtilities.invokeLater(() -> updateDownloadProgress(downloaded, progressTotal));
                            }
                        }
                    });

                settings.jarFileName = finalJarFileName;
                long localSize = -1;
                try {
                    if (Files.exists(savedPath)) {
                        localSize = Files.size(savedPath);
                    }
                } catch (Exception sizeException) {
                    LOG.warn("读取 Codefree jar 大小失败: " + savedPath, sizeException);
                }
                long finalLocalSize = localSize;
                // 下载完成，强制更新进度条到 100%
                // 使用 SwingUtilities.invokeLater 确保立即执行，不被阻塞
                SwingUtilities.invokeLater(() -> {
                    // 确保进度条显示 100%
                    if (remoteSize > 0) {
                        updateDownloadProgress(remoteSize, remoteSize);
                    }
                    finishDownloadUi(true, finalJarFileName, finalLocalSize, settings);
                    refreshCodefreeVersionInfo(settings);
                });
            } catch (Exception e) {
                // 使用 SwingUtilities.invokeLater 确保立即执行，不被阻塞
                SwingUtilities.invokeLater(() -> {
                    // 确保在异常情况下也清除下载标志
                    isDownloading = false;
                    finishDownloadUi(false, null, -1, settings);
                    JOptionPane.showMessageDialog(parentPanel != null ? parentPanel : getContent(),
                                                  e.getMessage(),
                                                  AICommonBundle.message("settings.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    /**
     * 开始下载 Codefree jar 文件时, 设置相关状态
     * <p>
     * 禁用下载按钮并显示下载中状态, 同时初始化下载进度条并禁用启动按钮.
     */
    private void setStartedDownloadState() {
        isDownloading = true;
        downloadButton.setEnabled(false);
        downloadButton.setText(AICommonBundle.message("settings.codefree.download.doing"));
        // 初始化进度条状态
        downloadProgressBar.setVisible(true);
        downloadProgressBar.setIndeterminate(true);
        downloadProgressBar.setValue(0);
        downloadProgressBar.setStringPainted(false);
        setDownloadStatusColor(CODEFREE_YELLOW);
        startButton.setEnabled(false);
        // 重置进度更新节流状态
        lastProgressUpdateTime = 0;
        lastProgressPercent = -1;
    }

    /**
     * 完成下载操作的 UI 更新
     * <p>
     * 根据下载是否成功更新下载按钮, 进度条, 状态指示灯和 Codefree 代理状态.
     *
     * @param success   下载是否成功
     * @param jarName   下载的 jar 文件名, 若下载失败则为 null
     * @param localSize 本地 jar 文件大小, 若下载失败则为 -1
     * @param settings  Codefree 代理设置
     */
    private void finishDownloadUi(boolean success,
                                  @Nullable String jarName,
                                  long localSize,
                                  @NotNull CodefreeAgentSettings settings) {
        // 标记下载完成
        isDownloading = false;
        downloadButton.setEnabled(true);
        downloadButton.setText(AICommonBundle.message("settings.codefree.download"));
        // 确保进度条在下载完成后被隐藏
        resetDownloadProgress();
        if (success && jarName != null) {
            setLocalJarName(jarName, localSize);
            setDownloadStatusColor(CODEFREE_GREEN);
        } else {
            setDownloadStatusColor(CODEFREE_RED);
        }
        // 更新状态（包括启动按钮状态）
        updateCodefreeStatus(settings);
    }

    /**
     * 启动或停止 Codefree 本地代理
     *
     * @param settings Codefree 代理设置
     */
    public void toggleCodefreeAgent(@NotNull CodefreeAgentSettings settings) {
        if (codefreeAgentManager.isRunning()) {
            codefreeAgentManager.stopAgent();
            updateCodefreeStatus(settings);
            return;
        }
        Path jarPath = codefreeAgentManager.resolveJarPath(settings);
        if (Files.notExists(jarPath)) {
            JOptionPane.showMessageDialog(parentPanel != null ? parentPanel : getContent(),
                                          AICommonBundle.message("settings.codefree.error.no.jar"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }
        startButton.setEnabled(false);
        startButton.setText(AICommonBundle.message("settings.codefree.starting"));
        setStartStatusColor(CODEFREE_YELLOW);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                long pid = codefreeAgentManager.startAgent(settings);
                ApplicationManager.getApplication().invokeLater(() -> {
                    startButton.setEnabled(true);
                    startButton.setText(AICommonBundle.message("settings.codefree.stop"));
                    String tooltip = codefreeAgentManager.getLocalOpenAiEndpoint();
                    if (pid > 0) {
                        tooltip = tooltip + " (PID: " + pid + ")";
                    }
                    getStatusLabel().setToolTipText(tooltip);
                    updateCodefreeStatus(settings);
                });
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    startButton.setEnabled(true);
                    startButton.setText(AICommonBundle.message("settings.codefree.start"));
                    JOptionPane.showMessageDialog(parentPanel != null ? parentPanel : getContent(),
                                                  e.getMessage(),
                                                  AICommonBundle.message("settings.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                    updateCodefreeStatus(settings);
                });
            }
        });
    }

    /**
     * 创建 Codefree 代理配置快照
     *
     * @return Codefree 代理设置快照
     */
    @NotNull
    public CodefreeAgentSettings snapshotCodefreeSettings() {
        CodefreeAgentSettings snapshot = new CodefreeAgentSettings();
        snapshot.autoStart = getAutoStartCheckBox().isSelected();
        snapshot.downloadUrl = getDownloadUrlField().getText().trim();
        snapshot.jarFileName = getCurrentJarName();
        return snapshot;
    }

}
