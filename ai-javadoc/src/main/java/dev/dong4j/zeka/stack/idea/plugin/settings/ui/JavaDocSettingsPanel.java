package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderConfigPanel;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * JavaDoc 设置面板 UI
 *
 * <p>构建设置界面的所有 UI 组件。
 *
 * @author dong4j
 * @version 1.0.0
 */
@SuppressWarnings( {"D", "DuplicatedCode"})
public class JavaDocSettingsPanel {

    /** 主界面主面板，用于承载主要功能组件和布局 */
    private JPanel mainPanel;

    /** 通用 AI 提供商配置面板 */
    private final AIProviderConfigPanel providerConfigPanel;

    // 功能配置
    /** 生成针对类的复选框 */
    private JBCheckBox generateForClassCheckBox;
    /** 方法生成复选框，用于控制是否为方法生成代码 */
    private JBCheckBox generateForMethodCheckBox;
    /** 生成字段的复选框 */
    private JBCheckBox generateForFieldCheckBox;
    /** 覆盖已有注释复选框 */
    private JBCheckBox overrideExistingCheckBox;
    /** 启用代码压缩的复选框 */
    private JBCheckBox enableCodeCompressionCheckBox;
    /** 在中英文间添加空格复选框 */
    private JBCheckBox addSpaceBetweenChineseAndEnglishCheckBox;
    /** 将中文标点符号转为英文标点符号复选框 */
    private JBCheckBox replaceChinesePunctuationCheckBox;
    /** 最大类代码行数设置控件 */
    private JSpinner maxClassCodeLinesSpinner;

    // 语言支持
    /** Java 语言支持选项框 */
    private JBCheckBox javaCheckBox;
    /** Kotlin 语言支持开关控件 */
    private JBCheckBox kotlinCheckBox;

    // JavaDoc 标签配置
    /** 显示自定义 JavaDoc 标签的复选框 */
    private JBCheckBox showCustomJavaDocTagsCheckBox;
    /** 自定义 JavaDoc 标签列表表格 */
    private JBTable customJavaDocTagsTable;
    /** 自定义 JavaDoc 标签列表面板（包含表格和工具栏） */
    private JPanel customJavaDocTagsPanel;
    /** 自定义 JavaDoc 标签列表表格模型 */
    private CustomJavaDocTagsTableModel customJavaDocTagsTableModel;

    // 高级设置
    /** 显示高级设置的复选框 */
    private JBCheckBox showAdvancedSettingsCheckBox;
    /** 高级设置容器面板（用于控制可见性） */
    private JPanel advancedSettingsPanel;


    /** 系统提示文本区域，用于显示或编辑系统提示内容 */
    public JTextArea systemPromptTextArea;
    /** 类提示文本区域，用于显示或输入类相关的提示信息 */
    public JTextArea classPromptTextArea;
    /** 方法提示文本区域，用于显示方法相关的提示信息 */
    public JTextArea methodPromptTextArea;
    /** 提示信息显示区域，用于展示操作提示或说明文字 */
    public JTextArea fieldPromptTextArea;
    /** 测试提示文本区域 */
    public JTextArea testPromptTextArea;

    /** 存储复选框和提示标签的映射关系，用于更新提示文本颜色 */
    private final java.util.Map<JBCheckBox, JBLabel> checkBoxHintLabelMap = new java.util.HashMap<>();

    /**
     * 构造函数，初始化 JavaDoc 设置面板
     * <p>
     * 调用创建用户界面和设置事件监听器的方法，完成面板的初始化
     */
    public JavaDocSettingsPanel() {
        AICredentialManager credentialManager = new AICredentialManager("AI Javadoc", "AI_JAVADOC_API_KEY_");
        this.providerConfigPanel = new AIProviderConfigPanel(credentialManager);
        createUI();
        setupListeners();
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * 初始化用户界面组件，创建并配置所有 UI 元素，包括下拉框、文本字段、按钮、复选框等。
     * <p>
     * 该方法负责构建整个设置界面的主面板，包括 AI 提供商配置、模型选择、基础 URL 和 API 密钥输入、
     * 连接测试按钮、模型刷新按钮、生成选项、语言支持、高级配置参数以及提示模板区域。
     */
    private void createUI() {
        // 功能配置
        generateForClassCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.class"));
        generateForMethodCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.method"));
        generateForFieldCheckBox = new JBCheckBox(JavaDocBundle.message("settings.generate.for.field"));
        overrideExistingCheckBox = new JBCheckBox(JavaDocBundle.message("settings.override.existing"));
        enableCodeCompressionCheckBox = new JBCheckBox(JavaDocBundle.message("settings.enable.code.compression"));
        maxClassCodeLinesSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 300000, 100));
        addSpaceBetweenChineseAndEnglishCheckBox = new JBCheckBox(JavaDocBundle.message("settings.add.space.between.chinese.and.english"));
        replaceChinesePunctuationCheckBox = new JBCheckBox(JavaDocBundle.message("settings.replace.chinese.punctuation"));

