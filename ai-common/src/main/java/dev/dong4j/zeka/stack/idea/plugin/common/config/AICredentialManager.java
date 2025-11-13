package dev.dong4j.zeka.stack.idea.plugin.common.config;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * AI API Key 凭证管理器。
 */
public final class AICredentialManager {

    private final String serviceName;
    private final String keyPrefix;

    public AICredentialManager(@NotNull String serviceName, @NotNull String keyPrefix) {
        this.serviceName = serviceName;
        this.keyPrefix = keyPrefix;
    }

    @Nullable
    public String getApiKey(@Nullable String credentialId) {
        if (credentialId == null || credentialId.isBlank()) {
            return null;
        }
        CredentialAttributes attributes = buildAttributes(credentialId);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    public void setApiKey(@NotNull String credentialId, @NotNull String apiKey) {
        CredentialAttributes attributes = buildAttributes(credentialId);
        PasswordSafe.getInstance().set(attributes, new Credentials(credentialId, apiKey));
    }

    public void deleteApiKey(@Nullable String credentialId) {
        if (credentialId == null || credentialId.isBlank()) {
            return;
        }
        CredentialAttributes attributes = buildAttributes(credentialId);
        PasswordSafe.getInstance().set(attributes, null);
    }

    public void loadApiKeyAsync(@Nullable String credentialId, @NotNull Consumer<String> callback) {
        if (credentialId == null || credentialId.isBlank()) {
            ApplicationManager.getApplication().invokeLater(() -> callback.accept(null), ModalityState.any());
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            String apiKey = getApiKey(credentialId);
            ApplicationManager.getApplication().invokeLater(() -> callback.accept(apiKey), ModalityState.any());
        });
    }

    @NotNull
    private CredentialAttributes buildAttributes(@NotNull String credentialId) {
        String key = keyPrefix + Objects.requireNonNull(credentialId);
        return new CredentialAttributes(serviceName, key);
    }
}
