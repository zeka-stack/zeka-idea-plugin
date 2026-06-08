package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ResponseLanguage;
import dev.dong4j.zeka.stack.idea.plugin.common.nextedit.NextEditSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.ui.component.StatusIndicatorButton;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.kit.StorageUtil;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 提供商配置控制器
 * <p>
 * 负责管理 AI 服务提供商的配置界面逻辑, 包括提供商类型选择,API 密钥管理, 模型配置,
 * 连接测试, 模型刷新等功能. 该控制器协调凭证管理器, 响应监听器和 UI 组件, 提供完整的
 * AI 服务配置管理功能, 支持多种 AI 提供商的配置和管理.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public final class AIProviderConfigController {

    /** 负责管理 AI 身份凭证的工具类实例 */
    private final AICredentialManager credentialManager;
    /** UI 界面组件, 用于展示和操作 AI 提供商配置信息 */
    private final AIProviderConfigUI ui;
    /** 配置是否已验证的标志, 用于标识当前配置是否通过验证 */
    private Boolean configurationVerified = Boolean.FALSE;
    /** 刷新模型操作是否成功 */
    private Boolean refreshModelsSuccess = null;
    /** 当前正在使用的 AI 提供商配置信息 */
    private AIProviderSettings workingSettings = new AIProviderSettings();

    /**
     * 初始化 AI 提供者配置控制器
     * <p>
     * 通过传入的凭证管理器, 响应监听器和 UI 组件来初始化 AI 提供者配置控制器
     *
     * @param credentialManager 凭证管理器, 不能为空
     * @param ui                UI 组件, 不能为空
     */
    public AIProviderConfigController(@NotNull AICredentialManager credentialManager,
                                      @NotNull AIProviderConfigUI ui) {
        this.credentialManager = credentialManager;
        this.ui = ui;
    }

    /**
     * 初始化 Agent 面板的状态更新回调
     * <p>
     * 需要在 UI 创建完成后调用，确保 agentPanel 已初始化。
     */
    public void initAgentPanel() {
        IntelliAgentPanel intelliAgentPanel = ui.getAgentPanel();
        // 设置父面板，用于显示对话框
        intelliAgentPanel.setParentPanel(ui.getMainPanel());
        // 设置 Agent 面板的状态更新回调
        intelliAgentPanel.setStatusUpdateCallback(() -> {
            IntelliAgentSettings snapshot = intelliAgentPanel.snapshotAgentSettings();
            intelliAgentPanel.updateAgentStatus(snapshot);
        });
    }

    /**
     * 释放 UI 资源
     * <p>
     * 该方法用于清除智能代理面板的状态更新回调, 释放相关资源, 确保在组件销毁时不会残留监听器或引用.
     */
    public void dispose() {
        ui.getAgentPanel().clearStatusUpdateCallback();
    }

    /**
     * 加载并应用设置到用户界面
     * <p>
     * 该方法用于将传入的 AI 提供者设置复制到内部工作设置中, 并根据设置更新用户界面相关组件的状态和显示内容.
     *
     * @param settings AI 提供者设置对象, 用于初始化内部工作设置
     */
    public void loadSettings(@NotNull AIProviderSettings settings) {
        this.workingSettings = settings.copy();

        AIProviderType defaultProviderType = workingSettings.aiProviderType != null
                                             ? workingSettings.aiProviderType
                                             : AIProviderType.QIANWEN;

        ui.setSelectedProviderType(defaultProviderType);
        updateBasicConnectionInfo();

        AIProviderConfig defaultConfig = workingSettings.getDefaultProviderConfig(defaultProviderType);
        ui.getModelComboBox().setSelectedItem(defaultConfig.modelName);
        ui.getBaseUrlField().setText(defaultConfig.baseUrl);
        configurationVerified = defaultConfig.configurationVerified;
        updateTestButtonState();

        loadApiKeyAsync(defaultConfig.credentialId, defaultProviderType.getProviderId());

        refreshModelsSuccess = null;
        updateRefreshButtonState();

        // 加载基础配置
        AIRuntimeSettings runtimeSettings = workingSettings.runtimeSettings != null
                                            ? workingSettings.runtimeSettings
                                            : new AIRuntimeSettings();
        ui.getVerboseLoggingCheckBox().setSelected(workingSettings.verboseLogging);
        ui.getLastUpdateCheckCheckBox().setSelected(workingSettings.lastUpdateCheck);
        ui.getShowUpdateNotificationCheckBox().setSelected(workingSettings.showUpdateNotification);
        ui.getNextEditEnabledCheckBox().setSelected(NextEditSettings.getInstance().enabled);
        ResponseLanguage responseLanguage = workingSettings.responseLanguage != null
                                            ? workingSettings.responseLanguage
                                            : ResponseLanguage.ZH;
        ui.getLanguageComboBox().setSelectedItem(responseLanguage);
        ui.updateCheckBoxHintColors();

        // 加载高级配置
        ui.getShowAdvancedSettingsCheckBox().setSelected(workingSettings.showAdvancedSettings);
        JPanel advancedPanel = ui.getAdvancedSettingsContentPanel();
        if (advancedPanel != null) {
            advancedPanel.setVisible(workingSettings.showAdvancedSettings);
        }
        ui.getMaxRetriesField().setText(String.valueOf(runtimeSettings.maxRetries));
        ui.getTimeoutField().setText(String.valueOf(runtimeSettings.timeout));
        AIModelParameters modelParameters = workingSettings.modelParameters != null
                                            ? workingSettings.modelParameters
                                            : new AIModelParameters();
        ui.getTemperatureField().setText(modelParameters.temperature != null ? modelParameters.temperature : "auto");
        // 迁移老配置中的 maxTokens（从实际 token 数转换为 K 单位）
        String maxTokens = AIModelParameters.migrateMaxTokens(modelParameters.maxTokens);
        ui.getMaxTokensField().setText(maxTokens);
        ui.getTopPField().setText(modelParameters.topP != null ? modelParameters.topP : "auto");
        ui.getTopKField().setText(modelParameters.topK != null ? modelParameters.topK : "auto");
        ui.getPresencePenaltyField().setText(modelParameters.presencePenalty != null ? modelParameters.presencePenalty : "auto");

        IntelliAgentSettings intelliAgentSettings = workingSettings.intelliAgentSettings != null
                                                    ? workingSettings.intelliAgentSettings
                                                    : new IntelliAgentSettings();
        IntelliAgentPanel intelliAgentPanel = ui.getAgentPanel();
        intelliAgentPanel.getAutoStartCheckBox().setSelected(intelliAgentSettings.autoStart);

        // 加载统计设置
        StatisticsSettings statisticsSettings =
            StatisticsSettings.getInstance();
        ui.getStatisticsSettingsPanel().loadSettings(statisticsSettings);
        intelliAgentPanel.getAutoUpdateCheckBox().setSelected(intelliAgentSettings.autoUpdate);
        intelliAgentPanel.getDownloadUrlField().setText(intelliAgentSettings.downloadUrl != null ? intelliAgentSettings.downloadUrl : "");
        intelliAgentPanel.setLocalJarName(intelliAgentSettings.jarFileName, -1);
        intelliAgentPanel.updateAgentStatus(intelliAgentSettings);
        intelliAgentPanel.refreshAgentVersionInfo(intelliAgentSettings);

        // 加载可用服务商
        ui.getAvailableProvidersTableModel().setData(workingSettings.availableProviders);
        ui.getShowAvailableProvidersCheckBox().setSelected(workingSettings.showAvailableProviders);
        boolean showAvailableProviders = workingSettings.showAvailableProviders;
        ui.getAvailableProvidersPanel().setVisible(showAvailableProviders);
        JBLabel descriptionLabel = ui.getAvailableProvidersDescriptionLabel();
        if (descriptionLabel != null) {
            descriptionLabel.setVisible(showAvailableProviders);
        }
        refreshAutocompleteProviderItems();
    }

    /**
     * 获取 AI 提供者的配置设置
     * <p>
     * 该方法用于获取当前 AI 提供者的配置信息, 包括模型名称, 基础 URL, 凭证信息等, 并更新到工作设置中.
     * 同时会清空可用提供者列表, 更新运行时设置和模型参数, 并应用参数到配置中.
     *
     * @return AI 提供者的配置设置对象
     */
    @NotNull
    public AIProviderSettings getSettings() {
        workingSettings = collectSettingsFromUi(true, true);
        return workingSettings;
    }

    /**
     * 判断当前设置是否与给定的基线设置不同
     * <p>
     * 通过比较当前设置与传入的基线设置, 判断是否发生修改
     *
     * @param baseline 基线设置对象, 用于比较
     * @return 如果当前设置与基线设置不同, 返回 true; 否则返回 false
     */
    public boolean isModified(@NotNull AIProviderSettings baseline) {
        AIProviderSettings latest = collectSettingsFromUi(false, false);
        if (!latest.contentEquals(baseline)) {
            return true;
        }
        if (ui.getNextEditEnabledCheckBox().isSelected() != NextEditSettings.getInstance().enabled) {
            return true;
        }
        // 检查统计设置是否修改
        StatisticsSettings statisticsSettings =
            StatisticsSettings.getInstance();
        return ui.getStatisticsSettingsPanel().isModified(statisticsSettings);
    }

    /**
     * 从当前 UI 收集设置快照。
     * <p>
     * 根据调用场景选择是否持久化凭据、是否应用统计设置，避免在 isModified 中产生副作用。
     * </p>
     *
     * @param persistCredentialAndApiKey 是否持久化凭据和 API Key
     * @param applyStatisticsToGlobal     是否将统计设置应用到全局设置
     * @return 收集后的设置快照
     */
    @NotNull
    private AIProviderSettings collectSettingsFromUi(boolean persistCredentialAndApiKey,
                                                     boolean applyStatisticsToGlobal) {
        AIProviderSettings snapshot = workingSettings.copy();
        AIProviderType providerType = resolveSelectedProviderType();

        AIProviderConfig defaultConfig = snapshot.getDefaultProviderConfig(providerType);
        String modelName = Objects.toString(ui.getModelComboBox().getSelectedItem(), "").trim();
        defaultConfig.modelName = modelName.isEmpty() ? providerType.getDefaultModel() : modelName;
        defaultConfig.baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText().trim());
        defaultConfig.configurationVerified = Boolean.TRUE.equals(configurationVerified);

        if (persistCredentialAndApiKey) {
            updateCredentialIdAndSaveApiKey(defaultConfig);
        } else {
            defaultConfig.updateCredentialId(getCurrentApiKey());
        }

        snapshot.availableProviders.clear();
        ui.getAvailableProvidersTableModel().getData().forEach(snapshot::addAvailableProvider);

        AIRuntimeSettings runtimeSnapshot = snapshotRuntimeSettings();
        snapshot.runtimeSettings = runtimeSnapshot.copy();
        snapshot.showAvailableProviders = ui.getShowAvailableProvidersCheckBox().isSelected();
        snapshot.showAdvancedSettings = ui.getShowAdvancedSettingsCheckBox().isSelected();

        // verboseLogging 已迁移到全局配置
        snapshot.verboseLogging = ui.getVerboseLoggingCheckBox().isSelected();
        snapshot.lastUpdateCheck = ui.getLastUpdateCheckCheckBox().isSelected();
        snapshot.showUpdateNotification = ui.getShowUpdateNotificationCheckBox().isSelected();
        ResponseLanguage selectedLanguage = (ResponseLanguage) ui.getLanguageComboBox().getSelectedItem();
        snapshot.responseLanguage = selectedLanguage != null ? selectedLanguage : ResponseLanguage.ZH;

        AIModelParameters modelSnapshot = snapshotModelParameters();
        snapshot.modelParameters = modelSnapshot.copy();
        snapshot.intelliAgentSettings = ui.getAgentPanel().snapshotAgentSettings().copy();

        AIProviderConfig selectedAutocompleteProvider = (AIProviderConfig) ui.getAutocompleteProviderComboBox().getSelectedItem();
        snapshot.autocompleteProviderCredentialId = selectedAutocompleteProvider != null
                                                    ? selectedAutocompleteProvider.credentialId
                                                    : null;
        snapshot.aiProviderType = providerType;

        applyParametersToConfig(defaultConfig, modelSnapshot, runtimeSnapshot);
        snapshot.updateDefaultProviderConfig(providerType, defaultConfig);

        if (applyStatisticsToGlobal) {
            StatisticsSettings statisticsSettings = StatisticsSettings.getInstance();
            ui.getStatisticsSettingsPanel().apply(statisticsSettings);
        }
        return snapshot;
    }

    /**
     * 判断是否启用“下一次编辑”功能
     * <p>
     * 该方法用于获取用户界面中“下一次编辑”复选框的选中状态, 返回布尔值表示是否启用该功能.
     *
     * @return 如果“下一次编辑”复选框被选中, 则返回 true; 否则返回 false
     */
    public boolean isNextEditEnabled() {
        return ui.getNextEditEnabledCheckBox().isSelected();
    }

    /**
     * 获取对话框父组件
     * <p>
     * 通过查找主面板的祖先窗口来确定对话框的父组件. 如果找到窗口, 则返回该窗口; 否则返回主面板本身.
     *
     * @return 对话框的父组件, 可能是窗口或主面板
     */
    @NotNull
    private Component resolveDialogParent() {
        Window window = SwingUtilities.getWindowAncestor(ui.getMainPanel());
        return window != null ? window : ui.getMainPanel();
    }

    /**
     * 获取当前用户的 API 密钥
     * <p>
     * 从用户界面获取 API 密钥字段中的密码值, 并返回其字符串形式 (去除前后空格)
     *
     * @return 当前用户的 API 密钥字符串
     */
    @NotNull
    public String getCurrentApiKey() {
        return new String(ui.getApiKeyField().getPassword()).trim();
    }

    /**
     * 更新基础连接信息, 包括模型选择框和基础 URL,API 密钥等字段
     * <p>
     * 该方法根据当前选中的 AI 服务提供商类型, 更新模型选择框内容, 并设置默认模型, 基础 URL 和 API 密钥字段.
     *
     * @since 1.0
     */
    public void updateBasicConnectionInfo() {
        AIProviderType providerType = resolveSelectedProviderType();

        String currentModel = (String) ui.getModelComboBox().getSelectedItem();

        String preferredSelection = (currentModel != null && !currentModel.trim().isEmpty())
                                    ? currentModel
                                    : providerType.getDefaultModel();
        List<String> cachedModels = loadCachedModels(providerType);
        if (cachedModels.isEmpty()) {
            cachedModels = providerType.getSupportedModels();
        }
        ui.updateModelItems(cachedModels, preferredSelection);

        ui.getBaseUrlField().setText(providerType.getDefaultBaseUrl());
        ui.getApiKeyField().setEnabled(true);
        updateBaseUrlEditable(providerType);
    }

    /**
     * 保存当前选中的 AI 服务提供商的配置信息
     * <p>
     * 根据用户输入的显示名称解析对应的 AI 服务提供商类型, 获取默认配置, 并填充模型名称, 基础 URL, 配置验证状态等信息. 若存在 API 密钥, 则更新密钥信息并保存. 最后将配置应用到工作设置中.
     *
     * @param providerType 显示名称, 用于解析对应的 AI 服务提供商类型
     */
    public void saveCurrentProviderConfig(@NotNull AIProviderType providerType) {
        AIProviderConfig currentConfig = workingSettings.getDefaultProviderConfig(providerType);

        String modelName = Objects.toString(ui.getModelComboBox().getSelectedItem(), "").trim();
        currentConfig.modelName = modelName.isEmpty() ? providerType.getDefaultModel() : modelName;
        String baseUrlText = ui.getBaseUrlField().getText();
        String trimmedBaseUrl = baseUrlText != null ? baseUrlText.trim() : "";
        currentConfig.baseUrl = normalizeBaseUrl(trimmedBaseUrl.isEmpty()
                                                 ? providerType.getDefaultBaseUrl()
                                                 : trimmedBaseUrl);
        currentConfig.configurationVerified = Boolean.TRUE.equals(configurationVerified);

        String currentApiKey = getCurrentApiKey();
        if (!currentApiKey.trim().isEmpty()) {
            updateCredentialIdAndSaveApiKey(currentConfig);
        }
        AIModelParameters modelSnapshot = snapshotModelParameters();
        AIRuntimeSettings runtimeSnapshot = snapshotRuntimeSettings();
        applyParametersToConfig(currentConfig, modelSnapshot, runtimeSnapshot);
        workingSettings.modelParameters = modelSnapshot.copy();
        workingSettings.runtimeSettings = runtimeSnapshot.copy();
        workingSettings.updateDefaultProviderConfig(providerType, currentConfig);
    }

    /**
     * 加载默认的提供者配置信息并更新界面相关组件
     * <p>
     * 该方法用于获取当前选中的提供者类型, 加载默认的配置信息, 并将配置信息
     * 应用于界面组件, 如模型名称, 基础 URL, 配置验证状态等, 同时触发异步加载
     * API 密钥的操作.
     *
     * @since 1.0
     */
    public void loadDefaultProviderConfig() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        ui.getModelComboBox().setSelectedItem(config.modelName);
        ui.getBaseUrlField().setText(config.baseUrl);
        configurationVerified = config.configurationVerified;
        updateTestButtonState();
        loadApiKeyAsync(config.credentialId, providerType.getProviderId());
    }

    /**
     * 异步加载指定凭证 ID 的 API 密钥, 并根据预期提供者 ID 更新 UI
     * <p>
     * 该方法首先清空 API 密钥输入框, 然后根据传入的凭证 ID 异步加载 API 密钥.
     * 如果加载成功且当前选择的提供者 ID 与预期提供者 ID 匹配, 则更新输入框内容并刷新测试按钮状态.
     *
     * @param credentialId       希望加载 API 密钥的凭证 ID, 可为 null
     * @param expectedProviderId 预期的提供者 ID, 不能为空
     */
    private void loadApiKeyAsync(@Nullable String credentialId, @NotNull String expectedProviderId) {
        ui.getApiKeyField().setText("");
        if (credentialId == null || credentialId.trim().isEmpty()) {
            return;
        }
        credentialManager.loadApiKeyAsync(credentialId, key -> {
            String currentProviderId = resolveSelectedProviderType().getProviderId();
            if (!Objects.equals(currentProviderId, expectedProviderId)) {
                return;
            }
            ui.getApiKeyField().setText(key != null ? key : "");
            updateTestButtonState();
        });
    }

    /**
     * 测试连接配置是否有效
     * <p>
     * 该方法用于测试当前配置的 AI 服务连接是否有效. 它会创建一个测试配置, 使用当前的 API 密钥和设置信息, 尝试连接 AI 服务并验证配置是否正确.
     * 如果验证成功, 会更新界面状态并提示成功信息; 如果失败, 则提示错误信息. 测试完成后, 按钮状态将恢复为可点击状态.
     *
     * @since 1.0
     */
    public void testConnection() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        AIProviderConfig testConfig = config.copy();
        testConfig.modelName = Objects.toString(ui.getModelComboBox().getSelectedItem(), "").trim();
        testConfig.baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText());
        testConfig.updateCredentialId(getCurrentApiKey());
        AIModelParameters modelSnapshot = snapshotModelParameters();
        AIRuntimeSettings runtimeSnapshot = snapshotRuntimeSettings();
        applyParametersToConfig(testConfig, modelSnapshot, runtimeSnapshot);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(testConfig);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(resolveDialogParent(),
                                          AICommonBundle.message("settings.error.provider.create.failed"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        StatusIndicatorButton testButton = ui.getTestConnectionButton();
        testButton.setEnabled(false);
        testButton.setText(AICommonBundle.message("settings.test.connection.testing"));
        testButton.setWarningStatus();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ValidationResult result = provider.validateConfiguration(getCurrentApiKey());
                SwingUtilities.invokeLater(() -> {
                    if (result.isSuccess()) {
                        configurationVerified = true;
                        updateTestButtonState();
                        addAvailableProvider(testConfig, providerType);
                        String message = buildSuccessMessage(result.getMessage(), testConfig);
                        javax.swing.Icon icon = AICommonIcons.getProviderIcon64(providerType);
                        JOptionPane.showMessageDialog(resolveDialogParent(),
                                                      message,
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE,
                                                      icon);
                    } else {
                        configurationVerified = false;
                        updateTestButtonState();
                        removeAvailableProvider(testConfig.credentialId);
                        // [HOM-194] 用统一 helper 弹窗: 同时把详情写入 idea.log,
                        // 并提供 "复制详情" / "Show Log" 两个辅助按钮
                        showTestErrorDialog(result.getFullErrorMessage(), result.getThrowable());
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    configurationVerified = false;
                    updateTestButtonState();
                    removeAvailableProvider(testConfig.credentialId);
                    showTestErrorDialog(
                        AICommonBundle.message("settings.test.connection.error", e.getMessage()), e);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    testButton.setText(AICommonBundle.message("settings.test.connection"));
                    testButton.setEnabled(true);
                });
            }
        });
    }

    /**
     * 测试连接失败时展示统一弹窗
     * <p>
     * 同一处入口完成三件事:
     * <ol>
     *   <li>用 {@code log.warn} 写入 idea.log (slf4j → IDEA 自带 logger), 即使用户关掉弹窗,
     *       详情仍然持久化, 方便用户事后 / 远程协助时定位</li>
     *   <li>弹窗以 ERROR 样式展示完整错误 (含 HTTP status + body 摘要 + cause 信息)</li>
     *   <li>提供 "复制详情" 一键 copy 到系统剪贴板, 用户可以直接贴到 GitHub issue;
     *       "Show Log" 直接打开 idea.log 所在目录</li>
     * </ol>
     * 该方法只在 EDT 线程调用 (UI 操作).
     *
     * @param message 已经经过 {@code AIServiceException.build} / i18n 处理的用户可读错误描述,
     *                可能跨多行, 不为 null
     * @param cause   原始异常, 可为 null. 不为 null 时会和 message 一起写入 idea.log 的堆栈段
     */
    private void showTestErrorDialog(@NotNull String message, @Nullable Throwable cause) {
        if (cause != null) {
            log.warn("AI test connection failed: " + message, cause);
        } else {
            log.warn("AI test connection failed: " + message);
        }

        Object[] options = {"OK", "复制详情", "Show Log"};
        int choice = JOptionPane.showOptionDialog(
            resolveDialogParent(),
            message,
            AICommonBundle.message("settings.test.result.title"),
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.ERROR_MESSAGE,
            null,
            options,
            options[0]
        );
        if (choice == 1) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(message), null);
            } catch (Throwable t) {
                // 剪贴板异常 (如 headless / 权限) 仅记录, 不应再开第二个弹窗打断用户
                log.warn("Failed to copy error detail to clipboard", t);
            }
        } else if (choice == 2) {
            openLogFile();
        }
    }

    /**
     * 打开 idea.log 所在目录
     * <p>
     * 优先反射调用 {@code dev.dong4j.zeka.stack.idea.plugin.common.action.ShowLogAction#showLog()}
     * (HOM-195 / Phase 3) 以便和插件菜单 + Find Action 入口保持一致. 但本 PR 基于
     * dev 分支, Phase 3 PR (#81) 尚未合并, 所以做了降级: 直接用 {@link PathManager#getLogPath()}
     * 拼接 idea.log 并交给 {@link RevealFileAction#openFile} 打开. 行为对用户等价,
     * 等 HOM-195 合并后反射路径会自动接管, 无需再次改这里.
     */
    private static void openLogFile() {
        try {
            Class<?> clazz = Class.forName(
                "dev.dong4j.zeka.stack.idea.plugin.common.action.ShowLogAction");
            clazz.getDeclaredMethod("showLog").invoke(null);
            return;
        } catch (ClassNotFoundException ignored) {
            // Phase 3 (HOM-195) 尚未合并, 走下面的降级路径
        } catch (Throwable t) {
            log.warn("Failed to call ShowLogAction.showLog(), falling back", t);
        }
        try {
            File logFile = new File(PathManager.getLogPath(), "idea.log");
            RevealFileAction.openFile(logFile);
        } catch (Throwable t) {
            log.warn("Failed to reveal idea.log", t);
        }
    }

    /**
     * 刷新可用的 AI 模型列表
     * <p>
     * 该方法用于根据当前配置刷新可用的 AI 模型, 并更新 UI 中的模型下拉框.
     * 在刷新过程中会禁用刷新按钮, 并显示加载状态. 刷新完成后根据结果更新按钮状态和提示信息.
     */
    @SuppressWarnings("D")
    public void refreshModels() {
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        AIProviderConfig refreshConfig = config.copy();
        refreshConfig.providerType = providerType;
        refreshConfig.modelName = Objects.toString(ui.getModelComboBox().getSelectedItem(), "").trim();
        refreshConfig.baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText());
        refreshConfig.updateCredentialId(getCurrentApiKey());
        AIModelParameters modelSnapshot = snapshotModelParameters();
        AIRuntimeSettings runtimeSnapshot = snapshotRuntimeSettings();
        applyParametersToConfig(refreshConfig, modelSnapshot, runtimeSnapshot);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(refreshConfig);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(resolveDialogParent(),
                                          AICommonBundle.message("settings.error.provider.create.failed.details"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        String baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText());
        if (baseUrl.isEmpty()) {
            JOptionPane.showMessageDialog(resolveDialogParent(),
                                          AICommonBundle.message("settings.error.base.url.missing"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        String apiKey = getCurrentApiKey();
        if (providerType.requiresApiKey() && apiKey.trim().isEmpty()) {
            JOptionPane.showMessageDialog(resolveDialogParent(),
                                          AICommonBundle.message("settings.error.api.key.missing"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        StatusIndicatorButton refreshButton = ui.getRefreshModelsButton();
        refreshButton.setEnabled(false);
        refreshButton.setText(AICommonBundle.message("settings.refresh.models.testing"));
        refreshButton.setWarningStatus();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String currentModelName = getSelectedModelName();
                List<String> models = provider.getAvailableModels(apiKey);
                models = filterLanguageModels(models);
                models.sort(String::compareToIgnoreCase);
                saveCachedModels(providerType, models);
                List<String> finalModels = models;
                SwingUtilities.invokeLater(() -> {
                    if (!finalModels.isEmpty()) {
                        final String preferredSelection = getPreferredSelection(refreshConfig, finalModels, currentModelName);
                        ui.updateModelItemsAndShowPopup(finalModels, preferredSelection);
                        refreshModelsSuccess = true;
                        // JOptionPane.showMessageDialog(resolveDialogParent(),
                        //                               AICommonBundle.message("settings.refresh.models.success", finalModels.size()),
                        //                               AICommonBundle.message("settings.test.result.title"),
                        //                               JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        refreshModelsSuccess = false;
                        ui.updateModelItems(List.of(), null);
                        String errorMessage = AICommonBundle.message("settings.refresh.models.empty");
                        if (providerType.requiresApiKey() && apiKey.trim().isEmpty()) {
                            errorMessage = AICommonBundle.message("settings.error.api.key.missing");
                        }
                        JOptionPane.showMessageDialog(resolveDialogParent(),
                                                      errorMessage,
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.WARNING_MESSAGE);
                    }
                    updateRefreshButtonState();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    refreshModelsSuccess = false;
                    updateRefreshButtonState();
                    String errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.trim().isEmpty()) {
                        errorMessage = e.getClass().getSimpleName();
                    }
                    JOptionPane.showMessageDialog(resolveDialogParent(),
                                                  AICommonBundle.message("settings.refresh.models.failed", errorMessage),
                                                  AICommonBundle.message("settings.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshButton.setText(AICommonBundle.message("settings.refresh.models"));
                    refreshButton.setEnabled(true);
                });
            }
        });
    }

    /**
     * 根据配置和模型列表选择最优的模型名称
     * <p>
     * 该方法用于从可用模型列表中选择一个首选模型名称, 优先级顺序为: 配置中指定的默认模型名称 > 当前选中的模型名称 > 列表中的第一个模型.
     * 若所有条件均不满足, 则返回列表中的第一个模型名称.
     *
     * @param refreshConfig    当前刷新配置对象, 用于获取默认模型名称
     * @param finalModels      过滤后的可用模型名称列表, 不能为空
     * @param currentModelName 当前用户选择的模型名称, 用于作为备选
     * @return 优先级最高的模型名称, 若列表为空则返回 null
     */
    private static String getPreferredSelection(AIProviderConfig refreshConfig, List<String> finalModels, String currentModelName) {
        String defaultModelName = refreshConfig.modelName;
        String preferredSelection;
        if (defaultModelName != null && !defaultModelName.trim().isEmpty() && finalModels.contains(defaultModelName)) {
            preferredSelection = defaultModelName;
        } else if (!currentModelName.trim().isEmpty() && finalModels.contains(currentModelName)) {
            preferredSelection = currentModelName;
        } else {
            preferredSelection = finalModels.getFirst();
        }
        return preferredSelection;
    }

    /**
     * 过滤掉包含特定关键词的模型名称
     * <p>
     * 该方法用于从模型列表中移除名称中包含 "vl" 或 "tts" 的模型, 通常用于过滤掉视觉或语音相关模型.
     * 返回的列表中仅包含符合过滤条件的模型名称.
     *
     * @param models 待过滤的模型名称列表, 可为 null 或空列表
     * @return 过滤后的模型名称列表, 若输入为空或 null, 则返回空列表
     */
    private static List<String> filterLanguageModels(List<String> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }
        List<String> filtered = new ArrayList<>(models.size());
        for (String model : models) {
            if (model == null) {
                continue;
            }
            String lowerName = model.toLowerCase(java.util.Locale.ROOT);
            if (lowerName.contains("vl") || lowerName.contains("tts")) {
                continue;
            }
            filtered.add(model);
        }
        return filtered;
    }

    /**
     * 从缓存文件中加载支持的 AI 模型列表
     * <p>
     * 根据指定的 AI 服务提供商类型, 查找并读取本地缓存文件中的模型名称列表. 若缓存文件不存在或读取失败, 则返回空列表.
     * 加载后的模型列表会经过语言模型过滤 (如移除包含 "vl" 或 "tts" 的模型), 并返回过滤后的结果.
     *
     * @param providerType AI 服务提供商类型, 用于确定缓存文件路径
     * @return 缓存中加载的模型名称列表, 若失败或无缓存则返回空列表
     * @since 1.0
     */
    @NotNull
    private static List<String> loadCachedModels(@NotNull AIProviderType providerType) {
        Path cachePath = resolveModelsCachePath(providerType);
        if (cachePath == null || !Files.exists(cachePath)) {
            return List.of();
        }
        try {
            List<String> cached = Files.readAllLines(cachePath, StandardCharsets.UTF_8);
            return filterLanguageModels(cached);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    /**
     * 将指定 AI 服务提供商支持的模型列表缓存到本地文件系统
     * <p>
     * 该方法根据传入的 AI 服务提供商类型, 解析缓存路径并写入模型列表. 如果缓存目录不存在, 则自动创建. 写入操作会覆盖现有文件内容.
     * 缓存失败时忽略异常, 不抛出错误.
     *
     * @param providerType AI 服务提供商类型, 不能为空
     * @param models       支持的模型名称列表, 不能为空
     */
    private static void saveCachedModels(@NotNull AIProviderType providerType, @NotNull List<String> models) {
        Path cachePath = resolveModelsCachePath(providerType);
        if (cachePath == null) {
            return;
        }
        try {
            Files.createDirectories(cachePath.getParent());
            Files.write(cachePath, models, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
        } catch (Exception ignored) {
            // ignore cache failures
        }
    }

    /**
     * 根据 AI 服务提供商类型解析模型缓存文件的路径
     * <p>
     * 该方法通过系统用户主目录构建模型缓存文件的完整路径, 文件名格式为 {@code models-<providerId>.txt}, 存储在 {@code ~/.zeka-stack/plugin/engine/} 目录下.
     * 如果用户主目录无法获取或为空, 则返回 null.
     *
     * @param providerType AI 服务提供商类型, 不能为空
     * @return 模型缓存文件的路径, 如果无法解析则返回 null
     */
    @Nullable
    private static Path resolveModelsCachePath(@NotNull AIProviderType providerType) {
        String homeDir = System.getProperty("user.home");
        if (homeDir == null || homeDir.trim().isEmpty()) {
            return null;
        }
        return StorageUtil.resolve(EngineContents.PLUGIN_SIMPLE_NAME,
                                   "models-" + providerType.getProviderId());
    }

    /**
     * 添加一个可用的 AI 服务提供商配置
     * <p>
     * 该方法用于将指定的 AI 服务提供商配置添加到可用列表中. 如果配置的备注为空, 则自动填充当前时间戳.
     * 同时更新凭证 ID 和 API 密钥, 并更新界面相关组件的状态.
     *
     * @param config       要添加的 AI 服务提供商配置对象
     * @param providerType AI 服务提供商类型
     */
    public void addAvailableProvider(@NotNull AIProviderConfig config, @NotNull AIProviderType providerType) {
        AIProviderConfig copy = config.copy();
        if (copy.remark == null || copy.remark.isEmpty()) {
            copy.remark = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
        }
        copy.providerType = providerType;
        copy.configurationVerified = true;

        updateCredentialIdAndSaveApiKey(copy);

        workingSettings.addAvailableProvider(copy);
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.addAvailableProvider(copy);

        ui.getAvailableProvidersTableModel().setData(workingSettings.availableProviders);
        ui.getShowAvailableProvidersCheckBox().setSelected(true);
        ui.getAvailableProvidersPanel().setVisible(true);
        refreshAutocompleteProviderItems();
    }

    /**
     * 更新凭证 ID 并保存 API 密钥
     * <p>
     * 根据传入的 AIProviderConfig 对象, 获取当前 API 密钥, 更新配置中的凭证 ID, 并在条件满足时将 API 密钥保存到凭证管理器中.
     * <p>
     * 密码保存操作在后台线程中执行, 避免阻塞 EDT.
     *
     * @param config AIProviderConfig 对象, 用于更新凭证 ID 和保存 API 密钥
     */
    private void updateCredentialIdAndSaveApiKey(@NotNull AIProviderConfig config) {
        String apiKey = getCurrentApiKey();
        config.updateCredentialId(apiKey);
        if (!apiKey.trim().isEmpty() && config.credentialId != null) {
            // 密码保存是慢操作, 需要在后台线程执行
            String credentialId = config.credentialId;
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> credentialManager.setApiKey(credentialId, apiKey),
                ApplicationManager.getApplication()::executeOnPooledThread);

            // 等待密码保存完成, 但设置超时避免无限等待
            try {
                future.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                // 静默处理异常, 避免影响主流程
                // 密码保存失败不会影响配置的保存, 只是下次需要重新输入
            }
        }
    }

    /**
     * 移除指定凭证 ID 对应的可用提供者
     * <p>
     * 如果凭证 ID 为空或仅包含空白字符, 则直接返回. 否则, 从工作设置和全局设置中移除该凭证对应的可用提供者, 并更新界面数据.
     *
     * @param credentialId 凭证 ID
     */
    public void removeAvailableProvider(@Nullable String credentialId) {
        if (credentialId == null || credentialId.trim().isEmpty()) {
            return;
        }
        workingSettings.removeAvailableProvider(credentialId);
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.removeAvailableProvider(credentialId);

        ui.getAvailableProvidersTableModel().setData(workingSettings.availableProviders);
        refreshAutocompleteProviderItems();
    }

    /**
     * 删除指定行的可用提供者配置
     * <p>
     * 根据表格中指定行的索引获取对应的提供者配置, 若配置存在则弹出确认对话框, 确认后根据配置的凭证 ID 删除该提供者.
     *
     * @param rowIndex 表格中提供者的行索引
     */
    public void removeAvailableProvider(int rowIndex) {
        AIProviderConfig config = ui.getAvailableProvidersTableModel().getProviderConfig(rowIndex);
        if (config == null) {
            return;
        }
        String provider = config.providerType != null
                          ? config.providerType.getDisplayName()
                          : AICommonBundle.message("settings.available.providers.unknown");
        String model = config.modelName != null ? config.modelName : "";
        Component dialogParent = getDialogParent();
        int result = JOptionPane.showConfirmDialog(dialogParent,
                                                   AICommonBundle.message("settings.available.providers.delete.confirm", provider, model),
                                                   AICommonBundle.message("settings.available.providers.delete.title"),
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            removeAvailableProvider(config.credentialId);
        }
    }

    /**
     * 清除所有可用的提供者设置
     * <p>
     * 显示确认对话框, 确认是否清除所有可用的提供者设置. 如果用户确认, 则清除本地和全局设置中的可用提供者, 并刷新界面数据.
     *
     * @since 1.0
     */
    public void clearAllAvailableProviders() {
        if (workingSettings.availableProviders.isEmpty()) {
            return;
        }
        Component dialogParent = getDialogParent();
        int result = JOptionPane.showConfirmDialog(dialogParent,
                                                   AICommonBundle.message("settings.available.providers.clear.confirm",
                                                                          workingSettings.availableProviders.size()),
                                                   AICommonBundle.message("settings.available.providers.clear.title"),
                                                   JOptionPane.YES_NO_OPTION,
                                                   JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            workingSettings.clearAvailableProviders();
            AIProviderSettings globalSettings = AIProviderSettings.getInstance();
            globalSettings.clearAvailableProviders();

            ui.getAvailableProvidersTableModel().setData(List.of());
            refreshAutocompleteProviderItems();
        }
    }

    /**
     * 刷新自动补全提供者下拉框的可用提供者列表项
     * <p>
     * 根据当前工作设置中的可用提供者列表和自动补全提供者凭证 ID, 更新用户界面中自动补全提供者下拉框的内容.
     * 此方法用于在配置变更或数据更新后, 确保下拉框显示最新的提供者选项.
     *
     */
    private void refreshAutocompleteProviderItems() {
        ui.setAutocompleteProviderItems(workingSettings.availableProviders, workingSettings.autocompleteProviderCredentialId);
    }

    /**
     * 更新测试按钮的状态图标, 根据配置验证结果显示不同的状态指示灯
     * <p>
     * 该方法通过获取测试连接按钮, 并根据配置验证是否通过的状态, 设置相应的状态指示图标.
     */
    public void updateTestButtonState() {
        StatusIndicatorButton testButton = ui.getTestConnectionButton();
        if (configurationVerified != null && configurationVerified) {
            testButton.setSuccessStatus();
        } else {
            testButton.setErrorStatus();
        }
    }

    /**
     * 更新刷新按钮的状态图标, 根据刷新模型操作的成功状态设置不同的颜色图标.
     * <p>
     * 该方法通过获取刷新按钮组件, 并根据刷新模型操作是否成功, 设置对应状态的图标.
     */
    public void updateRefreshButtonState() {
        StatusIndicatorButton refreshButton = ui.getRefreshModelsButton();
        if (refreshModelsSuccess == null) {
            refreshButton.setWarningStatus();
        } else if (refreshModelsSuccess) {
            refreshButton.setSuccessStatus();
        } else {
            refreshButton.setErrorStatus();
        }
    }

    /**
     * 下载 Agent jar
     */
    public void downloadAgentJar() {
        IntelliAgentPanel intelliAgentPanel = ui.getAgentPanel();
        IntelliAgentSettings snapshot = intelliAgentPanel.snapshotAgentSettings();
        intelliAgentPanel.downloadAgentJar(snapshot);
    }

    /**
     * 启动或停止 Agent 本地代理
     */
    public void toggleAgent() {
        IntelliAgentPanel intelliAgentPanel = ui.getAgentPanel();
        IntelliAgentSettings snapshot = intelliAgentPanel.snapshotAgentSettings();
        intelliAgentPanel.toggleAgentAgent(snapshot);
    }

    /**
     * 根据指定的 AI 服务提供商类型更新基础 URL 字段的可编辑状态
     * <p>
     * 该方法会根据传入的 AI 服务提供商类型设置基础 URL 字段的可编辑性, 并在不可编辑时设置默认的基础 URL 值.
     *
     * @param providerType AI 服务提供商类型, 不能为空
     */
    private void updateBaseUrlEditable(@NotNull AIProviderType providerType) {
        JBTextField baseUrlField = ui.getBaseUrlField();
        baseUrlField.setEditable(providerType.isBaseUrlEditable());
        if (!providerType.isBaseUrlEditable()) {
            baseUrlField.setText(providerType.getDefaultBaseUrl());
        }
    }

    /**
     * 从 UI 组件中获取 AI 模型参数的快照
     * <p>
     * 该方法用于从用户界面的各个输入框中读取当前设置的 AI 模型参数, 并创建一个 AIModelParameters 对象进行保存.
     * 参数包括温度, 最大令牌数,Top P,Top K 和存在惩罚值等.
     * <p>
     * 如果输入为空, 则设置为 "auto".
     *
     * @return 包含当前 UI 设置的 AI 模型参数对象
     */
    @NotNull
    private AIModelParameters snapshotModelParameters() {
        AIModelParameters params = new AIModelParameters();

        // 如果输入为空, 设置为 "auto"
        String temperature = ui.getTemperatureField().getText().trim();
        params.temperature = temperature.isEmpty() ? "auto" : temperature;

        String maxTokens = ui.getMaxTokensField().getText().trim();
        params.maxTokens = maxTokens.isEmpty() ? "auto" : maxTokens;

        String topP = ui.getTopPField().getText().trim();
        params.topP = topP.isEmpty() ? "auto" : topP;

        String topK = ui.getTopKField().getText().trim();
        params.topK = topK.isEmpty() ? "auto" : topK;

        String presencePenalty = ui.getPresencePenaltyField().getText().trim();
        params.presencePenalty = presencePenalty.isEmpty() ? "auto" : presencePenalty;

        return params;
    }

    /**
     * 创建当前运行时设置的快照
     * <p>
     * 从当前工作设置中复制运行时配置, 并根据 UI 组件获取相关参数, 生成一个新的运行时设置对象.
     *
     * @return 当前运行时设置的快照
     */
    @NotNull
    private AIRuntimeSettings snapshotRuntimeSettings() {
        AIRuntimeSettings snapshot = new AIRuntimeSettings();
        AIRuntimeSettings baseline = workingSettings.runtimeSettings != null ? workingSettings.runtimeSettings : new AIRuntimeSettings();
        snapshot.waitDuration = baseline.waitDuration;
        // verboseLogging 已迁移到全局配置，不在这里设置
        // 解析 maxRetries，如果为空或无效则使用默认值 2
        String maxRetriesText = ui.getMaxRetriesField().getText().trim();
        if (maxRetriesText.isEmpty() || "auto".equalsIgnoreCase(maxRetriesText)) {
            snapshot.maxRetries = 2; // 默认值
        } else {
            try {
                snapshot.maxRetries = Integer.parseInt(maxRetriesText);
            } catch (NumberFormatException e) {
                snapshot.maxRetries = 2; // 默认值
            }
        }

        // 解析 timeout，如果为空或无效则使用默认值 10
        String timeoutText = ui.getTimeoutField().getText().trim();
        if (timeoutText.isEmpty() || "auto".equalsIgnoreCase(timeoutText)) {
            snapshot.timeout = 10; // 默认值
        } else {
            try {
                snapshot.timeout = Integer.parseInt(timeoutText);
            } catch (NumberFormatException e) {
                snapshot.timeout = 10; // 默认值
            }
        }
        return snapshot;
    }


    /**
     * 将模型参数和运行时设置应用到目标配置对象中
     * <p>
     * 该方法用于将模型参数快照和运行时设置快照复制到目标配置对象中, 实现配置的更新或初始化.
     *
     * @param target          目标配置对象, 用于接收复制后的模型参数和运行时设置
     * @param modelSnapshot   模型参数快照, 包含需要复制的模型参数信息
     * @param runtimeSnapshot 运行时设置快照, 包含需要复制的运行时设置信息
     */
    private void applyParametersToConfig(@NotNull AIProviderConfig target,
                                         @NotNull AIModelParameters modelSnapshot,
                                         @NotNull AIRuntimeSettings runtimeSnapshot) {
        target.modelParameters = modelSnapshot.copy();
        target.runtimeSettings = runtimeSnapshot.copy();
    }

    /**
     * 解析并返回选中的 AI 服务提供商类型
     * <p>
     * 从 UI 组件中获取选中的提供商显示名称, 并根据名称解析对应的 AIProviderType 类型.
     * 如果解析失败或未选择, 则默认返回 QIANWEN 类型.
     *
     * @return 选中的 AI 服务提供商类型, 若解析失败或未选择则返回 QIANWEN
     */
    private AIProviderType resolveSelectedProviderType() {
        AIProviderType type = ui.getSelectedProviderType();
        return type != null ? type : AIProviderType.QIANWEN;
    }

    /**
     * 获取当前选中的模型名称
     * <p>
     * 从下拉框中获取用户选择的模型名称, 若下拉框中未选择项, 则从编辑器中获取当前输入的模型名称.
     * 若均未获取到, 则根据当前选择的提供者类型获取默认配置中的模型名称.
     *
     * @return 当前选中的模型名称
     */
    @NotNull
    private String getSelectedModelName() {
        ComboBox<String> modelComboBox = ui.getModelComboBox();
        Object selected = modelComboBox.getSelectedItem();
        if (selected != null) {
            return selected.toString().trim();
        }
        AIProviderType providerType = resolveSelectedProviderType();
        AIProviderConfig config = workingSettings.getDefaultProviderConfig(providerType);
        return config.modelName != null ? config.modelName : "";
    }

    /**
     * 对基础 URL 进行规范化处理, 去除末尾的斜杠
     * <p>
     * 如果传入的 URL 为 null 或为空字符串, 则返回空字符串. 如果 URL 以斜杠结尾, 则去掉最后一个斜杠; 否则返回原 URL.
     *
     * @param baseUrl 需要规范化的基础 URL, 可以为 null
     * @return 规范化后的基础 URL
     */
    @NotNull
    private static String normalizeBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 构建测试成功的消息，包含高级配置信息
     * <p>
     * 展示当前的高级配置参数，并告知用户可以在可用服务商列表中修改这些参数。
     *
     * @param baseMessage 基础成功消息
     * @param config      AI 提供商配置
     * @return 完整的成功消息
     */
    @NotNull
    private String buildSuccessMessage(@NotNull String baseMessage, @NotNull AIProviderConfig config) {

        AIRuntimeSettings runtime = config.runtimeSettings != null ? config.runtimeSettings : new AIRuntimeSettings();

        AIModelParameters modelParams = config.modelParameters != null ? config.modelParameters : new AIModelParameters();

        return baseMessage + "\n\n" +
               "=== 当前高级配置 ===\n\n" +

               // 运行时设置
               "【运行时设置】\n" +
               String.format("  最大重试次数: %d\n", runtime.maxRetries) +
               String.format("  请求超时: %d 秒\n", runtime.timeout) +
               "\n" +

               // 模型参数
               "【模型参数】\n" +
               String.format("  温度 (Temperature): %s\n", modelParams.temperature != null ? modelParams.temperature : "auto") +
               String.format("  最大 Token 数: %s\n", modelParams.maxTokens != null ? modelParams.maxTokens : "auto") +
               String.format("  Top P: %s\n", modelParams.topP != null ? modelParams.topP : "auto") +
               String.format("  Top K: %s\n", modelParams.topK != null ? modelParams.topK : "auto") +
               String.format("  存在惩罚 (Presence Penalty): %s\n", modelParams.presencePenalty != null ? modelParams.presencePenalty : "auto"
                            ) +
               "\n" +

               // 说明文字
               "💡 提示：测试连接之前先修改高级参数以适配不同场景需求\n";
    }

    /**
     * 获取对话框的父组件
     * <p> 该方法通过查找主面板的顶层窗口来获取对话框的父组件. 如果找到了顶层窗口 (Window),
     * 则返回该窗口; 否则返回主面板本身作为父组件.
     *
     * @return 对话框的父组件, 如果找到顶层窗口则返回 Window, 否则返回主面板
     */
    private Component getDialogParent() {
        Component parent = ui.getMainPanel();
        Component window = SwingUtilities.getWindowAncestor(parent);
        return window != null ? window : parent;
    }
}
