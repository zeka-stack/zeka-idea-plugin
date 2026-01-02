package dev.dong4j.zeka.stack.idea.javadoc.settings.ui;

import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.PanelUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.AIProviderSelectionPanel;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.FeedbackPanel;

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

    /** 主界面主面板, 用于承载主要功能组件和布局 */
    private JPanel mainPanel;

    /** AI 提供商选择面板 */
    private AIProviderSelectionPanel aiProviderSelectionPanel;

    /**
     * 语言支持面板
     *
     * @since 1.0
     */
    private LanguageSupportPanel languageSupportPanel;

    // Javadoc 标签配置
    /** 自定义 Javadoc 标签面板 */
    private CustomJavadocTagsPanel customJavaDocTagsPanel;

    /**
     * 生成规则配置面板
     * <p>
     * 用于配置 Javadoc 生成过程中的各种规则选项, 如代码分析模式, 注释风格, 标签优先级等.
     * 该面板提供用户界面组件以自定义生成行为.
     */
    private GenerationRulesPanel generationRulesPanel;

    /**
     * 类 Javadoc 模板面板
     * <p>
     * 用于配置和管理类级别的 Javadoc 模板设置.
     * 用户可以通过该面板自定义生成类注释时所使用的模板内容.
     */
    private ClassJavadocTemplatePanel classJavaDocTemplatePanel;

    /**
     * 提示词模板面板
     * <p>
     * 用于配置生成 Javadoc 所需的提示词模板, 用户可自定义不同场景下的 AI 输入内容.
     */
    private PromptTemplatesPanel promptTemplatesPanel;

    /**
     * 反馈面板
     * <p>
     * 用于显示和处理用户在使用 Javadoc 设置面板时的反馈信息, 包括错误提示, 操作成功提示等.
     * 支持与插件系统的集成, 提供反馈内容的展示和交互功能.
     *
     */
    private FeedbackPanel feedbackPanel;


    /**
     * 构造函数, 初始化 Javadoc 设置面板
     * <p>
     * 调用创建用户界面和设置事件监听器的方法, 完成面板的初始化
     */
    public JavadocSettingsPanel() {
        createUI();
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
        // 初始化 AI 提供商选择面板（使用 engine 插件中的通用类）
        aiProviderSelectionPanel = new AIProviderSelectionPanel(
            JavadocBundle::message,
            () -> {
                // 面板刷新后的回调：恢复选中的供应商
                SettingsState settings = SettingsState.getInstance();
                loadSettings(settings);
            }
        );
        // 初始化提示词模板面板
        promptTemplatesPanel = new PromptTemplatesPanel();
        // 初始化语言支持面板
        languageSupportPanel = new LanguageSupportPanel();
        // 初始化生成规则配置面板
        generationRulesPanel = new GenerationRulesPanel();
        // 初始化自定义 Javadoc 标签面板
        customJavaDocTagsPanel = new CustomJavadocTagsPanel();
        // 初始化类 Javadoc 模板面板
        classJavaDocTemplatePanel = new ClassJavadocTemplatePanel();
        // 初始化反馈面板
        feedbackPanel = new FeedbackPanel(
            null, // 应用级设置，project 为 null
            PluginContents.PLUGIN_ID, // 插件 ID
            PluginContents.PLUGIN_NAME, // 插件名称
            "zeka-stack-javadoc-plugin" // 签名密钥
        );

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
            .addSeparator(10)

            // 填充垂直空间，使反馈面板固定在底部
            .addComponentFillVertically(new JPanel(), 0)

            // 第六组：反馈面板（固定在底部）
            .addComponent(feedbackPanel.getContent())
            .getPanel();

        mainPanel.setBorder(JBUI.Borders.empty(10));
    }


    /**
     * 释放资源
     * <p>
     * 取消注册监听器，避免内存泄漏。
     * 应该在设置页面关闭时调用。
     * <p>
     * 当前只有 AIProviderSelectionPanel 注册了全局监听器需要手动释放。
     * 其他 panel 使用的是标准 Swing 监听器（ActionListener、DocumentListener），
     * 这些监听器会随着组件的销毁而自动清理，不需要手动释放。
     */
    public void dispose() {
        if (aiProviderSelectionPanel != null) {
            aiProviderSelectionPanel.dispose();
        }
        // 注意：其他 panel（promptTemplatesPanel, languageSupportPanel,
        // generationRulesPanel, customJavaDocTagsPanel, classJavaDocTemplatePanel）
        // 只使用了标准 Swing 监听器，会随着组件销毁自动清理，不需要手动释放
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
