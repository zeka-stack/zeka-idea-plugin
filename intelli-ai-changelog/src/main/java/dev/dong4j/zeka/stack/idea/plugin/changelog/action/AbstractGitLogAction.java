package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLogCommitSelection;
import com.intellij.vcs.log.VcsLogDataKeys;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogToolWindowService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ToolWindowTitleUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.kit.MessageFormatter;
import icons.ChangelogIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象 Git 日志操作类
 * <p>
 * 提供 Git 日志相关操作的基类实现, 用于在 IDE 中执行与 Git 提交记录相关的动作, 如生成变更日志, 显示结果等.
 * 该类定义了子类必须实现的抽象方法, 并提供了通用的 UI 更新和操作执行逻辑.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractGitLogAction extends AnAction {

    /**
     * 获取 Action 的文本资源键
     *
     * @return 文本资源键
     */
    @NotNull
    protected abstract String getTextKey();

    /**
     * 获取 Action 的描述资源键
     *
     * @return 描述资源键
     */
    @NotNull
    protected abstract String getDescriptionKey();

    /**
     * 获取 Action 的图标
     *
     * @return 图标对象
     */
    @NotNull
    protected Icon getIcon() {
        return ChangelogIcons.CHANGELOG_16;
    }

    /**
     * 获取进度标题的资源键
     *
     * @return 进度标题的资源键
     */
    @NotNull
    protected String getProgressTitleKey() {
        return "";
    }

    /**
     * 获取进度文本的资源键
     *
     * @return 进度文本的资源键
     */
    @NotNull
    protected String getProgressTextKey() {
        return "";
    }

    /**
     * 获取错误消息的资源键
     *
     * @return 错误消息的资源键
     */
    @NotNull
    protected String getErrorKey() {
        return "";
    }

    /**
     * 生成内容
     * <p>
     * 子类实现此方法来调用 ChangelogService 的相应方法生成内容。
     *
     * @param service      ChangelogService 实例
     * @param commitHashes 提交记录的 hash 列表
     * @return 生成的内容
     * @throws Exception 生成过程中可能发生的异常
     */
    @NotNull
    protected String generateContent(@NotNull ChangelogService service, @NotNull List<String> commitHashes) throws Exception {
        return "";
    }

    /**
     * 流式生成内容
     * <p>
     * 子类可重写此方法以启用 AI 流式输出. 默认实现会退化为一次性生成.
     *
     * @param service      ChangelogService 实例
     * @param commitHashes 提交记录 hash 列表
     * @param listener     流式监听器
     * @return 生成的内容
     * @throws Exception 生成过程中可能发生的异常
     */
    @NotNull
    protected String generateContentStream(@NotNull ChangelogService service,
                                           @NotNull List<String> commitHashes,
                                           @NotNull AIStreamResponseListener listener) throws Exception {
        String content = generateContent(service, commitHashes);
        listener.onStart();
        listener.onChunk(content);
        listener.onComplete(content);
        return content;
    }

    /**
     * 更新动作状态
     * <p>
     * 检查是否有选中的提交记录，如果有则启用按钮，否则禁用。
     * 同时设置按钮的文本、描述和图标。
     *
     * @param e 动作事件
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        // 检查项目是否处于索引模式
        if (project != null && DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        VcsLogCommitSelection selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION);

        // 设置按钮文本、描述和图标
        e.getPresentation().setText(ChangelogBundle.message(getTextKey()));
        e.getPresentation().setDescription(ChangelogBundle.message(getDescriptionKey()));
        e.getPresentation().setIcon(getIcon());

        // 只有在 Git Log 工具窗口中有选中提交时才启用
        boolean enabled = project != null && selection != null;
        if (enabled) {
            List<VcsFullCommitDetails> selectedCommits = selection.getCachedFullDetails();
            enabled = !selectedCommits.isEmpty();
        }

        e.getPresentation().setEnabled(enabled);
    }

    /**
     * 执行动作
     * <p>
     * 模板方法，提供统一的执行流程：
     * 1. 获取选中的提交记录
     * 2. 在后台任务中生成内容
     * 3. 显示结果对话框或错误提示
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 检查项目是否处于索引模式
        if (DumbService.isDumb(project)) {
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.indexing.warning"));
            return;
        }

        // 获取选中的提交记录
        VcsLogCommitSelection selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION);
        if (selection == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.git.log"));
            return;
        }

        List<VcsFullCommitDetails> selectedCommits = selection.getCachedFullDetails();
        if (selectedCommits.isEmpty()) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.commits.selected"));
            return;
        }

        // 获取选中的提交记录 hash
        List<String> selectedHashes = new ArrayList<>();
        for (VcsFullCommitDetails commit : selectedCommits) {
            selectedHashes.add(commit.getId().asString());
        }

        // 创建工具窗口输出会话，便于流式输出与复制
        // 标题格式为：简称:时间戳（例如：CM:14:30:25）
        String toolWindowTitle = ToolWindowTitleUtil.buildToolWindowTitle(getTextKey());
        String startPoint = selectedHashes.get(0);
        String endPoint = selectedHashes.get(selectedHashes.size() - 1);
        String provider = resolveProviderText();
        ChangelogToolWindowService.ChangelogOutputSession outputSession =
            ChangelogToolWindowService.getInstance(project).openSession(toolWindowTitle, startPoint, endPoint, provider);

        // 在后台任务中生成内容
        String progressTitle = ChangelogBundle.message(getProgressTitleKey());
        ProgressManager.getInstance().run(new Task.Backgroundable(project, progressTitle, true) {
            /**
             * 执行变更日志生成任务.
             * <p> 该方法在进度指示器上设置为不确定进度, 并显示相应文本. 随后尝试使用 {@link ChangelogService} 生成变更日志内容.
             * 如果生成过程中出现异常, 则在 UI 线程中通过 {@link NotificationUtil} 显示错误通知.
             *
             * @param indicator 用于显示任务进度的进度指示器
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // 检查 AI Provider 配置
                AIProviderConfig config = SettingsState.getInstance().providerConfig;
                if (!AIProviderUtils.hasAIProvider(project, config, ChangelogBundle.message("settings.display.name"),
                                                   ChangelogBundle.message("settings.ai.provider.selection"))) {
                    return;
                }

                indicator.setIndeterminate(true);
                indicator.setText(ChangelogBundle.message(getProgressTextKey()));

                try {
                    ChangelogService service = ChangelogService.getInstance(project);
                    StreamCancellationToken cancellationToken = new StreamCancellationToken();
                    outputSession.bindCancellationToken(cancellationToken);

                    AIStreamResponseListener listener = new AIStreamResponseListener() {
                        /**
                         * 在活动开始时清空输出会话文本
                         * <p> 该方法在活动启动时被调用, 用于重置或清空输出会话中的文本内容, 确保用户界面显示最新状态.
                         *
                         * @since 1.0
                         */
                        @Override
                        public void onStart() {
                            if (outputSession.isCancelled()) {
                                return;
                            }
                            outputSession.setText("");
                        }

                        /**
                         * 获取流式取消令牌
                         * <p> 返回用于控制流式输出取消的令牌对象
                         *
                         * @return 流式取消令牌
                         */
                        @Override
                        public @NotNull StreamCancellationToken cancellationToken() {
                            return cancellationToken;
                        }

                        /**
                         * 处理接收到的文本块数据
                         * <p> 当接收到一个文本块时, 将其追加到输出会话中
                         *
                         * @param chunk 要追加的文本块, 不能为 null
                         */
                        @Override
                        public void onChunk(@NotNull String chunk) {
                            if (outputSession.isCancelled()) {
                                return;
                            }
                            outputSession.append(chunk);
                        }

                        /**
                         * 处理完整的文本内容并完成输出会话
                         * <p> 当接收到完整的文本内容时, 对其进行格式化处理, 并将结果传递给输出会话以完成当前会话.
                         *
                         * @param fullText 完整的文本内容, 不能为空
                         */
                        @Override
                        public void onComplete(@NotNull String fullText) {
                            if (outputSession.isCancelled()) {
                                return;
                            }
                            String formattedText = MessageFormatter.format(fullText);
                            outputSession.complete(formattedText);
                        }

                        /**
                         * 处理提示信息
                         * <p> 将提示信息输出到日志, 供 UI 后续使用
                         *
                         * @param message 提示信息内容, 不能为空
                         */
                        @Override
                        public void onNotice(@NotNull String message) {
                            if (outputSession.isCancelled()) {
                                return;
                            }
                            // todo-dong4j : (2026.01.11 04:25) [输入到 UI]
                            log.debug("Changelog notice: {}", message);
                        }
                    };

                    generateContentStream(service, selectedHashes, listener);
                } catch (Exception e) {
                    // 在 EDT 中显示错误提示
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project,
                                                   ChangelogBundle.message(getErrorKey(), e.getMessage()));
                    });
                }
            }
        });
    }

    /**
     * 获取动作更新线程
     *
     * <p>在后台线程中执行更新操作，避免阻塞事件调度线程(EDT)。
     * 因为需要访问 VCS 数据（VcsLog），在后台线程中执行更安全。
     *
     * @return ActionUpdateThread.BGT 后台线程
     * @see ActionUpdateThread#BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 在后台线程中执行 update，避免阻塞 EDT
        return ActionUpdateThread.BGT;
    }

    /**
     * 构建当前 AI Provider 信息
     *
     * @return provider 文本
     */
    private @NotNull String resolveProviderText() {
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (config == null || config.providerType == null) {
            return "";
        }
        AIProviderType providerType = config.providerType;
        String providerName = providerType.getDisplayName();
        String modelName = config.modelName != null ? config.modelName.trim() : "";
        if (!modelName.isEmpty()) {
            return providerName + "(" + modelName + ")";
        }
        return providerName;
    }
}
