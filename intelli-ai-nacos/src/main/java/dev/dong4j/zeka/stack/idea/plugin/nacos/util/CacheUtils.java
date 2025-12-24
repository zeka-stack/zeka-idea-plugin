package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存工具类
 * 提供简单的内存缓存功能
 *
 * @author dong4j
 * @since 1.0.0
 */
public class CacheUtils {
    private static final ConcurrentHashMap<String, CacheEntry<?>> CACHE = new ConcurrentHashMap<>();

    /**
     * 缓存条目记录类
     * <p> 用于存储缓存数据及其过期时间, 提供判断缓存是否过期的功能, 适用于需要临时存储数据并设置有效期的场景
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.24
     * @since 1.0.0
     */
    private record CacheEntry<T>(T data, long expireTime) {
        private CacheEntry(T data, long expireTime) {
            this.data = data;
            this.expireTime = System.currentTimeMillis() + expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    /**
     * 存储数据到缓存
     *
     * @param key       键
     * @param data      数据
     * @param ttlMillis 存活时间（毫秒）
     * @param <T>       数据类型
     */
    public static <T> void put(@NotNull String key, @NotNull T data, long ttlMillis) {
        CACHE.put(key, new CacheEntry<>(data, ttlMillis));
    }

    /**
     * 从缓存获取数据
     *
     * @param key 键
     * @param <T> 数据类型
     * @return 数据，如果不存在或已过期则返回 null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T get(@NotNull String key) {
        CacheEntry<T> entry = (CacheEntry<T>) CACHE.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            CACHE.remove(key);
            return null;
        }

        return entry.data();
    }

    /**
     * 检查缓存中是否存在指定键
     *
     * @param key 键
     * @return 是否存在且未过期
     */
    public static boolean contains(@NotNull String key) {
        CacheEntry<?> entry = CACHE.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            CACHE.remove(key);
            return false;
        }

        return true;
    }

    /**
     * 从缓存中移除数据
     *
     * @param key 键
     */
    public static void remove(@NotNull String key) {
        CACHE.remove(key);
    }

    /**
     * 清空缓存
     */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    public static int size() {
        // 清理过期条目
        CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return CACHE.size();
    }

    /**
     * 存储数据到缓存（默认 5 分钟过期）
     *
     * @param key  键
     * @param data 数据
     * @param <T>  数据类型
     */
    public static <T> void put(@NotNull String key, @NotNull T data) {
        put(key, data, TimeUnit.MINUTES.toMillis(5));
    }
}
