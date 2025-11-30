package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.progress.ProgressIndicator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * 进度管理器
 * <p>
 * 用于管理文档生成任务的进度跟踪, 支持并行和串行两种模式. 该类负责更新进度指示器,
 * 统计任务完成情况 (完成数, 失败数, 跳过数), 并提供详细的进度信息显示功能.
 * 在并行模式下, 可以按提供者分别统计任务状态; 在串行模式下, 使用统一的计数器进行统计.
 * 主要用于文档生成过程中的进度可视化和状态监控.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ProgressManager {
    /** 进度指示器 */
    private final ProgressIndicator indicator;
    /** 总任务数 */
    private final int totalTasks;
    /** 是否为多线程模式 */
    private final boolean isParallel;
    /** 单线程模式下的完成计数器 */
    private final AtomicInteger completedCount = new AtomicInteger(0);
    /** 单线程模式下的失败计数器 */
    private final AtomicInteger failedCount = new AtomicInteger(0);
    /** 单线程模式下的跳过计数器 */
    private final AtomicInteger skippedCount = new AtomicInteger(0);
    /** 多线程模式下的提供商统计信息映射（可选） */
    private final Map<String, ProviderStatistics> providerStats;

    /**
     * 创建单线程模式的进度管理器
     *
     * @param indicator  进度指示器
     * @param totalTasks 总任务数
     */
    public ProgressManager(@NotNull ProgressIndicator indicator, int totalTasks) {
        this.indicator = indicator;
        this.totalTasks = totalTasks;
        this.isParallel = false;
        this.providerStats = null;
    }

    /**
     * 创建多线程模式的进度管理器
     *
     * @param indicator     进度指示器
     * @param totalTasks    总任务数
     * @param providerStats 提供商统计信息映射
     */
    public ProgressManager(@NotNull ProgressIndicator indicator,
                           int totalTasks,
                           @NotNull Map<String, ProviderStatistics> providerStats) {
        this.indicator = indicator;
        this.totalTasks = totalTasks;
        this.isParallel = true;
        this.providerStats = providerStats;
    }

    /**
     * 判断是否为多线程模式
     *
     * @return 如果是多线程模式返回 true
     */
    public boolean isParallelMode() {
        return isParallel;
    }

    /**
     * 获取当前已完成的任务数
     *
     * @return 已完成任务数
     */
    public int getCompletedCount() {
        if (isParallelMode() && providerStats != null) {
            return providerStats.values().stream()
                .mapToInt(ProviderStatistics::getCompletedCount)
                .sum();
        }
        return completedCount.get();
    }

    /**
     * 获取当前失败的任务数
     *
     * @return 失败任务数
     */
    public int getFailedCount() {
        if (isParallelMode() && providerStats != null) {
            return providerStats.values().stream()
                .mapToInt(ProviderStatistics::getFailedCount)
                .sum();
        }
        return failedCount.get();
    }

    /**
     * 获取当前跳过的任务数
     *
     * @return 跳过任务数
     */
    public int getSkippedCount() {
        if (isParallelMode() && providerStats != null) {
            return providerStats.values().stream()
                .mapToInt(ProviderStatistics::getSkippedCount)
                .sum();
        }
        return skippedCount.get();
    }

    /**
     * 增加已完成任务计数（单线程模式）
     */
    public void incrementCompleted() {
        if (!isParallelMode()) {
            completedCount.incrementAndGet();
        }
    }

    /**
     * 增加失败任务计数（单线程模式）
     */
    public void incrementFailed() {
        if (!isParallelMode()) {
            failedCount.incrementAndGet();
        }
    }

    /**
     * 增加跳过任务计数（单线程模式）
     */
    public void incrementSkipped() {
        if (!isParallelMode()) {
            skippedCount.incrementAndGet();
        }
    }

    /**
     * 更新进度指示器
     * <p>
     * 根据当前处理的任务索引和统计信息更新进度条、主文本和副文本。
     *
     * @param currentIndex 当前处理的任务索引（从0开始）
     * @param currentTask  当前处理的任务（可选，用于显示文件路径）
     * @param providerName 当前处理任务的提供商名称（可选，多线程模式使用）
     */
    @SuppressWarnings("D")
    public void updateProgress(int currentIndex, DocumentationTask currentTask, String providerName) {
        int processed = getCompletedCount() + getFailedCount() + getSkippedCount();
        double fraction = totalTasks > 0 ? (double) processed / totalTasks : 0.0;

        // 更新进度条
        indicator.setFraction(fraction);

        // 获取类的全路径和表情符号
        String qualifiedName = "";
        String emoji = "";
        if (currentTask != null) {
            qualifiedName = TaskElementHelper.getElementQualifiedName(currentTask.getElement());
            emoji = TaskElementHelper.getTaskTypeEmoji(currentTask.getType());
        }

        // 更新主文本：显示处理进度
        // 单线程和多线程模式都显示提供商信息
        if (providerName != null && !providerName.isEmpty()) {
            // 显示提供商名称和当前处理的元素详细信息
            if (!qualifiedName.isEmpty()) {
                indicator.setText(String.format("[%s] %s %s (%d/%d)",
                                                providerName, emoji, qualifiedName,
                                                isParallelMode() ? processed : currentIndex + 1, totalTasks));
            } else if (currentTask != null) {
                // 如果无法获取完整路径，回退到文件路径
                indicator.setText(String.format("[%s] %s %s (%d/%d)",
                                                providerName, emoji, currentTask.getFilePath(),
                                                isParallelMode() ? processed : currentIndex + 1, totalTasks));
            } else {
                indicator.setText(String.format("[%s] 已处理 %d/%d 个任务",
                                                providerName,
                                                isParallelMode() ? processed : currentIndex + 1, totalTasks));
            }
        } else {
            // 如果没有提供商信息，显示基本进度信息
            if (currentTask != null && !qualifiedName.isEmpty()) {
                indicator.setText(String.format("%s %s (%d/%d)",
                                                emoji, qualifiedName,
                                                isParallelMode() ? processed : currentIndex + 1, totalTasks));
            } else if (currentTask != null) {
                indicator.setText(String.format("%s %s (%d/%d)",
                                                emoji, currentTask.getFilePath(),
                                                isParallelMode() ? processed : currentIndex + 1, totalTasks));
            } else {
                indicator.setText(String.format("正在处理 (%d/%d)",
                                                isParallelMode() ? processed : currentIndex + 1, totalTasks));
            }
        }

        // 更新副文本：显示统计信息
        updateStatisticsText();
    }

    /**
     * 更新统计信息文本
     * <p>
     * 在多线程模式下，还会显示各提供商的处理情况。
     */
    public void updateStatisticsText() {
        if (isParallelMode() && providerStats != null) {
            // 多线程模式：显示总体统计和提供商详情
            StringBuilder statsText = new StringBuilder();
            statsText.append(String.format("完成: %d, 失败: %d, 跳过: %d",
                                           getCompletedCount(), getFailedCount(), getSkippedCount()));

            // 添加各提供商的详细统计信息
            if (!providerStats.isEmpty()) {
                statsText.append(" | ");
                final List<String> providerInfo = getProviderInfo();
                if (!providerInfo.isEmpty()) {
                    statsText.append(String.join(" | ", providerInfo));
                }
            }

            indicator.setText2(statsText.toString());
        } else {
            // 单线程模式：只显示总体统计
            indicator.setText2(String.format("完成: %d, 失败: %d, 跳过: %d",
                                             getCompletedCount(), getFailedCount(), getSkippedCount()));
        }
    }

    @NotNull
    private List<String> getProviderInfo() {
        List<String> providerInfo = new ArrayList<>();
        if (providerStats != null) {
            for (ProviderStatistics stats : providerStats.values()) {
                int completed = stats.getCompletedCount();
                int failed = stats.getFailedCount();
                int skipped = stats.getSkippedCount();
                int total = stats.getTotalCount();
                if (total > 0) {
                    // 显示提供商名称和详细统计：完成/失败/跳过
                    providerInfo.add(String.format("%s: 完成: %d, 失败: %d, 跳过: %d",
                                                   stats.getProviderName(), completed, failed, skipped));
                }
            }
        }
        return providerInfo;
    }

    /**
     * 完成进度更新
     * <p>
     * 将进度设置为100%，并显示完成消息。
     */
    public void finish() {
        indicator.setFraction(1.0);
        indicator.setText(JavaDocBundle.message("task.progress.completed"));
    }

    /**
     * 获取统计信息
     *
     * @return 任务统计信息
     */
    public TaskStatistics getStatistics() {
        return new TaskStatistics(
            getCompletedCount(),
            getFailedCount(),
            getSkippedCount()
        );
    }
}

