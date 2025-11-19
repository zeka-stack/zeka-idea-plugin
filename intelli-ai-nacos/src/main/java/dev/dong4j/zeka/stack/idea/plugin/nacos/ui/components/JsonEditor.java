package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.YamlUtils;

/**
 * Nacos JSON 编辑器组件
 * 支持多种配置格式的编辑器
 *
 * @author dong4j
 * @since 1.0.0
 */
public class JsonEditor extends JPanel {
    private static final Logger LOG = Logger.getInstance(JsonEditor.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final Project project;
    private EditorTextField editorTextField;
    private Language language; // 默认语言类型
    private String originalContent = "";

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
     * 设置内容并重置修改状态
     *
     * @param content 内容
     */
    public void setContentAndMarkClean(@Nullable String content) {
        setContent(content);
        markClean();
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
        String text = getContent();
        if (StringUtil.isEmptyOrSpaces(text)) {
            return;
        }
        try {
            String langId = getFileType().toLowerCase();
            String formatted = switch (langId) {
                case "json" -> formatJson(text);
                case "yaml", "yml" -> YamlUtils.formatYaml(text);
                case "xml" -> formatXml(text);
                case "properties" -> formatProperties(text);
                default -> text;
            };
            setContent(formatted);
        } catch (Exception ex) {
            LOG.warn("Format content failed", ex);
        }
    }

    /**
     * 检查内容是否已修改
     *
     * @return 是否已修改
     */
    public boolean isModified() {
        return !Objects.equals(originalContent, getContent());
    }

    /**
     * 标记为未修改
     */
    public void markClean() {
        this.originalContent = getContent();
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

    private String formatJson(@NotNull String content) throws Exception {
        JsonNode node = JSON_MAPPER.readTree(content);
        return JSON_MAPPER.writeValueAsString(node);
    }

    private String formatXml(@NotNull String content) throws Exception {
        var documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var document = documentBuilder.parse(new org.xml.sax.InputSource(new StringReader(content)));
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private String formatProperties(@NotNull String content) throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        StringWriter writer = new StringWriter();
        properties.store(writer, "Formatted by IntelliAI Nacos");
        return writer.toString();
    }
}