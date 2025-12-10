package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.settings.CommentLanguage;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;

/**
 * 语言支持面板
 * <p>
 * 提供编程语言支持选择和注释语言选择的配置界面。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.5.0
 */
public class LanguageSupportPanel {

    /** Java 语言支持选项框 */
    private JBCheckBox javaCheckBox;

    /** Kotlin 语言支持开关控件 */
    private JBCheckBox kotlinCheckBox;

    /** 注释语言选择下拉框 */
    private ComboBox<CommentLanguage> commentLanguageComboBox;

    /** 主面板 */
    private JPanel panel;

    /**
     * 构造函数
     */
    public LanguageSupportPanel() {
        createUI();
    }

    /**
     * 创建 UI
     */
    private void createUI() {
        // 语言支持
        javaCheckBox = new JBCheckBox(JavadocBundle.message("settings.language.java"));
        javaCheckBox.setEnabled(true);
        kotlinCheckBox = new JBCheckBox(JavadocBundle.message("settings.language.kotlin"));
        kotlinCheckBox.setEnabled(true);

        // 注释语言选择下拉框
        commentLanguageComboBox = new ComboBox<>(CommentLanguage.values());
        commentLanguageComboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JBLabel label = new JBLabel();
            if (value != null) {
                label.setText(value.getDesc());
            }
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            label.setOpaque(true);
            return label;
        });
        // 设置默认值为中文
        commentLanguageComboBox.setSelectedItem(CommentLanguage.ZH);

        // 创建内容面板
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(javaCheckBox)
            .addComponent(kotlinCheckBox)
            .addLabeledComponent(new JBLabel(JavadocBundle.message("settings.comment.language") + ":"), commentLanguageComboBox)
            .getPanel();

        // 创建带边框的面板
        panel = PanelUtil.createBorderPanel(contentPanel, "settings.language.support");
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
        // 保存注释语言配置，如果为空则使用默认值 ZH
        CommentLanguage selectedLanguage = (CommentLanguage) commentLanguageComboBox.getSelectedItem();
        if (selectedLanguage == null) {
            selectedLanguage = CommentLanguage.ZH; // 默认中文
        }
        settings.commentLanguage = selectedLanguage;

        settings.supportedLanguages = new HashSet<>();
        if (javaCheckBox.isSelected()) {
            settings.supportedLanguages.add("java");
        }
        if (kotlinCheckBox.isSelected()) {
            settings.supportedLanguages.add("kotlin");
        }
    }

    /**
     * 加载设置
     *
     * @param settings 设置对象
     */
    public void loadSettings(@NotNull SettingsState settings) {
        // 加载注释语言配置，如果为空则使用默认值 ZH
        CommentLanguage commentLanguage = settings.commentLanguage;
        if (commentLanguage == null) {
            commentLanguage = CommentLanguage.ZH; // 默认中文
        }
        commentLanguageComboBox.setSelectedItem(commentLanguage);

        javaCheckBox.setSelected(settings.supportedLanguages.contains("java"));
        kotlinCheckBox.setSelected(settings.supportedLanguages.contains("kotlin"));
    }
}

