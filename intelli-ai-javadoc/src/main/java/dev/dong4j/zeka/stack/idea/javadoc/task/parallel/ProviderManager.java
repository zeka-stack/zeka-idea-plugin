package dev.dong4j.zeka.stack.idea.javadoc.task.parallel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
    /**
     * 服务商状态映射
     * <p>
     * 存储所有服务商的当前状态, 用于管理服务商的可用性.
     */
    @Getter
    private final Map<String, ProviderStatus> providerStatuses;

    /**
     * 服务商执行器映射
     * <p>
     * 存储每个服务商对应的线程池执行器, 用于管理服务商的并发请求.
     */
    @Getter
    private final Map<String, ExecutorService> providerExecutors;

    /** AI 请求执行器 (隔离于工作线程池, 避免使用 commonPool) */
    private final ExecutorService requestExecutor;

    /**
     * 构造函数
     * <p>
     * 初始化服务商管理器, 创建并发映射用于存储服务商状态和执行器,
     * 并创建一个专用的请求执行器, 用于处理 AI 请求, 避免使用公共线程池.
     */
    public ProviderManager() {
        this.providerStatuses = new ConcurrentHashMap<>();
        this.providerExecutors = new ConcurrentHashMap<>();
        this.requestExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            /**
             * 用于生成线程序号的计数器, 初始值为 1
             * <p> 每次调用 newThread 方法时递增, 确保线程名称唯一
             */
            private final AtomicInteger index = new AtomicInteger(1);

            /**
             * 创建一个新的线程
             * <p> 根据指定的可运行任务创建线程, 线程名称格式为 "AIJ-Request-" 加上递增的索引, 设置为守护线程
             *
             * @param r 可运行任务, 不能为 null
             * @return 新创建的线程
             */
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r, "AIJ-Request-" + index.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });
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
     * 获取 AI 请求执行器
     *
     * @return 执行器
     */
    @NotNull
    public ExecutorService getRequestExecutor() {
        return requestExecutor;
    }

    /**
     * 检查服务商是否可用
     *
     * @param provider 服务商配置
     * @return 如果服务商可用返回 true, 否则返回 false
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
     * 获取可用服务商数量
     *
     * @return 可用数量
     */
    public int getAvailableProviderCount() {
        return (int) providerStatuses.values().stream()
            .filter(status -> status == ProviderStatus.AVAILABLE)
            .count();
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
        requestExecutor.shutdownNow();
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
