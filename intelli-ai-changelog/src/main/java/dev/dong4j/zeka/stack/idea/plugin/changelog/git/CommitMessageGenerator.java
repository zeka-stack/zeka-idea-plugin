package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Alarm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogToolWindowService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.kit.MessageFormatter;
import lombok.extern.slf4j.Slf4j;

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
    /** 输入提示动画的延迟时间 (毫秒), 用于控制打字机效果的节奏 */
    private static final int TYPING_INDICATOR_DELAY_MS = 200;

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
        generateForChanges(changes, null, null);
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
                                   @Nullable Object commitMessageControl) {
        generateForChanges(changes, commitMessageControl, null);
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
                                   @Nullable Object commitMessageControl,
                                   @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession) {
        if (changes.isEmpty()) {
            log.trace("Git 提交页面：没有代码变更需要处理");
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.changes"));
            return;
        }

        GenerationState state = new GenerationState();
        GENERATION_STATES.put(project, state);

        // 在后台任务中执行生成
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, ChangelogBundle.message("commit.generating.progress"), true) {
                /**
                 * 执行提交信息生成任务
                 * <p> 设置进度指示器为不确定状态, 并显示分析更改中的提示信息. 调用 ChangelogService 生成提交信息, 并在 UI 线程中显示结果或错误信息.
                 *
                 * @param indicator 进度指示器, 用于显示任务进度和状态信息
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(true);
                    indicator.setText(ChangelogBundle.message("commit.analyzing.changes"));
                    state.indicator.set(indicator);
                    state.thread.set(Thread.currentThread());
                    TypingIndicator typingIndicator = startTypingIndicator(commitMessageControl, outputSession);
                    state.typingIndicator.set(typingIndicator);
                    StreamCancellationToken cancellationToken = new StreamCancellationToken();
                    state.cancellationToken.set(cancellationToken);

                    try {
                        if (state.cancelled.get()) {
                            return;
                        }
                        String contextText = null;
                        if (SettingsState.getInstance().useCommitMessageInputAsContext) {
                            contextText = getCommitMessageText(commitMessageControl);
                        }
                        ChangelogService service = ChangelogService.getInstance(project);

                        // 多仓库支持：按 VCS Root 分组处理，避免跨仓库混合上下文。
                        Map<String, List<Change>> changesByRoot = groupChangesByRoot(changes);
                        if (changesByRoot.size() > 1) {
                            handleMultiRepositoryChanges(service,
                                                         changesByRoot,
                                                         contextText,
                                                         outputSession,
                                                         commitMessageControl,
                                                         typingIndicator);
                            return;
                        }

                        StringBuilder buffer = new StringBuilder();
                        AtomicReference<Boolean> updated = new AtomicReference<>(false);
                        final AIStreamResponseListener listener = getStreamResponseListener(buffer,
                                                                                            typingIndicator,
                                                                                            updated,
                                                                                            cancellationToken);

                        // 流式生成并同步返回最终结果
                        String commitMessage = service.generateCommitMessageFromDiffStream(changes, listener, contextText);
                        String formattedCommitMessage = MessageFormatter.format(commitMessage);

                        // 在 EDT 中显示结果
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // 直接写入提交面板，避免弹窗打断提交流程
                            boolean applied = !state.cancelled.get()
                                              && setCommitMessageText(formattedCommitMessage, commitMessageControl);
                            if (!applied && !updated.get()) {
                                log.trace("Git 提交页面：提交面板不可用，无法写入提交记录");
                            }
                            if (outputSession != null) {
                                outputSession.setText(formattedCommitMessage);
                            }
                        });

                        log.trace("Git 提交页面：提交记录生成成功");
                    } catch (Exception e) {
                        log.trace("Git 提交页面：生成提交记录失败", e);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            typingIndicator.stopAndClear();
                            String errorMessage = e.getMessage();
                            if (errorMessage != null && !errorMessage.isEmpty()) {
                                NotificationUtil.showError(project, errorMessage);
                            } else {
                                NotificationUtil.showError(project,
                                                           ChangelogBundle.message("commit.generation.error",
                                                                                   ChangelogBundle.message("error.ai.service.unknown")));
                            }
                        });
                    } finally {
                        typingIndicator.stop();
                        GENERATION_STATES.remove(project);
                    }
                }

                /**
                 * 创建 AI 流式响应监听器
                 * <p> 创建一个用于处理 AI 响应流的监听器, 该监听器会实时更新提交消息文本
                 * <p> 监听器包含三个回调方法:
                 * <ul>
                 *   <li>onStart: 启动时清空缓冲区 </li>
                 *   <li>onChunk: 处理接收到的文本块并实时更新 </li>
                 *   <li>onComplete: 在异步操作完成后更新提交消息 </li>
                 * </ul>
                 *
                 * @param buffer          用于累积文本的缓冲区, 不能为 null
                 * @param typingIndicator 打字指示器, 用于显示和停止打字动画效果, 不能为 null
                 * @param updated         原子引用, 用于跟踪提交消息文本是否已更新, 不能为 null
                 * @return AIStreamResponseListener 实例, 用于监听 AI 响应的流式事件
                 */
                private @NotNull AIStreamResponseListener getStreamResponseListener(StringBuilder buffer,
                                                                                    TypingIndicator typingIndicator,
                                                                                    AtomicReference<Boolean> updated,
                                                                                    StreamCancellationToken cancellationToken) {
                    AtomicBoolean contentStarted = new AtomicBoolean(false);
                    // 流式回调中实时写入提交面板，保证内容可见且可编辑
                    return new AIStreamResponseListener() {
                        /**
                         * 在监听器启动时调用
                         * <p> 清空缓冲区并将提交消息文本设置为空
                         *
                         * @since 1.0
                         */
                        @Override
                        public void onStart() {
                            buffer.setLength(0);
                        }

                        @Override
                        public @Nullable StreamCancellationToken cancellationToken() {
                            return cancellationToken;
                        }

                        /**
                         * 处理接收到的文本块
                         * <p> 将接收到的文本块追加到缓冲区, 并在事件调度线程中更新提交消息文本
                         *
                         * @param chunk 接收到的文本块
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
                                if (setCommitMessageText(buffer.toString(), commitMessageControl)) {
                                    updated.set(true);
                                }
                            });
                            if (outputSession != null) {
                                outputSession.append(chunk);
                            }
                        }

                        /**
                         * 在异步操作完成后更新提交消息文本
                         * <p> 此方法在异步操作完成后被调用, 用于更新提交消息文本. 如果更新成功, 则将 updated 标志设置为 true.
                         *
                         * @param fullText 完整的文本内容
                         */
                        @Override
                        public void onComplete(@NotNull String fullText) {
                            if (state.cancelled.get()) {
                                return;
                            }
                            if (!fullText.isBlank() && contentStarted.compareAndSet(false, true)) {
                                typingIndicator.stop();
                            } else if (fullText.isBlank()) {
                                typingIndicator.stop();
                            }
                            ApplicationManager.getApplication().invokeLater(() -> {
                                if (setCommitMessageText(fullText, commitMessageControl)) {
                                    updated.set(true);
                                }
                            });
                            if (outputSession != null) {
                                outputSession.setText(fullText);
                            }
                        }
                    };
                }
            });
    }

    /**
     * 按 VCS Root 对变更分组
     * <p> 多仓库场景下用于拆分生成上下文，避免跨仓库混合导致的噪音。
     *
     * @param changes 变更集合
     * @return key 为仓库根路径的分组 Map
     */
    @NotNull
    private Map<String, List<Change>> groupChangesByRoot(@NotNull Collection<Change> changes) {
        ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
        Map<String, List<Change>> grouped = new LinkedHashMap<>();
        for (Change change : changes) {
            VirtualFile file = change.getVirtualFile();
            VirtualFile root = file != null ? vcsManager.getVcsRootFor(file) : null;
            String rootKey = root != null ? root.getPresentableUrl() : ChangelogBundle.message("commit.multi.repo.unknown");
            grouped.computeIfAbsent(rootKey, key -> new java.util.ArrayList<>()).add(change);
        }
        return grouped;
    }

    /**
     * 处理多仓库变更
     * <p> 在工具窗口输出每个仓库的提交建议，并提示用户拆分提交。
     *
     * @param service       生成服务
     * @param changesByRoot 按仓库分组的变更
     * @param contextText   用户补充说明
     * @param outputSession 可能存在的输出会话
     * @throws Exception 生成失败时抛出
     */
    private void handleMultiRepositoryChanges(@NotNull ChangelogService service,
                                              @NotNull Map<String, List<Change>> changesByRoot,
                                              @Nullable String contextText,
                                              @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                                              @Nullable Object commitMessageControl,
                                              @NotNull TypingIndicator typingIndicator) throws Exception {
        String repoList = String.join(", ", changesByRoot.keySet());
        NotificationUtil.showWarning(project,
                                     ChangelogBundle.message("commit.multi.repo.detected",
                                                             changesByRoot.size(),
                                                             repoList));

        // 多仓库场景：为每个仓库生成独立的 commit message，再合并输出。
        List<String> commitMessages = new java.util.ArrayList<>();
        for (Map.Entry<String, List<Change>> entry : changesByRoot.entrySet()) {
            List<Change> rootChanges = entry.getValue();
            if (rootChanges.isEmpty()) {
                continue;
            }
            String commitMessage = service.generateCommitMessageFromDiff(rootChanges, contextText);
            String formattedCommitMessage = MessageFormatter.format(commitMessage);
            if (!formattedCommitMessage.isBlank()) {
                commitMessages.add(formattedCommitMessage.trim());
            }
        }

        // 合并输出，避免引入仓库标题，保持类似「多条 commit message」的展示格式。
        String combined = String.join("\n\n", commitMessages);

        if (!combined.isBlank()) {
            typingIndicator.stop();
            // 输出到工具窗口，便于复制与审阅。
            ChangelogToolWindowService.ChangelogOutputSession session = outputSession;
            if (session == null) {
                session = ChangelogToolWindowService.getInstance(project)
                    .openSession(ChangelogBundle.message("commit.multi.repo.session.title"));
            }
            session.setText(combined);

            // 若提交面板可用，同步写入多条 message，方便用户直接复制。
            setCommitMessageText(combined, commitMessageControl);
        }
    }

    /**
     * 设置提交消息文本
     * <p> 尝试通过反射调用提交面板对象的方法来设置提交消息文本. 支持的方法名包括 "setCommitMessage","setCommitMessageText" 和 "setText".
     *
     * @param commitMessage        提交消息文本
     * @param commitMessageControl 提交面板的控制对象
     * @return 如果成功设置提交消息文本, 则返回 true; 否则返回 false
     */
    private boolean setCommitMessageText(@NotNull String commitMessage, @Nullable Object commitMessageControl) {
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
                    log.trace("Git 提交页面：调用提交面板方法失败: {}", methodName, e);
                }
            }
        }
        return false;
    }

    /**
     * 读取提交消息文本
     * <p> 尝试通过反射调用提交面板对象的方法来读取提交消息文本. 支持的方法名包括 "getCommitMessage","getCommitMessageText" 和 "getText".
     *
     * @param commitMessageControl 提交面板的控制对象
     * @return 读取到的提交消息文本, 若读取失败则返回 null
     */
    @Nullable
    private String getCommitMessageText(@Nullable Object commitMessageControl) {
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
                    log.trace("Git 提交页面：读取提交消息失败: {}", methodName, e);
                }
            }
        }
        return null;
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
    private TypingIndicator startTypingIndicator(@Nullable Object commitMessageControl,
                                                 @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession) {
        TypingIndicator indicator = new TypingIndicator(commitMessageControl, outputSession, project);
        if (commitMessageControl != null || outputSession != null) {
            indicator.start();
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
        private final Object commitMessageControl;
        /** ChangeLog 工具窗口输出会话, 用于在界面中显示实时生成的提交信息 */
        @Nullable
        private final ChangelogToolWindowService.ChangelogOutputSession outputSession;
        /** 当前正在显示的点号索引, 用于控制 typing 指示器的动画效果 */
        private int dotIndex = 0;
        /**
         * 提交生成时的打字指示文本基础内容
         * <p> 该字符串用于在提交消息中显示打字指示符
         *
         * @see ChangelogBundle
         */
        private final String baseText = ChangelogBundle.message("commit.generating.typing");

        /**
         * 初始化打字指示器
         * <p> 创建一个用于在提交消息区域显示打字动画的指示器, 支持在 Swing 线程中定时更新文本内容
         *
         * @param commitMessageControl 提交消息控件对象, 可为 null
         * @param outputSession        输出会话对象, 用于在变更日志工具窗口中显示文本, 可为 null
         * @param project              所属项目对象, 不能为空
         */
        TypingIndicator(@Nullable Object commitMessageControl,
                        @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession,
                        @NotNull Project project) {
            this.commitMessageControl = commitMessageControl;
            this.outputSession = outputSession;
            this.alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
        }

        /**
         * 启动打字指示器
         * <p> 在 Swing 线程中执行, 清空提交消息文本并设置为空字符串, 如果存在输出会话则也清空其内容, 然后调度下一次打字指示器更新
         *
         */
        void start() {
            ApplicationManager.getApplication().invokeLater(() -> {
                setCommitMessageText("", commitMessageControl);
                if (outputSession != null) {
                    outputSession.setText("");
                }
            });
            scheduleNext();
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
            }
        }

        /**
         * 停止并清除输入提示状态
         * <p> 调用 stop 方法停止提示, 并清空与提交消息相关的文本内容
         *
         */
        void stopAndClear() {
            stop();
            ApplicationManager.getApplication().invokeLater(() -> {
                setCommitMessageText("", commitMessageControl);
                if (outputSession != null) {
                    outputSession.setText("");
                }
            });
        }

        /**
         * 安排下一次输入指示器的更新任务
         * <p> 此方法通过 Alarm 在指定延迟后执行, 用于周期性地更新提交消息控件和输出会话中的文本, 显示动态的“正在生成...”效果
         * <p> 每次调用时会根据当前的 dotIndex 生成不同数量的点符号 (最多 4 个), 并更新到界面上. 当停止标志为 true 时, 将不再继续调度
         *
         */
        private void scheduleNext() {
            alarm.addRequest(() -> {
                if (stopped.get()) {
                    return;
                }
                String dots = ".".repeat(dotIndex);
                String text = baseText + " " + dots;
                setCommitMessageText(text, commitMessageControl);
                if (outputSession != null) {
                    outputSession.setText(text);
                }
                dotIndex = (dotIndex + 1) % 5;
                scheduleNext();
            }, TYPING_INDICATOR_DELAY_MS);
        }
    }
}
