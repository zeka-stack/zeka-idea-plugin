package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务执行器类
 * <p>
 * 负责执行文档生成任务的管理器, 支持串行和并行两种处理模式. 该类提供了任务进度跟踪,
 * 多提供商并行处理, 统计信息收集等功能, 能够处理类, 方法, 字段等不同类型的文档生成任务.
 * 支持性能模式下的多线程并行处理, 提高大量任务的处理效率.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class TaskExecutor {

    /** 项目对象，用于表示当前操作的项目上下文 */
    private final Project project;
    /** 进度指示器，用于显示任务执行进度 */
    private final ProgressIndicator indicator;
    /** 用户设置状态对象，用于存储和管理应用的配置和用户偏好设置 */
    private final SettingsState settings;
    /** AI 服务实例 */
    private final AIService aiService;
    /** 进度管理器，统一管理单线程和多线程的进度更新 */
    private ProgressManager progressManager;

    /** 文档插入辅助类 */
    private final DocumentationInserterHelper inserterHelper;

    /**
     * 构造任务执行器
     *
     * @param project   项目对象
     * @param indicator 进度指示器
     */
    public TaskExecutor(@NotNull Project project, @NotNull ProgressIndicator indicator) {
        this.project = project;
        this.indicator = indicator;
        this.settings = SettingsState.getInstance();
        this.aiService = ApplicationManager.getApplication().getService(AIService.class);
        this.inserterHelper = new DocumentationInserterHelper(project, settings);
    }

    /**
     * 处理任务列表
     *
     * <p>批量处理文档生成任务列表，按顺序执行每个任务。
     * 在处理过程中更新进度指示器，显示实时统计信息。
     *
     * <p>处理流程：
     * <ol>
     *   <li>检查 AI 服务是否可用</li>
     *   <li>检查任务列表是否为空</li>
     *   <li>设置进度指示器为确定模式</li>
     *   <li>遍历任务列表逐个处理</li>
     *   <li>更新进度和统计信息</li>
     *   <li>处理完成后更新最终状态</li>
     * </ol>
     *
     * <p>取消支持：
     * <ul>
     *   <li>检查 indicator.isCanceled() 决定是否继续处理</li>
     *   <li>用户可以通过进度对话框取消操作</li>
     * </ul>
     *
     * @param project 项目对象
     * @param tasks   任务列表
     */
    public boolean processTasks(@NotNull Project project, @NotNull List<DocumentationTask> tasks) {
        if (tasks.isEmpty()) {
            return false;
        }

        indicator.setIndeterminate(false);
        int totalTasks = tasks.size();

        log.info("开始处理 {} 个文档生成任务", totalTasks);

        // 检查是否启用性能模式且任务数量大于5个
        if (SettingsState.getInstance().performanceMode && totalTasks > 10) {
            final Map<String, ProviderStatistics> providerStats = new ConcurrentHashMap<>();
            progressManager = new ProgressManager(indicator, totalTasks, providerStats);
            ParallelTaskProcessor processor = new ParallelTaskProcessor(
                this.project, indicator, settings, aiService, inserterHelper, progressManager
            );
            return processor.processTasks(project, tasks);
        } else {
            // 初始化单线程模式的进度管理器
            progressManager = new ProgressManager(indicator, totalTasks);
            SequentialTaskProcessor processor = new SequentialTaskProcessor(
                this.project, indicator, settings, aiService, inserterHelper, progressManager
            );
            return processor.processTasks(tasks);
        }
    }

    /**
     * 获取统计信息
     *
     * <p>返回任务处理的统计信息，包括完成、失败和跳过的任务数量。
     * 用于向用户显示处理结果。
     *
     * <p>统计内容：
     * <ul>
     *   <li>完成数量：成功处理的任务数</li>
     *   <li>失败数量：处理失败的任务数</li>
     *   <li>跳过数量：被跳过的任务数</li>
     *   <li>总计：所有任务的总数</li>
     * </ul>
     *
     * @return 任务统计信息
     * @see TaskStatistics
     */
    public TaskStatistics getStatistics() {
        if (progressManager != null) {
            return progressManager.getStatistics();
        }
        return new TaskStatistics(0, 0, 0);
    }
}

