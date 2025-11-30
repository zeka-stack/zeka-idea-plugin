package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

/**
 * AIProviderType 枚举
 * <p>
 * 定义了系统支持的 AI 服务提供商类型, 并为每个提供商提供了默认的基础 URL, 模型, 是否需要 API Key, 是否可编辑基础 URL 以及支持的模型列表等配置信息.
 * 该枚举可用于统一管理不同 AI 提供商的配置, 方便在调用时根据 providerId 或 displayName 进行快速查找和配置.
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.11.14
 * @since 1.0.0
 */
public enum AIProviderType {
    /**
     * 自定义模型配置
     * <p>
     * 用于指定 OpenAI API 的自定义模型参数, 包括模型名称, 支持的模型列表等信息
     */
    CUSTOM(
        "custom",
        "OpenAI API",
        "https://api.openai.com/v1",
        "gpt-3.5-turbo",
        true,
        true,
        List.of("gpt-3.5-turbo", "gpt-4o-mini")
    ),
    /**
     * 通义千问模型的标识信息
     * <p>
     * 包含模型名称, 别名, 图标地址, 默认模型版本, 是否启用, 是否为默认模型以及支持的模型版本列表
     */
    QIANWEN(
        "qianwen",
        "通义千问",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen3-8b",
        true,
        false,
        Arrays.asList("qwen3-32b", "qwen3-14b", "qwen3-8b", "qwen3-4b")
    ),
    /**
     * 硅基流动模型配置
     * <p>
     * 包含模型标识符, 中文名称, 模型地址, 是否默认模型, 是否启用, 支持的模型列表等信息
     */
    SILICONFLOW(
        "siliconflow",
        "硅基流动",
        "https://api.siliconflow.cn/v1",
        "Qwen/Qwen3-8B",
        true,
        false,
        List.of("Qwen/Qwen3-8B", "Qwen/Qwen2.5-14B-Instruct", "THUDM/glm-4-9b-chat")
    ),
    /**
     * Ollama 模型的配置信息
     * <p>
     * 包含模型名称, 描述, 基础 URL, 默认模型, 是否启用, 是否为本地模型以及支持的模型列表
     */
    OLLAMA(
        "ollama",
        "Ollama",
        "http://localhost:11434/v1",
        "gpt-oss:20b-cloud",
        false,
        true,
        Arrays.asList("gpt-oss:120b-cloud", "deepseek-r1:14b", "qwen3-8b")
    ),
    /**
     * LM Studio 模型配置
     * <p>
     * 用于标识 LM Studio 模型的相关信息, 包括模型名称, 显示名称, 基础 URL, 默认模型名称, 是否启用, 是否需要认证以及支持的模型列表.
     */
    LM_STUDIO(
        "lmstudio",
        "LM Studio",
        "http://localhost:1234/v1",
        "gpt-3.5-turbo",
        false,
        true,
        List.of("qwen3-8b")
    ),
    /**
     * ModelScope 模型配置
     * <p>
     * 使用 ModelScope Dolphin 接口, 默认不需要 API Key, 支持通过刷新获取最新模型列表.
     */
    MODELSCOPE(
        "modelscope",
        "ModelScope",
        "https://api-inference.modelscope.cn/v1",
        "ZhipuAI/GLM-4.6",
        true,
        true,
        List.of("Qwen/Qwen3-Coder-480B-A35B-Instruc", "Qwen/Qwen3-235B-A22B-Thinking-2507", "ZhipuAI/GLM-4.6")
    ),
    /**
     * IFlow 模型配置
     * <p>
     * IFlow AI 服务提供商, 支持多种模型, 包括 kimi-k2-0905, qwen3-coder-plus, glm-4.6, deepseek-r1 等.
     */
    IFLOW(
        "iflow",
        "IFlow",
        "https://apis.iflow.cn/v1",
        "kimi-k2-0905",
        true,
        true,
        List.of("kimi-k2-0905", "qwen3-coder-plus", "glm-4.6", "deepseek-r1")
    );

    /** 服务提供方唯一标识符 */
    private final String providerId;
    /**
     * 显示名称
     * <p>
     * 用于表示该对象的可读名称, 通常用于展示给用户
     */
    private final String displayName;
    /** 默认基础 URL */
    private final String defaultBaseUrl;
    /**
     * 默认模型名称或标识
     * <p>
     * 用于指定系统在未明确指定模型时使用的默认模型
     */
    private final String defaultModel;
    /** 是否需要 API 密钥 */
    private final boolean requiresApiKey;
    /**
     * 基础 URL 是否可编辑
     * -- GETTER --
     * 判断基础 URL 是否可编辑
     * <p>
     * 当返回
     * 时表示基础 URL 可以被修改; 返回
     * 表示不可修改.
     */
    @Getter
    private final boolean baseUrlEditable;
    /**
     * 支持的模型列表
     * <p>
     * 该字段存储当前系统或组件所支持的模型名称或标识符
     */
    private final List<String> supportedModels;

