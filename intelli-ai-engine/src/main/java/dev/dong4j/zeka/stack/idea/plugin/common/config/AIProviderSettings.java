package dev.dong4j.zeka.stack.idea.plugin.common.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * AI 服务提供商配置类（应用级服务）
 * <p>
 * 用于存储和管理 AI 服务提供商的相关配置信息, 包括可用提供商, 模型参数, 运行时参数等.
 * 作为应用级服务，所有插件共享同一份全局配置。
 * <p>
 * 注意：全局配置不维护"默认供应商"，每个插件维护自己的默认供应商选择。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@State(
    name = "AIProviderSettings",
    storages = @Storage("zeka.stack.intelliai.engine.xml")
)
public class AIProviderSettings implements PersistentStateComponent<AIProviderSettings> {
    /** 最后选中的服务商类型（用于恢复 UI 状态） */
    public AIProviderType aiProviderType = AIProviderType.QIANWEN;
    /** 默认支持的 AI 服务提供商及其配置信息 */
    public final Map<AIProviderType, AIProviderConfig> defaultProviders = new EnumMap<>(AIProviderType.class);
    /** 可用的 AI 服务提供商配置列表 */
    public final List<AIProviderConfig> availableProviders = new ArrayList<>();

    /**
     * 模型参数配置
     * <p>
     * 用于设置和管理 AI 模型的相关参数
     */
    public AIModelParameters modelParameters = new AIModelParameters();
    /** AI 运行时设置 */
    public AIRuntimeSettings runtimeSettings = new AIRuntimeSettings();

    /** 是否显示高级设置 */
    public boolean showAdvancedSettings = false;
    /** 是否显示可用的服务提供商 */
    public boolean showAvailableProviders = false;

    /** 监听器列表（使用 CopyOnWriteArrayList 保证线程安全） */
    private final List<AIProviderSettingsListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 获取服务实例（单例）
     *
     * @return AIProviderSettings 实例
     */
    public static AIProviderSettings getInstance() {
        return ApplicationManager.getApplication().getService(AIProviderSettings.class);
    }

