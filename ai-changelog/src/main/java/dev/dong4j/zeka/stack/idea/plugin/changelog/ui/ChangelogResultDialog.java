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
 * Changelog 结果展示对话框
 */
public class ChangelogResultDialog extends DialogWrapper {

    private final String changelog;
    private JTextArea textArea;

    public ChangelogResultDialog(@Nullable Project project, @NotNull String changelog) {
        super(project);
        this.changelog = changelog;
        setTitle("Generated Changelog");
        setModal(false);
        init();
    }

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

    @Override
    protected void doOKAction() {
        // 可以在这里添加复制到剪贴板的逻辑
        super.doOKAction();
    }
}

