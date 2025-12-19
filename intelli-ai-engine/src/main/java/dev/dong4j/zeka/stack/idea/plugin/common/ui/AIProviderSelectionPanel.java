package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettingsListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.SpacedJBLabel;
import icons.AICommonIcons;

/**
 * AI 提供商选择面板
 * <p>
 * 提供 AI 服务提供商的选择功能，显示可用提供商列表，
 * 并在没有可用提供商时显示提示信息和跳转链接。
 * 这是一个通用组件，可以被多个插件复用。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.5.0
 */
public class AIProviderSelectionPanel {

    /**
     * 国际化消息提供者接口
     * <p>
     * 用于提供插件特定的国际化消息，每个插件可以实现此接口
     * 来提供自己的消息资源。
     */
    @FunctionalInterface
    public interface MessageProvider {
        /**
         * 获取国际化消息
         *
         * @param key 消息键
         * @return 国际化后的消息文本
         */
        @NotNull
        String message(@NotNull String key);
    }

    /** AI 提供商选择下拉框 */
    private JComboBox<AIProviderConfig> providerComboBox;

    /** 主面板 */
    private JPanel panel;

    /** AI 提供商设置变更监听器 */
    private AIProviderSettingsListener providerSettingsListener;

    /** 国际化消息提供者 */
    private final MessageProvider messageProvider;

    /** 面板刷新后的回调（可选） */
    @Nullable
    private final Runnable onPanelRefreshed;

    /**
     * 构造函数
     *
     * @param messageProvider 国际化消息提供者，用于获取插件特定的消息
     */
    public AIProviderSelectionPanel(@NotNull MessageProvider messageProvider) {
        this(messageProvider, null);
    }

    /**
     * 构造函数
     *
     * @param messageProvider  国际化消息提供者，用于获取插件特定的消息
     * @param onPanelRefreshed 面板刷新后的回调（可选），当面板因提供商列表变化而重建时调用
     */
    public AIProviderSelectionPanel(@NotNull MessageProvider messageProvider,
                                    @Nullable Runnable onPanelRefreshed) {
        this.messageProvider = messageProvider;
        this.onPanelRefreshed = onPanelRefreshed;
        createPanel();
        registerProviderSettingsListener();
    }

    /**
     * 创建面板
     */
    private void createPanel() {
        // 从 intelli-ai-engine 获取可用服务商列表
        final java.util.List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        JPanel contentPanel;

        // 如果没有可用服务商，显示提示信息和跳转链接
        if (aiProviderTypes.isEmpty()) {
            // 创建提示信息面板
            JBLabel warningLabel = new SpacedJBLabel(messageProvider.message("settings.ai.provider.no.available.warning"));
            // 使用警告颜色（如果系统不支持，则使用默认的警告颜色）
            Color warningColor = UIManager.getColor("Label.warningForeground");
            if (warningColor == null) {
                warningColor = new JBColor(new Color(255, 140, 0), new Color(255, 140, 0)); // 橙色作为警告颜色
            }
            warningLabel.setForeground(warningColor);

            // 创建跳转链接
            HyperlinkLabel linkLabel = new HyperlinkLabel(messageProvider.message("settings.ai.provider.open.ai.common.settings"));
            linkLabel.addHyperlinkListener(e -> {
                // 打开 IntelliAI Engine 全局设置页面（应用级配置）
                // 使用 null 作为 parent 参数表示打开应用级（全局）配置，而不是项目级配置
                ShowSettingsUtil.getInstance().editConfigurable(null, EngineContents.PLUGIN_NAME);
            });

            // 创建空的下拉框（禁用状态）
            providerComboBox = new ComboBox<>(new AIProviderConfig[0]);
            providerComboBox.setEnabled(false);

            contentPanel = FormBuilder.createFormBuilder()
                .addComponent(warningLabel)
                .addComponent(linkLabel)
                .addComponent(new JBLabel()) // 空行
                .addLabeledComponent(new SpacedJBLabel(messageProvider.message("settings.ai.provider") + ":"), providerComboBox)
                .getPanel();
        } else {
            // 创建供应商下拉框
            providerComboBox = new ComboBox<>(aiProviderTypes.toArray(new AIProviderConfig[0]));
            providerComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
                JBLabel label = new JBLabel();
                if (value != null) {
                    Icon icon = AICommonIcons.getProviderIcon(value.providerType);
                    label.setIcon(icon);
                    label.setText(value.providerType.getDisplayName() + ":" + value.modelName);
                }
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
                label.setOpaque(true);
                return label;
            });

