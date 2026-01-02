package dev.dong4j.zeka.stack.idea.javadoc.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.ai.AIRequestComposer;
import dev.dong4j.zeka.stack.idea.javadoc.ai.JavadocAIResponseListener;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 顺序任务处理器
 * <p>
 * 负责按顺序处理文档生成任务，包括任务执行、进度更新、统计信息收集等。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SequentialTaskProcessor {
    /**
     * 项目对象
     * <p>
     * 用于持有当前处理的项目信息, 如项目配置, 资源路径等.
     *
     * @since 1.0.0
     */
    @NotNull
    private final Project project;

    /**
     * 进度指示器
     * <p>
     * 用于在任务处理过程中更新和显示任务进度的组件.
     * 可以用来通知用户当前任务的执行状态, 例如完成的任务数, 失败的任务数等.
     *
     * @since 1.0.0
     */
    @NotNull
    private final ProgressIndicator indicator;

    /** 设置配置 */
    @NotNull
    private final SettingsState settings;

    /**
     * AI 服务
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.12.01
     * @since 1.0.0
     */
    @NotNull
    private final AIService aiService;

    /**
     * 文档插入辅助类
     * <p>
     * 用于辅助插入生成的文档内容, 提供文档插入相关操作的封装.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.12.01
     * @since 1.0.0
     */
    @NotNull
    private final DocumentationInserterHelper inserterHelper;

    /**
     * 进度管理器
     *
     * <p> 用于管理文档生成任务的进度, 包括更新进度, 记录统计信息等.
     *
     * @see ProgressManager
     */
    @Nullable
    private final ProgressManager progressManager;

    /**
     * 顺序处理任务列表
     * <p>
     * 按顺序处理给定的任务列表, 更新进度指示器并记录处理结果统计信息.
     *
     * @param tasks 任务列表
     * @return 处理是否成功
     */
    public boolean processTasks(@NotNull List<DocumentationTask> tasks) {

        int totalTasks = tasks.size();

        // Console 日志：任务开始
        AIConsoleLoggerUtil.printWithTimestamp(project, String.format("========== 开始生成文档 任务总数: %s ==========", totalTasks));
        AIConsoleLoggerUtil.print(project, "");

        // 获取当前使用的提供商名称
        String providerName = settings.providerConfig != null
                              ? settings.providerConfig.providerType.getDisplayName()
                              : null;

        for (int i = 0; i < totalTasks && !indicator.isCanceled(); i++) {
            DocumentationTask task = tasks.get(i);

            // 任务开始处理时，更新进度显示当前处理的元素和提供商信息
            if (progressManager != null) {
                progressManager.updateProgress(i, task, providerName);
            }

            // 使用当前选中的服务商进行处理
            processTask(task, settings.providerConfig);

            // 任务处理完成后，更新进度显示最新的统计信息
            if (progressManager != null) {
                progressManager.updateProgress(i, task, providerName);
            }
        }

        // 使用进度管理器完成进度更新
        if (progressManager != null) {
            progressManager.finish();
        }

        // 使用进度管理器获取统计信息
        TaskStatistics statistics = progressManager != null ? progressManager.getStatistics() : new TaskStatistics(0, 0, 0);
        log.info("任务处理完成。成功: {}, 失败: {}, 跳过: {}",
                 statistics.completed(), statistics.failed(), statistics.skipped());

        // Console 日志：任务完成统计
        AIConsoleLoggerUtil.printWithTimestamp(project, "========== 生成完成 ==========");
        AIConsoleLoggerUtil.printSuccess(project, String.format("成功: %d | 失败: %d | 跳过: %d",
                                                                statistics.completed(), statistics.failed(), statistics.skipped()));

        return true;
    }

    /**
     * 处理文档生成任务
     * <p>
     * 该方法负责处理一个文档生成任务, 包括设置任务状态, 检查是否跳过, 生成文档, 插入文档以及处理异常.
     * 如果任务被跳过, 则更新状态并增加跳过计数. 如果生成文档失败或发生异常, 则更新任务状态为失败并记录错误信息.
     * 如果任务成功完成, 则更新状态为完成并增加完成计数.
     *
     * @param task     要处理的文档生成任务对象
     * @param provider AI 服务提供商配置
     */
    @SuppressWarnings("D")
    private void processTask(@NotNull DocumentationTask task, @NotNull AIProviderConfig provider) {
        try {
            task.setStatus(DocumentationTask.TaskStatus.PROCESSING);

            // 计算当前任务编号
            int currentTaskNum;
            if (progressManager != null) {
                currentTaskNum = progressManager.getCompletedCount() +
                                 progressManager.getFailedCount() +
                                 progressManager.getSkippedCount() + 1;
            } else {
                currentTaskNum = 1;
            }

            VirtualFile virtualFile = ApplicationManager.getApplication().runReadAction((Computable<VirtualFile>) () ->
                                                                                            task.getElement().getContainingFile().getVirtualFile()
                                                                                       );
            if (virtualFile != null) {
                String taskInfo = String.format("========== 任务 %d: %s %s ==========",
                                                currentTaskNum,
                                                task.getType().name(),
                                                task.getFilePath());
                AIConsoleLoggerUtil.printHyperlinkWithTimestamp(project, taskInfo, virtualFile, 0);
                AIConsoleLoggerUtil.print(project, "");
            }

            if (shouldSkip(task)) {
                task.setStatus(DocumentationTask.TaskStatus.SKIPPED);
                if (progressManager != null) {
                    progressManager.incrementSkipped();
                }
                AIConsoleLoggerUtil.printWarning(project, "⏭ 任务已跳过（已有文档）");
                AIConsoleLoggerUtil.print(project, "");
                return;
            }

            AIChatRequest request = AIRequestComposer.compose(settings, task);

            // 输出代码位置信息（可点击链接）
            boolean verboseLogging = isVerbose();
            if (virtualFile != null && verboseLogging) {
                PsiElement element = task.getElement();
                ApplicationManager.getApplication().runReadAction(() -> {
                    try {
                        Document document = FileDocumentManager.getInstance()
                            .getDocument(element.getContainingFile().getVirtualFile());
                        if (document != null) {
                            int startOffset = element.getTextRange().getStartOffset();
                            int lineNumber = document.getLineNumber(startOffset);
                            String fileName = virtualFile.getName();
                            String locationMessage = String.format("处理代码位置: %s:%d", fileName, lineNumber + 1);
                            AIConsoleLoggerUtil.printHyperlink(project, locationMessage, virtualFile, lineNumber);
                        }
                    } catch (Exception e) {
                        // 忽略异常，避免影响主功能
                    }
                });
            }

            // 使用 AIService API 生成内容
            AIResponseListener listener = verboseLogging ? new JavadocAIResponseListener(project) : null;
            String documentation = aiService.generateContent(project, request, provider, listener);

            if (documentation.trim().isEmpty()) {
                task.setStatus(DocumentationTask.TaskStatus.FAILED);
                task.setErrorMessage("生成的文档为空");
                if (progressManager != null) {
                    progressManager.incrementFailed();
                }
                AIConsoleLoggerUtil.printError(project, "✗ 任务失败: 生成的文档为空");
                AIConsoleLoggerUtil.print(project, "");
                return;
            }

            inserterHelper.insertDocumentation(task, documentation, verboseLogging);

            task.setStatus(DocumentationTask.TaskStatus.COMPLETED);
            task.setResult(documentation);
            if (progressManager != null) {
                progressManager.incrementCompleted();
            }

            AIConsoleLoggerUtil.printSuccess(project, "✓ 任务完成");
            AIConsoleLoggerUtil.print(project, "");

        } catch (AIServiceException e) {
            String errorMessage = AIServiceException.build(e);
            log.info("AI 服务调用失败: {} - {}", task, errorMessage, e);
            task.setStatus(DocumentationTask.TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            if (progressManager != null) {
                progressManager.incrementFailed();
            }
            AIConsoleLoggerUtil.printError(project, "✗ 任务失败: " + errorMessage);
            AIConsoleLoggerUtil.print(project, "");
        } catch (Exception e) {
            log.info("处理任务失败: {}", task, e);
            task.setStatus(DocumentationTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            if (progressManager != null) {
                progressManager.incrementFailed();
            }
            AIConsoleLoggerUtil.printError(project, "✗ 任务失败: " + e.getMessage());
            AIConsoleLoggerUtil.print(project, "");
        }
    }

    /**
     * 判断是否应该跳过任务
     *
     * <p>根据用户配置和元素状态决定是否跳过任务。
     * 主要用于避免重复生成已有文档的元素。
     *
     * <p>跳过条件：
     * <ul>
     *   <li>overrideExisting 配置为 false（默认）</li>
     *   <li>元素支持文档（PsiDocCommentOwner）</li>
     *   <li>元素已有 Javadoc 注释</li>
     * </ul>
     *
     * <p>逻辑说明：
     * <ul>
     *   <li>overrideExisting = false（默认）：跳过已有注释的元素</li>
     *   <li>overrideExisting = true：覆盖已有注释，不跳过</li>
     * </ul>
     *
     * <p>线程安全：
     * <ul>
     *   <li>PSI 访问必须在 read-action 中执行</li>
     *   <li>使用 ApplicationManager.runReadAction 确保线程安全</li>
     * </ul>
     *
     * @param task 文档生成任务
     * @return 如果应该跳过返回 true，否则返回 false
     * @see SettingsState#overrideExisting
     */
    private boolean shouldSkip(@NotNull DocumentationTask task) {
        if (settings.overrideExisting) {
            return false;
        }

        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            PsiElement element = task.getElement();
            if (element instanceof PsiDocCommentOwner) {
                com.intellij.psi.javadoc.PsiDocComment docComment = ((PsiDocCommentOwner) element).getDocComment();
                return docComment != null;
            }
            return false;
        });
    }

    /**
     * 判断指定提供者是否启用详细日志.
     *
     * @return 如果启用了详细日志则返回 true, 否则返回 false
     */
    private boolean isVerbose() {
        return AIProviderSettings.getInstance().verboseLogging;
    }
}

