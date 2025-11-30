package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务商管理器
 * <p>
 * 管理所有服务商的状态、线程池和执行器。当服务商出现 429 错误时，
 * 会标记服务商为不可用状态并销毁其所有线程。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
public class ProviderManager {
    /** 服务商状态映射 */
    @Getter
    private final Map<String, ProviderStatus> providerStatuses;

    /** 服务商执行器映射 */
    @Getter
    private final Map<String, ExecutorService> providerExecutors;

    /**
     * 构造函数
     */
    public ProviderManager() {
        this.providerStatuses = new ConcurrentHashMap<>();
        this.providerExecutors = new ConcurrentHashMap<>();
    }

    /**
     * 注册服务商
     *
     * @param provider 服务商配置
     * @param executor 服务商执行器
     */
    public void registerProvider(@NotNull AIProviderConfig provider, @NotNull ExecutorService executor) {
        String providerId = getProviderId(provider);
        providerStatuses.put(providerId, ProviderStatus.AVAILABLE);
        providerExecutors.put(providerId, executor);
        log.debug("注册服务商: {}", providerId);
    }

    /**
     * 检查服务商是否可用
     *
     * @param provider 服务商配置
     * @return 如果服务商可用返回 true
     */
    public boolean isProviderAvailable(@NotNull AIProviderConfig provider) {
        String providerId = getProviderId(provider);
        ProviderStatus status = providerStatuses.get(providerId);
        return status == ProviderStatus.AVAILABLE;
    }

    /**
     * 标记服务商为限流状态（429 错误）
     * <p>
     * 当服务商出现 429 错误时调用，会销毁该服务商的所有线程。
     *
     * @param provider 服务商配置
     */
    public void markProviderRateLimited(@NotNull AIProviderConfig provider) {
        String providerId = getProviderId(provider);
        log.warn("服务商 {} 出现限流错误（429），标记为不可用", providerId);

        providerStatuses.put(providerId, ProviderStatus.RATE_LIMITED);

        // 销毁服务商的所有线程
        ExecutorService executor = providerExecutors.get(providerId);
        if (executor != null) {
            executor.shutdownNow();
            log.info("已销毁服务商 {} 的所有线程", providerId);
        }
    }

    /**
     * 标记服务商为错误状态
     *
     * @param provider 服务商配置
     */
    public void markProviderError(@NotNull AIProviderConfig provider) {
        String providerId = getProviderId(provider);
        providerStatuses.put(providerId, ProviderStatus.ERROR);
        log.warn("服务商 {} 出现错误", providerId);
    }

    /**
     * 获取服务商执行器
     *
     * @param provider 服务商配置
     * @return 执行器，如果不存在返回 null
     */
    @Nullable
    public ExecutorService getExecutor(@NotNull AIProviderConfig provider) {
        String providerId = getProviderId(provider);
        return providerExecutors.get(providerId);
    }

    /**
     * 关闭所有服务商执行器
     */
    public void shutdownAll() {
        providerExecutors.values().forEach(executor -> {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        });
        providerExecutors.clear();
        providerStatuses.clear();
        log.info("已关闭所有服务商执行器");
    }

    /**
     * 获取服务商 ID
     *
     * @param provider 服务商配置
     * @return 服务商 ID
     */
    @NotNull
    private String getProviderId(@NotNull AIProviderConfig provider) {
        return provider.providerType.getProviderId();
    }
}

