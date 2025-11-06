package dev.dong4j.zeka.stack.idea.plugin.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import dev.dong4j.zeka.stack.idea.plugin.settings.JavaDocSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskExecutor;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档生成服务
 *
 * <p>提供统一的文档生成服务，消除各个 Action 类中的重复代码。
 * 封装了进度管理、任务执行、错误处理和结果通知等通用逻辑。
 *
 * <p>主要功能：
 * <ul>
 *   <li>统一的进度管理</li>
 *   <li>AI 服务可用性检查</li>
 *   <li>任务执行和错误处理</li>
 *   <li>结果通知回调</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * DocumentationGenerationService service = new DocumentationGenerationService();
 * service.generateDocumentation(project, tasks, "目标描述", (stats) -> {
 *     // 处理完成回调
 *     NotificationUtil.notifyCompletion(project, stats.completed(), stats.failed(), stats.skipped());
 * });
 * </pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class DocumentationGenerationService {

    /**
     * 生成文档
     *
     * <p>在后台任务中执行文档生成，提供统一的进度管理和错误处理。
     *
     * <p>执行流程：
     * <ol>
     *   <li>创建后台任务</li>
     *   <li>检查 AI 服务可用性</li>
     *   <li>执行文档生成任务</li>
     *   <li>调用完成回调</li>
     * </ol>
     *
     * @param project           项目对象
     * @param tasks             文档生成任务列表
     * @param targetDescription 目标描述（用于进度显示）
     * @param onComplete        完成回调，接收任务统计信息
     */
    public void generateDocumentation(@NotNull Project project,
                                      @NotNull List<DocumentationTask> tasks,
                                      @NotNull String targetDescription,
                                      @NotNull Consumer<TaskExecutor.TaskStatistics> onComplete) {

        if (tasks.isEmpty()) {
            log.warn("任务列表为空，跳过生成");
            return;
        }

        log.info("开始生成文档，任务数量: {}, 目标: {}", tasks.size(), targetDescription);

        // 在后台任务中处理
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, buildProgressTitle(targetDescription), true) {
                /**
                 * 执行文档生成任务，处理任务队列并更新进度指示器
                 * <p>
                 * 该方法用于启动文档生成任务，检查 AI 服务是否可用，执行任务队列，并根据任务执行结果
                 * 提供相应的反馈和回调处理。
                 *
                 * @param indicator 进度指示器，用于显示任务执行进度
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    TaskExecutor executor = new TaskExecutor(project, indicator);

                    // 检查 AI 服务是否可用
                    if (!executor.isServiceAvailable()) {
                        Notification notification = new Notification(NotificationUtil.NOTIFICATION_GROUP_ID,
                                                                     JavaDocBundle.message("notification.error.title"),
                                                                     "AI 服务配置错误，请在设置中检查 API Key、Base URL 等配置是否正确",
                                                                     NotificationType.ERROR);
                        // 添加设置动作
                        notification.addAction(new NotificationAction(JavaDocBundle.message("notification.error.message.config")) {
                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                JavaDocSettingsConfigurable configurable = new JavaDocSettingsConfigurable();
                                // 打开设置面板
                                ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
                                notification.expire();
                            }
                        });
                        Notifications.Bus.notify(notification, project);
                        return;
                    }

                    // 执行任务
                    boolean success = executor.processTasks(tasks);

                    if (success) {
                        TaskExecutor.TaskStatistics stats = executor.getStatistics();
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
     * 生成文档（简化版本）
     *
     * <p>使用默认的目标描述和标准的完成通知。
     *
     * @param project 项目对象
     * @param tasks   文档生成任务列表
     */
    public void generateDocumentation(@NotNull Project project, @NotNull List<DocumentationTask> tasks) {
        generateDocumentation(project, tasks, "文档", stats -> {
            NotificationUtil.notifyCompletion(project, stats.completed(), stats.failed(), stats.skipped());
        });
    }

    /**
     * 生成文档（带目标描述）
     *
     * <p>使用指定的目标描述和标准的完成通知。
     *
     * @param project           项目对象
     * @param tasks             文档生成任务列表
     * @param targetDescription 目标描述
     */
    public void generateDocumentation(@NotNull Project project,
                                      @NotNull List<DocumentationTask> tasks,
                                      @NotNull String targetDescription) {
        generateDocumentation(project, tasks, targetDescription, stats -> {
            NotificationUtil.notifyCompletion(project, stats.completed(), stats.failed(), stats.skipped());
        });
    }

    /**
     * 构建进度标题
     *
     * <p>根据目标描述构建进度对话框的标题。
     *
     * @param targetDescription 目标描述
     * @return 进度标题
     */
    @NotNull
    private String buildProgressTitle(@NotNull String targetDescription) {
        String baseTitle = JavaDocBundle.message("progress.generating");

        // 如果目标描述不是默认值，则添加到标题中
        if (!"文档".equals(targetDescription) && !targetDescription.isEmpty()) {
            return baseTitle + " - " + targetDescription;
        }

        return baseTitle;
    }

    /**
     * 检查任务是否为空
     *
     * <p>检查任务列表是否为空，如果为空则显示相应的通知。
     *
     * @param project 项目对象
     * @param tasks   任务列表
     * @param message 空任务时的提示消息
     * @return 如果任务为空返回 true
     */
    public boolean checkEmptyTasks(@NotNull Project project, @NotNull List<DocumentationTask> tasks, @NotNull String message) {
        if (tasks.isEmpty()) {
            NotificationUtil.notifyNoTask(project, message);
            return true;
        }
        return false;
    }
}
