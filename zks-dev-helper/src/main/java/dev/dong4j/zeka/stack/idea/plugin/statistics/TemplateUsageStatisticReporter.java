package dev.dong4j.zeka.stack.idea.plugin.statistics;

/**
 * 模板使用统计报告器接口
 * <p>
 * 用于报告模板的使用统计信息，提供统一的接口以记录和统计模板的使用情况。
 * 实现该接口的类需要提供具体的统计逻辑，如记录使用次数、调用时间等。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.10.25
 * @since 1.0.0
 */
public interface TemplateUsageStatisticReporter {
    /**
     * 报告模板的使用统计信息
     * <p>
     * 用于记录或上报指定模板的使用情况数据。
     *
     * @param template 模板名称或标识符
     */
    void reportUsage(String template);
}
