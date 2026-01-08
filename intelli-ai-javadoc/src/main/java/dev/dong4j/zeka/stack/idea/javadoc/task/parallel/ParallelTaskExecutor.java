package dev.dong4j.zeka.stack.idea.javadoc.task.parallel;

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
import java.util.concurrent.atomic.AtomicInteger;

import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.ProgressManager;
import dev.dong4j.zeka.stack.idea.javadoc.task.ProviderStatistics;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
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
    /** 项目对象, 用于关联当前操作的上下文项目 */
    @NotNull
    private final Project project;

    /**
     * AI 服务
     * 提供与人工智能模型交互的能力, 用于执行文档生成等任务.
     */
    @NotNull
    private final AIService aiService;

    /** AI 服务商设置, 包含各服务商的配置参数和行为规则 */
    @NotNull
    private final AIProviderSettings providerSettings;

    /** 设置配置 */
    @NotNull
    private final SettingsState settings;

    /**
     * 进度指示器
     * 用于在任务执行过程中显示进度信息
     */
    @NotNull
    private final ProgressIndicator indicator;

    /** 文档插入器 */
    @NotNull
    private final DocumentationInserter documentationInserter;

    /** 进度管理器, 用于获取和更新任务执行进度信息 */
    @NotNull
    private final ProgressManager progressManager;

    /**
     * 任务分发器
     * 用于管理并行处理中任务的分配逻辑, 确保同一文件的任务按顺序处理.
     */
    @Getter
    private TaskDispatcher taskDispatcher;

    /** 服务商管理器 */
    @Getter
    private ProviderManager providerManager;

    /**
     * 服务商统计信息映射 (从 ProgressManager 获取, 确保使用同一个实例)
     * <p>
     * 该映射用于存储每个服务商的处理统计信息, 例如已完成, 失败和跳过的任务数.
     * 通过 {@link #providerStats} 可以获取各个服务商的统计详情.
     */
    @Getter
    private Map<String, ProviderStatistics> providerStats;

    /** 在途任务计数 (队列外仍在执行的任务数) */
    @Getter
    private final AtomicInteger inflightCount = new AtomicInteger(0);

    /**
     * 执行并行任务处理
     *
     * @param tasks     任务列表
     * @param providers 服务商配置列表
     * @return 处理是否成功
     */
    @SuppressWarnings("D")
    public boolean execute(@NotNull List<DocumentationTask> tasks,
                           @NotNull List<AIProviderConfig> providers) {
        if (tasks.isEmpty()) {
            log.debug("任务列表为空");
            return false;
        }

        if (providers.isEmpty()) {
            log.debug("没有可用的服务商");
            return false;
        }

        // 初始化任务分发器
        taskDispatcher = new TaskDispatcher(tasks);

        // 初始化服务商管理器
        providerManager = new ProviderManager();

        // 从 ProgressManager 获取 providerStats（确保使用同一个实例）
        Map<String, ProviderStatistics> statsMap = progressManager.getProviderStats();
        if (statsMap == null) {
            log.debug("ProgressManager 未初始化 providerStats，无法继续执行");
            return false;
        }
        this.providerStats = statsMap;

        // 初始化统计信息（如果 Map 中还没有对应提供商的统计信息）
        for (AIProviderConfig provider : providers) {
            String providerId = provider.providerType.getProviderId();
            String providerName = provider.providerType.getDisplayName();
            // 如果已存在则跳过，避免覆盖
            if (!providerStats.containsKey(providerId)) {
                ProviderStatistics stats = new ProviderStatistics(providerName);
                providerStats.put(providerId, stats);
            }
        }

        // 计算总线程数
        int totalThreads = calculateTotalThreads(tasks.size(), providers.size());

        log.debug("开始并行处理: {} 个任务, {} 个服务商, {} 个线程",
                 tasks.size(), providers.size(), totalThreads);

        // Console 日志：任务开始
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                              String.format("========== 开始生成文档（并行模式）任务总数: %d ==========", tasks.size()));
        AIConsoleLoggerUtil.print(project, "");

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

                AtomicInteger threadIndex = new AtomicInteger(1);
                // 创建服务商的执行器
                ExecutorService executor = Executors.newFixedThreadPool(currentProviderThreads, r -> {
                    Thread t = new Thread(r, "ParallelWorker-" + providerName + "-" + threadIndex.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                });

                providerExecutors.put(providerId, executor);
                providerManager.registerProvider(provider, executor);

                // 获取已创建的统计信息
                ProviderStatistics stats = providerStats.get(providerId);

                AIConsoleLoggerUtil.print(project, String.format("创建服务商线程池: %s (%d 个线程)", providerName, currentProviderThreads));
                AIConsoleLoggerUtil.print(project, "");

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
                        progressManager,
                        inflightCount
                    );
                    executor.submit(worker);
                }
            }

            // 等待所有任务完成
            waitForCompletion(providerExecutors);

            // 完成所有统计信息（设置结束时间）
            providerStats.values().forEach(ProviderStatistics::finish);

            // 完成进度管理器
            progressManager.finish();

            // 输出统计信息
            printStatistics();

            return true;

        } catch (Exception e) {
            log.debug("并行任务处理失败", e);
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
     * 计算合适的线程数
     * 策略：根据任务数和提供商数动态计算
     * - 如果任务数较少（<=10），每个提供商1个线程
     * - 如果任务数中等（10-50），每个提供商2个线程
     * - 如果任务数较多（>50），每个提供商3-4个线程
     *
     * @param taskCount     任务数量
     * @param providerCount 服务商数量
     * @return 总线程数
     */
    private int calculateTotalThreads(int taskCount, int providerCount) {
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
        AIConsoleLoggerUtil.print(project, String.format("性能模式：使用 %d 个提供商，创建 %d 个线程（平均每个提供商 %.1f 个线程，平均并发度 %.1f）并行处理 %d 个任务",
                                                        providerCount, totalThreads, avgConcurrency, avgConcurrency, taskCount));

        return totalThreads;
    }

    /**
     * 等待所有任务完成
     * <p>
     * 等待逻辑：
     * <ol>
     *   <li>等待所有队列为空（包括文件队列和重试队列）</li>
     *   <li>等待所有正在处理的任务完成（给一个缓冲时间）</li>
     *   <li>关闭线程池并等待所有线程退出</li>
     * </ol>
     *
     * @param providerExecutors 服务商执行器映射
     */
    @SuppressWarnings("D")
    private void waitForCompletion(@NotNull Map<String, ExecutorService> providerExecutors) {
        // 1. 等待所有队列为空且在途计数归零
        waitForQueuesEmpty();

        // 2. 关闭所有执行器并等待线程退出
        CompletableFuture<?>[] futures = providerExecutors.values().stream()
            .map(executor -> CompletableFuture.runAsync(() -> {
                try {
                    executor.shutdown();
                    // 等待最多 60 秒让所有任务完成
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        log.debug("线程池未在 60 秒内完成，强制关闭");
                        executor.shutdownNow();
                        // 再等待 10 秒确保线程退出
                        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                            log.debug("线程池强制关闭后仍有线程未退出");
                        }
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
     * 等待所有队列为空
     * <p>
     * 轮询检查文件队列、重试队列及在途计数，直到全部清空。
     */
    private void waitForQueuesEmpty() {
        int maxWaitTime = 300; // 最多等待 30 秒（300 * 100ms）
        int waitCount = 0;
        int consecutiveEmptyChecks = 0; // 连续空队列检查次数

        while (waitCount < maxWaitTime) {
            boolean hasTasks = taskDispatcher.hasTasks();
            boolean hasInflight = inflightCount.get() > 0;

            if (!hasTasks && !hasInflight) {
                consecutiveEmptyChecks++;
                // 连续 5 次检查都为空，认为队列确实为空
                if (consecutiveEmptyChecks >= 5) {
                    log.debug("所有队列已为空，连续检查 {} 次确认", consecutiveEmptyChecks);
                    break;
                }
            } else {
                consecutiveEmptyChecks = 0; // 重置计数器
            }

            try {
                Thread.sleep(100); // 每 100ms 检查一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            waitCount++;
        }

        if (waitCount >= maxWaitTime) {
            log.debug("等待队列为空超时（30秒），可能仍有任务在处理中");
        }
    }

    /**
     * 输出统计信息
     */
    private void printStatistics() {
        int totalCompleted = 0;
        int totalFailed = 0;
        int totalSkipped = 0;

        for (ProviderStatistics stats : providerStats.values()) {
            totalCompleted += stats.getCompletedCount();
            totalFailed += stats.getFailedCount();
            totalSkipped += stats.getSkippedCount();
        }

        log.debug("并行任务处理完成。成功: {}, 失败: {}, 跳过: {}",
                 totalCompleted, totalFailed, totalSkipped);

        // Console 日志：任务完成统计
        AIConsoleLoggerUtil.printWithTimestamp(project, "========== 生成完成 ==========");
        AIConsoleLoggerUtil.printSuccess(project,
                                        String.format("成功: %d | 失败: %d | 跳过: %d",
                                                      totalCompleted, totalFailed, totalSkipped));
    }
}
