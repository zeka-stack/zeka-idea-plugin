package dev.dong4j.zeka.stack.idea.javadoc.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import dev.dong4j.zeka.stack.idea.javadoc.settings.OverrideMode;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.PanelUtil;

/**
 * 生成规则配置面板
 * <p>
 * 提供 Javadoc 生成规则的配置界面，包括生成选项、覆盖已有注释、代码压缩配置、
 * 文本格式化选项和性能模式配置等。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.4.0
 */
public class GenerationRulesPanel {

    /** 生成针对类的复选框, 用于控制是否为类生成文档注释 */
    private JBCheckBox generateForClassCheckBox;

    /** 方法生成复选框, 用于控制是否为方法生成代码 */
    private JBCheckBox generateForMethodCheckBox;

    /** 生成字段的复选框, 用于控制是否为字段生成文档注释. */
    private JBCheckBox generateForFieldCheckBox;

    /** 覆盖已有注释复选框 */
    private JBCheckBox overrideExistingCheckBox;

    /**
     * 覆写模式子配置面板容器
     * <p>
     * 用于存放覆写模式相关的配置项, 包括单选框和提示词输入区域.
     * 默认隐藏, 根据用户选择的覆写模式显示或隐藏.
     */
    private JPanel overrideModeSubConfigPanel;

    /** 仅修复错误注释单选框 */
    private JRadioButton fixModeRadioButton;

    /** 删除原注释并重新生成单选框 */
    private JRadioButton replaceModeRadioButton;

    /**
     * 覆写模式按钮组
     * <p>
     * 用于管理覆写模式的单选框 (仅修复错误注释 / 删除原注释并重新生成).
     */
    private ButtonGroup overrideModeButtonGroup;

    /** 修复错误 Javadoc 提示词文本区域, 用于编辑自定义的提示模板内容 */
    private JTextArea fixJavadocPromptTextArea;

    /** 修复错误 Javadoc 提示词面板容器 */
    private JPanel fixJavadocPromptPanel;

    /** 修复错误 Javadoc 提示词面板的内容面板（用于折叠/展开） */
    private JPanel fixJavadocPromptContentPanel;

    /** 修复错误 Javadoc 提示词面板的标题面板（用于更新箭头） */
    private JPanel fixJavadocPromptTitlePanel;

    /** 修复错误 Javadoc 提示词面板的标题文本 */
    private String fixJavadocPromptTitleText;

    /** 启用类级上下文的复选框 */
    private JBCheckBox enableGenerationContextCheckBox;

    /** 启用代码压缩的复选框 */
    private JBCheckBox enableCodeCompressionCheckBox;

    /** 在中英文间添加空格复选框 */
    private JBCheckBox compressSingleLineJavaDocCheckBox;

    private JBCheckBox addSpaceBetweenChineseAndEnglishCheckBox;

    /** 将中文标点符号转为英文标点符号复选框 */
    private JBCheckBox replaceChinesePunctuationCheckBox;

    /** 最大类代码行数设置控件 */
    private JSpinner maxClassCodeLinesSpinner;

    /** 性能模式复选框 */
    private JBCheckBox performanceModeCheckBox;

    /** 显示任务统计复选框 */
    private JBCheckBox showProviderStatisticsCheckBox;

    /** 类代码最大行数标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesLabel;

    /** 主面板 */
    private JPanel panel;


    /**
     * 构造函数
     */
    public GenerationRulesPanel() {
        createUI();
        setupListeners();
    }

