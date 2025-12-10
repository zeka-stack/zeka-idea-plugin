package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.component.CustomJavadocTagRegistrar;
import dev.dong4j.zeka.stack.idea.plugin.component.JavadocFileTemplatesHandler;
import dev.dong4j.zeka.stack.idea.plugin.settings.ui.JavaDocSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;

/**
 * Javadoc 设置可配置类
 * <p>
 * 实现了 SearchableConfigurable 接口, 用于在 IDE 中提供 Javadoc 生成插件的配置界面.
 * 该类负责管理 Javadoc 生成的相关设置, 包括 AI 提供商配置, 生成选项, 提示模板,
 * 自定义标签等配置项, 并提供配置的验证, 应用和重置功能.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
@SuppressWarnings("DuplicatedCode")
public class JavadocSettingsConfigurable implements SearchableConfigurable {

    /**
     * 设置面板 UI 组件
     *
     * <p>负责显示和处理用户界面交互。
     * 延迟初始化，在 createComponent 方法中创建。
     *
     * @see #createComponent()
     * @see JavaDocSettingsPanel
     */
    private JavaDocSettingsPanel settingsPanel;

    /**
     * 获取插件的唯一标识符
     * <p>
     * 返回该插件在系统中的唯一标识字符串，用于识别和区分不同的插件。
     *
     * @return 插件的唯一标识符
     */
    @NotNull
    @NonNls
    @Override
    public String getId() {
        return "zeka.stack.idea.plugin.aij";
    }

    /**
     * 获取显示名称
     *
     * <p>返回在设置界面中显示的面板名称。
     * 使用国际化资源文件获取名称，支持多语言。
     *
     * <p>显示位置：Settings → Tools → IntelliAI Javadoc
     *
     * @return 显示名称
     * @see JavadocBundle#message(String, Object...))
     */
    @Override
    public String getDisplayName() {
        return JavadocBundle.message("settings.display.name");
    }

    /**
     * 获取帮助主题
     *
     * <p>返回帮助系统的主题标识符。
     * 目前未实现具体帮助文档。
     *
     * @return 帮助主题标识符
     */
    @Nullable
    @Override
    public String getHelpTopic() {
        return "settings.javadoc.ai";
    }

    /**
     * 创建设置面板组件
     *
     * <p>创建并返回设置面板的 UI 组件。
     * 采用延迟初始化模式，只在首次调用时创建。
     *
     * <p>生命周期：
     * <ol>
     *   <li>首次调用时创建 JavaDocSettingsPanel</li>
     *   <li>后续调用返回已创建的面板</li>
     *   <li>在 disposeUIResources 中释放资源</li>
     * </ol>
     *
     * @return 设置面板的根组件
     * @see JavaDocSettingsPanel#getPanel()
     * @see #disposeUIResources()
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        if (settingsPanel == null) {
            settingsPanel = new JavaDocSettingsPanel();
            // 加载已保存的配置
            settingsPanel.loadSettings(SettingsState.getInstance());
        }
        return settingsPanel.getPanel();
    }

    /**
     * 检查配置是否已修改
     *
     * <p>比较当前配置和面板中的配置是否一致。
     * 用于确定是否需要显示"Apply"按钮。
     *
     * <p>比较策略：
     * <ul>
     *   <li>逐项比较所有配置项</li>
     *   <li>使用 equals 方法进行比较</li>
     *   <li>处理 null 值和边界情况</li>
     * </ul>
     *
     * <p>比较的配置项：
     * <ul>
     *   <li>AI 提供商配置（提供商 ID、模型名、Base URL、API Key、验证状态）</li>
     *   <li>功能开关配置（类、方法、字段生成开关，跳过已有文档）</li>
     *   <li>高级选项配置（重试次数、超时时间、温度参数等）</li>
     *   <li>语言支持配置</li>
     * </ul>
     *
     * @return 如果配置已修改返回 true，否则返回 false
     * @see SettingsState#getInstance()
     * @see JavaDocSettingsPanel#getSettings()
     */
    @SuppressWarnings("D")
    @Override
    public boolean isModified() {
        if (settingsPanel == null) {
            return false;
        }

        SettingsState currentSettings = SettingsState.getInstance();
        SettingsState panelSettings = settingsPanel.getSettings();

        // 比较各个配置项
        if (currentSettings.providerConfig != panelSettings.providerConfig) {
            return true;
        }

        if (currentSettings.generateForClass != panelSettings.generateForClass) {
            return true;
        }
        if (currentSettings.generateForMethod != panelSettings.generateForMethod) {
            return true;
        }
        if (currentSettings.generateForField != panelSettings.generateForField) {
            return true;
        }
        if (currentSettings.overrideExisting != panelSettings.overrideExisting) {
            return true;
        }
        if (currentSettings.enableCodeCompression != panelSettings.enableCodeCompression) {
            return true;
        }
        if (currentSettings.maxClassCodeLines != panelSettings.maxClassCodeLines) {
            return true;
        }

        // 比较 Prompt 模板配置
        if (!currentSettings.systemPromptTemplate.equals(panelSettings.systemPromptTemplate)) {
            return true;
        }
        if (!currentSettings.classPromptTemplate.equals(panelSettings.classPromptTemplate)) {
            return true;
        }
        if (!currentSettings.methodPromptTemplate.equals(panelSettings.methodPromptTemplate)) {
            return true;
        }
        if (!currentSettings.fieldPromptTemplate.equals(panelSettings.fieldPromptTemplate)) {
            return true;
        }
        if (!currentSettings.testPromptTemplate.equals(panelSettings.testPromptTemplate)) {
            return true;
        }

        // 比较自定义 Javadoc 标签
        List<String> currentTags = currentSettings.getNormalizedCustomJavaDocTags();
        List<String> panelTags = panelSettings.getNormalizedCustomJavaDocTags();
        if (!currentTags.equals(panelTags)) {
            return true;
        }
        if (currentSettings.showCustomJavaDocTags != panelSettings.showCustomJavaDocTags) {
            return true;
        }
        if (currentSettings.enableClassJavaDocTemplate != panelSettings.enableClassJavaDocTemplate) {
            return true;
        }
        // 检查类 Javadoc 模板内容是否被修改
        if (!Objects.equals(currentSettings.classJavaDocTemplate, panelSettings.classJavaDocTemplate)) {
            return true;
        }
        if (currentSettings.showAdvancedSettings != panelSettings.showAdvancedSettings) {
            return true;
        }
        if (currentSettings.compressSingleLineJavaDoc != panelSettings.compressSingleLineJavaDoc) {
            return true;
        }
        if (currentSettings.addSpaceBetweenChineseAndEnglish != panelSettings.addSpaceBetweenChineseAndEnglish) {
            return true;
        }
        if (currentSettings.replaceChinesePunctuation != panelSettings.replaceChinesePunctuation) {
            return true;
        }
        if (!Objects.equals(currentSettings.commentLanguage, panelSettings.commentLanguage)) {
            return true;
        }
        if (currentSettings.performanceMode != panelSettings.performanceMode) {
            return true;
        }
        if (currentSettings.showProviderStatistics != panelSettings.showProviderStatistics) {
            return true;
        }

        return !currentSettings.supportedLanguages.equals(panelSettings.supportedLanguages);
    }

    /**
     * 应用配置
     *
     * <p>将面板中的配置应用到全局配置实例。
     * 在用户点击"Apply"或"OK"按钮时调用。
     *
     * <p>处理流程：
     * <ol>
     *   <li>验证配置有效性</li>
     *   <li>获取面板配置</li>
     *   <li>复制配置到全局实例</li>
     * </ol>
     *
     * <p>异常处理：
     * <ul>
     *   <li>配置验证失败时抛出 ConfigurationException</li>
     *   <li>面板为空时不执行任何操作</li>
     * </ul>
     *
     * @throws ConfigurationException 配置验证失败时抛出
     * @see #validateSettings(SettingsState)
     * @see SettingsState#getInstance()
     */
    @SuppressWarnings("D")
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }

        SettingsState panelSettings = settingsPanel.getSettings();

        // 验证配置
        if (!validateSettings(panelSettings)) {
            throw new ConfigurationException(JavadocBundle.message("error.validation.failed"));
        }

        // 应用配置
        SettingsState currentSettings = SettingsState.getInstance();
        currentSettings.providerConfig = panelSettings.providerConfig;
        // 注意：API Key 现在在全局设置中管理（Settings → Tools → IntelliAI Engine）

        currentSettings.generateForClass = panelSettings.generateForClass;
        currentSettings.generateForMethod = panelSettings.generateForMethod;
        currentSettings.generateForField = panelSettings.generateForField;
        currentSettings.overrideExisting = panelSettings.overrideExisting;
        currentSettings.enableCodeCompression = panelSettings.enableCodeCompression;
        currentSettings.maxClassCodeLines = panelSettings.maxClassCodeLines;

        // 保存 Prompt 模板配置
        currentSettings.systemPromptTemplate = panelSettings.systemPromptTemplate;
        currentSettings.classPromptTemplate = panelSettings.classPromptTemplate;
        currentSettings.methodPromptTemplate = panelSettings.methodPromptTemplate;
        currentSettings.fieldPromptTemplate = panelSettings.fieldPromptTemplate;
        currentSettings.testPromptTemplate = panelSettings.testPromptTemplate;

        currentSettings.supportedLanguages = panelSettings.supportedLanguages;

        // 保存自定义 Javadoc 标签配置
        currentSettings.customJavadocTags = panelSettings.customJavadocTags;
        currentSettings.showCustomJavaDocTags = panelSettings.showCustomJavaDocTags;
        currentSettings.enableClassJavaDocTemplate = panelSettings.enableClassJavaDocTemplate;
        currentSettings.classJavaDocTemplate = panelSettings.classJavaDocTemplate;
        currentSettings.showAdvancedSettings = panelSettings.showAdvancedSettings;

        // 应用类 Javadoc 模板配置到所有打开的项目（需要在写操作中执行）
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                // 对所有打开的项目应用模板配置
                Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
                for (Project project : openProjects) {
                    if (project != null && !project.isDisposed()) {
                        JavadocFileTemplatesHandler.applyTemplateConfiguration(project, currentSettings);
                    }
                }
                // 如果没有打开的项目，至少应用到默认项目
                if (openProjects.length == 0) {
                    Project defaultProject = ProjectManager.getInstance().getDefaultProject();
                    if (!defaultProject.isDisposed()) {
                        JavadocFileTemplatesHandler.applyTemplateConfiguration(defaultProject, currentSettings);
                    }
                }
            });
        });
        currentSettings.compressSingleLineJavaDoc = panelSettings.compressSingleLineJavaDoc;
        currentSettings.addSpaceBetweenChineseAndEnglish = panelSettings.addSpaceBetweenChineseAndEnglish;
        currentSettings.replaceChinesePunctuation = panelSettings.replaceChinesePunctuation;
        currentSettings.commentLanguage = panelSettings.commentLanguage;
        currentSettings.performanceMode = panelSettings.performanceMode;
        currentSettings.showProviderStatistics = panelSettings.showProviderStatistics;

        // 触发标签同步（需要在写操作中执行）
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                Project project = ProjectManager.getInstance().getDefaultProject();
                if (!project.isDisposed()) {
                    CustomJavadocTagRegistrar.syncCustomTags(project);
                }
            });
        });
    }

    /**
     * 重置配置
     *
     * <p>将设置面板重置为当前全局配置。
     * 在用户点击"Reset"按钮时调用。
     *
     * <p>操作流程：
     * <ol>
     *   <li>检查面板是否已创建</li>
     *   <li>获取当前全局配置</li>
     *   <li>将配置加载到面板</li>
     * </ol>
     *
     * @see JavaDocSettingsPanel#loadSettings(SettingsState)
     * @see SettingsState#getInstance()
     */
    @Override
    public void reset() {
        if (settingsPanel != null) {
            settingsPanel.loadSettings(SettingsState.getInstance());
        }
    }

    /**
     * 释放 UI 资源
     *
     * <p>释放设置面板占用的资源。
     * 在设置对话框关闭时调用。
     *
     * <p>资源管理：
     * <ul>
     *   <li>将面板引用设为 null</li>
     *   <li>触发垃圾回收</li>
     *   <li>避免内存泄漏</li>
     * </ul>
     *
     * @see #createComponent()
     */
    @Override
    public void disposeUIResources() {
        if (settingsPanel != null) {
            settingsPanel.dispose();
            settingsPanel = null;
        }
    }

    /**
     * 验证设置是否有效
     *
     * <p>验证面板中的配置是否完整和有效。
     * 在应用配置前调用，确保配置的正确性。
     *
     * <p>验证内容：
     * <ul>
     *   <li>必填字段检查（提供商、模型、Base URL）</li>
     *   <li>API Key 检查（根据提供商需求）</li>
     *   <li>数值范围检查（重试次数、超时时间等）</li>
     * </ul>
     *
     * <p>验证规则：
     * <ul>
     *   <li>提供商 ID 不能为空</li>
     *   <li>模型名称不能为空</li>
     *   <li>Base URL 不能为空</li>
     *   <li>需要 API Key 时必须填写</li>
     *   <li>重试次数：0-10</li>
     *   <li>超时时间：1000-300000 毫秒</li>
     *   <li>温度参数：0.0-2.0</li>
     *   <li>最大 Token：100-10000</li>
     * </ul>
     *
     * @param settings 面板中的配置
     * @return 如果配置有效返回 true，否则返回 false
     * @see SettingsState#requiresApiKey()
     */
    private boolean validateSettings(SettingsState settings) {
        // 检查必填字段
        return settings.providerConfig != null;

        // 其他验证逻辑（模型参数、运行时设置等）现在在全局设置中验证
        // 这里只验证插件特定的配置
    }

}

