package dev.dong4j.zeka.stack.idea.plugin.common.ui.dialog;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingContext;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingEffort;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingParamStrategy;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingParamStrategyRegistry;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingUiCapability;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingCapability;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingProbeResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.SpacedJBLabel;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 高级参数编辑对话框
 * <p>
 * 复用于两类场景:
 * <ul>
 *   <li>{@link Mode#CONNECTION_TEMPLATE}：测试连接前编辑当前连接的高级参数模板（隐藏备注）</li>
 *   <li>{@link Mode#AVAILABLE_PROVIDER}：可用服务商列表中后置编辑单条配置（含备注）</li>
 * </ul>
 * 服务商与模型名称只读展示, 避免改动身份字段导致 credentialId 不一致.
 * Think 开关经 {@code AIProviderConfig.shouldEnableThinking()} 参与请求体构建.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class AvailableProviderSettingsDialog extends DialogWrapper {

    /**
     * 对话框使用模式
     */
    public enum Mode {
        /** 测试连接前的高级参数模板 */
        CONNECTION_TEMPLATE,
        /** 可用服务商列表条目编辑 */
        AVAILABLE_PROVIDER
    }

    /** 编辑前的配置快照, 用于保留不可编辑字段 */
    private final AIProviderConfig sourceConfig;
    /** 当前对话框模式 */
    private final Mode mode;

    private final JBTextField maxRetriesField = new JBTextField();
    private final JBTextField timeoutField = new JBTextField();
    private final JBTextField maxTokensField = new JBTextField();
    private final JBTextField temperatureField = new JBTextField();
    private final JBTextField topPField = new JBTextField();
    private final JBTextField topKField = new JBTextField();
    private final JBTextField presencePenaltyField = new JBTextField();
    private final JBTextField remarkField = new JBTextField();
    private final JBCheckBox enableThinkingCheckBox =
        new JBCheckBox(AICommonBundle.message("settings.available.providers.enable.thinking"));
    private final ComboBox<EffortOption> thinkingEffortCombo = new ComboBox<>();

    private final ThinkingUiCapability thinkingUiCapability;

    private JPanel centerPanel;

    /**
     * 创建可用服务商设置对话框（含备注）
     *
     * @param parent 父组件, 用于定位模态对话框
     * @param config 待编辑的可用服务商配置, 不会被直接修改
     */
    public AvailableProviderSettingsDialog(@Nullable Component parent, @NotNull AIProviderConfig config) {
        this(parent, config, Mode.AVAILABLE_PROVIDER);
    }

    /**
     * 按指定模式创建高级参数设置对话框
     *
     * @param parent 父组件
     * @param config 待编辑配置副本源
     * @param mode   使用场景
     */
    public AvailableProviderSettingsDialog(@Nullable Component parent,
                                           @NotNull AIProviderConfig config,
                                           @NotNull Mode mode) {
        super(parent, true);
        this.sourceConfig = config.copy();
        this.mode = mode;
        ThinkingParamStrategy thinkingStrategy = ThinkingParamStrategyRegistry.resolve(this.sourceConfig);
        this.thinkingUiCapability = thinkingStrategy.uiCapability(ThinkingContext.from(this.sourceConfig));
        String titleKey = mode == Mode.CONNECTION_TEMPLATE
                          ? "settings.connection.advanced.settings.title"
                          : "settings.available.providers.edit.title";
        setTitle(AICommonBundle.message(titleKey));
        setOKButtonText(AICommonBundle.message("settings.available.providers.edit.ok"));
        setCancelButtonText(AICommonBundle.message("settings.available.providers.edit.cancel"));
        setResizable(true);
        initEffortComboModel();
        initFieldsFromConfig();
        init();
        Dimension preferred = centerPanel.getPreferredSize();
        setSize(Math.max(preferred.width, JBUI.scale(520)), Math.max(preferred.height + JBUI.scale(80), JBUI.scale(420)));
    }

    /**
     * 将源配置填充到表单控件
     * <p>
     * 空值统一回退为默认占位 (如 auto / 数字默认值), 方便用户对照修改.
     */
    private void initFieldsFromConfig() {
        AIRuntimeSettings runtime = sourceConfig.runtimeSettings != null
                                    ? sourceConfig.runtimeSettings
                                    : new AIRuntimeSettings();
        AIModelParameters params = sourceConfig.modelParameters != null
                                   ? sourceConfig.modelParameters
                                   : new AIModelParameters();

        maxRetriesField.setText(String.valueOf(runtime.maxRetries));
        timeoutField.setText(String.valueOf(runtime.timeout));
        maxTokensField.setText(valueOrAuto(AIModelParameters.migrateMaxTokens(params.maxTokens)));
        temperatureField.setText(valueOrAuto(params.temperature));
        topPField.setText(valueOrAuto(params.topP));
        topKField.setText(valueOrAuto(params.topK));
        presencePenaltyField.setText(valueOrAuto(params.presencePenalty));
        remarkField.setText(sourceConfig.remark != null ? sourceConfig.remark : "");
        enableThinkingCheckBox.setSelected(sourceConfig.enableThinking);
        selectEffort(sourceConfig.resolveThinkingEffort());
        applyThinkingCapabilityUiState();
        updateEffortEnabledState();
        enableThinkingCheckBox.addActionListener(e -> updateEffortEnabledState());

        Dimension fieldSize = new Dimension(JBUI.scale(160), maxRetriesField.getPreferredSize().height);
        maxRetriesField.setPreferredSize(fieldSize);
        timeoutField.setPreferredSize(fieldSize);
        maxTokensField.setPreferredSize(fieldSize);
        temperatureField.setPreferredSize(fieldSize);
        topPField.setPreferredSize(fieldSize);
        topKField.setPreferredSize(fieldSize);
        presencePenaltyField.setPreferredSize(fieldSize);
        remarkField.setPreferredSize(new Dimension(JBUI.scale(280), remarkField.getPreferredSize().height));
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        String providerName = sourceConfig.providerType != null
                              ? sourceConfig.providerType.getDisplayName()
                              : AICommonBundle.message("settings.available.providers.unknown");
        String modelName = sourceConfig.modelName != null ? sourceConfig.modelName : "";

        JBLabel providerValue = new JBLabel(providerName);
        JBLabel modelValue = new JBLabel(modelName);
        providerValue.setCopyable(true);
        modelValue.setCopyable(true);

        JBLabel thinkHint = new SpacedJBLabel(AICommonBundle.message(resolveThinkHintKey()));
        thinkHint.setFont(thinkHint.getFont().deriveFont(thinkHint.getFont().getSize() - 1f));
        thinkHint.setForeground(UIManager.getColor("Label.disabledForeground"));

        JBLabel probeLabel = new SpacedJBLabel(resolveProbeDisplayText());
        probeLabel.setFont(probeLabel.getFont().deriveFont(probeLabel.getFont().getSize() - 1f));
        probeLabel.setForeground(UIUtil.getContextHelpForeground());

        JPanel thinkPanel = new JPanel();
        thinkPanel.setLayout(new javax.swing.BoxLayout(thinkPanel, javax.swing.BoxLayout.Y_AXIS));
        thinkPanel.setOpaque(false);
        enableThinkingCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinkHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        probeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinkingEffortCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (thinkingUiCapability.supportsToggle()) {
            thinkPanel.add(enableThinkingCheckBox);
            thinkPanel.add(javax.swing.Box.createVerticalStrut(JBUI.scale(4)));
        }
        thinkPanel.add(thinkHint);
        if (thinkingUiCapability.supportsEffort()) {
            JPanel effortRow = new JPanel(new java.awt.BorderLayout(JBUI.scale(8), 0));
            effortRow.setOpaque(false);
            effortRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            effortRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, thinkingEffortCombo.getPreferredSize().height + JBUI.scale(4)));
            effortRow.add(new SpacedJBLabel(AICommonBundle.message("settings.available.providers.thinking.effort")),
                          java.awt.BorderLayout.WEST);
            thinkingEffortCombo.setPreferredSize(new Dimension(JBUI.scale(160), thinkingEffortCombo.getPreferredSize().height));
            effortRow.add(thinkingEffortCombo, java.awt.BorderLayout.EAST);
            thinkPanel.add(javax.swing.Box.createVerticalStrut(JBUI.scale(6)));
            thinkPanel.add(effortRow);
        }
        thinkPanel.add(javax.swing.Box.createVerticalStrut(JBUI.scale(4)));
        thinkPanel.add(probeLabel);

        boolean showThinkSection = thinkingUiCapability.supportsToggle() || thinkingUiCapability.supportsEffort();
        FormBuilder formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.available.providers.edit.provider")),
                                 providerValue)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.available.providers.edit.model")),
                                 modelValue)
            .addSeparator(JBUI.scale(8))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.max.retries")),
                                 withHint(maxRetriesField, "settings.max.retries.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.timeout")),
                                 withHint(timeoutField, "settings.timeout.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.max.tokens")),
                                 withHint(maxTokensField, "settings.max.tokens.hint"))
            .addSeparator(JBUI.scale(8))
            .addComponent(createSamplingParamsCollapsible());
        if (showThinkSection) {
            formBuilder.addSeparator(JBUI.scale(8)).addComponent(thinkPanel);
        }

        // 模板模式不展示备注: 测试成功加入列表时仍会自动填充时间戳
        if (mode == Mode.AVAILABLE_PROVIDER) {
            formBuilder.addSeparator(JBUI.scale(8))
                .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.available.providers.edit.remark")),
                                     remarkField);
        }

        centerPanel = formBuilder
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
        centerPanel.setBorder(JBUI.Borders.empty(8, 12, 4, 12));
        return centerPanel;
    }

    /**
     * 收集表单内容并返回更新后的配置副本
     * <p>
     * 身份相关字段 (providerType / modelName / baseUrl / credentialId 等) 保持源配置不变,
     * 仅覆盖运行时参数、模型参数、Think 开关; 可用服务商模式还会更新备注.
     *
     * @return 更新后的配置副本
     */
    @NotNull
    public AIProviderConfig getUpdatedConfig() {
        AIProviderConfig updated = sourceConfig.copy();

        if (updated.runtimeSettings == null) {
            updated.runtimeSettings = new AIRuntimeSettings();
        }
        updated.runtimeSettings.maxRetries = parseIntOrDefault(maxRetriesField.getText(), updated.runtimeSettings.maxRetries);
        updated.runtimeSettings.timeout = Math.max(1, Math.min(600,
                                                               parseIntOrDefault(timeoutField.getText(), updated.runtimeSettings.timeout)));

        if (updated.modelParameters == null) {
            updated.modelParameters = new AIModelParameters();
        }
        updated.modelParameters.maxTokens = normalizeAutoable(maxTokensField.getText());
        updated.modelParameters.temperature = normalizeAutoable(temperatureField.getText());
        updated.modelParameters.topP = normalizeAutoable(topPField.getText());
        updated.modelParameters.topK = normalizeAutoable(topKField.getText());
        updated.modelParameters.presencePenalty = normalizeAutoable(presencePenaltyField.getText());

        if (thinkingUiCapability.supportsToggle()) {
            updated.enableThinking = enableThinkingCheckBox.isSelected();
            // 禁用勾选时仍按探测结论落盘, 避免 UI 状态与请求策略不一致
            ThinkingCapability capability = updated.thinkingProbeResult != null
                                            ? updated.thinkingProbeResult.capability
                                            : null;
            if (capability == ThinkingCapability.REQUIRED_TRUE) {
                updated.enableThinking = true;
            } else if (capability == ThinkingCapability.UNSUPPORTED) {
                updated.enableThinking = false;
            }
        }
        if (thinkingUiCapability.supportsEffort()) {
            EffortOption selected = (EffortOption) thinkingEffortCombo.getSelectedItem();
            updated.thinkingEffort = selected != null ? selected.effort().name() : ThinkingEffort.AUTO.name();
        }
        if (mode == Mode.AVAILABLE_PROVIDER) {
            updated.remark = remarkField.getText() != null ? remarkField.getText().trim() : "";
        }
        return updated;
    }

    /**
     * 初始化思考强度下拉
     */
    private void initEffortComboModel() {
        DefaultComboBoxModel<EffortOption> model = new DefaultComboBoxModel<>();
        model.addElement(new EffortOption(ThinkingEffort.AUTO,
                                          AICommonBundle.message("settings.available.providers.thinking.effort.auto")));
        for (ThinkingEffort effort : thinkingUiCapability.allowedEfforts()) {
            model.addElement(new EffortOption(effort, effortDisplayLabel(effort)));
        }
        thinkingEffortCombo.setModel(model);
    }

    private void selectEffort(@NotNull ThinkingEffort effort) {
        for (int i = 0; i < thinkingEffortCombo.getItemCount(); i++) {
            EffortOption option = thinkingEffortCombo.getItemAt(i);
            if (option != null && option.effort() == effort) {
                thinkingEffortCombo.setSelectedIndex(i);
                return;
            }
        }
        thinkingEffortCombo.setSelectedIndex(0);
    }

    private void updateEffortEnabledState() {
        if (!thinkingUiCapability.supportsEffort()) {
            return;
        }
        // 仅强度（如 Kimi K3）始终可调；有开关时随勾选启用
        if (!thinkingUiCapability.supportsToggle()) {
            thinkingEffortCombo.setEnabled(true);
            return;
        }
        thinkingEffortCombo.setEnabled(enableThinkingCheckBox.isEnabled() && enableThinkingCheckBox.isSelected());
    }

    @NotNull
    private String resolveThinkHintKey() {
        if (!thinkingUiCapability.supportsToggle() && thinkingUiCapability.supportsEffort()) {
            return "settings.available.providers.enable.thinking.hint.effort.only";
        }
        if (thinkingUiCapability.supportsEffort() && thinkingUiCapability.supportsProbe()) {
            return "settings.available.providers.enable.thinking.hint.qianwen";
        }
        if (thinkingUiCapability.supportsEffort()) {
            return "settings.available.providers.enable.thinking.hint.thinking.type";
        }
        return "settings.available.providers.enable.thinking.hint";
    }

    /**
     * 按探测结论调整 Think 勾选可用性
     */
    private void applyThinkingCapabilityUiState() {
        if (!thinkingUiCapability.supportsToggle()) {
            return;
        }
        ThinkingProbeResult probe = sourceConfig.thinkingProbeResult;
        ThinkingCapability capability = probe != null ? probe.capability : null;
        if (capability == ThinkingCapability.UNSUPPORTED) {
            enableThinkingCheckBox.setSelected(false);
            enableThinkingCheckBox.setEnabled(false);
        } else if (capability == ThinkingCapability.REQUIRED_TRUE) {
            enableThinkingCheckBox.setSelected(true);
            enableThinkingCheckBox.setEnabled(false);
        } else {
            enableThinkingCheckBox.setEnabled(true);
        }
    }

    @NotNull
    private String resolveProbeDisplayText() {
        ThinkingProbeResult probe = sourceConfig.thinkingProbeResult;
        if (probe == null || probe.capability == null) {
            String noneKey = thinkingUiCapability.supportsProbe()
                             ? "settings.available.providers.thinking.probe.none"
                             : "settings.available.providers.thinking.probe.official";
            return AICommonBundle.message("settings.available.providers.thinking.probe.label") + " "
                   + AICommonBundle.message(noneKey);
        }
        return AICommonBundle.message("settings.available.providers.thinking.probe.label") + " "
               + probe.capability.displayLabel();
    }

    @NotNull
    private static String effortDisplayLabel(@NotNull ThinkingEffort effort) {
        return switch (effort) {
            case LOW -> AICommonBundle.message("settings.available.providers.thinking.effort.low");
            case HIGH -> AICommonBundle.message("settings.available.providers.thinking.effort.high");
            case MAX -> AICommonBundle.message("settings.available.providers.thinking.effort.max");
            case AUTO -> AICommonBundle.message("settings.available.providers.thinking.effort.auto");
        };
    }

    /**
     * 思考强度下拉项
     */
    private record EffortOption(@NotNull ThinkingEffort effort, @NotNull String label) {
        @Override
        public @NotNull String toString() {
            return label;
        }
    }

    /**
     * 采样参数折叠区（Temperature / Top-P / Top-K / Presence Penalty）
     * <p>
     * 日常使用较少且思考模式下常被服务端忽略，默认收起以缩短对话框。
     *
     * @return 可折叠面板
     */
    @NotNull
    private JPanel createSamplingParamsCollapsible() {
        String title = AICommonBundle.message("settings.available.providers.sampling.params");
        JPanel fields = FormBuilder.createFormBuilder()
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.temperature")),
                                 withHint(temperatureField, "settings.temperature.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.top.p")),
                                 withHint(topPField, "settings.top.p.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.top.k")),
                                 withHint(topKField, "settings.top.k.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.presence.penalty")),
                                 withHint(presencePenaltyField, "settings.presence.penalty.hint"))
            .getPanel();
        fields.setOpaque(false);
        fields.setVisible(false);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(fields, BorderLayout.NORTH);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(true);
        container.setBackground(UIUtil.getPanelBackground());
        container.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        container.add(contentWrapper, BorderLayout.CENTER);
        applyCollapsibleBorder(container, title, false);

        container.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean expanding = !fields.isVisible();
                fields.setVisible(expanding);
                applyCollapsibleBorder(container, title, expanding);
                container.revalidate();
                container.repaint();
                // 展开/收起后同步调整对话框高度，避免裁切或留白
                Window window = SwingUtilities.getWindowAncestor(container);
                if (window != null) {
                    window.pack();
                }
            }
        });
        return container;
    }

    /**
     * 更新折叠标题边框箭头
     */
    private static void applyCollapsibleBorder(@NotNull JPanel container, @NotNull String title, boolean expanded) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder((expanded ? "▼ " : "▶ ") + title);
        titledBorder.setTitleFont(UIUtil.getLabelFont());
        titledBorder.setTitleColor(UIUtil.getLabelForeground());
        container.setBorder(BorderFactory.createCompoundBorder(titledBorder, JBUI.Borders.empty(5)));
    }

    /**
     * 创建「提示 + 输入框」行: 提示在左, 输入框靠右.
     */
    @NotNull
    private static JPanel withHint(@NotNull JBTextField field, @NotNull String hintKey) {
        JPanel panel = new JPanel(new java.awt.BorderLayout(JBUI.scale(8), 0));
        panel.setOpaque(false);

        JBLabel hintLabel = new SpacedJBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2f));
        hintLabel.setForeground(UIUtil.getContextHelpForeground());
        panel.add(hintLabel, java.awt.BorderLayout.CENTER);
        panel.add(field, java.awt.BorderLayout.EAST);
        return panel;
    }

    @NotNull
    private static String valueOrAuto(@Nullable String value) {
        return value == null || value.isBlank() ? "auto" : value;
    }

    @NotNull
    private static String normalizeAutoable(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "auto";
        }
        return value.trim();
    }

    private static int parseIntOrDefault(@Nullable String text, int defaultValue) {
        if (text == null || text.isBlank() || "auto".equalsIgnoreCase(text.trim())) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
