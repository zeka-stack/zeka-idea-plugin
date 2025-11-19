package dev.dong4j.zeka.stack.idea.plugin.nacos.settings.ui;

import com.alibabacloud.intellij.model.edas.LocalRegistry;
import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryConstants;
import com.alibabacloud.intellij.service.edas.registry.local.LocalRegistryManager;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import javax.swing.JButton;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.local.LocalNacosService;
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

    /** 全局管理员复选框 */
    private final JBCheckBox globalAdminCheckBox;

    /** 使用本地注册中心复选框 */
    private final JBCheckBox localRegistryCheckBox;

    /** 启动本地注册中心按钮 */
    private final JButton startLocalButton;

    /** 停止本地注册中心按钮 */
    private final JButton stopLocalButton;

    /** 状态标签 */
    private final JBLabel localStatusIndicator;

    /** 访问链接 */
    private final HyperlinkLabel localStatusLink;

    /** 打开本地目录按钮 */
    private final JButton openLocalDirButton;

    /** 是否处于本地 Nacos 操作中 */
    private boolean localOperationInProgress = false;

    /** 当前本地 Nacos 运行状态 */
    private volatile boolean localRegistryRunning = false;

    /**
     * 构造函数, 初始化设置面板
     */
    public NacosSettingsPanel() {
        // 初始化组件
        serverAddrField = new JBTextField();
        usernameField = new JBTextField();
        passwordField = new JBPasswordField();
        testConnectionButton = new JButton(NacosBundle.message("settings.nacos.test.connection"));
        connectionStatusLabel = new JBLabel();
        globalAdminCheckBox = new JBCheckBox(NacosBundle.message("settings.nacos.global.admin"));
        localRegistryCheckBox = new JBCheckBox(NacosBundle.message("settings.nacos.local.enable"));
        startLocalButton = new JButton(NacosBundle.message("settings.nacos.local.start"));
        stopLocalButton = new JButton(NacosBundle.message("settings.nacos.local.stop"));
        localStatusIndicator = new JBLabel(NacosBundle.message("settings.nacos.local.status.checking"));
        localStatusIndicator.setForeground(JBColor.GRAY);
        localStatusLink = new HyperlinkLabel();
        localStatusLink.setVisible(false);
        localStatusLink.setHyperlinkTarget(LocalRegistryConstants.NACOS_TEST_URL);
        localStatusLink.setHyperlinkText(
            NacosBundle.message("settings.nacos.local.status.link.prefix"),
            NacosBundle.message("settings.nacos.local.status.link.text"),
            "");
        localStatusLink.setToolTipText(LocalRegistryConstants.NACOS_TEST_URL);
        localStatusLink.addHyperlinkListener(e -> BrowserUtil.browse(LocalRegistryConstants.NACOS_TEST_URL));
        openLocalDirButton = new JButton(NacosBundle.message("settings.nacos.local.open.dir"));
        openLocalDirButton.addActionListener(e -> openLocalNacosDir());

        startLocalButton.setEnabled(false);
        stopLocalButton.setEnabled(false);

        // 设置组件提示信息
        serverAddrField.getEmptyText().setText("http://localhost:8848");
        usernameField.getEmptyText().setText("nacos");

        JPanel localActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(8), 0));
        localActionsPanel.setOpaque(false);
        localActionsPanel.add(startLocalButton);
        localActionsPanel.add(stopLocalButton);

        SettingsState settings = SettingsState.getInstance();
        localRegistryCheckBox.setSelected(settings.useLocalRegistry);

        JPanel localStatusPanel = buildLocalStatusPanel();

        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(new TitledSeparator(NacosBundle.message("settings.nacos.local.section")))
            .addComponent(localRegistryCheckBox)
            .addComponent(localActionsPanel)
            .addComponent(localStatusPanel)
            .addSeparator(12)
            .addComponent(new TitledSeparator(NacosBundle.message("settings.nacos.custom.section")))
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.server.addr") + ":"), serverAddrField)
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.username") + ":"), usernameField)
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.password") + ":"), passwordField)
            .addComponent(testConnectionButton)
            .addComponent(connectionStatusLabel)
            .addComponent(globalAdminCheckBox)
            .addComponentFillVertically(new JPanel(), 0);

        // 构建主面板
        mainPanel = builder.getPanel();

        // 设置边框
        mainPanel.setBorder(JBUI.Borders.empty(10));

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
        String password = new String(passwordField.getPassword());

        if (serverAddr.isEmpty()) {
            connectionStatusLabel.setText(
                NacosBundle.message("settings.nacos.connection.failed.server.empty"));
            connectionStatusLabel.setForeground(JBColor.RED);
            return;
        }

        // 在后台线程中执行连接测试
        new Task.Backgroundable(null, "Testing Nacos Connection", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Connecting to Nacos server...");

                boolean success;
                String message;
                boolean globalAdmin = false;
                try {
                    NacosClientUtils.removeClient(serverAddr, username);
                    NacosClient client = NacosClient.getInstance(serverAddr, username, password);
                    success = client.login();
                    globalAdmin = client.isGlobalAdmin();
                    message = success
                              ? NacosBundle.message("settings.nacos.connection.success")
                              : NacosBundle.message("settings.nacos.connection.failed", "Unauthorized");
                    if (success) {
                        SettingsState settings = SettingsState.getInstance();
                        settings.serverAddr = serverAddr;
                        settings.username = username;
                        settings.globalAdmin = globalAdmin;
                        settings.isAuthed = true;
                        settings.setPassword(password);
                    }
                } catch (Exception ex) {
                    success = false;
                    message = NacosBundle.message("settings.nacos.connection.failed", ex.getMessage());
                }

                boolean finalSuccess = success;
                boolean finalGlobalAdmin = globalAdmin;
                String finalMessage = message;
                ApplicationManager.getApplication().invokeLater(() -> {
                    connectionStatusLabel.setText(finalMessage);
                    connectionStatusLabel.setForeground(finalSuccess ? JBColor.GREEN : JBColor.RED);
                    if (finalSuccess) {
                        globalAdminCheckBox.setSelected(finalGlobalAdmin);
                    }
                }, ModalityState.defaultModalityState());
            }
        }.queue();
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     */
    public boolean isModified(SettingsState settings) {
        String storedPassword = settings.getPassword();
        String currentPassword = new String(passwordField.getPassword());
        boolean passwordChanged = storedPassword != null
                                  ? !Objects.equals(storedPassword, currentPassword)
                                  : !currentPassword.isEmpty();

        return !serverAddrField.getText().equals(settings.serverAddr)
               || !usernameField.getText().equals(settings.username)
               || globalAdminCheckBox.isSelected() != settings.globalAdmin
               || localRegistryCheckBox.isSelected() != settings.useLocalRegistry
               || passwordChanged;
    }

    /**
     * 将界面中的设置项应用到给定的 SettingsState 对象中
     */
    public void apply(SettingsState settings) {
        settings.serverAddr = serverAddrField.getText();
        settings.username = usernameField.getText();
        settings.globalAdmin = globalAdminCheckBox.isSelected();
        settings.useLocalRegistry = localRegistryCheckBox.isSelected();

        // 保存密码到 CredentialStore，当输入为空时清除已保存密码
        String password = new String(passwordField.getPassword());
        if (password.isEmpty()) {
            settings.setPassword(null);
        } else {
            settings.setPassword(password);
        }
    }

    /**
     * 重置界面设置为指定的配置状态
     */
    public void reset(SettingsState settings) {
        serverAddrField.setText(settings.serverAddr);
        usernameField.setText(settings.username);
        globalAdminCheckBox.setSelected(settings.globalAdmin);
        String password = settings.getPassword();
        passwordField.setText(password != null ? password : "");
        localRegistryCheckBox.setSelected(settings.useLocalRegistry);
        updateLocalRegistryState();
        refreshLocalStatus();
    }

    /**
     * 释放资源
     */
    public void dispose() {
        // 清理资源
    }

    /**
     * 根据本地注册中心开关更新其他组件状态
     */
    private void updateLocalRegistryState() {
        boolean useLocalRegistry = localRegistryCheckBox.isSelected();
        boolean controlsEnabled = useLocalRegistry && !localOperationInProgress;
        boolean remoteEnabled = !localOperationInProgress;

        localRegistryCheckBox.setEnabled(!localOperationInProgress);
        startLocalButton.setEnabled(controlsEnabled);
        stopLocalButton.setEnabled(controlsEnabled);
        openLocalDirButton.setEnabled(controlsEnabled && localRegistryRunning);

        serverAddrField.setEnabled(remoteEnabled);
        usernameField.setEnabled(remoteEnabled);
        passwordField.setEnabled(remoteEnabled);
        testConnectionButton.setEnabled(remoteEnabled);
        connectionStatusLabel.setEnabled(remoteEnabled);
        globalAdminCheckBox.setEnabled(remoteEnabled);
    }

    private void startLocalNacos() {
        if (localOperationInProgress) {
            return;
        }
        setLocalOperationInProgress(true);
        startLocalButton.setText(NacosBundle.message("settings.nacos.local.starting"));
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
                    bindLocalOperation(LocalNacosService.getInstance().startLocalRegistry(),
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
        bindLocalOperation(LocalNacosService.getInstance().stopLocalRegistry(),
                           () -> {
                               if (updateButtonLabel) {
                                   stopLocalButton.setText(NacosBundle.message("settings.nacos.local.stop"));
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
        localStatusIndicator.setText(NacosBundle.message("settings.nacos.local.status.checking"));
        localStatusIndicator.setForeground(JBColor.GRAY);
        localStatusLink.setVisible(false);
        CompletableFuture
            .supplyAsync(() -> LocalRegistryManager.localRegistryStarted(LocalRegistry.NACOS),
                         AppExecutorUtil.getAppExecutorService())
            .thenAccept(running -> ApplicationManager.getApplication().invokeLater(() -> updateLocalStatusVisual(running),
                                                                                   ModalityState.any()));
    }

    private void updateLocalStatusVisual(boolean running) {
        this.localRegistryRunning = running;
        if (running) {
            localStatusIndicator.setText(NacosBundle.message("settings.nacos.local.status.running"));
            localStatusIndicator.setForeground(JBColor.GREEN);
            localStatusLink.setVisible(true);
            openLocalDirButton.setEnabled(true);
        } else {
            localStatusIndicator.setText(NacosBundle.message("settings.nacos.local.status.stopped"));
            localStatusIndicator.setForeground(JBColor.RED);
            localStatusLink.setVisible(false);
            openLocalDirButton.setEnabled(false);
        }
    }

    private JPanel buildLocalStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = JBUI.insets(0, 0, JBUI.scale(4), JBUI.scale(8));
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        panel.add(localStatusIndicator, gbc);
        gbc.gridx = 1;
        panel.add(localStatusLink, gbc);
        gbc.gridx = 2;
        panel.add(openLocalDirButton, gbc);
        return panel;
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
}