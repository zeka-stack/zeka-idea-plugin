package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Alarm;
import com.intellij.util.concurrency.AppExecutorUtil;
import dev.dong4j.zeka.stack.idea.plugin.changelog.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.changelog.hint.CommitMessageHintManager;
import dev.dong4j.zeka.stack.idea.plugin.changelog.hint.CommitMessageHintService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogToolWindowService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import dev.dong4j.zeka.stack.idea.plugin.kit.PluginUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 提交消息生成器类
 * <p> 用于根据代码变更生成 Git 提交消息. 该类通过分析代码变更, 调用变更日志服务生成提交消息, 并在后台任务中处理生成过程.
 * 支持自定义的提交消息控制对象, 以便在不同的提交面板中设置提交消息.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
@Slf4j
public class CommitMessageGenerator {
    /** 项目对象, 用于关联当前生成器与 IDE 项目上下文 */
    private final Project project;
    /** 全局生成状态映射表, 用于跟踪各项目中提交记录生成任务的运行状态 */
    private static final Map<Project, GenerationState> GENERATION_STATES = new ConcurrentHashMap<>();
    /** 输入提示动画的文本间隔 (毫秒), 用于控制逐字输出速度 */
    private static final int TYPING_TEXT_DELAY_MS = 45;
    /** 行与行之间的停顿时间 (毫秒) */
    private static final int TYPING_LINE_PAUSE_MS = 1000;
    /** 光标闪烁间隔 (毫秒) */
    private static final int TYPING_CURSOR_DELAY_MS = 500;

    /**
     * 流式生成接口
     * <p> 定义了用于生成提交消息的流式处理逻辑, 支持通过 AI 服务逐块接收响应内容并实时更新提交消息. 该接口被设计为函数式接口, 便于在后台任务中动态调用不同生成策略 (如单提交, 压缩提交等).</p>
     * <p> 实现类需提供以下功能:</p>
     * <ul>
     *   <li> 接收变更服务, 监听器, 上下文文本和打字指示器作为参数 </li>
     *   <li> 返回生成的提交消息字符串, 支持异常抛出 </li>
     *   <li> 支持在流式响应过程中控制文本输出节奏和状态反馈 </li>
     * </ul>
     * <p> 典型使用场景包括:</p>
     * <ul>
     *   <li> 根据代码变更生成提交消息 </li>
     *   <li> 根据 Git 提交历史生成压缩提交消息 </li>
     *   <li> 在 UI 线程中实时更新提交面板内容 </li>
     * </ul>
     * <p> 该接口被封装在 <code>CommitMessageGenerator</code> 类中, 作为生成流程的核心抽象, 支持灵活替换不同 AI 服务或生成策略.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.19
     * @since 1.0.0
     */
    @FunctionalInterface
    private interface StreamGeneration {
        /**
         * 生成流式响应内容
         * <p> 根据传入的服务, 监听器, 上下文文本和输入指示器生成流式响应内容
         *
         * @param service         用于生成变更日志的服务实例, 不能为空
         * @param listener        用于监听流式响应的监听器, 不能为空
         * @param contextText     上下文文本, 可为空
         * @param typingIndicator 输入指示器, 不能为空
         * @return 生成的流式响应内容, 不能为空
         * @throws Exception 在生成过程中发生异常时抛出
         */
        @NotNull
        String generate(@NotNull ChangelogService service,
                        @NotNull AIStreamResponseListener listener,
                        @Nullable String contextText,
                        @NotNull TypingIndicator typingIndicator) throws Exception;
    }

