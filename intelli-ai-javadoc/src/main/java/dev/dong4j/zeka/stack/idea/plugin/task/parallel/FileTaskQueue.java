package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件任务队列
 * <p>
 * 管理单个文件的所有任务，确保同一文件的任务按顺序处理。
 * 使用锁机制保证线程安全，多个线程可以并发从不同文件的队列中获取任务。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
public class FileTaskQueue {
    /** 文件路径 */
    @NotNull
    @Getter
    private final String filePath;

    /** 任务队列 */
    @NotNull
    private final Queue<DocumentationTask> taskQueue;

    /** 队列锁，保证同一文件的任务串行处理 */
    @NotNull
    private final ReentrantLock lock;

    /**
     * 构造函数
     *
     * @param filePath 文件路径
     */
    public FileTaskQueue(@NotNull String filePath) {
        this.filePath = filePath;
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.lock = new ReentrantLock();
    }

    /**
     * 添加任务到队列
     *
     * @param task 任务
     */
    public void addTask(@NotNull DocumentationTask task) {
        taskQueue.offer(task);
    }

    /**
     * 从队列头部获取任务（调用方需已持有锁）
     *
     * @return 任务，如果队列为空返回 null
     */
    @Nullable
    public DocumentationTask pollTaskLocked() {
        return taskQueue.poll();
    }

    /**
     * 释放锁
     * <p>
     * 在任务处理完成后调用，允许下一个任务被获取。
     */
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 尝试获取锁
     * <p>
     * 非阻塞方式尝试获取锁，如果获取成功返回 true，否则返回 false。
     *
     * @return 如果成功获取锁返回 true
     */
    public boolean tryLock() {
        return lock.tryLock();
    }

    /**
     * 检查队列是否为空
     *
     * @return 如果队列为空返回 true
     */
    public boolean isEmpty() {
        return taskQueue.isEmpty();
    }

    /**
     * 获取队列大小
     *
     * @return 队列中任务的数量
     */
    public int size() {
        return taskQueue.size();
    }
}
