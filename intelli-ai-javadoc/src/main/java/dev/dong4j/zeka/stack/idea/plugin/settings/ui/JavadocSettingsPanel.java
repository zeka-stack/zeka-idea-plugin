package dev.dong4j.zeka.stack.idea.plugin.settings.ui;

import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * Javadoc 设置面板类
 * <p>
 * 提供 Javadoc 生成工具的配置界面, 允许用户配置 AI 提供商, 生成规则, 语言支持,
 * 代码压缩, 性能模式等各项设置. 该面板包含多个功能区域, 如 AI 提供商选择,
 * 生成规则配置, 自定义 Javadoc 标签管理, 高级提示模板设置等.
 * 支持 Java 和 Kotlin 语言, 提供中文英文间距调整, 中文标点替换等本地化功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class JavadocSettingsPanel {

    /** 主界面主面板，用于承载主要功能组件和布局 */
    private JPanel mainPanel;

    /** AI 提供商选择面板 */
    private AIProviderSelectionPanel aiProviderSelectionPanel;

    /** 语言支持面板 */
    private LanguageSupportPanel languageSupportPanel;

    // Javadoc 标签配置
    /** 自定义 Javadoc 标签面板 */
    private CustomJavaDocTagsPanel customJavaDocTagsPanel;

    /** 生成规则配置面板 */
    private GenerationRulesPanel generationRulesPanel;

    /** 类 Javadoc 模板面板 */
    private ClassJavaDocTemplatePanel classJavaDocTemplatePanel;

    /** 提示词模板面板 */
    private PromptTemplatesPanel promptTemplatesPanel;


    /**
     * 构造函数，初始化 Javadoc 设置面板
     * <p>
     * 调用创建用户界面和设置事件监听器的方法，完成面板的初始化
     */
    public JavadocSettingsPanel() {
        createUI();
        setupListeners();
    }

    /**
     * 获取主面板
     * <p>
     * 返回用于显示主要内容的面板组件
     *
     * @return 主面板组件
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * 初始化用户界面组件，创建并配置所有 UI 元素，包括下拉框、文本字段、按钮、复选框等。
     * <p>
     * 该方法负责构建整个设置界面的主面板，包括 AI 提供商配置、模型选择、基础 URL 和 API 密钥输入、
     * 连接测试按钮、模型刷新按钮、生成选项、语言支持、高级配置参数以及提示模板区域。
     */
    private void createUI() {
        // 初始化语言支持面板
        languageSupportPanel = new LanguageSupportPanel();

        // 初始化自定义 Javadoc 标签面板
        customJavaDocTagsPanel = new CustomJavaDocTagsPanel();

        // 初始化类 Javadoc 模板面板
        classJavaDocTemplatePanel = new ClassJavaDocTemplatePanel();

        // 初始化提示词模板面板
        promptTemplatesPanel = new PromptTemplatesPanel();

        // 初始化生成规则配置面板
        generationRulesPanel = new GenerationRulesPanel();

        // 初始化 AI 提供商选择面板
        aiProviderSelectionPanel = new AIProviderSelectionPanel();

        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            // 第一组：AI 提供商选择
            .addComponent(aiProviderSelectionPanel.getPanel())
            .addSeparator(10)

            // 第二组：提示词模板（可折叠）
            .addComponent(promptTemplatesPanel.getPanel())
            .addSeparator(10)

            // 第三组：支持的语言
            .addComponent(languageSupportPanel.getPanel())
            .addSeparator(10)

            // 第四组：生成规则配置
            .addComponent(generationRulesPanel.getPanel())
            .addSeparator(10)

            // 第五组：其他设置
            .addComponent(createOtherSettingsPanel())

            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
    }


    /**
     * 释放资源
     * <p>
     * 取消注册监听器，避免内存泄漏。
     * 应该在设置页面关闭时调用。
     */
    public void dispose() {
        if (aiProviderSelectionPanel != null) {
            aiProviderSelectionPanel.dispose();
        }
    }



    /**
     * 创建带边框的面板
     *
     * <p>通用方法，用于创建包含任意数量组件的带边框面板。
     * 将多个组件使用 FormBuilder 添加到内容面板中，
     * 然后为面板添加带标题的边框。
     *
     * @param borderTitle 边框标题的国际化键
     * @param components  要添加到面板中的组件（可变参数）
     * @return 带边框的面板
     */
    private JPanel createPanelWithBorder(String borderTitle, JComponent... components) {
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        for (JComponent component : components) {
            formBuilder.addComponent(component);
        }
        JPanel contentPanel = formBuilder.getPanel();

        return PanelUtil.createBorderPanel(contentPanel, borderTitle);
    }


    /**
     * 创建其他设置面板
     *
     * <p>创建一个包含其他设置所有组件的面板，并添加边框。
     *
     * @return 其他设置面板
     */
    private JPanel createOtherSettingsPanel() {
        return createPanelWithBorder("settings.other.settings",
                                     customJavaDocTagsPanel.getPanel(),
                                     classJavaDocTemplatePanel.getPanel());
    }

    /**
     * 初始化各种监听器，用于响应用户界面组件的变化
     * <p>
     * 该方法为各个输入组件添加动作监听器，当组件内容发生变化时，触发相应的更新或验证状态清除操作。
     * 包括提供商、Base URL、API Key、模型选择以及代码优化配置等变化的监听。
     */
    private void setupListeners() {
        // 所有监听器已移到各自的面板类中
    }

    /**
     * 获取当前设置状态对象, 用于保存用户配置的各类设置信息.
     * <p>
     * 该方法会从界面组件中读取用户选择的配置项, 并将其封装到 SettingsState 对象中.
     * 包括提供者设置, 生成选项, 语言支持, 提示模板, 自定义 Javadoc 标签等.
     *
     * @return 当前设置状态对象
     * @since 1.0
     */
    @NotNull
    public SettingsState getSettings() {
        SettingsState settings = new SettingsState();

        // 获取选择的供应商类型
        settings.providerConfig = aiProviderSelectionPanel != null ? aiProviderSelectionPanel.getSelectedProvider() : null;

        // 从生成规则配置面板获取设置
        if (generationRulesPanel != null) {
            generationRulesPanel.getSettings(settings);
        }

        // 从语言支持面板获取设置
        if (languageSupportPanel != null) {
            languageSupportPanel.getSettings(settings);
        }

        // 从自定义 Javadoc 标签面板获取设置
        if (customJavaDocTagsPanel != null) {
            customJavaDocTagsPanel.getSettings(settings);
        }

        // 从提示词模板面板获取设置
        if (promptTemplatesPanel != null) {
            promptTemplatesPanel.getSettings(settings);
        }

        // 从类 Javadoc 模板面板获取设置
        if (classJavaDocTemplatePanel != null) {
            classJavaDocTemplatePanel.getSettings(settings);
        }

        return settings;
    }

    /**
     * 加载设置配置到界面组件中
     * <p>
     * 将传入的 SettingsState 对象中的配置信息同步到各个 UI 控件中, 包括生成选项, 代码压缩设置,
     * 提示模板, 自定义 Javadoc 标签等高级设置.
     *
     * @param settings 包含所有设置信息的 SettingsState 对象
     */
    @SuppressWarnings( {"DuplicatedCode", "D"})
    public void loadSettings(@NotNull SettingsState settings) {
        // 加载 AI 提供商配置
        if (aiProviderSelectionPanel != null) {
            if (settings.providerConfig == null) {
                // 如果没有保存的配置，尝试选择第一个可用的提供商
                AIProviderSettings globalSettings = AIProviderSettings.getInstance();
                final List<AIProviderConfig> aiProviderTypes = globalSettings.getVerifiedProviders();
                if (CollectionUtils.isNotEmpty(aiProviderTypes)) {
                    aiProviderSelectionPanel.setSelectedProvider(aiProviderTypes.get(0));
                }
            } else {
                aiProviderSelectionPanel.setSelectedProvider(settings.providerConfig);
            }
        }

        // 加载生成规则配置面板设置
        if (generationRulesPanel != null) {
            generationRulesPanel.loadSettings(settings);
        }

        // 加载语言支持面板设置
        if (languageSupportPanel != null) {
            languageSupportPanel.loadSettings(settings);
        }

        // 加载自定义 Javadoc 标签面板设置
        if (customJavaDocTagsPanel != null) {
            customJavaDocTagsPanel.loadSettings(settings);
        }

        // 加载提示词模板面板设置
        if (promptTemplatesPanel != null) {
            promptTemplatesPanel.loadSettings(settings);
        }

        // 加载类 Javadoc 模板面板设置
        if (classJavaDocTemplatePanel != null) {
            classJavaDocTemplatePanel.loadSettings(settings);
        }
    }


}
