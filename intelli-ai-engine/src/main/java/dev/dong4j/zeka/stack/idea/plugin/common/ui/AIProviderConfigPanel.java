package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ItemEvent;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * AI 提供商配置面板类
 * <p>
 * 该类提供了一个完整的 AI 服务提供商配置界面, 用于管理 AI 服务提供商的连接配置,
 * API 密钥管理, 可用提供商列表以及高级设置等功能. 通过该面板用户可以配置
 * 不同的 AI 服务提供商, 测试连接, 刷新模型列表等操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class AIProviderConfigPanel {

    /** AI 提供商配置界面 UI 组件, 用于渲染和管理用户界面元素 */
    private final AIProviderConfigUI ui;
    /** AI 提供者配置控制器, 负责管理配置面板的业务逻辑和数据流 */
    private final AIProviderConfigController controller;
    /** 是否已设置监听器, 用于防止重复添加监听器 */
    private boolean listenersSetup = false;

    /**
     * 初始化 AI 提供者配置面板
     *
     * @param credentialManager 凭证管理器，用于管理 AI 相关的凭证信息，不能为空
     */
    public AIProviderConfigPanel(@NotNull AICredentialManager credentialManager) {
        this.ui = new AIProviderConfigUI();
        this.controller = new AIProviderConfigController(credentialManager, ui);

        // 创建 UI，传入回调函数
        // 清除所有可用提供者的回调
        ui.createUI(
            () -> {
                // 移除可用提供者的回调
                int selected = ui.getSelectedAvailableProviderRow();
                if (selected >= 0) {
                    controller.removeAvailableProvider(selected);
                }
            },
            controller::clearAllAvailableProviders
                   );

        // UI 创建完成后，初始化 Agent 面板
        controller.initAgentPanel();

        setupListeners();
    }

    /**
     * 获取主面板组件
     *
     * @return 主面板组件
     */
    @NotNull
    public JPanel getPanel() {
        return ui.getMainPanel();
    }

    /**
     * 加载 AI 提供商设置
     *
     * @param settings 要加载的 AI 提供商设置，不能为空
     */
    public void loadSettings(@NotNull AIProviderSettings settings) {
        controller.loadSettings(settings);
    }

    /**
     * 获取 AI 提供者的配置设置
     *
     * @return 更新后的 AI 提供者配置设置对象
     */
    @NotNull
    public AIProviderSettings getSettings() {
        return controller.getSettings();
    }

    /**
     * 检查当前设置是否与基准设置不同
     *
     * @param baseline 用于比较的基准设置
     * @return 如果当前设置与基准设置不同则返回 true，否则返回 false
     */
    public boolean isModified(@NotNull AIProviderSettings baseline) {
        return controller.isModified(baseline);
    }

    /**
     * 检查是否允许进行下一步编辑操作
     * <p> 该方法委托给控制器的 isNextEditEnabled 方法, 用于判断当前配置状态是否满足继续编辑的条件.
     *
     * @return 如果允许进行下一步编辑则返回 true, 否则返回 false
     */
    public boolean isNextEditEnabled() {
        return controller.isNextEditEnabled();
    }

    /**
     * 初始化各种监听器，用于响应用户界面组件的事件
     */
    private void setupListeners() {
        if (listenersSetup) {
            return; // 防止重复添加监听器
        }
        listenersSetup = true;

        ui.getProviderComboBox().addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                AIProviderType selectedType = ui.getSelectedProviderType();
                if (selectedType == null) {
                    return;
                }
                // 处理切换之后的提供者信息
                controller.updateBasicConnectionInfo();
                controller.loadDefaultProviderConfig();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                // 保存切换之前的提供者信息
                AIProviderType previousType = null;
                Object item = e.getItem();
                if (item instanceof AIProviderConfigUI.ProviderOption option && !option.isGroup()) {
                    previousType = option.getProviderType();
                }
                if (previousType != null) {
                    controller.saveCurrentProviderConfig(previousType);
                }
            }
        });

        ui.getShowAvailableProvidersCheckBox().addActionListener(e -> {
            boolean selected = ui.getShowAvailableProvidersCheckBox().isSelected();
            ui.getAvailableProvidersPanel().setVisible(selected);
            if (ui.getAvailableProvidersDescriptionLabel() != null) {
                ui.getAvailableProvidersDescriptionLabel().setVisible(selected);
            }
        });

        ui.getShowAdvancedSettingsCheckBox().addActionListener(e -> {
            JPanel advancedPanel = ui.getAdvancedSettingsContentPanel();
            if (advancedPanel != null) {
                advancedPanel.setVisible(ui.getShowAdvancedSettingsCheckBox().isSelected());
            }
        });

        ui.getTestConnectionButton().addActionListener(e -> controller.testConnection());
        ui.getRefreshModelsButton().addActionListener(e -> controller.refreshModels());
        ui.getAgentPanel().getDownloadButton().addActionListener(e -> controller.downloadAgentJar());
        ui.getAgentPanel().getStartButton().addActionListener(e -> controller.toggleAgent());
    }
}
