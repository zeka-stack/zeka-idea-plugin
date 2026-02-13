package dev.dong4j.zeka.stack.idea.plugin.common.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import lombok.Getter;

/**
 * AI 服务提供商类型枚举
 * <p>
 * 定义了系统支持的各种 AI 服务提供商类型, 包括自定义 API, 通义千问, 硅基流动,Ollama,
 * LM Studio,ModelScope,IFlow, 智谱 AI 等. 每个枚举值包含提供商的标识符, 显示名称,
 * 默认 API 基础 URL, 默认模型,API 密钥要求, 基础 URL 可编辑性以及支持的模型列表等信息.
 * 提供了根据提供商 ID 或显示名称查找枚举值的静态方法, 以及获取所有提供商 ID 和显示名称的工具方法.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public enum AIProviderType {
    /** Dong4j Cloud(OpenAI 兼容) 模型配置 */
    FREEAI(
        "free",
        "FreeAI",
        "https://zekastack.dong4j.site/freeai/v1",
        "studio/ollama/glm-4.7",
        true,
        false,
        List.of("studio/ollama/glm-4.7", "studio/lm/glm-4.7")
    ),
    /**
     * 自定义模型配置
     * <p>
     * 用于指定 OpenAI API 的自定义模型参数, 包括模型名称, 支持的模型列表等信息
     */
    OPENAI(
        "openai",
        "OpenAI",
        "https://api.openai.com/v1",
        "gpt-3.5-turbo",
        true,
        true,
        List.of("gpt-3.5-turbo", "gpt-4o-mini")
    ),
    /**
     * Claude（Anthropic）模型配置
     */
    ANTHROPIC(
        "anthropic",
        "Anthropic",
        "https://api.anthropic.com",
        "claude-3-5-sonnet-20241022",
        true,
        true,
        List.of(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
               )
    ),
    /**
     * NVIDIA (OpenAI 兼容) 模型配置
     */
    NVIDIA(
        "nvidia",
        "Nvidia",
        "https://integrate.api.nvidia.com/v1",
        "minimaxai/minimax-m2",
        true,
        true,
        List.of("minimaxai/minimax-m2")
    ),
    /**
     * HuggingFace (OpenAI 兼容) 模型配置
     */
    HUGGINGFACE(
        "huggingface",
        "HuggingFace",
        "https://router.huggingface.co/v1",
        "zai-org/GLM-4.7:novita",
        true,
        true,
        List.of("zai-org/GLM-4.7:novita")
    ),
    /**
     * OpenRouter (OpenAI 兼容) 模型配置
     */
    OPENROUTER(
        "openrouter",
        "OpenRouter",
        "https://openrouter.ai/api/v1",
        "z-ai/glm-4.5-air:free",
        true,
        true,
        List.of("z-ai/glm-4.5-air:free")
    ),
    /**
     * Cloudflare Workers AI (OpenAI 兼容) 模型配置
     */
    CLOUDFLARE(
        "cloudflare",
        "Cloudflare Workers AI",
        "https://api.cloudflare.com/client/v4/accounts/{YOUR_ACCOUNT_ID}/ai/v1",
        "@cf/meta/llama-3.1-8b-instruct",
        true,
        true,
        List.of("@cf/meta/llama-3.1-8b-instruct")
    ),
    /**
     * Amazon Bedrock (OpenAI 兼容) 模型配置
     */
    BEDROCK(
        "bedrock",
        "Amazon Bedrock",
        "https://bedrock-mantle.us-east-1.api.aws/v1",
        "openai.gpt-oss-120b",
        true,
        true,
        List.of("openai.gpt-oss-120b")
    ),
    /**
     * Azure OpenAI (OpenAI 兼容) 模型配置
     */
    AZURE(
        "azure_openai",
        "Azure OpenAI",
        "https://{YOUR_RESOURCE_NAME}.openai.azure.com/openai/deployments/{YOUR_DEPLOYMENT_NAME}",
        "gpt-4o-mini",
        true,
        true,
        List.of("gpt-4o-mini")
    ),
    /**
     * GitHub Models (OpenAI 兼容) 模型配置
     */
    GITHUB_MODELS(
        "github_models",
        "GitHub Models",
        "https://models.github.ai/inference",
        "openai/gpt-4.1",
        true,
        true,
        List.of("openai/gpt-4.1")
    ),
    /**
     * Mistral AI (OpenAI 兼容) 模型配置
     */
    MISTRAL(
        "mistral",
        "Mistral AI",
        "https://api.mistral.ai/v1",
        "mistral-small-latest",
        true,
        true,
        List.of("mistral-small-latest")
    ),
    /**
     * DeepSeek (OpenAI 兼容) 模型配置
     */
    DEEPSEEK(
        "deepseek",
        "DeepSeek",
        "https://api.deepseek.com/v1",
        "deepseek-chat",
        true,
        true,
        List.of("deepseek-chat", "deepseek-reasoner")
    ),
    /**
     * DeepSeek (Anthropic 兼容) 模型配置
     */
    DEEPSEEK_ANTHROPIC(
        "deepseek_anthropic",
        "DeepSeek",
        "https://api.deepseek.com/anthropic",
        "deepseek-chat",
        true,
        true,
        List.of("deepseek-chat", "deepseek-reasoner")
    ),
    /**
     * 豆包 (OpenAI 兼容) 模型配置
     */
    DOUBAO(
        "doubao",
        "豆包",
        "https://ark.cn-beijing.volces.com/api/coding/v3",
        "doubao-seed-1-8-251228",
        true,
        true,
        List.of("doubao-seed-1-8-251228", "doubao-seed-1-6-flash-250828")
    ),
    /**
     * 豆包 (Anthropic 兼容) 模型配置
     */
    DOUBAO_ANTHROPIC(
        "doubao_anthropic",
        "豆包",
        "https://ark.cn-beijing.volces.com/api/coding",
        "doubao-seed-1-8-251228",
        true,
        true,
        List.of("doubao-seed-1-8-251228", "doubao-seed-1-6-flash-250828")
    ),
    /**
     * Grok (OpenAI 兼容) 模型配置
     */
    GROK(
        "grok",
        "Grok",
        "https://api.x.ai/v1",
        "grok-3-latest",
        true,
        true,
        List.of(
            "grok-3-beta",
            "grok-3",
            "grok-3-latest",
            "grok-3-fast-beta",
            "grok-3-fast",
            "grok-3-fast-latest",
            "grok-3-mini-beta",
            "grok-3-mini",
            "grok-3-mini-latest"
               )
    ),
    /**
     * 混元 (OpenAI 兼容) 模型配置
     */
    HUNYUAN(
        "hunyuan",
        "混元",
        "https://api.hunyuan.cloud.tencent.com/v1",
        "hunyuan-2.0-instruct-20251111",
        true,
        true,
        List.of("hunyuan-2.0-instruct-20251111", "hunyuan-2.0-thinking-20251109")
    ),
    /**
     * 混元 (Anthropic 兼容) 模型配置
     */
    HUNYUAN_ANTHROPIC(
        "hunyuan_anthropic",
        "混元",
        "https://api.hunyuan.cloud.tencent.com/anthropic",
        "hunyuan-2.0-instruct-20251111",
        true,
        true,
        List.of("hunyuan-2.0-instruct-20251111", "hunyuan-2.0-thinking-20251109")
    ),
    /**
     * Moonshot (OpenAI 兼容) 模型配置
     */
    MOONSHOT(
        "moonshot",
        "Moonshot",
        "https://api.moonshot.cn/v1",
        "kimi-k2-thinking-turbo",
        true,
        true,
        List.of("kimi-k2-thinking-turbo", "kimi-k2-thinking")
    ),
    /**
     * Moonshot (Anthropic 兼容) 模型配置
     */
    MOONSHOT_ANTHROPIC(
        "moonshot_anthropic",
        "Moonshot",
        "https://api.moonshot.cn/anthropic",
        "kimi-k2-thinking-turbo",
        true,
        true,
        List.of("kimi-k2-thinking-turbo", "kimi-k2-thinking")
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
        true,
        Arrays.asList("qwen3-omni-flash-2025-12-01", "qwen3-32b", "qwen3-14b", "qwen3-8b")
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
        true,
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
        "ZhipuAI/GLM-4.7",
        true,
        true,
        List.of("ZhipuAI/GLM-4.7", "GLM-4.7-Flash","Qwen/Qwen3-235B-A22B-Thinking-2507", "deepseek-ai/DeepSeek-V3.2")
    ),
    /**
     * ModelScope Anthropic 兼容模型配置
     */
    MODELSCOPE_ANTHROPIC(
        "modelscope_anthropic",
        "ModelScope",
        "https://api-inference.modelscope.cn",
        "ZhipuAI/GLM-4.7",
        true,
        true,
        List.of("ZhipuAI/GLM-4.7", "GLM-4.7-Flash","Qwen/Qwen3-235B-A22B-Thinking-2507", "deepseek-ai/DeepSeek-V3.2")
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
    ),
    /**
     * 智谱AI 模型配置
     * <p>
     * 智谱AI (ChatGLM) 服务提供商, 支持多种 GLM 模型, 包括 glm-4.6, glm-4.5, glm-4.5-flash 等.
     */
    ZHIPU(
        "glm",
        "智谱AI",
        "https://open.bigmodel.cn/api/coding/paas/v4",
        "glm-4.7",
        true,
        true,
        List.of("glm-4.7", "glm-4.6", "glm-4.5")
    ),
    /**
     * Z.AI（智谱海外版）OpenAI 兼容模型配置
     */
    ZAI(
        "zai",
        "Z.AI",
        "https://api.z.ai/api/coding/paas/v4",
        "glm-4.7",
        true,
        true,
        List.of("glm-4.7", "glm-4.6", "glm-4.5")
    ),
    /**
     * Z.AI（智谱 Anthropic 兼容）模型配置
     */
    ZHIPU_ANTHROPIC(
        "glm_anthropic",
        "智谱AI",
        "https://open.bigmodel.cn/api/anthropic",
        "glm-4.7",
        true,
        true,
        List.of("glm-4.7", "glm-4.6", "glm-4.5")
    ),
    /**
     * Z.AI（智谱海外版）Anthropic 兼容模型配置
     */
    ZAI_ANTHROPIC(
        "zai_anthropic",
        "Z.AI",
        "https://api.z.ai/api/anthropic",
        "glm-4.7",
        true,
        true,
        List.of("glm-4.7", "glm-4.6", "glm-4.5")
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

    /**
     * 获取按分组分类的 AI 服务提供商列表
     * <p>
     * 根据预定义的分组规则, 将所有 AI 服务提供商类型按类别组织成映射结构. 当前支持三类分组:
     * <ul>
     *   <li>OpenAI 系列提供商 (包括 OpenAI, 通义千问, 硅基流动,IFlow,LM Studio,ModelScope, 智谱 AI,Z.AI)</li>
     *   <li>Anthropic 系列提供商 (包括 Anthropic,ModelScope(Anthropic), 智谱 AI(Anthropic),Z.AI(Anthropic))</li>
     *   <li> 其他提供商 (包括 Gemini,Codex,Ollama)</li>
     * </ul>
     * 该方法主要用于 UI 界面或配置页面中对服务提供商进行分组展示.
     *
     * @return 包含分组名称与对应提供商列表的映射表, 键为分组名称 (如 "OpenAI"), 值为该分组内所有 {@link AIProviderType} 枚举值的列表
     */
    @NotNull
    public static Map<String, List<AIProviderType>> getGroupedProviders() {
        Map<String, List<AIProviderType>> groupedProviders = new LinkedHashMap<>();
        groupedProviders.put(AICommonBundle.message("settings.provider.group.openai"), List.of(
            AIProviderType.FREEAI,
            AIProviderType.OPENAI,
            AIProviderType.NVIDIA,
            AIProviderType.HUGGINGFACE,
            AIProviderType.OPENROUTER,
            AIProviderType.CLOUDFLARE,
            AIProviderType.BEDROCK,
            AIProviderType.AZURE,
            AIProviderType.GITHUB_MODELS,
            AIProviderType.MISTRAL,
            AIProviderType.DEEPSEEK,
            AIProviderType.DOUBAO,
            AIProviderType.GROK,
            AIProviderType.HUNYUAN,
            AIProviderType.MOONSHOT,
            AIProviderType.QIANWEN,
            AIProviderType.SILICONFLOW,
            AIProviderType.IFLOW,
            AIProviderType.LM_STUDIO,
            AIProviderType.MODELSCOPE,
            AIProviderType.ZHIPU,
            AIProviderType.ZAI
                                                                                              ));
        groupedProviders.put(AICommonBundle.message("settings.provider.group.anthropic"), List.of(
            AIProviderType.ANTHROPIC,
            AIProviderType.DEEPSEEK_ANTHROPIC,
            AIProviderType.DOUBAO_ANTHROPIC,
            AIProviderType.HUNYUAN_ANTHROPIC,
            AIProviderType.MOONSHOT_ANTHROPIC,
            AIProviderType.MODELSCOPE_ANTHROPIC,
            AIProviderType.ZHIPU_ANTHROPIC,
            AIProviderType.ZAI_ANTHROPIC
                                                                                                 ));
        groupedProviders.put(AICommonBundle.message("settings.provider.group.other"), List.of(
            AIProviderType.OLLAMA
                                                                                             ));
        return groupedProviders;
    }

    /**
     * 获取 API Key 的获取地址
     * <p>
     * 返回当前提供商获取 API Key 的 URL 地址。如果不需要 API Key 或没有专门的获取页面，返回 null。
     *
     * @return API Key 获取地址，如果不需要或不存在则返回 null
     */
    @SuppressWarnings("DuplicatedCode")
    public String getApiKeyUrl() {
        return switch (this) {
            case FREEAI -> "https://zekastack.dong4j.site/#/settings";
            case OPENAI -> "https://platform.openai.com/api-keys";
            case NVIDIA -> "https://docs.api.nvidia.com/nim/reference/llm-apis";
            case HUGGINGFACE -> "https://huggingface.co/docs/inference-providers/index";
            case OPENROUTER -> "https://openrouter.ai/settings/keys";
            case CLOUDFLARE -> "https://developers.cloudflare.com/workers-ai/get-started/rest-api/";
            case BEDROCK -> "https://docs.aws.amazon.com/bedrock/latest/userguide/api-keys-generate.html";
            case AZURE -> "https://portal.azure.com/";
            case GITHUB_MODELS -> "https://github.com/settings/tokens";
            case MISTRAL -> "https://console.mistral.ai/";
            case ANTHROPIC -> "https://console.anthropic.com/settings/keys";
            case DEEPSEEK, DEEPSEEK_ANTHROPIC -> "https://platform.deepseek.com/api_keys";
            case DOUBAO, DOUBAO_ANTHROPIC -> "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey";
            case GROK -> "https://console.x.ai/team/default/api-keys";
            case HUNYUAN, HUNYUAN_ANTHROPIC -> "https://console.cloud.tencent.com/hunyuan/start";
            case MOONSHOT, MOONSHOT_ANTHROPIC -> "https://platform.moonshot.cn/console/api-keys";
            case QIANWEN -> "https://dashscope.console.aliyun.com/apiKey";
            case SILICONFLOW -> "https://cloud.siliconflow.cn/settings/api-keys";
            case OLLAMA -> "https://ollama.com/cloud";
            case LM_STUDIO -> "https://lmstudio.ai/";
            case MODELSCOPE, MODELSCOPE_ANTHROPIC -> "https://modelscope.cn/usercenter/personal/settings/api-token";
            case IFLOW -> "https://console.iflow.cn/api-key";
            case ZHIPU -> "https://docs.bigmodel.cn/cn/guide/develop/openai/introduction";
            case ZHIPU_ANTHROPIC -> "https://docs.bigmodel.cn/cn/guide/develop/claude/introduction";
            case ZAI -> "https://docs.z.ai/api-reference/introduction";
            case ZAI_ANTHROPIC -> "https://docs.z.ai/scenario-example/develop-tools/claude";
        };
    }
}
