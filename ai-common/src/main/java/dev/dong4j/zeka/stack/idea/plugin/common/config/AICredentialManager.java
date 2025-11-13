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

    /**
     * 服务名称
     * <p>
     * 用于标识当前服务的唯一名称, 通常在服务注册或日志记录中使用
     */
    private final String serviceName;
    /** 用于构建缓存键的前缀, 确保缓存键的唯一性和分类 */
    private final String keyPrefix;

    /**
     * 初始化 AICredentialManager 实例
     * <p>
     * 使用指定的服务名称和密钥前缀创建凭证管理器
     *
     * @param serviceName 服务名称, 用于标识不同的 AI 服务
     * @param keyPrefix   密钥前缀, 用于构建存储凭证的键
     */
    public AICredentialManager(@NotNull String serviceName, @NotNull String keyPrefix) {
        this.serviceName = serviceName;
        this.keyPrefix = keyPrefix;
    }

    /**
     * 根据凭证 ID 获取 API 密钥
     * <p>
     * 通过传入的凭证 ID 查找对应的凭证属性, 再根据属性获取对应的凭证信息, 最后返回 API 密钥字符串.
     *
     * @param credentialId 凭证 ID
     * @return API 密钥字符串, 若凭证 ID 为空或无效则返回 null
     */
    @Nullable
    public String getApiKey(@Nullable String credentialId) {
        if (credentialId == null || credentialId.isBlank()) {
            return null;
        }
        CredentialAttributes attributes = buildAttributes(credentialId);
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    /**
     * 设置 API 密钥信息
     * <p>
     * 根据提供的凭证 ID 和 API 密钥, 构建凭证属性并设置到密码安全系统中
     *
     * @param credentialId 凭证 ID, 不能为空
     * @param apiKey       API 密钥, 不能为空
     */
    public void setApiKey(@NotNull String credentialId, @NotNull String apiKey) {
        CredentialAttributes attributes = buildAttributes(credentialId);
        PasswordSafe.getInstance().set(attributes, new Credentials(credentialId, apiKey));
    }

    /**
     * 删除指定凭证的 API 密钥
     * <p>
     * 如果 {@code credentialId} 为 {@code null} 或空字符串, 则不执行任何操作. 否则构造凭证属性并将其对应的密码设置为 {@code null}, 从而从 {@link PasswordSafe} 中删除该凭证.
     *
     * @param credentialId 要删除的凭证 ID, 可能为 {@code null}
     */
    public void deleteApiKey(@Nullable String credentialId) {
        if (credentialId == null || credentialId.isBlank()) {
            return;
        }
        CredentialAttributes attributes = buildAttributes(credentialId);
        PasswordSafe.getInstance().set(attributes, null);
    }

    /**
     * 异步加载指定凭证 ID 对应的 API 密钥
     * <p>
     * 如果凭证 ID 为 null 或空字符串, 则立即通过回调返回 null. 否则, 在后台线程中获取 API 密钥, 并在 UI 线程中通过回调返回结果.
     *
     * @param credentialId 凭证 ID, 可能为 null
     * @param callback     回调函数, 用于接收 API 密钥结果
     * @throws NullPointerException 如果 callback 为 null 时调用, 但该方法参数未声明抛出异常, 因此不标注
     * @since 1.0
     */
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

    /**
     * 构建凭证属性对象
     * <p>
     * 根据提供的凭证 ID 生成对应的属性对象, 用于存储或操作凭证信息.
     *
     * @param credentialId 凭证 ID
     * @return 包含凭证服务名称和键的 CredentialAttributes 对象
     */
    @NotNull
    private CredentialAttributes buildAttributes(@NotNull String credentialId) {
        String key = keyPrefix + Objects.requireNonNull(credentialId);
        return new CredentialAttributes(serviceName, key);
    }
}
