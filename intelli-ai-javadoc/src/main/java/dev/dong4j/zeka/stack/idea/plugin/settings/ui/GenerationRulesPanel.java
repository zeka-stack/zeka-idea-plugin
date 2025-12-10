package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;

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

    /** 类代码最大行数提示标签，用于控制其可用性 */
    private JBLabel maxClassCodeLinesHintLabel;

    /** 主面板 */
    private JPanel panel;

    /** 存储复选框和提示标签的映射关系，用于更新提示文本颜色 */
    private final java.util.Map<JBCheckBox, JBLabel> checkBoxHintLabelMap = new java.util.HashMap<>();

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
    private void createUI() {
        // 功能配置
        generateForClassCheckBox = new JBCheckBox(JavadocBundle.message("settings.generate.for.class"));
        generateForMethodCheckBox = new JBCheckBox(JavadocBundle.message("settings.generate.for.method"));
        generateForFieldCheckBox = new JBCheckBox(JavadocBundle.message("settings.generate.for.field"));
        overrideExistingCheckBox = new JBCheckBox(JavadocBundle.message("settings.override.existing"));
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
        enableCodeCompressionCheckBox.setSelected(settings.enableCodeCompression);
        maxClassCodeLinesSpinner.setValue(settings.maxClassCodeLines);
        compressSingleLineJavaDocCheckBox.setSelected(settings.compressSingleLineJavaDoc);
        addSpaceBetweenChineseAndEnglishCheckBox.setSelected(settings.addSpaceBetweenChineseAndEnglish);
        replaceChinesePunctuationCheckBox.setSelected(settings.replaceChinesePunctuation);

        performanceModeCheckBox.setSelected(settings.performanceMode);
        showProviderStatisticsCheckBox.setSelected(settings.showProviderStatistics);

        updateMaxClassCodeLinesEnabled();
        updatePerformanceModeSubConfigEnabled();
        updateAllCheckBoxHintColors();
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
    public JPanel createCheckBoxWithHint(JBCheckBox checkBox, String hintKey) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));

        // 复选框放在左侧
        panel.add(checkBox, BorderLayout.WEST);

        // 提示文本放在右侧
        JBLabel hintLabel = new JBLabel(JavadocBundle.message(hintKey));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setPreferredSize(new Dimension(400, hintLabel.getPreferredSize().height));

        // 保存映射关系，用于后续更新颜色
        checkBoxHintLabelMap.put(checkBox, hintLabel);

        // 根据复选框状态设置提示文本颜色
        updateHintLabelColor(hintLabel, checkBox.isSelected());

        // 监听复选框状态变化，动态更新提示文本颜色
        // 但性能模式的监听器在 setupListeners 中添加，因为它需要特殊处理
        if (checkBox != performanceModeCheckBox) {
            checkBox.addActionListener(e -> updateHintLabelColor(hintLabel, checkBox.isSelected()));
        }

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

        // 创建提示标签
        maxClassCodeLinesHintLabel = new JBLabel(JavadocBundle.message("settings.max.class.code.lines.hint"));
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
     * 更新性能模式子配置的启用状态
     * <p>
     * 根据性能模式复选框的状态, 启用或禁用相关配置项, 并更新提示标签的显示状态和颜色.
     */
    private void updatePerformanceModeSubConfigEnabled() {
        boolean enabled = performanceModeCheckBox.isSelected();
        showProviderStatisticsCheckBox.setEnabled(enabled);
        if (!enabled) {
            showProviderStatisticsCheckBox.setSelected(false);
        }

        // 更新提示文本颜色
        JBLabel statisticsHintLabel = checkBoxHintLabelMap.get(showProviderStatisticsCheckBox);
        if (statisticsHintLabel != null) {
            if (enabled) {
                updateHintLabelColor(statisticsHintLabel, showProviderStatisticsCheckBox.isSelected());
            } else {
                statisticsHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
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
}