    /**
     * 构造一个 AI 服务提供商类型对象
     * <p>
     * 初始化 AI 服务提供商的配置信息, 包括提供商 ID, 显示名称, 默认基础 URL, 默认模型, 是否需要 API 密钥, 基础 URL 是否可编辑以及支持的模型列表
     *
     * @param providerId      提供商唯一标识符
     * @param displayName     提供商显示名称
     * @param defaultBaseUrl  默认基础 URL
     * @param defaultModel    默认模型
     * @param requiresApiKey  是否需要 API 密钥
     * @param baseUrlEditable 基础 URL 是否可编辑
     * @param supportedModels 支持的模型列表
     */
    AIProviderType(@NotNull String providerId,
                   @NotNull String displayName,
                   @NotNull String defaultBaseUrl,
                   @NotNull String defaultModel,
                   boolean requiresApiKey,
                   boolean baseUrlEditable,
                   @NotNull List<String> supportedModels) {
        this.providerId = providerId;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
        this.requiresApiKey = requiresApiKey;
        this.baseUrlEditable = baseUrlEditable;
        this.supportedModels = supportedModels;
    }

    /**
     * 获取提供者 ID
     * <p>
     * 返回当前对象的提供者标识字符串
     *
     * @return 提供者 ID
     */
    @NotNull
    public String getProviderId() {
        return providerId;
    }

    /**
     * 获取显示名称
     * <p>
     * 返回当前对象的显示名称属性值.
     *
     * @return 显示名称
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取默认的基础 URL
     * <p>
     * 返回系统配置中的默认基础 URL 值
     *
     * @return 默认的基础 URL
     */
    @NotNull
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    /**
     * 获取默认模型
     * <p>
     * 返回当前设置的默认模型名称
     *
     * @return 默认模型名称
     */
    @NotNull
    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * 判断是否需要 API 密钥
     * <p>
     * 返回一个布尔值, 表示当前操作是否需要 API 密钥进行验证.
     *
     * @return 如果需要 API 密钥则返回 true, 否则返回 false
     */
    public boolean requiresApiKey() {
        return requiresApiKey;
    }

    /**
     * 获取当前支持的模型列表
     * <p>
     * 返回系统当前支持的所有模型名称列表
     *
     * @return 支持的模型名称列表
     */
    @NotNull
    public List<String> getSupportedModels() {
        return supportedModels;
    }

    /**
     * 根据提供商 ID 查找对应的 AI 提供商类型
     * <p>
     * 遍历所有 AI 提供商类型, 查找与给定提供商 ID 匹配的类型.
     *
     * @param providerId 提供商 ID, 不能为空
     * @return 匹配的 AI 提供商类型, 若未找到则返回 null
     * @throws NullPointerException 如果 providerId 为 null 时调用此方法
     */
    @Nullable
    public static AIProviderType fromProviderId(@NotNull String providerId) {
        for (AIProviderType type : values()) {
            if (type.providerId.equals(providerId)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据显示名称获取对应的 AI 服务提供商类型
     * <p>
     * 遍历所有 AI 服务提供商类型, 查找与指定显示名称匹配的类型, 若找到则返回该类型, 否则返回 null.
     *
     * @param displayName 显示名称
     * @return 对应的 AI 服务提供商类型, 若未找到则返回 null
     */
    @Nullable
    public static AIProviderType fromDisplayName(@NotNull String displayName) {
        for (AIProviderType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据显示名称获取对应的提供者 ID.
     * <p>
     * 该方法首先调用 {@link AIProviderType#fromDisplayName(String)} 将传入的显示名称转换为 {@link AIProviderType} 枚举,
     * 然后返回该枚举的 {@code providerId}. 如果没有匹配的枚举, 方法将返回 {@code null}.
     *
     * @param displayName 显示名称, 不能为空
     * @return 对应的提供者 ID; 若不存在匹配的提供者, 则返回 {@code null}
     */
    @Nullable
    public static String getProviderIdByDisplayName(@NotNull String displayName) {
        AIProviderType type = fromDisplayName(displayName);
        return type != null ? type.providerId : null;
    }

    /**
     * 根据提供者 ID 获取显示名称
     * <p>
     * 通过提供者 ID 查找对应的 AI 提供者类型, 并返回其显示名称. 如果类型不存在, 则返回 null.
     *
     * @param providerId 提供者 ID
     * @return AI 提供者的显示名称, 若类型不存在则返回 null
     */
    @Nullable
    public static String getDisplayNameByProviderId(@NotNull String providerId) {
        AIProviderType type = fromProviderId(providerId);
        return type != null ? type.displayName : null;
    }

    /**
     * 获取所有提供商的 ID 列表
     * <p>
     * 遍历所有枚举值, 并提取每个提供商的 ID, 返回包含所有提供商 ID 的列表.
     *
     * @return 包含所有提供商 ID 的列表
     */
    @NotNull
    public static List<String> getAllProviderIds() {
        return Arrays.stream(values()).map(AIProviderType::getProviderId).toList();
    }

    /**
     * 获取所有 AI 提供者的显示名称列表
     * <p>
     * 遍历所有 AI 提供者枚举值, 获取每个枚举实例的显示名称, 并返回名称列表
     *
     * @return 所有 AI 提供者的显示名称列表
     */
    @NotNull
    public static List<String> getAllDisplayNames() {
        return Arrays.stream(values()).map(AIProviderType::getDisplayName).toList();
    }
}