        // 语言支持
        javaCheckBox = new JBCheckBox(JavaDocBundle.message("settings.language.java"));
        javaCheckBox.setEnabled(true);
        kotlinCheckBox = new JBCheckBox(JavaDocBundle.message("settings.language.kotlin"));
        kotlinCheckBox.setEnabled(false);

        // 创建自定义 JavaDoc 标签组件
        showCustomJavaDocTagsCheckBox = new JBCheckBox(JavaDocBundle.message("settings.custom.javadoc.tags"));
        customJavaDocTagsTableModel = new CustomJavaDocTagsTableModel();
        customJavaDocTagsTable = new JBTable(customJavaDocTagsTableModel);
        customJavaDocTagsTable.setPreferredScrollableViewportSize(new Dimension(500, 100));

        // 创建带工具栏的面板
        ToolbarDecorator tagsDecorator = ToolbarDecorator.createDecorator(customJavaDocTagsTable)
            .setAddAction(button -> addCustomJavaDocTag())
            .setRemoveAction(button -> {
                int selectedRow = customJavaDocTagsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    removeCustomJavaDocTag(selectedRow);
                }
            })
            .addExtraAction(new AnAction(JavaDocBundle.message("settings.custom.javadoc.tags.clear.all"),
                                         JavaDocBundle.message("settings.custom.javadoc.tags.clear.all.description"),
                                         com.intellij.icons.AllIcons.Actions.GC) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    clearAllCustomJavaDocTags();
                }

                @Override
                public void update(@NotNull AnActionEvent e) {
                    // 根据表格状态启用/禁用按钮
                    boolean hasData = customJavaDocTagsTableModel.getRowCount() > 0;
                    e.getPresentation().setEnabled(hasData);
                }

                @Override
                public @NotNull ActionUpdateThread getActionUpdateThread() {
                    // 需要访问 Swing 组件（表格模型），必须在 EDT 中执行
                    return ActionUpdateThread.EDT;
                }
            });

        customJavaDocTagsPanel = tagsDecorator.createPanel();
        // 可见性将在 loadSettings 中根据配置设置

        // Prompt 配置 - 创建文本区域（将在 Tab 页中使用）
        // 增加初始高度：15行（原来10行），宽度保持50列不变
        systemPromptTextArea = new JTextArea(15, 50);
        classPromptTextArea = new JTextArea(15, 50);
        methodPromptTextArea = new JTextArea(15, 50);
        fieldPromptTextArea = new JTextArea(15, 50);
        testPromptTextArea = new JTextArea(15, 50);

        // 创建高级设置复选框
        showAdvancedSettingsCheckBox = new JBCheckBox(JavaDocBundle.message("settings.prompt.settings.show"));

        // 创建高级设置容器面板
        advancedSettingsPanel = new JPanel(new BorderLayout());
        advancedSettingsPanel.setVisible(false); // 默认隐藏

        // 构建高级设置面板内容（只包含 Prompt 模板，AI 配置已在 AIProviderConfigPanel 中）
        JPanel advancedSettingsContent = FormBuilder.createFormBuilder()
            // Prompt 模板与提示词
            .addComponent(createPromptTemplatesPanel())
            .getPanel();

        advancedSettingsPanel.add(advancedSettingsContent, BorderLayout.NORTH);

        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            // 第一组：基础连接配置（API 接入）
            .addComponent(createBasicConnectionConfigPanel())
            .addSeparator(10)

            // 第二组：高级设置（可折叠）
            .addComponent(showAdvancedSettingsCheckBox)
            .addComponent(advancedSettingsPanel)
            .addSeparator(10)

            // 第三组：支持的语言
            .addComponent(createLanguageSupportPanel())
            .addSeparator(10)

            // 第四组：生成规则配置
            .addComponent(createGenerationRulesPanel())
            .addSeparator(10)

            // 第五组：其他设置
            .addComponent(createOtherSettingsPanel())

            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
    }

    /**
     * 创建基础连接配置面板
     *
     * <p>直接使用 AIProviderConfigPanel，它已经包含了所有 AI 相关的配置（连接配置、基础配置、高级配置）。
     *
     * @return 基础连接配置面板
     */
    private JPanel createBasicConnectionConfigPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(providerConfigPanel.getPanel(), BorderLayout.CENTER);
        wrapper.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.basic.connection.config")));
        return wrapper;
    }


    /**
     * 创建 Prompt 模板与提示词面板
     *
     * <p>创建一个包含 Prompt 模板与提示词所有组件的面板，并添加边框。
     *
     * @return Prompt 模板与提示词面板
     */
    private JPanel createPromptTemplatesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + JavaDocBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.advanced.settings.prompt.templates")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建语言支持面板
     *
     * <p>创建一个包含语言支持复选框的面板，用于选择支持哪些编程语言，并添加边框。
     *
     * @return 语言支持面板
     */
    private JPanel createLanguageSupportPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(javaCheckBox)
            .addComponent(kotlinCheckBox)
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.language.support")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建生成规则配置面板
     *
     * <p>创建一个包含生成规则配置的面板，包括：
     * <ul>
     *   <li>生成选项（类/方法/字段）</li>
     *   <li>覆盖已有注释</li>
     *   <li>代码压缩配置</li>
     * </ul>
     * 并添加边框。
     *
     * @return 生成规则配置面板
     */
    private JPanel createGenerationRulesPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createGenerationOptionsPanel())
            .addComponent(createCheckBoxWithHint(overrideExistingCheckBox, "settings.override.existing.hint"))
            .addComponent(createCheckBoxWithHint(enableCodeCompressionCheckBox, "settings.enable.code.compression.hint"))
            .addComponent(createCodeCompressionSubConfigPanel())
            .addComponent(
                createCheckBoxWithHint(addSpaceBetweenChineseAndEnglishCheckBox,
                                       "settings.add.space.between.chinese.and.english.hint"))
            .addComponent(createCheckBoxWithHint(replaceChinesePunctuationCheckBox,
                                                 "settings.replace.chinese.punctuation.hint"))
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.generation.rules.config")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建其他设置面板
     *
     * <p>创建一个包含其他设置所有组件的面板，并添加边框。
     *
     * @return 其他设置面板
     */
    private JPanel createOtherSettingsPanel() {
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(showCustomJavaDocTagsCheckBox)
            .addComponent(customJavaDocTagsPanel)
            .getPanel();

        // 创建带边框的面板
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(contentPanel, BorderLayout.CENTER);

        // 添加带标题的边框
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            JavaDocBundle.message("settings.other.settings")
                                                                    );
        panel.setBorder(titledBorder);

        return panel;
    }

    /**
     * 创建生成选项面板
     *
     * <p>创建一个包含生成选项复选框的面板，用于选择要为哪些类型的元素生成文档。
     * 面板包含3个复选框水平排列（类、方法、字段）。
     *
     * @return 生成选项面板
     */
    private JPanel createGenerationOptionsPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new java.awt.BorderLayout());

        // 3个复选框水平排列
        JBCheckBox[] checkBoxes = {
            generateForClassCheckBox,
            generateForMethodCheckBox,
            generateForFieldCheckBox
        };

        String[] hintKeys = {
            "settings.generate.for.class.hint",
            "settings.generate.for.method.hint",
            "settings.generate.for.field.hint"
        };

        JPanel checkBoxPanel = createHorizontalCheckBoxPanel(checkBoxes, hintKeys, 3);
        mainPanel.add(checkBoxPanel, java.awt.BorderLayout.NORTH);

        return mainPanel;
    }

    /**
     * 创建高级配置面板，包含一个带宽度限制的 JSpinner 和提示标签
     * <p>
     * 该方法用于构建一个布局面板，左侧放置一个设置宽度的 JSpinner 控件，右侧放置一个带有提示信息的标签。
     * 提示标签的字体大小和颜色会根据系统 UI 设置进行调整。
     *
     * @param spinner 用于配置的 JSpinner 控件
     * @param hintKey 提示信息的键，用于从资源文件中获取对应的提示文本
     * @return 包含 JSpinner 和提示标签的面板
     */
    private JPanel createAdvancedConfigPanel(JSpinner spinner, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // 固定输入框宽度
        spinner.setPreferredSize(new Dimension(120, spinner.getPreferredSize().height));
        panel.add(spinner, BorderLayout.WEST);

        // 提示文本放在右侧，但限制宽度
        JBLabel hintLabel = new JBLabel(JavaDocBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        hintLabel.setPreferredSize(new Dimension(300, hintLabel.getPreferredSize().height));
        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建包含复选框和提示文本的面板
     * <p>
     * 该方法用于创建一个包含复选框和提示文本的面板，提示文本通过指定的键从资源文件中获取。
     * 当复选框被勾选时，提示文本会以正常颜色（高亮）显示；未勾选时，提示文本以较暗的颜色显示。
     *
     * <p>特殊处理：
     * <ul>
     *   <li>显示统计信息复选框：不在这里添加监听器，因为它需要依赖性能模式的状态，监听器在 setupListeners 中添加</li>
     *   <li>其他复选框：自动添加监听器来更新提示文本颜色</li>
     * </ul>
     *
     * @param checkBox 要添加到面板中的复选框
     * @param hintKey  用于获取提示文本的资源键
     * @return 包含复选框和提示文本的面板
     */
    private JPanel createCheckBoxWithHint(JBCheckBox checkBox, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // 复选框放在左侧
        panel.add(checkBox, BorderLayout.WEST);

        // 提示文本放在右侧
        JBLabel hintLabel = new JBLabel(JavaDocBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));

        // 保存映射关系，用于后续更新颜色
        checkBoxHintLabelMap.put(checkBox, hintLabel);

        // 根据复选框状态设置提示文本颜色
        updateHintLabelColor(hintLabel, checkBox.isSelected());

        // 监听复选框状态变化，动态更新提示文本颜色
        checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));

        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

    /** 类代码最大行数标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesLabel;

    /** 类代码最大行数提示标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesHintLabel;

    /**
     * 创建代码压缩的子配置面板（类代码最大行数）
     * <p>
     * 该类代码最大行数配置作为代码压缩的子配置，会向右缩进2个空格。
     * 当代码压缩复选框被勾选时，该配置才可用。
     *
     * @return 包含类代码最大行数配置的面板
     */
    private JPanel createCodeCompressionSubConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 添加左侧缩进（2个空格，约20像素）
        JPanel indentPanel = new JPanel(new BorderLayout());
        indentPanel.setBorder(JBUI.Borders.emptyLeft(22));

        // 创建标签
        maxClassCodeLinesLabel = new JBLabel(JavaDocBundle.message("settings.max.class.code.lines"));

        // 创建提示标签
        maxClassCodeLinesHintLabel = new JBLabel(JavaDocBundle.message("settings.max.class.code.lines.hint"));
        maxClassCodeLinesHintLabel.setFont(maxClassCodeLinesHintLabel.getFont().deriveFont(maxClassCodeLinesHintLabel.getFont().getSize() - 2.0f));
        maxClassCodeLinesHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        maxClassCodeLinesHintLabel.setPreferredSize(new Dimension(300, maxClassCodeLinesHintLabel.getPreferredSize().height));

        // 创建包含标签、输入框和提示的面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(maxClassCodeLinesLabel, BorderLayout.WEST);

        // 创建输入框和提示的面板
        JPanel spinnerPanel = new JPanel(new BorderLayout(5, 0));
        maxClassCodeLinesSpinner.setPreferredSize(new Dimension(120, maxClassCodeLinesSpinner.getPreferredSize().height));
        spinnerPanel.add(maxClassCodeLinesSpinner, BorderLayout.WEST);
        spinnerPanel.add(maxClassCodeLinesHintLabel, BorderLayout.CENTER);
        contentPanel.add(spinnerPanel, BorderLayout.CENTER);

        indentPanel.add(contentPanel, BorderLayout.CENTER);
        panel.add(indentPanel, BorderLayout.CENTER);

        // 初始状态：根据代码压缩复选框的状态设置可用性
        updateMaxClassCodeLinesEnabled();

        return panel;
    }


    /**
     * 更新提示标签的颜色
     * <p>
     * 根据复选框的选中状态，设置提示标签的前景色。
     * 选中时使用正常颜色（高亮），未选中时使用较暗的颜色。
     *
     * @param hintLabel 提示标签
     * @param selected  是否选中
     */
    private void updateHintLabelColor(JBLabel hintLabel, boolean selected) {
        if (selected) {
            // 选中时使用正常颜色（高亮显示）
            hintLabel.setForeground(UIManager.getColor("Label.foreground"));
        } else {
            // 未选中时使用较暗的颜色
            hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }

    /**
     * 更新类代码最大行数输入框的可用性
     * <p>
     * 根据代码压缩复选框的状态，设置类代码最大行数输入框、标签和提示的可用性。
     */
    private void updateMaxClassCodeLinesEnabled() {
        boolean enabled = enableCodeCompressionCheckBox.isSelected();
        maxClassCodeLinesSpinner.setEnabled(enabled);
        if (maxClassCodeLinesLabel != null) {
            maxClassCodeLinesLabel.setEnabled(enabled);
        }
        if (maxClassCodeLinesHintLabel != null) {
            maxClassCodeLinesHintLabel.setEnabled(enabled);
            // 根据可用性更新提示文本颜色
            if (enabled) {
                maxClassCodeLinesHintLabel.setForeground(UIManager.getColor("Label.foreground"));
            } else {
                maxClassCodeLinesHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }
    }

    /**
     * 更新所有复选框的提示文本颜色
     * <p>
     * 根据每个复选框的当前选中状态，更新对应的提示文本颜色。
     * 用于在加载设置时初始化提示文本的颜色。
     */
    private void updateAllCheckBoxHintColors() {
        checkBoxHintLabelMap.forEach((checkBox, hintLabel) ->
                                         updateHintLabelColor(hintLabel, checkBox.isSelected()));
    }

    /**
     * 创建水平排列的复选框面板
     *
     * @param checkBoxes  复选框数组
     * @param hintKeys    对应的提示文本键数组
     * @param itemsPerRow 每行显示的复选框数量
     * @return 水平排列的复选框面板
     */
    private JPanel createHorizontalCheckBoxPanel(JBCheckBox[] checkBoxes, String[] hintKeys, int itemsPerRow) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();

        // 设置间距 - 减少水平间距
        gbc.insets = JBUI.insets(5, 1);
        gbc.anchor = java.awt.GridBagConstraints.WEST;

        for (int i = 0; i < checkBoxes.length; i++) {
            // 计算行和列
            int row = i / itemsPerRow;

            gbc.gridx = i % itemsPerRow;
            gbc.gridy = row;
            gbc.weightx = 1.0 / itemsPerRow; // 平均分配宽度

            // 创建单个复选框的面板
            JPanel checkBoxPanel = new JPanel(new BorderLayout(5, 0));
            checkBoxPanel.add(checkBoxes[i], BorderLayout.WEST);

            // 添加提示文本
            if (i < hintKeys.length && hintKeys[i] != null) {
                JBLabel hintLabel = new JBLabel(JavaDocBundle.message(hintKeys[i]));
                hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
                hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                checkBoxPanel.add(hintLabel, BorderLayout.CENTER);
            }

            mainPanel.add(checkBoxPanel, gbc);
        }

        return mainPanel;
    }

    /**
     * 创建用于显示提示配置的选项卡面板
     * <p>
     * 初始化一个包含多个提示配置选项卡的 JBTabbedPane，每个选项卡对应不同的提示类型，如系统提示、类提示、方法提示等。
     *
     * @return 包含提示配置选项卡的 JBTabbedPane 实例
     */
    private JBTabbedPane createPromptTabbedPane() {
        // Prompt 配置 - Tab 页
        JBTabbedPane promptTabbedPane = new JBTabbedPane();
        // 增加 Tab 页的高度：宽度保持600不变，高度从200增加到400
        promptTabbedPane.setPreferredSize(new Dimension(600, 400));

        // 创建各个 Tab 页
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.system"), createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.class"), createPromptTab(classPromptTextArea, "class"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.method"), createPromptTab(methodPromptTextArea, "method"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.field"), createPromptTab(fieldPromptTextArea, "field"));
        promptTabbedPane.addTab(JavaDocBundle.message("settings.prompt.tab.test"), createPromptTab(testPromptTextArea, "test"));

        return promptTabbedPane;
    }

    /**
     * 创建提示信息标签页面板
     * <p>
     * 根据给定的文本区域和提示类型，创建一个包含文本区域和重置按钮的标签页面板。
     *
     * @param textArea   文本区域组件
     * @param promptType 提示类型，用于加载对应的提示信息和资源
     * @return 包含文本区域和重置按钮的面板
     */
    private JPanel createPromptTab(JTextArea textArea, String promptType) {
        JPanel tabPanel = new JPanel(new BorderLayout());

        // 创建文本区域
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(JavaDocBundle.message("settings.prompt." + promptType + ".tooltip"));

        // 添加文档监听器，根据内容自动调整大小
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }
        });

        // 创建滚动面板，并添加边框以在四周留出空间
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 添加边框，在四周留出10像素的空间
        scrollPane.setBorder(JBUI.Borders.empty(10));

        tabPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建重置按钮
        JButton resetButton = new JButton(JavaDocBundle.message("settings.prompt.reset"));
        resetButton.addActionListener(e -> resetPromptToDefault(promptType, textArea));
        tabPanel.add(resetButton, BorderLayout.SOUTH);

        // 初始化时根据内容调整大小
        SwingUtilities.invokeLater(() -> adjustTextAreaSize(textArea));

        return tabPanel;
    }

    /**
     * 根据文本内容自动调整文本区域的大小
     * <p>
     * 该方法会根据文本内容的行数自动调整文本区域的行数，但会设置最小和最大行数限制。
     * 最小行数：15行（初始大小）
     * 最大行数：50行（避免占用过多空间）
     *
     * @param textArea 要调整大小的文本区域
     */
    private void adjustTextAreaSize(JTextArea textArea) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 计算文本的行数
                int lineCount = textArea.getLineCount();

                // 设置最小和最大行数限制
                int minRows = 15;  // 最小行数
                int maxRows = 50;   // 最大行数

                // 计算实际需要的行数（至少显示所有内容，但不超过最大值）
                int rows = Math.max(minRows, Math.min(lineCount, maxRows));

                // 如果行数发生变化，更新文本区域的行数
                if (rows != textArea.getRows()) {
                    textArea.setRows(rows);
                    // 触发父容器重新布局
                    if (textArea.getParent() != null) {
                        textArea.getParent().revalidate();
                    }
                }
            } catch (Exception e) {
                // 静默处理异常，避免影响功能
            }
        });
    }

    /**
     * 将指定类型的提示内容重置为默认模板
     * <p>
     * 根据传入的提示类型，获取对应的默认提示模板，并将其设置到指定的文本区域中。
     *
     * @param promptType 提示类型，如 "system"、"class"、"method" 等
     * @param textArea   要设置默认模板的文本区域组件
     */
    public void resetPromptToDefault(String promptType, JTextArea textArea) {
        String defaultTemplate = switch (promptType) {
            case "system" -> SettingsState.getDefaultSystemPromptTemplate();
            case "class" -> SettingsState.getDefaultClassPromptTemplate();
            case "method" -> SettingsState.getDefaultMethodPromptTemplate();
            case "field" -> SettingsState.getDefaultFieldPromptTemplate();
            case "test" -> SettingsState.getDefaultTestPromptTemplate();
            default -> "";
        };
        textArea.setText(defaultTemplate);
    }

    /**
     * 创建一个带有指定文本区域的滚动面板
     * <p>
     * 该方法用于创建一个 JScrollPane 实例，并设置其首选大小和滚动条策略。
     *
     * @param textArea 要放入滚动面板中的文本区域
     * @return 配置好的滚动面板实例
     */
    private JScrollPane createScrollPane(JTextArea textArea) {
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 150));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    /**
     * 初始化各种监听器，用于响应用户界面组件的变化
     * <p>
     * 该方法为各个输入组件添加动作监听器，当组件内容发生变化时，触发相应的更新或验证状态清除操作。
     * 包括提供商、Base URL、API Key、模型选择以及代码优化配置等变化的监听。
     */
    private void setupListeners() {
        enableCodeCompressionCheckBox.addActionListener(e -> updateMaxClassCodeLinesEnabled());

        showAdvancedSettingsCheckBox.addActionListener(e -> {
            boolean selected = showAdvancedSettingsCheckBox.isSelected();
            advancedSettingsPanel.setVisible(selected);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        showCustomJavaDocTagsCheckBox.addActionListener(e ->
                                                            customJavaDocTagsPanel.setVisible(showCustomJavaDocTagsCheckBox.isSelected())
                                                       );
    }

    @NotNull
    public SettingsState getSettings() {
        SettingsState settings = new SettingsState();

        // 直接从 AIProviderConfigPanel 获取所有 AI 配置
        settings.providerSettings = providerConfigPanel.getSettings();

        settings.generateForClass = generateForClassCheckBox.isSelected();
        settings.generateForMethod = generateForMethodCheckBox.isSelected();
        settings.generateForField = generateForFieldCheckBox.isSelected();
        settings.overrideExisting = overrideExistingCheckBox.isSelected();
        settings.enableCodeCompression = enableCodeCompressionCheckBox.isSelected();
        settings.maxClassCodeLines = (Integer) maxClassCodeLinesSpinner.getValue();
        settings.addSpaceBetweenChineseAndEnglish = addSpaceBetweenChineseAndEnglishCheckBox.isSelected();
        settings.replaceChinesePunctuation = replaceChinesePunctuationCheckBox.isSelected();

        settings.supportedLanguages = new HashSet<>();
        if (javaCheckBox.isSelected()) {
            settings.supportedLanguages.add("java");
        }
        if (kotlinCheckBox.isSelected()) {
            settings.supportedLanguages.add("kotlin");
        }

        settings.systemPromptTemplate = systemPromptTextArea.getText().trim();
        settings.classPromptTemplate = classPromptTextArea.getText().trim();
        settings.methodPromptTemplate = methodPromptTextArea.getText().trim();
        settings.fieldPromptTemplate = fieldPromptTextArea.getText().trim();
        settings.testPromptTemplate = testPromptTextArea.getText().trim();

        settings.customJavaDocTags = customJavaDocTagsTableModel.getData();
        settings.showCustomJavaDocTags = showCustomJavaDocTagsCheckBox.isSelected();
        settings.showAdvancedSettings = showAdvancedSettingsCheckBox.isSelected();

        return settings;
    }

    public String getCurrentApiKey() {
        return providerConfigPanel.getCurrentApiKey();
    }

    @SuppressWarnings("DuplicatedCode")
    public void loadSettings(@NotNull SettingsState settings) {
        // 直接使用 AIProviderConfigPanel 加载所有 AI 配置
        providerConfigPanel.loadSettings(settings.providerSettings);

        generateForClassCheckBox.setSelected(settings.generateForClass);
        generateForMethodCheckBox.setSelected(settings.generateForMethod);
        generateForFieldCheckBox.setSelected(settings.generateForField);
        overrideExistingCheckBox.setSelected(settings.overrideExisting);
        enableCodeCompressionCheckBox.setSelected(settings.enableCodeCompression);
        maxClassCodeLinesSpinner.setValue(settings.maxClassCodeLines);
        addSpaceBetweenChineseAndEnglishCheckBox.setSelected(settings.addSpaceBetweenChineseAndEnglish);
        replaceChinesePunctuationCheckBox.setSelected(settings.replaceChinesePunctuation);

        updateMaxClassCodeLinesEnabled();

        javaCheckBox.setSelected(settings.supportedLanguages.contains("java"));
        kotlinCheckBox.setSelected(settings.supportedLanguages.contains("kotlin"));

        systemPromptTextArea.setText(settings.systemPromptTemplate);
        classPromptTextArea.setText(settings.classPromptTemplate);
        methodPromptTextArea.setText(settings.methodPromptTemplate);
        fieldPromptTextArea.setText(settings.fieldPromptTemplate);
        testPromptTextArea.setText(settings.testPromptTemplate);

        customJavaDocTagsTableModel.setData(settings.customJavaDocTags);
        showCustomJavaDocTagsCheckBox.setSelected(settings.showCustomJavaDocTags);
        customJavaDocTagsPanel.setVisible(settings.showCustomJavaDocTags);

        showAdvancedSettingsCheckBox.setSelected(settings.showAdvancedSettings);
        advancedSettingsPanel.setVisible(settings.showAdvancedSettings);

        updateAllCheckBoxHintColors();
    }

    private java.awt.Window getParentWindow() {
        return SwingUtilities.getWindowAncestor(mainPanel);
    }

    private void addCustomJavaDocTag() {
        String tagName = JOptionPane.showInputDialog(
            getParentWindow(),
            JavaDocBundle.message("settings.custom.javadoc.tags.add.prompt"),
            JavaDocBundle.message("settings.custom.javadoc.tags.add.title"),
            JOptionPane.QUESTION_MESSAGE
                                                    );

        if (tagName != null && !tagName.trim().isEmpty()) {
            tagName = tagName.trim();

            // 验证标签名称
            if (!SettingsState.isValidTagName(tagName)) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavaDocBundle.message("settings.custom.javadoc.tags.invalid.name", tagName),
                    JavaDocBundle.message("settings.error.title"),
                    JOptionPane.ERROR_MESSAGE
                                             );
                return;
            }

            // 检查是否已存在
            List<String> currentTags = customJavaDocTagsTableModel.getData();
            String tagNameLower = tagName.toLowerCase();
            if (currentTags.stream().anyMatch(t -> t.toLowerCase().equals(tagNameLower))) {
                JOptionPane.showMessageDialog(
                    getParentWindow(),
                    JavaDocBundle.message("settings.custom.javadoc.tags.already.exists", tagName),
                    JavaDocBundle.message("settings.error.title"),
                    JOptionPane.WARNING_MESSAGE
                                             );
                return;
            }

            // 添加到表格
            customJavaDocTagsTableModel.addTag(tagName);
        }
    }

    /**
     * 删除自定义 JavaDoc 标签
     */
    private void removeCustomJavaDocTag(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= customJavaDocTagsTableModel.getRowCount()) {
            return;
        }

        String tagName = customJavaDocTagsTableModel.getData().get(selectedRow);

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            JavaDocBundle.message("settings.custom.javadoc.tags.delete.confirm", tagName),
            JavaDocBundle.message("settings.custom.javadoc.tags.delete.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result == JOptionPane.YES_OPTION) {
            customJavaDocTagsTableModel.removeRow(selectedRow);
        }
    }

    /**
     * 清空所有自定义 JavaDoc 标签
     */
    private void clearAllCustomJavaDocTags() {
        if (customJavaDocTagsTableModel.getRowCount() == 0) {
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            getParentWindow(),
            JavaDocBundle.message("settings.custom.javadoc.tags.clear.confirm",
                                  customJavaDocTagsTableModel.getRowCount()),
            JavaDocBundle.message("settings.custom.javadoc.tags.clear.title"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
                                                  );

        if (result == JOptionPane.YES_OPTION) {
            customJavaDocTagsTableModel.clearAll();
        }
    }

    /**
     * 自定义 JavaDoc 标签列表的表格模型
     */
    private static class CustomJavaDocTagsTableModel extends AbstractTableModel {
        private final String[] columnNames = {JavaDocBundle.message("settings.custom.javadoc.tags.column.name")};
        private final List<String> data;

        public CustomJavaDocTagsTableModel() {
            this.data = new ArrayList<>();
        }

        public void setData(List<String> newData) {
            this.data.clear();
            if (newData != null) {
                this.data.addAll(newData);
            }
            fireTableDataChanged();
        }

        public List<String> getData() {
            return new ArrayList<>(data);
        }

        public void addTag(String tagName) {
            data.add(tagName);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public void removeRow(int row) {
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        public void clearAll() {
            int size = data.size();
            if (size > 0) {
                data.clear();
                fireTableRowsDeleted(0, size - 1);
            }
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < data.size()) {
                return data.get(rowIndex);
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; // 允许编辑标签名称
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < data.size() && aValue != null) {
                String newTagName = aValue.toString().trim();

                // 验证标签名称
                if (!SettingsState.isValidTagName(newTagName)) {
                    // 可以显示错误提示，这里简单处理为不更新
                    return;
                }

                // 检查是否与其他标签重复
                String newTagNameLower = newTagName.toLowerCase();
                for (int i = 0; i < data.size(); i++) {
                    if (i != rowIndex && data.get(i).toLowerCase().equals(newTagNameLower)) {
                        // 重复标签，不更新
                        return;
                    }
                }

                data.set(rowIndex, newTagName);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

}

