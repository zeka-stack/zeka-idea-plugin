package dev.dong4j.zeka.stack.idea.javadoc.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;

/**
 * 类 Javadoc 模板面板
 * <p>
 * 提供类 Javadoc 模板的配置界面，允许用户启用/禁用类 Javadoc 模板功能，
 * 并编辑模板内容。支持重置为默认值功能。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.4.0
 */
public class ClassJavadocTemplatePanel {

    /**
     * 启用类 Javadoc 模板的复选框
     * <p>
     * 用于控制是否启用类 Javadoc 模板功能.
     */
    private JBCheckBox enableClassJavaDocTemplateCheckBox;

    /**
     * 类 Javadoc 模板文本区域
     * <p>
     * 用于编辑类 Javadoc 模板内容的文本区域, 支持换行和单词换行.
     *
     */
    private JTextArea classJavaDocTemplateTextArea;

    /** 类 Javadoc 模板编辑器面板 (包含文本区域和重置按钮) */
    private JPanel classJavaDocTemplateEditorPanel;

    /**
     * 类 Javadoc 模板提示标签
     *
     * @see #getClassJavaDocTemplateHintLabel()
     */
    private JBLabel classJavaDocTemplateHintLabel;

    /**
     * 主面板
     * <p>
     * 用于存放类 Javadoc 模板配置界面的主要容器面板, 包含启用模板的复选框,
     * 模板内容编辑区域以及相关提示信息.
     *
     */
    private JPanel panel;

    /** 构造函数, 初始化类 Javadoc 模板配置面板 */
    public ClassJavadocTemplatePanel() {
        createUI();
        setupListeners();
    }

    /**
     * 创建 UI
     * <p>
     * 初始化并配置用户界面组件, 包括启用类 Javadoc 模板复选框, 类 Javadoc 模板文本区域, 提示标签和主面板.
     *
     * @since 1.4.0
     */
    private void createUI() {
        // 创建启用类 Javadoc 模板复选框
        enableClassJavaDocTemplateCheckBox = new JBCheckBox(JavadocBundle.message("settings.enable.class.javadoc.template"));

        // 创建类 Javadoc 模板文本区域
        classJavaDocTemplateTextArea = new JTextArea(10, 10);
        classJavaDocTemplateTextArea.setLineWrap(true);
        classJavaDocTemplateTextArea.setWrapStyleWord(true);
        classJavaDocTemplateTextArea.setToolTipText(JavadocBundle.message("settings.class.javadoc.template.hint"));

        // 创建类 Javadoc 模板编辑器面板
        classJavaDocTemplateEditorPanel = createClassJavaDocTemplateEditorPanel();
        classJavaDocTemplateEditorPanel.setVisible(false); // 默认隐藏

        // 创建提示标签
        classJavaDocTemplateHintLabel = new JBLabel(JavadocBundle.message("settings.enable.class.javadoc.template.hint"));
        classJavaDocTemplateHintLabel.setFont(classJavaDocTemplateHintLabel.getFont().deriveFont(classJavaDocTemplateHintLabel.getFont().getSize() - 1f));
        classJavaDocTemplateHintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        classJavaDocTemplateHintLabel.setBorder(JBUI.Borders.emptyLeft(22)); // 与复选框对齐

        // 构建主面板
        panel = new JPanel(new BorderLayout());
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(enableClassJavaDocTemplateCheckBox, BorderLayout.NORTH);
        contentPanel.add(classJavaDocTemplateHintLabel, BorderLayout.CENTER);
        contentPanel.add(classJavaDocTemplateEditorPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        // 初始可见性
        classJavaDocTemplateEditorPanel.setVisible(false);
        classJavaDocTemplateHintLabel.setVisible(false);
    }

    /**
     * 设置监听器
     * <p>
     * 为启用类 Javadoc 模板的复选框添加动作监听器, 当复选框状态改变时,
     * 控制类 Javadoc 模板编辑器面板和提示标签的可见性.
     */
    private void setupListeners() {
        enableClassJavaDocTemplateCheckBox.addActionListener(e -> {
            boolean selected = enableClassJavaDocTemplateCheckBox.isSelected();
            classJavaDocTemplateEditorPanel.setVisible(selected);
            if (classJavaDocTemplateHintLabel != null) {
                classJavaDocTemplateHintLabel.setVisible(selected);
            }
        });
    }

    /**
     * 创建类 Javadoc 模板编辑器面板
     * <p>
     * 创建一个包含模板文本区域和重置按钮的面板, 参考提示词的展示方式.
     *
     * @return 类 Javadoc 模板编辑器面板
     */
    private JPanel createClassJavaDocTemplateEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 创建滚动面板，并添加边框以在四周留出空间
        JBScrollPane scrollPane = new JBScrollPane(classJavaDocTemplateTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // 添加边框，在四周留出10像素的空间，并添加左侧缩进（22像素，与复选框对齐）
        scrollPane.setBorder(JBUI.Borders.empty(10));
        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setBorder(JBUI.Borders.emptyLeft(22)); // 与复选框对齐
        scrollWrapper.add(scrollPane, BorderLayout.CENTER);

        panel.add(scrollWrapper, BorderLayout.CENTER);

        // 创建重置按钮（参考提示词模板的样式）
        JButton resetButton = new JButton(JavadocBundle.message("settings.class.javadoc.template.reset"));
        resetButton.addActionListener(e -> resetClassJavaDocTemplateToDefault());
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(JBUI.Borders.emptyLeft(22)); // 与复选框对齐
        buttonPanel.add(resetButton, BorderLayout.CENTER); // 按钮占据整个宽度
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
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
     * @param settings 设置对象, 将读取的值填充到此对象中
     */
    public void getSettings(@NotNull SettingsState settings) {
        settings.enableClassJavaDocTemplate = enableClassJavaDocTemplateCheckBox.isSelected();
        settings.classJavaDocTemplate = classJavaDocTemplateTextArea.getText().trim();
    }

    /**
     * 加载设置
     *
     * @param settings 设置对象
     */
    public void loadSettings(@NotNull SettingsState settings) {
        enableClassJavaDocTemplateCheckBox.setSelected(settings.enableClassJavaDocTemplate);
        if (settings.classJavaDocTemplate != null && !settings.classJavaDocTemplate.isEmpty()) {
            classJavaDocTemplateTextArea.setText(settings.classJavaDocTemplate);
        } else {
            // 如果设置中没有模板内容，使用默认模板
            SettingsState defaultSettings = new SettingsState();
            classJavaDocTemplateTextArea.setText(defaultSettings.classJavaDocTemplate);
        }
        classJavaDocTemplateEditorPanel.setVisible(settings.enableClassJavaDocTemplate);
        // 提示语也随复选框状态显示/隐藏
        if (classJavaDocTemplateHintLabel != null) {
            classJavaDocTemplateHintLabel.setVisible(settings.enableClassJavaDocTemplate);
        }
    }

    /**
     * 将类 Javadoc 模板重置为默认值
     * <p>
     * 从 SettingsState 获取默认的类 Javadoc 模板内容，并将其设置到模板文本区域中。
     */
    private void resetClassJavaDocTemplateToDefault() {
        SettingsState defaultSettings = new SettingsState();
        classJavaDocTemplateTextArea.setText(defaultSettings.classJavaDocTemplate);
    }
}

