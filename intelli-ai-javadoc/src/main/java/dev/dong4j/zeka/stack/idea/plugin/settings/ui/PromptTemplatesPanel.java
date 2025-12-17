package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentListener;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.PanelUtil;

/**
 * 提示词模板面板
 * <p>
 * 提供提示词模板的配置界面，允许用户配置系统、类、方法、字段和测试的提示词模板。
 * 支持通过复选框控制面板的显示/隐藏，通过选项卡切换不同类型的提示词，并提供重置为默认值功能。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.4.0
 */
public class PromptTemplatesPanel {

    /** 显示提示词模板的复选框，用于控制提示词模板面板的显示/隐藏 */
    private JBCheckBox showPromptTemplatesCheckBox;

    /** 提示词模板内容面板（包含选项卡面板） */
    private JPanel promptTemplatesContentPanel;

    /** 系统提示文本区域，用于显示或编辑系统提示内容 */
    private JTextArea systemPromptTextArea;

    /** 类提示文本区域，用于显示或输入类相关的提示信息 */
    private JTextArea classPromptTextArea;

    /** 方法提示文本区域，用于显示方法相关的提示信息 */
    private JTextArea methodPromptTextArea;

    /** 字段提示文本区域，用于展示操作提示或说明文字 */
    private JTextArea fieldPromptTextArea;

    /** 测试提示文本区域 */
    private JTextArea testPromptTextArea;

    /** 主面板 */
    private JPanel panel;

    /**
     * 构造函数
     */
    public PromptTemplatesPanel() {
        createUI();
        setupListeners();
    }

    /**
     * 创建用户界面组件
     * <p>
     * 初始化多个文本区域用于输入提示信息, 并构建包含提示模板设置的面板结构
     */
    private void createUI() {
        // Prompt 配置 - 创建文本区域（将在 Tab 页中使用）
        // 增加初始高度：15行（原来10行），宽度保持50列不变
        systemPromptTextArea = new JTextArea(15, 15);
        classPromptTextArea = new JTextArea(15, 15);
        methodPromptTextArea = new JTextArea(15, 15);
        fieldPromptTextArea = new JTextArea(15, 15);
        testPromptTextArea = new JTextArea(15, 15);

        // 创建提示词模板内容面板
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(new JBLabel("  " + JavadocBundle.message("settings.prompt.hint")))
            .addComponent(createPromptTabbedPane())
            .getPanel();

        // 创建带边框的面板
        JPanel borderedPanel = PanelUtil.createBorderPanel(contentPanel, "settings.advanced.settings.prompt.templates");

        // 创建显示提示词模板的复选框
        showPromptTemplatesCheckBox = new JBCheckBox(JavadocBundle.message("settings.prompt.settings.show"));

        // 创建提示词模板内容面板容器
        promptTemplatesContentPanel = new JPanel(new BorderLayout());
        promptTemplatesContentPanel.add(borderedPanel, BorderLayout.CENTER);
        promptTemplatesContentPanel.setVisible(false); // 默认隐藏

        // 构建主面板
        panel = new JPanel(new BorderLayout());
        panel.add(showPromptTemplatesCheckBox, BorderLayout.NORTH);
        panel.add(promptTemplatesContentPanel, BorderLayout.CENTER);
    }

    /**
     * 设置监听器以响应显示提示模板复选框的状态变化
     * <p>
     * 当复选框状态改变时, 根据选中状态显示或隐藏提示模板内容面板, 并重新验证和绘制父容器
     */
    private void setupListeners() {
        showPromptTemplatesCheckBox.addActionListener(e -> {
            boolean selected = showPromptTemplatesCheckBox.isSelected();
            promptTemplatesContentPanel.setVisible(selected);
            if (panel.getParent() != null) {
                panel.getParent().revalidate();
                panel.getParent().repaint();
            }
        });
    }

