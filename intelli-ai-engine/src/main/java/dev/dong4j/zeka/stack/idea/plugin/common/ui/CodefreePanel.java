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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;

import dev.dong4j.zeka.stack.idea.plugin.common.codefree.CodefreeAgentManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.CodefreeAgentSettings;
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
    private static final Logger LOG = Logger.getInstance(CodefreePanel.class);
    private static final JBColor DOT_GREEN = new JBColor(new Color(52, 199, 89), new Color(48, 209, 88));
    private static final JBColor DOT_RED = new JBColor(new Color(239, 68, 68), new Color(255, 82, 82));
    private static final JBColor DOT_YELLOW = new JBColor(new Color(255, 193, 7), new Color(255, 214, 10));
    private static final JBColor CODEFREE_GREEN = new JBColor(new Color(76, 175, 80), new Color(76, 175, 80));
    private static final JBColor CODEFREE_RED = new JBColor(new Color(244, 67, 54), new Color(244, 67, 54));
    private static final JBColor CODEFREE_YELLOW = new JBColor(new Color(255, 193, 7), new Color(255, 193, 7));
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.00");
    /** 主面板 */
    @NotNull
    private final JPanel content;
    /** 自动启动复选框 */
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
    /** 下载进度条 */
    @NotNull
    private final JProgressBar downloadProgressBar;
    /** 下载按钮 */
    @NotNull
    private final JButton downloadButton;
    /** 启动/停止按钮 */
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
    /** 远端版本名称 */
    @Nullable
    private String latestJarName;
    /** 当前本地 jar 名称 */
    @Nullable
    private String currentJarName;
    /** Codefree 代理管理器 */
    @NotNull
    private final CodefreeAgentManager codefreeAgentManager = CodefreeAgentManager.getInstance();
    /** 父面板，用于显示对话框 */
    @Nullable
    private JPanel parentPanel;

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

        statusLabel = new JBLabel(AICommonBundle.message("settings.codefree.status.not.ready"));
        latestVersionLabel = new JBLabel(AICommonBundle.message("settings.codefree.version.checking"));
        localJarLabel = new JBLabel(AICommonBundle.message("settings.codefree.version.local.empty"));
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
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.codefree.download.url")), downloadUrlField)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.codefree.version.latest")), latestVersionLabel)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.codefree.version.local")), localJarLabel)
            .addLabeledComponent(new JBLabel(AICommonBundle.message("settings.codefree.status")), statusLabel)
            .addComponent(progressPanel)
            .addComponent(buttonsPanel)
            .getPanel();

        // 创建可折叠容器
        content = createCollapsiblePanel();

        updateDownloadButtonState();

        // 启动状态更新定时器（每 2 秒检查一次服务状态）
        startStatusUpdateTimer();
    }

    /**
     * 启动状态更新定时器
     * <p>
     * 定期检查 Codefree 代理服务状态，并更新 UI。
     */
    private void startStatusUpdateTimer() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
        }
        statusUpdateTimer = new Timer(2000, e -> {
            if (statusUpdateCallback != null) {
                statusUpdateCallback.run();
            }
        });
        statusUpdateTimer.setRepeats(true);
        statusUpdateTimer.start();
    }

    /**
     * 停止状态更新定时器
     */
    public void stopStatusUpdateTimer() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
            statusUpdateTimer = null;
        }
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
     * 创建可折叠的标题栏
     *
     * @param title 标题文本（包含箭头）
     * @return 标题栏面板
     */
    @NotNull
    private JPanel createCollapsibleTitle(@NotNull String title) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        // 默认折叠状态，使用右箭头
        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
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
     * @param title      标题文本（不包含箭头）
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

        JBLabel hintLabel = new JBLabel(AICommonBundle.message(hintKey));
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

    public void updateDownloadProgress(long downloaded, long totalBytes) {
        downloadProgressBar.setVisible(true);
        if (totalBytes > 0) {
            int percent = (int) Math.min(100, Math.round(downloaded * 100.0 / totalBytes));
            downloadProgressBar.setIndeterminate(false);
            downloadProgressBar.setValue(percent);
        } else {
            downloadProgressBar.setIndeterminate(true);
        }
        downloadProgressBar.repaint();
    }

    public void resetDownloadProgress() {
        downloadProgressBar.setVisible(false);
        downloadProgressBar.setIndeterminate(false);
        downloadProgressBar.setValue(0);
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
        boolean buttonEnabled = running || jarReady;
        JBColor startColor;

        if (running) {
            // 服务正在运行
            status = AICommonBundle.message("settings.codefree.status.running.endpoint", codefreeAgentManager.getLocalOpenAiEndpoint());
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

        setStatusText(status);
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
     * 下载 Codefree jar
     *
     * @param settings Codefree 代理设置
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

        downloadButton.setEnabled(false);
        downloadButton.setText(AICommonBundle.message("settings.codefree.download.doing"));
        resetDownloadProgress();
        downloadProgressBar.setVisible(true);
        downloadProgressBar.setIndeterminate(true);
        downloadProgressBar.setValue(0);
        setDownloadStatusColor(CODEFREE_YELLOW);
        startButton.setEnabled(false);

        String finalJarFileName = jarFileName;
        String jarDownloadUrl = codefreeAgentManager.buildDownloadUrl(downloadUrl, finalJarFileName);
        CodefreeAgentSettings downloadSettings = settings.copy();
        downloadSettings.downloadUrl = jarDownloadUrl;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressIndicator indicator = new EmptyProgressIndicator();
            try {
                long remoteSize = -1;
                try {
                    remoteSize = codefreeAgentManager.fetchRemoteJarSize(downloadUrl, finalJarFileName);
                } catch (Exception sizeException) {
                    LOG.warn("获取 Codefree jar 大小失败: " + finalJarFileName, sizeException);
                }
                long finalRemoteSize = remoteSize;
                ApplicationManager.getApplication().invokeLater(() -> {
                    downloadProgressBar.setIndeterminate(finalRemoteSize <= 0);
                    downloadProgressBar.setValue(0);
                });
                Path savedPath = codefreeAgentManager.downloadJar(
                    downloadSettings,
                    finalJarFileName,
                    indicator,
                    (downloaded, total) -> ApplicationManager.getApplication().invokeLater(() -> {
                        long progressTotal = total > 0 ? total : finalRemoteSize;
                        updateDownloadProgress(downloaded, progressTotal);
                    }));

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
                ApplicationManager.getApplication().invokeLater(() -> {
                    finishDownloadUi(true, finalJarFileName, finalLocalSize, settings);
                    refreshCodefreeVersionInfo(settings);
                });
            } catch (Exception e) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    finishDownloadUi(false, null, -1, settings);
                    JOptionPane.showMessageDialog(parentPanel != null ? parentPanel : getContent(),
                                                  e.getMessage(),
                                                  AICommonBundle.message("settings.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void finishDownloadUi(boolean success,
                                  @Nullable String jarName,
                                  long localSize,
                                  @NotNull CodefreeAgentSettings settings) {
        downloadButton.setEnabled(true);
        downloadButton.setText(AICommonBundle.message("settings.codefree.download"));
        resetDownloadProgress();
        if (success && jarName != null) {
            setLocalJarName(jarName, localSize);
            setDownloadStatusColor(CODEFREE_GREEN);
        } else {
            setDownloadStatusColor(CODEFREE_RED);
        }
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

    /**
     * 带呼吸效果的圆点图标
     */
    private static class BreathingDotIcon implements Icon {
        private static final int SIZE = JBUI.scale(8);
        private static final int TIMER_DELAY = 50;
        private final Timer timer;
        private final java.awt.Component owner;
        private float phase;
        private Color color;

        BreathingDotIcon(@NotNull java.awt.Component owner, @NotNull Color initialColor) {
            this.owner = owner;
            this.color = initialColor;
            this.timer = new Timer(TIMER_DELAY, e -> {
                phase += 0.08f;
                if (phase > Math.PI * 2) {
                    phase -= (float) (Math.PI * 2);
                }
                owner.repaint();
            });
            this.timer.start();
        }

        void setColor(@NotNull Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            float alpha = 0.5f + 0.5f * (float) Math.sin(phase);
            int a = (int) (alpha * 255);
            @SuppressWarnings("UseJBColor")
            Color drawColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(60, Math.min(255, a)));

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(drawColor);
            g2.fillOval(x, y, SIZE, SIZE);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
