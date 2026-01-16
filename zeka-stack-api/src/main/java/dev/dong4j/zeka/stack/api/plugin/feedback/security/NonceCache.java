package dev.dong4j.zeka.stack.api.plugin.feedback.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * 非重复令牌缓存类
 * <p> 用于在系统内部验证请求中携带的 nonce(一次性令牌), 防止重复提交或重放攻击.
 * 该类通过内存缓存机制记录已使用的 nonce, 并在指定时间后自动清理过期项, 确保安全性与性能平衡.
 * 本类不负责请求处理, 仅作为基础设施组件提供验证服务, 符合面向对象设计原则, 避免将基础设施关注点污染业务逻辑.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
@Component
public class NonceCache {
    /** Nonce 过期时间 (秒), 默认为 300 秒 (5 分钟) */
    private static final long NONCE_EXPIRY_SECONDS = 300; // 5 分钟

    /** 存储 nonce 和其过期时间 */
    private final ConcurrentHashMap<String, Long> nonceMap = new ConcurrentHashMap<>();

    /**
     * 定时清理任务调度器
     * <p>
     * 用于周期性执行过期 nonce 的清理工作, 每 60 秒一次, 确保缓存内存占用可控.
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html">ScheduledExecutorService</a>
     */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "nonce-cache-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 构造函数
     * <p>
     * 启动定时清理任务, 每 60 秒执行一次清理过期 nonce 的操作, 以确保缓存内存占用可控.
     *
     * @since 1.0.0
     */
    public NonceCache() {
        // 每 60 秒清理一次过期的 nonce
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredNonces, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 检查并记录 nonce
     * <p>
     * 如果 nonce 已存在, 返回 false(表示重放攻击)<br>
     * 如果 nonce 不存在, 记录它并返回 true
     *
     * @param nonce 随机数
     * @return 如果 nonce 有效 (未使用过) 返回 true, 否则返回 false
     */
    public boolean checkAndStore(String nonce) {
        if (nonce == null || nonce.isEmpty()) {
            return false;
        }

        long expiryTime = Instant.now().getEpochSecond() + NONCE_EXPIRY_SECONDS;
        Long existing = nonceMap.putIfAbsent(nonce, expiryTime);

        if (existing != null) {
            // nonce 已存在，可能是重放攻击
            log.debug("Duplicate nonce detected: {}", nonce);
            return false;
        }

        return true;
    }

    /**
     * 清理过期的 nonce
     * <p>
     * 遍历缓存中的所有 nonce 条目, 移除那些过期时间小于当前时间戳的条目.
     * 该方法用于定期清理内存中已过期的 nonce, 防止缓存无限增长.
     *
     * @since 1.0.0
     */
    private void cleanupExpiredNonces() {
        long now = Instant.now().getEpochSecond();
        nonceMap.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    /**
     * 销毁方法
     * <p>
     * 关闭定时清理任务, 确保资源被正确释放. 如果在等待终止期间被中断, 则会立即强制关闭调度器并恢复中断状态.
     *
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

