package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.JPanel;

import lombok.Data;

/**
 * 统一格式插件设置面板
 * <p>
 * 该类用于展示和管理统一格式化插件的设置界面，提供文件模板、Live Template、代码风格和使用统计等功能的开关配置。
 * 用户可以通过该面板对插件的各项功能进行启用或禁用设置，并将配置保存或恢复到指定的设置对象中。
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
    /** 启用统计信息的复选框 */
    private JBCheckBox enableStatisticsCheckBox;
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
        // 创建组件
        enableFileTemplatesCheckBox = new JBCheckBox("启用文件模板");
        enableLiveTemplatesCheckBox = new JBCheckBox("启用 Live Template");
        enableCodeStyleCheckBox = new JBCheckBox("启用代码风格配置");
        enableStatisticsCheckBox = new JBCheckBox("启用使用统计");

        descriptionLabel = new JBLabel("<html>" +
                                       "<p>Uniform Format 插件提供统一的代码格式化和模板管理功能。</p>" +
                                       "<ul>" +
                                       "<li><b>文件模板</b>：自动添加统一的文件头部注释</li>" +
                                       "<li><b>Live Template</b>：快速生成常用代码片段</li>" +
                                       "<li><b>代码风格</b>：自动配置统一的代码格式化规则</li>" +
                                       "<li><b>使用统计</b>：统计模板使用情况</li>" +
                                       "</ul>" +
                                       "</html>");

        // 设置默认值
        enableFileTemplatesCheckBox.setSelected(true);
        enableLiveTemplatesCheckBox.setSelected(true);
        enableCodeStyleCheckBox.setSelected(true);
        enableStatisticsCheckBox.setSelected(true);

        // 使用 FormBuilder 创建布局
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(descriptionLabel)
            .addSeparator()
            .addComponent(enableFileTemplatesCheckBox)
            .addComponent(enableLiveTemplatesCheckBox)
            .addComponent(enableCodeStyleCheckBox)
            .addComponent(enableStatisticsCheckBox)
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
               enableCodeStyleCheckBox.isSelected() != settings.isEnableCodeStyle() ||
               enableStatisticsCheckBox.isSelected() != settings.isEnableStatistics();
    }

    /**
     * 应用格式设置状态到指定的设置对象
     * <p>
     * 将复选框的状态应用到统一格式设置状态对象中，用于配置文件模板、实时模板、代码样式和统计功能的启用状态。
     *
     * @param settings 格式设置状态对象，用于存储配置信息
     */
    public void apply(UniformFormatSettingsState settings) {
        settings.setEnableFileTemplates(enableFileTemplatesCheckBox.isSelected());
        settings.setEnableLiveTemplates(enableLiveTemplatesCheckBox.isSelected());
        settings.setEnableCodeStyle(enableCodeStyleCheckBox.isSelected());
        settings.setEnableStatistics(enableStatisticsCheckBox.isSelected());
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
        enableStatisticsCheckBox.setSelected(settings.isEnableStatistics());
    }

}
