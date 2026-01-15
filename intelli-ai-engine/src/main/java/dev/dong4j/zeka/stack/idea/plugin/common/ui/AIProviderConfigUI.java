package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TextFieldWithAutoCompletion;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ResponseLanguage;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.SpacedJBLabel;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.StatusIndicatorButton;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import icons.AICommonIcons;

/**
 * AI 提供商配置 UI 类
 * <p>
 * 该类负责创建和管理 AI 服务提供商的配置界面, 包括提供商选择, 模型配置,API 密钥设置,
 * 连接测试, 可用提供商管理以及高级参数配置等功能. 提供完整的 UI 组件和交互逻辑,
 * 支持多种 AI 服务提供商的配置和管理.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class AIProviderConfigUI {
    /** 主界面主面板, 用于承载主要功能组件和布局 */
    private JPanel mainPanel;
    /** 服务提供商下拉选择框 */
    private ComboBox<String> providerComboBox;
    /** 获取 API Key 的超链接 */
    private HyperlinkLabel getApiKeyLink;
    /** 模型下拉选择框 */
    private ComboBox<String> modelComboBox;
    /** 模型智能搜索输入框 */
    private TextFieldWithAutoCompletion<String> modelSearchField;
    /** 模型下拉框的完整数据源 */
    private final List<String> modelItems = new ArrayList<>();
    /** 模型过滤更新标记 */
    private boolean suppressModelFilter;
    /** 模型过滤监听是否已安装 */
    private boolean modelFilterInstalled;
    /** 基础 URL 输入框 */
    private JBTextField baseUrlField;
    /** API 密钥输入框 */
    private JBPasswordField apiKeyField;
    /** 测试连接按钮 */
    private StatusIndicatorButton testConnectionButton;
    /** 刷新模型列表的按钮 */
    private StatusIndicatorButton refreshModelsButton;
    /** 显示可用提供者的复选框 */
    private JBCheckBox showAvailableProvidersCheckBox;
    /** 可用提供者面板, 用于展示和管理可用 AI 服务提供商的列表及其操作 */
    private JPanel availableProvidersPanel;
    /** 可用提供者的描述标签 */
    private JBLabel availableProvidersDescriptionLabel;
    /** 可用提供者表格组件, 用于展示和选择可用的提供者列表 */
    private JBTable availableProvidersTable;
    /** 可用提供者表格模型 */
    private AvailableProvidersTableModel availableProvidersTableModel;
    /** Autocomplete 默认服务商下拉框 */
    private ComboBox<AIProviderConfig> autocompleteProviderComboBox;
    /** 控制日志详细输出的复选框 */
    private JBCheckBox verboseLoggingCheckBox;
    /** 控制是否启用自动更新检查的复选框 */
    private JBCheckBox lastUpdateCheckCheckBox;
    /** 控制是否显示新版本通知的复选框 */
    private JBCheckBox showUpdateNotificationCheckBox;
    /** 控制是否启用下一步建议 */
    private JBCheckBox nextEditEnabledCheckBox;
    /** 注释语言选择下拉框 */
    private ComboBox<ResponseLanguage> languageComboBox;
    /** 控制是否显示高级设置内容的复选框 */
    private JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置内容面板, 用于展示和管理高级配置选项 */
    private JPanel advancedSettingsContentPanel;
    /** 最大重试次数输入框, 支持输入数字或 "auto" */
    private JBTextField maxRetriesField;
    /** 超时时间输入框, 用于设置请求超时时间, 支持输入数字或 "auto" */
    private JBTextField timeoutField;
    /** 温度设置控件, 用于用户输入和调整温度值, 支持输入 "auto" 或数字 */
    private JBTextField temperatureField;
    /** 最大令牌数输入控件, 支持输入 "auto" 或数字 */
    private JBTextField maxTokensField;
    /** 顶部参数输入控件, 支持输入 "auto" 或数字 */
    private JBTextField topPField;
    /** 用于设置 AI 模型的 Top-k 参数的输入控件, 支持输入 "auto" 或数字 */
    private JBTextField topKField;
    /** 偏差惩罚值输入控件, 用于设置生成文本时的偏差惩罚参数, 支持输入 "auto" 或数字 */
    private JBTextField presencePenaltyField;
    /** Agent 代理面板 */
    private IntelliAgentPanel intelliAgentPanel;
    /** checkBoxHintLabelMap 用于映射复选框与对应的提示标签 */
    private final Map<JBCheckBox, JBLabel> checkBoxHintLabelMap = new HashMap<>();

    /**
     * 初始化并创建用户界面组件
     * <p>
     * 该方法用于初始化和创建所有用户界面组件, 包括下拉框, 文本字段, 按钮, 复选框,
     * 旋钮控件以及表格等, 并设置它们的属性和布局. 同时, 为表格添加工具栏装饰器,
     * 用于提供删除和清除所有可用提供者的操作.
     *
     * @param removeAvailableProviderCallback    删除可用提供者时的回调, 可为 null
     * @param clearAllAvailableProvidersCallback 清除所有可用提供者时的回调, 可为 null
     */
    public void createUI(@Nullable Runnable removeAvailableProviderCallback,
                         @Nullable Runnable clearAllAvailableProvidersCallback) {
        // 初始化连接配置组件
        providerComboBox = new ComboBox<>(AIProviderType.getAllDisplayNames().toArray(new String[0]));
        providerComboBox.setRenderer(new ProviderListCellRenderer());

        // 创建获取 API Key 的超链接
        getApiKeyLink = new HyperlinkLabel(AICommonBundle.message("settings.get.api.key"));
        updateApiKeyLinkUrl();

        // 添加下拉框选择监听器，当选择改变时更新链接 URL
        providerComboBox.addItemListener(e -> updateApiKeyLinkUrl());

        modelComboBox = new ComboBox<>();
        modelComboBox.setEditable(true);
        // 设置固定宽度，防止输入超长模型名称时拉宽整个 UI
        Dimension modelComboBoxSize = new Dimension(400, modelComboBox.getPreferredSize().height);
        modelComboBox.setPreferredSize(modelComboBoxSize);
        modelComboBox.setMaximumSize(modelComboBoxSize);
        SwingUtilities.invokeLater(this::installModelSearchFilter);
        modelSearchField = TextFieldWithAutoCompletion.create(getDefaultProject(),
                                                              modelItems,
                                                              false,
                                                              "");
        modelSearchField.setVisible(false);

        baseUrlField = new JBTextField();
        baseUrlField.setToolTipText(AICommonBundle.message("settings.base.url.tooltip"));
        Dimension baseUrlFieldSize = new Dimension(400, baseUrlField.getPreferredSize().height);
        baseUrlField.setPreferredSize(baseUrlFieldSize);
        baseUrlField.setMaximumSize(baseUrlFieldSize);

        apiKeyField = new JBPasswordField();
        apiKeyField.setToolTipText(AICommonBundle.message("settings.api.key.tooltip"));
        Dimension apiKeyFieldSize = new Dimension(400, apiKeyField.getPreferredSize().height);
        apiKeyField.setPreferredSize(apiKeyFieldSize);
        apiKeyField.setMaximumSize(apiKeyFieldSize);

        testConnectionButton = new StatusIndicatorButton(AICommonBundle.message("settings.test.connection"));
        refreshModelsButton = new StatusIndicatorButton(AICommonBundle.message("settings.refresh.models"));

        // 设置按钮宽度一致，取两个按钮文本中较长的宽度
        int buttonWidth = Math.max(
            testConnectionButton.getPreferredSize().width,
            refreshModelsButton.getPreferredSize().width
                                  );
        Dimension buttonSize = new Dimension(buttonWidth, testConnectionButton.getPreferredSize().height);
        testConnectionButton.setPreferredSize(buttonSize);
        testConnectionButton.setMaximumSize(buttonSize);
        refreshModelsButton.setPreferredSize(buttonSize);
        refreshModelsButton.setMaximumSize(buttonSize);

        // 初始化基础配置组件
        verboseLoggingCheckBox = new JBCheckBox(AICommonBundle.message("settings.verbose.logging"));
        lastUpdateCheckCheckBox = new JBCheckBox(AICommonBundle.message("settings.auto.update"));
        showUpdateNotificationCheckBox = new JBCheckBox(AICommonBundle.message("settings.show.update.notification"));
        nextEditEnabledCheckBox = new JBCheckBox(AICommonBundle.message("settings.nextedit.enabled"));
        languageComboBox = new ComboBox<>(ResponseLanguage.values());
        languageComboBox.setRenderer(new DefaultListCellRenderer() {
            /**
             * 重写列表单元格渲染器组件的方法, 用于自定义显示语言描述
             * <p> 该方法继承自父类的 {@code getListCellRendererComponent} 方法, 当列表项为 {@link ResponseLanguage} 类型时,
             * 会将显示文本设置为该语言对象的描述信息 ({@code getDesc()}).
             *
             * @param list         列表组件
             * @param value        当前列表项的数据对象
             * @param index        当前列的索引
             * @param isSelected   当前列是否被选中
             * @param cellHasFocus 当前列是否有焦点
             * @return 渲染后的 JLabel 组件
             */
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ResponseLanguage language) {
                    label.setText(language.getDesc());
                }
                return label;
            }
        });
        languageComboBox.setSelectedItem(ResponseLanguage.ZH);

        // 初始化高级配置组件
        showAdvancedSettingsCheckBox = new JBCheckBox(AICommonBundle.message("settings.advanced.settings.show"));
        maxRetriesField = new JBTextField("2");
        maxRetriesField.setHorizontalAlignment(JBTextField.RIGHT);
        timeoutField = new JBTextField("10");
        timeoutField.setHorizontalAlignment(JBTextField.RIGHT);
        temperatureField = new JBTextField("auto");
        temperatureField.setHorizontalAlignment(JBTextField.RIGHT);
        maxTokensField = new JBTextField("auto");
        maxTokensField.setHorizontalAlignment(JBTextField.RIGHT);
        topPField = new JBTextField("auto");
        topPField.setHorizontalAlignment(JBTextField.RIGHT);
        topKField = new JBTextField("auto");
        topKField.setHorizontalAlignment(JBTextField.RIGHT);
        presencePenaltyField = new JBTextField("auto");
        presencePenaltyField.setHorizontalAlignment(JBTextField.RIGHT);

        // 为所有输入框添加验证逻辑（只能输入数字或 "auto"），并设置边界条件
        // max retries(整数): [0,10]
        setupInputValidation(maxRetriesField, true, 0, 10, true, true);
        // timeout(整数): [1,999999999999]
        setupInputValidation(timeoutField, true, 1, 999999999999L, true, true);
        // max tokens(整数): [1,999999999999]
        setupInputValidation(maxTokensField, true, 1, 999999999999L, true, true);
        // temperature(小数): [0.0, 2.0)
        setupInputValidation(temperatureField, false, 0.0, 2.0, true, false);
        // top_p(小数): (0,1.0]
        setupInputValidation(topPField, false, 0.0, 1.0, false, true);
        // top_k(整数): [0,100]
        setupInputValidation(topKField, true, 0, 100, true, true);
        // presence_penalty(小数): [-2.0, 2.0]
        setupInputValidation(presencePenaltyField, false, -2.0, 2.0, true, true);

        // Agent 相关配置
        intelliAgentPanel = new IntelliAgentPanel();

        // 设置所有输入框的长度一致
        Dimension fieldSize = new Dimension(120, maxRetriesField.getPreferredSize().height);
        maxRetriesField.setPreferredSize(fieldSize);
        timeoutField.setPreferredSize(fieldSize);
        temperatureField.setPreferredSize(fieldSize);
        maxTokensField.setPreferredSize(fieldSize);
        topPField.setPreferredSize(fieldSize);
        topKField.setPreferredSize(fieldSize);
        presencePenaltyField.setPreferredSize(fieldSize);

        // 初始化可用服务商表格
        availableProvidersTableModel = new AvailableProvidersTableModel();
        availableProvidersTable = new JBTable(availableProvidersTableModel);
        availableProvidersTable.setPreferredScrollableViewportSize(new Dimension(480, 120));
        availableProvidersTable.getColumnModel().getColumn(0).setCellRenderer(new ProviderTableCellRenderer());

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(availableProvidersTable)
            .setRemoveAction(button -> {
                if (removeAvailableProviderCallback != null) {
                    removeAvailableProviderCallback.run();
                }
            })
            .addExtraAction(new AnAction(AICommonBundle.message("settings.available.providers.clear.all"),
                                         AICommonBundle.message("settings.available.providers.clear.all.description"),
                                         AllIcons.Actions.GC) {
                /**
                 * 处理动作事件, 调用清除所有可用提供者的回调方法
                 * <p>
                 * 当接收到动作事件时, 如果存在清除所有可用提供者的回调方法, 则执行该回调
                 *
                 * @param e 动作事件对象
                 */
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    if (clearAllAvailableProvidersCallback != null) {
                        clearAllAvailableProvidersCallback.run();
                    }
                }

                /**
                 * 获取动作更新线程类型
                 * <p>
                 * 返回动作更新所使用的线程类型, 该方法覆盖了父类方法以提供特定实现.
                 *
                 * @return 动作更新线程类型, 保证不为 null
                 */
                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    return ActionUpdateThread.EDT;
                }
            });
        availableProvidersPanel = decorator.createPanel();
        availableProvidersPanel.setVisible(false);

        showAvailableProvidersCheckBox = new JBCheckBox(AICommonBundle.message("settings.show.available.providers"));

        // 创建子面板
        JPanel connectionPanel = createConnectionPanel();
        JPanel availableProvidersSectionPanel = createAvailableProvidersPanel();
        JPanel autocompleteProviderPanel = createAutocompleteProviderPanel();
        JPanel basicPanel = createBasicPanel();
        JPanel advancedPanel = createAdvancedPanel();
        // 个人信息面板（作者信息）
        PersonalInfoPanel personalInfoPanel = createPersonalInfoPanel();
        // 反馈面板
        FeedbackPanel feedbackPanel = createFeedbackPanel();

        // 组合成主面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(connectionPanel)
            .addSeparator(10)
            .addComponent(availableProvidersSectionPanel)
            .addSeparator(10)
            .addComponent(autocompleteProviderPanel)
            .addSeparator(10)
            .addComponent(basicPanel)
            .addSeparator(10)
            .addComponent(advancedPanel)
            .addComponentFillVertically(new JPanel(), 0)
            .addComponent(intelliAgentPanel.getContent())
            .addComponent(feedbackPanel.getContent())
            .addComponent(personalInfoPanel.getContent())
            .getPanel();
        mainPanel.setBorder(JBUI.Borders.empty(8));
        // 设置最小宽度为 800
        mainPanel.setMinimumSize(new Dimension(1200, mainPanel.getMinimumSize().height));
    }

    /**
     * 获取主面板组件
     * <p> 返回 AI 提供商配置界面的主面板, 该面板包含了所有配置相关的 UI 组件 </p>
     *
     * @return 主面板组件, 不会返回 null
     */
    @NotNull
    public JPanel getMainPanel() {
        return mainPanel;
    }

    // ==================== Getter 方法 ====================

    /**
     * 获取提供商下拉选择框组件
     * <p> 返回用于选择 AI 服务提供商的组合框组件, 包含所有可用的提供商选项 </p>
     *
     * @return 提供商下拉选择框组件, 保证不为 null
     */
    @NotNull
    public ComboBox<String> getProviderComboBox() {
        return providerComboBox;
    }

    /**
     * 获取模型选择下拉框组件
     * <p> 返回用于选择 AI 模型的组合框控件, 该控件支持编辑功能, 允许用户输入自定义模型名称 </p>
     *
     * @return 模型选择下拉框组件, 保证不为 null
     */
    @NotNull
    public ComboBox<String> getModelComboBox() {
        return modelComboBox;
    }

    /**
     * 获取模型智能搜索输入框组件
     * <p> 返回用于输入和自动完成模型名称的文本框组件, 支持智能搜索功能 </p>
     *
     * @return 模型智能搜索输入框组件, 保证不为 null
     */
    @NotNull
    public TextFieldWithAutoCompletion<String> getModelSearchField() {
        return modelSearchField;
    }

    /**
     * 触发模型搜索弹出框
     * <p> 此方法用于触发模型搜索弹出框的显示. 首先检查模型搜索字段是否存在, 然后将其设为焦点.
     * 如果调用 <code>invokeModelSearchPopup</code> 方法成功触发了任意一种弹出框 ("showPopup","showCompletionPopup" 或 "showAutoPopup"), 则直接返回.
     * 否则, 尝试通过在文本末尾添加一个空格并立即移除的方式来触发弹出框.</p>
     *
     * @since 1.0.0
     */
    public void triggerModelSearchPopup() {
        if (modelSearchField == null) {
            return;
        }
        modelSearchField.requestFocusInWindow();
        if (invokeModelSearchPopup("showPopup")
            || invokeModelSearchPopup("showCompletionPopup")
            || invokeModelSearchPopup("showAutoPopup")) {
            return;
        }
        String text = modelSearchField.getText();
        modelSearchField.setText(text + " ");
        modelSearchField.setCaretPosition(text.length());
        modelSearchField.setText(text);
        modelSearchField.setCaretPosition(text.length());
    }

    /**
     * 调用模型搜索弹窗的指定方法
     * <p> 通过反射机制获取并调用 {@code TextFieldWithAutoCompletion} 类中指定名称的方法, 用于触发模型搜索弹窗的显示行为. 如果调用失败则返回 false.</p>
     *
     * @param methodName 要调用的方法名称, 必须为有效方法名
     * @return 如果成功调用方法并触发弹窗则返回 true, 否则返回 false
     */
    private boolean invokeModelSearchPopup(@NotNull String methodName) {
        try {
            java.lang.reflect.Method method = TextFieldWithAutoCompletion.class.getMethod(methodName);
            method.setAccessible(true);
            method.invoke(modelSearchField);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }


    /**
     * 更新模型下拉框选项并应用当前过滤
     *
     * @param items              完整模型列表
     * @param preferredSelection 优先选中的模型
     */
    public void updateModelItems(@NotNull List<String> items, @Nullable String preferredSelection) {
        modelItems.clear();
        modelItems.addAll(items);
        modelSearchField.setVariants(modelItems);
        suppressModelFilter = true;
        try {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (String item : modelItems) {
                model.addElement(item);
            }
            modelComboBox.setModel(model);
            modelComboBox.setEditable(true);

            if (preferredSelection != null && !preferredSelection.trim().isEmpty() && model.getSize() > 0) {
                if (model.getIndexOf(preferredSelection) >= 0) {
                    modelComboBox.setSelectedItem(preferredSelection);
                }
            }

            String filterText = getModelFilterText();
            if (!filterText.isEmpty() && modelComboBox.getEditor() != null) {
                modelComboBox.getEditor().setItem(filterText);
            }
        } finally {
            suppressModelFilter = false;
        }
    }

    /**
     * 安装模型搜索过滤器监听器
     * <p> 当模型下拉框编辑器组件为文本组件时, 为其文档添加监听器, 监听文本内容的插入, 删除和变更事件,
     * 并在每次变更后调用 {@code onModelFilterChanged()} 方法以触发模型过滤更新 </p>
     *
     * @since 1.0.0
     */
    private void installModelSearchFilter() {
        if (modelFilterInstalled) {
            return;
        }
        if (modelComboBox.getEditor() == null) {
            return;
        }
        Object editorComponent = modelComboBox.getEditor().getEditorComponent();
        if (editorComponent instanceof javax.swing.text.JTextComponent textComponent) {
            textComponent.getDocument().addDocumentListener(new DocumentListener() {
                /**
                 * 文档内容插入更新事件处理方法
                 * <p> 当文档内容发生插入操作时调用此方法, 并触发模型过滤器变更通知
                 *
                 * @param e 文档事件对象, 包含插入操作的详细信息
                 */
                @Override
                public void insertUpdate(DocumentEvent e) {
                    onModelFilterChanged();
                }

                /**
                 * 处理文档删除事件
                 * <p> 当文档内容被删除时, 此方法会被调用, 触发模型过滤刷新操作
                 *
                 * @param e 文档事件对象, 包含文档变更的详细信息
                 */
                @Override
                public void removeUpdate(DocumentEvent e) {
                    onModelFilterChanged();
                }

                /**
                 * 文档结构属性发生变化时的回调方法
                 * <p>当文档的结构属性 (如段落样式, 字体等) 发生变化时,Swing 框架会自动调用此方法.
                 * 该方法内部调用 {@code onModelFilterChanged()} 方法来处理文档变更事件.
                 *
                 * @param e 文档事件对象, 包含变更的详细信息
                 * @see javax.swing.event.DocumentListener
                 * @see #insertUpdate(javax.swing.event.DocumentEvent)
                 * @see #removeUpdate(javax.swing.event.DocumentEvent)
                 */
                @Override
                public void changedUpdate(DocumentEvent e) {
                    onModelFilterChanged();
                }
            });
            modelFilterInstalled = true;
        }
    }


    /**
     * 处理模型过滤器变化事件
     * <p> 当模型下拉框的过滤器文本发生变化时触发此方法. 如果当前处于抑制过滤器状态,
     * 则直接返回以避免重复处理. 该方法使用 Swing 的事件调度线程延迟执行模型过滤操作,
     * 以确保 UI 操作的线程安全性.
     */
    private void onModelFilterChanged() {
        if (suppressModelFilter) {
            return;
        }
        SwingUtilities.invokeLater(() -> applyModelFilter(getModelFilterText(), null));
    }

    /**
     * 获取当前模型下拉框编辑器中的过滤文本
     * <p>
     * 如果编辑器为空则返回空字符串; 否则将编辑器中的对象转换为字符串,
     * 覆盖为默认值空字符串, 然后使用 {@link String#trim()} 去除首尾空白字符.
     *
     * @return 过滤文本, 若无编辑器返回空字符串
     */
    @NotNull
    private String getModelFilterText() {
        if (modelComboBox.getEditor() == null) {
            return "";
        }
        return Objects.toString(modelComboBox.getEditor().getItem(), "").trim();
    }

    /**
     * 根据指定的过滤文本及可选的优先选择项, 更新模型下拉框的内容.
     *
     * <p> 该方法首先构造一个 {@link DefaultComboBoxModel}, 将 {@link #modelItems} 中满足过滤条件的
     * 项目添加进去. 若 {@code filterText} 为空, 则全部项目均被加入; 否则仅将包含
     * {@code filterText}(不区分大小写) 的项目加入模型. 完成后将新的模型设置给 {@code
     * modelComboBox}, 并保持其可编辑状态. 若 {@code preferredSelection} 非空且存在于模型中,
     * 则将其设为当前选择. 最后如果 {@code modelComboBox} 的编辑器不为 {@code null}, 则把
     * {@code filterText} 重新写入编辑器, 以便保持显示一致.
     *
     * @param filterText         用于过滤模型列表的文本, 支持不区分大小写的子串搜索
     * @param preferredSelection 若不为空且存在于筛选结果中, 则将其设为当前选中项; 否则保持
     *                           原选择
     */
    private void applyModelFilter(@NotNull String filterText, @Nullable String preferredSelection) {
        suppressModelFilter = true;
        try {
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            if (filterText.isEmpty()) {
                for (String item : modelItems) {
                    model.addElement(item);
                }
            } else {
                String keyword = filterText.toLowerCase();
                for (String item : modelItems) {
                    if (item != null && item.toLowerCase().contains(keyword)) {
                        model.addElement(item);
                    }
                }
            }
            modelComboBox.setModel(model);
            modelComboBox.setEditable(true);

            if (preferredSelection != null && !preferredSelection.trim().isEmpty() && model.getSize() > 0) {
                if (model.getIndexOf(preferredSelection) >= 0) {
                    modelComboBox.setSelectedItem(preferredSelection);
                }
            }

            if (modelComboBox.getEditor() != null) {
                modelComboBox.getEditor().setItem(filterText);
            }
        } finally {
            suppressModelFilter = false;
        }
    }

    /**
     * 获取默认项目实例
     * <p> 返回 IDE 中的默认项目对象, 用于获取与项目相关的资源和配置信息 </p>
     *
     * @return 默认项目对象, 保证不为 null
     */
    private static @NotNull Project getDefaultProject() {
        return ProjectManager.getInstance().getDefaultProject();
    }

    /**
     * 获取基础 URL 输入框组件
     * <p> 返回用于输入 AI 服务基础 URL 的文本字段组件
     *
     * @return 基础 URL 输入框组件, 保证不为 null
     */
    @NotNull
    public JBTextField getBaseUrlField() {
        return baseUrlField;
    }

    /**
     * 获取 API 密钥输入框组件
     * <p> 返回用于输入 AI 服务提供商 API 密钥的密码字段组件 </p>
     *
     * @return API 密钥输入框组件, 保证不为 null
     */
    @NotNull
    public JBPasswordField getApiKeyField() {
        return apiKeyField;
    }

    /**
     * 获取测试连接按钮组件
     * <p> 返回用于测试 AI 服务连接状态的按钮组件, 点击该按钮可执行连接测试操作.</p>
     *
     * @return 测试连接按钮组件, 保证不为 null
     */
    @NotNull
    public StatusIndicatorButton getTestConnectionButton() {
        return testConnectionButton;
    }

    /**
     * 获取刷新模型列表的按钮组件
     * <p> 返回用于刷新 AI 模型列表的按钮控件, 该按钮通常用于重新加载当前服务提供商支持的模型列表 </p>
     *
     * @return 刷新模型列表的按钮组件, 保证不为 null
     */
    @NotNull
    public StatusIndicatorButton getRefreshModelsButton() {
        return refreshModelsButton;
    }

    /**
     * 获取显示可用提供者的复选框组件
     * <p> 返回用于控制是否显示可用 AI 服务提供商列表的复选框组件 </p>
     *
     * @return 显示可用提供者的复选框组件, 保证不为 null
     */
    @NotNull
    public JBCheckBox getShowAvailableProvidersCheckBox() {
        return showAvailableProvidersCheckBox;
    }

    /**
     * 获取可用提供者面板组件
     * <p> 返回用于展示和管理可用 AI 服务提供商列表及其操作的面板组件 </p>
     *
     * @return 可用提供者面板组件, 保证不为 null
     */
    @NotNull
    public JPanel getAvailableProvidersPanel() {
        return availableProvidersPanel;
    }

    /**
     * 获取显示可用提供者描述的标签组件.
     *
     * <p> 该方法返回用于在 UI 上展示可用 AI 服务提供者信息的 {@link JBLabel} 组件. 若当前 UI 初始化未创建该标签或标签被隐藏,
     * 则会返回 {@code null}.</p>
     *
     * @return 可用提供者描述标签组件, 若组件不存在则返回 {@code null}
     */
    @Nullable
    public JBLabel getAvailableProvidersDescriptionLabel() {
        return availableProvidersDescriptionLabel;
    }

    /**
     * 获取可用提供者表格模型
     * <p> 返回用于管理可用 AI 服务提供商列表的表格模型, 包含提供者信息及其相关操作 </p>
     *
     * @return 可用提供者表格模型, 保证不为 null
     */
    @NotNull
    public AvailableProvidersTableModel getAvailableProvidersTableModel() {
        return availableProvidersTableModel;
    }

    /**
     * 获取自动补全提供者下拉框组件
     * <p> 返回用于在输入时自动推荐 AI 服务提供者的组合框组件, 支持从预设配置列表中选择并绑定到当前上下文 </p>
     *
     * @return 自动补全提供者下拉框组件, 保证不为 null
     */
    @NotNull
    public ComboBox<AIProviderConfig> getAutocompleteProviderComboBox() {
        return autocompleteProviderComboBox;
    }

    /**
     * 设置自动完成提供商列表及其选中项
     * <p> 该方法用于初始化自动完成提供商下拉框的数据模型, 并根据指定的凭证 ID 设置默认选中的项.</p>
     *
     * @param providers            提供商配置列表, 不可为 null
     * @param selectedCredentialId 要选中的提供商凭证 ID, 可为 null
     */
    public void setAutocompleteProviderItems(@NotNull List<AIProviderConfig> providers,
                                             @Nullable String selectedCredentialId) {
        DefaultComboBoxModel<AIProviderConfig> model = new DefaultComboBoxModel<>();
        for (AIProviderConfig config : providers) {
            model.addElement(config.copy());
        }
        autocompleteProviderComboBox.setModel(model);
        autocompleteProviderComboBox.setEnabled(model.getSize() > 0);

        AIProviderConfig selected = null;
        if (selectedCredentialId != null) {
            for (int i = 0; i < model.getSize(); i++) {
                AIProviderConfig config = model.getElementAt(i);
                if (selectedCredentialId.equals(config.credentialId)) {
                    selected = config;
                    break;
                }
            }
        }
        if (selected != null) {
            autocompleteProviderComboBox.setSelectedItem(selected);
        } else if (model.getSize() > 0) {
            autocompleteProviderComboBox.setSelectedItem(null);
        }
    }

    /**
     * 获取控制日志详细输出的复选框组件
     * <p> 返回用于控制是否启用详细日志记录功能的复选框 UI 组件 </p>
     *
     * @return 日志详细输出复选框组件, 保证不为 null
     */
    @NotNull
    public JBCheckBox getVerboseLoggingCheckBox() {
        return verboseLoggingCheckBox;
    }

    /**
     * 获取自动更新检查复选框组件
     * <p> 返回用于控制是否启用自动更新检查功能的复选框组件, 该复选框允许用户开启或关闭自动检查软件更新的功能 </p>
     *
     * @return 自动更新检查复选框组件, 保证不为 null
     */
    @NotNull
    public JBCheckBox getLastUpdateCheckCheckBox() {
        return lastUpdateCheckCheckBox;
    }

    /**
     * 获取是否显示更新通知的复选框组件
     * <p> 返回用于控制是否显示新版本通知的复选框, 该复选框允许用户选择是否接收更新通知.</p>
     *
     * @return 显示更新通知的复选框组件, 保证不为 null
     */
    @NotNull
    public JBCheckBox getShowUpdateNotificationCheckBox() {
        return showUpdateNotificationCheckBox;
    }

    /**
     * 获取是否启用下一步建议的复选框组件
     * <p> 返回用于控制是否启用下一步建议功能的复选框组件, 该组件允许用户在使用 AI 服务时决定是否开启智能建议功能 </p>
     *
     * @return 是否启用下一步建议的复选框组件, 保证不为 null
     */
    @NotNull
    public JBCheckBox getNextEditEnabledCheckBox() {
        return nextEditEnabledCheckBox;
    }

    /**
     * 获取注释语言选择下拉框组件
     * <p> 返回用于设置 AI 生成文本语言的组合框控件, 支持多种语言选项 </p>
     *
     * @return 注释语言选择下拉框组件, 保证不为 null
     */
    @NotNull
    public ComboBox<ResponseLanguage> getLanguageComboBox() {
        return languageComboBox;
    }

    /**
     * 获取是否显示高级设置内容的复选框组件
     * <p> 返回一个复选框组件, 用于指示是否在界面上显示高级设置内容.</p>
     *
     * @return 复选框组件, 保证不为 null
     */
    @NotNull
    public JBCheckBox getShowAdvancedSettingsCheckBox() {
        return showAdvancedSettingsCheckBox;
    }

    /**
     * 获取高级设置内容面板
     * <p> 返回用于展示和管理高级配置选项的面板组件 </p>
     *
     * @return 高级设置内容面板, 如果未初始化则返回 null
     */
    @Nullable
    public JPanel getAdvancedSettingsContentPanel() {
        return advancedSettingsContentPanel;
    }

    /**
     * 获取最大重试次数输入框组件
     * <p> 返回用于设置 AI 请求最大重试次数的文本字段组件, 支持输入数字或 "auto", 默认值为 "2"</p>
     *
     * @return 最大重试次数输入框组件, 保证不为 null
     */
    @NotNull
    public JBTextField getMaxRetriesField() {
        return maxRetriesField;
    }

    /**
     * 获取超时时间输入框组件
     * <p> 返回用于设置请求超时时间的文本字段组件, 支持输入数字或 "auto"</p>
     *
     * @return 超时时间输入框组件, 保证不为 null
     */
    @NotNull
    public JBTextField getTimeoutField() {
        return timeoutField;
    }

    /**
     * 获取温度设置输入框组件
     * <p> 返回用于设置 AI 模型生成文本时的温度参数的文本字段组件, 支持输入 "auto" 或数字值 </p>
     *
     * @return 温度设置输入框组件, 保证不为 null
     */
    @NotNull
    public JBTextField getTemperatureField() {
        return temperatureField;
    }

    /**
     * 获取最大令牌数输入控件
     * <p> 返回用于设置 AI 模型最大令牌数的文本输入框组件, 支持输入 "auto" 或数字 </p>
     *
     * @return 最大令牌数输入框组件, 保证不为 null
     */
    @NotNull
    public JBTextField getMaxTokensField() {
        return maxTokensField;
    }

    /**
     * 获取 Top-p 参数输入框组件
     * <p> 返回用于设置 AI 模型的 Top-p 参数的输入控件, 支持输入 "auto" 或数字 </p>
     *
     * @return Top-p 参数输入框组件, 保证不为 null
     */
    @NotNull
    public JBTextField getTopPField() {
        return topPField;
    }

    /**
     * 获取用于设置 AI 模型 Top-k 参数的输入控件
     * <p> 该控件允许用户输入数字或 "auto" 值, 用于控制模型生成文本时的 Top-k 参数 </p>
     *
     * @return Top-k 参数输入控件, 保证不为 null
     */
    @NotNull
    public JBTextField getTopKField() {
        return topKField;
    }

    /**
     * 获取偏差惩罚值输入控件
     * <p> 返回用于设置生成文本时的偏差惩罚参数的输入控件, 支持输入 "auto" 或数字 </p>
     *
     * @return 偏差惩罚值输入控件, 保证不为 null
     */
    @NotNull
    public JBTextField getPresencePenaltyField() {
        return presencePenaltyField;
    }

    /**
     * 获取智能代理面板组件
     * <p> 返回用于展示和管理智能代理功能的面板组件, 该面板包含与 AI 代理交互的用户界面元素和操作控件 </p>
     *
     * @return 智能代理面板组件, 保证不为 null
     */
    @NotNull
    public IntelliAgentPanel getAgentPanel() {
        return intelliAgentPanel;
    }

    // ==================== UI 创建方法 ====================

    /**
     * 创建连接配置面板, 用于显示和配置 AI 服务的连接相关信息
     * <p>
     * 该方法构建一个包含提供商选择, 基础 URL 输入,API 密钥输入以及模型选择的面板, 用于设置 AI 服务的基本连接配置.
     *
     * @return 包含连接配置组件的面板
     */
    private JPanel createConnectionPanel() {
        // 只设置高度，宽度动态拉伸
        int preferredHeight = providerComboBox.getPreferredSize().height;
        providerComboBox.setPreferredSize(new Dimension(0, preferredHeight));
        providerComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferredHeight));

        // 创建包含下拉框和超链接的面板（同一行）
        // 下拉框在中间动态拉伸，超链接在右边对齐
        JPanel providerPanel = new JPanel(new BorderLayout(5, 0));
        providerPanel.add(providerComboBox, BorderLayout.CENTER);
        providerPanel.add(getApiKeyLink, BorderLayout.EAST);

        JPanel apiKeyPanel = new JPanel(new BorderLayout(5, 0));
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);
        apiKeyPanel.add(refreshModelsButton, BorderLayout.EAST);

        JPanel modelPanel = new JPanel(new BorderLayout(5, 0));
        modelPanel.add(modelComboBox, BorderLayout.CENTER);
        modelPanel.add(testConnectionButton, BorderLayout.EAST);

        JPanel modelSearchPanel = new JPanel(new BorderLayout(5, 0));
        modelSearchPanel.add(modelSearchField, BorderLayout.CENTER);
        modelSearchPanel.setVisible(false);

        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.provider.label")), providerPanel)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.base.url.label")), baseUrlField)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.api.key.label")), apiKeyPanel)
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.model.label")), modelPanel)
            // todo-dong4j : (2026.01.7 16:11) [暂时隐藏, 还需要完善]
            // .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.model.search.label")), modelSearchPanel)
            .getPanel();

        return createPanelWithTitledBorder(panel, AICommonBundle.message("settings.basic.connection.config"));
    }

    /**
     * 更新 API Key 链接的 URL
     * <p>
     * 根据当前选择的服务提供商更新获取 API Key 链接的 URL
     */
    private void updateApiKeyLinkUrl() {
        String selectedDisplayName = (String) providerComboBox.getSelectedItem();
        if (selectedDisplayName == null) {
            getApiKeyLink.setVisible(false);
            return;
        }

        AIProviderType providerType = AIProviderType.fromDisplayName(selectedDisplayName);
        if (providerType == null) {
            getApiKeyLink.setVisible(false);
            return;
        }

        String apiKeyUrl = providerType.getApiKeyUrl();
        if (apiKeyUrl != null && !apiKeyUrl.isEmpty()) {
            final String url = apiKeyUrl; // 需要在 lambda 中使用 final 变量
            getApiKeyLink.setHyperlinkTarget(url);
            // 每次更新时添加新的监听器（HyperlinkLabel 会管理监听器，多次添加不会导致问题）
            getApiKeyLink.addHyperlinkListener(e -> BrowserUtil.browse(url));
            getApiKeyLink.setVisible(true);
        } else {
            // 如果该提供商不需要 API Key，隐藏链接
            getApiKeyLink.setVisible(false);
        }
    }

    /**
     * 创建可用提供者面板
     * <p>
     * 用于构建显示可用提供者的面板, 包含描述标签和相关组件.
     *
     * @return 包含可用提供者信息的面板
     */
    private JPanel createAvailableProvidersPanel() {
        availableProvidersDescriptionLabel = new SpacedJBLabel();
        String descriptionText = AICommonBundle.message("settings.available.providers.description");
        descriptionText = "<html>" + descriptionText.replace("\n", "<br>") + "</html>";
        availableProvidersDescriptionLabel.setText(descriptionText);
        availableProvidersDescriptionLabel.setFont(availableProvidersDescriptionLabel.getFont().deriveFont(availableProvidersDescriptionLabel.getFont().getSize() - 1f));
        availableProvidersDescriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        availableProvidersDescriptionLabel.setBorder(JBUI.Borders.empty(5, 0, 10, 0));
        availableProvidersDescriptionLabel.setVisible(false);

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showAvailableProvidersCheckBox, "settings.show.available.providers.hint"))
            .addComponent(availableProvidersDescriptionLabel)
            .addComponent(availableProvidersPanel)
            .getPanel();

        return createPanelWithTitledBorder(contentPanel, AICommonBundle.message("settings.show.available.providers"));
    }

    /**
     * 创建 Autocomplete 默认服务商选择面板
     */
    private JPanel createAutocompleteProviderPanel() {
        autocompleteProviderComboBox = new ComboBox<>();
        autocompleteProviderComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JBLabel label = new JBLabel();
            if (value != null) {
                Icon icon = AICommonIcons.getProviderIcon(value.providerType);
                label.setIcon(icon);
                String modelName = value.modelName != null ? value.modelName : "";
                label.setText(value.providerType.getDisplayName() + ":" + modelName);
            } else {
                label.setText(AICommonBundle.message("settings.autocomplete.provider.empty"));
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

        // Autocomplete 服务商提示
        JBLabel autocompleteProviderHintLabel = new SpacedJBLabel(AICommonBundle.message("settings.autocomplete.provider.hint"));
        autocompleteProviderHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        autocompleteProviderHintLabel.setFont(autocompleteProviderHintLabel.getFont().deriveFont(autocompleteProviderHintLabel.getFont().getSize() - 1f));

        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.autocomplete.provider.label")),
                                 autocompleteProviderComboBox)
            .addComponent(autocompleteProviderHintLabel)
            .getPanel();

        return createPanelWithTitledBorder(panel, AICommonBundle.message("settings.autocomplete.provider.selection"));
    }

    /**
     * 创建基础配置面板
     * <p>
     * 用于生成包含日志详细设置选项的基础配置面板, 包含一个复选框用于控制日志详细级别.
     *
     * @return 包含日志详细设置选项的面板
     */
    private JPanel createBasicPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.comment.language")), languageComboBox)
            .addComponent(createCheckBoxWithHint(verboseLoggingCheckBox, "settings.verbose.logging.hint"))
            .addComponent(createCheckBoxWithHint(lastUpdateCheckCheckBox, "settings.auto.update.hint"))
            .addComponent(createCheckBoxWithHint(showUpdateNotificationCheckBox, "settings.show.update.notification.hint"))
            .addComponent(createCheckBoxWithHint(nextEditEnabledCheckBox, "settings.nextedit.enabled.hint"))
            .getPanel();

        return createPanelWithTitledBorder(panel, AICommonBundle.message("settings.basic.config"));
    }


    /**
     * 创建高级设置面板, 用于显示 AI 模型的高级配置选项
     * <p>
     * 该方法构建一个包含多个可配置参数的面板, 包括最大重试次数, 超时时间, 最大令牌数, 温度值,Top-p,Top-k, 存在惩罚等设置项.
     * 面板中还包含一个复选框, 用于控制是否显示高级设置内容.
     *
     * @return 包含高级设置内容的面板
     */
    private JPanel createAdvancedPanel() {
        advancedSettingsContentPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.max.retries")),
                                 createTextFieldWithHint(maxRetriesField, "settings.max.retries.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.timeout")),
                                 createTextFieldWithHint(timeoutField, "settings.timeout.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.max.tokens")),
                                 createTextFieldWithHint(maxTokensField, "settings.max.tokens.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.temperature")),
                                 createTextFieldWithHint(temperatureField, "settings.temperature.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.top.p")),
                                 createTextFieldWithHint(topPField, "settings.top.p.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.top.k")),
                                 createTextFieldWithHint(topKField, "settings.top.k.hint"))
            .addLabeledComponent(new SpacedJBLabel(AICommonBundle.message("settings.presence.penalty")),
                                 createTextFieldWithHint(presencePenaltyField, "settings.presence.penalty.hint"))
            .getPanel();
        advancedSettingsContentPanel.setVisible(false);

        JPanel panel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showAdvancedSettingsCheckBox, "settings.advanced.settings.show.hint"))
            .addComponent(advancedSettingsContentPanel)
            .getPanel();

        return createPanelWithTitledBorder(panel, AICommonBundle.message("settings.advanced.config"));
    }

    /**
     * 创建个人信息面板
     * <p>
     * 构建包含作者信息的面板，展示个人简介、社交媒体链接等信息
     *
     * @return 个人信息面板
     */
    @NotNull
    private PersonalInfoPanel createPersonalInfoPanel() {
        return PersonalInfoPanel.create();
    }

    /**
     * 创建反馈面板
     * <p>
     * 构建包含反馈表单的面板，用于收集用户反馈并提交到反馈服务器
     *
     * @return 反馈面板
     */
    @NotNull
    private FeedbackPanel createFeedbackPanel() {
        return new FeedbackPanel(
            null, // 应用级设置，project 为 null
            EngineContents.PLUGIN_ID, // 插件 ID
            EngineContents.PLUGIN_NAME, // 插件名称
            "zeka-stack-engine-plugin" // 签名密钥
        );
    }

    /**
     * 创建包含提示标签的面板, 用于与 JSpinner 组件结合使用
     * <p>
     * 该方法创建一个 JPanel, 其中包含一个 JSpinner 组件和一个提示标签. 提示标签使用指定的提示键获取国际化消息, 并设置为较暗的字体颜色和较小的字体大小, 以实现提示效果.
     *
     * @param spinner 要添加到面板中的 JSpinner 组件
     * @param hintKey 国际化提示消息的键, 用于获取提示文本
     * @return 包含 JSpinner 和提示标签的 JPanel
     */
    private JPanel createSpinnerWithHint(JSpinner spinner, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(spinner, BorderLayout.WEST);

        JBLabel hintLabel = new SpacedJBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(300, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建包含提示标签的面板, 用于与 JBTextField 组件结合使用
     * <p>
     * 该方法创建一个 JPanel, 其中包含一个 JBTextField 组件和一个提示标签. 提示标签使用指定的提示键获取国际化消息, 并设置为较暗的字体颜色和较小的字体大小, 以实现提示效果.
     *
     * @param textField 要添加到面板中的 JBTextField 组件
     * @param hintKey 国际化提示消息的键, 用于获取提示文本
     * @return 包含 JBTextField 和提示标签的 JPanel
     */
    private JPanel createTextFieldWithHint(JBTextField textField, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(textField, BorderLayout.WEST);

        JBLabel hintLabel = new SpacedJBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(300, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建一个包含复选框和提示标签的面板
     * <p>
     * 该方法用于创建一个包含复选框和提示标签的面板, 提示标签根据提供的键获取对应的提示信息, 并设置样式和布局.
     *
     * @param checkBox 要添加到面板中的复选框
     * @param hintKey  用于获取提示信息的键
     * @return 包含复选框和提示标签的面板
     */
    private JPanel createCheckBoxWithHint(JBCheckBox checkBox, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(checkBox, BorderLayout.WEST);

        JBLabel hintLabel = new SpacedJBLabel(AICommonBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        checkBoxHintLabelMap.put(checkBox, hintLabel);
        updateHintLabelColor(hintLabel, checkBox.isSelected());
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        return panel;
    }

    /**
     * 更新提示标签的字体颜色
     * <p>
     * 根据传入的选中状态, 设置提示标签的字体颜色为正常或禁用状态的颜色.
     *
     * @param hintLabel 提示标签对象
     * @param selected  是否选中状态, 为 true 时使用正常颜色, 为 false 时使用禁用颜色
     */
    private void updateHintLabelColor(JBLabel hintLabel, boolean selected) {
        if (selected) {
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    /**
     * 为输入框设置验证逻辑，只允许输入数字（正负、小数、整数）或 "auto" 字符串
     * <p>
     * 该方法使用 DocumentFilter 来限制输入，确保用户只能输入有效的数字或 "auto" 字符串。
     *
     * @param textField 要设置验证的输入框
     */
    private void setupInputValidation(@NotNull JBTextField textField) {
        setupInputValidation(textField, false, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, true);
    }

    /**
     * 为输入框设置验证逻辑，只允许输入数字（正负、小数、整数）或 "auto" 字符串，并验证边界条件
     * <p>
     * 该方法使用 DocumentFilter 来限制输入，确保用户只能输入有效的数字或 "auto" 字符串，并在范围内。
     *
     * @param textField     要设置验证的输入框
     * @param integerOnly   是否只允许整数
     * @param minValue      最小值
     * @param maxValue      最大值
     * @param minInclusive  最小值是否包含边界（true 表示 [min, false 表示 (min）
     * @param maxInclusive  最大值是否包含边界（true 表示 max], false 表示 max)）
     */
    private void setupInputValidation(@NotNull JBTextField textField, boolean integerOnly, double minValue, double maxValue, boolean minInclusive, boolean maxInclusive) {
        // 使用 final 变量以便在内部类中访问
        final boolean isIntegerOnly = integerOnly;
        final double min = minValue;
        final double max = maxValue;
        final boolean minInc = minInclusive;
        final boolean maxInc = maxInclusive;

        AbstractDocument doc = (AbstractDocument) textField.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            /**
             * 在指定位置插入字符串
             * <p> 此方法在过滤器中插入字符串之前, 会调用 {@link #isValidInput} 方法验证输入的有效性. 只有当输入有效时, 才会将字符串插入到文档中.
             *
             * @param fb     过滤器旁路对象, 提供对底层文档的访问
             * @param offset 插入字符串的位置
             * @param string 要插入的字符串
             * @param attr   字符串的属性集
             * @throws BadLocationException 如果指定的位置无效, 则抛出此异常
             */
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (isValidInput(fb.getDocument().getText(0, fb.getDocument().getLength()), string, offset)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            /**
             * 替换文档中的部分内容
             * <p> 在指定位置替换文本内容, 如果输入通过验证则执行替换操作.
             *
             * @param fb     文本过滤器的绕过对象, 用于访问和修改文档内容
             * @param offset 要替换的起始位置 (偏移量)
             * @param length 要替换的旧文本长度
             * @param text   新插入的文本内容
             * @param attrs  插入文本的属性集
             * @throws BadLocationException 如果偏移量或长度无效时抛出此异常
             */
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (isValidInput(currentText, text, offset, length)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            /**
             * 验证输入是否有效
             *
             * @param currentText 当前文本
             * @param newText     新输入的文本
             * @param offset      插入位置
             * @return 如果输入有效返回 true
             */
            private boolean isValidInput(String currentText, String newText, int offset) {
                return isValidInput(currentText, newText, offset, 0);
            }

            /**
             * 验证输入是否有效
             *
             * @param currentText 当前文本
             * @param newText     新输入的文本
             * @param offset      插入位置
             * @param length      要替换的长度
             * @return 如果输入有效返回 true
             */
            private boolean isValidInput(String currentText, String newText, int offset, int length) {
                if (newText == null || newText.isEmpty()) {
                    return true; // 允许删除
                }

                // 构建新文本
                String before = currentText.substring(0, offset);
                String after = currentText.substring(offset + length);
                String fullText = before + newText + after;
                String trimmed = fullText.trim();

                // 允许 "auto"（不区分大小写）
                if ("auto".equalsIgnoreCase(trimmed)) {
                    return true;
                }

                // 如果输入的是 "auto" 的一部分（如 "a", "au", "aut"），允许输入
                String lowerTrimmed = trimmed.toLowerCase();
                if ("auto".startsWith(lowerTrimmed)) {
                    return true;
                }

                // 检查是否为有效的数字（正负、小数、整数）
                // 正则表达式：可选的正负号，后跟数字（可能包含小数点）
                // 匹配：-123, +123, 123, -123.45, +123.45, 123.45, .5, -.5, +.5
                // 但不匹配：空字符串、单独的 +、-、.、多个小数点
                String numberPattern = isIntegerOnly ? "^[+-]?\\d+$" : "^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$";
                if (trimmed.matches(numberPattern)) {
                    // 验证数值范围
                    try {
                        double value = Double.parseDouble(trimmed);
                        // 检查最小值边界
                        if (minInc) {
                            if (value < min) {
                                return false;
                            }
                        } else {
                            if (value <= min) {
                                return false;
                            }
                        }
                        // 检查最大值边界
                        if (maxInc) {
                            if (value > max) {
                                return false;
                            }
                        } else {
                            if (value >= max) {
                                return false;
                            }
                        }
                        // 如果是整数类型，检查是否为整数
                        return !isIntegerOnly || value == (long) value;
                    } catch (NumberFormatException e) {
                        // 如果无法解析为数字，返回 false
                        return false;
                    }
                }

                // 如果新输入的文本是单个字符，检查是否可以作为数字的一部分
                if (newText.length() == 1) {
                    char c = newText.charAt(0);
                    // 允许数字、小数点、正负号
                    if (Character.isDigit(c) || c == '.' || c == '+' || c == '-') {
                        // 检查插入后是否可能形成有效数字
                        String testText = before + newText + after;
                        String testTrimmed = testText.trim();

                        // 如果当前文本是 "auto"，允许替换
                        if ("auto".equalsIgnoreCase(currentText.trim())) {
                            return true;
                        }

                        // 检查是否可能形成有效数字或 "auto" 的一部分
                        if ("auto".startsWith(testTrimmed.toLowerCase())) {
                            return true;
                        }

                        // 检查是否可能形成有效数字
                        String testNumberPattern = isIntegerOnly ? "^[+-]?\\d*$" : "^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$";
                        if (testTrimmed.matches(testNumberPattern)) {
                            // 如果已经是一个完整的数字，验证范围
                            if (testTrimmed.matches(isIntegerOnly ? "^[+-]?\\d+$" : "^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$")) {
                                try {
                                    double testValue = Double.parseDouble(testTrimmed);
                                    // 检查最小值边界
                                    if (minInc) {
                                        if (testValue < min) {
                                            return false;
                                        }
                                    } else {
                                        if (testValue <= min) {
                                            return false;
                                        }
                                    }
                                    // 检查最大值边界
                                    if (maxInc) {
                                        if (testValue > max) {
                                            return false;
                                        }
                                    } else {
                                        if (testValue >= max) {
                                            return false;
                                        }
                                    }
                                    // 如果是整数类型，检查是否为整数
                                    if (isIntegerOnly && testValue != (long) testValue) {
                                        return false;
                                    }
                                } catch (NumberFormatException e) {
                                    // 如果无法解析，可能是输入过程中的中间状态，允许继续输入
                                }
                            }
                            return true;
                        }
                        return false;
                    }
                }

                return false;
            }
        });
    }

    /**
     * 配置 TitledBorder 的字体和颜色
     * <p>
     * 显式设置字体和颜色，确保在 2025 版本中正常显示。
     * 使用 UIUtil 获取主题感知的文本颜色，自动适配浅色和深色主题。
     *
     * @param titledBorder 要配置的 TitledBorder
     */
    private void configureTitledBorder(@NotNull TitledBorder titledBorder) {
        titledBorder.setTitleFont(UIManager.getFont("Label.font"));
        Color titleColor = UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
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
        configureTitledBorder(titledBorder);

        wrapper.setBorder(titledBorder);
        return wrapper;
    }

    /**
     * 更新所有复选框的提示标签颜色
     * <p>
     * 遍历复选框与提示标签的映射关系, 根据复选框的选中状态更新对应的提示标签颜色
     *
     * @since 1.0
     */
    public void updateCheckBoxHintColors() {
        checkBoxHintLabelMap.forEach((checkBox, hintLabel) -> updateHintLabelColor(hintLabel, checkBox.isSelected()));
    }

    /**
     * 创建一个状态点图标, 用于表示某种状态
     * <p>
     * 根据指定颜色生成一个圆形图标, 常用于状态指示
     *
     * @param color 图标填充颜色
     * @return 新创建的图标对象
     */
    @NotNull
    public Icon createStatusDotIcon(Color color) {
        int size = 4;
        BufferedImage image = ImageUtil.createImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(0, 0, size, size);
        g2d.dispose();
        return new ImageIcon(image);
    }

    /**
     * 获取选中的可用服务提供商行索引
     * <p>
     * 返回可用服务提供商表格中当前选中的行的索引
     *
     * @return 选中的行索引, 若未选中则返回 -1
     */
    public int getSelectedAvailableProviderRow() {
        return availableProvidersTable.getSelectedRow();
    }

    // ==================== 内部类 ====================

    /**
     * 自定义的 Provider 列表单元格渲染器
     * <p>
     * 用于在 JList 中渲染 AI Provider 的显示名称和对应的图标, 支持根据显示名称获取对应的 Provider 类型, 并设置相应的图标.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.28
     * @since 1.0.0
     */
    private static class ProviderListCellRenderer extends DefaultListCellRenderer {
        /**
         * 覆盖父类方法, 自定义 JList 单元格的渲染逻辑
         * <p> 根据传入的值设置标签文本和图标, 用于在列表中显示 AI Provider 的名称及其对应图标.
         *
         * @param list         列表组件
         * @param value        当前单元格的值, 预期为 String 类型的显示名称
         * @param index        当前单元格的索引
         * @param isSelected   当前单元格是否被选中
         * @param cellHasFocus 当前单元格是否有焦点
         * @return 渲染后的 JLabel 组件
         */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof String displayName) {
                label.setText(displayName);
                AIProviderType providerType = AIProviderType.fromDisplayName(displayName);
                if (providerType != null) {
                    Icon icon = AICommonIcons.getProviderIcon(providerType);
                    label.setIcon(icon);
                }
            }
            return label;
        }
    }

    /**
     * 表格单元格渲染器, 用于自定义 AI 提供商配置信息的显示样式
     * <p>
     * 该渲染器继承自 DefaultTableCellRenderer, 用于在表格中渲染 AIProviderConfig 对象, 显示其对应的显示名称和图标.
     * 支持根据配置类型获取对应的显示名称和图标, 提升表格数据的可读性和可视化效果.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.28
     * @since 1.0.0
     */
    private static class ProviderTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        /**
         * 用于自定义 AI 提供商配置信息在表格中的显示样式
         * <p> 该方法继承自 DefaultTableCellRenderer 的 getTableCellRendererComponent 方法, 用于渲染 AIProviderConfig 对象,
         * 显示其对应的显示名称和图标. 支持根据配置类型获取对应的显示名称和图标, 提升表格数据的可读性和可视化效果.
         *
         * @param table      表格组件
         * @param value      当前单元格的值, 应为 AIProviderConfig 类型
         * @param isSelected 是否选中当前单元格
         * @param hasFocus   是否具有焦点
         * @param row        当前行号
         * @param column     当前列号
         * @return 返回渲染后的组件, 通常为 JLabel
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                       int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JLabel label && value instanceof AIProviderConfig config) {
                String displayName = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message(
                    "settings.available.providers.unknown");
                label.setText(displayName);
                if (config.providerType != null) {
                    Icon icon = AICommonIcons.getProviderIcon(config.providerType);
                    label.setIcon(icon);
                }
            }
            return component;
        }
    }

    /**
     * 可用提供者表格模型类
     * <p>
     * 用于展示和操作可用 AI 提供者的配置信息, 支持数据的增删改查以及表格的显示与编辑功能.
     * 该模型继承自 AbstractTableModel, 实现了表格数据的绑定和操作接口.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.11.28
     * @since 1.0.0
     */
    public static class AvailableProvidersTableModel extends AbstractTableModel {
        /** 列名数组, 用于定义表格中各列的显示标题 */
        private final String[] columnNames = {
            AICommonBundle.message("settings.available.providers.column.provider"),
            AICommonBundle.message("settings.available.providers.column.model"),
            AICommonBundle.message("settings.available.providers.column.timeout"),
            AICommonBundle.message("settings.available.providers.column.max.tokens"),
            AICommonBundle.message("settings.available.providers.column.remark")
        };
        /**
         * 存储可用 AI 提供者配置信息的列表
         *
         * @see AIProviderConfig
         */
        private final List<AIProviderConfig> data = new java.util.ArrayList<>();

        /**
         * 设置可用提供者的数据
         * <p> 清空当前数据列表, 并将传入的 AIProviderConfig 列表中的每个配置对象的副本添加到数据列表中.
         * 调用 fireTableDataChanged() 方法通知观察者数据已更改.
         *
         * @param configs 包含 AIProviderConfig 对象的列表
         */
        public void setData(List<AIProviderConfig> configs) {
            data.clear();
            configs.forEach(config -> data.add(config.copy()));
            fireTableDataChanged();
        }

        /**
         * 获取当前表格中所有 AI 提供者配置的副本列表
         * <p> 该方法遍历内部数据列表, 对每个配置对象调用 copy() 方法生成副本, 并返回一个独立的列表, 避免外部直接修改原始数据.
         *
         * @return 包含所有配置对象副本的列表, 类型为 {@code List<AIProviderConfig>}
         */
        public List<AIProviderConfig> getData() {
            List<AIProviderConfig> copy = new java.util.ArrayList<>();
            data.forEach(config -> copy.add(config.copy()));
            return copy;
        }

        /**
         * 根据索引获取 AI 提供者配置对象
         * <p> 通过指定的索引从数据列表中获取对应的 AIProviderConfig 实例, 如果索引越界则返回 null
         *
         * @param index 索引位置, 必须为非负数且小于数据列表大小
         * @return 对应索引的 AIProviderConfig 实例, 如果索引越界则返回 null
         */
        public AIProviderConfig getProviderConfig(int index) {
            if (index >= 0 && index < data.size()) {
                return data.get(index);
            }
            return null;
        }

        /**
         * 获取表格行数
         * <p> 返回当前数据列表中的元素数量, 用于确定表格显示的总行数 </p>
         *
         * @return 数据列表的行数
         */
        @Override
        public int getRowCount() {
            return data.size();
        }

        /**
         * 获取表格的列数量
         * <p> 返回表格模型中定义的列名称数组的长度, 即表格的总列数
         *
         * @return 表格的列数量, 等于列名称数组的长度
         */
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * 获取指定列的列名称
         * <p> 根据列索引返回对应的列名称, 用于表格显示
         *
         * @param column 列索引, 从 0 开始
         * @return 指定列的名称字符串
         */
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        /**
         * 获取表格中指定单元格的值
         * <p> 根据行索引和列索引返回对应的单元格数据, 不同列返回不同的提供者配置信息
         *
         * @param rowIndex    行索引
         * @param columnIndex 列索引,0 - 提供者配置,1 - 模型名称,2 - 超时设置,3 - 最大 token 数,4 - 备注信息
         * @return 对应单元格的值, 当列索引无效时返回空字符串
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AIProviderConfig config = data.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> config;
                case 1 -> config.modelName != null ? config.modelName : "";
                case 2 -> {
                    AIRuntimeSettings runtime = config.runtimeSettings != null ? config.runtimeSettings : new AIRuntimeSettings();
                    yield runtime.timeout;
                }
                case 3 -> {
                    AIModelParameters params = config.modelParameters != null ? config.modelParameters : new AIModelParameters();
                    // 迁移老配置中的 maxTokens（从实际 token 数转换为 K 单位）
                    yield AIModelParameters.migrateMaxTokens(params.maxTokens);
                }
                case 4 -> config.remark != null ? config.remark : "";
                default -> "";
            };
        }

        /**
         * 判断表格中指定位置的单元格是否可编辑.
         * <p>本模型仅允许第 2,3,4 列 (即 “timeout”,"max tokens","remark" 列) 的单元格可编辑, 其余列不可编辑.
         *
         * @param rowIndex    行索引
         * @param columnIndex 列索引
         * @return 若该单元格属于可编辑列, 返回 {@code true}; 否则返回 {@code false}
         */
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 || columnIndex == 3 || columnIndex == 4;
        }

        /**
         * 设置指定单元格的值, 并同步更新内部数据模型.<p>
         * <p>
         * 根据 {@code columnIndex} 的值执行不同的更新逻辑:<ul>
         * <li> 列 2(超时时间): 若 {@code aValue} 可解析为整数, 则将其限制在 1~600 范围内后保存; 若解析失败则忽略.</li>
         * <li> 列 3(最大 Token): 将 {@code aValue} 转为字符串并去除首尾空格; 若为空字符串则默认设为 {@code "auto"}.</li>
         * <li> 列 4(备注): 直接将 {@code aValue} 解析为字符串并保存.</li>
         * </ul>
         * 其它列保持不变. 随后调用 {@link #fireTableCellUpdated(int, int)} 通知监听器单元格已更新.<p>
         * <p>
         * 当 {@code rowIndex} 超出数据集合范围时方法直接返回, 保持表状态不变.<p>
         *
         * @param aValue      要设置的新值, 支持 {@code String},{@code Integer} 等类型; 若为 {@code null} 则按默认处理.
         * @param rowIndex    行索引, 从 0 开始. 若小于 0 或大于等于当前数据行数, 方法无操作.
         * @param columnIndex 列索引, 主要关注 2,3,4. 其他索引值保持原始内容不变.
         */
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= data.size()) {
                return;
            }
            AIProviderConfig config = data.get(rowIndex);
            switch (columnIndex) {
                case 2 -> {
                    // 超时时间
                    if (config.runtimeSettings == null) {
                        config.runtimeSettings = new AIRuntimeSettings();
                    }
                    try {
                        int timeout = Integer.parseInt(aValue != null ? aValue.toString() : "10");
                        config.runtimeSettings.timeout = Math.max(1, Math.min(600, timeout));
                        fireTableCellUpdated(rowIndex, columnIndex);
                    } catch (NumberFormatException ignored) {
                        // 忽略无效输入
                    }
                }
                case 3 -> {
                    // 最大 Token
                    if (config.modelParameters == null) {
                        config.modelParameters = new AIModelParameters();
                    }
                    String maxTokensStr = aValue != null ? aValue.toString().trim() : "auto";
                    config.modelParameters.maxTokens = maxTokensStr.isEmpty() ? "auto" : maxTokensStr;
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
                case 4 -> {
                    // 备注
                    config.remark = aValue != null ? aValue.toString() : "";
                    fireTableCellUpdated(rowIndex, columnIndex);
                }
            }
        }
    }
}