    /**
     * 创建 UI
     */
    @SuppressWarnings("DuplicatedCode")
    private void createUI() {
        // 功能配置
        generateForClassCheckBox = new JBCheckBox(JavadocBundle.message("settings.generate.for.class"));
        generateForMethodCheckBox = new JBCheckBox(JavadocBundle.message("settings.generate.for.method"));
        generateForFieldCheckBox = new JBCheckBox(JavadocBundle.message("settings.generate.for.field"));
        overrideExistingCheckBox = new JBCheckBox(JavadocBundle.message("settings.override.existing"));
        enableGenerationContextCheckBox = new JBCheckBox(JavadocBundle.message("settings.enable.generation.context"));
        enableCodeCompressionCheckBox = new JBCheckBox(JavadocBundle.message("settings.enable.code.compression"));
        maxClassCodeLinesSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 300000, 100));
        compressSingleLineJavaDocCheckBox = new JBCheckBox(JavadocBundle.message("settings.compress.single.line.javadoc"));
        addSpaceBetweenChineseAndEnglishCheckBox = new JBCheckBox(JavadocBundle.message("settings.add.space.between.chinese.and.english"));
        replaceChinesePunctuationCheckBox = new JBCheckBox(JavadocBundle.message("settings.replace.chinese.punctuation"));

        performanceModeCheckBox = new JBCheckBox(JavadocBundle.message("settings.performance.mode"));
        showProviderStatisticsCheckBox = new JBCheckBox(JavadocBundle.message("settings.show.provider.statistics"));

        // 创建内容面板
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createGenerationOptionsPanel())
            .addComponent(createCheckBoxWithHint(overrideExistingCheckBox, "settings.override.existing.hint"))
            .addComponent(createOverrideModeSubConfigPanel())
            .addComponent(createCheckBoxWithHint(enableGenerationContextCheckBox, "settings.enable.generation.context.hint"))
            .addComponent(createCheckBoxWithHint(enableCodeCompressionCheckBox, "settings.enable.code.compression.hint"))
            .addComponent(createCodeCompressionSubConfigPanel())
            .addComponent(
                createCheckBoxWithHint(compressSingleLineJavaDocCheckBox,
                                       "settings.compress.single.line.javadoc.hint"))
            .addComponent(
                createCheckBoxWithHint(addSpaceBetweenChineseAndEnglishCheckBox,
                                       "settings.add.space.between.chinese.and.english.hint"))
            .addComponent(createCheckBoxWithHint(replaceChinesePunctuationCheckBox,
                                                 "settings.replace.chinese.punctuation.hint"))
            .addComponent(createCheckBoxWithHint(performanceModeCheckBox, "settings.performance.mode.hint"))
            .addComponent(createPerformanceModeSubConfigPanel())
            .getPanel();

        // 创建带边框的面板
        panel = PanelUtil.createBorderPanel(contentPanel, "settings.generation.rules.config");
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        enableCodeCompressionCheckBox.addActionListener(e -> updateMaxClassCodeLinesEnabled());
        performanceModeCheckBox.addActionListener(e -> updatePerformanceModeSubConfigEnabled());
        overrideExistingCheckBox.addActionListener(e -> updateOverrideModeSubConfigVisibility());
        fixModeRadioButton.addActionListener(e -> updateFixJavadocPromptVisibility());
        replaceModeRadioButton.addActionListener(e -> updateFixJavadocPromptVisibility());
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
     * 获取设置
     *
     * @param settings 设置对象，将读取的值填充到此对象中
     */
    public void getSettings(@NotNull SettingsState settings) {
        settings.generateForClass = generateForClassCheckBox.isSelected();
        settings.generateForMethod = generateForMethodCheckBox.isSelected();
        settings.generateForField = generateForFieldCheckBox.isSelected();
        settings.overrideExisting = overrideExistingCheckBox.isSelected();
        if (fixModeRadioButton.isSelected()) {
            settings.overrideMode = OverrideMode.FIX;
        } else if (replaceModeRadioButton.isSelected()) {
            settings.overrideMode = OverrideMode.REPLACE;
        }
        settings.fixJavadocPromptTemplate = fixJavadocPromptTextArea.getText().trim();
        settings.enableGenerationContext = enableGenerationContextCheckBox.isSelected();
        settings.enableCodeCompression = enableCodeCompressionCheckBox.isSelected();
        settings.maxClassCodeLines = (Integer) maxClassCodeLinesSpinner.getValue();
        settings.compressSingleLineJavaDoc = compressSingleLineJavaDocCheckBox.isSelected();
        settings.addSpaceBetweenChineseAndEnglish = addSpaceBetweenChineseAndEnglishCheckBox.isSelected();
        settings.replaceChinesePunctuation = replaceChinesePunctuationCheckBox.isSelected();

        settings.performanceMode = performanceModeCheckBox.isSelected();
        settings.showProviderStatistics = showProviderStatisticsCheckBox.isSelected();
    }

    /**
     * 加载设置
     *
     * @param settings 设置对象
     */
    public void loadSettings(@NotNull SettingsState settings) {
        generateForClassCheckBox.setSelected(settings.generateForClass);
        generateForMethodCheckBox.setSelected(settings.generateForMethod);
        generateForFieldCheckBox.setSelected(settings.generateForField);
        overrideExistingCheckBox.setSelected(settings.overrideExisting);
        if (settings.overrideMode == OverrideMode.FIX) {
            fixModeRadioButton.setSelected(true);
        } else {
            replaceModeRadioButton.setSelected(true);
        }
        fixJavadocPromptTextArea.setText(settings.fixJavadocPromptTemplate);
        enableGenerationContextCheckBox.setSelected(settings.enableGenerationContext);
        enableCodeCompressionCheckBox.setSelected(settings.enableCodeCompression);
        maxClassCodeLinesSpinner.setValue(settings.maxClassCodeLines);
        compressSingleLineJavaDocCheckBox.setSelected(settings.compressSingleLineJavaDoc);
        addSpaceBetweenChineseAndEnglishCheckBox.setSelected(settings.addSpaceBetweenChineseAndEnglish);
        replaceChinesePunctuationCheckBox.setSelected(settings.replaceChinesePunctuation);

        performanceModeCheckBox.setSelected(settings.performanceMode);
        showProviderStatisticsCheckBox.setSelected(settings.showProviderStatistics);

        updateMaxClassCodeLinesEnabled();
        updatePerformanceModeSubConfigEnabled();
        updateOverrideModeSubConfigVisibility();
        updateFixJavadocPromptVisibility();
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
     * 创建包含复选框和提示文本的面板
     * <p>
     * 该方法用于创建一个包含复选框和提示文本的面板，提示文本通过指定的键从资源文件中获取。
     * 提示文本默认显示为灰色。
     *
     * @param comp    要添加到面板中的复选框
     * @param hintKey 用于获取提示文本的资源键
     * @return 包含复选框和提示文本的面板
     */
    public JPanel createCheckBoxWithHint(Component comp, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // 复选框放在左侧
        panel.add(comp, BorderLayout.WEST);

        // 提示文本放在右侧，默认显示为灰色
        JBLabel hintLabel = new JBLabel(JavadocBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setPreferredSize(new Dimension(300, hintLabel.getPreferredSize().height));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        panel.add(hintLabel, BorderLayout.CENTER);

        return panel;
    }

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
        maxClassCodeLinesLabel = new JBLabel(JavadocBundle.message("settings.max.class.code.lines"));

        // 创建包含标签、输入框和提示的面板
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(maxClassCodeLinesLabel, BorderLayout.WEST);

        // 创建输入框和提示的面板
        JPanel spinnerPanel = createCheckBoxWithHint(maxClassCodeLinesSpinner, "settings.max.class.code.lines.hint");
        maxClassCodeLinesSpinner.setPreferredSize(new Dimension(120, maxClassCodeLinesSpinner.getPreferredSize().height));
        spinnerPanel.add(maxClassCodeLinesSpinner, BorderLayout.WEST);
        contentPanel.add(spinnerPanel, BorderLayout.CENTER);
        indentPanel.add(contentPanel, BorderLayout.CENTER);
        panel.add(indentPanel, BorderLayout.CENTER);

        // 初始状态：根据代码压缩复选框的状态设置可用性
        updateMaxClassCodeLinesEnabled();

        return panel;
    }

    /**
     * 创建性能模式子配置面板
     * <p>
     * 该方法构建一个用于配置性能模式相关选项的面板，包含显示任务统计复选框。
     * 需要向右缩进2个空格（约22像素）。
     *
     * @return 返回配置好的性能模式子配置面板
     */
    private JPanel createPerformanceModeSubConfigPanel() {
        // 性能模式的子配置面板，包含显示任务统计
        // 需要向右缩进2个空格（约22像素）
        JPanel indentPanel = new JPanel(new BorderLayout());
        indentPanel.setBorder(JBUI.Borders.emptyLeft(20));

        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(showProviderStatisticsCheckBox, "settings.show.provider.statistics.hint"))
            .getPanel();

        indentPanel.add(contentPanel, BorderLayout.CENTER);
        return indentPanel;
    }

    /**
     * 创建水平排列的复选框面板
     *
     * @param checkBoxes  复选框数组
     * @param hintKeys    对应的提示文本键数组
     * @param itemsPerRow 每行显示的复选框数量
     * @return 水平排列的复选框面板
     */
    @SuppressWarnings("SameParameterValue")
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
                JBLabel hintLabel = new JBLabel(JavadocBundle.message(hintKeys[i]));
                hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
                hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
                checkBoxPanel.add(hintLabel, BorderLayout.CENTER);
            }

            mainPanel.add(checkBoxPanel, gbc);
        }

        return mainPanel;
    }

    /**
     * 更新类代码最大行数输入框的可用性
     * <p>
     * 根据代码压缩复选框的状态，设置类代码最大行数输入框和标签的可用性。
     * 提示文本始终保持灰色显示，不随可用性变化。
     */
    private void updateMaxClassCodeLinesEnabled() {
        boolean enabled = enableCodeCompressionCheckBox.isSelected();
        maxClassCodeLinesSpinner.setEnabled(enabled);
        if (maxClassCodeLinesLabel != null) {
            maxClassCodeLinesLabel.setEnabled(enabled);
        }
        // 提示文本保持灰色，不再根据可用性更新颜色
    }

    /**
     * 更新性能模式子配置的启用状态
     * <p>
     * 根据性能模式复选框的状态, 启用或禁用相关配置项.
     * 提示文本始终保持灰色显示，不随可用性变化。
     */
    private void updatePerformanceModeSubConfigEnabled() {
        boolean enabled = performanceModeCheckBox.isSelected();
        showProviderStatisticsCheckBox.setEnabled(enabled);
        if (!enabled) {
            showProviderStatisticsCheckBox.setSelected(false);
        }
        // 提示文本保持灰色，不再根据可用性更新颜色
    }

    /**
     * 创建覆写模式子配置面板
     * <p>
     * 该方法构建一个用于配置覆写模式相关选项的面板，包含两个单选框：
     * - 仅修复错误注释
     * - 删除原注释并重新生成
     * 需要向右缩进2个空格（约22像素）。
     * 当选择"仅修复错误注释"时，显示修复错误 Javadoc 提示词输入框。
     *
     * @return 返回配置好的覆写模式子配置面板
     */
    private JPanel createOverrideModeSubConfigPanel() {
        // 覆写模式的子配置面板，包含两个单选框
        // 需要向右缩进2个空格（约22像素）
        JPanel indentPanel = new JPanel(new BorderLayout());
        indentPanel.setBorder(JBUI.Borders.emptyLeft(22));

        // 创建单选框
        fixModeRadioButton = new JRadioButton(JavadocBundle.message("settings.override.mode.fix"));
        replaceModeRadioButton = new JRadioButton(JavadocBundle.message("settings.override.mode.replace"));

        // 创建按钮组
        overrideModeButtonGroup = new ButtonGroup();
        overrideModeButtonGroup.add(fixModeRadioButton);
        overrideModeButtonGroup.add(replaceModeRadioButton);

        // 默认选择"删除原注释并重新生成"
        replaceModeRadioButton.setSelected(true);

        // 创建单选框面板（第一个单选框）
        JPanel firstRadioPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(fixModeRadioButton, "settings.override.mode.fix.hint"))
            .getPanel();

        // 创建修复错误 Javadoc 提示词面板（可折叠）
        fixJavadocPromptPanel = createFixJavadocPromptPanel();

        // 创建单选框面板（第二个单选框）
        JPanel secondRadioPanel = FormBuilder.createFormBuilder()
            .addComponent(createCheckBoxWithHint(replaceModeRadioButton, "settings.override.mode.replace.hint"))
            .getPanel();

        // 创建内容面板，包含单选框和提示词面板（提示词面板在两个单选框中间）
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(firstRadioPanel)
            .addComponent(fixJavadocPromptPanel)
            .addComponent(secondRadioPanel)
            .getPanel();

        indentPanel.add(contentPanel, BorderLayout.CENTER);

        // 创建容器面板
        overrideModeSubConfigPanel = new JPanel(new BorderLayout());
        overrideModeSubConfigPanel.add(indentPanel, BorderLayout.CENTER);
        overrideModeSubConfigPanel.setVisible(false); // 默认隐藏

        return overrideModeSubConfigPanel;
    }

    /**
     * 创建修复错误 Javadoc 提示词面板（可折叠）
     * <p>
     * 该方法创建一个可折叠的面板，包含文本区域和重置按钮，用于编辑修复错误 Javadoc 的提示词。
     * 默认折叠状态，只有在选择"仅修复错误注释"时才能展开（但不会自动展开）。
     *
     * @return 返回配置好的修复错误 Javadoc 提示词面板
     */
    private JPanel createFixJavadocPromptPanel() {
        // 创建带边框的容器面板（边框会包围整个区域，包括标题和内容）
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());

        // 创建标题文本（用于边框标题）
        fixJavadocPromptTitleText = JavadocBundle.message("settings.fix.javadoc.prompt");

        // 创建标题面板（不带边框，因为边框在容器上）
        fixJavadocPromptTitlePanel = new JPanel(new BorderLayout());
        fixJavadocPromptTitlePanel.setBorder(JBUI.Borders.empty(5));
        fixJavadocPromptTitlePanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        fixJavadocPromptTitlePanel.setOpaque(false);

        // 创建文本区域
        fixJavadocPromptTextArea = new JTextArea(15, 15);
        fixJavadocPromptTextArea.setLineWrap(true);
        fixJavadocPromptTextArea.setWrapStyleWord(true);
        fixJavadocPromptTextArea.setToolTipText(JavadocBundle.message("settings.fix.javadoc.prompt.hint"));

        // 添加文档监听器，根据内容自动调整大小
        fixJavadocPromptTextArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(fixJavadocPromptTextArea);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(fixJavadocPromptTextArea);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(fixJavadocPromptTextArea);
            }
        });

        // 创建滚动面板
        JBScrollPane scrollPane = new JBScrollPane(fixJavadocPromptTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        // 创建重置按钮
        JButton resetButton = new JButton(JavadocBundle.message("settings.fix.javadoc.prompt.reset"));
        resetButton.addActionListener(e -> resetFixJavadocPromptToDefault());

        // 创建主内容面板（不包含标签，因为标题已经在可折叠标题栏中显示）
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(resetButton, BorderLayout.SOUTH);
        mainPanel.setBorder(JBUI.Borders.empty(5, 0));

        // 添加左侧缩进（再缩进2个空格，约22像素）
        fixJavadocPromptContentPanel = new JPanel(new BorderLayout());
        fixJavadocPromptContentPanel.setBorder(JBUI.Borders.emptyLeft(22));
        fixJavadocPromptContentPanel.add(mainPanel, BorderLayout.CENTER);

        // 默认折叠：隐藏内容面板
        fixJavadocPromptContentPanel.setVisible(false);

        // 使用包装面板确保内容居中
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(fixJavadocPromptContentPanel, BorderLayout.NORTH);
        contentWrapper.setOpaque(false);

        // 将标题栏和内容面板添加到容器面板
        container.add(fixJavadocPromptTitlePanel, BorderLayout.NORTH);
        container.add(contentWrapper, BorderLayout.CENTER);

        // 为容器设置 TitledBorder（边框会包围整个区域）
        TitledBorder containerBorder = BorderFactory.createTitledBorder("▶ " + fixJavadocPromptTitleText);
        configureTitledBorder(containerBorder);
        container.setBorder(BorderFactory.createCompoundBorder(
            containerBorder,
            JBUI.Borders.empty(5)
                                                              ));
        container.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        container.setOpaque(true);
        container.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());

        // 为容器添加点击事件（整个容器都可以点击）
        container.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 只有在选择"仅修复错误注释"时才能展开
                if (!fixModeRadioButton.isSelected()) {
                    return;
                }
                toggleFixJavadocPromptPanel();
            }
        });

        // 初始化时根据内容调整大小
        SwingUtilities.invokeLater(() -> adjustTextAreaSize(fixJavadocPromptTextArea));

        return container;
    }

    /**
     * 创建可折叠的标题栏
     *
     * @param title 标题文本（包含箭头）
     * @return 标题栏面板
     */
    private JPanel createCollapsibleTitle(@NotNull String title) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        // 默认折叠状态，使用右箭头
        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
        configureTitledBorder(titledBorder);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            JBUI.Borders.empty(5)
                                                               ));
        titlePanel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        titlePanel.setOpaque(true);
        titlePanel.setBackground(com.intellij.util.ui.UIUtil.getPanelBackground());
        return titlePanel;
    }

    /**
     * 更新可折叠标题栏的箭头图标
     *
     * @param titlePanel 标题栏面板
     * @param title      标题文本（不包含箭头）
     * @param expanded   是否展开
     */
    private void updateCollapsibleTitle(@NotNull JPanel titlePanel, @NotNull String title, boolean expanded) {
        String arrow = expanded ? "▼ " : "▶ ";
        TitledBorder titledBorder = BorderFactory.createTitledBorder(arrow + title);
        configureTitledBorder(titledBorder);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
            titledBorder,
            JBUI.Borders.empty(5)
                                                               ));
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
        Color titleColor = com.intellij.util.ui.UIUtil.getLabelForeground();
        titledBorder.setTitleColor(titleColor);
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
     * 将修复错误 Javadoc 提示词重置为默认模板
     */
    private void resetFixJavadocPromptToDefault() {
        fixJavadocPromptTextArea.setText(SettingsState.getDefaultFixJavadocPromptTemplate());
    }

    /**
     * 更新覆写模式子配置的显示/隐藏状态
     * <p>
     * 根据覆写注释复选框的状态，显示或隐藏覆写模式子配置面板。
     */
    private void updateOverrideModeSubConfigVisibility() {
        boolean visible = overrideExistingCheckBox.isSelected();
        overrideModeSubConfigPanel.setVisible(visible);
        if (panel.getParent() != null) {
            panel.getParent().revalidate();
            panel.getParent().repaint();
        }
        // 同时更新提示词面板的显示状态
        updateFixJavadocPromptVisibility();
    }

    /**
     * 更新修复错误 Javadoc 提示词面板的显示/隐藏状态
     * <p>
     * 根据覆写注释复选框和覆写模式单选框的状态，显示或隐藏修复错误 Javadoc 提示词面板。
     * 只有当覆写注释复选框被勾选且选择了"仅修复错误注释"时才显示。
     * 注意：面板显示时仍然是折叠状态，需要用户手动点击展开。
     * 当切换到"删除原注释并重新生成"时，如果面板已展开，会自动折叠。
     */
    private void updateFixJavadocPromptVisibility() {
        boolean visible = overrideExistingCheckBox.isSelected() && fixModeRadioButton.isSelected();
        fixJavadocPromptPanel.setVisible(visible);

        // 如果切换到"删除原注释并重新生成"，且面板已展开，则自动折叠并更新边框
        if (!fixModeRadioButton.isSelected() && fixJavadocPromptContentPanel != null && fixJavadocPromptContentPanel.isVisible()) {
            fixJavadocPromptContentPanel.setVisible(false);
            // 更新容器的边框（因为边框在容器上）
            if (fixJavadocPromptPanel != null && fixJavadocPromptTitleText != null) {
                TitledBorder containerBorder = BorderFactory.createTitledBorder("▶ " + fixJavadocPromptTitleText);
                configureTitledBorder(containerBorder);
                fixJavadocPromptPanel.setBorder(BorderFactory.createCompoundBorder(
                    containerBorder,
                    JBUI.Borders.empty(5)
                                                                                  ));
            }
        }

        if (panel.getParent() != null) {
            panel.getParent().revalidate();
            panel.getParent().repaint();
        }
    }

    /**
     * 切换修复错误 Javadoc 提示词面板的展开/折叠状态
     */
    private void toggleFixJavadocPromptPanel() {
        if (fixJavadocPromptContentPanel == null || fixJavadocPromptPanel == null || fixJavadocPromptTitleText == null) {
            return;
        }
        boolean isVisible = fixJavadocPromptContentPanel.isVisible();
        fixJavadocPromptContentPanel.setVisible(!isVisible);

        // 更新容器的 TitledBorder（因为边框在容器上）
        String arrow = !isVisible ? "▼ " : "▶ ";
        TitledBorder containerBorder = BorderFactory.createTitledBorder(arrow + fixJavadocPromptTitleText);
        configureTitledBorder(containerBorder);
        fixJavadocPromptPanel.setBorder(BorderFactory.createCompoundBorder(
            containerBorder,
            JBUI.Borders.empty(5)
                                                                          ));

        if (fixJavadocPromptPanel.getParent() != null) {
            fixJavadocPromptPanel.revalidate();
            fixJavadocPromptPanel.repaint();
        }
    }
}

