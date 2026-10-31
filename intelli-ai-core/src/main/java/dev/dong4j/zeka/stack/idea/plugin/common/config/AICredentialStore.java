package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 凭证存储 SPI 接口
 * <p>
 * 抽象 API Key 等敏感凭证的存取能力, 供 {@link AICredentialManager} 委托调用.
 * <p>
 * 设计约束: core 模块不依赖任何具体凭证设施, 由宿主负责提供实现并注册:
 * <ul>
 *   <li>IntelliJ 插件宿主: 基于 PasswordSafe(系统钥匙串/凭据管理器)实现</li>
 *   <li>独立 MCP Server 宿主: 基于配置文件或环境变量实现</li>
 * </ul>
 * 宿主必须在首次凭证读写前调用 {@link AICredentialManager#setCredentialStore(AICredentialStore)}
 * 完成注册, 否则凭证操作将抛出 {@link IllegalStateException}.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public interface AICredentialStore {

    /**
     * 读取指定服务与键对应的密码
     *
     * @param serviceName 服务名称, 用于在系统凭证设施中隔离不同插件/应用的凭证
     * @param key         凭证键(已含前缀), 不能为空
     * @return 密码明文, 不存在时返回 null
     */
    @Nullable
    String getPassword(@NotNull String serviceName, @NotNull String key);

    /**
     * 写入或删除指定服务与键对应的密码
     *
     * @param serviceName 服务名称, 不能为空
     * @param key         凭证键(已含前缀), 不能为空
     * @param password    密码明文, 传 null 表示删除该凭证
     */
    void setPassword(@NotNull String serviceName, @NotNull String key, @Nullable String password);
}
