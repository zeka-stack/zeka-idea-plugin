package dev.dong4j.zeka.stack.idea.mcpserver;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialStore;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * MCP Server 配置
 * <p>
 * 装配 core 执行链所需的 {@link AIProviderConfig}, 配置来源按优先级:
 * <ol>
 *   <li>环境变量: INTELLIAI_PROVIDER / INTELLIAI_BASE_URL / INTELLIAI_MODEL / INTELLIAI_API_KEY</li>
 *   <li>配置文件: ~/.intelli-ai/mcp-server.properties(同名小写下划线键)</li>
 *   <li>内置默认值(通义千问官方端点)</li>
 * </ol>
 * API Key 单独支持通过 {@link AICredentialManager} 的 SPI 注入, 本类同时注册
 * 基于配置文件/环境变量的凭证实现, 保证 core 执行链在无 IDE 环境下可取到密钥.
 * <p>
 * 配置文件示例:
 * <pre>
 * provider=qianwen
 * base_url=https://dashscope.aliyuncs.com/compatible-mode/v1
 * model=qwen-plus
 * api_key=sk-xxx
 * </pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class ServerConfig {

    /** 配置文件固定路径: 用户主目录下 */
    private static final Path CONFIG_PATH = Path.of(System.getProperty("user.home"),
                                                    ".intelli-ai", "mcp-server.properties");

    /** 组装完成的供应商配置, 供 core 执行链使用 */
    private final AIProviderConfig providerConfig;

    /** API Key 明文, 调用 provider 时直接传入 */
    private final String apiKey;

    /** 各供应商独立密钥(配置文件 api_key.<PROVIDER> 或环境变量 INTELLIAI_API_KEY_<PROVIDER>), 支持调用时切换供应商 */
    private final Map<AIProviderType, String> perProviderKeys;

    /**
     * 读取配置并装配供应商对象
     */
    private ServerConfig(@NotNull AIProviderConfig providerConfig,
                         @Nullable String apiKey,
                         @NotNull Map<AIProviderType, String> perProviderKeys) {
        this.providerConfig = providerConfig;
        this.apiKey = apiKey;
        this.perProviderKeys = perProviderKeys;
    }

    /**
     * 获取供应商配置
     *
     * @return 供应商配置对象
     */
    public @NotNull AIProviderConfig providerConfig() {
        return providerConfig;
    }

    /**
     * 获取 API Key
     *
     * @return API Key, 未配置时返回 null(调用会失败并返回明确的错误信息)
     */
    public @Nullable String apiKey() {
        return apiKey;
    }

    /**
     * 加载配置
     * <p> 环境变量优先于配置文件, 便于在 MCP 客户端配置中以 env 注入密钥而不落盘.
     *
     * @return 装配完成的配置对象
     */
    public static @NotNull ServerConfig load() {
        Properties props = loadConfigFile();
        Map<AIProviderType, String> perProviderKeys = collectPerProviderKeys(props);

        String provider = firstNonBlank(System.getenv("INTELLIAI_PROVIDER"),
                                        props.getProperty("provider"), "QIANWEN");
        String baseUrl = firstNonBlank(System.getenv("INTELLIAI_BASE_URL"),
                                       props.getProperty("base_url"), null);
        String model = firstNonBlank(System.getenv("INTELLIAI_MODEL"),
                                     props.getProperty("model"), null);
        String apiKey = firstNonBlank(System.getenv("INTELLIAI_API_KEY"),
                                      props.getProperty("api_key"), null);

        // provider 类型解析: AIProviderType 携带各服务商默认端点与模型, 未配置时自动补齐
        AIProviderType type = resolveType(provider);
        AIProviderConfig config = new AIProviderConfig();
        config.providerType = type;
        config.baseUrl = baseUrl != null ? baseUrl : type.getDefaultBaseUrl();
        config.modelName = model != null ? model : type.getDefaultModel();
        config.modelParameters = new AIModelParameters();
        config.runtimeSettings = new AIRuntimeSettings();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("未配置 API Key(环境变量 INTELLIAI_API_KEY 或配置文件 {}), AI 调用将失败", CONFIG_PATH);
        }
        log.info("MCP Server 配置完成: provider={}, model={}, baseUrl={}",
                 config.providerType, config.modelName, config.baseUrl);
        return new ServerConfig(config, apiKey, perProviderKeys);
    }

    /**
     * 解析调用级覆盖后的供应商配置
     * <p> 工具入参可携带 provider / model 覆盖启动配置, 实现单次调用切换服务商与模型:
     * <ul>
     *   <li>仅指定 model: 沿用启动供应商, 只换模型</li>
     *   <li>指定 provider: 切换供应商, 端点/默认模型取新供应商默认值,
     *       密钥优先取该供应商的独立密钥(api_key.&lt;PROVIDER&gt;), 无则回退全局密钥</li>
     * </ul>
     *
     * @param providerOverride 供应商覆盖值, 可为 null
     * @param modelOverride    模型覆盖值, 可为 null
     * @return 覆盖后的配置对象(新实例, 不影响启动配置)
     */
    public @NotNull ResolvedCall resolveConfig(@Nullable String providerOverride,
                                               @Nullable String modelOverride) {
        if ((providerOverride == null || providerOverride.isBlank())
            && (modelOverride == null || modelOverride.isBlank())) {
            return new ResolvedCall(providerConfig, apiKey);
        }
        AIProviderType type = providerOverride != null && !providerOverride.isBlank()
            ? resolveType(providerOverride)
            : providerConfig.providerType;
        AIProviderConfig config = new AIProviderConfig();
        config.providerType = type;
        config.baseUrl = providerOverride != null && !providerOverride.isBlank()
            ? type.getDefaultBaseUrl()
            : providerConfig.baseUrl;
        config.modelName = modelOverride != null && !modelOverride.isBlank()
            ? modelOverride
            : (providerOverride != null && !providerOverride.isBlank()
                ? type.getDefaultModel()
                : providerConfig.modelName);
        config.modelParameters = providerConfig.modelParameters;
        config.runtimeSettings = providerConfig.runtimeSettings;
        String key = perProviderKeys.getOrDefault(type, apiKey);
        return new ResolvedCall(config, key);
    }

    /**
     * 收集各供应商独立密钥
     * <p> 来源: 配置文件 {@code api_key.<PROVIDER>} 与环境变量
     * {@code INTELLIAI_API_KEY_<PROVIDER>}(环境变量优先).
     *
     * @param props 配置文件属性
     * @return 供应商 -> 密钥映射
     */
    private static @NotNull Map<AIProviderType, String> collectPerProviderKeys(@NotNull Properties props) {
        Map<AIProviderType, String> keys = new EnumMap<>(AIProviderType.class);
        for (AIProviderType type : AIProviderType.values()) {
            String env = System.getenv("INTELLIAI_API_KEY_" + type.name());
            if (env != null && !env.isBlank()) {
                keys.put(type, env.trim());
                continue;
            }
            String file = props.getProperty("api_key." + type.name());
            if (file != null && !file.isBlank()) {
                keys.put(type, file.trim());
            }
        }
        return keys;
    }

    /**
     * 读取用户目录配置文件
     *
     * @return 配置属性, 文件不存在时返回空属性
     */
    private static @NotNull Properties loadConfigFile() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream is = Files.newInputStream(CONFIG_PATH)) {
                props.load(is);
            } catch (IOException e) {
                log.warn("读取配置文件失败: {}", CONFIG_PATH, e);
            }
        }
        return props;
    }

    /**
     * 解析供应商类型
     * <p> 支持枚举名(QIANWEN/DEEPSEEK/ZHIPU/OLLAMA 等)与常见别名(qianwen/dashscope).
     *
     * @param provider 供应商标识
     * @return 供应商类型, 无法识别时回退 QIANWEN 并输出告警
     */
    private static @NotNull AIProviderType resolveType(@NotNull String provider) {
        String normalized = provider.trim().toUpperCase().replace("-", "_");
        // 常见别名映射
        switch (normalized) {
            case "DASHSCOPE", "TONGYI" -> normalized = "QIANWEN";
            case "GLM" -> normalized = "ZHIPU";
            case "DEEP_SEEK" -> normalized = "DEEPSEEK";
            default -> {
            }
        }
        try {
            return AIProviderType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            String supported = java.util.Arrays.stream(AIProviderType.values())
                .map(AIProviderType::name).collect(java.util.stream.Collectors.joining(", "));
            throw new IllegalArgumentException(
                "无法识别的 provider: " + provider + ", 支持的值: " + supported);
        }
    }

    /**
     * 返回第一个非空白的值
     *
     * @param values 候选值序列
     * @return 首个非空白值, 全部为空时返回 null
     */
    private static @Nullable String firstNonBlank(@Nullable String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /**
     * 注册 core 凭证 SPI 的配置文件/环境变量实现
     * <p> MCP Server 场景密钥直接来自配置, 凭证接口的读写按无操作处理(读取由
     * {@link ServerConfig#load()} 完成), 此处注册仅为满足 core 执行链的 SPI 前置检查.
     *
     * @param apiKey 当前生效的 API Key
     */
    public static void registerCredentialStore(@Nullable String apiKey) {
        AICredentialManager.setCredentialStore(new AICredentialStore() {
            @Override
            public @Nullable String getPassword(@NotNull String serviceName, @NotNull String key) {
                return apiKey;
            }

            @Override
            public void setPassword(@NotNull String serviceName, @NotNull String key, @Nullable String password) {
                log.warn("MCP Server 运行时不支持写入凭证, 请修改 {} 或环境变量", CONFIG_PATH);
            }
        });
    }

    /**
     * 单次调用的解析结果
     *
     * @param config 供应商配置
     * @param apiKey 本次调用使用的密钥
     */
    public record ResolvedCall(@NotNull AIProviderConfig config, @Nullable String apiKey) {
    }
}
