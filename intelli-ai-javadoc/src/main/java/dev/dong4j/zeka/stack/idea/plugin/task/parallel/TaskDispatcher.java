package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务分发器
 * <p>
 * 管理所有文件队列和重试队列，提供统一的任务获取接口。
 * 线程可以从文件队列或重试队列中获取任务进行处理。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
public class TaskDispatcher {
    /** 文件队列映射 */
    @Getter
    private final Map<String, FileTaskQueue> fileQueues;

    /** 重试队列 */
    @Getter
    private final RetryQueue retryQueue;

    /** 所有文件路径列表（用于轮询） */
    private final List<String> filePaths;

    /** 当前轮询的文件索引 */
    private int currentFileIndex = 0;

    /**
     * 构造函数
     *
     * @param tasks 所有任务列表
     */
    public TaskDispatcher(@NotNull List<DocumentationTask> tasks) {
        this.fileQueues = new ConcurrentHashMap<>();
        this.retryQueue = new RetryQueue();

        // 按文件路径分组任务
        Map<String, List<DocumentationTask>> tasksByFile = tasks.stream()
            .collect(Collectors.groupingBy(DocumentationTask::getFilePath));

        // 为每个文件创建队列
        tasksByFile.forEach((filePath, fileTasks) -> {
            FileTaskQueue queue = new FileTaskQueue(filePath);
            fileTasks.forEach(queue::addTask);
            fileQueues.put(filePath, queue);
        });

        this.filePaths = List.copyOf(fileQueues.keySet());

        log.info("任务分发器初始化完成: {} 个文件, {} 个任务",
                 fileQueues.size(), tasks.size());
    }

    /**
     * 获取下一个任务
     * <p>
     * 优先从文件队列获取任务，如果所有文件队列都为空，则从重试队列获取。
     * 使用轮询策略，确保负载均衡。
     *
     * @return 任务包装对象，包含任务和队列信息，如果所有队列都为空返回 null
     */
    @Nullable
    public TaskWrapper getNextTask() {
        // 1. 优先从文件队列获取任务（轮询所有文件）
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(currentFileIndex);
            currentFileIndex = (currentFileIndex + 1) % filePaths.size();

            FileTaskQueue queue = fileQueues.get(filePath);
            if (queue == null || queue.isEmpty()) {
                continue;
            }

            // 尝试获取锁并取任务
            if (queue.tryLock()) {
                try {
                    DocumentationTask task = queue.pollTask();
                    if (task != null) {
                        return new TaskWrapper(task, queue, null);
                    }
                } finally {
                    // 如果没有取到任务，释放锁
                    queue.unlock();
                }
            }
        }

        // 2. 如果所有文件队列都为空，从重试队列获取
        RetryableTask retryableTask = retryQueue.pollTask();
        if (retryableTask != null) {
            return new TaskWrapper(retryableTask.getTask(), null, retryableTask);
        }

        return null;
    }

    /**
     * 检查是否还有任务
     *
     * @return 如果还有任务返回 true
     */
    public boolean hasTasks() {
        // 检查文件队列
        boolean hasFileTasks = fileQueues.values().stream()
            .anyMatch(queue -> !queue.isEmpty());

        // 检查重试队列
        boolean hasRetryTasks = !retryQueue.isEmpty();

        return hasFileTasks || hasRetryTasks;
    }

    /**
     * 将任务添加到重试队列
     *
     * @param task      任务
     * @param lastError 错误信息
     */
    public void addToRetryQueue(@NotNull DocumentationTask task, @Nullable String lastError) {
        RetryableTask retryableTask = new RetryableTask(task);
        retryableTask.setLastError(lastError);
        retryQueue.addTask(retryableTask);
    }

    /**
     * 将可重试任务添加到重试队列（复用现有的 RetryableTask）
     *
     * @param retryableTask 可重试任务
     */
    public void addToRetryQueue(@NotNull RetryableTask retryableTask) {
        retryQueue.addTask(retryableTask);
    }

    /**
         * 任务包装类
         * <p>
         * 包含任务、文件队列和重试任务信息，用于任务处理完成后释放锁。
         */
        public record TaskWrapper(@NotNull DocumentationTask task, @Nullable FileTaskQueue fileQueue, @Nullable RetryableTask retryableTask) {

        /**
             * 释放文件队列锁
             * <p>
             * 任务处理完成后调用，允许下一个任务被获取。
             */
            public void releaseLock() {
                if (fileQueue != null) {
                    fileQueue.unlock();
                }
            }

            /**
             * 检查是否是重试任务
             *
             * @return 如果是重试任务返回 true
             */
            public boolean isRetryTask() {
                return retryableTask != null;
            }
        }
}

