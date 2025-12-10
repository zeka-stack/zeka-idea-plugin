package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.UIManager;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettingsListener;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;
import icons.AICommonIcons;

/**
 * AI 提供商选择面板
 * <p>
 * 提供 AI 服务提供商的选择功能，显示可用提供商列表，
 * 并在没有可用提供商时显示提示信息和跳转链接。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.5.0
 */
public class AIProviderSelectionPanel {

    /** AI 提供商选择下拉框 */
    private JComboBox<AIProviderConfig> providerComboBox;

    /** 主面板 */
    private JPanel panel;

    /** AI 提供商设置变更监听器 */
    private AIProviderSettingsListener providerSettingsListener;

    /**
     * 构造函数
     */
    public AIProviderSelectionPanel() {
        createPanel();
        registerProviderSettingsListener();
    }

    /**
     * 创建面板
     */
    private void createPanel() {
        // 从 intelli-ai-engine 获取可用服务商列表
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        // 如果没有可用服务商，显示提示信息和跳转链接
        if (aiProviderTypes.isEmpty()) {
            // 创建提示信息面板
            JBLabel warningLabel = new JBLabel(JavadocBundle.message("settings.ai.provider.no.available.warning"));
            // 使用警告颜色（如果系统不支持，则使用默认的警告颜色）
            Color warningColor = UIManager.getColor("Label.warningForeground");
            if (warningColor == null) {
                warningColor = new JBColor(new Color(255, 140, 0), new Color(255, 140, 0)); // 橙色作为警告颜色
            }
            warningLabel.setForeground(warningColor);

            // 创建跳转链接
            HyperlinkLabel linkLabel = new HyperlinkLabel(JavadocBundle.message("settings.ai.provider.open.ai.common.settings"));
            linkLabel.addHyperlinkListener(e -> {
                // 打开 IntelliAI Engine 全局设置页面（应用级配置）
                // 使用 null 作为 parent 参数表示打开应用级（全局）配置，而不是项目级配置
                ShowSettingsUtil.getInstance().editConfigurable(null, EngineContents.PLUGIN_NAME);
            });

            // 创建空的下拉框（禁用状态）
            providerComboBox = new ComboBox<>(new AIProviderConfig[0]);
            providerComboBox.setEnabled(false);

            panel = FormBuilder.createFormBuilder()
                .addComponent(warningLabel)
                .addComponent(linkLabel)
                .addComponent(new JBLabel()) // 空行
                .addLabeledComponent(new JBLabel(JavadocBundle.message("settings.ai.provider") + ":"), providerComboBox)
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

            JBLabel providerLabel = new JBLabel(JavadocBundle.message("settings.ai.provider") + ":");
            JBLabel hintLabel = new JBLabel(JavadocBundle.message("settings.ai.provider.hint"));
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 1f));

            panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(providerLabel, providerComboBox)
                .addComponent(hintLabel)
                .getPanel();
        }

        // TitledBorder titledBorder = BorderFactory.createTitledBorder(
        //     BorderFactory.createEtchedBorder(),
        //     JavadocBundle.message("settings.ai.provider.selection"));
        // PanelUtil.configureTitledBorder(titledBorder);
        // panel.setBorder(titledBorder);


        panel = PanelUtil.createBorderPanel(panel, JavadocBundle.message("settings.ai.provider.selection"));
    }

    /**
     * 获取已验证的 AI 服务提供商类型列表
     *
     * @return 包含已验证 AI 服务提供商类型的列表
     */
    @NotNull
    private static List<AIProviderConfig> getAiProviderTypes() {
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
     */
    @SuppressWarnings("D")
    private void refreshProviderComboBox() {
        if (panel == null) {
            return;
        }

        // 从 intelli-ai-engine 获取可用服务商列表
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

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

