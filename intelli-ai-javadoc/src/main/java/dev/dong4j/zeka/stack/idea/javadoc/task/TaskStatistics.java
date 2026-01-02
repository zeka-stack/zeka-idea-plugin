package dev.dong4j.zeka.stack.idea.javadoc.task;

import org.jetbrains.annotations.NotNull;

/**
 * 任务统计记录类
 * <p>
 * 用于统计任务执行情况, 包括已完成, 失败和跳过的任务数量,
 * 并提供获取总任务数和判断是否有任务执行的方法.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public record TaskStatistics(int completed, int failed, int skipped) {

    /**
     * 计算并返回总任务数
     * <p>
     * 将已完成, 失败和跳过的任务数量相加, 得到总任务数
     *
     * @return 总任务数
     */
    public int getTotal() {
        return completed + failed + skipped;
    }

    /**
     * 判断是否已执行过任务
     * <p>
     * 检查当前对象中已完成或跳过的任务数量总和是否大于 0, 若大于 0 表示已执行过任务.
     *
     * @return 如果已完成或跳过的任务数大于 0, 则返回 true; 否则返回 false
     */
    public boolean isRunned() {
        return completed + skipped > 0;
    }

    /**
     * 返回对象的字符串表示形式
     * <p>
     * 以格式化字符串的形式展示对象的完成数、失败数、跳过数和总计数
     *
     * @return 对象的字符串表示
     */
    @NotNull
    @Override
    public String toString() {
        return String.format("完成: %d, 失败: %d, 跳过: %d, 总计: %d",
                             completed, failed, skipped, getTotal());
    }
}

