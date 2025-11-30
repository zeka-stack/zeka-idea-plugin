package dev.dong4j.zeka.stack.idea.plugin.changelog.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * 变更日志结果对话框
 * <p>
 * 用于显示生成的变更日志内容的对话框窗口, 继承自 DialogWrapper,
 * 提供了一个可编辑的文本区域来展示和编辑变更日志信息
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ChangelogResultDialog extends DialogWrapper {

    /** 变更日志内容 */
    private final String changelog;
    /** 文本输入区域 */
    private JTextArea textArea;

    /**
     * 创建 Changelog 结果对话框
     *
     * @param project   项目对象
     * @param changelog 要显示的内容
     */
    public ChangelogResultDialog(@Nullable Project project, @NotNull String changelog) {
        this(project, changelog, "Generated Changelog");
    }

    /**
     * 创建 Changelog 结果对话框（带自定义标题）
     *
     * @param project   项目对象
     * @param changelog 要显示的内容
     * @param title     对话框标题
     */
    public ChangelogResultDialog(@Nullable Project project, @NotNull String changelog, @NotNull String title) {
        super(project);
        this.changelog = changelog;
        setTitle(title);
        setModal(false);
        init();
    }

    /**
     * 创建中心面板
     * <p>
     * 该方法构造一个包含可编辑文本区域的面板, 用于显示变更日志. 文本区域使用等宽字体, 默认光标位置为起始位置, 并放置在可滚动的滚动面板中. 面板的首选尺寸为 800×600.
     *
     * @return 包含文本区域的 {@link JComponent}
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(JBUI.size(800, 600));

        textArea = new JTextArea(changelog);
        textArea.setEditable(true);
        textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        textArea.setCaretPosition(0);

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setBorder(JBUI.Borders.empty(10));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 执行 OK 操作
     * <p>
     * 调用父类的 doOKAction 方法, 完成 OK 操作的默认实现
     *
     * @since 1.0
     */
    @Override
    protected void doOKAction() {
        // 可以在这里添加复制到剪贴板的逻辑
        super.doOKAction();
    }

    /**
     * 获取用户编辑后的文本内容
     *
     * @return 用户编辑后的文本内容
     */
    @NotNull
    public String getText() {
        return textArea != null ? textArea.getText() : changelog;
    }
}

