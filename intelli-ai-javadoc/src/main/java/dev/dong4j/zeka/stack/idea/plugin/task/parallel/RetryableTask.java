package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import lombok.Getter;
import lombok.Setter;

/**
 * 可重试任务包装类
 * <p>
 * 用于包装需要重试的任务，记录重试次数和最后一次错误信息。
 * 当任务执行失败时，会被放入重试队列，由其他线程重新处理。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
public class RetryableTask {
    /** 原始任务 */
    @NotNull
    @Getter
    private final DocumentationTask task;

    /** 重试次数 */
    @Getter
    @Setter
    private int retryCount;

    /** 最后一次错误信息 */
    @Nullable
    @Getter
    @Setter
    private String lastError;

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    /**
     * 构造函数
     *
     * @param task 原始任务
     */
    public RetryableTask(@NotNull DocumentationTask task) {
        this.task = task;
        this.retryCount = 0;
        this.lastError = null;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        ++retryCount;
    }

    /**
     * 检查是否超过最大重试次数
     *
     * @return 如果超过最大重试次数返回 true
     */
    public boolean isMaxRetriesExceeded() {
        return retryCount >= MAX_RETRY_COUNT;
    }

    /**
     * 获取最大重试次数
     *
     * @return 最大重试次数
     */
    public static int getMaxRetryCount() {
        return MAX_RETRY_COUNT;
    }
}

