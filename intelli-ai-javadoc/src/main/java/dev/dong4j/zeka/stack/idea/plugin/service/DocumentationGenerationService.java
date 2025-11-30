package dev.dong4j.zeka.stack.idea.plugin.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskExecutor;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskStatistics;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档生成服务类
 * <p>
 * 负责处理项目中文档的生成任务, 提供异步的文档生成功能, 支持进度监控和完成回调.
 * 该服务类主要用于 IDE 环境中的文档自动生成, 能够处理多个文档任务并提供进度反馈.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class DocumentationGenerationService {
    /**
     * 生成文档的主方法, 执行文档生成任务并处理结果
     * <p>
     * 该方法接收项目, 任务列表, 目标描述和任务完成回调, 如果任务列表为空则直接返回.
     * 否则, 开始文档生成流程, 处理任务并根据结果调用回调.
     *
     * @param project           项目对象, 用于获取项目相关资源
     * @param tasks             文档生成任务列表
     * @param targetDescription 目标描述, 用于标识生成文档的目标
     * @param onComplete        任务完成后的回调函数, 接收任务执行统计信息
     */
    public void generateDocumentation(@NotNull Project project,
                                      @NotNull List<DocumentationTask> tasks,
                                      @NotNull String targetDescription,
                                      @NotNull Consumer<TaskStatistics> onComplete) {

        if (tasks.isEmpty()) {
            log.warn("任务列表为空，跳过生成");
            return;
        }

        log.info("开始生成文档，任务数量: {}, 目标: {}", tasks.size(), targetDescription);

        // 在后台任务中处理
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, buildProgressTitle(targetDescription), true) {
                /**
                 * 执行文档生成任务, 根据进度指示器更新状态并处理任务结果
                 * <p>
                 * 该方法用于启动文档生成任务, 检查 AI 服务是否可用, 若不可用则提示配置错误;
                 * 若可用则执行任务, 并根据任务执行结果通知用户.
                 *
                 * @param indicator 进度指示器, 用于显示任务执行进度
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    TaskExecutor executor = new TaskExecutor(project, indicator);

                    // 检查 AI 服务是否可用
                    if (!executor.isServiceAvailable()) {
                        Notification notification = new Notification(NotificationUtil.NOTIFICATION_GROUP_ID,
                                                                     JavaDocBundle.message("notification.error.title"),
                                                                     JavaDocBundle.message("notification.service.config.error"),
                                                                     NotificationType.ERROR);
                        // 添加设置动作
                        NotificationUtil.addOpenConfigurablePanelAction(notification, project);
                        return;
                    }

                    // 执行任务
                    boolean success = executor.processTasks(tasks);

                    if (success) {
                        TaskStatistics stats = executor.getStatistics();
                        log.info("文档生成完成: {}", stats);

                        // 在 EDT 中调用完成回调
                        ApplicationManager.getApplication().invokeLater(() -> {
                            onComplete.accept(stats);
                        });
                    } else {
                        log.warn("文档生成失败");
                    }
                }
            }
                                         );
    }

    /**
     * 生成项目文档并通知任务完成状态
     * <p>
     * 根据指定的项目, 任务列表和目标描述生成文档, 并在任务执行完成后通过通知机制告知用户任务完成情况.
     *
     * @param project           项目对象, 用于标识文档生成的项目
     * @param tasks             任务列表, 包含需要执行的文档生成任务
     * @param targetDescription 目标描述, 用于通知中的说明信息
     */
    public void generateDocumentation(@NotNull Project project,
                                      @NotNull List<DocumentationTask> tasks,
                                      @NotNull String targetDescription) {
        generateDocumentation(project, tasks, targetDescription, stats -> {
            if (stats.isRunned()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.notifyTargetCompletion(project, targetDescription, stats.completed(), stats.failed(), stats.skipped());
                });
            }
        });
    }

    /**
     * 构建进度标题字符串
     * <p>
     * 根据目标描述生成进度标题, 若目标描述不是“文档”且不为空, 则在基础标题后添加描述内容.
     *
     * @param targetDescription 目标描述信息
     * @return 生成的进度标题字符串
     */
    @NotNull
    private String buildProgressTitle(@NotNull String targetDescription) {
        String baseTitle = JavaDocBundle.message("background.task.title");
        String defaultTarget = JavaDocBundle.message("task.target.selection");

        // 如果目标描述不是默认值，则添加到标题中
        if (!targetDescription.isEmpty() && !defaultTarget.equals(targetDescription)) {
            return JavaDocBundle.message("background.task.title.with.target", targetDescription);
        }

        return baseTitle;
    }

    /**
     * 检查任务列表是否为空, 若为空则发送通知并返回 true
     * <p>
     * 该方法用于判断传入的任务列表是否为空, 若为空则调用通知工具发送提示信息, 并返回 true.
     *
     * @param project 项目对象
     * @param tasks   任务列表
     * @param message 提示信息内容
     * @return 若任务列表为空返回 true, 否则返回 false
     */
    public boolean checkEmptyTasks(@NotNull Project project, @NotNull List<DocumentationTask> tasks, @NotNull String message) {
        if (tasks.isEmpty()) {
            NotificationUtil.notifyNoTask(project, message);
            return true;
        }
        return false;
    }
}
