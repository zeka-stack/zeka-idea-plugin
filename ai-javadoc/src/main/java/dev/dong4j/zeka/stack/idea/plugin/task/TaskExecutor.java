package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIRequestComposer;
import dev.dong4j.zeka.stack.idea.plugin.ai.JavaDocAIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.console.JavaDocConsoleView;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务执行器
 *
 * <p>负责执行文档生成任务队列，处理多个文件的批量生成。
 * 作为文档生成流程的核心组件，协调 AI 服务调用、文档插入和进度管理。
 *
 * <p>核心功能：
 * <ul>
 *   <li>批量处理文档生成任务</li>
 *   <li>与 AI 服务交互生成文档内容</li>
 *   <li>将生成的文档插入到源代码中</li>
 *   <li>实时显示处理进度和统计信息</li>
 *   <li>处理异常和错误情况</li>
 *   <li>支持用户取消操作</li>
 * </ul>
 *
 * <p>执行流程：
 * <ol>
 *   <li>初始化 AI 服务提供商</li>
 *   <li>遍历任务列表逐个处理</li>
 *   <li>更新进度指示器</li>
 *   <li>调用 AI 服务生成文档</li>
 *   <li>将文档插入到源代码</li>
 *   <li>收集处理统计信息</li>
 * </ol>
 *
 * <p>线程安全：
 * <ul>
 *   <li>使用 AtomicInteger 确保计数器线程安全</li>
 *   <li>PSI 访问在适当的线程上下文中执行</li>
 *   <li>UI 更新通过 invokeLater 调度</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@SuppressWarnings("D")
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
    /** AI 响应监听器 */
    private final JavaDocAIResponseListener responseListener;
    /** 进度管理器，统一管理单线程和多线程的进度更新 */
    private ProgressManager progressManager;

    /**
     * 提供商统计信息
     * <p>
     * 注意：性能模式下的多提供商支持需要进一步实现
     */
    public static class ProviderStatistics {
        /** 服务提供商名称 */
        @Getter
        private final String providerName;
        /** 完成的任务数量计数器，用于记录已成功完成的任务数 */
        private final AtomicInteger completedCount = new AtomicInteger(0);
        /** 失败计数器，用于记录失败操作的次数 */
        private final AtomicInteger failedCount = new AtomicInteger(0);
        /** 被跳过的记录数量 */
        private final AtomicInteger skippedCount = new AtomicInteger(0);
        /** 开始时间戳，表示操作或任务开始的时刻 */
        private final long startTime;
        /** 结束时间，表示某个操作或任务的结束时间戳 */
        private long endTime;

        /**
         * 初始化 ProviderStatistics 实例
         * <p>
         * 通过传入的 providerName 初始化统计信息，并记录开始时间
         *
         * @param providerName 提供商名称
         */
        public ProviderStatistics(String providerName) {
            this.providerName = providerName;
            this.startTime = System.currentTimeMillis();
        }

        /**
         * 获取已完成任务的数量
         * <p>
         * 返回当前已完成任务的计数值
         *
         * @return 已完成任务的数量
         */
        public int getCompletedCount() {
            return completedCount.get();
        }

        /**
         * 获取失败操作的计数
         * <p>
         * 返回当前记录的失败操作次数。
         *
         * @return 失败操作的计数
         */
        public int getFailedCount() {
            return failedCount.get();
        }

        /**
         * 获取已跳过的项目数量
         * <p>
         * 返回当前已跳过的项目计数。
         *
         * @return 已跳过的项目数量
         */
        public int getSkippedCount() {
            return skippedCount.get();
        }

        /**
         * 获取总任务数
         * <p>
         * 返回已完成、失败和跳过任务数的总和
         *
         * @return 总任务数
         */
        public int getTotalCount() {
            return completedCount.get() + failedCount.get() + skippedCount.get();
        }

        /**
         * 获取操作的持续时间
         * <p>
         * 计算并返回从开始时间到结束时间的时间差，单位为毫秒
         *
         * @return 操作的持续时间（毫秒）
         */
        public long getDuration() {
            return endTime - startTime;
        }

        /**
         * 增加已完成任务的计数
         * <p>
         * 调用计数器的 incrementAndGet 方法，将已完成任务的数量增加 1。
         */
        public void incrementCompleted() {
            completedCount.incrementAndGet();
        }

        /**
         * 增加失败计数器的值
         * <p>
         * 该方法用于将失败计数器的值增加1，通常用于记录系统或操作失败的次数。
         */
        public void incrementFailed() {
            failedCount.incrementAndGet();
        }

        /**
         * 增加跳过计数
         * <p>
         * 用于增加跳过操作的计数器值。
         */
        public void incrementSkipped() {
            skippedCount.incrementAndGet();
        }

        /**
         * 结束计时，记录当前时间作为结束时间
         * <p>
         * 该方法用于标记操作或任务的结束时间，将当前系统时间赋值给 endTime 字段
         */
        public void finish() {
            this.endTime = System.currentTimeMillis();
        }

        /**
         * 返回该对象的字符串表示形式，包含执行状态的详细信息。
         * <p>
         * 该方法按照指定格式拼接字符串，展示完成数、失败数、跳过数、总计数以及耗时信息。
         *
         * @return 对象的字符串表示，格式为：providerName: 完成=..., 失败=..., 跳过=..., 总计=..., 耗时=...
         */
        @Override
        public String toString() {
            return String.format("%s: 完成=%d, 失败=%d, 跳过=%d, 总计=%d, 耗时=%.1fs",
                                 providerName, getCompletedCount(), getFailedCount(),
                                 getSkippedCount(), getTotalCount(), getDuration() / 1000.0);
        }
    }

    /**
     * 获取元素的完整类路径（import 路径）
     * <p>
     * 根据元素类型返回对应的完整类路径，用于在进度显示中展示。
     * <ul>
     *   <li>如果是类/接口/枚举：直接返回类的全路径（如 com.example.MyClass）</li>
     *   <li>如果是方法/字段等在类内部的元素：使用点号拼接（如 com.example.MyClass.methodName）</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 完整类路径，如果无法获取则返回元素名称
     */
    @NotNull
    private static String getElementQualifiedName(@NotNull PsiElement element) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            // 1. 如果是类/接口/枚举，直接返回类的全路径
            if (element instanceof PsiClass psiClass) {
                String qualifiedName = psiClass.getQualifiedName();
                return qualifiedName != null ? qualifiedName : psiClass.getName();
            }

            // 2. 如果是方法，使用点号拼接：类全路径.方法名
            if (element instanceof PsiMethod method) {
                PsiClass containingClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                final String className = buildClassName(containingClass, method.getName());
                return Objects.requireNonNullElseGet(className, method::getName);
            }

            // 3. 如果是字段，使用点号拼接：类全路径.字段名
            if (element instanceof PsiField field) {
                PsiClass containingClass = PsiTreeUtil.getParentOfType(field, PsiClass.class);
                final String className = buildClassName(containingClass, field.getName());
                return Objects.requireNonNullElseGet(className, field::getName);
            }

            // 4. 如果是文件，尝试获取文件中的第一个类
            if (element instanceof PsiFile) {
                if (element instanceof PsiJavaFile javaFile) {
                    PsiClass[] classes = javaFile.getClasses();
                    if (classes.length > 0) {
                        String qualifiedName = classes[0].getQualifiedName();
                        return qualifiedName != null ? qualifiedName : classes[0].getName();
                    }
                }
                return element.getContainingFile().getName();
            }

            // 5. 其他情况，尝试查找包含的类
            PsiClass containingClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (containingClass != null) {
                String className = containingClass.getQualifiedName();
                if (className != null) {
                    return className + "." + element.getClass().getSimpleName();
                }
            }

            return element.getClass().getSimpleName();
        });
    }

    @Nullable
    private static String buildClassName(PsiClass containingClass, String element) {
        if (containingClass != null) {
            String className = containingClass.getQualifiedName();
            if (className != null) {
                return className + "." + element;
            }
            // 如果类没有全路径，使用类名
            String classSimpleName = containingClass.getName();
            if (classSimpleName != null) {
                return classSimpleName + "." + element;
            }
        }
        return null;
    }

    /**
     * 获取任务类型的表情符号
     *
     * @param type 任务类型
     * @return 表情符号
     */
    @NotNull
    private static String getTaskTypeEmoji(@NotNull DocumentationTask.TaskType type) {
        switch (type) {
            case CLASS:
                return "📦";
            case METHOD:
                return "⚙️";
            case TEST_METHOD:
                return "🧪";
            case FIELD:
                return "📝";
            case INTERFACE:
                return "🔌";
            case ENUM:
                return "🔢";
            default:
                return "📄";
        }
    }

    /**
     * 进度管理器
     * <p>
     * 统一管理单线程和多线程模式下的进度更新和统计信息。
     * 提供统一的接口来更新进度指示器，支持单线程和多线程两种模式。
     *
     * <p>功能：
     * <ul>
     *   <li>单线程模式：使用内部计数器跟踪统计信息</li>
     *   <li>多线程模式：汇总所有提供商的统计信息，为每个提供商创建子进度指示器</li>
     *   <li>统一更新进度指示器（进度条、文本、统计信息）</li>
     *   <li>线程安全的统计信息访问</li>
     * </ul>
     */
    private static class ProgressManager {
        /** 进度指示器 */
        private final ProgressIndicator indicator;
        /** 总任务数 */
        private final int totalTasks;
        /** 是否为多线程模式 */
        private final boolean isParallel;
        /** 单线程模式下的完成计数器 */
        private final AtomicInteger completedCount = new AtomicInteger(0);
        /** 单线程模式下的失败计数器 */
        private final AtomicInteger failedCount = new AtomicInteger(0);
        /** 单线程模式下的跳过计数器 */
        private final AtomicInteger skippedCount = new AtomicInteger(0);
        /** 多线程模式下的提供商统计信息映射（可选） */
        private final Map<String, ProviderStatistics> providerStats;
        /** 多线程模式下的提供商进度指示器映射 */
        private final Map<String, ProgressIndicator> providerIndicators = new ConcurrentHashMap<>();

        /**
         * 创建单线程模式的进度管理器
         *
         * @param indicator  进度指示器
         * @param totalTasks 总任务数
         */
        public ProgressManager(@NotNull ProgressIndicator indicator, int totalTasks) {
            this.indicator = indicator;
            this.totalTasks = totalTasks;
            this.isParallel = false;
            this.providerStats = null;
        }

        /**
         * 创建多线程模式的进度管理器
         *
         * @param indicator     进度指示器
         * @param totalTasks    总任务数
         * @param providerStats 提供商统计信息映射
         */
        public ProgressManager(@NotNull ProgressIndicator indicator,
                               int totalTasks,
                               @NotNull Map<String, ProviderStatistics> providerStats) {
            this.indicator = indicator;
            this.totalTasks = totalTasks;
            this.isParallel = true;
            this.providerStats = providerStats;
        }

        /**
         * 判断是否为多线程模式
         *
         * @return 如果是多线程模式返回 true
         */
        public boolean isParallelMode() {
            return isParallel;
        }

        /**
         * 获取当前已完成的任务数
         *
         * @return 已完成任务数
         */
        public int getCompletedCount() {
            if (isParallelMode() && providerStats != null) {
                return providerStats.values().stream()
                    .mapToInt(ProviderStatistics::getCompletedCount)
                    .sum();
            }
            return completedCount.get();
        }

        /**
         * 获取当前失败的任务数
         *
         * @return 失败任务数
         */
        public int getFailedCount() {
            if (isParallelMode() && providerStats != null) {
                return providerStats.values().stream()
                    .mapToInt(ProviderStatistics::getFailedCount)
                    .sum();
            }
            return failedCount.get();
        }

        /**
         * 获取当前跳过的任务数
         *
         * @return 跳过任务数
         */
        public int getSkippedCount() {
            if (isParallelMode() && providerStats != null) {
                return providerStats.values().stream()
                    .mapToInt(ProviderStatistics::getSkippedCount)
                    .sum();
            }
            return skippedCount.get();
        }

        /**
         * 增加已完成任务计数（单线程模式）
         */
        public void incrementCompleted() {
            if (!isParallelMode()) {
                completedCount.incrementAndGet();
            }
        }

        /**
         * 增加失败任务计数（单线程模式）
         */
        public void incrementFailed() {
            if (!isParallelMode()) {
                failedCount.incrementAndGet();
            }
        }

        /**
         * 增加跳过任务计数（单线程模式）
         */
        public void incrementSkipped() {
            if (!isParallelMode()) {
                skippedCount.incrementAndGet();
            }
        }

        /**
         * 更新进度指示器
         * <p>
         * 根据当前处理的任务索引和统计信息更新进度条、主文本和副文本。
         *
         * @param currentIndex 当前处理的任务索引（从0开始）
         * @param currentTask  当前处理的任务（可选，用于显示文件路径）
         * @param providerName 当前处理任务的提供商名称（可选，多线程模式使用）
         */
        public void updateProgress(int currentIndex, DocumentationTask currentTask, String providerName) {
            int processed = getCompletedCount() + getFailedCount() + getSkippedCount();
            double fraction = totalTasks > 0 ? (double) processed / totalTasks : 0.0;

            // 更新进度条
            indicator.setFraction(fraction);

            // 获取类的全路径和表情符号
            String qualifiedName = "";
            String emoji = "";
            if (currentTask != null) {
                qualifiedName = getElementQualifiedName(currentTask.getElement());
                emoji = getTaskTypeEmoji(currentTask.getType());
            }

            // 更新主文本：显示处理进度
            // 单线程和多线程模式都显示提供商信息
            if (providerName != null && !providerName.isEmpty()) {
                // 显示提供商名称和当前处理的元素详细信息
                if (!qualifiedName.isEmpty()) {
                    indicator.setText(String.format("[%s] %s %s (%d/%d)",
                                                    providerName, emoji, qualifiedName,
                                                    isParallelMode() ? processed : currentIndex + 1, totalTasks));
                } else if (currentTask != null) {
                    // 如果无法获取完整路径，回退到文件路径
                    indicator.setText(String.format("[%s] %s %s (%d/%d)",
                                                    providerName, emoji, currentTask.getFilePath(),
                                                    isParallelMode() ? processed : currentIndex + 1, totalTasks));
                } else {
                    indicator.setText(String.format("[%s] 已处理 %d/%d 个任务",
                                                    providerName,
                                                    isParallelMode() ? processed : currentIndex + 1, totalTasks));
                }
            } else {
                // 如果没有提供商信息，显示基本进度信息
                if (currentTask != null && !qualifiedName.isEmpty()) {
                    indicator.setText(String.format("%s %s (%d/%d)",
                                                    emoji, qualifiedName,
                                                    isParallelMode() ? processed : currentIndex + 1, totalTasks));
                } else if (currentTask != null) {
                    indicator.setText(String.format("%s %s (%d/%d)",
                                                    emoji, currentTask.getFilePath(),
                                                    isParallelMode() ? processed : currentIndex + 1, totalTasks));
                } else {
                    indicator.setText(String.format("正在处理 (%d/%d)",
                                                    isParallelMode() ? processed : currentIndex + 1, totalTasks));
                }
            }

            // 更新副文本：显示统计信息
            updateStatisticsText();
        }

        /**
         * 更新进度指示器（兼容旧版本调用）
         *
         * @param currentIndex 当前处理的任务索引（从0开始）
         * @param currentTask  当前处理的任务（可选，用于显示文件路径）
         */
        public void updateProgress(int currentIndex, DocumentationTask currentTask) {
            updateProgress(currentIndex, currentTask, null);
        }

        /**
         * 更新统计信息文本
         * <p>
         * 在多线程模式下，还会显示各提供商的处理情况。
         */
        public void updateStatisticsText() {
            if (isParallelMode() && providerStats != null) {
                // 多线程模式：显示总体统计和提供商详情
                StringBuilder statsText = new StringBuilder();
                statsText.append(String.format("完成: %d, 失败: %d, 跳过: %d",
                                               getCompletedCount(), getFailedCount(), getSkippedCount()));

                // 添加各提供商的详细统计信息
                if (!providerStats.isEmpty()) {
                    statsText.append(" | ");
                    List<String> providerInfo = new ArrayList<>();
                    for (ProviderStatistics stats : providerStats.values()) {
                        int completed = stats.getCompletedCount();
                        int failed = stats.getFailedCount();
                        int skipped = stats.getSkippedCount();
                        int total = stats.getTotalCount();
                        if (total > 0) {
                            // 显示提供商名称和详细统计：完成/失败/跳过
                            providerInfo.add(String.format("%s: 完成%d 失败%d 跳过%d",
                                                           stats.getProviderName(), completed, failed, skipped));
                        }
                    }
                    if (!providerInfo.isEmpty()) {
                        statsText.append(String.join(" | ", providerInfo));
                    }
                }

                indicator.setText2(statsText.toString());
            } else {
                // 单线程模式：只显示总体统计
                indicator.setText2(String.format("完成: %d, 失败: %d, 跳过: %d",
                                                 getCompletedCount(), getFailedCount(), getSkippedCount()));
            }
        }

        /**
         * 完成进度更新
         * <p>
         * 将进度设置为100%，并显示完成消息。
         */
        public void finish() {
            indicator.setFraction(1.0);
            indicator.setText(JavaDocBundle.message("task.progress.completed"));
        }

        /**
         * 获取统计信息
         *
         * @return 任务统计信息
         */
        public TaskStatistics getStatistics() {
            return new TaskStatistics(
                getCompletedCount(),
                getFailedCount(),
                getSkippedCount()
            );
        }
    }

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
        this.responseListener = new JavaDocAIResponseListener(project);
    }

    /**
     * 检查 AI 服务是否可用
     *
     * @return 如果 AI 服务可用返回 true，否则返回 false
     */
    public boolean isServiceAvailable() {
        try {
            return aiService != null && settings.providerConfig != null && settings.providerConfig.configurationVerified;
        } catch (Exception e) {
            return false;
        }
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
     * @param tasks 任务列表
     */
    public boolean processTasks(@NotNull List<DocumentationTask> tasks) {
        if (tasks.isEmpty()) {
            return false;
        }

        indicator.setIndeterminate(false);
        int totalTasks = tasks.size();

        log.info("开始处理 {} 个文档生成任务", totalTasks);

        // 检查是否启用性能模式且任务数量大于5个
        if (AIProviderSettings.getInstance().runtimeSettings.performanceMode && totalTasks > 5) {
            return processTasksInParallel(tasks);
        } else {
            // 初始化单线程模式的进度管理器
            progressManager = new ProgressManager(indicator, totalTasks);
            return processTasksSequentially(tasks);
        }
    }

    /**
     * 并行处理任务（性能模式）
     * <p>
     * 该方法在性能模式下，利用多个AI服务提供商并行处理任务列表。如果无可用提供商，则回退到顺序处理。
     * <p>
     * 线程分配策略：
     * <ul>
     *   <li>根据任务数和提供商数量动态计算合适的线程数</li>
     *   <li>每个提供商可以有多个线程并发处理任务</li>
     *   <li>并发度 = 总线程数 / 提供商数</li>
     *   <li>所有线程共享任务队列，从队列中获取任务处理</li>
     * </ul>
     *
     * @param tasks 任务列表，包含需要处理的文档任务
     * @return 处理是否成功
     */
    private boolean processTasksInParallel(@NotNull List<DocumentationTask> tasks) {
        final List<AIProviderConfig> aiProviderTypes = getAiProviderTypes();

        if (aiProviderTypes.isEmpty()) {
            log.warn("性能模式启用但无可用提供商");
            return false;
        }

        int providerCount = aiProviderTypes.size();
        final int totalThreads = getTotalThreads(tasks, providerCount);

        // 创建动态大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

        // 为每个提供商创建统计对象
        Map<String, ProviderStatistics> providerStats = new ConcurrentHashMap<>();

        // 初始化多线程模式的进度管理器
        progressManager = new ProgressManager(indicator, tasks.size(), providerStats);

        try {
            // 创建所有线程任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            AtomicInteger taskIndex = new AtomicInteger(0);

            // 计算余数，用于分配额外的线程
            int remainder = totalThreads % providerCount;
            int baseThreadsPerProvider = totalThreads / providerCount;

            // 为每个提供商创建多个线程
            for (int providerIdx = 0; providerIdx < aiProviderTypes.size(); providerIdx++) {
                AIProviderConfig provider = aiProviderTypes.get(providerIdx);
                String providerName = provider.providerType.getDisplayName();
                ProviderStatistics stats = new ProviderStatistics(providerName);
                providerStats.put(providerName, stats);

                // 计算当前提供商应该创建的线程数
                // 前 remainder 个提供商多分配1个线程，以处理余数
                int currentProviderThreads = baseThreadsPerProvider + (providerIdx < remainder ? 1 : 0);

                log.debug("提供商 {} 分配 {} 个线程", providerName, currentProviderThreads);

                // 为当前提供商创建线程
                for (int i = 0; i < currentProviderThreads; i++) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        processTasksWithProvider(tasks, provider, taskIndex, stats);
                    }, executor);
                    futures.add(future);
                }
            }

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 完成所有统计
            providerStats.values().forEach(ProviderStatistics::finish);

            // 使用进度管理器完成进度更新
            progressManager.finish();

            // 显示每个提供商的统计信息（如果启用）
            SettingsState settings = SettingsState.getInstance();
            if (AIProviderSettings.getInstance().showProviderStatistics) {
                showProviderStatistics(providerStats);
            }

            // 使用进度管理器获取统计信息
            TaskStatistics statistics = progressManager.getStatistics();
            log.info("并行任务处理完成。成功: {}, 失败: {}, 跳过: {}",
                     statistics.completed(), statistics.failed(), statistics.skipped());

            // Console 日志：任务完成统计
            JavaDocConsoleView.printWithTimestamp(project, "========== 生成完成 ==========");
            JavaDocConsoleView.printSuccess(project, String.format("成功: %d | 失败: %d | 跳过: %d",
                                                                   statistics.completed(), statistics.failed(), statistics.skipped()));

            return true;
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 计算总线程数, 根据任务数量和提供商数量动态调整线程分配
     * <p>
     * 根据任务总数和提供商数量, 计算每个提供商应分配的线程数, 并最终确定总线程数.
     * 线程数会根据任务数量进行限制, 确保不超过任务总数.
     *
     * @param tasks         任务列表, 用于确定任务总数
     * @param providerCount 提供商数量, 用于计算每个提供商的线程分配
     * @return 总线程数, 用于并行处理任务
     */
    private static int getTotalThreads(@NotNull List<DocumentationTask> tasks, int providerCount) {
        int taskCount = tasks.size();

        // 计算合适的线程数
        // 策略：根据任务数和提供商数动态计算
        // - 如果任务数较少（<=10），每个提供商1个线程
        // - 如果任务数中等（10-50），每个提供商2个线程
        // - 如果任务数较多（>50），每个提供商3-4个线程
        int threadsPerProvider;
        if (taskCount <= 10) {
            threadsPerProvider = 1;
        } else if (taskCount <= 50) {
            threadsPerProvider = 2;
        } else {
            // 任务数较多时，每个提供商最多4个线程
            threadsPerProvider = Math.min(4, Math.max(2, taskCount / (providerCount * 2)));
        }

        // 计算总线程数
        int totalThreads = providerCount * threadsPerProvider;

        // 如果总线程数超过任务数，限制总线程数为任务数
        // 然后重新计算每个提供商的线程数（平均分配，但每个提供商至少1个线程）
        if (totalThreads > taskCount) {
            totalThreads = taskCount;
            // 平均分配线程，每个提供商至少1个线程
            threadsPerProvider = Math.max(1, totalThreads / providerCount);
            // 重新计算总线程数（可能因为取整而略小于 taskCount）
            totalThreads = providerCount * threadsPerProvider;
        }

        // 计算平均并发度
        double avgConcurrency = (double) totalThreads / providerCount;
        log.info("性能模式：使用 {} 个提供商，创建 {} 个线程（平均每个提供商 {} 个线程，平均并发度 {}）并行处理 {} 个任务",
                 providerCount, totalThreads, String.format("%.1f", avgConcurrency),
                 String.format("%.1f", avgConcurrency), taskCount);
        return totalThreads;
    }

    /**
     * 使用指定提供商处理任务列表
     * <p>
     * 遍历任务列表，依次使用指定的AI服务提供商处理每个任务，并更新处理进度和统计信息。
     *
     * @param tasks     任务列表，包含需要处理的文档任务
     * @param provider  AI服务提供商，用于执行具体的任务处理逻辑
     * @param taskIndex 用于记录当前处理任务索引的原子整数，确保线程安全
     * @param stats     统计信息对象，用于记录处理过程中的完成、失败和跳过任务数量
     */
    private void processTasksWithProvider(@NotNull List<DocumentationTask> tasks,
                                          @NotNull AIProviderConfig provider,
                                          @NotNull AtomicInteger taskIndex,
                                          @NotNull ProviderStatistics stats) {
        int totalTasks = tasks.size();
        String providerName = provider.providerType.getDisplayName();

        while (taskIndex.get() < totalTasks && !indicator.isCanceled()) {
            int currentIndex = taskIndex.getAndIncrement();
            if (currentIndex >= totalTasks) {
                break;
            }

            DocumentationTask task = tasks.get(currentIndex);

            // 任务开始处理时，更新进度显示当前提供商正在处理
            SwingUtilities.invokeLater(() -> {
                if (progressManager != null) {
                    progressManager.updateProgress(currentIndex, task, providerName);
                }
            });

            // 处理任务（会更新统计信息）
            processTask(task, provider, stats);

            // 任务处理完成后，使用进度管理器更新进度（需要在 EDT 中执行）
            SwingUtilities.invokeLater(() -> {
                if (progressManager != null) {
                    progressManager.updateProgress(currentIndex, task, providerName);
                }
            });
        }
    }

    /**
     * 顺序处理任务列表
     * <p>
     * 按顺序处理给定的任务列表，更新进度指示器并记录处理结果统计信息。
     *
     * @param tasks 任务列表
     * @return 始终返回 true，表示处理完成
     */
    private boolean processTasksSequentially(@NotNull List<DocumentationTask> tasks) {
        if (aiService == null) {
            return false;
        }

        int totalTasks = tasks.size();

        // Console 日志：任务开始
        JavaDocConsoleView.printWithTimestamp(project, String.format("========== 开始生成文档 任务总数: %s ==========", totalTasks));
        JavaDocConsoleView.print(project, "");

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
        JavaDocConsoleView.printWithTimestamp(project, "========== 生成完成 ==========");
        JavaDocConsoleView.printSuccess(project, String.format("成功: %d | 失败: %d | 跳过: %d",
                                                               statistics.completed(), statistics.failed(), statistics.skipped()));

        return true;
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

    /**
     * 显示提供商的统计信息，包括HTML格式的表格和日志信息。
     * <p>
     * 该方法接收一个包含提供商统计信息的Map，生成HTML格式的统计表格，并在日志中记录详细信息。
     * 同时，会弹出一个对话框展示统计结果。
     *
     * @param providerStats 包含提供商统计信息的Map，键为服务商名称，值为对应的统计对象
     */
    private void showProviderStatistics(@NotNull Map<String, ProviderStatistics> providerStats) {
        // 创建HTML格式的统计信息
        StringBuilder htmlContent = new StringBuilder();
        // formatter:off
        htmlContent.append("<html><head><style>");
        htmlContent.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 10px; font-size: 12px; }");
        htmlContent.append("h2 { color: #2E7D32; margin-bottom: 15px; font-size: 16px; }");
        htmlContent.append("h3 { color: #1976D2; margin-bottom: 10px; font-size: 14px; }");
        htmlContent.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("th { background-color:rgb(122, 127, 131); color: white; padding: 8px; text-align: center; font-weight: bold; font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("td { padding: 8px; text-align: center; font-size: 11px; border: 1px solid #ddd; }");
        htmlContent.append("td.provider-name { text-align: left; }");
        htmlContent.append("tr:nth-child(even) { background-color: #f8f9fa; }");
        htmlContent.append("tr:hover { background-color: #e3f2fd; }");
        htmlContent.append(".summary-row { background-color:rgb(41, 96, 123); color: white; font-weight: bold; }");
        htmlContent.append(".summary-row td { border: 1px solid #ddd; }");
        htmlContent.append("</style></head><body>");
        // formatter:on
        // 添加标题
        htmlContent.append("<h2>🚀 性能模式处理完成</h2>");

        // 创建提供商统计表格
        htmlContent.append("<table>");
        htmlContent.append("<tr><th>服务商名称</th><th>完成数量</th><th>失败数量</th><th>跳过数量</th><th>耗时</th></tr>");

        int totalCompleted = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        long totalDuration = 0;

        for (ProviderStatistics stats : providerStats.values()) {
            htmlContent.append("<tr>");
            htmlContent.append("<td class='provider-name'>").append(stats.getProviderName()).append("</td>");
            htmlContent.append("<td>").append(stats.getCompletedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getFailedCount()).append("</td>");
            htmlContent.append("<td>").append(stats.getSkippedCount()).append("</td>");
            htmlContent.append("<td>").append(String.format("%.1fs", stats.getDuration() / 1000.0)).append("</td>");
            htmlContent.append("</tr>");

            totalCompleted += stats.getCompletedCount();
            totalFailed += stats.getFailedCount();
            totalSkipped += stats.getSkippedCount();
            totalDuration += stats.getDuration();
        }

        // 添加总体统计行
        htmlContent.append("<tr class='summary-row'>");
        htmlContent.append("<td>📊 总体统计</td>");
        htmlContent.append("<td>").append(totalCompleted).append("</td>");
        htmlContent.append("<td>").append(totalFailed).append("</td>");
        htmlContent.append("<td>").append(totalSkipped).append("</td>");
        htmlContent.append("<td>").append(String.format("%.1fs", totalDuration / 1000.0)).append("</td>");
        htmlContent.append("</tr>");

        htmlContent.append("</table>");
        htmlContent.append("</body></html>");

        // 在日志中记录详细信息
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("性能模式统计详情：\n");
        logMessage.append("各提供商处理统计：\n");

        for (ProviderStatistics stats : providerStats.values()) {
            logMessage.append("• ").append(stats.toString()).append("\n");
        }

        logMessage.append("\n总体统计：\n");
        logMessage.append(String.format("• 总计: %d 个任务\n", totalCompleted + totalFailed + totalSkipped));
        logMessage.append(String.format("• 完成: %d 个\n", totalCompleted));
        logMessage.append(String.format("• 失败: %d 个\n", totalFailed));
        logMessage.append(String.format("• 跳过: %d 个\n", totalSkipped));
        logMessage.append(String.format("• 总耗时: %.1f 秒\n", totalDuration / 1000.0));

        if (totalCompleted > 0) {
            double avgTimePerTask = (double) totalDuration / totalCompleted;
            logMessage.append(String.format("• 平均每任务耗时: %.1f 秒", avgTimePerTask / 1000.0));
        }

        log.info("{}", logMessage);

        // 显示HTML格式的通知给用户
        SwingUtilities.invokeLater(() -> {
            // 创建自定义对话框（非模态）
            javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null, "性能模式处理完成", false);
            dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);

            // 创建HTML内容面板
            javax.swing.JEditorPane editorPane = new javax.swing.JEditorPane();
            editorPane.setContentType("text/html");
            editorPane.setText(htmlContent.toString());
            editorPane.setEditable(false);
            editorPane.setBackground(javax.swing.UIManager.getColor("Panel.background"));

            // 计算动态高度
            int providerCount = providerStats.size();

            // 每行高度约30px，表头高度约35px，总体统计行高度约35px, 在加上标题和一定的冗余量
            int calculatedHeight = 35 + (providerCount * 30) + 35 + 170;

            // 设置最小和最大高度阈值
            int minHeight = 200;  // 最小高度
            int maxHeight = 800;  // 最大高度
            // 应用阈值限制
            int finalHeight = Math.max(minHeight, Math.min(maxHeight, calculatedHeight));

            // 记录高度计算信息
            log.debug("动态高度计算: 提供商数量={}, 计算高度={}, 最终高度={}",
                      providerCount, calculatedHeight, finalHeight);

            // 设置滚动面板
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(editorPane);
            scrollPane.setPreferredSize(new java.awt.Dimension(800, finalHeight));

            // 添加确定按钮
            javax.swing.JButton okButton = new javax.swing.JButton("确定");
            okButton.addActionListener(e -> dialog.dispose());

            javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
            buttonPanel.add(okButton);

            // 设置布局
            dialog.setLayout(new java.awt.BorderLayout());
            dialog.add(scrollPane, java.awt.BorderLayout.CENTER);
            dialog.add(buttonPanel, java.awt.BorderLayout.SOUTH);

            // 设置对话框属性
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
    }

    /**
     * 处理文档生成任务（单线程模式）
     * <p>
     * 该方法负责处理一个文档生成任务，包括设置任务状态、检查是否跳过、生成文档、插入文档以及处理异常。
     * 如果任务被跳过，则更新状态并增加跳过计数。如果生成文档失败或发生异常，则更新任务状态为失败并记录错误信息。
     * 如果任务成功完成，则更新状态为完成并增加完成计数。
     *
     * @param task     要处理的文档生成任务对象
     * @param provider AI 服务提供商配置
     */
    private void processTask(@NotNull DocumentationTask task, @NotNull AIProviderConfig provider) {
        processTask(task, provider, null);
    }

    /**
     * 处理文档生成任务
     * <p>
     * 该方法负责处理一个文档生成任务，包括设置任务状态、检查是否跳过、生成文档、插入文档以及处理异常。
     * 如果任务被跳过，则更新状态并增加跳过计数。如果生成文档失败或发生异常，则更新任务状态为失败并记录错误信息。
     * 如果任务成功完成，则更新状态为完成并增加完成计数。
     *
     * @param task     要处理的文档生成任务对象
     * @param provider AI 服务提供商配置
     * @param stats    提供商统计信息（多线程模式使用，单线程模式为 null）
     */
    private void processTask(@NotNull DocumentationTask task, @NotNull AIProviderConfig provider, ProviderStatistics stats) {
        try {
            task.setStatus(DocumentationTask.TaskStatus.PROCESSING);

            // 计算当前任务编号
            int currentTaskNum;
            if (stats != null) {
                // 多线程模式：使用提供商统计信息
                currentTaskNum = stats.getTotalCount() + 1;
            } else if (progressManager != null) {
                // 单线程模式：使用进度管理器
                currentTaskNum =
                    progressManager.getCompletedCount() + progressManager.getFailedCount() + progressManager.getSkippedCount() + 1;
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
                JavaDocConsoleView.printHyperlinkWithTimestamp(project, taskInfo, virtualFile, 0);
                JavaDocConsoleView.print(project, "");
            }

            if (shouldSkip(task)) {
                task.setStatus(DocumentationTask.TaskStatus.SKIPPED);
                if (stats != null) {
                    stats.incrementSkipped();
                } else if (progressManager != null) {
                    progressManager.incrementSkipped();
                }
                JavaDocConsoleView.printWarning(project, "⏭ 任务已跳过（已有文档）");
                JavaDocConsoleView.print(project, "");
                return;
            }

            AIChatRequest request = AIRequestComposer.compose(settings, task);

            // 输出代码位置信息（可点击链接）
            if (virtualFile != null && AIProviderSettings.getInstance().runtimeSettings.verboseLogging) {
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
                            JavaDocConsoleView.printHyperlink(project, locationMessage, virtualFile, lineNumber);
                        }
                    } catch (Exception e) {
                        // 忽略异常，避免影响主功能
                    }
                });
            }

            // 使用 AIService API 生成内容
            String documentation = aiService.generateContent(project, request, provider, responseListener);

            if (documentation.trim().isEmpty()) {
                task.setStatus(DocumentationTask.TaskStatus.FAILED);
                task.setErrorMessage("生成的文档为空");
                if (stats != null) {
                    stats.incrementFailed();
                } else if (progressManager != null) {
                    progressManager.incrementFailed();
                }
                JavaDocConsoleView.printError(project, "✗ 任务失败: 生成的文档为空");
                JavaDocConsoleView.print(project, "");
                return;
            }

            insertDocumentation(task, documentation);

            task.setStatus(DocumentationTask.TaskStatus.COMPLETED);
            task.setResult(documentation);
            if (stats != null) {
                stats.incrementCompleted();
            } else if (progressManager != null) {
                progressManager.incrementCompleted();
            }

            JavaDocConsoleView.printSuccess(project, "✓ 任务完成");
            JavaDocConsoleView.print(project, "");

        } catch (AIServiceException e) {
            String errorMessage = AIServiceException.build(e);
            log.info("AI 服务调用失败: {} - {}", task, errorMessage, e);
            task.setStatus(DocumentationTask.TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            if (stats != null) {
                stats.incrementFailed();
            } else if (progressManager != null) {
                progressManager.incrementFailed();
            }
            JavaDocConsoleView.printError(project, "✗ 任务失败: " + errorMessage);
            JavaDocConsoleView.print(project, "");
        } catch (Exception e) {
            log.info("处理任务失败: {}", task, e);
            task.setStatus(DocumentationTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            if (stats != null) {
                stats.incrementFailed();
            } else if (progressManager != null) {
                progressManager.incrementFailed();
            }
            JavaDocConsoleView.printError(project, "✗ 任务失败: " + e.getMessage());
            JavaDocConsoleView.print(project, "");
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
     *   <li>元素已有 JavaDoc 注释</li>
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
                PsiDocComment docComment = ((PsiDocCommentOwner) element).getDocComment();
                return docComment != null;
            }
            return false;
        });
    }

    /**
     * 插入文档到代码中
     *
     * <p>将生成的文档注释插入到源代码的适当位置。
     * 如果元素已有注释，会先删除旧注释，再插入新注释。
     * 整个操作在 IntelliJ 的命令和写入操作上下文中执行。
     *
     * <p>插入流程：
     * <ol>
     *   <li>获取元素对应的文档对象</li>
     *   <li>删除已有的旧注释</li>
     *   <li>确定插入位置</li>
     *   <li>格式化并插入新注释</li>
     *   <li>应用代码格式化</li>
     * </ol>
     *
     * <p>线程模型：
     * <ul>
     *   <li>使用 invokeLater 调度到事件调度线程</li>
     *   <li>在命令上下文中执行</li>
     *   <li>在写入操作中执行</li>
     * </ul>
     *
     * @param task          文档生成任务
     * @param documentation 生成的文档内容
     * @see #deleteOldDocComment(PsiElement, Document)
     * @see #getInsertPosition(PsiElement)
     */
    @SuppressWarnings("D")
    private void insertDocumentation(@NotNull DocumentationTask task, @NotNull String documentation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            PsiElement element = task.getElement();
            Document document = FileDocumentManager.getInstance()
                .getDocument(element.getContainingFile().getVirtualFile());

            if (document == null) {
                return;
            }

            PsiDocumentManager.getInstance(project)
                .doPostponedOperationsAndUnblockDocument(document);

            CommandProcessor.getInstance().executeCommand(
                project,
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    try {
                        // 1. 先删除旧注释（如果存在）
                        deleteOldDocComment(element, document);

                        // 2. 提交删除操作
                        PsiDocumentManager.getInstance(project).commitDocument(document);

                        // 3. 获取插入位置（删除后需要重新获取）
                        int startPosition = getInsertPosition(element);
                        int lineNumber = document.getLineNumber(startPosition);
                        int lineStartPosition = document.getLineStartOffset(lineNumber);

                        // 4. 确保文档以 /** 开头
                        String javadoc = documentation.trim();
                        if (!javadoc.startsWith("/**")) {
                            javadoc = "/**\n" + javadoc;
                        }
                        if (!javadoc.endsWith("*/")) {
                            javadoc = javadoc + "\n */";
                        }

                        // 5. 格式化 JavaDoc 内容（根据配置进行格式化）
                        javadoc = formatJavaDocContent(javadoc);

                        // 6. 插入新 JavaDoc
                        document.insertString(lineStartPosition, javadoc + "\n");
                        PsiDocumentManager.getInstance(project).commitDocument(document);

                        // 7. 格式化插入的 JavaDoc
                        PsiFile psiFile = element.getContainingFile();
                        if (psiFile != null) {
                            int endPosition = lineStartPosition + javadoc.length() + 1;
                            CodeStyleManager.getInstance(project).reformatText(psiFile, lineStartPosition, endPosition);
                        }

                        // Console 日志：输出最终插入的 JavaDoc（仅详细日志模式）
                        JavaDocConsoleView.printWithTimestamp(project, "=== 最终插入的 JavaDoc ===");
                        JavaDocConsoleView.print(project, javadoc);
                        JavaDocConsoleView.print(project, "");

                        // 输出可点击跳转的代码位置
                        VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
                        if (virtualFile != null) {
                            String fileName = virtualFile.getName();
                            int line = lineNumber + 1; // 行号从 1 开始显示
                            String locationMessage = String.format("==>>: %s:%d", fileName, line);

                            // 使用可点击的超链接格式输出
                            JavaDocConsoleView.printHyperlink(project, locationMessage, virtualFile, lineNumber);
                        }
                        JavaDocConsoleView.print(project, "");

                    } catch (Exception e) {
                        log.info("插入文档失败", e);
                    }
                }),
                "Insert JavaDoc",
                "AI Javadoc"
                                                         );
        });
    }

    /**
     * 删除元素的旧 JavaDoc 注释
     *
     * <p>删除元素已有的 JavaDoc 注释，为新注释腾出空间。
     * 同时删除注释前后的空白行，防止空行累积。
     *
     * <p>删除策略：
     * <ul>
     *   <li>删除注释本身</li>
     *   <li>删除注释后面的一个换行符（如果有）</li>
     *   <li>删除注释前面的所有空白行（防止累积）</li>
     * </ul>
     *
     * <p>安全措施：
     * <ul>
     *   <li>检查元素是否支持文档</li>
     *   <li>检查是否已有注释</li>
     *   <li>捕获异常防止中断操作</li>
     *   <li>边界检查防止越界</li>
     * </ul>
     *
     * @param element  目标元素
     * @param document 文档对象
     */
    @SuppressWarnings("D")
    private void deleteOldDocComment(@NotNull PsiElement element, @NotNull Document document) {
        if (!(element instanceof PsiDocCommentOwner)) {
            return;
        }

        PsiDocComment oldComment = ((PsiDocCommentOwner) element).getDocComment();
        if (oldComment == null) {
            return;
        }

        try {
            int startOffset = oldComment.getTextRange().getStartOffset();
            int endOffset = oldComment.getTextRange().getEndOffset();

            // 计算实际删除范围
            int deleteStart = startOffset;
            final int deleteEnd = getDeleteEnd(document, endOffset);

            // 2. 向前扩展：删除注释前面的所有空白行（包括空格、制表符）
            // 这是防止空行累积的关键！
            int lineStart = document.getLineStartOffset(document.getLineNumber(startOffset));
            while (deleteStart > lineStart) {
                char prevChar = document.getCharsSequence().charAt(deleteStart - 1);
                // 只删除空白字符（空格和制表符），但保留换行符
                if (prevChar == ' ' || prevChar == '\t') {
                    deleteStart--;
                } else {
                    break;
                }
            }

            // 如果注释前面只有空白字符，则从行首开始删除
            if (deleteStart == lineStart) {
                // 检查是否可以继续向前删除空行
                while (lineStart > 0) {
                    int prevLineEnd = lineStart - 1;
                    // 跳过换行符
                    if (document.getCharsSequence().charAt(prevLineEnd) == '\n') {
                        int prevLineStart = document.getLineStartOffset(document.getLineNumber(prevLineEnd));
                        // 检查前一行是否为空行（只包含空白字符）
                        boolean isEmptyLine = true;
                        for (int i = prevLineStart; i < prevLineEnd; i++) {
                            char c = document.getCharsSequence().charAt(i);
                            if (c != ' ' && c != '\t' && c != '\r') {
                                isEmptyLine = false;
                                break;
                            }
                        }

                        if (isEmptyLine) {
                            // 是空行，继续向前删除
                            deleteStart = prevLineStart;
                            lineStart = prevLineStart;
                        } else {
                            // 不是空行，停止向前扩展
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }

            // 执行删除
            document.deleteString(deleteStart, deleteEnd);

            if (AIProviderSettings.getInstance().runtimeSettings.verboseLogging) {
                log.debug("删除旧注释: 从 {} 到 {} (原注释: {} 到 {})",
                          deleteStart, deleteEnd, startOffset, endOffset);
            }

        } catch (Exception e) {
            log.warn("删除旧注释失败", e);
        }
    }

    /**
     * 计算删除操作的结束位置
     * <p>
     * 根据给定的文档对象和结束偏移量，计算删除操作的实际结束位置。该方法会处理换行符，包括Windows风格的\r\n换行符。
     *
     * @param document  文档对象，用于获取文本内容和长度
     * @param endOffset 初始的结束偏移量
     * @return 调整后的删除结束位置
     */
    private static int getDeleteEnd(@NotNull Document document, int endOffset) {
        int deleteEnd = endOffset;

        // 1. 向后扩展：删除注释后面的一个换行符（如果有）
        if (deleteEnd < document.getTextLength()) {
            char nextChar = document.getCharsSequence().charAt(deleteEnd);
            if (nextChar == '\n') {
                deleteEnd++;
            } else if (nextChar == '\r' && deleteEnd + 1 < document.getTextLength()) {
                // 处理 Windows 风格的换行符 \r\n
                if (document.getCharsSequence().charAt(deleteEnd + 1) == '\n') {
                    deleteEnd += 2;
                } else {
                    deleteEnd++;
                }
            }
        }
        return deleteEnd;
    }

    /**
     * 获取文档插入位置
     *
     * <p>确定新文档注释应该插入的位置。
     * 通常插入在元素修饰符列表之前，确保注释位置正确。
     *
     * <p>位置规则：
     * <ul>
     *   <li>PsiMethod：方法修饰符列表之前</li>
     *   <li>PsiClass：类修饰符列表之前</li>
     *   <li>PsiField：字段修饰符列表之前</li>
     *   <li>其他：元素起始位置</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 文档插入位置的偏移量
     */
    private int getInsertPosition(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            return ((PsiMethod) element).getModifierList().getTextRange().getStartOffset();
        } else if (element instanceof PsiClass) {
            return ((PsiClass) element).getModifierList().getTextRange().getStartOffset();
        } else if (element instanceof PsiField) {
            return ((PsiField) element).getModifierList().getTextRange().getStartOffset();
        }
        return element.getTextRange().getStartOffset();
    }

    /**
     * 格式化 JavaDoc 内容
     *
     * <p>对 JavaDoc 注释进行格式化处理，根据用户配置决定是否执行各项格式化操作：
     * <ul>
     *   <li>在中英文之间添加空格（如果配置启用）</li>
     *   <li>将中文标点符号替换为英文标点符号（如果配置启用）</li>
     * </ul>
     *
     * @param javadoc 原始 JavaDoc 文本
     * @return 格式化后的 JavaDoc 文本
     */
    @NotNull
    private String formatJavaDocContent(@NotNull String javadoc) {
        if (javadoc.isEmpty()) {
            return javadoc;
        }

        return JavaDocFormatter.format(
            javadoc,
            settings.addSpaceBetweenChineseAndEnglish,
            settings.replaceChinesePunctuation
                                      );
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

    /**
     * 任务统计信息
     *
     * <p>记录任务处理的统计信息，用于结果展示和日志记录。
     * 使用 record 简化代码，提供基本的统计计算和格式化功能。
     *
     * <p>包含的信息：
     * <ul>
     *   <li>completed：成功完成的任务数</li>
     *   <li>failed：处理失败的任务数</li>
     *   <li>skipped：被跳过的任务数</li>
     * </ul>
     *
     * <p>提供的方法：
     * <ul>
     *   <li>getTotal()：计算任务总数</li>
     *   <li>toString()：格式化统计信息</li>
     * </ul>
     */
    public record TaskStatistics(int completed, int failed, int skipped) {

        /**
         * 计算并返回总任务数
         * <p>
         * 将已完成、失败和跳过的任务数量相加，得到总任务数
         *
         * @return 总任务数
         */
        public int getTotal() {
            return completed + failed + skipped;
        }

        /**
         * 判断是否已执行过任务
         * <p>
         * 检查当前对象中已完成或跳过的任务总数是否大于0，若大于0则返回true，表示已执行过任务
         *
         * @return 是否已执行过任务
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
}

