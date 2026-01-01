package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.PanelUtil;

/**
 * 语言支持面板
 * <p>
 * 提供编程语言支持选择和生成提示设置的配置界面。
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

    /** 显示生成 Javadoc 提示复选框 */
    private JBCheckBox showGenerateJavadocHintCheckBox;

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

        // 显示生成 Javadoc 提示复选框
        showGenerateJavadocHintCheckBox = new JBCheckBox(JavadocBundle.message("settings.show.generate.javadoc.hint"));
        showGenerateJavadocHintCheckBox.setToolTipText(JavadocBundle.message("settings.show.generate.javadoc.hint.hint"));
        showGenerateJavadocHintCheckBox.setEnabled(true);

        // 创建内容面板
        JPanel contentPanel = FormBuilder.createFormBuilder()
            .addComponent(javaCheckBox)
            .addComponent(kotlinCheckBox)
            .addComponent(showGenerateJavadocHintCheckBox)
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
        settings.supportedLanguages = new HashSet<>();
        if (javaCheckBox.isSelected()) {
            settings.supportedLanguages.add(PluginContents.JAVA);
        }
        if (kotlinCheckBox.isSelected()) {
            settings.supportedLanguages.add(PluginContents.KOTLIN);
        }

        // 保存显示生成 Javadoc 提示配置
        settings.showGenerateJavadocHint = showGenerateJavadocHintCheckBox.isSelected();
    }

    /**
     * 加载设置
     *
     * @param settings 设置对象
     */
    public void loadSettings(@NotNull SettingsState settings) {
        javaCheckBox.setSelected(settings.supportedLanguages.contains(PluginContents.JAVA));
        kotlinCheckBox.setSelected(settings.supportedLanguages.contains(PluginContents.KOTLIN));

        // 加载显示生成 Javadoc 提示配置
        showGenerateJavadocHintCheckBox.setSelected(settings.showGenerateJavadocHint);
    }
}
