package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * AI 提供商设置集合。
 */
public class AIProviderSettings {

    public AIProviderType providerType = AIProviderType.QIANWEN;
    public final Map<AIProviderType, AIProviderConfig> defaultProviders = new EnumMap<>(AIProviderType.class);
    public final List<AIProviderConfig> availableProviders = new ArrayList<>();

    public final AIModelParameters modelParameters = new AIModelParameters();
    public final AIRuntimeSettings runtimeSettings = new AIRuntimeSettings();

    public boolean performanceMode = false;
    public boolean showProviderStatistics = false;
    public boolean showAdvancedSettings = false;
    public boolean showAvailableProviders = false;

    public AIProviderSettings copy() {
        AIProviderSettings settings = new AIProviderSettings();
        settings.providerType = this.providerType;
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

        settings.performanceMode = this.performanceMode;
        settings.showProviderStatistics = this.showProviderStatistics;
        settings.showAdvancedSettings = this.showAdvancedSettings;
        settings.showAvailableProviders = this.showAvailableProviders;
        return settings;
    }

    @NotNull
    public AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType type) {
        return defaultProviders.computeIfAbsent(type, AIProviderConfig::new).copy();
    }

    public void updateDefaultProviderConfig(@NotNull AIProviderType type, @NotNull AIProviderConfig config) {
        defaultProviders.put(type, config.copy());
    }

    @NotNull
    public List<AIProviderConfig> getVerifiedProviders() {
        return availableProviders.stream()
            .filter(config -> config.configurationVerified)
            .map(AIProviderConfig::copy)
            .toList();
    }

    public void addAvailableProvider(@NotNull AIProviderConfig config) {
        availableProviders.removeIf(existing -> Objects.equals(existing.credentialId, config.credentialId));
        availableProviders.add(config.copy());
    }

    public void removeAvailableProvider(@Nullable String credentialId) {
        availableProviders.removeIf(config -> Objects.equals(config.credentialId, credentialId));
    }

    public void clearAvailableProviders() {
        availableProviders.clear();
    }

    public void applyFrom(@NotNull AIProviderSettings source) {
        this.providerType = source.providerType;

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

        this.performanceMode = source.performanceMode;
        this.showProviderStatistics = source.showProviderStatistics;
        this.showAdvancedSettings = source.showAdvancedSettings;
        this.showAvailableProviders = source.showAvailableProviders;
    }

    public boolean contentEquals(@NotNull AIProviderSettings other) {
        if (providerType != other.providerType) {
            return false;
        }
        if (performanceMode != other.performanceMode
            || showProviderStatistics != other.showProviderStatistics
            || showAdvancedSettings != other.showAdvancedSettings
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
