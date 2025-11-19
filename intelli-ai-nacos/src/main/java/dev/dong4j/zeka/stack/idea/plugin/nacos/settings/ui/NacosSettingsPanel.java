package dev.dong4j.zeka.stack.idea.plugin.nacos.settings.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

/**
 * Nacos 插件设置面板 UI
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosSettingsPanel {

    /** 主面板 */
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

        // 设置组件提示信息
        serverAddrField.getEmptyText().setText("http://localhost:8848");
        usernameField.getEmptyText().setText("nacos");

        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.server.addr") + ":"), serverAddrField)
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.username") + ":"), usernameField)
            .addLabeledComponent(new JBLabel(NacosBundle.message("settings.nacos.password") + ":"), passwordField)
            .addComponent(testConnectionButton)
            .addComponent(connectionStatusLabel)
            .addComponent(globalAdminCheckBox)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        // 设置边框
        mainPanel.setBorder(JBUI.Borders.empty(10));

        // 设置监听器
        setupListeners();
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        testConnectionButton.addActionListener(e -> testConnection());
    }

    /**
     * 测试 Nacos 连接
     */
    private void testConnection() {
        String serverAddr = serverAddrField.getText();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (serverAddr.isEmpty() || username.isEmpty()) {
            connectionStatusLabel.setText(NacosBundle.message("settings.nacos.connection.failed", "服务器地址和用户名不能为空"));
            connectionStatusLabel.setForeground(JBColor.RED);
            return;
        }

        // 在后台线程中执行连接测试
        new Task.Backgroundable(null, "Testing Nacos Connection", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Connecting to Nacos server...");

                try {
                    // TODO: 实际的 Nacos 连接测试逻辑将在后续实现
                    // 这里模拟连接测试
                    Thread.sleep(2000); // 模拟网络延迟

                    // 模拟连接成功
                    boolean success = true;
                    String message = success ?
                                     NacosBundle.message("settings.nacos.connection.success") :
                                     NacosBundle.message("settings.nacos.connection.failed", "连接超时");

                    // 在 EDT 线程中更新 UI
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (success) {
                            connectionStatusLabel.setText(message);
                            connectionStatusLabel.setForeground(JBColor.GREEN);
                        } else {
                            connectionStatusLabel.setText(message);
                            connectionStatusLabel.setForeground(JBColor.RED);
                        }
                    }, ModalityState.defaultModalityState());
                } catch (Exception e) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        connectionStatusLabel.setText(NacosBundle.message("settings.nacos.connection.failed", e.getMessage()));
                        connectionStatusLabel.setForeground(JBColor.RED);
                    }, ModalityState.defaultModalityState());
                }
            }
        }.queue();
    }

    /**
     * 获取主面板
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     */
    public boolean isModified(SettingsState settings) {
        return !serverAddrField.getText().equals(settings.serverAddr)
               || !usernameField.getText().equals(settings.username)
               || globalAdminCheckBox.isSelected() != settings.globalAdmin;
        // 注意：密码字段不参与比较，因为不会从设置状态中读取密码
    }

    /**
     * 将界面中的设置项应用到给定的 SettingsState 对象中
     */
    public void apply(SettingsState settings) {
        settings.serverAddr = serverAddrField.getText();
        settings.username = usernameField.getText();
        settings.globalAdmin = globalAdminCheckBox.isSelected();

        // 保存密码到 CredentialStore
        String password = new String(passwordField.getPassword());
        if (!password.isEmpty()) {
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

        // 从 CredentialStore 获取密码（仅用于测试连接，不在界面中显示）
        // 注意：出于安全考虑，我们不在界面中显示已保存的密码
    }

    /**
     * 释放资源
     */
    public void dispose() {
        // 清理资源
    }
}