package dev.dong4j.zeka.stack.idea.plugin.common.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import icons.AICommonIcons;

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
public final class AIProviderConfigController {

    /** 负责管理 AI 身份凭证的工具类实例 */
    private final AICredentialManager credentialManager;
    /** 响应监听器, 用于处理 AI 响应结果 */
    private final AIResponseListener responseListener;
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
     * @param responseListener  响应监听器, 可以为空
     * @param ui                UI 组件, 不能为空
     */
    public AIProviderConfigController(@NotNull AICredentialManager credentialManager,
                                      @Nullable AIResponseListener responseListener,
                                      @NotNull AIProviderConfigUI ui) {
        this.credentialManager = credentialManager;
        this.responseListener = responseListener;
        this.ui = ui;
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

        ui.getProviderComboBox().setSelectedItem(defaultProviderType.getDisplayName());
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
        ui.updateCheckBoxHintColors();

        // 加载高级配置
        ui.getShowAdvancedSettingsCheckBox().setSelected(workingSettings.showAdvancedSettings);
        JPanel advancedPanel = ui.getAdvancedSettingsContentPanel();
        if (advancedPanel != null) {
            advancedPanel.setVisible(workingSettings.showAdvancedSettings);
        }
        ui.getMaxRetriesSpinner().setValue(runtimeSettings.maxRetries);
        ui.getTimeoutSpinner().setValue(runtimeSettings.timeout);
        AIModelParameters modelParameters = workingSettings.modelParameters != null
                                            ? workingSettings.modelParameters
                                            : new AIModelParameters();
        ui.getTemperatureSpinner().setValue(modelParameters.temperature);
        ui.getMaxTokensSpinner().setValue(Math.max(0.1d, modelParameters.maxTokens / 1000.0d));
        ui.getTopPSpinner().setValue(modelParameters.topP);
        ui.getTopKSpinner().setValue(modelParameters.topK);
        ui.getPresencePenaltySpinner().setValue(modelParameters.presencePenalty);

        // 加载可用服务商
        ui.getAvailableProvidersTableModel().setData(workingSettings.availableProviders);
        ui.getShowAvailableProvidersCheckBox().setSelected(workingSettings.showAvailableProviders);
        boolean showAvailableProviders = workingSettings.showAvailableProviders;
        ui.getAvailableProvidersPanel().setVisible(showAvailableProviders);
        JBLabel descriptionLabel = ui.getAvailableProvidersDescriptionLabel();
        if (descriptionLabel != null) {
            descriptionLabel.setVisible(showAvailableProviders);
        }
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
        AIProviderType providerType = resolveSelectedProviderType();

        AIProviderConfig defaultConfig = workingSettings.getDefaultProviderConfig(providerType);
        String modelName = Objects.toString(ui.getModelComboBox().getEditor().getItem(), "").trim();
        defaultConfig.modelName = modelName.isEmpty() ? providerType.getDefaultModel() : modelName;
        defaultConfig.baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText().trim());
        defaultConfig.configurationVerified = Boolean.TRUE.equals(configurationVerified);

        updateCredentialIdAndSaveApiKey(defaultConfig);
        workingSettings.availableProviders.clear();
        ui.getAvailableProvidersTableModel().getData().forEach(workingSettings::addAvailableProvider);

        AIRuntimeSettings runtimeSnapshot = snapshotRuntimeSettings();
        workingSettings.runtimeSettings = runtimeSnapshot.copy();
        workingSettings.showAvailableProviders = ui.getShowAvailableProvidersCheckBox().isSelected();

        workingSettings.showAdvancedSettings = ui.getShowAdvancedSettingsCheckBox().isSelected();

        // verboseLogging 已迁移到全局配置
        workingSettings.verboseLogging = ui.getVerboseLoggingCheckBox().isSelected();

        AIModelParameters modelSnapshot = snapshotModelParameters();
        workingSettings.modelParameters = modelSnapshot.copy();

        workingSettings.aiProviderType = providerType;