    /**
     * 获取面板组件
     * <p>
     * 返回当前的面板对象
     *
     * @return 面板组件, 保证不为 null
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
        settings.systemPromptTemplate = systemPromptTextArea.getText().trim();
        settings.classPromptTemplate = classPromptTextArea.getText().trim();
        settings.methodPromptTemplate = methodPromptTextArea.getText().trim();
        settings.fieldPromptTemplate = fieldPromptTextArea.getText().trim();
        settings.testPromptTemplate = testPromptTextArea.getText().trim();
        // 保存显示/隐藏状态（使用 showPromptTemplates 字段）
        settings.showPromptTemplates = showPromptTemplatesCheckBox.isSelected();
    }

    /**
     * 加载设置
     *
     * @param settings 设置对象
     */
    public void loadSettings(@NotNull SettingsState settings) {
        systemPromptTextArea.setText(settings.systemPromptTemplate);
        classPromptTextArea.setText(settings.classPromptTemplate);
        methodPromptTextArea.setText(settings.methodPromptTemplate);
        fieldPromptTextArea.setText(settings.fieldPromptTemplate);
        testPromptTextArea.setText(settings.testPromptTemplate);
        // 加载显示/隐藏状态（使用 showPromptTemplates 字段）
        showPromptTemplatesCheckBox.setSelected(settings.showPromptTemplates);
        promptTemplatesContentPanel.setVisible(settings.showPromptTemplates);
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
        // 只设置首选高度，宽度自适应父容器（参考 intelli-ai-engine 的实现）
        promptTabbedPane.setPreferredSize(new Dimension(0, 400));

        // 创建各个 Tab 页
        promptTabbedPane.addTab(JavadocBundle.message("settings.prompt.tab.system"), createPromptTab(systemPromptTextArea, "system"));
        promptTabbedPane.addTab(JavadocBundle.message("settings.prompt.tab.class"), createPromptTab(classPromptTextArea, "class"));
        promptTabbedPane.addTab(JavadocBundle.message("settings.prompt.tab.method"), createPromptTab(methodPromptTextArea, "method"));
        promptTabbedPane.addTab(JavadocBundle.message("settings.prompt.tab.field"), createPromptTab(fieldPromptTextArea, "field"));
        promptTabbedPane.addTab(JavadocBundle.message("settings.prompt.tab.test"), createPromptTab(testPromptTextArea, "test"));

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
        textArea.setToolTipText(JavadocBundle.message("settings.prompt." + promptType + ".tooltip"));

        // 添加文档监听器，根据内容自动调整大小
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            /**
             * 处理文档事件, 调整文本区域大小
             * <p>
             * 当文档事件发生时, 调用 adjustTextAreaSize 方法调整文本区域的大小
             *
             * @param e 文档事件对象
             */
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            /**
             * 文档更新时触发的回调方法.
             * <p>
             * 当文本域的内容发生变化时, 该方法会被调用,
             *
             * @param e 文档事件, 包含更新的相关信息
             */
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                adjustTextAreaSize(textArea);
            }

            /**
             * 处理文档内容变化事件, 调整文本区域大小
             * <p>
             * 当文本区域内容发生变化时, 调用 adjustTextAreaSize 方法调整其尺寸
             *
             * @param e 文档变化事件对象
             */
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

        // 创建内容面板，包含占位符说明（如果是类提示）和文本区域
        JPanel contentPanel = new JPanel(new BorderLayout());

        // 如果是类提示，添加占位符说明
        if ("class".equals(promptType)) {
            JBLabel placeholderHint = new JBLabel(JavadocBundle.message("settings.prompt.class.placeholder.hint"));
            placeholderHint.setFont(placeholderHint.getFont().deriveFont(placeholderHint.getFont().getSize() - 1f));
            placeholderHint.setForeground(UIManager.getColor("Label.disabledForeground"));
            placeholderHint.setBorder(JBUI.Borders.empty(5, 10));
            contentPanel.add(placeholderHint, BorderLayout.NORTH);
        }

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        tabPanel.add(contentPanel, BorderLayout.CENTER);

        // 创建重置按钮
        JButton resetButton = new JButton(JavadocBundle.message("settings.prompt.reset"));
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
    private void resetPromptToDefault(String promptType, JTextArea textArea) {
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
}

