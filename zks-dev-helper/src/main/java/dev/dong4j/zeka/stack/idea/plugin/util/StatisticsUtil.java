package dev.dong4j.zeka.stack.idea.plugin.util;

import dev.dong4j.zeka.stack.idea.plugin.settings.UniformFormatSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.statistics.TemplateUsageStatisticReporter;
import dev.dong4j.zeka.stack.idea.plugin.statistics.TemplateUsageStatisticsReporterImpl;

/**
 * 统计工具类
 * <p>
 * 提供模板使用情况的统计功能，用于记录不同类型的模板（如文件模板、Live Template、代码风格等）的使用情况。
 * 该类通过统一的统计接口，将模板使用数据上报至指定的统计报告器。
 * <p>
 * 支持以下统计类型：
 * - 文件模板使用情况
 * - Live Template 使用情况
 * - 代码风格应用情况
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
public class StatisticsUtil {

    /** 模板使用统计报告器，用于收集和上报模板的使用情况 */
    private static final TemplateUsageStatisticReporter REPORTER = new TemplateUsageStatisticsReporterImpl();

    /**
     * 报告指定模板的使用情况
     * <p>
     * 根据配置是否启用统计功能，上报指定模板的使用情况。
     *
     * @param templateName 模板名称
     */
    public static void reportTemplateUsage(String templateName) {
        UniformFormatSettingsState settings = UniformFormatSettingsState.getInstance();
        if (settings.isEnableStatistics()) {
            REPORTER.reportUsage(templateName);
        }
    }

    /**
     * 报告文件模板的使用情况
     * <p>
     * 调用报告模板使用的方法，传入"file-template"作为模板类型，用于统计或记录文件模板的使用情况。
     */
    public static void reportFileTemplateUsage() {
        reportTemplateUsage("file-template");
    }

    /**
     * 报告 Live Template 的使用情况
     * <p>
     * 该方法用于记录指定名称的 Live Template 被使用的事件，内部会将模板名称转换为特定格式后调用报告模板使用的方法。
     *
     * @param templateName Live Template 的名称
     */
    public static void reportLiveTemplateUsage(String templateName) {
        reportTemplateUsage("live-template-" + templateName);
    }

    /**
     * 报告代码风格应用情况
     * <p>
     * 调用报告模板使用方法，用于记录或展示代码风格的使用情况。
     *
     * @param templateId 模板ID，用于标识具体的代码风格模板
     */
    public static void reportCodeStyleUsage() {
        reportTemplateUsage("code-style");
    }
}
