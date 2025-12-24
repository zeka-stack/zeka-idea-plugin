package dev.dong4j.zeka.stack.idea.plugin.nacos.settings.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.common.ui.FeedbackPanel;
import dev.dong4j.zeka.stack.idea.plugin.nacos.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.local.LocalNacosService;
import dev.dong4j.zeka.stack.idea.plugin.nacos.model.LocalRegistry;
import dev.dong4j.zeka.stack.idea.plugin.nacos.model.LocalRegistryConstants;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.manager.LocalRegistryManager;
import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;
import lombok.Getter;

/**
 * Nacos 插件设置面板 UI
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosSettingsPanel {
    private static final Logger LOG = Logger.getInstance(NacosSettingsPanel.class);
    private static final JBColor DOT_COLOR_GREEN = new JBColor(new Color(52, 199, 89), new Color(48, 209, 88));
    private static final JBColor DOT_COLOR_RED = new JBColor(new Color(239, 68, 68), new Color(255, 82, 82));
    private static final JBColor DOT_COLOR_YELLOW = new JBColor(new Color(255, 193, 7), new Color(255, 214, 10));
    private static final long STOP_TIMEOUT_MILLIS = 5000L;
    private static final long STOP_FIRST_CHECK_DELAY = 3000L;
    private static final long STOP_SECOND_CHECK_DELAY = 2000L;

    /**
     * 主面板
     * -- GETTER --
     * 获取主面板
     */
    @Getter
    private final JPanel mainPanel;

    /** Nacos 服务器地址输入框 */
    private final JBTextField serverAddrField;

    /** 用户名输入框 */
    private final JBTextField usernameField;

    /** 密码输入框 */
    private final JBPasswordField passwordField;

    /** 测试连接按钮 */
    private final JButton testConnectionButton;

    /** 连接状态标签 */
    private final JBLabel connectionStatusLabel;

    /** 使用本地注册中心复选框 */
    private final JBCheckBox localRegistryCheckBox;

    /** 启动本地注册中心按钮 */
    private final JButton startLocalButton;

    /** 停止本地注册中心按钮 */
    private final JButton stopLocalButton;

    /** 访问链接 */
    private final HyperlinkLabel localStatusLink;

    /** 打开本地目录按钮 */
    private final JButton openLocalDirButton;

    /** Nacos 版本下拉列表 */
    private final ComboBox<String> versionComboBox;

    /** 启用 GitHub 加速下载复选框 */
    private final JBCheckBox enableGitHubProxyCheckBox;

    /** GitHub 代理地址输入框 */
    private final JBTextField gitHubProxyUrlField;

    /** 启动按钮呼吸指示器 */
    private final BreathingDotIcon startButtonIndicator;

    /** 测试连接按钮呼吸指示器 */
    private final BreathingDotIcon testButtonIndicator;

    /** JVM 环境变量配置表 */
    private final JBTable jvmOptionTable;
    private final JvmOptionTableModel jvmOptionTableModel;

    /** 是否处于本地 Nacos 操作中 */
    private boolean localOperationInProgress = false;

    /** 当前本地 Nacos 运行状态 */
    private volatile boolean localRegistryRunning = false;

    /** 缓存的存储密码（用于 isModified 比较，避免在 EDT 上调用慢操作） */
    private String cachedStoredPassword = null;

    /**
     * 构造函数, 初始化设置面板
     */
    public NacosSettingsPanel() {
        // 初始化组件
        serverAddrField = new JBTextField();
        usernameField = new JBTextField();
        passwordField = new JBPasswordField();
        testConnectionButton = new JButton(NacosBundle.message("settings.nacos.test.connection"));
        testConnectionButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        testConnectionButton.setIconTextGap(JBUI.scale(6));
        connectionStatusLabel = new JBLabel();
        localRegistryCheckBox = new JBCheckBox(NacosBundle.message("settings.nacos.local.enable"));
        startLocalButton = new JButton(NacosBundle.message("settings.nacos.local.start"));
        startLocalButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        startLocalButton.setIconTextGap(JBUI.scale(6));
        stopLocalButton = new JButton(NacosBundle.message("settings.nacos.local.stop"));
        localStatusLink = new HyperlinkLabel();
        localStatusLink.setHyperlinkTarget(LocalRegistryConstants.NACOS_TEST_URL);
        localStatusLink.setHyperlinkText(
            NacosBundle.message("settings.nacos.local.status.link.text"));
        localStatusLink.setToolTipText(LocalRegistryConstants.NACOS_TEST_URL);
        localStatusLink.addHyperlinkListener(e -> BrowserUtil.browse(LocalRegistryConstants.NACOS_TEST_URL));
        openLocalDirButton = new JButton(NacosBundle.message("settings.nacos.local.open.dir"));
        openLocalDirButton.addActionListener(e -> openLocalNacosDir());

        // 初始化 GitHub 加速下载复选框和输入框
        enableGitHubProxyCheckBox = new JBCheckBox(NacosBundle.message("settings.nacos.local.github.proxy.enable"));
        gitHubProxyUrlField = new JBTextField();
        gitHubProxyUrlField.getEmptyText().setText("https://gh-proxy.org/");
        gitHubProxyUrlField.setEnabled(false); // 默认禁用

        // 初始化版本下拉列表
        versionComboBox = new ComboBox<>(new String[] {"2.4.3"});
        Dimension versionSize = versionComboBox.getPreferredSize();
        versionComboBox.setMinimumSize(new Dimension(JBUI.scale(200), versionSize.height));
        versionComboBox.setPreferredSize(null);
        versionComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, versionSize.height));
        // 注意：版本选择会在 reset() 方法中从设置中回显
        versionComboBox.addActionListener(e -> {
            // 版本变更时，删除旧版本的 zip 包
            String selectedVersion = (String) versionComboBox.getSelectedItem();
            if (selectedVersion != null) {
                deleteOldVersionZipFiles(selectedVersion);
            }
            updateLocalHintDetailText();
        });

        jvmOptionTableModel = new JvmOptionTableModel();
        jvmOptionTable = new JBTable(jvmOptionTableModel);
        jvmOptionTable.setStriped(true);
        jvmOptionTable.setShowGrid(false);
        jvmOptionTable.setFillsViewportHeight(true);
        int headerHeight = jvmOptionTable.getTableHeader().getPreferredSize().height;
        int rowHeight = jvmOptionTable.getRowHeight() > 0 ? jvmOptionTable.getRowHeight() : JBUI.scale(24);
        int tableHeight = headerHeight + rowHeight * JvmOptionTableModel.MAX_ROWS;
        jvmOptionTable.setPreferredScrollableViewportSize(new Dimension(JBUI.scale(420), tableHeight));

        startLocalButton.setEnabled(false);
        stopLocalButton.setEnabled(false);
        startButtonIndicator = new BreathingDotIcon(startLocalButton, DOT_COLOR_RED);
        startLocalButton.setIcon(startButtonIndicator);
        startLocalButton.setDisabledIcon(startButtonIndicator);
        testButtonIndicator = new BreathingDotIcon(testConnectionButton, DOT_COLOR_YELLOW);
        testConnectionButton.setIcon(testButtonIndicator);
        testConnectionButton.setDisabledIcon(testButtonIndicator);

        // 设置组件提示信息
        //noinspection DialogTitleCapitalization
        serverAddrField.getEmptyText().setText("http://127.0.0.1:8848");
        usernameField.getEmptyText().setText(NacosBundle.message("settings.nacos.username.placeholder"));

        JPanel versionAndActionsPanel = createVersionAndActionsPanel();
        JPanel localLinksPanel = buildLocalLinksPanel();

        JPanel localRegistryPanel = FormBuilder.createFormBuilder()
            .addComponent(new TitledSeparator(NacosBundle.message("settings.nacos.local.section")))
            .addComponent(localRegistryCheckBox)
            .addComponent(createGitHubProxyPanel())
            .addComponent(versionAndActionsPanel)
            .addComponent(createConsoleLinkPanel())
            .addSeparator(8)
            .addComponent(createJvmOptionsPanel())
            .addComponent(localLinksPanel)
            .getPanel();
        localRegistryPanel.setBorder(sectionBorder());

        JPanel customRegistryPanel = FormBuilder.createFormBuilder()
            .addComponent(new TitledSeparator(NacosBundle.message("settings.nacos.custom.section")))
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.server.addr")), serverAddrField)
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.username")), usernameField)
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.password")), passwordField)
            .addLabeledComponent(new JBLabel(" "), createTestButtonRow())
            .addLabeledComponent(new JBLabel(" "), connectionStatusLabel)
            .getPanel();
        customRegistryPanel.setBorder(sectionBorder());

        // 初始化反馈面板
        FeedbackPanel feedbackPanel = new FeedbackPanel(
            null, // 应用级设置，project 为 null
            "dev.dong4j.zeka.stack.idea.plugin.nacos", // 插件 ID
            PluginContents.PLUGIN_NAME, // 插件名称
            "zeka-stack-nacos-plugin" // 签名密钥
        );

        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(localRegistryPanel)
            .addSeparator(12)
            .addComponent(customRegistryPanel)
            .addSeparator(12)
            .addComponent(feedbackPanel.getContent())
            .addComponentFillVertically(new JPanel(), 0);

        // 构建主面板
        mainPanel = builder.getPanel();

        // 设置边框
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // 初始化 JVM 配置
        jvmOptionTableModel.setData(SettingsState.getInstance().localJvmOptions);

        // 设置监听器
        setupListeners();

        updateLocalRegistryState();
        refreshLocalStatus();
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        testConnectionButton.addActionListener(e -> testConnection());
        localRegistryCheckBox.addActionListener(e -> {
            boolean selected = localRegistryCheckBox.isSelected();
            updateLocalRegistryState();
            if (selected) {
                refreshLocalStatus();
            } else {
                autoStopLocalNacos();
            }
        });
        enableGitHubProxyCheckBox.addActionListener(e -> {
            boolean selected = enableGitHubProxyCheckBox.isSelected();
            gitHubProxyUrlField.setEnabled(selected);
        });
        startLocalButton.addActionListener(e -> startLocalNacos());
        stopLocalButton.addActionListener(e -> stopLocalNacos());
        openLocalDirButton.addActionListener(e -> openLocalNacosDir());
    }

    /**
     * 测试 Nacos 连接
     */
    private void testConnection() {
        String serverAddr = serverAddrField.getText();
        String username = usernameField.getText();

        // 使用本地 nacos 服务时
        if (StringUtils.isNotBlank(username) && username.equals(LocalRegistryConstants.LOCAL_USERNAME)) {
            // 本地服务未启动
            if (!localRegistryRunning) {
                connectionStatusLabel.setText(
                    NacosBundle.message("settings.nacos.connection.failed.local.notstart"));
                connectionStatusLabel.setForeground(JBColor.RED);
                testButtonIndicator.setColor(DOT_COLOR_RED);
                return;
            }
        }

        String password = new String(passwordField.getPassword());

        if (serverAddr.isEmpty()) {
            connectionStatusLabel.setText(
                NacosBundle.message("settings.nacos.connection.failed.server.empty"));
            connectionStatusLabel.setForeground(JBColor.RED);
            testButtonIndicator.setColor(DOT_COLOR_RED);
            return;
        }

        testButtonIndicator.setColor(DOT_COLOR_YELLOW);

        // 在后台线程中执行连接测试
        new Task.Backgroundable(null, "Testing Nacos Connection", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Connecting to Nacos server...");

                boolean success;
                String message;
                try {
                    NacosClientUtils.removeClient(serverAddr, username);
                    NacosClient client = NacosClient.getInstance(serverAddr, username, password);
                    success = client.login();
                    message = success
                              ? NacosBundle.message("settings.nacos.connection.success")
                              : NacosBundle.message("settings.nacos.connection.failed", "Unauthorized");
                    if (success) {
                        SettingsState settings = SettingsState.getInstance();
                        settings.serverAddr = serverAddr;
                        settings.username = username;
                        settings.isAuthed = true;
                        settings.setPassword(password);
                    }
                } catch (Exception ex) {
                    success = false;
                    message = NacosBundle.message("settings.nacos.connection.failed", ex.getMessage());
                }

                boolean finalSuccess = success;
                String finalMessage = message;
                ApplicationManager.getApplication().invokeLater(() -> {
                    connectionStatusLabel.setText(finalMessage);
                    connectionStatusLabel.setForeground(finalSuccess ? JBColor.GREEN : JBColor.RED);
                    testButtonIndicator.setColor(finalSuccess ? DOT_COLOR_GREEN : DOT_COLOR_RED);
                }, ModalityState.defaultModalityState());
            }
        }.queue();
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     * <p>
     * 注意：此方法在 EDT 上调用，不能执行慢操作（如 PasswordSafe.get()）。
     * 因此使用缓存的密码值进行比较。
     */
    public boolean isModified(SettingsState settings) {
        // 使用缓存的密码值，避免在 EDT 上调用慢操作
        String storedPassword = cachedStoredPassword;
        String currentPassword = new String(passwordField.getPassword());
        boolean passwordChanged = storedPassword != null
                                  ? !Objects.equals(storedPassword, currentPassword)
                                  : !currentPassword.isEmpty();

        String selectedVersion = (String) versionComboBox.getSelectedItem();
        return !serverAddrField.getText().equals(settings.serverAddr)
               || !usernameField.getText().equals(settings.username)
               || localRegistryCheckBox.isSelected() != settings.useLocalRegistry
               || !Objects.equals(selectedVersion, settings.localNacosVersion)
               || enableGitHubProxyCheckBox.isSelected() != settings.enableGitHubProxy
               || !Objects.equals(gitHubProxyUrlField.getText(), settings.gitHubProxyUrl)
               || passwordChanged
               || isJvmOptionModified(settings);
    }

    /**
     * 将界面中的设置项应用到给定的 SettingsState 对象中
     */
    public void apply(SettingsState settings) {
        settings.serverAddr = serverAddrField.getText();
        settings.username = usernameField.getText();
        settings.useLocalRegistry = localRegistryCheckBox.isSelected();
        settings.enableGitHubProxy = enableGitHubProxyCheckBox.isSelected();
        settings.gitHubProxyUrl = gitHubProxyUrlField.getText();
        String selectedVersion = (String) versionComboBox.getSelectedItem();
        if (selectedVersion == null || selectedVersion.isEmpty()) {
            selectedVersion = "2.4.3";
        }
        settings.localNacosVersion = selectedVersion;

        // 保存密码到 CredentialStore，当输入为空时清除已保存密码
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            settings.setPassword(null);
            cachedStoredPassword = null; // 更新缓存
        } else {
            settings.setPassword(password);
            cachedStoredPassword = password; // 更新缓存
        }

        // 删除非当前版本的其他 zip 包
        deleteOldVersionZipFiles(selectedVersion);

        settings.localJvmOptions = new ArrayList<>(jvmOptionTableModel.getData());
    }

    /**
     * 重置界面设置为指定的配置状态
     */
    public void reset(SettingsState settings) {
        serverAddrField.setText(settings.serverAddr);
        usernameField.setText(settings.username);
        // 密码获取是慢操作，需要在后台线程执行
        passwordField.setText(""); // 先清空，避免显示旧密码
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String password = settings.getPassword();
                // 缓存密码值，用于 isModified() 比较（避免在 EDT 上调用慢操作）
                cachedStoredPassword = password;
                ApplicationManager.getApplication().invokeLater(() -> {
                    passwordField.setText(password != null ? password : "");
                }, ModalityState.any());
            } catch (Exception e) {
                LOG.warn("Failed to get password", e);
                cachedStoredPassword = null;
                ApplicationManager.getApplication().invokeLater(() -> {
                    passwordField.setText("");
                }, ModalityState.any());
            }
        });
        localRegistryCheckBox.setSelected(settings.useLocalRegistry);
        enableGitHubProxyCheckBox.setSelected(settings.enableGitHubProxy);
        gitHubProxyUrlField.setText(settings.gitHubProxyUrl != null && !settings.gitHubProxyUrl.isEmpty()
                                    ? settings.gitHubProxyUrl : "https://gh-proxy.org/");
        gitHubProxyUrlField.setEnabled(settings.enableGitHubProxy);

        // 回显版本选择
        String version = settings.localNacosVersion != null && !settings.localNacosVersion.isEmpty()
                         ? settings.localNacosVersion : "2.4.3";
        // 确保版本在下拉列表中，如果不存在则添加
        boolean versionExists = false;
        for (int i = 0; i < versionComboBox.getItemCount(); i++) {
            if (version.equals(versionComboBox.getItemAt(i))) {
                versionExists = true;
                break;
            }
        }
        if (!versionExists) {
            versionComboBox.addItem(version);
        }
        versionComboBox.setSelectedItem(version);
        jvmOptionTableModel.setData(settings.localJvmOptions);
        updateLocalHintDetailText();

        testButtonIndicator.setColor(DOT_COLOR_YELLOW);
        updateLocalRegistryState();
        refreshLocalStatus();
    }

    /**
     * 释放资源
     */
    public void dispose() {
        // 清理资源
        startButtonIndicator.dispose();
        testButtonIndicator.dispose();
    }

    /**
     * 根据本地注册中心开关更新其他组件状态
     */
    private void updateLocalRegistryState() {
        boolean useLocalRegistry = localRegistryCheckBox.isSelected();
        boolean controlsEnabled = useLocalRegistry && !localOperationInProgress;
        boolean remoteEnabled = !localOperationInProgress;

        localRegistryCheckBox.setEnabled(!localOperationInProgress);
        versionComboBox.setEnabled(useLocalRegistry && !localOperationInProgress);
        startLocalButton.setEnabled(controlsEnabled && !localRegistryRunning);
        stopLocalButton.setEnabled(controlsEnabled && localRegistryRunning);
        openLocalDirButton.setEnabled(true);

        serverAddrField.setEnabled(remoteEnabled);
        usernameField.setEnabled(remoteEnabled);
        passwordField.setEnabled(remoteEnabled);
        testConnectionButton.setEnabled(remoteEnabled);
        connectionStatusLabel.setEnabled(remoteEnabled);

        // GitHub 代理相关组件根据本地注册中心状态和复选框状态控制
        boolean proxyEnabled = useLocalRegistry && !localOperationInProgress && enableGitHubProxyCheckBox.isSelected();
        enableGitHubProxyCheckBox.setEnabled(useLocalRegistry && !localOperationInProgress);
        gitHubProxyUrlField.setEnabled(proxyEnabled);
    }

    private void startLocalNacos() {
        if (localOperationInProgress) {
            return;
        }
        setLocalOperationInProgress(true);
        startLocalButton.setText(NacosBundle.message("settings.nacos.local.starting"));
        String selectedVersion = (String) versionComboBox.getSelectedItem();
        if (selectedVersion == null || selectedVersion.isEmpty()) {
            // 如果下拉列表没有选择，从设置中获取
            SettingsState settings = SettingsState.getInstance();
            selectedVersion = settings.localNacosVersion != null && !settings.localNacosVersion.isEmpty()
                              ? settings.localNacosVersion : "2.4.3";
        }
        final String version = selectedVersion;

        CompletableFuture
            .supplyAsync(() -> LocalRegistryManager.localRegistryStarted(LocalRegistry.NACOS),
                         AppExecutorUtil.getAppExecutorService())
            .thenAccept(running -> ApplicationManager.getApplication().invokeLater(() -> {
                if (running) {
                    startLocalButton.setText(NacosBundle.message("settings.nacos.local.start"));
                    NotificationUtil.showInfo(null,
                                              NacosBundle.message("notification.local.nacos.already.running"));
                    setLocalOperationInProgress(false);
                    updateLocalStatusVisual(true);
                } else {
                    bindLocalOperation(LocalNacosService.getInstance().startLocalRegistry(version),
                                       () -> startLocalButton.setText(NacosBundle.message("settings.nacos.local.start")));
                }
            }, ModalityState.any()));
    }

    private void stopLocalNacos() {
        stopLocalNacosInternal(true);
    }

    private void stopLocalNacosInternal(boolean updateButtonLabel) {
        if (localOperationInProgress) {
            return;
        }
        setLocalOperationInProgress(true);
        if (updateButtonLabel) {
            stopLocalButton.setText(NacosBundle.message("settings.nacos.local.stopping"));
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(null,
                                                                  NacosBundle.message("settings.nacos.local.stopping"),
                                                                  false) {
            private String failureMessage;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0.0);
                try {
                    LocalNacosService.getInstance().stopLocalRegistry().get(STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                    if (!waitUntilRegistryStops(indicator)) {
                        failureMessage = NacosBundle.message("notification.local.nacos.stop.timeout");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    failureMessage = ex.getMessage();
                } catch (ExecutionException | TimeoutException ex) {
                    failureMessage = ex.getMessage();
                }
            }

            @Override
            public void onFinished() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (updateButtonLabel) {
                        stopLocalButton.setText(NacosBundle.message("settings.nacos.local.stop"));
                    }
                    setLocalOperationInProgress(false);
                    refreshLocalStatus();
                    if (failureMessage != null) {
                        NotificationUtil.showError(null,
                                                   NacosBundle.message("notification.local.nacos.stop.failed",
                                                                       failureMessage));
                    }
                }, ModalityState.any());
            }
        });
    }

    private void bindLocalOperation(@NotNull CompletableFuture<Void> future, @NotNull Runnable onComplete) {
        future.whenComplete((unused, throwable) -> ApplicationManager.getApplication().invokeLater(() -> {
            try {
                onComplete.run();
            } finally {
                setLocalOperationInProgress(false);
                refreshLocalStatus();
                if (throwable != null) {
                    LOG.warn("Local Nacos operation failed", throwable);
                }
            }
        }, ModalityState.any()));
    }

    private void setLocalOperationInProgress(boolean inProgress) {
        this.localOperationInProgress = inProgress;
        updateLocalRegistryState();
    }

    private void autoStopLocalNacos() {
        CompletableFuture
            .supplyAsync(() -> LocalRegistryManager.localRegistryStarted(LocalRegistry.NACOS),
                         AppExecutorUtil.getAppExecutorService())
            .thenAccept(running -> ApplicationManager.getApplication().invokeLater(() -> {
                if (running) {
                    stopLocalNacosInternal(false);
                } else {
                    refreshLocalStatus();
                }
            }, ModalityState.any()));
    }

    private void refreshLocalStatus() {
        CompletableFuture
            .supplyAsync(() -> LocalRegistryManager.localRegistryStarted(LocalRegistry.NACOS),
                         AppExecutorUtil.getAppExecutorService())
            .thenAccept(running -> ApplicationManager.getApplication().invokeLater(() -> updateLocalStatusVisual(running),
                                                                                   ModalityState.any()));
    }

    private void updateLocalStatusVisual(boolean running) {
        this.localRegistryRunning = running;
        if (running) {
            openLocalDirButton.setEnabled(true);
            startButtonIndicator.setColor(DOT_COLOR_GREEN);
        } else {
            openLocalDirButton.setEnabled(false);
            startButtonIndicator.setColor(DOT_COLOR_RED);
        }
        updateLocalRegistryState();
    }

    private boolean waitUntilRegistryStops(@NotNull ProgressIndicator indicator) throws InterruptedException {
        if (waitAndCheck(indicator, STOP_FIRST_CHECK_DELAY, 0.6)) {
            return true;
        }
        return waitAndCheck(indicator, STOP_SECOND_CHECK_DELAY, 1.0);
    }

    private boolean waitAndCheck(@NotNull ProgressIndicator indicator, long waitMillis, double fraction)
        throws InterruptedException {
        indicator.checkCanceled();
        TimeUnit.MILLISECONDS.sleep(waitMillis);
        indicator.checkCanceled();
        indicator.setFraction(fraction);
        return !LocalRegistryManager.localRegistryStarted(LocalRegistry.NACOS);
    }

    private JPanel buildLocalLinksPanel() {
        JPanel firstLine = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0));
        firstLine.setOpaque(false);
        firstLine.add(new JBLabel(NacosBundle.message("settings.nacos.local.hint.prefix")));
        firstLine.add(openLocalDirButton);
        firstLine.add(new JBLabel(NacosBundle.message("settings.nacos.local.hint.suffix")));
        firstLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        String zipPath = updateLocalHintDetailText();
        String hintHtml = NacosBundle.message("settings.nacos.local.hint.detail", zipPath);
        JBLabel hintLabel = new JBLabel(hintHtml);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(JBUI.Borders.emptyLeft(20));
        container.add(firstLine);
        container.add(hintLabel);
        return container;
    }

    private void openLocalNacosDir() {
        String path = LocalRegistryConstants.NACOS_DIR;
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                NotificationUtil.showWarning(null, NacosBundle.message("notification.local.nacos.open.dir.missing", path));
                return;
            }
            Desktop.getDesktop().open(dir);
        } catch (Exception ex) {
            NotificationUtil.showError(null, NacosBundle.message("notification.local.nacos.open.dir.failed", ex.getMessage()));
            LOG.warn("Failed to open local Nacos directory", ex);
        }
    }

    private String updateLocalHintDetailText() {
        String version = (String) versionComboBox.getSelectedItem();
        if (StringUtils.isBlank(version)) {
            version = SettingsState.getInstance().localNacosVersion;
        }
        if (StringUtils.isBlank(version)) {
            version = "2.4.3";
        }
        return SystemInfo.isWindows
               ? LocalRegistryConstants.getNacosLocalPathForWin(version)
               : LocalRegistryConstants.getNacosLocalPathForMac(version);

    }

    /**
     * 删除非当前版本的其他 zip 包
     *
     * @param currentVersion 当前版本号
     */
    private void deleteOldVersionZipFiles(String currentVersion) {
        int deletedCount = LocalRegistryManager.deleteOldVersionZipFiles(currentVersion);
        if (deletedCount > 0) {
            LOG.info("Deleted " + deletedCount + " old version zip file(s)");
        }
    }

    /**
     * 创建统一的 section 边框
     *
     * @return 边框
     */
    private static Border sectionBorder() {
        return JBUI.Borders.compound(
            BorderFactory.createLineBorder(UIManager.getColor("Separator.separatorColor")),
            JBUI.Borders.empty(10));
    }

    private JPanel createGitHubProxyPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.emptyLeft(20));

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        // 复选框
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        gbc.insets = JBUI.insetsRight(JBUI.scale(8));
        contentPanel.add(enableGitHubProxyCheckBox, gbc);

        // 输入框
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.emptyInsets();
        contentPanel.add(gitHubProxyUrlField, gbc);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createVersionAndActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.emptyLeft(20));

        JBLabel label = new JBLabel(NacosBundle.message("settings.nacos.local.version"));
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        centerPanel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = JBUI.insetsLeft(JBUI.scale(8));
        centerPanel.add(versionComboBox, gbc);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(startLocalButton);
        actionsPanel.add(stopLocalButton);

        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(actionsPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createRightAlignedPanel(@NotNull JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.add(component);
        return panel;
    }

    private JPanel createTestButtonRow() {
        JPanel panel = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        panel.setOpaque(false);

        JBLabel hintLabel = new JBLabel(NacosBundle.message("settings.nacos.local.username.hint"));
        hintLabel.setForeground(JBColor.GRAY);

        panel.add(hintLabel, BorderLayout.WEST);
        panel.add(createRightAlignedPanel(testConnectionButton), BorderLayout.EAST);
        return panel;
    }

    private JPanel createConsoleLinkPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
        panel.setOpaque(false);
        JBLabel label = new JBLabel();
        panel.add(label);
        panel.add(localStatusLink);
        return panel;
    }

    private JPanel createJvmOptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(JBUI.Borders.emptyLeft(20));

        JBLabel title = new JBLabel(NacosBundle.message("settings.nacos.local.jvm.options"));
        title.setBorder(JBUI.Borders.emptyBottom(4));
        JBLabel hint = new JBLabel(NacosBundle.message("settings.nacos.local.jvm.options.hint"));
        hint.setForeground(JBColor.GRAY);
        hint.setBorder(JBUI.Borders.emptyBottom(4));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(hint, BorderLayout.CENTER);
        panel.add(header, BorderLayout.NORTH);

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(jvmOptionTable)
            .setAddAction(actionButton -> jvmOptionTableModel.addRow())
            .setRemoveAction(actionButton -> {
                int selected = jvmOptionTable.getSelectedRow();
                if (selected >= 0) {
                    jvmOptionTableModel.removeRow(selected);
                }
            })
            .setMoveUpAction(actionButton -> jvmOptionTableModel.moveRowUp(jvmOptionTable.getSelectedRow()))
            .setMoveDownAction(actionButton -> jvmOptionTableModel.moveRowDown(jvmOptionTable.getSelectedRow()));

        panel.add(decorator.createPanel(), BorderLayout.CENTER);
        return panel;
    }

    private boolean isJvmOptionModified(SettingsState settings) {
        java.util.List<SettingsState.EnvVariable> current = jvmOptionTableModel.getData();
        java.util.List<SettingsState.EnvVariable> stored = settings.localJvmOptions;
        if (current.size() != stored.size()) {
            return true;
        }
        for (int i = 0; i < current.size(); i++) {
            if (!current.get(i).equals(stored.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static class JvmOptionTableModel extends AbstractTableModel {
        private static final int MAX_ROWS = 3;
        private final java.util.List<SettingsState.EnvVariable> data = new java.util.ArrayList<>();
        private final String[] columns = {
            NacosBundle.message("settings.nacos.local.jvm.options.column.name"),
            NacosBundle.message("settings.nacos.local.jvm.options.column.value")
        };

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SettingsState.EnvVariable entry = data.get(rowIndex);
            return columnIndex == 0 ? entry.name : entry.value;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            SettingsState.EnvVariable entry = data.get(rowIndex);
            if (columnIndex == 0) {
                entry.name = aValue != null ? aValue.toString() : "";
            } else {
                entry.value = aValue != null ? aValue.toString() : "";
            }
            fireTableRowsUpdated(rowIndex, rowIndex);
        }

        void setData(java.util.List<SettingsState.EnvVariable> newData) {
            data.clear();
            if (newData != null) {
                for (SettingsState.EnvVariable item : newData) {
                    if (data.size() >= MAX_ROWS) {
                        break;
                    }
                    data.add(new SettingsState.EnvVariable(item.name, item.value));
                }
            }
            if (data.isEmpty()) {
                data.add(new SettingsState.EnvVariable());
            }
            fireTableDataChanged();
        }

        java.util.List<SettingsState.EnvVariable> getData() {
            java.util.List<SettingsState.EnvVariable> copy = new java.util.ArrayList<>();
            for (SettingsState.EnvVariable item : data) {
                boolean emptyName = item.name == null || item.name.trim().isEmpty();
                boolean emptyValue = item.value == null || item.value.trim().isEmpty();
                if (emptyName && emptyValue) {
                    continue;
                }
                copy.add(new SettingsState.EnvVariable(item.name, item.value));
            }
            return copy;
        }

        void addRow() {
            if (data.size() >= MAX_ROWS) {
                return;
            }
            data.add(new SettingsState.EnvVariable());
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        void removeRow(int row) {
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                fireTableRowsDeleted(row, row);
            }
            if (data.isEmpty()) {
                data.add(new SettingsState.EnvVariable());
                fireTableRowsInserted(0, 0);
            }
        }

        void moveRowUp(int row) {
            if (row > 0 && row < data.size()) {
                java.util.Collections.swap(data, row, row - 1);
                fireTableRowsUpdated(row - 1, row);
            }
        }

        void moveRowDown(int row) {
            if (row >= 0 && row < data.size() - 1) {
                java.util.Collections.swap(data, row, row + 1);
                fireTableRowsUpdated(row, row + 1);
            }
        }
    }

    /**
     * 带呼吸效果的圆点图标
     */
    private static class BreathingDotIcon implements Icon {
        private static final int SIZE = JBUI.scale(8);
        private static final int TIMER_DELAY = 50;
        private final Timer timer;
        private final Component owner;
        private float phase;
        private Color color;

        BreathingDotIcon(@NotNull Component owner, @NotNull Color initialColor) {
            this.owner = owner;
            this.color = initialColor;
            this.timer = new Timer(TIMER_DELAY, e -> {
                phase += 0.08f;
                if (phase > Math.PI * 2) {
                    phase -= Math.PI * 2;
                }
                owner.repaint();
            });
            this.timer.start();
        }

        void setColor(@NotNull Color color) {
            this.color = color;
        }

        void dispose() {
            timer.stop();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
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