            JBLabel providerLabel = new SpacedJBLabel(messageProvider.message("settings.ai.provider") + ":");
            JBLabel hintLabel = new SpacedJBLabel(messageProvider.message("settings.ai.provider.hint"));
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 1f));

            contentPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(providerLabel, providerComboBox)
                .addComponent(hintLabel)
                .getPanel();
        }

        // 使用 createPanelWithTitledBorder 创建带边框的面板，确保布局正确
        panel = createPanelWithTitledBorder(contentPanel, messageProvider.message("settings.ai.provider.selection"));
    }

    /**
     * 创建带标题边框的面板
     * <p>
     * 创建一个带标题边框的面板，显式设置字体和颜色以确保在不同 IntelliJ 版本中都能正常显示。
     *
     * @param contentPanel 内容面板
     * @param title        标题文本
     * @return 带标题边框的面板
     */
    private JPanel createPanelWithTitledBorder(@NotNull JPanel contentPanel, @NotNull String title) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(contentPanel, BorderLayout.CENTER);

        // 创建 TitledBorder 并显式设置字体和颜色
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            title
                                                                    );

        // 显式设置字体和颜色，确保在 2025 版本中正常显示
        // 使用 UIUtil 获取主题感知的文本颜色，自动适配浅色和深色主题
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);

        wrapper.setBorder(titledBorder);
        return wrapper;
    }

    /**
     * 获取已验证的 AI 服务提供商类型列表
     *
     * @return 包含已验证 AI 服务提供商类型的列表
     */
    @NotNull
    private static java.util.List<AIProviderConfig> getAiProviderTypes() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getVerifiedProviders();
    }

    /**
     * 注册 AI 提供商设置变更监听器
     */
    private void registerProviderSettingsListener() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        providerSettingsListener = settings -> refreshProviderComboBox();
        globalSettings.addListener(providerSettingsListener);
    }

    /**
     * 刷新提供商下拉框
     * <p>
     * 从 IntelliAI Engine 设置中重新获取可用提供商列表，并更新下拉框内容。
     * 如果之前没有可用提供商，现在有了，会重新创建面板。
     */
    @SuppressWarnings("D")
    private void refreshProviderComboBox() {
        if (panel == null) {
            return;
        }

        // 从 intelli-ai-engine 获取可用服务商列表
        final java.util.List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        // 判断之前是否有可用提供商（通过下拉框是否启用来判断）
        boolean hadProviders = providerComboBox != null && providerComboBox.isEnabled();
        boolean hasProviders = !aiProviderTypes.isEmpty();

        // 如果状态没有变化，只需要更新下拉框内容
        if (hadProviders && hasProviders) {
            // 保存当前选中的值
            AIProviderConfig selectedValue = (AIProviderConfig) providerComboBox.getSelectedItem();

            // 更新下拉框模型
            providerComboBox.setModel(new DefaultComboBoxModel<>(aiProviderTypes.toArray(new AIProviderConfig[0])));

            // 恢复之前选中的值（如果还存在）
            if (selectedValue != null && aiProviderTypes.contains(selectedValue)) {
                providerComboBox.setSelectedItem(selectedValue);
            } else if (!aiProviderTypes.isEmpty()) {
                // 如果之前选中的值不存在了，选择第一个
                providerComboBox.setSelectedIndex(0);
            }
            return;
        }

        // 如果状态发生变化（从无到有，或从有到无），需要重新创建整个面板
        JPanel parent = (JPanel) panel.getParent();
        if (parent != null) {
            int index = -1;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                if (parent.getComponent(i) == panel) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                parent.remove(index);
                createPanel();
                parent.add(panel, index);
                parent.revalidate();
                parent.repaint();

                // 执行刷新后的回调（如果提供）
                if (onPanelRefreshed != null) {
                    onPanelRefreshed.run();
                }
            }
        }
    }

    /**
     * 获取主面板
     *
     * @return 主面板组件
     */
    @NotNull
    public JPanel getPanel() {
        return panel;
    }

    /**
     * 获取选中的提供商配置
     *
     * @return 选中的提供商配置，如果未选中则返回 null
     */
    @Nullable
    public AIProviderConfig getSelectedProvider() {
        if (providerComboBox == null) {
            return null;
        }
        return (AIProviderConfig) providerComboBox.getSelectedItem();
    }

    /**
     * 设置选中的提供商配置
     *
     * @param providerConfig 要设置的提供商配置
     */
    public void setSelectedProvider(@Nullable AIProviderConfig providerConfig) {
        if (providerComboBox != null) {
            providerComboBox.setSelectedItem(providerConfig);
        }
    }

    /**
     * 释放资源
     * <p>
     * 取消注册监听器，避免内存泄漏。
     * 应该在设置页面关闭时调用。
     */
    public void dispose() {
        if (providerSettingsListener != null) {
            AIProviderSettings globalSettings = AIProviderSettings.getInstance();
            globalSettings.removeListener(providerSettingsListener);
            providerSettingsListener = null;
        }
    }
}

