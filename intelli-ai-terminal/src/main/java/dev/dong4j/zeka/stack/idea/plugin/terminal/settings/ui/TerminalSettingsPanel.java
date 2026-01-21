package dev.dong4j.zeka.stack.idea.plugin.terminal.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderSelectionPanel;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.FeedbackPanel;
import dev.dong4j.zeka.stack.idea.plugin.terminal.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.PromptTemplateVersionStore;
import dev.dong4j.zeka.stack.idea.plugin.terminal.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.terminal.util.TerminalBundle;
import lombok.Getter;

/**
 * 终端设置面板类
 * <p> 该类用于配置终端的相关设置, 包括 AI 提供商选择, 高级设置开关, 触发前缀, 系统提示和终端模板等.
 * 通过提供图形界面组件, 用户可以方便地进行相关设置的修改和应用.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class TerminalSettingsPanel {

    /**
     * 主面板
     * <p> 用于布局和管理设置面板中的各个组件.
     */
    @Getter
    private final JPanel mainPanel;
    /** AI 服务商选择面板 */
    private final AIProviderSelectionPanel aiProviderSelectionPanel;

    // 高级设置
    /** 用于显示高级设置的复选框 */
    private final JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板 (用于控制可见性) */
    private final JPanel advancedSettingsPanel;
    /** 是否启用 Terminal AI 的复选框 */
    private final JBCheckBox enableTerminalAICheckBox;
    /** 是否启用流式输出 */
    private final JBCheckBox enableStreamResponseCheckBox;
    /** 是否启用上下文检测 */
    private final JBCheckBox enableTerminalContextCheckBox;
    /** 触发前缀下拉框, 用于设置终端命令触发前缀 */
    private final ComboBox<String> triggerPrefixField;

    // Prompt 配置
    /** 系统提示文本区域, 用于输入和编辑系统级别的提示词内容 */
    public final JBTextArea systemPromptTextArea = new JBTextArea(15, 50);
    /** 终端模板文本区域 */
    public final JBTextArea terminalTemplateTextArea = new JBTextArea(15, 50);

    /**
     * 构造函数, 初始化设置面板
     * <p> 创建并配置插件设置面板的主界面, 包括 AI 服务商选择, 高级设置选项, 系统提示和终端模板输入区域等
     * <p> 初始化过程中会创建并设置以下组件:
     * <ul>
     * <li>{@link JBCheckBox}: 显示高级设置的复选框 </li>
     * <li>{@link JPanel}: 高级设置内容容器面板 </li>
     * <li>{@link AIProviderSelectionPanel}:AI 服务商选择面板 </li>
     * <li>{@link JBTextArea}: 系统提示和终端模板文本区域 </li>
     * <li>{@link FeedbackPanel}: 反馈面板 </li>
     * </ul>
     * <p> 所有组件通过 FormBuilder 进行布局, 并设置适当的边距和分隔符
     *
     * @since 1.0.0
     */
    public TerminalSettingsPanel() {
        showAdvancedSettingsCheckBox = new JBCheckBox(TerminalBundle.message("settings.prompt.settings.show"));
        enableTerminalAICheckBox = new JBCheckBox(TerminalBundle.message("settings.terminal.enable"));
        enableStreamResponseCheckBox = new JBCheckBox(TerminalBundle.message("settings.terminal.stream.enable"));
        enableTerminalContextCheckBox = new JBCheckBox(TerminalBundle.message("settings.terminal.context.enable"));
        triggerPrefixField = new ComboBox<>(new String[] {"#", "::", "??"});
        triggerPrefixField.setEditable(true);

        advancedSettingsPanel = new JPanel(new BorderLayout());
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            .addComponent(createPromptTemplatesPanel())
            .getPanel();
        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            TerminalBundle::message,
            () -> {
                SettingsState settings = SettingsState.getInstance();
                reset(settings);
            }
        );

        FeedbackPanel feedbackPanel = new FeedbackPanel(
            null,
            PluginContents.PLUGIN_ID,
            PluginContents.PLUGIN_NAME,
            "zeka-stack-terminal-plugin"
        );

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(createLogoPanel())
            .addComponent(aiProviderSelectionPanel.getPanel())
            .addComponent(createGifPanel())
            .addSeparator(10)
            .addComponent(enableTerminalAICheckBox)
            .addComponent(enableStreamResponseCheckBox)
            .addComponent(enableTerminalContextCheckBox)
            .addLabeledComponent(TerminalBundle.message("settings.terminal.trigger.prefix"), triggerPrefixField)
            .addComponent(showAdvancedSettingsCheckBox)
            .addComponent(advancedSettingsPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .addComponent(feedbackPanel.getContent())
            .getPanel();

        mainPanel.setMinimumSize(new Dimension(JBUI.scale(630), 0));
        mainPanel.setBorder(JBUI.Borders.empty(10));
        setupListeners();
    }

    /**
     * 判断当前设置是否与给定的设置状态发生修改
     * <p>比较界面上的配置项 (如系统提示词, 终端模板, 高级设置可见性,AI 服务提供商配置等) 与传入的 SettingsState 实例, 以确定是否有变更.
     * <p>如果 AI 服务商配置为空或不匹配, 也认为设置已修改.
     *
     * @param settings         需要比较的设置状态对象, 不能为 null
     * @param providerSettings 当前选中的 AI 提供商配置, 可能为 null
     * @return 如果界面设置与指定的设置状态不同, 则返回 true; 否则返回 false
     * @since 1.0.0
     */
    public boolean isModified(@NotNull SettingsState settings, @Nullable AIProviderConfig providerSettings) {
        if (!systemPromptTextArea.getText().equals(settings.systemPrompt)
            || !terminalTemplateTextArea.getText().equals(settings.terminalTemplate)
            || showAdvancedSettingsCheckBox.isSelected() != settings.showPromptSettings
            || enableTerminalAICheckBox.isSelected() != settings.enableTerminalAI
            || enableStreamResponseCheckBox.isSelected() != settings.enableStreamResponse
            || enableTerminalContextCheckBox.isSelected() != settings.enableTerminalContext
            || !getSelectedTriggerPrefix().equals(settings.triggerPrefix)) {
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
     * 将界面中的设置项应用到指定的 SettingsState 对象中
     * <p> 该方法用于将当前设置面板中配置的系统提示词, 终端模板, 高级设置可见性, 终端 AI 启用状态, 触发前缀以及 AI 提供商配置等应用到给定的 SettingsState 对象中, 实现界面与数据模型的同步
     * <p> 如果当前使用默认提示词模板, 还会自动更新提示词模板版本号
     *
     * @param settings 需要应用设置的 SettingsState 对象, 不能为 null
     */
    public void apply(@NotNull SettingsState settings) {
        settings.systemPrompt = systemPromptTextArea.getText();
        settings.terminalTemplate = terminalTemplateTextArea.getText();
        settings.showPromptSettings = showAdvancedSettingsCheckBox.isSelected();
        settings.enableTerminalAI = enableTerminalAICheckBox.isSelected();
        settings.enableStreamResponse = enableStreamResponseCheckBox.isSelected();
        settings.enableTerminalContext = enableTerminalContextCheckBox.isSelected();
        settings.triggerPrefix = getSelectedTriggerPrefix();
        if (settings.isUsingDefaultPrompts()) {
            settings.promptTemplateVersion = SettingsState.PROMPT_TEMPLATE_VERSION;
            settings.promptTemplateNoticeVersion = SettingsState.PROMPT_TEMPLATE_VERSION;
            PromptTemplateVersionStore.setPromptTemplateVersion(SettingsState.PROMPT_TEMPLATE_VERSION);
            PromptTemplateVersionStore.setPromptTemplateNoticeVersion(SettingsState.PROMPT_TEMPLATE_VERSION);
        }

        AIProviderConfig selectedConfig = aiProviderSelectionPanel.getSelectedProvider();
        if (selectedConfig != null) {
            settings.providerConfig = selectedConfig.copy();
        }
    }

    /**
     * 将界面设置重置为指定的配置状态
     * <p> 该方法会根据给定的 SettingsState 对象, 更新界面中的各个组件状态, 使其与配置一致
     *
     * @param settings 包含当前设置状态的 SettingsState 对象, 不能为 null
     */
    public void reset(@NotNull SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPrompt);
        terminalTemplateTextArea.setText(settings.terminalTemplate);
        showAdvancedSettingsCheckBox.setSelected(settings.showPromptSettings);
        enableTerminalAICheckBox.setSelected(settings.enableTerminalAI);
        enableStreamResponseCheckBox.setSelected(settings.enableStreamResponse);
        enableTerminalContextCheckBox.setSelected(settings.enableTerminalContext);
        triggerPrefixField.setSelectedItem(settings.triggerPrefix);
        advancedSettingsPanel.setVisible(settings.showPromptSettings);
        aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
    }

    /**
     * 为高级设置复选框添加事件监听器,
     * 当复选框状态改变时根据其选中状态更新高级设置面板的可见性.
     */
    private void setupListeners() {
        showAdvancedSettingsCheckBox.addActionListener(e ->
                                                           advancedSettingsPanel.setVisible(showAdvancedSettingsCheckBox.isSelected()));
    }

    /**
     * 创建提示词模板面板
     * <p> 用于生成包含提示词模板内容的面板, 包含标签和提示词模板 Tab 页面板.
     *
     * @return 包含提示词模板内容的面板
     * @since 1.0.0
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + TerminalBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        TitledBorder titledBorder = javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createEtchedBorder(),
            TerminalBundle.message("settings.advanced.settings.prompt.templates"));
        configureTitledBorder(titledBorder);
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建包含 GIF 图片的面板
     * <p> 该方法用于生成一个居中显示 GIF 图片的面板, 图片路径为 "sample.gif", 若资源不存在则返回空面板.
     * <p> 图片会根据指定宽度进行缩放, 保持原始宽高比, 确保显示效果适配界面.
     *
     * @return 包含缩放后 GIF 图片的面板, 若资源不存在则返回空面板
     */
    private JPanel createGifPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        URL gifUrl = TerminalSettingsPanel.class.getClassLoader().getResource("sample.gif");
        if (gifUrl == null) {
            return panel;
        }
        ImageIcon icon = new ImageIcon(gifUrl);
        int targetWidth = JBUI.scale(630);
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        if (width > 0 && height > 0 && width > targetWidth) {
            int targetHeight = (int) ((long) height * targetWidth / width);
            Image scaled = icon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
            icon = new ImageIcon(scaled);
        }
        RoundedGifLabel label = new RoundedGifLabel(icon, JBUI.scale(10));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        return panel;
    }

    /**
     * 创建包含 Logo 图片的面板
     * <p> Logo 位于设置页顶部, 左对齐显示, 并保持等比例缩放与圆角效果.</p>
     *
     * @return 包含 Logo 图片的面板, 若资源不存在则返回空面板
     */
    private JPanel createLogoPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        URL logoUrl = TerminalSettingsPanel.class.getClassLoader().getResource("logo.png");
        if (logoUrl == null) {
            return panel;
        }
        ImageIcon icon = new ImageIcon(logoUrl);
        int targetWidth = JBUI.scale(630);
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();
        if (width > 0 && height > 0 && width > targetWidth) {
            int targetHeight = (int) ((long) height * targetWidth / width);
            Image scaled = icon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
            icon = new ImageIcon(scaled);
        }
        RoundedGifLabel label = new RoundedGifLabel(icon, JBUI.scale(10));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        panel.add(label);
        return panel;
    }

    private static final class RoundedGifLabel extends JBLabel {
        /** 圆角半径值, 用于绘制圆角矩形背景 */
        private final int radius;

        /**
         * 初始化圆角 GIF 标签组件
         * <p> 创建一个带有指定图标和圆角半径的标签组件, 设置为非透明背景, 确保圆角区域显示图标内容
         *
         * @param icon   图标对象, 不能为空
         * @param radius 圆角半径, 最小值为 0, 负值将被截断为 0
         */
        private RoundedGifLabel(@NotNull ImageIcon icon, int radius) {
            super(icon);
            this.radius = Math.max(0, radius);
            setOpaque(false);
        }

        /**
         * 重绘组件内容, 绘制圆角背景内的图标
         * <p> 在绘制前先裁剪为圆角矩形区域, 然后绘制图标. 若图标为空则调用父类绘制方法.
         *
         * @param g 绘制上下文
         */
        @Override
        protected void paintComponent(Graphics g) {
            ImageIcon icon = (ImageIcon) getIcon();
            if (icon == null) {
                super.paintComponent(g);
                return;
            }
            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(new RoundRectangle2D.Float(0, 0, w, h, radius, radius));
            icon.paintIcon(this, g2, 0, 0);
            g2.dispose();
        }

        /**
         * 获取组件的首选尺寸
         * <p> 当图标不为空时, 返回图标的宽度和高度作为尺寸; 否则返回父类的默认尺寸
         *
         * @return 组件的首选尺寸, 若图标为空则返回父类计算的尺寸
         */
        @Override
        public Dimension getPreferredSize() {
            ImageIcon icon = (ImageIcon) getIcon();
            return icon == null ? super.getPreferredSize() : new Dimension(icon.getIconWidth(), icon.getIconHeight());
        }
    }

    /**
     * 创建提示词模板的 Tab 页面板
     * <p> 用于创建包含系统提示词和终端模板的标签页面板, 每个标签页内包含文本区域和重置按钮.
     *
     * @return 包含提示词内容和重置按钮的标签页面板
     */
    private JBTabbedPane createPromptTabbedPane() {
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        promptTabbedPane.addTab(TerminalBundle.message("settings.prompt.tab.system"),
                                createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(TerminalBundle.message("settings.prompt.tab.user"),
                                createPromptTab(terminalTemplateTextArea, "user"));

        return promptTabbedPane;
    }

    /**
     * 创建提示词编辑标签页面板
     * <p> 用于生成包含文本区域和重置按钮的面板, 支持换行和单词换行, 适用于编辑系统提示或终端模板等提示信息.
     * <p> 文本区域设置为垂直滚动, 水平不滚动, 并添加适当边距; 底部放置重置按钮, 点击后可将内容恢复为默认值.
     *
     * @param textArea   用于输入提示信息的文本区域, 不能为 null
     * @param promptType 提示信息的类型, 如 "system" 或 "terminal"
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JBTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(TerminalBundle.message("settings.prompt." + promptType + ".tooltip"));

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        JButton resetButton = new JButton(TerminalBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        return tabPanel;
    }

    /**
     * 将指定类型的提示词重置为其默认值
     * <p> 根据给定的提示词类型, 将对应的文本区域内容重置为默认的系统提示或终端模板
     *
     * @param promptType 提示词类型, 可以是 "system" 或 "terminal"
     * @param textArea   文本区域对象, 用于显示和编辑提示词内容
     */
    private void resetPromptToDefault(String promptType, JBTextArea textArea) {
        switch (promptType) {
            case "system":
                textArea.setText(SettingsState.getDefaultSystemPrompt());
                break;
            case "user":
                textArea.setText(SettingsState.getDefaultUserPrompt());
                break;
            default:
                break;
        }
    }

    /**
     * 获取当前选中的触发前缀
     * <p> 从触发前缀下拉框的编辑器中获取当前选中项, 若项为 null 则返回空字符串, 否则转换为字符串并去除首尾空格
     *
     * @return 当前选中的触发前缀字符串, 若无选中项则返回空字符串
     */
    private String getSelectedTriggerPrefix() {
        Object item = triggerPrefixField.getEditor().getItem();
        String value = item == null ? "" : item.toString();
        return value.trim();
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p> 设置标题的字体为系统中定义的标签字体, 并设置标题颜色为系统中定义的标签前景色
     *
     * @param titledBorder 要配置的 TitledBorder 对象, 不能为 null
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        java.awt.Color titleColor = com.intellij.util.ui.UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
    }

    /**
     * 释放资源
     * <p> 该方法用于释放面板中占用的资源, 通常在关闭或销毁设置面板时调用.
     */
    public void dispose() {
        aiProviderSelectionPanel.dispose();
    }
}
