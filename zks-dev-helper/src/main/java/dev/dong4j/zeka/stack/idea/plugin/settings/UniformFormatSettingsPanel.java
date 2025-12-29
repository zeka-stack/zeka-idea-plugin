package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;
import lombok.Data;

/**
 * ZKS Dev Helper 插件设置面板
 * <p>
 * 该类用于展示和管理 ZKS Dev Helper 插件的设置界面，提供代码样式模块（文件模板、Live Template、代码风格）等功能的开关配置。
 * 用户可以通过该面板对插件的各项功能进行启用或禁用设置，并将配置保存或恢复到指定的设置对象中。
 * 后续会扩展支持 MyBatis、Proxyer 等其他功能模块的配置界面。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.25
 * @since 1.0.0
 */
@Data
public class UniformFormatSettingsPanel {

    /**
     * 主界面主面板，用于承载主要功能组件和布局
     * -- GETTER --
     * 获取主面板组件
     * <p>
     * 返回应用程序的主面板，用于展示主要界面内容。
     */
    private JPanel mainPanel;
    /** 文件模板启用状态复选框 */
    private JBCheckBox enableFileTemplatesCheckBox;
    /** 启用活模板的复选框 */
    private JBCheckBox enableLiveTemplatesCheckBox;
    /** 启用代码样式检查的复选框 */
    private JBCheckBox enableCodeStyleCheckBox;
    /** 描述标签，用于显示相关信息 */
    private JBLabel descriptionLabel;

    /**
     * 构造函数，初始化统一格式设置面板
     * <p>
     * 调用初始化组件方法，完成面板的初始化工作
     */
    public UniformFormatSettingsPanel() {
        initializeComponents();
    }

    /**
     * 初始化插件的组件和布局
     * <p>
     * 该方法用于创建并配置插件所需的各个组件，包括复选框和描述标签，并使用 FormBuilder
     * 构建最终的布局面板。所有组件默认状态为选中。
     *
     * @author 插件开发人员
     * @since 1.0
     */
    private void initializeComponents() {
        // 创建组件（使用国际化）
        enableFileTemplatesCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.enable.file.templates.label"));
        enableLiveTemplatesCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.enable.live.templates.label"));
        enableCodeStyleCheckBox = new JBCheckBox(
            HelperBundle.message("settings.codestyle.enable.code.style.label"));

        // 构建描述文本（使用国际化）
        String descriptionHtml = "<html>" +
                                 "<p>" + HelperBundle.message("settings.description.title") + "</p>" +
                                 "<p><b>" + HelperBundle.message("settings.description.current.features") + "</b></p>" +
                                 "<ul>" +
                                 "<li><b>" + HelperBundle.message("settings.description.file.templates") + "</b></li>" +
                                 "<li><b>" + HelperBundle.message("settings.description.live.templates") + "</b></li>" +
                                 "<li><b>" + HelperBundle.message("settings.description.code.style") + "</b></li>" +
                                 "</ul>" +
                                 "<p><b>" + HelperBundle.message("settings.description.planned.features") + "</b></p>" +
                                 "<ul>" +
                                 "<li>" + HelperBundle.message("settings.description.planned.mybatis") + "</li>" +
                                 "<li>" + HelperBundle.message("settings.description.planned.proxyer") + "</li>" +
                                 "<li>" + HelperBundle.message("settings.description.planned.more") + "</li>" +
                                 "</ul>" +
                                 "</html>";
        descriptionLabel = new JBLabel(descriptionHtml);

        // 设置默认值
        enableFileTemplatesCheckBox.setSelected(true);
        enableLiveTemplatesCheckBox.setSelected(true);
        enableCodeStyleCheckBox.setSelected(true);

        // 使用 FormBuilder 创建布局
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(descriptionLabel)
            .addSeparator()
            .addComponent(enableFileTemplatesCheckBox)
            .addComponent(enableLiveTemplatesCheckBox)
            .addComponent(enableCodeStyleCheckBox)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    /**
     * 判断当前设置是否与给定的设置状态不同
     * <p>
     * 比较当前设置项与传入的设置状态，若任一设置项的选中状态不同，则返回 true。
     *
     * @param settings 要比较的设置状态对象
     * @return 如果当前设置与给定设置状态不同，返回 true；否则返回 false
     */
    public boolean isModified(UniformFormatSettingsState settings) {
        return enableFileTemplatesCheckBox.isSelected() != settings.isEnableFileTemplates() ||
               enableLiveTemplatesCheckBox.isSelected() != settings.isEnableLiveTemplates() ||
               enableCodeStyleCheckBox.isSelected() != settings.isEnableCodeStyle();
    }

    /**
     * 应用格式设置状态到指定的设置对象
     * <p>
     * 将复选框的状态应用到统一格式设置状态对象中，用于配置文件模板、实时模板、代码样式功能的启用状态。
     *
     * @param settings 格式设置状态对象，用于存储配置信息
     */
    public void apply(UniformFormatSettingsState settings) {
        settings.setEnableFileTemplates(enableFileTemplatesCheckBox.isSelected());
        settings.setEnableLiveTemplates(enableLiveTemplatesCheckBox.isSelected());
        settings.setEnableCodeStyle(enableCodeStyleCheckBox.isSelected());
    }

    /**
     * 重置统一格式设置状态
     * <p>
     * 根据传入的设置状态，更新相关复选框的选中状态。
     *
     * @param settings 统一格式设置状态对象
     */
    public void reset(UniformFormatSettingsState settings) {
        enableFileTemplatesCheckBox.setSelected(settings.isEnableFileTemplates());
        enableLiveTemplatesCheckBox.setSelected(settings.isEnableLiveTemplates());
        enableCodeStyleCheckBox.setSelected(settings.isEnableCodeStyle());
    }

}
