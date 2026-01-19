package dev.dong4j.zeka.stack.api.auth.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * OAuth state 缓存
 */
@Component
public class OAuthStateCache {
    /** OAuth state 过期时间 (单位: 秒) */
    private static final long STATE_EXPIRY_SECONDS = 600;
    /** 用于存储 OAuth 状态信息的并发哈希映射表 */
    private final ConcurrentHashMap<String, StateEntry> stateMap = new ConcurrentHashMap<>();
    /**
     * 定时清理过期 OAuth 状态的调度器
     * <p>
     * 使用单线程计划执行器定期清理过期的 OAuth 状态条目.
     *
     * @see ScheduledExecutorService
     */
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "oauth-state-cleanup");
        t.setDaemon(true);
        return t;
    });

    /**
     * 构造函数, 初始化 OAuthStateCache 对象
     * <p> 在构造函数中, 设置定时任务以每分钟清理过期的 OAuth 状态条目
     *
     * @since 1.0
     */
    public OAuthStateCache() {
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpired, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 将指定的 state 和设备 ID 存储到缓存中, 用于 OAuth 流程中的状态验证
     * <p> 该方法会计算过期时间, 并将状态与设备 ID 关联存储在 ConcurrentHashMap 中
     *
     * @param state    要存储的状态字符串, 通常用于 OAuth 的 state 参数
     * @param deviceId 与状态相关联的设备 ID
     */
    public void store(String state, String deviceId) {
        long expiryTime = Instant.now().getEpochSecond() + STATE_EXPIRY_SECONDS;
        stateMap.put(state, new StateEntry(deviceId, expiryTime));
    }

    /**
     * 消费 OAuth 状态令牌并返回关联的设备 ID
     * <p> 从缓存中移除指定状态令牌, 并验证其是否未过期. 若状态有效, 则返回关联的设备 ID; 否则返回 null.
     * <p> 该方法会检查状态令牌是否存在, 是否已过期, 若均通过验证, 则返回对应的设备 ID.
     *
     * @param state OAuth 状态令牌, 不能为空或空字符串
     * @return 关联的设备 ID, 若状态无效或已过期则返回 null
     */
    public String consume(String state) {
        if (state == null || state.isEmpty()) {
            return null;
        }
        StateEntry entry = stateMap.remove(state);
        if (entry == null) {
            return null;
        }
        long now = Instant.now().getEpochSecond();
        if (entry.expiryTime() < now) {
            return null;
        }
        return entry.deviceId();
    }

    /**
     * 清理已过期的 OAuth 状态条目
     * <p> 该方法会检查 stateMap 中的所有条目, 移除那些已过期的状态记录. 过期判断依据是当前时间与状态条目中记录的过期时间进行比较.
     *
     * @since 1.0
     */
    private void cleanupExpired() {
        long now = Instant.now().getEpochSecond();
        stateMap.entrySet().removeIf(entry -> entry.getValue().expiryTime() < now);
    }

    /**
     * OAuth 状态条目类
     * <p> 用于存储 OAuth 状态验证相关的设备 ID 和过期时间信息 </p>
     * <p> 该类主要用于 OAuth state 缓存机制中, 为每个状态值保存对应的设备标识和时间戳,
     * 支持过期验证和清理功能 </p>
     * <p> 通常由 {@link Component} 注解的缓存服务内部使用 </p>
     *
     * @param deviceId   设备 ID
     * @param expiryTime 过期时间, 单位为毫秒
     * @author dong4j
     * @version 1.0.0
     * @email mailto:dong4j@gmail.com
     * @date 2026.01.19
     * @since x.x.x
     */
        private record StateEntry(String deviceId, long expiryTime) {
    }
}