    /**
     * 添加监听器
     * <p>
     * 当可用提供商列表发生变化时，会通知所有已注册的监听器。
     *
     * @param listener 监听器实例
     */
    public void addListener(@NotNull AIProviderSettingsListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除监听器
     *
     * @param listener 要移除的监听器实例
     */
    public void removeListener(@NotNull AIProviderSettingsListener listener) {
        listeners.remove(listener);
    }

    /**
     * 通知所有监听器可用提供商列表已发生变化
     * <p>
     * 在 EDT 线程中执行监听器回调，确保 UI 更新的线程安全。
     * 同时触发配置的立即持久化，确保第三方插件能够立即看到变更。
     */
    private void notifyListeners() {
        // 立即持久化配置，确保第三方插件能够立即看到变更
        // saveState();

        if (listeners.isEmpty()) {
            return;
        }
        // 在 EDT 线程中执行监听器回调
        ApplicationManager.getApplication().invokeLater(() -> {
            for (AIProviderSettingsListener listener : listeners) {
                try {
                    listener.onAvailableProvidersChanged(this);
                } catch (Exception e) {
                    // 静默处理异常，避免影响其他监听器
                }
            }
        });
    }

    /**
     * 手动触发配置持久化
     * <p>
     * 在可用提供商列表发生变更时，立即持久化配置，确保第三方插件能够立即看到变更。
     * 这样就不需要用户手动点击 Apply 按钮。
     */
    private void saveState() {
        ApplicationManager.getApplication().saveSettings();
    }

    /**
     * 获取状态（用于持久化）
     *
     * @return 当前状态
     */
    @Override
    @NotNull
    public AIProviderSettings getState() {
        return this;
    }

    /**
     * 加载状态（用于持久化）
     *
     * @param state 要加载的状态
     */
    @Override
    public void loadState(@NotNull AIProviderSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 创建并返回当前对象的一个副本
     * <p>
     * 该方法深拷贝当前 AIProviderSettings 对象的所有属性, 包括默认提供者, 可用提供者, 模型参数和运行时设置等.
     *
     * @return 当前对象的一个新副本
     */
    public AIProviderSettings copy() {
        AIProviderSettings settings = new AIProviderSettings();
        this.defaultProviders.forEach((type, config) -> settings.defaultProviders.put(type, config.copy()));
        this.availableProviders.forEach(config -> settings.availableProviders.add(config.copy()));
        settings.modelParameters.temperature = this.modelParameters.temperature;
        settings.modelParameters.maxTokens = this.modelParameters.maxTokens;
        settings.modelParameters.topP = this.modelParameters.topP;
        settings.modelParameters.topK = this.modelParameters.topK;
        settings.modelParameters.presencePenalty = this.modelParameters.presencePenalty;

        settings.runtimeSettings.maxRetries = this.runtimeSettings.maxRetries;
        settings.runtimeSettings.timeout = this.runtimeSettings.timeout;
        settings.runtimeSettings.waitDuration = this.runtimeSettings.waitDuration;
        settings.runtimeSettings.verboseLogging = this.runtimeSettings.verboseLogging;

        settings.showAdvancedSettings = this.showAdvancedSettings;
        settings.showAvailableProviders = this.showAvailableProviders;
        settings.aiProviderType = this.aiProviderType;
        return settings;
    }

    /**
     * 获取指定类型的默认 AI 提供者配置
     * <p>
     * 根据提供的 AI 提供者类型, 从缓存中获取或创建对应的默认配置, 并返回其副本.
     *
     * @param type AI 提供者类型
     * @return 指定类型的默认 AI 提供者配置副本
     */
    @NotNull
    public AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType type) {
        return defaultProviders.computeIfAbsent(type, this::createDefaultProviderConfig).copy();
    }

    private AIProviderConfig createDefaultProviderConfig(@NotNull AIProviderType type) {
        AIProviderConfig config = new AIProviderConfig(type);
        config.modelParameters = modelParameters != null ? modelParameters.copy() : new AIModelParameters();
        config.runtimeSettings = runtimeSettings != null ? runtimeSettings.copy() : new AIRuntimeSettings();
        return config;
    }

    /**
     * 更新默认的 AI 服务提供商配置
     * <p>
     * 将指定类型的 AI 服务提供商配置更新为新的配置, 并保存一份副本到默认配置映射中.
     *
     * @param type   AI 服务提供商类型
     * @param config AI 服务提供商配置对象
     */
    public void updateDefaultProviderConfig(@NotNull AIProviderType type, @NotNull AIProviderConfig config) {
        defaultProviders.put(type, config.copy());
    }

    /**
     * 获取已验证的 AI 提供商配置列表
     * <p>
     * 该方法从 {@code availableProviders} 中筛选出 {@code configurationVerified} 为 {@code true} 的配置,
     * 对每个符合条件的配置执行 {@link AIProviderConfig#copy()} 以生成副本,
     * 并将所有副本收集到一个新的 {@link List} 中返回.
     *
     * @return 已验证配置的副本列表, 列表元素均为 {@link AIProviderConfig} 的拷贝
     */
    @NotNull
    public List<AIProviderConfig> getVerifiedProviders() {
        return availableProviders.stream()
            .filter(config -> config.configurationVerified)
            .map(AIProviderConfig::copy)
            .toList();
    }

    /**
     * 添加一个可用的 AI 服务提供商配置
     * <p>
     * 根据传入的 AI 服务提供商配置, 移除已存在的相同凭证 ID 的配置, 并添加新的配置到可用列表中.
     *
     * @param config 要添加的 AI 服务提供商配置对象, 不能为空
     */
    public void addAvailableProvider(@NotNull AIProviderConfig config) {
        availableProviders.removeIf(existing -> Objects.equals(existing.credentialId, config.credentialId));
        availableProviders.add(config.copy());
        notifyListeners();
    }

    /**
     * 移除指定凭证 ID 的可用提供者配置
     * <p>
     * 从可用提供者列表中移除与给定凭证 ID 匹配的配置项
     *
     * @param credentialId 准则 ID, 可以为 null
     */
    public void removeAvailableProvider(@Nullable String credentialId) {
        boolean removed = availableProviders.removeIf(config -> Objects.equals(config.credentialId, credentialId));
        if (removed) {
            notifyListeners();
        }
    }

    /**
     * 清除所有可用提供者
     * <p>
     * 该方法用于清空 availableProviders 集合中的所有元素
     */
    public void clearAvailableProviders() {
        if (!availableProviders.isEmpty()) {
            availableProviders.clear();
            notifyListeners();
        }
    }

    /**
     * 将给定的 {@link AIProviderSettings} 对象的配置复制到当前实例.
     * <p>
     * 该方法会逐一复制源对象的所有可配置字段, 包括:
     * <ul>
     *   <li> 默认提供者列表 {@code defaultProviders}</li>
     *   <li> 可用提供者列表 {@code availableProviders}</li>
     *   <li> 模型参数 {@code modelParameters}</li>
     *   <li> 运行时设置 {@code runtimeSettings}</li>
     *   <li> 性能模式, 统计显示与高级设置等布尔标志 </li>
     * </ul>
     * 复制过程中会使用 {@link AIProviderSettings#copy()} 方法生成深拷贝, 确保源对象与目标对象互不影响.
     * <p>
     * 注意：不复制 providerType，因为全局配置不维护默认供应商。
     *
     * @param source 源配置对象, 不能为空
     */
    public void applyFrom(@NotNull AIProviderSettings source) {
        // 检查可用提供商列表是否有变化
        boolean availableProvidersChanged = !this.availableProviders.equals(source.availableProviders);
        
        this.defaultProviders.clear();
        source.defaultProviders.forEach((type, config) -> this.defaultProviders.put(type, config.copy()));

        this.availableProviders.clear();
        source.availableProviders.forEach(config -> this.availableProviders.add(config.copy()));

        AIModelParameters sourceModel = source.modelParameters;
        this.modelParameters.temperature = sourceModel.temperature;
        this.modelParameters.maxTokens = sourceModel.maxTokens;
        this.modelParameters.topP = sourceModel.topP;
        this.modelParameters.topK = sourceModel.topK;
        this.modelParameters.presencePenalty = sourceModel.presencePenalty;

        AIRuntimeSettings sourceRuntime = source.runtimeSettings;
        this.runtimeSettings.maxRetries = sourceRuntime.maxRetries;
        this.runtimeSettings.timeout = sourceRuntime.timeout;
        this.runtimeSettings.waitDuration = sourceRuntime.waitDuration;
        this.runtimeSettings.verboseLogging = sourceRuntime.verboseLogging;

        this.showAdvancedSettings = source.showAdvancedSettings;
        this.showAvailableProviders = source.showAvailableProviders;
        this.aiProviderType = source.aiProviderType;

        // 如果可用提供商列表有变化，通知监听器并触发持久化
        if (availableProvidersChanged) {
            notifyListeners();
        }
    }

    /**
     * 检查当前对象与指定对象的设置内容是否相等
     * <p>
     * 该方法用于比较当前对象与另一个 AIProviderSettings 对象的各个配置属性是否完全相同.
     * 如果所有属性都相等, 则返回 true; 否则返回 false.
     *
     * @param other 要比较的 AIProviderSettings 对象
     * @return 如果当前对象与指定对象的设置内容相等, 返回 true; 否则返回 false
     */
    @SuppressWarnings( {"BooleanMethodIsAlwaysInverted", "D"})
    public boolean contentEquals(@NotNull AIProviderSettings other) {
        // 注意：不比较 providerType，因为全局配置不维护默认供应商
        if (showAdvancedSettings != other.showAdvancedSettings
            || showAvailableProviders != other.showAvailableProviders) {
            return false;
        }

        if (Double.compare(modelParameters.temperature, other.modelParameters.temperature) != 0) {
            return false;
        }
        if (modelParameters.maxTokens != other.modelParameters.maxTokens) {
            return false;
        }
        if (Double.compare(modelParameters.topP, other.modelParameters.topP) != 0) {
            return false;
        }
        if (modelParameters.topK != other.modelParameters.topK) {
            return false;
        }
        if (Double.compare(modelParameters.presencePenalty, other.modelParameters.presencePenalty) != 0) {
            return false;
        }

        if (runtimeSettings.maxRetries != other.runtimeSettings.maxRetries) {
            return false;
        }

        if (runtimeSettings.timeout != other.runtimeSettings.timeout) {
            return false;
        }
        if (runtimeSettings.waitDuration != other.runtimeSettings.waitDuration) {
            return false;
        }
        if (runtimeSettings.verboseLogging != other.runtimeSettings.verboseLogging) {
            return false;
        }

        if (defaultProviders.size() != other.defaultProviders.size()) {
            return false;
        }

        if(aiProviderType != other.aiProviderType){
            return false;
        }

        for (Map.Entry<AIProviderType, AIProviderConfig> entry : defaultProviders.entrySet()) {
            AIProviderConfig otherConfig = other.defaultProviders.get(entry.getKey());
            if (otherConfig == null || !entry.getValue().contentEquals(otherConfig)) {
                return false;
            }
        }

        if (availableProviders.size() != other.availableProviders.size()) {
            return false;
        }
        for (int i = 0; i < availableProviders.size(); i++) {
            if (!availableProviders.get(i).contentEquals(other.availableProviders.get(i))) {
                return false;
            }
        }

        return true;
    }
}