    /**
     * 初始化 CommitMessageGenerator 实例
     * <p> 构造函数, 用于创建 CommitMessageGenerator 对象, 并初始化项目对象
     *
     * @param project 项目对象
     */
    public CommitMessageGenerator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理代码变更, 生成提交记录
     * <p> 该方法用于处理提交的代码变更, 根据代码的实际改动生成提交记录. 此方法会调用另一个重载的 generateForChanges 方法, 并传入 null 作为提交面板的提交信息控件.
     *
     * @param changes 变更集合
     * @since 1.0.0
     */
    public void generateForChanges(@NotNull Collection<Change> changes) {
        generateForChanges(changes, null, null, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 处理代码变更, 生成提交记录
     * <p> 该方法用于处理提交的代码变更, 根据代码的实际改动生成提交记录. 如果没有代码变更, 则记录警告日志并显示警告通知.
     * 如果存在代码变更, 则启动后台任务进行提交记录的生成, 并将结果写入提交面板.
     *
     * @param changes              变更集合
     * @param commitMessageControl 提交面板的提交信息控件, 可以为 null
     * @since 1.0.0
     */
    public void generateForChanges(@NotNull Collection<Change> changes,
                                   @Nullable CommitMessageI commitMessageControl) {
        generateForChanges(changes, commitMessageControl, null, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 处理代码变更, 生成提交记录
     * <p> 支持在提交面板与变更日志工具窗口同步输出最终结果, 便于复制.
     *
     * @param changes              变更集合
     * @param commitMessageControl 提交面板的提交信息控件, 可以为 null
     * @param outputSession        工具窗口输出会话, 可以为 null
     * @since 1.0.0
     */
    public void generateForChanges(@NotNull Collection<Change> changes,
                                   @Nullable CommitMessageI commitMessageControl,
                                   @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession) {
        generateForChanges(changes, commitMessageControl, outputSession, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 处理代码变更并生成提交记录
     * <p> 该方法用于根据传入的代码变更集合生成对应的提交消息. 若变更集合为空, 则记录调试日志并显示警告通知. 否则, 启动后台任务进行提交记录的生成, 并将结果写入提交面板或工具窗口输出会话.</p>
     * <p> 支持在生成过程中根据变更的根目录分组, 若存在多个根目录变更, 则调用多仓库处理逻辑; 否则, 直接调用服务生成单次提交消息.</p>
     *
     * @param changes              变更集合, 不能为空
     * @param commitMessageControl 提交面板的提交信息控件, 可为空
     * @param outputSession        工具窗口输出会话, 可为空
     * @param userAction           用户操作统计类型, 不能为空
     * @since 1.0.0
     */
    public void generateForChanges(@NotNull Collection<Change> changes,
                                   @Nullable CommitMessageI commitMessageControl,
                                   @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                                   @NotNull StatisticsUserAction userAction) {
        if (changes.isEmpty()) {
            log.debug("Git 提交页面：没有代码变更需要处理");
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.changes"));
            return;
        }
        runGeneration(commitMessageControl,
            outputSession,
            (service, listener, contextText, typingIndicator) -> {
                // 多仓库支持：按 VCS Root 分组处理，避免跨仓库混合上下文。
                Map<String, List<Change>> changesByRoot = groupChangesByRoot(changes);
                if (changesByRoot.size() > 1) {
                    return handleMultiRepositoryChanges(service,
                        changesByRoot,
                        contextText,
                        outputSession,
                        commitMessageControl,
                        typingIndicator,
                        userAction);
                }
                return service.generateCommitMessageFromDiffStream(changes, listener, contextText, userAction);
            });
    }

    /**
     * 基于 Git Log 中已提交记录的真实 diff 再生提交信息
     *
     * @param commitHash           提交 hash
     * @param commitMessageControl 提交消息控件（编辑提交消息对话框或提交面板）
     * @param outputSession        工具窗口输出会话，可为空
     */
    public void generateForCommitHash(@NotNull String commitHash,
                                      @Nullable CommitMessageI commitMessageControl,
                                      @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession) {
        generateForCommitHash(commitHash, commitMessageControl, outputSession, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 基于单个提交哈希再生提交信息
     * <p> 根据指定的提交哈希, 从 Git 日志中提取真实变更内容, 调用 AI 服务生成提交信息, 并在 UI 线程中更新提交面板或工具窗口输出. 该方法适用于单条提交的再生场景.
     *
     * @param commitHash           提交哈希值, 不能为空, 用于定位 Git 日志中的提交记录
     * @param commitMessageControl 提交消息控件 (如编辑提交消息对话框或提交面板), 可为空
     * @param outputSession        工具窗口输出会话, 用于同步显示生成结果, 可为空
     * @param userAction           统计用户操作类型, 不能为空, 用于记录用户行为数据
     */
    public void generateForCommitHash(@NotNull String commitHash,
                                      @Nullable CommitMessageI commitMessageControl,
                                      @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                                      @NotNull StatisticsUserAction userAction) {
        if (commitHash.isBlank()) {
            log.debug("Git 提交页面：提交记录再生失败，commit hash 为空");
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.regenerate.select.single.commit"));
            return;
        }
        generateForCommitSelection(List.of(commitHash), List.of(), commitMessageControl, outputSession, userAction);
    }

    /**
     * 基于 Git Log 中多条已提交记录的真实 diff（压缩提交/Squash）再生提交信息
     *
     * @param commitHashes         提交 hash 列表（至少 2 条）
     * @param selectedCommitTitles 选中提交的原始 message（可为空，用于帮助模型合并语义）
     * @param commitMessageControl 提交消息控件（压缩提交对话框）
     * @param outputSession        工具窗口输出会话，可为空
     */
    public void generateForCommitHashes(@NotNull List<String> commitHashes,
                                        @NotNull List<String> selectedCommitTitles,
                                        @Nullable CommitMessageI commitMessageControl,
                                        @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession) {
        generateForCommitHashes(commitHashes, selectedCommitTitles, commitMessageControl, outputSession, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 基于提交哈希列表再生提交信息 (支持单条或压缩提交)
     * <p> 该方法用于根据指定的提交哈希列表, 从 Git 日志中提取真实变更内容, 调用 AI 服务生成提交信息, 并在 UI 线程中更新提交面板或工具窗口输出. 支持多提交压缩合并场景.
     *
     * @param commitHashes         提交哈希列表 (至少 2 条), 用于定位 Git 日志中的提交记录
     * @param selectedCommitTitles 选中提交的原始消息列表, 用于辅助模型合并语义, 不能为空
     * @param commitMessageControl 提交消息控件 (如压缩提交对话框或提交面板), 可为空
     * @param outputSession        工具窗口输出会话, 用于同步显示生成结果, 可为空
     * @param userAction           统计用户操作类型, 不能为空
     * @since 1.0.0
     */
    public void generateForCommitHashes(@NotNull List<String> commitHashes,
                                        @NotNull List<String> selectedCommitTitles,
                                        @Nullable CommitMessageI commitMessageControl,
                                        @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                                        @NotNull StatisticsUserAction userAction) {
        if (commitHashes.size() < 2) {
            log.debug("Git 提交页面：压缩提交再生失败，commit hash 数量不足，size={}", commitHashes.size());
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.regenerate.select.at.least.one.commit"));
            return;
        }
        generateForCommitSelection(commitHashes, selectedCommitTitles, commitMessageControl, outputSession, userAction);
    }

    /**
     * 基于提交哈希列表再生提交信息 (支持单条或压缩提交)
     * <p> 该方法用于根据指定的提交哈希列表, 从 Git 日志中提取真实变更内容, 调用 AI 服务生成提交信息, 并在 UI 线程中更新提交面板或工具窗口输出. 支持多提交压缩合并场景.
     *
     * @param commitHashes         提交哈希列表 (至少一条), 用于定位 Git 日志中的提交记录
     * @param selectedCommitTitles 选中提交的原始消息列表 (可为空), 用于辅助模型合并语义
     * @param commitMessageControl 提交消息控件 (如压缩提交对话框或提交面板), 可为空
     * @param outputSession        工具窗口输出会话, 用于同步显示生成结果, 可为空
     * @since 1.0.0
     */
    private void generateForCommitSelection(@NotNull List<String> commitHashes,
                                            @NotNull List<String> selectedCommitTitles,
                                            @Nullable CommitMessageI commitMessageControl,
                                            @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                                            @NotNull StatisticsUserAction userAction) {
        if (commitHashes.isEmpty()) {
            log.debug("Git 提交页面：提交记录再生失败，commit hash 为空列表");
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.regenerate.select.at.least.one.commit"));
            return;
        }
        runGeneration(commitMessageControl,
            outputSession,
            (service, listener, contextText, typingIndicator) -> commitHashes.size() > 1
                ? service.generateSquashCommitMessageFromGitLogStream(
                commitHashes,
                selectedCommitTitles,
                listener,
                contextText,
                userAction)
                : service.generateCommitMessageFromGitLogStream(
                commitHashes.getFirst(),
                listener,
                contextText,
                userAction));
    }

    /**
     * 执行提交消息生成任务
     * <p> 该方法在后台线程中启动提交消息生成流程, 支持流式响应处理, 进度指示, 取消控制和结果回显. 通过传入的流式生成接口, 调用 AI 服务生成提交消息, 并在 UI 线程中更新提交面板或工具窗口输出.</p>
     * <p> 流程包括:</p>
     * <ul>
     *   <li> 初始化生成状态并绑定到当前项目 </li>
     *   <li> 设置进度指示器并启动异步任务 </li>
     *   <li> 根据配置决定是否使用提交文本作为上下文 </li>
     *   <li> 创建打字动画指示器并绑定取消令牌 </li>
     *   <li> 调用流式生成接口获取提交消息 </li>
     *   <li> 格式化消息并在 UI 线程中写入提交面板或输出窗口 </li>
     *   <li> 异常处理: 失败时显示错误提示并标记生成失败 </li>
     *   <li> 清理资源: 重置占位符, 停止动画, 移除状态记录 </li>
     * </ul>
     *
     * @param commitMessageControl 提交消息控件 (如提交面板或压缩对话框), 可为空
     * @param outputSession        工具窗口输出会话, 用于同步显示生成结果, 可为空
     * @param generation           流式生成接口, 定义了如何从 AI 服务获取响应内容并实时更新提交消息
     * @since 1.0.0
     */
    private void runGeneration(@Nullable CommitMessageI commitMessageControl,
                               @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                               @NotNull StreamGeneration generation) {
        GenerationState state = new GenerationState();
        GENERATION_STATES.put(project, state);

        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, ChangelogBundle.message("commit.generating.progress"), true) {
                /**
                 * 执行 Git 提交消息生成任务
                 * <p> 该方法在后台线程中运行, 负责分析变更内容, 调用 AI 服务生成提交消息, 并在 UI 线程中更新提交面板.
                 * 任务过程中会显示进度指示器, 支持取消操作, 并在失败时弹出错误通知.
                 *
                 * @param indicator 进度指示器, 用于显示任务状态和进度
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(ChangelogBundle.message("commit.analyzing.changes"));
                    state.indicator.set(indicator);
                    state.thread.set(Thread.currentThread());

                    String contextText = null;
                    if (SettingsState.getInstance().useCommitMessageInputAsContext) {
                        contextText = getCommitMessageText(commitMessageControl);
                    }

                    TypingIndicator typingIndicator = startTypingIndicator(commitMessageControl, outputSession, contextText == null);
                    state.typingIndicator.set(typingIndicator);
                    StreamCancellationToken cancellationToken = new StreamCancellationToken();
                    state.cancellationToken.set(cancellationToken);
                    if (outputSession != null) {
                        outputSession.bindCancellationToken(cancellationToken);
                    }

                    try {
                        if (state.cancelled.get()) {
                            return;
                        }

                        ChangelogService service = ChangelogService.getInstance(project);

                        StringBuilder buffer = new StringBuilder();
                        AtomicReference<Boolean> updated = new AtomicReference<>(false);
                        final AIStreamResponseListener listener =
                            createStreamResponseListener(state,
                                buffer,
                                typingIndicator,
                                updated,
                                cancellationToken,
                                commitMessageControl,
                                outputSession);

                        String commitMessage = generation.generate(service, listener, contextText, typingIndicator);
                        String formattedCommitMessage = CommitMessageFormatter.format(commitMessage);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (project.isDisposed() || state.cancelled.get()) {
                                log.debug("项目已销毁或任务已取消，跳过设置提交消息");
                                return;
                            }
                            if (formattedCommitMessage.isBlank() && !updated.get()) {
                                return;
                            }

                            boolean applied = setCommitMessageText(formattedCommitMessage, commitMessageControl, true);
                            if (!applied && !updated.get()) {
                                log.debug("Git 提交页面：提交面板不可用，无法写入提交记录");
                            }

                            if (outputSession != null && !project.isDisposed()) {
                                outputSession.setText(formattedCommitMessage);
                            }
                        });
                    } catch (Exception e) {
                        log.debug("Git 提交页面：生成提交记录失败", e);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (project.isDisposed() || state.cancelled.get()) {
                                return;
                            }
                            typingIndicator.generateFailure();
                            String errorMessage = e.getMessage();
                            if (errorMessage != null && !errorMessage.isEmpty()) {
                                NotificationUtil.showError(project, errorMessage);
                            } else {
                                NotificationUtil.showError(
                                    project,
                                    ChangelogBundle.message("commit.generation.error",
                                        ChangelogBundle.message("error.ai.service.unknown")));
                            }
                        });
                    } finally {
                        resetCommitMessagePlaceholder(commitMessageControl);
                        typingIndicator.stop();
                        GENERATION_STATES.remove(project);
                    }
                }
            });
    }

    /**
     * 创建 AI 流式响应监听器
     * <p> 创建一个用于处理 AI 响应流的监听器, 该监听器会实时更新提交消息文本.
     * <p> 监听器包含四个回调方法:
     * <ul>
     * <li>{@code onStart}: 启动时清空缓冲区 </li>
     * <li>{@code onChunk}: 处理接收到的文本块并实时更新提交消息文本 </li>
     * <li>{@code onComplete}: 在异步操作完成后更新提交消息文本 </li>
     * <li>{@code onError}: 处理错误情况, 显示失败提示 </li>
     * </ul>
     * <p> 该监听器支持在 UI 线程中安全更新提交消息控件, 并可选地将内容输出到工具窗口会话.
     *
     * @param state                生成状态对象, 用于跟踪任务生命周期和取消状态, 不能为 null
     * @param buffer               用于累积文本的缓冲区, 不能为 null
     * @param typingIndicator      打字指示器, 用于控制动画效果, 不能为 null
     * @param updated              原子引用, 用于标记提交消息是否已更新, 不能为 null
     * @param cancellationToken    流取消令牌, 用于控制流式操作的取消行为, 不能为 null
     * @param commitMessageControl 提交消息控件, 可为空, 用于设置生成的提交消息文本
     * @param outputSession        工具窗口输出会话, 可为空, 用于将生成内容追加到输出窗口
     * @return AIStreamResponseListener 实例, 用于监听 AI 响应的流式事件
     */
    private @NotNull AIStreamResponseListener createStreamResponseListener(@NotNull GenerationState state,
                                                                           @NotNull StringBuilder buffer,
                                                                           @NotNull TypingIndicator typingIndicator,
                                                                           @NotNull AtomicReference<Boolean> updated,
                                                                           @NotNull StreamCancellationToken cancellationToken,
                                                                           @Nullable CommitMessageI commitMessageControl,
                                                                           @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession) {
        AtomicBoolean contentStarted = new AtomicBoolean(false);
        return new AIStreamResponseListener() {
            /**
             * 初始化缓冲区, 清空之前的内容
             * <p> 在流开始时调用, 用于重置缓冲区内容, 为后续接收数据做准备
             */
            @Override
            public void onStart() {
                buffer.setLength(0);
            }

            /**
             * 获取流取消令牌
             * <p> 返回当前流操作的取消令牌, 用于在需要时取消流处理过程
             *
             * @return 流取消令牌, 如果未设置则返回 null
             */
            @Override
            public @NotNull StreamCancellationToken cancellationToken() {
                return cancellationToken;
            }

            /**
             * 处理流式响应的片段数据
             * <p> 当接收到新的响应片段时, 将片段内容追加到缓冲区, 并在 UI 线程中更新提交信息文本.
             * 如果内容开始标志未设置且片段非空, 则停止打字指示器.
             * 若输出会话存在, 则将片段内容追加到输出会话中.
             *
             * @param chunk 当前接收到的响应片段内容
             */
            @Override
            public void onChunk(@NotNull String chunk) {
                if (state.cancelled.get()) {
                    return;
                }
                if (!chunk.isBlank() && contentStarted.compareAndSet(false, true)) {
                    typingIndicator.stop();
                }
                buffer.append(chunk);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed() || state.cancelled.get()) {
                        return;
                    }
                    if (setCommitMessageText(buffer.toString(), commitMessageControl)) {
                        updated.set(true);
                    }
                });
                if (outputSession != null) {
                    outputSession.append(chunk);
                }
            }

            /**
             * 处理流式响应中的思考阶段片段数据
             * <p> 当接收到表示“思考中”的响应片段时, 若片段内容非空, 则启动思考状态指示器. 如果当前任务已被取消, 则不执行任何操作.
             *
             * @param chunk 当前接收到的响应片段内容, 非空字符串
             */
            @Override
            public void onThinkingChunk(@NotNull String chunk) {
                if (state.cancelled.get()) {
                    return;
                }
                if (!chunk.isBlank()) {
                    typingIndicator.startThinkingStage();
                }
            }

            /**
             * 当流处理完成时的回调方法
             * <p> 在流内容完全接收后被调用, 用于执行后续处理逻辑. 如果流已被取消或项目已销毁, 则不执行任何操作.
             *
             * @param fullContent 完整的流内容字符串
             */
            @Override
            public void onComplete(@NotNull String fullContent) {
                if (state.cancelled.get()) {
                    return;
                }
                if (!fullContent.isBlank() && contentStarted.compareAndSet(false, true)) {
                    typingIndicator.stop();
                } else if (fullContent.isBlank()) {
                    typingIndicator.stop();
                }
                String formattedContent = CommitMessageFormatter.format(fullContent);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed() || state.cancelled.get()) {
                        return;
                    }
                    if (setCommitMessageText(formattedContent, commitMessageControl, true)) {
                        updated.set(true);
                    }
                    if (outputSession != null) {
                        outputSession.setText(formattedContent);
                    }
                });
            }

            /**
             * 处理流式响应中的通知事件
             * <p> 当接收到通知消息时, 该方法在 UI 线程中执行, 用于在编辑器组件上显示通知提示. 如果当前任务已被取消或项目已销毁, 则不执行任何操作.
             *
             * @param message 通知消息内容, 非空字符串
             */
            @Override
            public void onNotice(@NotNull String message) {
                if (state.cancelled.get()) {
                    return;
                }

                final EditorTextField editorField = getEditorTextField(commitMessageControl);
                if (editorField != null) {
                    showNoticeActionTip(editorField.getComponent(), message);
                }
            }

            /**
             * 处理流式响应错误事件
             * <p> 当流式响应发生错误时, 该方法会被调用. 在 UI 线程中执行错误处理逻辑, 包括停止打字指示器的失败状态.
             * 如果当前任务已被取消或项目已销毁, 则不执行任何操作.
             *
             * @param error     错误信息, 非空字符串
             * @param exception 错误异常, 可能为 null
             */
            @Override
            public void onError(@NotNull String error, @Nullable Throwable exception) {
                if (state.cancelled.get()) {
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed() || state.cancelled.get()) {
                        return;
                    }
                    typingIndicator.generateFailure();
                });
            }
        };
    }


    /**
     * 显示操作提示气泡
     * <p> 在指定组件的下方显示一个包含 HTML 格式提示信息的气泡提示框, 提示用户当前上下文功能已启用.
     * <p> 气泡中包含一个"关闭"超链接, 点击后将禁用上下文功能并持久化设置.
     * <p> 注意：每个项目只会显示一次此提示，避免重复打扰用户。
     * <p> 示例:
     * <pre>{@code
     * showActionTip(myComponent);
     * }</pre>
     *
     * @param component 显示气泡的组件, 不能为 null
     */
    private void showContextSettingActionTip(@NotNull JComponent component) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String shownKey = "changelog.commit.context.tip.shown";
        String versionKey = "changelog.commit.context.tip.version";

        // 获取当前插件版本号
        String currentVersion = PluginUtil.getVersion(PluginContents.PLUGIN_ID);
        if (currentVersion == null) {
            log.debug("无法获取插件版本号，跳过版本检查");
            // 如果无法获取版本号，使用原有逻辑（每个项目只显示一次）
            if (propertiesComponent.getBoolean(shownKey, false)) {
                log.debug("提交上下文提示已显示过，跳过");
                return;
            }
            propertiesComponent.setValue(shownKey, true);
        } else {
            // 获取存储的版本号
            String storedVersion = propertiesComponent.getValue(versionKey);

            // 如果版本号不一致（说明是升级），重置已显示标记
            if (storedVersion != null && !storedVersion.equals(currentVersion)) {
                log.debug("检测到插件版本升级：{} -> {}，重置提示状态", storedVersion, currentVersion);
                propertiesComponent.unsetValue(shownKey);
            }

            // 检查是否已经显示过提示（每个版本只显示一次）
            if (propertiesComponent.getBoolean(shownKey, false)) {
                log.debug("提交上下文提示已显示过（版本：{}），跳过", currentVersion);
                return;
            }

            // 标记为已显示，并保存当前版本号
            propertiesComponent.setValue(shownKey, true);
            propertiesComponent.setValue(versionKey, currentVersion);
        }

        showActionTip(component,
            "",
            ChangelogBundle.message("commit.context.tip.enabled"),
            "action:close",
            MessageType.INFO,
            5000);
    }

    /**
     * 显示操作提示气泡
     * <p> 在指定组件的下方显示一个包含 HTML 格式提示信息的气泡提示框, 提示用户当前上下文功能已启用.
     * <p> 气泡中包含一个 "关闭" 超链接, 点击后将禁用上下文功能并持久化设置.
     * <p> 注意: 每个项目只会显示一次此提示, 避免重复打扰用户.
     * <p> 示例:
     * <pre>{@code
     * showNoticeActionTip(myComponent, feedbackUrl);
     * }</pre>
     *
     * @param component 显示气泡的组件, 不能为 null
     * @param message   反馈 URL, 用于打开浏览器
     */
    private void showNoticeActionTip(@NotNull JComponent component, @NotNull String message) {
        showActionTip(component,
            "",
            ChangelogBundle.message("commit.context.tip.fallback", message),
            "action:fallback",
            MessageType.WARNING,
            10000);
    }


    /**
     * 显示操作提示气泡
     * <p> 在指定组件的下方显示一个包含 HTML 格式提示信息的气泡提示框, 用于展示操作提示内容.
     * <p> 气泡中包含一个超链接, 点击后根据配置执行指定动作 (如打开浏览器).
     * <p> 该方法为重载方法, 需配合多个参数使用, 确保提示内容和交互行为正确.
     * <p> 示例:
     * <pre>{@code
     * showActionTip(myComponent, "提示信息内容", "<html> 点击此处反馈 </html>", "action:fallback", MessageType.WARNING, 10000);
     * }</pre>
     *
     * @param component   显示气泡的组件, 不能为 null
     * @param openUrl     提示信息内容, 不能为 null
     * @param htmlContent HTML 格式的内容, 用于显示在气泡中
     * @param actionName  超链接的标识名称, 用于匹配点击事件, 例如 "action:fallback"
     * @param messageType 提示气泡的类型, 如 MessageType.INFO 或 MessageType.WARNING
     * @param outTime     气泡自动消失的延迟时间 (毫秒)
     */
    private void showActionTip(@NotNull JComponent component,
                               @NotNull String openUrl,
                               String htmlContent,
                               String actionName,
                               MessageType messageType,
                               int outTime) {
        HyperlinkAdapter linkListener = new HyperlinkAdapter() {
            /**
             * 处理超链接激活事件
             * <p> 当用户点击超链接时触发, 若链接描述为 "action:fallback", 则在浏览器中打开反馈 URL
             * <p> 示例:
             * <pre>{@code
             * // 当用户点击反馈链接时, 使用浏览器打开反馈 URL
             * hyperlinkActivated(event); // event.getDescription() 返回 "action:fallback"
             * }</pre>
             *
             * @param e 超链接事件对象, 不能为 null
             */
            @Override
            protected void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                String url = e.getDescription();
                // 处理 action:fallback 链接
                if (actionName.equals(url) && !openUrl.isEmpty()) {
                    BrowserUtil.browse(openUrl);
                }
            }
        };

        final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                htmlContent,
                messageType,
                linkListener)
            .setFadeoutTime(outTime)
            .setLayer(Balloon.Layer.normal)
            .setHideOnLinkClick(true)
            .createBalloon();

        balloon.show(new RelativePoint(component,
                new Point(component.getWidth() / 2, component.getHeight())),
            Balloon.Position.below);
    }

