package dev.dong4j.zeka.stack.idea.plugin.changelog.git;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.event.HyperlinkEvent;

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
    /** 输入提示动画的文本间隔 (毫秒), 用于控制逐字输出速度 */
    private static final int TYPING_TEXT_DELAY_MS = 45;
    /** 行与行之间的停顿时间 (毫秒) */
    private static final int TYPING_LINE_PAUSE_MS = 1000;
    /** 光标闪烁间隔 (毫秒) */
    private static final int TYPING_CURSOR_DELAY_MS = 500;

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
                                   @Nullable CommitMessageI commitMessageControl) {
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
                                   @Nullable CommitMessageI commitMessageControl,
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

                    String contextText = null;
                    if (SettingsState.getInstance().useCommitMessageInputAsContext) {
                        contextText = getCommitMessageText(commitMessageControl);
                    }

                    // 如果 contextText 不为 null, 则不能清空
                    TypingIndicator typingIndicator = startTypingIndicator(commitMessageControl, outputSession, contextText == null);
                    state.typingIndicator.set(typingIndicator);
                    StreamCancellationToken cancellationToken = new StreamCancellationToken();
                    state.cancellationToken.set(cancellationToken);

                    try {
                        if (state.cancelled.get()) {
                            return;
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
                            // 检查项目是否已销毁
                            if (project.isDisposed() || state.cancelled.get()) {
                                log.trace("项目已销毁或任务已取消，跳过设置提交消息");
                                return;
                            }

                            // 直接写入提交面板，避免弹窗打断提交流程
                            boolean applied = setCommitMessageText(formattedCommitMessage, commitMessageControl, true);
                            if (!applied && !updated.get()) {
                                log.trace("Git 提交页面：提交面板不可用，无法写入提交记录");
                            }

                            if (outputSession != null && !project.isDisposed()) {
                                outputSession.setText(formattedCommitMessage);
                            }
                        });

                        log.trace("Git 提交页面：提交记录生成成功");
                    } catch (Exception e) {
                        log.trace("Git 提交页面：生成提交记录失败", e);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // 检查项目是否已销毁
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
                        // 重设提示语
                        resetCommitMessagePlaceholder(commitMessageControl);
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

                        /**
                         * 获取与此监听器关联的流取消令牌
                         * <p> 返回用于控制流操作取消行为的令牌对象, 如果未设置则返回 null
                         *
                         * @return 流取消令牌, 可能为 null
                         * @since 1.0
                         */
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
                                // 检查项目是否已销毁
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
                         * 处理接收到的思考阶段文本块
                         * <p> 当接收到思考阶段的文本块时, 若未取消操作且文本非空, 则启动思考状态指示器
                         *
                         * @param chunk 接收到的文本块
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
                                // 检查项目是否已销毁
                                if (project.isDisposed() || state.cancelled.get()) {
                                    return;
                                }
                                if (setCommitMessageText(fullText, commitMessageControl, true)) {
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
    private void showActionTip(@NotNull JComponent component) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(project);
        String shownKey = "changelog.commit.context.tip.shown";
        String versionKey = "changelog.commit.context.tip.version";

        // 获取当前插件版本号
        String currentVersion = dev.dong4j.zeka.stack.idea.plugin.kit.PluginUtil.getVersion(PluginContents.PLUGIN_ID);
        if (currentVersion == null) {
            log.trace("无法获取插件版本号，跳过版本检查");
            // 如果无法获取版本号，使用原有逻辑（每个项目只显示一次）
            if (propertiesComponent.getBoolean(shownKey, false)) {
                log.trace("提交上下文提示已显示过，跳过");
                return;
            }
            propertiesComponent.setValue(shownKey, true);
        } else {
            // 获取存储的版本号
            String storedVersion = propertiesComponent.getValue(versionKey);

            // 如果版本号不一致（说明是升级），重置已显示标记
            if (storedVersion != null && !storedVersion.equals(currentVersion)) {
                log.trace("检测到插件版本升级：{} -> {}，重置提示状态", storedVersion, currentVersion);
                propertiesComponent.unsetValue(shownKey);
            }

            // 检查是否已经显示过提示（每个版本只显示一次）
            if (propertiesComponent.getBoolean(shownKey, false)) {
                log.trace("提交上下文提示已显示过（版本：{}），跳过", currentVersion);
                return;
            }

            // 标记为已显示，并保存当前版本号
            propertiesComponent.setValue(shownKey, true);
            propertiesComponent.setValue(versionKey, currentVersion);
        }
        // 创建超链接监听器, 处理"关闭"链接的点击事件
        HyperlinkAdapter linkListener = new HyperlinkAdapter() {
            /**
             * 处理超链接激活事件
             * <p> 当用户点击超链接时触发, 若链接描述为 "action:close", 则关闭提交信息输入框作为上下文功能, 并保存设置
             * <p> 示例:
             * <pre>{@code
             * // 当用户点击关闭按钮时, 将设置 useCommitMessageInputAsContext 置为 false 并保存
             * hyperlinkActivated(event); // event.getDescription() 返回 "action:close"
             * }</pre>
             *
             * @param e 超链接事件对象, 不能为 null
             */
            @Override
            protected void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                String url = e.getDescription();
                // 处理 action:close 链接
                if ("action:close".equals(url)) {
                    // 关闭"使用提交输入作为上下文"设置并持久化
                    SettingsState settings = SettingsState.getInstance();
                    settings.useCommitMessageInputAsContext = false;
                    ApplicationManager.getApplication().saveSettings();
                }
            }
        };

        final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                ChangelogBundle.message("commit.context.tip.enabled"),
                MessageType.INFO,
                linkListener)
            .setFadeoutTime(5000)
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
     * 通过 PluginManagerCore 获取插件的版本信息。
     *
     * @return 插件版本号，如果获取失败则返回 null
     */
    @Nullable
    private String getCurrentPluginVersion() {
        try {
            PluginId pluginId = PluginId.getId(PluginContents.PLUGIN_ID);
            IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);
            if (pluginDescriptor != null) {
                return pluginDescriptor.getVersion();
            }
        } catch (Exception e) {
            log.trace("获取插件版本号失败", e);
        }
        return null;
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
    private Map<String, List<Change>> groupChangesByRoot(@NotNull Collection<Change> changes) {
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
                                              @Nullable CommitMessageI commitMessageControl,
                                              @NotNull TypingIndicator typingIndicator) throws Exception {
        // 提取每个仓库路径的最后一级目录名，兼容 Windows 和 Unix 系统
        String repoList = changesByRoot.keySet().stream()
            .map(path -> new java.io.File(path).getName())
            .collect(java.util.stream.Collectors.joining(", "));

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
            // 必须在 EDT 中执行 UI 操作
            ApplicationManager.getApplication().invokeLater(() -> {
                // 检查项目是否已销毁
                if (project.isDisposed()) {
                    return;
                }
                setCommitMessagePlaceholder(ChangelogBundle.message("commit.multi.repo.placeholder",
                                                                    changesByRoot.size()), commitMessageControl);
            });
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
                    log.trace("Git 提交页面：调用提交面板方法失败: {}", methodName, e);
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
            log.trace("项目已销毁，跳过设置提交消息文本");
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
                log.trace("项目已销毁，无法设置提交消息文本", e);
            } else {
                log.trace("设置提交消息文本失败", e);
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
            log.trace("项目已销毁，跳过通知生成完成状态");
            return;
        }

        try {
            // 获取 EditorTextField
            EditorTextField editorField = getEditorTextField(commitMessageControl);
            if (editorField == null) {
                return;
            }

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
            log.trace("通知生成完成状态失败", e);
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
                    log.trace("Git 提交页面：读取提交消息失败: {}", methodName, e);
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
                        showActionTip(editorField.getComponent());
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
