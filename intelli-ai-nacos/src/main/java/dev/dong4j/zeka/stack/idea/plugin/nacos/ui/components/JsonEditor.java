package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.components;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.util.Collections;

import javax.swing.JPanel;

/**
 * Nacos JSON 编辑器组件
 * 支持多种配置格式的编辑器
 *
 * @author dong4j
 * @since 1.0.0
 */
public class JsonEditor extends JPanel {
    private final Project project;
    private EditorTextField editorTextField;
    private Language language; // 默认语言类型

    public JsonEditor(@NotNull Project project) {
        this.project = project;
        // 初始化默认语言为YAML
        this.language = Language.findLanguageByID("yaml");
        this.editorTextField = createEditorTextField();

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        add(editorTextField, BorderLayout.CENTER);
    }

    private EditorTextField createEditorTextField() {
        EditorTextFieldProvider provider = EditorTextFieldProvider.getInstance();
        return provider.getEditorField(language != null ? language : Language.ANY, project, Collections.emptyList());
    }

    /**
     * 设置编辑器内容
     *
     * @param content 内容
     */
    public void setContent(@Nullable String content) {
        editorTextField.setText(content != null ? content : "");
    }

    /**
     * 获取编辑器内容
     *
     * @return 内容
     */
    public String getContent() {
        return editorTextField.getText();
    }

    /**
     * 设置文件类型
     *
     * @param fileType 文件类型 (yaml, json, xml, html, txt)
     */
    public void setFileType(@NotNull String fileType) {
        Language newLanguage = Language.findLanguageByID(fileType);
        if (newLanguage != null && this.language != newLanguage) {
            this.language = newLanguage;
            // 重新创建编辑器以支持新的文件类型
            removeAll();
            EditorTextField newEditor = createEditorTextField();
            newEditor.setText(editorTextField.getText());
            editorTextField = newEditor;
            add(editorTextField, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    /**
     * 获取文件类型
     *
     * @return 文件类型
     */
    public String getFileType() {
        return language != null ? language.getID() : "text";
    }

    /**
     * 格式化内容
     */
    public void formatContent() {
        // TODO: 实现内容格式化逻辑
        // 这里可以调用相应的格式化工具
    }

    /**
     * 检查内容是否已修改
     *
     * @return 是否已修改
     */
    public boolean isModified() {
        // TODO: 实现修改状态检查逻辑
        return false;
    }

    /**
     * 设置只读模式
     *
     * @param readOnly 是否只读
     */
    public void setReadOnly(boolean readOnly) {
        editorTextField.setViewer(readOnly);
    }

    /**
     * 获取编辑器文本字段
     *
     * @return 编辑器文本字段
     */
    public EditorTextField getEditorTextField() {
        return editorTextField;
    }
}