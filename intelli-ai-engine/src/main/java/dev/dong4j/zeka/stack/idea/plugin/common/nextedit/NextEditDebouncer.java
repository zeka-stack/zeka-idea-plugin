package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.util.concurrency.AppExecutorUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 防抖任务调度器类
 * <p> 用于对相同 key 的重复操作进行防抖处理, 确保在指定延迟时间内只执行最后一次任务.
 * <p> 通过使用 ScheduledExecutorService 实现异步延迟执行, 并支持取消之前的未完成任务.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditDebouncer {
    /** 存储待执行任务的映射, 键为任务标识, 值为定时任务句柄, 用于去重和取消重复任务 */
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    /**
     * 延迟执行指定任务
     * <p> 根据给定的键取消已存在的相同键的任务, 并在指定延迟后执行新的任务
     *
     * @param key     用于标识任务的唯一键
     * @param delayMs 延迟时间, 单位为毫秒
     * @param task    要延迟执行的任务
     */
    void debounce(String key, long delayMs, Runnable task) {
        ScheduledFuture<?> existing = tasks.remove(key);
        if (existing != null) {
            existing.cancel(false);
        }
        ScheduledFuture<?> future = AppExecutorUtil.getAppScheduledExecutorService()
            .schedule(task, delayMs, TimeUnit.MILLISECONDS);
        tasks.put(key, future);
    }
}