    /**
     * 获取当前插件版本号
     * <p>
     * 通过公开插件管理 API 获取插件的版本信息。
     *
     * @return 插件版本号，如果获取失败则返回 null
     */
    @Nullable
    private String getCurrentPluginVersion() {
        return PluginUtil.getVersion(PluginContents.PLUGIN_ID);
    }

    /**
     * 按 VCS Root 对变更分组
     * <p> 多仓库场景下用于拆分生成上下文，避免跨仓库混合导致的噪音.
     * <p> 对于删除的文件, 会从 ContentRevision 获取文件路径来查找 VCS root, 确保正确分组.
     *
     * @param changes 变更集合
     * @return key 为仓库根路径的分组 Map
     */
    @NotNull
    Map<String, List<Change>> groupChangesByRoot(@NotNull Collection<Change> changes) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        Map<String, List<Change>> grouped = new LinkedHashMap<>();
        for (Change change : changes) {
            VirtualFile root = findVcsRootForChange(change, vcsManager);
            if (root == null) {
                // 无法确定 VCS root, 跳过该变更并记录日志
                String filePath = getChangeFilePath(change);
                log.warn("无法确定变更文件的 VCS root, 跳过该文件: {}", filePath);
                continue;
            }
            String rootKey = root.getPresentableUrl();
            grouped.computeIfAbsent(rootKey, key -> new java.util.ArrayList<>()).add(change);
        }
        return grouped;
    }

    /**
     * 获取变更对象的文件路径
     * <p> 用于日志输出, 优先从 VirtualFile 获取, 否则从 ContentRevision 获取.
     *
     * @param change 变更对象, 不能为 null
     * @return 文件路径, 如果无法获取则返回 "unknown"
     */
    @NotNull
    private String getChangeFilePath(@NotNull Change change) {
        VirtualFile file = change.getVirtualFile();
        if (file != null) {
            return file.getPath();
        }
        ContentRevision revision = getContentRevision(change);
        if (revision != null) {
            String path = revision.getFile().getPath();
            return !path.isEmpty() ? path : "unknown";
        }
        return "unknown";
    }

    /**
     * 查找变更对应的 VCS root
     * <p> 对于正常文件, 直接从 VirtualFile 获取 VCS root.
     * <p> 如果 VirtualFile 为 null, 则从 ContentRevision 获取文件路径:
     * <ul>
     *   <li>删除的文件: 使用 beforeRevision 获取路径</li>
     *   <li>新增的文件: 使用 afterRevision 获取路径</li>
     *   <li>重命名/移动的文件: 优先使用 afterRevision 获取新路径</li>
     *   <li>其他情况: 优先使用 afterRevision, 否则使用 beforeRevision</li>
     * </ul>
     * 然后通过路径查找 VirtualFile 并获取 VCS root.
     *
     * @param change     变更对象, 不能为 null
     * @param vcsManager VCS 管理器, 不能为 null
     * @return VCS root, 如果无法确定则返回 null
     */
    @Nullable
    private VirtualFile findVcsRootForChange(@NotNull Change change, @NotNull ProjectLevelVcsManager vcsManager) {
        // 首先尝试从 VirtualFile 获取 (适用于大多数情况)
        VirtualFile file = change.getVirtualFile();
        if (file != null) {
            return vcsManager.getVcsRootFor(file);
        }

        // 如果 VirtualFile 为 null, 从 ContentRevision 获取文件路径
        final ContentRevision revision = getContentRevision(change);

        if (revision == null) {
            return null;
        }

        // 从 ContentRevision 获取文件路径
        String filePath = revision.getFile().getPath();
        if (filePath.isEmpty()) {
            return null;
        }

        // 尝试通过路径查找 VirtualFile (即使文件已删除, 父目录可能还存在)
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        VirtualFile virtualFile = localFileSystem.findFileByPath(filePath);
        if (virtualFile != null) {
            return vcsManager.getVcsRootFor(virtualFile);
        }

        // 如果文件不存在, 尝试向上查找父目录, 直到找到存在的目录或项目根目录
        java.io.File ioFile = new java.io.File(filePath);
        java.io.File currentDir = ioFile.getParentFile();
        String projectBasePath = project.getBasePath();

        while (currentDir != null) {
            // 如果已经超出项目根目录, 停止查找
            if (projectBasePath != null && !currentDir.getPath().startsWith(projectBasePath)) {
                break;
            }

            VirtualFile dirVirtualFile = localFileSystem.findFileByPath(currentDir.getPath());
            if (dirVirtualFile != null) {
                VirtualFile root = vcsManager.getVcsRootFor(dirVirtualFile);
                if (root != null) {
                    return root;
                }
            }

            currentDir = currentDir.getParentFile();
        }

        return null;
    }

    /**
     * 根据变更对象获取对应的 ContentRevision
     * <p> 根据文件状态判断应使用哪个修订版本:
     * <ul>
     * <li> 若文件已删除, 则使用 beforeRevision</li>
     * <li> 若存在 afterRevision, 则使用 afterRevision</li>
     * <li> 若不存在 afterRevision 但存在 beforeRevision, 则使用 beforeRevision</li>
     * </ul>
     * <p> 若所有修订版本均不存在, 则返回 null.
     *
     * @param change 变更对象, 不能为 null
     * @return 对应的 ContentRevision, 若无法确定则返回 null
     */
    private static @Nullable ContentRevision getContentRevision(@NotNull Change change) {
        ContentRevision beforeRevision = change.getBeforeRevision();
        ContentRevision afterRevision = change.getAfterRevision();

        // 确定使用哪个 ContentRevision 获取路径
        ContentRevision revision = null;
        FileStatus fileStatus = change.getFileStatus();
        boolean isDeleted = fileStatus == FileStatus.DELETED || fileStatus == FileStatus.DELETED_FROM_FS;

        if (isDeleted) {
            // 删除的文件: 使用 beforeRevision
            revision = beforeRevision;
        } else if (afterRevision != null) {
            // 新增、修改、重命名等: 优先使用 afterRevision (新路径更可能存在于文件系统)
            revision = afterRevision;
        } else if (beforeRevision != null) {
            // 如果 afterRevision 为 null, 使用 beforeRevision
            revision = beforeRevision;
        }
        return revision;
    }

    /**
     * 处理多仓库变更的提交信息生成
     * <p> 该方法用于在存在多个 VCS 仓库根目录的场景下, 分别对每个仓库的变更进行提交信息生成. 每个仓库的变更会异步处理, 最终将所有生成的提交信息合并并显示在提交面板中.
     * <p> 如果某个仓库的提交信息生成失败, 将忽略该失败并继续处理其他仓库.
     *
     * @param service              提交信息生成服务实例, 用于调用 AI 服务生成提交消息, 不能为 null
     * @param changesByRoot        按仓库根路径分组的变更集合,key 为仓库根路径,value 为该路径下的变更列表, 不能为 null
     * @param contextText          用于生成提交消息的上下文文本, 可以为 null
     * @param outputSession        工具窗口输出会话, 用于在工具窗口中同步输出最终结果, 可以为 null
     * @param commitMessageControl 提交面板的提交信息控件, 用于在 UI 中设置生成的提交消息, 可以为 null
     * @param typingIndicator      打字动画指示器, 用于在生成过程中显示打字效果, 不能为 null
     * @throws Exception 当任意仓库的提交信息生成失败时抛出异常
     */
    private @NotNull String handleMultiRepositoryChanges(@NotNull ChangelogService service,
                                                         @NotNull Map<String, List<Change>> changesByRoot,
                                                         @Nullable String contextText,
                                                         @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                                                         @Nullable CommitMessageI commitMessageControl,
                                                         @NotNull TypingIndicator typingIndicator,
                                                         @NotNull StatisticsUserAction userAction) throws Exception {

        // 多仓库场景：为每个仓库并行生成独立的 commit message，再合并输出。
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (Map.Entry<String, List<Change>> entry : changesByRoot.entrySet()) {
            List<Change> rootChanges = entry.getValue();
            if (rootChanges.isEmpty()) {
                continue;
            }
            CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    String commitMessage;
                    try {
                        commitMessage = service.generateCommitMessageFromDiff(rootChanges, contextText, userAction);
                    } catch (Exception e) {
                        log.debug("Git 提交页面：[{}] 生成失败", rootChanges, e);
                        commitMessage = "";
                    }
                    String formattedCommitMessage = CommitMessageFormatter.format(commitMessage);
                    return formattedCommitMessage.isBlank() ? "" : formattedCommitMessage.trim();
                }, AppExecutorUtil.getAppExecutorService())
                .exceptionally(ex -> {
                    log.debug("Git 提交页面：多仓库提交信息生成失败", ex);
                    return "";
                });
            futures.add(future);
        }

        if (futures.isEmpty()) {
            return "";
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        List<String> commitMessages = new ArrayList<>();
        for (CompletableFuture<String> future : futures) {
            String message = future.join();
            if (!message.isBlank()) {
                commitMessages.add(message);
            }
        }

        // 合并输出，避免引入仓库标题，保持类似「多条 commit message」的展示格式。
        String combined = String.join("\n\n", commitMessages);

        if (!combined.isBlank()) {
            typingIndicator.stop();
        }
        return combined;
    }

    /**
     * 将生成的提交信息文本输出到工具窗口会话中
     * <p> 如果传入的输出会话为 null, 则自动创建一个新的会话并设置标题为多仓库会话标题. 然后将合并后的文本内容写入该会话.
     *
     * @param outputSession 工具窗口输出会话, 可以为 null
     * @param combined      合并后的提交信息文本内容, 不能为空
     */
    private void printToToolwindow(ChangelogToolWindowService.@Nullable ChangelogOutputSession outputSession, String combined) {
        ChangelogToolWindowService.ChangelogOutputSession session = outputSession;
        if (session == null) {
            session = ChangelogToolWindowService.getInstance(project)
                .openSession(ChangelogBundle.message("commit.multi.repo.session.title"));
        }
        session.setText(combined);
    }

    /**
     * 设置提交消息文本
     * <p> 尝试通过反射调用提交面板对象的方法来设置提交消息文本. 支持的方法名包括 "setCommitMessage","setCommitMessageText" 和 "setText".
     *
     * @param commitMessage        提交消息文本
     * @param commitMessageControl 提交面板的控制对象
     * @return 如果成功设置提交消息文本, 则返回 true; 否则返回 false
     */
    private boolean setCommitMessageText1(@NotNull String commitMessage, @Nullable CommitMessageI commitMessageControl) {
        if (commitMessageControl == null) {
            return false;
        }
        // 兼容不同版本的提交面板控件 API
        for (String methodName : List.of("setCommitMessage", "setCommitMessageText", "setText")) {
            Method method = findMethod(commitMessageControl.getClass(), methodName, String.class);
            if (method != null) {
                try {
                    method.invoke(commitMessageControl, commitMessage);
                    return true;
                } catch (Exception e) {
                    log.debug("Git 提交页面：调用提交面板方法失败: {}", methodName, e);
                }
            }
        }
        return false;
    }

    /**
     * 设置提交消息文本
     * <p> 直接调用提交面板控制对象的 setCommitMessage 方法设置提交消息文本.
     * <p> 此方法为简化版本, 不尝试通过反射查找方法, 直接调用已知方法名.
     *
     * @param commitMessage        提交消息文本, 不能为 null
     * @param commitMessageControl 提交面板的控制对象, 可以为 null
     * @return 如果成功调用 setCommitMessage 方法并设置文本, 则返回 true; 否则返回 false
     */
    private boolean setCommitMessageText(@NotNull String commitMessage, @Nullable CommitMessageI commitMessageControl) {
        return setCommitMessageText(commitMessage, commitMessageControl, false);
    }

    /**
     * 设置提交消息文本
     *
     * @param commitMessage        提交消息文本，不能为 null
     * @param commitMessageControl 提交面板的控制对象，可以为 null
     * @param isComplete           是否为最终完成（true 表示生成完成，false 表示流式输出中的中间状态）
     * @return 如果成功设置文本返回 true，否则返回 false
     */
    private boolean setCommitMessageText(@NotNull String commitMessage,
                                         @Nullable CommitMessageI commitMessageControl,
                                         boolean isComplete) {
        if (commitMessageControl == null) {
            return false;
        }

        // 检查项目是否已销毁
        if (project.isDisposed()) {
            log.debug("项目已销毁，跳过设置提交消息文本");
            return false;
        }

        try {
            commitMessageControl.setCommitMessage(commitMessage);

            // 只有在最终完成时才通知 CommitMessageHintManager
            if (isComplete) {
                notifyGenerationCompleted(commitMessageControl);
            }

            return true;
        } catch (Exception e) {
            // 捕获可能的异常（如项目已销毁）
            if (project.isDisposed()) {
                log.debug("项目已销毁，无法设置提交消息文本", e);
            } else {
                log.debug("设置提交消息文本失败", e);
            }
            return false;
        }
    }

    /**
     * 通知 CommitMessageHintManager 生成完成
     * <p>
     * 当提交消息生成完成后，通知对应的 Hint Manager 设置生成完成状态。
     *
     * @param commitMessageControl 提交面板的控制对象，不能为 null
     */
    private void notifyGenerationCompleted(@NotNull CommitMessageI commitMessageControl) {
        // 检查项目是否已销毁
        if (project.isDisposed()) {
            log.debug("项目已销毁，跳过通知生成完成状态");
            return;
        }

        try {
            // 获取 EditorTextField
            EditorTextField editorField = getEditorTextField(commitMessageControl);

            // 获取 Editor
            Editor editor = editorField.getEditor();
            if (editor == null) {
                return;
            }

            // 获取项目（从 Editor 获取，可能与当前项目不同）
            Project editorProject = editor.getProject();
            if (editorProject == null || editorProject.isDisposed()) {
                return;
            }

            // 获取 CommitMessageHintService（使用 Editor 的项目）
            CommitMessageHintService hintService = editorProject.getService(CommitMessageHintService.class);
            if (hintService == null) {
                return;
            }

            // 获取 Hint Manager 并标记生成完成
            CommitMessageHintManager hintManager = hintService.getHintManager(editor);
            if (hintManager != null) {
                hintManager.markGenerationCompleted(true);
            }
        } catch (Exception e) {
            // 静默处理异常，避免影响主流程
            log.debug("通知生成完成状态失败", e);
        }
    }

    /**
     * 设置提交消息文本的占位符内容
     * <p> 尝试将指定的占位符文本设置到提交面板的编辑器字段中. 如果提交面板控制对象为 null, 则直接返回.
     *
     * @param text                 占位符文本内容, 不能为 null
     * @param commitMessageControl 提交面板的控制对象, 可以为 null
     */
    private void setCommitMessagePlaceholder(@NotNull String text, @Nullable CommitMessageI commitMessageControl) {
        final EditorTextField editorField = getEditorTextField(commitMessageControl);
        if (editorField == null) {
            return;
        }
        editorField.setPlaceholder(text);
        // 刷新 EditorTextField 以显示更新的 placeholder
        ApplicationManager.getApplication().invokeLater(() -> {
            // 检查项目是否已销毁
            if (project.isDisposed()) {
                return;
            }
            editorField.revalidate();
            editorField.repaint();
        });
    }

    /**
     * 获取提交面板的编辑器字段
     * <p> 根据提交面板控制对象获取其内部的编辑器字段对象. 如果控制对象为 null, 则直接返回 null.
     *
     * @param commitMessageControl 提交面板的控制对象, 可以为 null
     * @return 编辑器字段对象, 如果控制对象为 null 则返回 null
     */
    private EditorTextField getEditorTextField(@Nullable CommitMessageI commitMessageControl) {
        if (commitMessageControl == null) {
            return null;
        }
        return ((CommitMessage) commitMessageControl).getEditorField();
    }

    /**
     * 重置提交消息的占位符内容
     * <p> 将提交面板编辑器字段的占位符内容设置为空字符串, 用于清除当前显示的占位符文本.
     * todo-dong4j : (2026.01.9 18:57) [无法换行输出]
     *
     * @param commitMessageControl 提交面板的控制对象, 可以为 null
     */
    private void resetCommitMessagePlaceholder(@Nullable CommitMessageI commitMessageControl) {
        setCommitMessagePlaceholder(ChangelogBundle.message("commit.message.placeholder.reset"), commitMessageControl);
    }

    /**
     * 重置提交消息的占位符内容
     * <p> 将指定的占位符文本设置到提交面板编辑器字段中, 用于清除当前显示的占位符文本.
     *
     * @param text                 占位符文本内容, 不能为 null
     * @param commitMessageControl 提交面板的控制对象, 可以为 null
     */
    private void resetCommitMessagePlaceholder(@NotNull String text, @Nullable CommitMessageI commitMessageControl) {
        setCommitMessagePlaceholder(text, commitMessageControl);
    }

    /**
     * 读取提交消息文本
     * <p> 尝试通过反射调用提交面板对象的方法来读取提交消息文本. 支持的方法名包括 "getCommitMessage","getCommitMessageText" 和 "getText".
     *
     * @param commitMessageControl 提交面板的控制对象
     * @return 读取到的提交消息文本, 若读取失败则返回 null
     */
    @Nullable
    private String getCommitMessageText1(@Nullable CommitMessageI commitMessageControl) {
        if (commitMessageControl == null) {
            return null;
        }
        for (String methodName : List.of("getCommitMessage", "getCommitMessageText", "getText")) {
            Method method = findMethod(commitMessageControl.getClass(), methodName);
            if (method != null) {
                try {
                    Object result = method.invoke(commitMessageControl);
                    if (result instanceof String text) {
                        String trimmed = text.trim();
                        return trimmed.isEmpty() ? null : trimmed;
                    }
                } catch (Exception e) {
                    log.debug("Git 提交页面：读取提交消息失败: {}", methodName, e);
                }
            }
        }
        return null;
    }

    /**
     * 读取提交消息文本
     * <p> 从提交面板的编辑器字段中获取当前输入的提交消息文本. 如果提交面板控制对象为 null, 则返回 null; 如果文本为空或仅包含空白字符, 则返回 null.
     *
     * @param commitMessageControl 提交面板的控制对象, 可以为 null
     * @return 读取到的提交消息文本, 若读取失败或文本为空则返回 null
     */
    @Nullable
    private String getCommitMessageText(@Nullable CommitMessageI commitMessageControl) {
        if (commitMessageControl == null) {
            return null;
        }
        final EditorTextField editorField = ((CommitMessage) commitMessageControl).getEditorField();
        final String text = editorField.getText();
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 查找指定类中的方法
     * <p> 根据方法名和参数类型查找目标类中的方法. 如果找到, 则返回该方法对象; 否则返回 null.
     *
     * @param target    目标类
     * @param name      方法名
     * @param paramType 参数类型
     * @return 匹配的方法对象, 如果未找到则返回 null
     */
    @Nullable
    private static Method findMethod(@NotNull Class<?> target, @NotNull String name, @NotNull Class<?> paramType) {
        try {
            return target.getMethod(name, paramType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 查找指定类中的无参方法
     * <p> 根据方法名查找目标类中的方法. 如果找到, 则返回该方法对象; 否则返回 null.
     *
     * @param target 目标类
     * @param name   方法名
     * @return 匹配的方法对象, 如果未找到则返回 null
     */
    @Nullable
    private static Method findMethod(@NotNull Class<?> target, @NotNull String name) {
        try {
            return target.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 检查指定项目的提交记录生成是否正在运行
     * <p> 通过检查全局的生成状态映射表来判断指定项目的提交记录生成任务是否正在进行中.
     *
     * @param project 要检查的项目对象
     * @return 如果提交记录生成任务正在运行, 则返回 true; 否则返回 false
     * @since 1.0.0
     */
    public static boolean isRunning(@NotNull Project project) {
        return GENERATION_STATES.containsKey(project);
    }

    /**
     * 停止指定项目的提交记录生成任务
     * <p> 此方法用于停止正在运行的提交记录生成任务. 如果任务正在运行, 则取消进度指示器并中断相关线程.
     *
     * @param project 要停止的项目对象
     * @since 1.0.0
     */
    public static void stop(@NotNull Project project) {
        GenerationState state = GENERATION_STATES.remove(project);
        if (state == null) {
            return;
        }
        state.cancelled.set(true);
        StreamCancellationToken cancellationToken = state.cancellationToken.get();
        if (cancellationToken != null) {
            cancellationToken.cancel();
        }
        TypingIndicator typingIndicator = state.typingIndicator.get();
        if (typingIndicator != null) {
            typingIndicator.stop();
        }
        ProgressIndicator indicator = state.indicator.get();
        if (indicator != null) {
            indicator.cancel();
        }
        Thread runningThread = state.thread.get();
        if (runningThread != null) {
            runningThread.interrupt();
        }
    }

    /**
     * 生成状态类
     * <p> 用于管理生成过程中的状态信息, 包括取消标志, 进度指示器和线程引用
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.31
     * @since 1.0.0
     */
    private static class GenerationState {
        /** 是否已取消生成过程 */
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        /** 进度指示器引用, 用于在生成过程中更新和访问当前进度状态 */
        private final AtomicReference<ProgressIndicator> indicator = new AtomicReference<>();
        /** 生成线程引用, 用于管理生成任务所在线程 */
        private final AtomicReference<Thread> thread = new AtomicReference<>();
        /**
         * 键入指示器
         * <p> 用于表示当前的键入状态, 支持在生成过程中进行更新和访问
         *
         * @see TypingIndicator
         */
        private final AtomicReference<TypingIndicator> typingIndicator = new AtomicReference<>();
        /** 流式取消令牌 */
        private final AtomicReference<StreamCancellationToken> cancellationToken = new AtomicReference<>();
    }

    /**
     * 启动打字机效果的指示器
     * <p> 创建并启动一个 TypingIndicator 实例, 用于在提交面板或工具窗口中显示生成提交消息的动画效果.
     *
     * @param commitMessageControl 提交面板的控制对象, 可以为 null
     * @param outputSession        工具窗口的输出会话, 可以为 null
     * @return 创建的 TypingIndicator 实例
     */
    private TypingIndicator startTypingIndicator(@Nullable CommitMessageI commitMessageControl,
                                                 @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                                                 boolean canClear) {
        Disposable parentDisposable = commitMessageControl instanceof Disposable disposable ? disposable : null;
        TypingIndicator indicator = new TypingIndicator(commitMessageControl, outputSession, parentDisposable);
        if (commitMessageControl != null || outputSession != null) {
            indicator.start(canClear);
        }
        return indicator;
    }

    /**
     * 输入提示指示器类
     * <p>用于在提交信息输入区域或变更日志工具窗口中显示动态的打字提示效果(如 "正在生成..." + 点状动画), 提升用户体验.
     * <p>该类通过定时器 (Alarm) 周期性更新文本内容, 模拟打字动画效果, 支持启动, 停止和清除状态.
     * <p>主要功能包括:
     * <ul>
     *   <li>初始化时绑定提交消息控制对象和输出会话对象</li>
     *   <li>启动后在 Swing 线程中定时更新文本内容, 显示点状动画</li>
     *   <li>支持停止动画并取消所有定时任务</li>
     *   <li>支持停止并清除文本内容</li>
     * </ul>
     * <p>使用示例:
     * <pre>{@code
     * TypingIndicator indicator = new TypingIndicator(commitControl, outputSession, project);
     * indicator.start();
     * // ... 使用完成后调用 indicator.stop()或 indicator.stopAndClear()
     * }</pre>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.08
     * @since 1.0.0
     */
    private class TypingIndicator {
        /** 定时器服务, 用于调度打字指示器的动画更新 */
        private final Alarm alarm;
        /** 是否已停止打字指示器动画 */
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        /** 提交信息控制对象, 用于在提交消息输入框中显示或更新内容, 可能为 null */
        @Nullable
        private final CommitMessageI commitMessageControl;
        /** ChangeLog 工具窗口输出会话, 用于在界面中显示实时生成的提交信息 */
        @Nullable
        private final ChangelogToolWindowService.ChangelogOutputSession outputSession;
        /** 当前正在显示的点号索引, 用于控制光标闪烁 */
        private int dotIndex = 0;
        /** 已输出的提示内容 */
        private final StringBuilder typedHint = new StringBuilder();
        /** 是否已触发思考阶段 */
        private final AtomicBoolean thinkingStageStarted = new AtomicBoolean(false);
        /** 第一行是否已完成 */
        private final AtomicBoolean analyzingLineCompleted = new AtomicBoolean(false);
        /** 是否由父级管理该定时器的生命周期 */
        private final boolean disposableManagedByParent;

        /** 分析阶段的提示文本, 用于在提交消息中显示“正在分析...”等阶段提示 */
        private final String analyzingText = ChangelogBundle.message("commit.generating.step.analyzing");
        /** 思考阶段的提示文本, 用于在提交消息中显示“正在思考...”等阶段提示 */
        private final String thinkingText = ChangelogBundle.message("commit.generating.step.thinking");
        /** 生成草稿阶段的提示文本 */
        private final String draftingText = ChangelogBundle.message("commit.generating.step.drafting");
        /** 光标闪烁时显示的文本 */
        private final String cursorText = ChangelogBundle.message("commit.generating.cursor");

        /**
         * 初始化打字指示器
         * <p> 创建一个用于在提交消息区域显示打字动画的指示器, 支持在 Swing 线程中定时更新文本内容
         *
         * @param commitMessageControl 提交消息控件对象, 可为 null
         * @param outputSession        输出会话对象, 用于在变更日志工具窗口中显示文本, 可为 null
         * @param parentDisposable     所属项目对象, 不能为空
         */
        TypingIndicator(@Nullable CommitMessageI commitMessageControl,
                        @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                        @Nullable Disposable parentDisposable) {
            this.commitMessageControl = commitMessageControl;
            this.outputSession = outputSession;
            if (parentDisposable != null) {
                this.alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, parentDisposable);
                this.disposableManagedByParent = true;
            } else {
                this.alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
                this.disposableManagedByParent = false;
            }
        }

        /**
         * 启动打字指示器
         * <p> 在 Swing 线程中执行, 清空提交消息文本并设置为空字符串, 如果存在输出会话则也清空其内容, 然后调度下一次打字指示器更新
         */
        void start(boolean canClear) {
            resetState();
            ApplicationManager.getApplication().invokeLater(() -> {
                // 检查项目是否已销毁
                if (project.isDisposed()) {
                    return;
                }
                if (canClear) {
                    setCommitMessageText("", commitMessageControl);
                } else {
                    final EditorTextField editorField = getEditorTextField(commitMessageControl);
                    if (editorField != null) {
                        showContextSettingActionTip(editorField.getComponent());
                    }
                }
                setCommitMessagePlaceholder("", commitMessageControl);
                if (outputSession != null) {
                    outputSession.setPlaceholder("");
                    if (canClear) {
                        outputSession.setText("");
                    }
                }
            });
            startAnalyzingLine();
        }

        /**
         * 停止打字指示器的动画
         * <p> 如果当前状态未停止, 则将状态设置为已停止, 并取消所有计划的任务
         *
         * @since 1.0
         */
        void stop() {
            if (stopped.compareAndSet(false, true)) {
                alarm.cancelAllRequests();
                if (!disposableManagedByParent) {
                    Disposer.dispose(alarm);
                }
            }
        }

        /**
         * 停止并清除输入提示状态
         * <p> 调用 stop 方法停止提示, 并清空与提交消息相关的文本内容
         *
         */
        void generateFailure() {
            stop();
            ApplicationManager.getApplication().invokeLater(() -> {
                // 检查项目是否已销毁
                if (project.isDisposed()) {
                    return;
                }
                resetCommitMessagePlaceholder(
                    ChangelogBundle.message("commit.message.placeholder.reset.error.message"), commitMessageControl);
                if (outputSession != null) {
                    outputSession.setText(ChangelogBundle.message("commit.message.placeholder.reset.error.message"));
                }
            });
        }

        /**
         * 进入思考阶段
         * <p> 当检测到思考内容输出时, 逐字输出阶段提示, 完成后进入光标闪烁状态
         */
        void startThinkingStage() {
            if (!thinkingStageStarted.compareAndSet(false, true)) {
                return;
            }
            if (analyzingLineCompleted.get()) {
                alarm.cancelAllRequests();
                scheduleThinkingLine();
            }
        }

        /**
         * 调度光标闪烁动画
         * <p> 通过定时器周期性更新文本内容, 实现光标闪烁效果.
         * 当已显示提示文本时, 根据 dotIndex 的奇偶性决定是否显示光标:
         * <ul>
         * <li> 偶数索引: 显示光标文本 </li>
         * <li> 奇数索引: 不显示光标 </li>
         * </ul>
         * <p> 每次执行后增加 dotIndex 并递归调度下一次闪烁, 形成循环动画.
         * 如果当前状态已停止, 则不执行任何操作.
         *
         * @since 1.0
         */
        private void startCursorBlink() {
            dotIndex = 0;
            scheduleCursorBlink();
        }

        /**
         * 调度光标闪烁动画
         * <p> 通过定时器周期性更新文本内容, 实现光标闪烁效果. 当已显示提示文本时, 根据 dotIndex 的奇偶性决定是否显示光标:
         * <ul>
         * <li> 偶数索引: 显示光标文本 </li>
         * <li> 奇数索引: 不显示光标 </li>
         * </ul>
         * 每次执行后增加 dotIndex 并递归调度下一次闪烁, 形成循环动画. 如果当前状态已停止, 则不执行任何操作.
         *
         * @since 1.0
         */
        private void scheduleCursorBlink() {
            alarm.addRequest(() -> {
                if (stopped.get()) {
                    return;
                }
                String base = typedHint.toString();
                String text = dotIndex % 2 == 0 ? base + cursorText : base;
                setCommitMessagePlaceholder(text, commitMessageControl);
                if (outputSession != null) {
                    outputSession.setPlaceholder(text);
                }
                dotIndex++;
                scheduleCursorBlink();
            }, TYPING_CURSOR_DELAY_MS);
        }

        /**
         * 更新提示文本内容
         * <p>将当前已生成的提示文本 (typedHint) 显示在提交消息控件和输出会话中, 保持界面同步
         * <p>如果存在提交消息控件, 则更新其内容; 如果存在输出会话, 则同步更新其内容
         *
         * @since 1.0
         */
        private void updateHintText() {
            String text = typedHint.toString();
            setCommitMessagePlaceholder(text, commitMessageControl);
            if (outputSession != null) {
                outputSession.setPlaceholder(text);
            }
        }

        /**
         * 重置打字指示器状态
         * <p> 取消所有计划的定时任务, 清空当前显示的提示文本, 重置思考阶段和分析行完成状态
         *
         * @since 1.0
         */
        private void resetState() {
            alarm.cancelAllRequests();
            dotIndex = 0;
            typedHint.setLength(0);
            thinkingStageStarted.set(false);
            analyzingLineCompleted.set(false);
        }

        /**
         * 开始分析阶段
         * <p> 用于启动分析阶段的文本输出, 当检测到分析内容输出时, 逐字输出分析提示文本, 完成后进入思考阶段.
         *
         * @since 1.0
         */
        private void startAnalyzingLine() {
            typeLine(analyzingText, () -> {
                analyzingLineCompleted.set(true);
                if (thinkingStageStarted.get()) {
                    scheduleThinkingLine();
                    return;
                }
                startCursorBlink();
            });
        }

        /**
         * 调度思考阶段文本的显示
         * <p> 在分析阶段完成后, 使用定时器逐字显示“思考中”提示文本, 并在显示完成后进入草稿阶段
         *
         * @since 1.0.0
         */
        private void scheduleThinkingLine() {
            alarm.addRequest(() -> typeLine(thinkingText, this::scheduleDraftingLine), TYPING_LINE_PAUSE_MS);
        }

        /**
         * 安排起草阶段的文本输出
         * <p> 在 Swing 线程中调度一个请求, 用于逐字输出起草阶段提示文本, 并在完成后启动光标闪烁动画.
         *
         */
        private void scheduleDraftingLine() {
            alarm.addRequest(() -> typeLine(draftingText, this::startCursorBlink), TYPING_LINE_PAUSE_MS);
        }

        /**
         * 逐字输出指定的提示文本行
         * <p> 该方法用于在提交消息区域或变更日志工具窗口中模拟打字效果, 逐字符显示传入的字符串.
         * <p> 当所有字符输出完成后, 执行给定的回调操作. 如果当前状态已停止, 则不会继续执行任何动作.
         *
         * @param line       要逐字输出的提示文本内容
         * @param onComplete 所有字符输出完成后的回调操作, 不可为 null
         * @since 1.0.0
         */
        private void typeLine(@NotNull String line, @NotNull Runnable onComplete) {
            int[] index = {0};
            alarm.addRequest(new Runnable() {
                /**
                 * 模拟打字效果的运行逻辑
                 * <p> 该方法用于逐字符显示文本内容, 每次执行一个字符并更新显示, 直到文本全部显示完毕后执行完成回调
                 * <p> 当文本未完全显示时, 会延迟后再次请求执行自身; 当文本显示完毕后, 调用 onComplete 回调
                 *
                 * @since 1.0
                 */
                @Override
                public void run() {
                    if (stopped.get()) {
                        return;
                    }
                    if (index[0] == 0 && !typedHint.isEmpty()) {
                        typedHint.append('\n');
                    }
                    if (index[0] < line.length()) {
                        typedHint.append(line.charAt(index[0]));
                        index[0]++;
                        updateHintText();
                        alarm.addRequest(this, TYPING_TEXT_DELAY_MS);
                    } else {
                        updateHintText();
                        onComplete.run();
                    }
                }
            }, TYPING_TEXT_DELAY_MS);
        }

    }
}
