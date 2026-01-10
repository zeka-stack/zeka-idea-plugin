package dev.dong4j.zeka.stack.idea.plugin.statistics;

import com.intellij.openapi.application.ApplicationInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

/**
 * 模板使用统计报告器实现类
 * <p>
 * 该类用于实现模板使用统计报告功能，负责收集模板使用信息并异步上报。主要功能包括构建使用模型、记录使用时间、IDE版本、插件版本等信息，并通过异步方式将统计数据发送至日志系统。
 * <p>
 * 使用异步机制确保统计上报不会影响主线程性能，同时通过日志记录方式实现数据持久化和监控。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
@Slf4j
public class TemplateUsageStatisticsReporterImpl implements TemplateUsageStatisticReporter {
    /**
     * 异步上报使用统计数据
     * <p>
     * 根据传入的模板信息创建使用统计模型，并异步执行上报操作。上报逻辑可扩展，如发送到服务器或写入本地文件。
     *
     * @param template 模板名称，用于标识统计数据的类型或来源
     */
    @Override
    public void reportUsage(String template) {
        try {
            UsageModel usageModel = new UsageModel();
            usageModel.setTemplateName(template);
            usageModel.setTimestamp(System.currentTimeMillis());
            usageModel.setVersion("1.0.0");
            usageModel.setIde(ApplicationInfo.getInstance().getVersionName());
            usageModel.setIdeVersion(ApplicationInfo.getInstance().getFullVersion());
            usageModel.setPluginVersion("1.0.0");

            // 异步上报统计数据
            CompletableFuture.runAsync(() -> {
                try {
                    // 这里可以实现具体的上报逻辑
                    // 比如发送到服务器、写入本地文件等
                    logUsageStatistics(usageModel);
                } catch (Exception e) {
                    log.debug("Failed to report usage statistics", e);
                }
            });

        } catch (Exception e) {
            log.debug("Failed to create usage model", e);
        }
    }

    /**
     * 记录模板使用统计信息
     * <p>
     * 将模板使用情况记录到日志中，包括模板名称、时间戳、IDE信息及版本等
     *
     * @param usageModel 使用统计模型，包含模板使用相关数据
     */
    private void logUsageStatistics(UsageModel usageModel) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.debug("Template usage statistics: template=" + usageModel.getTemplateName() +
                 ", timestamp=" + timestamp +
                 ", ide=" + usageModel.getIde() +
                 ", ideVersion=" + usageModel.getIdeVersion() +
                 ", pluginVersion=" + usageModel.getPluginVersion());
    }
}
