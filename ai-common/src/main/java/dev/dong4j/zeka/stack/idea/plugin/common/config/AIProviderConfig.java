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
    /** AI 服务提供商类型, 默认为通义千问 */
    public AIProviderType providerType = AIProviderType.QIANWEN;
    /** 模型名称, 表示当前使用的默认模型 */
    public String modelName = AIProviderType.QIANWEN.getDefaultModel();
    /**
     * 默认的基础 URL, 根据 AIProviderType.QIANWEN 获取
     */
    public String baseUrl = AIProviderType.QIANWEN.getDefaultBaseUrl();
    /** 配置是否已验证的标志位 */
    public boolean configurationVerified;
    /** 最后一次验证时间 */
    public long lastVerifiedTime;
    /** 备注信息 */
    public String remark;
    /** 身份凭证标识符 */
    public String credentialId;

    /**
     * 构造函数, 用于初始化 AIProviderConfig 对象.
     * <p>
     * 该方法不执行任何操作, 仅用于创建 AIProviderConfig 的实例.
     */
    public AIProviderConfig() {
    }

    /**
     * 构造函数, 用于初始化 AIProviderConfig 对象
     * <p>
     * 根据指定的 AI 服务提供商类型, 设置默认的模型名称和基础 URL
     *
     * @param providerType AI 服务提供商类型, 不能为空
     */
    public AIProviderConfig(@NotNull AIProviderType providerType) {
        this.providerType = providerType;
        this.modelName = providerType.getDefaultModel();
        this.baseUrl = providerType.getDefaultBaseUrl();
    }

    /**
     * 创建并返回当前 AIProviderConfig 对象的一个副本
     * <p>
     * 该方法用于复制当前配置对象的所有属性值到一个新的 AIProviderConfig 实例中,
     * 并返回该实例.
     *
     * @return 当前配置对象的副本
     */
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

    /**
     * 根据提供的参数生成并更新凭证 ID
     * <p>
     * 使用指定的提供商 ID, 模型名称,API 密钥和基础 URL 生成唯一的凭证 ID, 并将其赋值给当前对象的 credentialId 字段.
     *
     * @param apiKey 可选的 API 密钥, 用于生成唯一凭证 ID
     * @throws NullPointerException 如果 providerType 或 modelName 为 null 时可能抛出异常
     */
    public void updateCredentialId(@Nullable String apiKey) {
        this.credentialId = ProviderConfigUtils.generateUniqueId(
            providerType != null ? providerType.getProviderId() : null,
            modelName,
            apiKey,
            baseUrl
                                                                );
    }

    /**
     * 判断当前对象与指定对象是否相等
     * <p>
     * 先检查引用是否相同, 若相同返回 {@code true}; 随后判断传入对象是否为 {@code null} 或类型不匹配, 若是则返回 {@code false}; 最后通过比较 {@link #credentialId} 字段来确定两对象是否相等.
     *
     * @param o 需要比较的对象
     * @return 如果 {@code o} 与当前对象在类型和 {@code credentialId} 上相等, 则返回 {@code true}, 否则返回 {@code false}
     */
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

    /**
     * 重写 hashCode 方法, 根据 credentialId 生成对象的哈希码
     * <p>
     * 使用 credentialId 的哈希码作为该对象的哈希值
     *
     * @return 对象的哈希码值
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(credentialId);
    }

    /**
     * 判断当前配置对象是否与另一个配置对象内容相等
     * <p>
     * 比较 providerType,modelName,baseUrl,configurationVerified,credentialId 和 remark 字段是否与指定对象相同
     *
     * @param other 要比较的另一个配置对象
     * @return 如果所有字段都相等, 则返回 true; 否则返回 false
     */
    public boolean contentEquals(@NotNull AIProviderConfig other) {
        return providerType == other.providerType
               && Objects.equals(modelName, other.modelName)
               && Objects.equals(baseUrl, other.baseUrl)
               && configurationVerified == other.configurationVerified
               && Objects.equals(credentialId, other.credentialId)
               && Objects.equals(remark, other.remark);
    }
}
