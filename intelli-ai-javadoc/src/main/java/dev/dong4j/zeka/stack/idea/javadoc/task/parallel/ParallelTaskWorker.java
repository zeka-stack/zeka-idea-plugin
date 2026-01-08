package dev.dong4j.zeka.stack.idea.javadoc.task.parallel;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.dong4j.zeka.stack.idea.javadoc.ai.AIRequestComposer;
import dev.dong4j.zeka.stack.idea.javadoc.ai.JavadocAIResponseListener;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.ProgressManager;
import dev.dong4j.zeka.stack.idea.javadoc.task.ProviderStatistics;
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
 * 并行任务工作线程
 * <p>
 * 负责从任务分发器中获取任务并处理。支持超时控制、错误处理和重试机制。
 * 当任务执行失败时，会根据错误类型进行相应处理（429 错误销毁服务商线程，其他错误放入重试队列）。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ParallelTaskWorker implements Runnable {
    /**
     * 任务分发器
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.12.01
     * @since 1.0.0
     */
    @NotNull
    private final TaskDispatcher taskDispatcher;

    /**
     * 服务商管理器
     * <p>
     * 用于管理可用的服务商, 包括状态监控, 限流处理和线程销毁等操作.
     */
    @NotNull
    private final ProviderManager providerManager;

    /**
     * 服务商配置
     * <p>
     * 存储与当前工作线程相关联的服务商的配置信息, 用于 AI 服务调用参数设置.
     */
    @NotNull
    private final AIProviderConfig provider;

    /** AI 服务 */
    @NotNull
    private final AIService aiService;

    /** 项目对象 */
    @NotNull
    private final Project project;

    /**
     * 设置配置
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.12.01
     * @since 1.0.0
     */
    @NotNull
    private final AIProviderSettings providerSettings;

    /**
     * 设置配置
     *
     * @see SettingsState
     */
    @NotNull
    private final SettingsState settings;

    /**
     * 进度指示器
     * <p>
     * 用于监控任务执行进度, 并在用户取消操作时提供中断信号.
     * 该指示器通常由外部进度管理器维护, 线程运行时会检查其是否被取消.
     */
    @NotNull
    private final ProgressIndicator indicator;

    /**
     * 统计信息
     * 用于记录并行任务处理过程中的统计指标, 如已完成, 失败和跳过的任务数量.
     */
    @NotNull
    private final ProviderStatistics stats;

    /**
     * 文档插入器
     * <p>
     * 用于将生成的文档内容插入到指定的位置.
     */
    @NotNull
    private final DocumentationInserter documentationInserter;

    /** 进度管理器 */
    @NotNull
    private final ProgressManager progressManager;

    /** 在途任务计数 */
    @NotNull
    private final java.util.concurrent.atomic.AtomicInteger inflightCount;

    /** 默认超时时间（秒） */
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

    @Override
    public void run() {
        String providerName = provider.providerType.getDisplayName();
        String threadName = Thread.currentThread().getName();
        log.debug("工作线程 {} 开始运行（服务商: {}）", threadName, providerName);

        while (!indicator.isCanceled() && providerManager.isProviderAvailable(provider)) {
            // 检查是否还有任务
            if (!taskDispatcher.hasTasks()) {
                log.debug("工作线程 {} 没有更多任务，退出", threadName);
                break;
            }

            // 从分发器获取任务
            TaskDispatcher.TaskWrapper taskWrapper = taskDispatcher.getNextTask();
            if (taskWrapper == null) {
                // 短暂休眠后重试，避免 CPU 空转
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            DocumentationTask task = taskWrapper.task();
            boolean isRetryTask = taskWrapper.isRetryTask();

            try {
                // 处理任务（带超时）
                processTaskWithTimeout(task, taskWrapper, isRetryTask);
            } catch (Exception e) {
                log.debug("工作线程 {} 处理任务时发生异常", threadName, e);
                handleTaskFailure(task, taskWrapper, e.getMessage(), isRetryTask);
            } finally {
                // 释放文件队列锁
                taskWrapper.releaseLock();
            }
        }

        log.debug("工作线程 {} 结束运行", threadName);
    }

    /**
     * 处理任务（带超时控制）
     *
     * @param task        任务
     * @param taskWrapper 任务包装对象
     * @param isRetryTask 是否是重试任务
     */
    private void processTaskWithTimeout(@NotNull DocumentationTask task,
                                        @NotNull TaskDispatcher.TaskWrapper taskWrapper,
                                        boolean isRetryTask) {

        // 设置任务状态
        task.setStatus(DocumentationTask.TaskStatus.PROCESSING);
        inflightCount.incrementAndGet();

        // 使用可控执行器实现超时控制
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return executeTask(task);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, providerManager.getRequestExecutor());

        try {
            // 设置超时时间（默认 10 秒）
            String documentation = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // 已跳过的任务不再走成功/失败流程
            if (task.getStatus() == DocumentationTask.TaskStatus.SKIPPED) {
                return;
            }

            // 任务成功完成
            handleTaskSuccess(task, documentation, isRetryTask);

        } catch (TimeoutException e) {
            // 超时处理
            future.cancel(true);
            log.debug("任务执行超时（{}秒）: {}", DEFAULT_TIMEOUT_SECONDS, task.getFilePath());
            handleTaskFailure(task, taskWrapper, "任务执行超时（" + DEFAULT_TIMEOUT_SECONDS + "秒）", isRetryTask);

        } catch (Exception e) {
            // 其他异常处理
            Throwable cause = e.getCause();
            if (cause instanceof AIServiceException) {
                handleAIServiceException(task, taskWrapper, (AIServiceException) cause, isRetryTask);
            } else {
                handleTaskFailure(task, taskWrapper, e.getMessage(), isRetryTask);
            }
        } finally {
            inflightCount.decrementAndGet();
        }
    }

    /**
     * 执行任务（生成文档）
     *
     * @param task 任务
     * @return 生成的文档内容
     * @throws AIServiceException AI 服务异常
     */
    @Nullable
    private String executeTask(@NotNull DocumentationTask task) throws AIServiceException {
        // 输出任务开始信息
        outputTaskStartInfo(task);

        // 检查是否跳过
        if (shouldSkip(task)) {
            task.setStatus(DocumentationTask.TaskStatus.SKIPPED);
            stats.incrementSkipped();
            updateProgress(task);
            AIConsoleLoggerUtil.printWarning(project, "⏭ 任务已跳过（已有文档）");
            AIConsoleLoggerUtil.print(project, "");
            return null;
        }

        // 输出代码位置信息（详细日志模式）
        boolean verboseLogging = isVerbose();
        if (verboseLogging) {
            outputCodeLocation(task);
        }

        // 构建 AI 请求
        AIChatRequest request = AIRequestComposer.compose(settings, task);

        // 生成文档内容
        AIResponseListener listener = verboseLogging ? new JavadocAIResponseListener(project) : null;
        String documentation = aiService.generateContent(project, request, provider, listener);

        if (documentation.trim().isEmpty()) {
            throw new AIServiceException("生成的文档为空", AIServiceException.ErrorCode.INVALID_RESPONSE);
        }

        // 插入文档（需要在 EDT 中执行）
        ApplicationManager.getApplication().invokeAndWait(() -> {
            documentationInserter.insertDocumentation(task, documentation, verboseLogging);
        });

        return documentation;
    }

    /**
     * 输出任务开始信息
     *
     * @param task 任务
     */
    private void outputTaskStartInfo(@NotNull DocumentationTask task) {
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                PsiElement element = task.getElement();

                VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
                if (virtualFile == null) {
                    return;
                }

                // 计算当前任务编号
                int currentTaskNum = progressManager.getCompletedCount() +
                                     progressManager.getFailedCount() +
                                     progressManager.getSkippedCount() + 1;

                String taskInfo = String.format("========== 任务 %d: %s %s ==========",
                                                currentTaskNum,
                                                task.getType().name(),
                                                task.getFilePath());
                AIConsoleLoggerUtil.printHyperlinkWithTimestamp(project, taskInfo, virtualFile, 0);
                AIConsoleLoggerUtil.print(project, "");
            } catch (Exception e) {
                // 忽略异常，避免影响主功能
            }
        });
    }

    /**
     * 输出代码位置信息
     *
     * @param task 任务
     */
    private void outputCodeLocation(@NotNull DocumentationTask task) {
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                PsiElement element = task.getElement();

                VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
                if (virtualFile == null) {
                    return;
                }

                Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
                if (document == null) {
                    return;
                }

                int startOffset = element.getTextRange().getStartOffset();
                int lineNumber = document.getLineNumber(startOffset);
                String fileName = virtualFile.getName();
                String locationMessage = String.format("处理代码位置: %s:%d", fileName, lineNumber + 1);
                AIConsoleLoggerUtil.printHyperlink(project, locationMessage, virtualFile, lineNumber);
            } catch (Exception e) {
                // 忽略异常，避免影响主功能
            }
        });
    }

    /**
     * 更新进度
     * <p>
     * 进度更新必须在 EDT 中执行，因为 ProgressIndicator 的更新需要在 UI 线程中进行。
     *
     * @param task 任务
     */
    private void updateProgress(@NotNull DocumentationTask task) {
        String providerName = provider.providerType.getDisplayName();
        int currentIndex = progressManager.getCompletedCount() +
                           progressManager.getFailedCount() +
                           progressManager.getSkippedCount();

        // 进度更新必须在 EDT 中执行
        ApplicationManager.getApplication().invokeLater(() -> {
            progressManager.updateProgress(currentIndex, task, providerName);
        });
    }

    /**
     * 处理任务成功
     *
     * @param task          任务
     * @param documentation 生成的文档
     * @param isRetryTask   是否是重试任务
     */
    private void handleTaskSuccess(@NotNull DocumentationTask task,
                                   @NotNull String documentation,
                                   boolean isRetryTask) {
        task.setStatus(DocumentationTask.TaskStatus.COMPLETED);
        task.setResult(documentation);
        stats.incrementCompleted();
        updateProgress(task);

        if (isRetryTask) {
            AIConsoleLoggerUtil.printSuccess(project, "✓ 任务完成（重试成功）");
        } else {
            AIConsoleLoggerUtil.printSuccess(project, "✓ 任务完成");
        }
        AIConsoleLoggerUtil.print(project, "");
    }

    /**
     * 处理 AI 服务异常
     *
     * @param task        任务
     * @param taskWrapper 任务包装对象
     * @param e           AI 服务异常
     * @param isRetryTask 是否是重试任务
     */
    private void handleAIServiceException(@NotNull DocumentationTask task,
                                          @NotNull TaskDispatcher.TaskWrapper taskWrapper,
                                          @NotNull AIServiceException e,
                                          boolean isRetryTask) {
        AIServiceException.ErrorCode errorCode = e.getErrorCode();
        String errorMessage = AIServiceException.build(e);

        if (errorCode == AIServiceException.ErrorCode.RATE_LIMIT) {
            // 429 错误：标记服务商不可用，销毁所有线程
            log.debug("服务商 {} 出现限流错误（429），销毁所有线程", provider.providerType.getDisplayName());
            providerManager.markProviderRateLimited(provider);
            // 将任务重新分配给其他可用服务商（如果存在）
            if (providerManager.getAvailableProviderCount() > 0) {
                RetryableTask retryableTask = taskWrapper.retryableTask();
                if (retryableTask == null) {
                    retryableTask = new RetryableTask(task);
                }
                retryableTask.setLastError(errorMessage);
                taskDispatcher.addToRetryQueue(retryableTask);
                AIConsoleLoggerUtil.printWarning(project,
                                                 String.format("⚠ 服务商限流，任务转交其他可用服务商: %s", task.getFilePath()));
            } else {
                stats.incrementFailed();
                task.setStatus(DocumentationTask.TaskStatus.FAILED);
                task.setErrorMessage(errorMessage);
                updateProgress(task);
                AIConsoleLoggerUtil.printError(project,
                                               "✗ 所有服务商不可用，任务失败: " + task.getFilePath());
            }

        } else {
            // 其他错误：放入重试队列
            handleTaskFailure(task, taskWrapper, errorMessage, isRetryTask);
        }
    }

    /**
     * 处理任务失败
     *
     * @param task         任务
     * @param taskWrapper  任务包装对象
     * @param errorMessage 错误信息
     * @param isRetryTask  是否是重试任务
     */
    private void handleTaskFailure(@NotNull DocumentationTask task,
                                   @NotNull TaskDispatcher.TaskWrapper taskWrapper,
                                   @Nullable String errorMessage,
                                   boolean isRetryTask) {
        RetryableTask retryableTask = taskWrapper.retryableTask();

        if (retryableTask == null) {
            retryableTask = new RetryableTask(task);
        }

        if (retryableTask.isMaxRetriesExceeded()) {
            task.setStatus(DocumentationTask.TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            stats.incrementFailed();
            updateProgress(task);
            AIConsoleLoggerUtil.printError(project,
                                           String.format("✗ 任务失败（重试 %d 次后仍失败）: %s",
                                                         retryableTask.getRetryCount(), errorMessage));
            AIConsoleLoggerUtil.print(project, "");
            return;
        }

        retryableTask.setLastError(errorMessage);
        taskDispatcher.addToRetryQueue(retryableTask);
        AIConsoleLoggerUtil.printWarning(project,
                                         String.format("⚠ 任务失败，将重试（第 %d 次）: %s",
                                                       retryableTask.getRetryCount(), errorMessage));
        AIConsoleLoggerUtil.print(project, "");
    }

    /**
     * 检查是否应该跳过任务
     *
     * @param task 任务
     * @return 如果应该跳过返回 true
     */
    private boolean shouldSkip(@NotNull DocumentationTask task) {
        if (settings.overrideExisting) {
            return false;
        }

        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            var element = task.getElement();
            if (element instanceof com.intellij.psi.PsiDocCommentOwner docOwner) {
                return docOwner.getDocComment() != null;
            }
            return false;
        });
    }

    /**
     * 检查是否启用详细日志
     *
     * @return 如果启用详细日志返回 true
     */
    private boolean isVerbose() {
        return AIProviderSettings.getInstance().verboseLogging;
    }
}