        applyParametersToConfig(defaultConfig, modelSnapshot, runtimeSnapshot);
        workingSettings.updateDefaultProviderConfig(providerType, defaultConfig);

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
        AIProviderSettings latest = getSettings();
        return !latest.contentEquals(baseline);
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

        ui.getModelComboBox().removeAllItems();
        for (String model : providerType.getSupportedModels()) {
            ui.getModelComboBox().addItem(model);
        }

        if (currentModel != null && !currentModel.trim().isEmpty()) {
            ui.getModelComboBox().setSelectedItem(currentModel);
        } else {
            ui.getModelComboBox().setSelectedItem(providerType.getDefaultModel());
        }

        ui.getBaseUrlField().setText(providerType.getDefaultBaseUrl());
        ui.getApiKeyField().setEnabled(true);
        updateBaseUrlEditable(providerType);
    }

    /**
     * 保存当前选中的 AI 服务提供商的配置信息
     * <p>
     * 根据用户输入的显示名称解析对应的 AI 服务提供商类型, 获取默认配置, 并填充模型名称, 基础 URL, 配置验证状态等信息. 若存在 API 密钥, 则更新密钥信息并保存. 最后将配置应用到工作设置中.
     *
     * @param displayName 显示名称, 用于解析对应的 AI 服务提供商类型
     */
    public void saveCurrentProviderConfig(String displayName) {
        AIProviderType providerType = resolveSelectedProviderType(displayName);
        AIProviderConfig currentConfig = workingSettings.getDefaultProviderConfig(providerType);

        String modelName = Objects.toString(ui.getModelComboBox().getEditor().getItem(), "").trim();
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
        testConfig.modelName = Objects.toString(ui.getModelComboBox().getEditor().getItem(), "").trim();
        testConfig.baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText());
        testConfig.updateCredentialId(getCurrentApiKey());
        AIModelParameters modelSnapshot = snapshotModelParameters();
        AIRuntimeSettings runtimeSnapshot = snapshotRuntimeSettings();
        applyParametersToConfig(testConfig, modelSnapshot, runtimeSnapshot);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(testConfig);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ui.getMainPanel(),
                                          AICommonBundle.message("settings.error.provider.create.failed"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        JButton testButton = ui.getTestConnectionButton();
        testButton.setEnabled(false);
        testButton.setText(AICommonBundle.message("settings.test.connection.testing"));
        testButton.setIcon(ui.createStatusDotIcon(Gray._158));

        new Thread(() -> {
            try {
                ValidationResult result = provider.validateConfiguration(getCurrentApiKey());
                SwingUtilities.invokeLater(() -> {
                    if (result.isSuccess()) {
                        configurationVerified = true;
                        updateTestButtonState();
                        addAvailableProvider(testConfig, providerType);
                        String message = buildSuccessMessage(result.getMessage(), testConfig);
                        javax.swing.Icon icon = AICommonIcons.getProviderIcon64(providerType);
                        JOptionPane.showMessageDialog(ui.getMainPanel(),
                                                      message,
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE,
                                                      icon);
                    } else {
                        configurationVerified = false;
                        updateTestButtonState();
                        removeAvailableProvider(testConfig.credentialId);
                        JOptionPane.showMessageDialog(ui.getMainPanel(),
                                                      result.getFullErrorMessage(),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    configurationVerified = false;
                    updateTestButtonState();
                    removeAvailableProvider(testConfig.credentialId);
                    JOptionPane.showMessageDialog(ui.getMainPanel(),
                                                  AICommonBundle.message("settings.test.connection.error", e.getMessage()),
                                                  AICommonBundle.message("settings.test.result.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    testButton.setText(AICommonBundle.message("settings.test.connection"));
                    testButton.setEnabled(true);
                });
            }
        }).start();
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
        refreshConfig.modelName = Objects.toString(ui.getModelComboBox().getEditor().getItem(), "").trim();
        refreshConfig.baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText());
        refreshConfig.updateCredentialId(getCurrentApiKey());
        AIModelParameters modelSnapshot = snapshotModelParameters();
        AIRuntimeSettings runtimeSnapshot = snapshotRuntimeSettings();
        applyParametersToConfig(refreshConfig, modelSnapshot, runtimeSnapshot);

        AIServiceProvider provider;
        try {
            provider = AIServiceFactory.createProvider(refreshConfig);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(ui.getMainPanel(),
                                          AICommonBundle.message("settings.error.provider.create.failed.details"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        String baseUrl = normalizeBaseUrl(ui.getBaseUrlField().getText());
        if (baseUrl.isEmpty()) {
            JOptionPane.showMessageDialog(ui.getMainPanel(),
                                          AICommonBundle.message("settings.error.base.url.missing"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        String apiKey = getCurrentApiKey();
        if (providerType.requiresApiKey() && apiKey.trim().isEmpty()) {
            JOptionPane.showMessageDialog(ui.getMainPanel(),
                                          AICommonBundle.message("settings.error.api.key.missing"),
                                          AICommonBundle.message("settings.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        JButton refreshButton = ui.getRefreshModelsButton();
        refreshButton.setEnabled(false);
        refreshButton.setText(AICommonBundle.message("settings.refresh.models.testing"));
        refreshButton.setIcon(ui.createStatusDotIcon(Gray._158));

        new Thread(() -> {
            try {
                String currentModelName = getSelectedModelName();
                List<String> models = provider.getAvailableModels(apiKey);
                models.sort(String::compareToIgnoreCase);
                SwingUtilities.invokeLater(() -> {
                    ComboBox<String> modelComboBox = ui.getModelComboBox();
                    modelComboBox.removeAllItems();
                    if (!models.isEmpty()) {
                        models.forEach(modelComboBox::addItem);
                        String defaultModelName = refreshConfig.modelName;
                        if (defaultModelName != null && !defaultModelName.trim().isEmpty() && models.contains(defaultModelName)) {
                            modelComboBox.setSelectedItem(defaultModelName);
                        } else if (!currentModelName.trim().isEmpty() && models.contains(currentModelName)) {
                            modelComboBox.setSelectedItem(currentModelName);
                        } else {
                            modelComboBox.setSelectedItem(models.get(0));
                        }
                        refreshModelsSuccess = true;
                        JOptionPane.showMessageDialog(ui.getMainPanel(),
                                                      AICommonBundle.message("settings.refresh.models.success", models.size()),
                                                      AICommonBundle.message("settings.test.result.title"),
                                                      JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        refreshModelsSuccess = false;
                        String errorMessage = AICommonBundle.message("settings.refresh.models.empty");
                        if (providerType.requiresApiKey() && apiKey.trim().isEmpty()) {
                            errorMessage = AICommonBundle.message("settings.error.api.key.missing");
                        }
                        JOptionPane.showMessageDialog(ui.getMainPanel(),
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
                    JOptionPane.showMessageDialog(ui.getMainPanel(),
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
        }).start();
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
            String finalApiKey = apiKey;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                credentialManager.setApiKey(credentialId, finalApiKey);
            }, ApplicationManager.getApplication()::executeOnPooledThread);

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
        String provider = config.providerType != null ? config.providerType.getDisplayName() : AICommonBundle.message("settings.available" +
                                                                                                                      ".providers.unknown");
        String model = config.modelName != null ? config.modelName : "";
        int result = JOptionPane.showConfirmDialog(ui.getMainPanel(),
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
        int result = JOptionPane.showConfirmDialog(ui.getMainPanel(),
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
        }
    }

    /**
     * 更新测试按钮的状态图标, 根据配置验证结果显示不同的状态指示灯
     * <p>
     * 该方法通过获取测试连接按钮, 并根据配置验证是否通过的状态, 设置相应的状态指示图标.
     */
    public void updateTestButtonState() {
        JButton testButton = ui.getTestConnectionButton();
        if (configurationVerified != null && configurationVerified) {
            testButton.setIcon(ui.createStatusDotIcon(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))));
        } else {
            testButton.setIcon(ui.createStatusDotIcon(new JBColor(new Color(244, 67, 54), new Color(244, 67, 54))));
        }
    }

    /**
     * 更新刷新按钮的状态图标, 根据刷新模型操作的成功状态设置不同的颜色图标.
     * <p>
     * 该方法通过获取刷新按钮组件, 并根据刷新模型操作是否成功, 设置对应状态的图标.
     */
    public void updateRefreshButtonState() {
        JButton refreshButton = ui.getRefreshModelsButton();
        if (refreshModelsSuccess == null) {
            refreshButton.setIcon(ui.createStatusDotIcon(new JBColor(new Color(255, 193, 7), new Color(255, 193, 7))));
        } else if (refreshModelsSuccess) {
            refreshButton.setIcon(ui.createStatusDotIcon(new JBColor(new Color(76, 175, 80), new Color(76, 175, 80))));
        } else {
            refreshButton.setIcon(ui.createStatusDotIcon(new JBColor(new Color(244, 67, 54), new Color(244, 67, 54))));
        }
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
     * 该方法用于从用户界面的各个旋钮控件中读取当前设置的 AI 模型参数, 并创建一个 AIModelParameters 对象进行保存.
     * 参数包括温度, 最大令牌数,Top P,Top K 和存在惩罚值等.
     *
     * @return 包含当前 UI 设置的 AI 模型参数对象
     */
    @NotNull
    private AIModelParameters snapshotModelParameters() {
        AIModelParameters params = new AIModelParameters();
        params.temperature = ((Number) ui.getTemperatureSpinner().getValue()).doubleValue();
        double maxTokensInK = ((Number) ui.getMaxTokensSpinner().getValue()).doubleValue();
        params.maxTokens = (int) Math.max(100, Math.round(maxTokensInK * 1000));
        params.topP = ((Number) ui.getTopPSpinner().getValue()).doubleValue();
        params.topK = ((Number) ui.getTopKSpinner().getValue()).intValue();
        params.presencePenalty = ((Number) ui.getPresencePenaltySpinner().getValue()).doubleValue();
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
        snapshot.maxRetries = ((Number) ui.getMaxRetriesSpinner().getValue()).intValue();
        snapshot.timeout = ((Number) ui.getTimeoutSpinner().getValue()).intValue();
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
        String displayName = (String) ui.getProviderComboBox().getSelectedItem();
        AIProviderType type = displayName != null ? AIProviderType.fromDisplayName(displayName) : null;
        return type != null ? type : AIProviderType.QIANWEN;
    }

    /**
     * 根据显示名称解析对应的 AI 服务提供商类型
     * <p>
     * 若传入的显示名称不为空, 则根据名称获取对应的 AI 服务提供商类型; 若为空, 则默认返回通义千问类型.
     *
     * @param displayName 显示名称
     * @return 对应的 AI 服务提供商类型, 若无法解析则返回默认类型 AIProviderType.QIANWEN
     */
    private AIProviderType resolveSelectedProviderType(String displayName) {
        AIProviderType type = displayName != null ? AIProviderType.fromDisplayName(displayName) : null;
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
        Object editorItem = modelComboBox.getEditor().getItem();
        if (editorItem != null) {
            return editorItem.toString().trim();
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

        String message = baseMessage + "\n\n" +
                         "=== 当前高级配置 ===\n\n" +

                         // 运行时设置
                         "【运行时设置】\n" +
                         String.format("  最大重试次数: %d\n", runtime.maxRetries) +
                         String.format("  请求超时: %d 秒\n", runtime.timeout) +
                         "\n" +

                         // 模型参数
                         "【模型参数】\n" +
                         String.format("  温度 (Temperature): %.2f\n", modelParams.temperature) +
                         String.format("  最大 Token 数: %d\n", modelParams.maxTokens) +
                         String.format("  Top P: %.2f\n", modelParams.topP) +
                         String.format("  Top K: %d\n", modelParams.topK) +
                         String.format("  存在惩罚 (Presence Penalty): %.2f\n", modelParams.presencePenalty) +
                         "\n" +

                         // 说明文字
                         "💡 提示：测试连接之前先修改高级参数以适配不同场景需求\n";

        return message;
    }
}

