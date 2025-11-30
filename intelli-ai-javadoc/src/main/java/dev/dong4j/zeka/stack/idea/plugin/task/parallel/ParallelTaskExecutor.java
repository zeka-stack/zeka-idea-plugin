package dev.dong4j.zeka.stack.idea.plugin.task.parallel;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.console.JavaDocConsoleView;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskExecutor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 并行任务执行器
 * <p>
 * 实现消消乐式的任务处理算法：
 * - 每个文件是一个列，文件中的任务是列中的块
 * - 多个线程（乒乓球）并发从队列中获取任务
 * - 同一文件的任务必须按顺序处理（一个块消除后才能处理下一个）
 * - 不同文件的任务可以并行处理
 * <p>
 * 支持功能：
 * - 超时控制（默认 10 秒）
 * - 429 错误处理（销毁服务商线程）
 * - 重试机制（最大 3 次）
 * - 负载均衡（轮询所有文件队列）
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ParallelTaskExecutor {
    /** 项目对象 */
    @NotNull
    private final Project project;

    /** AI 服务 */
    @NotNull
    private final AIService aiService;

    @NotNull
    private final AIProviderSettings providerSettings;

    /** 设置配置 */
    @NotNull
    private final SettingsState settings;

    /** 进度指示器 */
    @NotNull
    private final ProgressIndicator indicator;

    /** 文档插入器 */
    @NotNull
    private final DocumentationInserter documentationInserter;

    /** 任务分发器 */
    @Getter
    private TaskDispatcher taskDispatcher;

    /** 服务商管理器 */
    @Getter
    private ProviderManager providerManager;

    /** 服务商统计信息映射 */
    @Getter
    private final Map<String, TaskExecutor.ProviderStatistics> providerStats = new ConcurrentHashMap<>();

    /**
     * 执行并行任务处理
     *
     * @param tasks     任务列表
     * @param providers 服务商配置列表
     * @return 处理是否成功
     */
    public boolean execute(@NotNull List<DocumentationTask> tasks,
                           @NotNull List<AIProviderConfig> providers) {
        if (tasks.isEmpty()) {
            log.warn("任务列表为空");
            return false;
        }

        if (providers.isEmpty()) {
            log.warn("没有可用的服务商");
            return false;
        }

        // 初始化任务分发器
        taskDispatcher = new TaskDispatcher(tasks);

        // 初始化服务商管理器
        providerManager = new ProviderManager();

        // 初始化进度管理器（需要在创建统计信息后）
        // 先创建统计信息，然后创建进度管理器
        for (AIProviderConfig provider : providers) {
            String providerId = provider.providerType.getProviderId();
            String providerName = provider.providerType.getDisplayName();
            TaskExecutor.ProviderStatistics stats = new TaskExecutor.ProviderStatistics(providerName);
            providerStats.put(providerId, stats);
        }

        // 进度管理器
        TaskExecutor.ProgressManager progressManager = new TaskExecutor.ProgressManager(indicator, tasks.size(), providerStats);

        // 计算总线程数
        int totalThreads = calculateTotalThreads(tasks.size(), providers.size());

        log.info("开始并行处理: {} 个任务, {} 个服务商, {} 个线程",
                 tasks.size(), providers.size(), totalThreads);

        // Console 日志：任务开始
        JavaDocConsoleView.printWithTimestamp(project,
                                              String.format("========== 开始生成文档（并行模式）任务总数: %d ==========", tasks.size()));
        JavaDocConsoleView.print(project, "");

        // 为每个服务商创建线程池
        Map<String, ExecutorService> providerExecutors = new ConcurrentHashMap<>();
        int threadsPerProvider = totalThreads / providers.size();
        int remainder = totalThreads % providers.size();

        try {
            // 为每个服务商创建线程池和工作线程
            for (int i = 0; i < providers.size(); i++) {
                AIProviderConfig provider = providers.get(i);
                String providerId = provider.providerType.getProviderId();
                String providerName = provider.providerType.getDisplayName();

                // 计算当前服务商的线程数
                int currentProviderThreads = threadsPerProvider + (i < remainder ? 1 : 0);

                // 创建服务商的执行器
                ExecutorService executor = Executors.newFixedThreadPool(currentProviderThreads, r -> {
                    Thread t = new Thread(r, "ParallelWorker-" + providerName + "-" + System.currentTimeMillis());
                    t.setDaemon(true);
                    return t;
                });

                providerExecutors.put(providerId, executor);
                providerManager.registerProvider(provider, executor);

                // 获取已创建的统计信息
                TaskExecutor.ProviderStatistics stats = providerStats.get(providerId);

                JavaDocConsoleView.print(project, String.format("创建服务商线程池: %s (%d 个线程)", providerName, currentProviderThreads));
                JavaDocConsoleView.print(project, "");

                // 为服务商创建多个工作线程
                for (int j = 0; j < currentProviderThreads; j++) {
                    ParallelTaskWorker worker = new ParallelTaskWorker(
                        taskDispatcher,
                        providerManager,
                        provider,
                        aiService,
                        project,
                        providerSettings,
                        settings,
                        indicator,
                        stats,
                        documentationInserter,
                        progressManager
                    );
                    executor.submit(worker);
                }
            }

            // 等待所有任务完成
            waitForCompletion(providerExecutors);

            // 完成所有统计信息（设置结束时间）
            providerStats.values().forEach(TaskExecutor.ProviderStatistics::finish);

            // 完成进度管理器
            progressManager.finish();

            // 输出统计信息
            printStatistics();

            return true;

        } catch (Exception e) {
            log.error("并行任务处理失败", e);
            return false;
        } finally {
            // 关闭所有执行器
            providerExecutors.values().forEach(executor -> {
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdownNow();
                }
            });
            providerManager.shutdownAll();
        }
    }

    /**
     * 计算总线程数
     *
     * @param taskCount     任务数量
     * @param providerCount 服务商数量
     * @return 总线程数
     */
    private int calculateTotalThreads(int taskCount, int providerCount) {
        // 计算合适的线程数
        // 策略：根据任务数和提供商数动态计算
        // - 如果任务数较少（<=10），每个提供商1个线程
        // - 如果任务数中等（10-50），每个提供商2个线程
        // - 如果任务数较多（>50），每个提供商3-4个线程
        int threadsPerProvider;
        if (taskCount <= 10) {
            threadsPerProvider = 1;
        } else if (taskCount <= 50) {
            threadsPerProvider = 2;
        } else {
            threadsPerProvider = Math.min(4, Math.max(2, taskCount / (providerCount * 2)));
        }

        int totalThreads = providerCount * threadsPerProvider;

        // 如果总线程数超过任务数，限制为任务数
        if (totalThreads > taskCount) {
            totalThreads = taskCount;
            threadsPerProvider = Math.max(1, totalThreads / providerCount);
            totalThreads = providerCount * threadsPerProvider;
        }

        // 计算平均并发度
        double avgConcurrency = (double) totalThreads / providerCount;
        JavaDocConsoleView.print(project, String.format("性能模式：使用 %d 个提供商，创建 %d 个线程（平均每个提供商 %.1f 个线程，平均并发度 %.1f）并行处理 %d 个任务",
                                                        providerCount, totalThreads, avgConcurrency, avgConcurrency, taskCount));

        return totalThreads;
    }

    /**
     * 等待所有任务完成
     *
     * @param providerExecutors 服务商执行器映射
     */
    private void waitForCompletion(@NotNull Map<String, ExecutorService> providerExecutors) {
        // 等待所有执行器完成
        CompletableFuture<?>[] futures = providerExecutors.values().stream()
            .map(executor -> CompletableFuture.runAsync(() -> {
                try {
                    executor.shutdown();
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
    }

    /**
     * 输出统计信息
     */
    private void printStatistics() {
        int totalCompleted = 0;
        int totalFailed = 0;
        int totalSkipped = 0;

        for (TaskExecutor.ProviderStatistics stats : providerStats.values()) {
            totalCompleted += stats.getCompletedCount();
            totalFailed += stats.getFailedCount();
            totalSkipped += stats.getSkippedCount();
        }

        log.info("并行任务处理完成。成功: {}, 失败: {}, 跳过: {}",
                 totalCompleted, totalFailed, totalSkipped);

        // Console 日志：任务完成统计
        JavaDocConsoleView.printWithTimestamp(project, "========== 生成完成 ==========");
        JavaDocConsoleView.printSuccess(project,
                                        String.format("成功: %d | 失败: %d | 跳过: %d",
                                                      totalCompleted, totalFailed, totalSkipped));
    }
}

