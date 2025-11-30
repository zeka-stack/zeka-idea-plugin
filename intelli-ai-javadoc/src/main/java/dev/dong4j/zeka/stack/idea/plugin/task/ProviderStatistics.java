package dev.dong4j.zeka.stack.idea.plugin.task;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

/**
 * 服务提供者统计信息类
 * <p>
 * 用于统计服务提供者的执行情况, 包括完成数量, 失败数量, 跳过数量等指标,
 * 并提供执行时长统计和状态更新功能. 该类是线程安全的, 使用原子类来保证
 * 计数器的并发安全性.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class ProviderStatistics {
    /** 服务提供商名称 */
    @Getter
    private final String providerName;
    /** 完成的任务数量计数器，用于记录已成功完成的任务数 */
    private final AtomicInteger completedCount = new AtomicInteger(0);
    /** 失败计数器，用于记录失败操作的次数 */
    private final AtomicInteger failedCount = new AtomicInteger(0);
    /** 被跳过的记录数量 */
    private final AtomicInteger skippedCount = new AtomicInteger(0);
    /** 开始时间戳，表示操作或任务开始的时刻 */
    private final long startTime;
    /** 结束时间，表示某个操作或任务的结束时间戳 */
    private long endTime;

    /**
     * 初始化 ProviderStatistics 实例
     * <p>
     * 通过传入的 providerName 初始化统计信息，并记录开始时间
     *
     * @param providerName 提供商名称
     */
    public ProviderStatistics(String providerName) {
        this.providerName = providerName;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 获取已完成任务的数量
     * <p>
     * 返回当前已完成任务的计数值
     *
     * @return 已完成任务的数量
     */
    public int getCompletedCount() {
        return completedCount.get();
    }

    /**
     * 获取失败操作的计数
     * <p>
     * 返回当前记录的失败操作次数。
     *
     * @return 失败操作的计数
     */
    public int getFailedCount() {
        return failedCount.get();
    }

    /**
     * 获取已跳过的项目数量
     * <p>
     * 返回当前已跳过的项目计数。
     *
     * @return 已跳过的项目数量
     */
    public int getSkippedCount() {
        return skippedCount.get();
    }

    /**
     * 获取总任务数
     * <p>
     * 返回已完成、失败和跳过任务数的总和
     *
     * @return 总任务数
     */
    public int getTotalCount() {
        return completedCount.get() + failedCount.get() + skippedCount.get();
    }

    /**
     * 获取操作的持续时间
     * <p>
     * 计算并返回从开始时间到结束时间的时间差，单位为毫秒
     *
     * @return 操作的持续时间（毫秒）
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * 增加已完成任务的计数
     * <p>
     * 调用计数器的 incrementAndGet 方法，将已完成任务的数量增加 1。
     */
    public void incrementCompleted() {
        completedCount.incrementAndGet();
    }

    /**
     * 增加失败计数器的值
     * <p>
     * 该方法用于将失败计数器的值增加1，通常用于记录系统或操作失败的次数。
     */
    public void incrementFailed() {
        failedCount.incrementAndGet();
    }

    /**
     * 增加跳过计数
     * <p>
     * 用于增加跳过操作的计数器值。
     */
    public void incrementSkipped() {
        skippedCount.incrementAndGet();
    }

    /**
     * 结束计时，记录当前时间作为结束时间
     * <p>
     * 该方法用于标记操作或任务的结束时间，将当前系统时间赋值给 endTime 字段
     */
    public void finish() {
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 返回该对象的字符串表示形式，包含执行状态的详细信息。
     * <p>
     * 该方法按照指定格式拼接字符串，展示完成数、失败数、跳过数、总计数以及耗时信息。
     *
     * @return 对象的字符串表示，格式为：providerName: 完成=..., 失败=..., 跳过=..., 总计=..., 耗时=...
     */
    @Override
    public String toString() {
        return String.format("%s: 完成=%d, 失败=%d, 跳过=%d, 总计=%d, 耗时=%.1fs",
                             providerName, getCompletedCount(), getFailedCount(),
                             getSkippedCount(), getTotalCount(), getDuration() / 1000.0);
    }
}

