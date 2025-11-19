package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能优化工具类
 * 提供异步执行、线程池管理等性能优化功能
 *
 * @author dong4j
 * @since 1.0.0
 */
public class PerformanceUtils {
    private static final ExecutorService EXECUTOR_SERVICE = createExecutorService();

    /**
     * 创建线程池
     *
     * @return 线程池
     */
    private static ExecutorService createExecutorService() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r, "Nacos-Plugin-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };

        return Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            threadFactory
                                           );
    }

    /**
     * 异步执行任务
     *
     * @param task 任务
     * @param <T>  返回值类型
     * @return CompletableFuture
     */
    public static <T> CompletableFuture<T> executeAsync(java.util.function.Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, EXECUTOR_SERVICE);
    }

    /**
     * 在后台线程执行任务
     *
     * @param project 项目实例
     * @param title   任务标题
     * @param task    任务
     */
    public static void executeInBackground(@Nullable Project project, @NotNull String title, @NotNull Runnable task) {
        new Task.Backgroundable(project, title, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                task.run();
            }
        }.queue();
    }

    /**
     * 在读操作中执行任务
     *
     * @param task 任务
     * @param <T>  返回值类型
     * @return 结果
     */
    public static <T> T runInReadAction(java.util.function.Supplier<T> task) {
        return ApplicationManager.getApplication().runReadAction((com.intellij.openapi.util.Computable<T>) task::get);
    }

    /**
     * 在写操作中执行任务
     *
     * @param task 任务
     */
    public static void runInWriteAction(@NotNull Runnable task) {
        ApplicationManager.getApplication().runWriteAction(task);
    }

    /**
     * 在事件调度线程中执行任务
     *
     * @param task 任务
     */
    public static void runInEdt(@NotNull Runnable task) {
        ApplicationManager.getApplication().invokeLater(task);
    }

    /**
     * 带有延迟的执行任务
     *
     * @param task  任务
     * @param delay 延迟时间（毫秒）
     */
    public static void executeWithDelay(@NotNull Runnable task, long delay) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                Thread.sleep(delay);
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        EXECUTOR_SERVICE.shutdown();
    }
}