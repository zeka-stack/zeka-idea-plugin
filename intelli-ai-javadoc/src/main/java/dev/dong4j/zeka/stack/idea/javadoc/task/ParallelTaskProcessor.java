package dev.dong4j.zeka.stack.idea.javadoc.task;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.task.parallel.DocumentationInserter;
import dev.dong4j.zeka.stack.idea.javadoc.task.parallel.DocumentationInserterWrapper;
import dev.dong4j.zeka.stack.idea.javadoc.task.parallel.ParallelTaskExecutor;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 并行任务处理器
 * <p>
 * 负责并行处理文档生成任务，使用消消乐式并行处理算法。
 * 支持多服务商并行处理、超时控制、错误处理和重试机制。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ParallelTaskProcessor {
    /**
     * 项目对象
     *
     * @NotNull
     * @see Project
     */
    @NotNull
    private final Project project;

    /**
     * 进度指示器
     * <p>
     * 用于显示并行任务处理过程中的进度信息, 包括任务状态, 完成百分比等.
     */
    @NotNull
    private final ProgressIndicator indicator;

    /** 设置配置 */
    @NotNull
    private final SettingsState settings;

    /**
     * AI 服务
     * <p>
     * 用于提供 AI 相关功能支持, 如文本生成, 代码补全等.
     * 该字段通过依赖注入方式注入, 确保在运行时可用.
     *
     * @see AIService
     */
    @NotNull
    private final AIService aiService;

    /**
     * 文档插入辅助类
     * <p>
     * 用于帮助插入文档的相关操作.
     */
    @NotNull
    private final DocumentationInserterHelper inserterHelper;

    /** 进度管理器, 用于管理任务执行过程中的进度显示和控制 */
    @NotNull
    private final ProgressManager progressManager;

    /**
     * 并行处理任务列表(性能模式)
     * <p>
     * 使用新的消消乐式并行处理算法:
     * <ul>
     * <li>每个文件是一个列, 文件中的任务是列中的块</li>
     * <li>多个线程 (乒乓球) 并发从队列中获取任务</li>
     * <li>同一文件的任务必须按顺序处理(一个块消除后才能处理下一个)</li>
     * <li>不同文件的任务可以并行处理</li>
     * </ul>
     * <p>
     * 支持功能:
     * <ul>
     * <li>超时控制(默认 10 秒)</li>
     * <li>429 错误处理(销毁服务商线程)</li>
     * <li>重试机制(最大 3 次)</li>
     * <li>负载均衡(轮询所有文件队列)</li>
     * </ul>
     *
     * @param project 项目对象, 用于上下文信息和资源访问
     * @param tasks   任务列表, 包含需要处理的文档任务
     * @return 处理是否成功
     */
    public boolean processTasks(@NotNull Project project, @NotNull List<DocumentationTask> tasks) {
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        if (aiProviderTypes.isEmpty()) {
            log.debug("性能模式启用但无可用提供商");
            return false;
        }

        DocumentationInserter documentationInserter = new DocumentationInserterWrapper(inserterHelper);

        AIProviderSettings providerSettings = AIProviderSettings.getInstance();

        // 创建并行任务执行器
        ParallelTaskExecutor parallelExecutor = new ParallelTaskExecutor(
            project,
            aiService,
            providerSettings,
            settings,
            indicator,
            documentationInserter,
            progressManager
        );

        // 执行并行任务处理
        boolean success = parallelExecutor.execute(tasks, aiProviderTypes);

        // 获取统计信息并更新进度管理器
        if (!parallelExecutor.getProviderStats().isEmpty()) {
            progressManager.finish();

            // 显示每个提供商的统计信息（如果启用）
            if (settings.showProviderStatistics) {
                ProviderStatisticsDisplay.showProviderStatistics(project, parallelExecutor.getProviderStats());
            }
        }

        return success;
    }

    /**
     * 获取已验证的 AI 服务提供商类型列表
     * <p>
     * 从全局设置中获取已验证的 AI 服务提供商配置, 并提取其中唯一的提供商类型.
     *
     * @return 包含已验证 AI 服务提供商类型的列表
     */
    @NotNull
    private static List<AIProviderConfig> getAiProviderTypes() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getVerifiedProviders();
    }

}

