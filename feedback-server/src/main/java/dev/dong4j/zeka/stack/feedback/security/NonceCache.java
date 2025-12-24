package dev.dong4j.zeka.stack.feedback.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Nonce 缓存管理
 * <p>
 * 用于防止重放攻击，每个 nonce 只能使用一次，并在 5 分钟后自动过期。
 * 使用内存缓存实现，适合单机部署。如果需要分布式部署，应使用 Redis。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.12.23
 * @since 1.0.0
 */
@Slf4j
@Component
public class NonceCache {
    /** Nonce 过期时间（秒） */
    private static final long NONCE_EXPIRY_SECONDS = 300; // 5 分钟

    /** 存储 nonce 和其过期时间 */
    private final ConcurrentHashMap<String, Long> nonceMap = new ConcurrentHashMap<>();

    /** 定时清理任务 */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nonce-cache-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 构造函数
     * <p>
     * 启动定时清理任务，每 60 秒清理一次过期的 nonce
     */
    public NonceCache() {
        // 每 60 秒清理一次过期的 nonce
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredNonces, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 检查并记录 nonce
     * <p>
     * 如果 nonce 已存在，返回 false（表示重放攻击）
     * 如果 nonce 不存在，记录它并返回 true
     *
     * @param nonce 随机数
     * @return 如果 nonce 有效（未使用过）返回 true，否则返回 false
     */
    public boolean checkAndStore(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }

        long expiryTime = Instant.now().getEpochSecond() + NONCE_EXPIRY_SECONDS;
        Long existing = nonceMap.putIfAbsent(nonce, expiryTime);

        if (existing != null) {
            // nonce 已存在，可能是重放攻击
            log.warn("Duplicate nonce detected: {}", nonce);
            return false;
        }

        return true;
    }

    /**
     * 清理过期的 nonce
     */
    private void cleanupExpiredNonces() {
        long now = Instant.now().getEpochSecond();
        nonceMap.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    /**
     * 销毁方法
     * <p>
     * 关闭定时清理任务
     */
    public void destroy() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

