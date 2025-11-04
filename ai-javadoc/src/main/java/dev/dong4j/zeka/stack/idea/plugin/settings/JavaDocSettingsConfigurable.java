package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import dev.dong4j.zeka.stack.idea.plugin.settings.ui.JavaDocSettingsPanel;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * AI Javadoc 插件设置面板
 *
 * <p>提供用户界面来配置插件的各项设置，包括：
 * <ul>
 *   <li>AI 服务提供商选择</li>
 *   <li>模型配置</li>
 *   <li>API 密钥</li>
 *   <li>语言支持</li>
 *   <li>高级选项</li>
 *   <li>Prompt 模板配置</li>
 * </ul>
 *
 * <p>配置界面位于：Settings → Tools → AI Javadoc
 *
 * <p>实现 IntelliJ Platform 的 Configurable 接口，
 * 提供标准的设置面板功能：创建组件、检查修改、应用配置、重置配置等。
 *
 * <p>核心功能：
 * <ul>
 *   <li>UI 组件管理</li>
 *   <li>配置变更检测</li>
 *   <li>配置验证和应用</li>
 *   <li>配置重置支持</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see Configurable
 * @since 1.0.0
 */
@SuppressWarnings("DuplicatedCode")
public class JavaDocSettingsConfigurable implements Configurable {

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
     * 原始配置状态
     *
     * <p>保存应用配置前的原始状态，
     * 用于比较和检测配置变更。
     *
     * @see #isModified()
     * @see SettingsState
     */
    private SettingsState originalSettings;

    /**
     * 获取显示名称
     *
     * <p>返回在设置界面中显示的面板名称。
     * 使用国际化资源文件获取名称，支持多语言。
     *
     * <p>显示位置：Settings → Tools → AI Javadoc
     *
     * @return 显示名称
     * @see JavaDocBundle#message(String, Object...))
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return JavaDocBundle.message("settings.display.name");
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
        if (!currentSettings.aiProvider.equals(panelSettings.aiProvider)) {
            return true;
        }
        if (!currentSettings.modelName.equals(panelSettings.modelName)) {
            return true;
        }
        if (!currentSettings.baseUrl.equals(panelSettings.baseUrl)) {
            return true;
        }
        if (!currentSettings.apiKey.equals(panelSettings.apiKey)) {
            return true;
        }
        if (currentSettings.configurationVerified != panelSettings.configurationVerified) {
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

        if (currentSettings.maxRetries != panelSettings.maxRetries) {
            return true;
        }
        if (currentSettings.timeout != panelSettings.timeout) {
            return true;
        }
        if (currentSettings.temperature != panelSettings.temperature) {
            return true;
        }
        if (currentSettings.maxTokens != panelSettings.maxTokens) {
            return true;
        }
        if (currentSettings.performanceMode != panelSettings.performanceMode) {
            return true;
        }
        if (currentSettings.showProviderStatistics != panelSettings.showProviderStatistics) {
            return true;
        }
        if (currentSettings.verboseLogging != panelSettings.verboseLogging) {
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
    @Override
    public void apply() throws ConfigurationException {
        if (settingsPanel == null) {
            return;
        }

        SettingsState panelSettings = settingsPanel.getSettings();

        // 验证配置
        if (!validateSettings(panelSettings)) {
            throw new ConfigurationException(JavaDocBundle.message("error.validation.failed"));
        }

        // 应用配置
        SettingsState currentSettings = SettingsState.getInstance();
        currentSettings.aiProvider = panelSettings.aiProvider;
        currentSettings.modelName = panelSettings.modelName;
        currentSettings.baseUrl = panelSettings.baseUrl;
        currentSettings.apiKey = panelSettings.apiKey;
        currentSettings.configurationVerified = panelSettings.configurationVerified;

        currentSettings.generateForClass = panelSettings.generateForClass;
        currentSettings.generateForMethod = panelSettings.generateForMethod;
        currentSettings.generateForField = panelSettings.generateForField;
        currentSettings.overrideExisting = panelSettings.overrideExisting;
        currentSettings.enableCodeCompression = panelSettings.enableCodeCompression;
        currentSettings.maxClassCodeLines = panelSettings.maxClassCodeLines;

        currentSettings.maxRetries = panelSettings.maxRetries;
        currentSettings.timeout = panelSettings.timeout;
        currentSettings.temperature = panelSettings.temperature;
        currentSettings.maxTokens = panelSettings.maxTokens;
        currentSettings.performanceMode = panelSettings.performanceMode;
        currentSettings.showProviderStatistics = panelSettings.showProviderStatistics;
        currentSettings.verboseLogging = panelSettings.verboseLogging;

        // 保存 Prompt 模板配置
        currentSettings.systemPromptTemplate = panelSettings.systemPromptTemplate;
        currentSettings.classPromptTemplate = panelSettings.classPromptTemplate;
        currentSettings.methodPromptTemplate = panelSettings.methodPromptTemplate;
        currentSettings.fieldPromptTemplate = panelSettings.fieldPromptTemplate;
        currentSettings.testPromptTemplate = panelSettings.testPromptTemplate;

        currentSettings.supportedLanguages = panelSettings.supportedLanguages;
        currentSettings.verboseLogging = panelSettings.verboseLogging;
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
        settingsPanel = null;
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
    @SuppressWarnings("D")
    private boolean validateSettings(SettingsState settings) {
        // 检查必填字段
        if (settings.aiProvider == null || settings.aiProvider.trim().isEmpty()) {
            return false;
        }

        if (settings.modelName == null || settings.modelName.trim().isEmpty()) {
            return false;
        }

        if (settings.baseUrl == null || settings.baseUrl.trim().isEmpty()) {
            return false;
        }

        // 检查是否需要 API Key
        if (settings.requiresApiKey() &&
            (settings.apiKey == null || settings.apiKey.trim().isEmpty())) {
            return false;
        }

        // 检查数值范围
        if (settings.maxRetries < 0 || settings.maxRetries > 10) {
            return false;
        }

        if (settings.timeout < 1000 || settings.timeout > 300000) {
            return false;
        }

        if (settings.temperature < 0.0 || settings.temperature > 2.0) {
            return false;
        }

        return settings.maxTokens >= 100 && settings.maxTokens <= 10000;
    }
}

