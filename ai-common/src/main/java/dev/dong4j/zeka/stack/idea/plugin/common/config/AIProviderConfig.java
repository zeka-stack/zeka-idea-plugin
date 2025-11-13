package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.util.ProviderConfigUtils;

/**
 * AI 提供商配置。
 */
public class AIProviderConfig {
    public AIProviderType providerType = AIProviderType.QIANWEN;
    public String modelName = AIProviderType.QIANWEN.getDefaultModel();
    public String baseUrl = AIProviderType.QIANWEN.getDefaultBaseUrl();
    public boolean configurationVerified;
    public long lastVerifiedTime;
    public String remark;
    public String credentialId;

    public AIProviderConfig() {
    }

    public AIProviderConfig(@NotNull AIProviderType providerType) {
        this.providerType = providerType;
        this.modelName = providerType.getDefaultModel();
        this.baseUrl = providerType.getDefaultBaseUrl();
    }

    public AIProviderConfig copy() {
        AIProviderConfig config = new AIProviderConfig();
        config.providerType = this.providerType;
        config.modelName = this.modelName;
        config.baseUrl = this.baseUrl;
        config.configurationVerified = this.configurationVerified;
        config.lastVerifiedTime = this.lastVerifiedTime;
        config.remark = this.remark;
        config.credentialId = this.credentialId;
        return config;
    }

    public void updateCredentialId(@Nullable String apiKey) {
        this.credentialId = ProviderConfigUtils.generateUniqueId(
            providerType != null ? providerType.getProviderId() : null,
            modelName,
            apiKey,
            baseUrl
                                                                );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AIProviderConfig that = (AIProviderConfig) o;
        return Objects.equals(credentialId, that.credentialId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(credentialId);
    }

    public boolean contentEquals(@NotNull AIProviderConfig other) {
        return providerType == other.providerType
               && Objects.equals(modelName, other.modelName)
               && Objects.equals(baseUrl, other.baseUrl)
               && configurationVerified == other.configurationVerified
               && Objects.equals(credentialId, other.credentialId)
               && Objects.equals(remark, other.remark);
    }
}
