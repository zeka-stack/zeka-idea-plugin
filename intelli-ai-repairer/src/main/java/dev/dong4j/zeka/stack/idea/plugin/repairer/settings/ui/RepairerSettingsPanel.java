package dev.dong4j.zeka.stack.idea.plugin.repairer.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderSelectionPanel;
import dev.dong4j.zeka.stack.idea.plugin.repairer.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.repairer.util.RepairerBundle;

/**
 * Repairer 设置面板
 * <p>
 * 提供 AI 服务商选择与系统/用户/增强提示词编辑界面，供 Repairer 插件设置页使用。
 * </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class RepairerSettingsPanel {

    /** 主设置面板, 包含所有设置组件 */
    private final JPanel mainPanel;
    /** AI 服务商选择面板 */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;
    /** 显示高级设置复选框 */
    private final JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置面板 */
    private final JPanel advancedSettingsPanel;
    /** 系统提示词文本区域, 用于编辑展示给 AI 模型的系统角色指令. */
    private final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    /**
     * 用户提示文本区域
     * <p> 用于编辑和显示用户提示内容
     */
    private final JBTextArea userPromptTextArea = new JBTextArea(15, 50);
    /** 增强用户提示词文本区域 */
    private final JBTextArea enhancedUserPromptTextArea = new JBTextArea(15, 50);

    /**
     * 构造函数, 初始化 RepairerSettingsPanel 对象
     * <p> 创建并配置各个组件, 包括 AI 服务商选择面板, 高级设置复选框, 高级设置面板等, 并设置监听器
     *
     * @since 1.0.0
     */
    public RepairerSettingsPanel() {
        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            RepairerBundle::message,
            () -> {
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        );

        showAdvancedSettingsCheckBox = new JBCheckBox(RepairerBundle.message("settings.prompt.settings.show"));
        advancedSettingsPanel = new JPanel(new BorderLayout());
        JPanel advancedContent = FormBuilder.createFormBuilder()
            .addComponent(createPromptPanel())
            .getPanel();
        advancedSettingsPanel.add(advancedContent, BorderLayout.NORTH);

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(aiProviderSelectionPanel.getPanel())
            .addSeparator(10)
            .addComponent(showAdvancedSettingsCheckBox)
            .addComponent(advancedSettingsPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
        setupListeners();
    }

    /**
     * 创建提示词配置面板
     * <p> 该面板包含提示词说明标签和提示词选项卡, 用于管理各类提示词模板
     *
     * @return 配置好的提示词面板 {@link JPanel}
     */
    private JPanel createPromptPanel() {
        JPanel content = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + RepairerBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(content, BorderLayout.CENTER);
        TitledBorder border = new TitledBorder(
            javax.swing.BorderFactory.createEtchedBorder(),
            RepairerBundle.message("settings.advanced.settings.prompt.templates"));
        border.setTitleFont(UIManager.getFont("Label.font"));
        border.setTitleColor(UIUtil.getLabelForeground());
        panel.setBorder(border);
        return panel;
    }

    /**
     * 创建提示词选项卡面板
     * <p> 该方法初始化并返回一个包含系统提示词, 用户提示词和增强用户提示词三个选项卡的选项卡面板.
     * 每个选项卡都通过调用 {@link #createPromptTab} 方法创建, 并配置了相应的文本区域, 提示信息和默认值获取逻辑.
     *
     * @return 配置好的提示词选项卡面板 {@link JBTabbedPane}
     */
    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane tabbedPane = new JBTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(600, 400));
        tabbedPane.addTab(RepairerBundle.message("settings.prompt.tab.system"),
                          createPromptTab(systemPromptTextArea, RepairerBundle.message("settings.prompt.system.tooltip"),
                                          SettingsState::getDefaultSystemPrompt));
        tabbedPane.addTab(RepairerBundle.message("settings.prompt.tab.user"),
                          createPromptTab(userPromptTextArea, RepairerBundle.message("settings.prompt.user.tooltip"),
                                          SettingsState::getDefaultUserPromptTemplate));
        tabbedPane.addTab(RepairerBundle.message("settings.prompt.tab.enhanced"),
                          createPromptTab(enhancedUserPromptTextArea, RepairerBundle.message("settings.prompt.enhanced.tooltip"),
                                          SettingsState::getDefaultEnhancedUserPromptTemplate));
        return tabbedPane;
    }

    /**
     * 创建提示词编辑面板
     * <p> 创建一个包含文本区域和重置按钮的面板, 用于编辑系统, 用户或增强提示词.</p>
     *
     * @param textArea        用于输入和显示提示词的文本区域
     * @param tooltip         提示信息, 显示在文本区域上
     * @param defaultSupplier 用于获取默认提示词值的函数
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JBTextArea textArea, String tooltip, java.util.function.Supplier<String> defaultSupplier) {
        JPanel tabPanel = new JPanel(new BorderLayout());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(tooltip);
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));
        tabPanel.add(scrollPane, BorderLayout.CENTER);
        javax.swing.JButton resetButton = new javax.swing.JButton(RepairerBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> textArea.setText(defaultSupplier.get()));
        tabPanel.add(resetButton, BorderLayout.SOUTH);
        return tabPanel;
    }

    /**
     * 设置高级设置面板的可见性监听器
     * <p> 为 "显示高级设置" 复选框添加动作监听器, 当复选框状态改变时, 控制高级设置面板的可见性.</p>
     *
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e ->
                                                           advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected()));
    }

    /**
     * 获取主设置面板
     * <p> 返回当前 Repairer 设置面板的主容器面板, 用于在 UI 中展示所有设置组件.</p>
     *
     * @return 主设置面板, 类型为 {@code JPanel}
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 判断设置是否已修改
     * <p> 比较当前设置面板中的值与传入的设置状态, 检查是否有任何修改.
     *
     * @param settings         当前设置状态
     * @param providerSettings AI 服务提供商配置
     * @return 如果有修改则返回 true, 否则返回 false
     */
    public boolean isModified(@NotNull SettingsState settings, @Nullable AIProviderConfig providerSettings) {
        String sys = settings.systemPrompt != null ? settings.systemPrompt : SettingsState.getDefaultSystemPrompt();
        String user = settings.userPromptTemplate != null ? settings.userPromptTemplate : SettingsState.getDefaultUserPromptTemplate();
        String enhanced = settings.enhancedUserPromptTemplate != null ? settings.enhancedUserPromptTemplate :
                          SettingsState.getDefaultEnhancedUserPromptTemplate();
        if (!systemPromptTextArea.getText().equals(sys)
            || !userPromptTextArea.getText().equals(user)
            || !enhancedUserPromptTextArea.getText().equals(enhanced)
            || showAdvancedSettingsCheckBox.isSelected() != settings.showAdvancedSettings) {
            return true;
        }
        AIProviderConfig selectedConfig = aiProviderSelectionPanel.getSelectedProvider();
        if (selectedConfig == null) {
            return providerSettings != null;
        }
        if (providerSettings == null) {
            return true;
        }
        return !providerSettings.contentEquals(selectedConfig);
    }

    /**
     * 应用当前设置到指定的配置状态对象
     * <p> 将当前界面中输入的系统提示词, 用户提示词, 增强用户提示词, 高级设置显示状态以及选定的 AI 服务提供商配置应用到传入的 SettingsState 对象中 </p>
     *
     * @param settings 需要被应用设置的配置状态对象, 不能为 null
     */
    public void apply(@NotNull SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.userPromptTemplate = userPromptTextArea.getText();
        settings.enhancedUserPromptTemplate = enhancedUserPromptTextArea.getText();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();
        AIProviderConfig selectedConfig = aiProviderSelectionPanel.getSelectedProvider();
        if (selectedConfig != null) {
            settings.providerConfig = selectedConfig.copy();
        }
    }

    /**
     * 重置设置面板到指定状态
     * <p> 根据传入的设置状态对象, 将所有提示词编辑框, 高级设置开关和 AI 服务商选择器恢复到对应值 </p>
     *
     * @param settings 设置状态对象, 包含系统提示词, 用户提示词, 增强提示词, 是否显示高级设置及所选服务商配置
     * @since 1.0.0
     */
    public void reset(@NotNull SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt != null ? settings.systemPrompt : SettingsState.getDefaultSystemPrompt());
        userPromptTextArea.setText(settings.userPromptTemplate != null ? settings.userPromptTemplate :
                                   SettingsState.getDefaultUserPromptTemplate());
        enhancedUserPromptTextArea.setText(settings.enhancedUserPromptTemplate != null ? settings.enhancedUserPromptTemplate :
                                           SettingsState.getDefaultEnhancedUserPromptTemplate());
        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);
        aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
    }

    /**
     * 释放资源
     * <p> 调用 AI 服务商选择面板的释放方法, 释放相关资源
     */
    public void dispose() {
        aiProviderSelectionPanel.dispose();
    }
}
