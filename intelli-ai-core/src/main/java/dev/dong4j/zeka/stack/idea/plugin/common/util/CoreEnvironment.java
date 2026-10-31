package dev.dong4j.zeka.stack.idea.plugin.common.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * core 环境信息持有器
 * <p>
 * 承载原本来自 IDE 插件环境(EngineContents/PluginUtil)的基础信息, 如插件 ID 与版本号,
 * 主要用于 HTTP User-Agent 等标识场景. core 不依赖具体宿主, 由宿主启动时注册真实取值:
 * <ul>
 *   <li>IntelliJ 插件宿主: 注册为 {@code PluginUtil.getVersion(EngineContents.PLUGIN_ID)}</li>
 *   <li>独立 MCP Server 宿主: 注册为自身版本号, 或使用默认值</li>
 * </ul>
 * 未注册时版本回退为 {@code unknown}, 不影响功能, 仅影响 User-Agent 的可读性.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class CoreEnvironment {

    /** core 所属插件的 ID, 与 EngineContents.PLUGIN_ID 保持一致 */
    public static final String PLUGIN_ID = "dev.dong4j.zeka.stack.idea.plugin.common.ai";

    /** 版本提供器, 宿主启动时注册; 默认返回 unknown */
    private static volatile Supplier<String> versionProvider = () -> "unknown";

    /**
     * 私有构造函数, 防止实例化工具类
     */
    private CoreEnvironment() {
    }

    /**
     * 注册宿主的版本提供器
     *
     * @param provider 版本提供器, 不能为空
     */
    public static void setVersionProvider(@NotNull Supplier<String> provider) {
        versionProvider = Objects.requireNonNull(provider);
    }

    /**
     * 获取当前宿主版本号
     *
     * @return 版本号字符串, 未注册时为 "unknown"
     */
    public static @NotNull String version() {
        return versionProvider.get();
    }
}
