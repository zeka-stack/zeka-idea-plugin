package dev.dong4j.zeka.stack.idea.javadoc.task.parallel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 重试队列管理器
 * <p>
 * 管理所有需要重试的任务. 当任务执行失败时 (非 429 错误), 会被放入重试队列.
 * 重试队列中的任务可以被任何可用的线程获取并重试.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
public class RetryQueue {
    /** 重试任务队列, 用于存储需要重试的任务 */
    @Getter
    private final Queue<RetryableTask> retryQueue;

    /**
     * 构造函数
     * <p>
     * 初始化一个新的重试队列, 创建一个并发安全的任务队列用于存储需要重试的任务.
     */
    public RetryQueue() {
        this.retryQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * 添加任务到重试队列
     * <p>
     * 当任务执行失败时 (非 429 错误), 会被放入重试队列. 重试队列中的任务可以被任何可用的线程获取并重试.
     *
     * @param retryableTask 可重试任务
     */
    public void addTask(@NotNull RetryableTask retryableTask) {
        retryableTask.incrementRetry();
        retryQueue.offer(retryableTask);
        log.debug("任务加入重试队列: {}, 重试次数: {}",
                  retryableTask.getTask().getFilePath(),
                  retryableTask.getRetryCount());
    }

    /**
     * 从重试队列获取任务
     *
     * @return 可重试任务, 如果队列为空则返回 null
     */
    @Nullable
    public RetryableTask pollTask() {
        return retryQueue.poll();
    }

    /**
     * 检查队列是否为空
     *
     * @return 如果队列为空返回 true
     */
    public boolean isEmpty() {
        return retryQueue.isEmpty();
    }

    /**
     * 获取队列大小
     *
     * @return 队列中任务的数量
     */
    public int size() {
        return retryQueue.size();
    }
}

