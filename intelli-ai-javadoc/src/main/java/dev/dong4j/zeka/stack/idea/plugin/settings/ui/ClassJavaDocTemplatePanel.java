package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

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

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;

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
public class ClassJavaDocTemplatePanel {

    /** 启用类 Javadoc 模板复选框 */
    private JBCheckBox enableClassJavaDocTemplateCheckBox;

    /** 类 Javadoc 模板文本区域 */
    private JTextArea classJavaDocTemplateTextArea;

    /** 类 Javadoc 模板编辑器面板（包含文本区域和重置按钮） */
    private JPanel classJavaDocTemplateEditorPanel;

    /** 主面板 */
    private JPanel panel;

    /**
     * 构造函数
     */
    public ClassJavaDocTemplatePanel() {
        createUI();
        setupListeners();
    }

    /**
     * 创建 UI
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
        JBLabel hintLabel = new JBLabel(JavadocBundle.message("settings.enable.class.javadoc.template.hint"));
        hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize() - 2.0f));
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        // 构建主面板
        JPanel checkBoxPanel = new JPanel(new BorderLayout(5, 0));
        checkBoxPanel.add(enableClassJavaDocTemplateCheckBox, BorderLayout.WEST);
        checkBoxPanel.add(hintLabel, BorderLayout.CENTER);

        panel = new JPanel(new BorderLayout());
        panel.add(checkBoxPanel, BorderLayout.NORTH);
        panel.add(classJavaDocTemplateEditorPanel, BorderLayout.CENTER);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        enableClassJavaDocTemplateCheckBox.addActionListener(e -> {
            boolean selected = enableClassJavaDocTemplateCheckBox.isSelected();
            classJavaDocTemplateEditorPanel.setVisible(selected);
            if (panel.getParent() != null) {
                panel.getParent().revalidate();
                panel.getParent().repaint();
            }
        });
    }

    /**
     * 创建类 Javadoc 模板编辑器面板
     * <p>
     * 创建一个包含模板文本区域和重置按钮的面板，参考提示词的展示方式。
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
     * @param settings 设置对象，将读取的值填充到此对象中
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

