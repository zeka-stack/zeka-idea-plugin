package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

final class AutocompleteProviderResolver {
    @NotNull
    AIProviderConfig resolvePrimary() {
        AIProviderSettings providerSettings = AIProviderSettings.getInstance();
        AIProviderConfig byCredential = resolveByCredential(providerSettings);
        if (byCredential != null) {
            return byCredential;
        }
        AutocompleteSettings settings = AutocompleteSettings.getInstance();
        AIProviderType type = resolveProviderType(settings.providerId, providerSettings);
        return resolveConfig(type, providerSettings);
    }

    @Nullable
    AIProviderConfig resolveFallback() {
        AutocompleteSettings settings = AutocompleteSettings.getInstance();
        if (settings.fallbackProviderId == null || settings.fallbackProviderId.isBlank()) {
            return null;
        }
        AIProviderSettings providerSettings = AIProviderSettings.getInstance();
        AIProviderType type = resolveProviderType(settings.fallbackProviderId, providerSettings);
        return resolveConfig(type, providerSettings);
    }

    private AIProviderType resolveProviderType(@Nullable String providerId, @NotNull AIProviderSettings settings) {
        if (providerId != null && !providerId.isBlank()) {
            AIProviderType type = AIProviderType.fromProviderId(providerId);
            if (type != null) {
                return type;
            }
        }
        return settings.aiProviderType != null ? settings.aiProviderType : AIProviderType.QIANWEN;
    }

    private AIProviderConfig resolveConfig(@NotNull AIProviderType type, @NotNull AIProviderSettings settings) {
        List<AIProviderConfig> verified = settings.getVerifiedProviders();
        for (AIProviderConfig config : verified) {
            if (type.equals(config.providerType)) {
                return config;
            }
        }
        return settings.getDefaultProviderConfig(type);
    }

    private @Nullable AIProviderConfig resolveByCredential(@NotNull AIProviderSettings settings) {
        String credentialId = settings.autocompleteProviderCredentialId;
        if (credentialId == null || credentialId.isBlank()) {
            return null;
        }
        List<AIProviderConfig> verified = settings.getVerifiedProviders();
        for (AIProviderConfig config : verified) {
            if (credentialId.equals(config.credentialId)) {
                return config;
            }
        }
        return null;
    }
}
