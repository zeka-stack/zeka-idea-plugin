package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingEffort;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingIntent;
import dev.dong4j.zeka.stack.idea.plugin.common.util.ProviderConfigUtils;

/**
 * AI 提供者配置类
 * <p>
 * 用于管理 AI 服务提供者的配置信息, 包括提供者类型, 模型名称, 基础 URL, 认证信息,
 * 模型参数和运行时设置等. 该类支持配置的复制, 验证和比较功能, 确保 AI 服务配置的
 * 一致性和有效性.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIProviderConfig {
    /** AI 服务提供商类型, 默认为通义千问 */
    public AIProviderType providerType = AIProviderType.OPENAI;
    /** 模型名称, 表示当前使用的默认模型 */
    public String modelName = AIProviderType.OPENAI.getDefaultModel();
    /**
     * 默认的基础 URL, 根据 AIProviderType.CUSTOM 获取
     */
    public String baseUrl = AIProviderType.OPENAI.getDefaultBaseUrl();
    /** 配置是否已验证的标志位 */
    public boolean configurationVerified;
    /** 最后一次验证时间 */
    public long lastVerifiedTime;
    /** 备注信息 */
    public String remark;
    /** 身份凭证标识符 */
    public String credentialId;
    /** 模型参数配置 */
    public AIModelParameters modelParameters = new AIModelParameters();
    /** 运行时配置 */
    public AIRuntimeSettings runtimeSettings = new AIRuntimeSettings();
    /**
     * 是否启用 Think / 思考模式（用户意图）
     * <p>
     * 实际写入哪些 HTTP 字段由 {@link dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking.ThinkingParamStrategy} 决定.
     * 默认 false, 兼容已有持久化配置 (缺失字段反序列化为 false).
     */
    public boolean enableThinking;

    /**
     * 思考强度（用户意图）
     * <p>
     * 取值见 {@link ThinkingEffort}；老配置缺失时按 {@link ThinkingEffort#AUTO} 处理.
     * 仅部分策略（如 DeepSeek）会写入对应请求字段.
     */
    public String thinkingEffort = ThinkingEffort.AUTO.name();

    /**
     * 测试连接时思考能力探测 / 合成结论; 可为 null (未探测过的老配置)
     */
    public ThinkingProbeResult thinkingProbeResult;

    /**
     * 判断当前配置是否「希望」启用 Think (用户勾选或模型名约定)
     * <p>
     * 最终请求字段由 ThinkingParamStrategy 决定.
     *
     * @return 用户侧希望启用思考时返回 true
     */
    public boolean shouldEnableThinking() {
        if (enableThinking) {
            return true;
        }
        ThinkingCapability capability = thinkingProbeResult != null ? thinkingProbeResult.capability : null;
        // 已有探测结果时不再用模型名猜测, 避免误伤 omni 等名称
        if (capability != null && capability != ThinkingCapability.UNKNOWN) {
            return false;
        }
        return modelName != null && modelName.toLowerCase(Locale.ROOT).contains("think");
    }

    /**
     * 解析用户选择的思考强度；非法值回退 {@link ThinkingEffort#AUTO}
     *
     * @return 思考强度
     */
    @NotNull
    public ThinkingEffort resolveThinkingEffort() {
        return ThinkingEffort.fromConfig(thinkingEffort);
    }

    /**
     * 将配置转换为与厂商字段无关的思考意图
     *
     * @return 思考意图
     */
    @NotNull
    public ThinkingIntent toThinkingIntent() {
        return new ThinkingIntent(shouldEnableThinking(), resolveThinkingEffort());
    }

    /**
     * 根据探测结果与用户勾选, 决定请求体如何发送 enable_thinking
     * <p>
     * 保留供兼容调用; 新逻辑请走 ThinkingParamStrategy.
     * OPTIONAL 关闭时默认 {@link ThinkingSendMode#OMIT} (不写 false), 以提升兼容性.
     *
     * @return 发送策略
     * @deprecated 使用 ThinkingParamStrategy.apply
     */
    @Deprecated
    @NotNull
    public ThinkingSendMode resolveThinkingSendMode() {
        ThinkingCapability capability = thinkingProbeResult != null ? thinkingProbeResult.capability : null;
        if (capability == ThinkingCapability.UNSUPPORTED) {
            return ThinkingSendMode.OMIT;
        }
        if (capability == ThinkingCapability.REQUIRED_TRUE) {
            return ThinkingSendMode.TRUE;
        }
        if (capability == ThinkingCapability.DEFAULT_ON_NO_PARAM) {
            return shouldEnableThinking() ? ThinkingSendMode.TRUE : ThinkingSendMode.OMIT;
        }
        return shouldEnableThinking() ? ThinkingSendMode.TRUE : ThinkingSendMode.OMIT;
    }

    /**
     * 根据探测结论调整默认 Think 勾选
     * <p>
     * REQUIRED_TRUE → 强制勾选; UNSUPPORTED → 强制取消; 其余保持现状.
     */
    public void applyThinkingProbeDefaults() {
        if (thinkingProbeResult == null || thinkingProbeResult.capability == null) {
            return;
        }
        switch (thinkingProbeResult.capability) {
            case REQUIRED_TRUE -> enableThinking = true;
            case UNSUPPORTED -> enableThinking = false;
            default -> {
                // OPTIONAL / DEFAULT_ON_NO_PARAM / UNKNOWN: 保留用户或模板勾选
            }
        }
    }

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
        config.modelParameters = this.modelParameters != null ? this.modelParameters.copy() : new AIModelParameters();
        config.runtimeSettings = this.runtimeSettings != null ? this.runtimeSettings.copy() : new AIRuntimeSettings();
        config.enableThinking = this.enableThinking;
        config.thinkingEffort = this.thinkingEffort != null ? this.thinkingEffort : ThinkingEffort.AUTO.name();
        config.thinkingProbeResult = this.thinkingProbeResult != null ? this.thinkingProbeResult.copy() : null;
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
               && Objects.equals(remark, other.remark)
               && enableThinking == other.enableThinking
               && Objects.equals(resolveThinkingEffort(), other.resolveThinkingEffort())
               && compareThinkingProbe(other)
               && compareModelParameters(other)
               && compareRuntimeSettings(other);
    }

    private boolean compareThinkingProbe(@NotNull AIProviderConfig other) {
        if (thinkingProbeResult == null) {
            return other.thinkingProbeResult == null;
        }
        return thinkingProbeResult.contentEquals(other.thinkingProbeResult);
    }

    /**
     * 比较当前对象与指定对象的模型参数是否相等
     * <p>
     * 该方法用于比较两个 {@link AIModelParameters} 对象的温度, 最大令牌数,Top-P,Top-K 和存在惩罚等参数值.
     * 若参数为 null, 则默认使用 "auto" 进行比较. 比较结果为所有参数值完全相等时返回 true, 否则返回 false.
     *
     * @param other 要比较的另一个配置对象, 不能为空
     * @return 如果两个对象的模型参数在所有关键字段上相等, 则返回 true, 否则返回 false
     */
    private boolean compareModelParameters(@NotNull AIProviderConfig other) {
        AIModelParameters left = modelParameters != null ? modelParameters : new AIModelParameters();
        AIModelParameters right = other.modelParameters != null ? other.modelParameters : new AIModelParameters();
        String temp1 = left.temperature != null ? left.temperature : "auto";
        String temp2 = right.temperature != null ? right.temperature : "auto";
        String maxTokens1 = left.maxTokens != null ? left.maxTokens : "auto";
        String maxTokens2 = right.maxTokens != null ? right.maxTokens : "auto";
        String topP1 = left.topP != null ? left.topP : "auto";
        String topP2 = right.topP != null ? right.topP : "auto";
        String topK1 = left.topK != null ? left.topK : "auto";
        String topK2 = right.topK != null ? right.topK : "auto";
        String presencePenalty1 = left.presencePenalty != null ? left.presencePenalty : "auto";
        String presencePenalty2 = right.presencePenalty != null ? right.presencePenalty : "auto";
        return temp1.equals(temp2)
               && maxTokens1.equals(maxTokens2)
               && topP1.equals(topP2)
               && topK1.equals(topK2)
               && presencePenalty1.equals(presencePenalty2);
    }

    /**
     * 比较当前运行时设置与另一个配置对象的运行时设置是否相等
     * <p>
     * 该方法用于比较两个 {@link AIRuntimeSettings} 对象的 {@code maxRetries},{@code timeout} 和 {@code waitDuration} 字段是否完全一致.
     * 如果其中一个对象为 null, 则使用默认的空运行时设置进行比较.
     *
     * @param other 要比较的另一个配置对象
     * @return 如果两个运行时设置在所有字段上相等, 则返回 {@code true}, 否则返回 {@code false}
     */
    private boolean compareRuntimeSettings(@NotNull AIProviderConfig other) {
        AIRuntimeSettings left = runtimeSettings != null ? runtimeSettings : new AIRuntimeSettings();
        AIRuntimeSettings right = other.runtimeSettings != null ? other.runtimeSettings : new AIRuntimeSettings();
        return left.maxRetries == right.maxRetries
               && left.timeout == right.timeout
               && left.waitDuration == right.waitDuration;
    }
}
