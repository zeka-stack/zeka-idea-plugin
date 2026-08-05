package dev.dong4j.zeka.stack.idea.plugin.changelog.service;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLogCommitSelection;
import com.intellij.vcs.log.VcsLogDataKeys;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Point;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.CommitMessageGenerator;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.ChangelogIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 提交信息生成服务类
 * <p>用于在 IntelliJ IDEA 插件中提供智能提交信息的生成与更新功能, 支持从 Git 提交面板或日志面板中根据文件变更或提交记录自动生成提交信息. 该服务通过异步线程更新操作按钮状态, 并在用户触发时调用 AI 服务生成提交消息,
 * 同时提供状态提示与用户交互反馈.</p>
 * <p>本服务为单例模式, 通过 {@code ApplicationManager.getApplication().getService()} 获取实例, 适用于插件中需要全局访问提交信息生成逻辑的场景.</p>
 * <p>主要功能包括:</p>
 * <ul>
 *   <li>根据当前项目状态 (如是否处于索引中, 是否已运行生成任务) 动态更新操作按钮的可见性与状态</li>
 *   <li>支持从提交面板中选择文件变更并生成对应提交信息</li>
 *   <li>支持从 Git 日志面板中选择单个或多个提交记录并生成对应提交信息</li>
 *   <li>提供用户提示弹窗, 用于引导用户选择变更内容或提示索引状态</li>
 * </ul>
 * <p>依赖组件包括: 项目上下文,VCS 提交控制,AI 提供商配置, 提交工作流处理器等.</p>
 * <p>本服务使用 {@code @Slf4j} 日志注解记录关键操作, 如提交生成开始, 未选择变更, 提交哈希处理等.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
@Slf4j
@Service(Service.Level.APP)
public final class GenerateCommitMessageService {

    /**
     * 获取当前应用中 {@code GenerateCommitMessageService} 的单例实例
     * <p> 通过 IntelliJ 平台的应用程序管理器获取该服务的实例, 确保全局唯一性 </p>
     *
     * @return 非空的 {@code GenerateCommitMessageService} 实例
     */
    public static @NotNull GenerateCommitMessageService getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(GenerateCommitMessageService.class);
    }

    /**
     * 获取动作更新线程的执行上下文
     * <p> 返回当前服务所使用的动作更新线程类型, 此处固定返回后台线程 (BGT)</p>
     *
     * @return 动作更新线程类型, 始终为 {@link ActionUpdateThread#BGT}
     */
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 更新动作的界面显示状态
     * <p> 根据当前项目状态, 索引状态以及提交消息生成器是否正在运行, 动态调整动作的文本, 图标, 描述和启用 / 可见状态.</p>
     * <p> 当项目已销毁或处于索引中时, 动作将被禁用但保持可见; 当生成器正在运行时, 动作显示为“停止”按钮; 否则显示为“生成”按钮.</p>
     *
     * @param e 动作事件对象, 包含当前上下文信息, 如项目, 组件等
     */
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
            return;
        }

        if (DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(true);
            return;
        }

        if (CommitMessageGenerator.isRunning(project)) {
            String text = ChangelogBundle.message("commit.action.stop.text");
            e.getPresentation().setText(text);
            e.getPresentation().setDescription(text);
            e.getPresentation().setIcon(AllIcons.Process.Stop);
            e.getPresentation().putClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP, null);
        } else {
            String title = ChangelogBundle.message("commit.action.text");
            String description = ChangelogBundle.message("commit.action.description");
            e.getPresentation().setText("");
            e.getPresentation().setDescription(description);
            e.getPresentation().setIcon(ChangelogIcons.CHANGELOG_16);
            e.getPresentation().putClientProperty(ActionButton.CUSTOM_HELP_TOOLTIP,
                                                  new HelpTooltip().setTitle(title).setDescription(description));
        }

        e.getPresentation().setEnabled(true);
        e.getPresentation().setVisible(true);
    }

    /**
     * 处理动作事件, 根据当前上下文执行提交消息生成逻辑
     * <p> 该方法在用户点击提交消息生成按钮时被调用, 支持从 Git 提交页面或提交日志页面生成提交信息.
     * 若当前项目处于索引中或 AI 提供商未配置, 则会显示提示信息并终止操作.
     * 若用户在提交页面中选择了文件变更, 则调用生成器处理这些变更; 若在日志页面中选择了提交记录,
     * 则根据提交哈希或提交消息生成对应提交信息.</p>
     *
     * @param e 动作事件对象, 包含当前操作的上下文信息
     * @since 1.0
     */
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        if (DumbService.isDumb(project)) {
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.indexing.warning"));
            return;
        }

        if (CommitMessageGenerator.isRunning(project)) {
            CommitMessageGenerator.stop(project);
            return;
        }

        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (!AIProviderUtils.hasAIProvider(project,
                                           config,
                                           ChangelogBundle.message("settings.display.name"),
                                           ChangelogBundle.message("settings.ai.provider.selection"))) {
            return;
        }

        CommitMessageI commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        CommitMessageGenerator generator = new CommitMessageGenerator(project);

        // 2025.3+ 可能只有 COMMIT_WORKFLOW_UI, handler.getUi() 已从接口移除; 用 hasCommitWorkflow 判断提交面板上下文。
        if (dev.dong4j.zeka.stack.idea.plugin.kit.CommitUtil.hasCommitWorkflow(e.getDataContext())) {
            log.debug("Git 提交页面：开始生成提交记录");
            Collection<Change> changes = dev.dong4j.zeka.stack.idea.plugin.kit.CommitUtil.getSelectedChanges(e);
            if (changes.isEmpty()) {
                log.debug("Git 提交页面：未选择任何文件变更");
                showActionTip(e, ChangelogBundle.message("commit.no.selected.changes"));
                return;
            }

            log.debug("Git 提交页面：找到 {} 个文件变更", changes.size());
            generator.generateForChanges(changes, commitMessageControl, null, StatisticsUserAction.COMMIT_PANEL);
            return;
        }

        VcsLogCommitSelection selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION);
        if (selection == null) {
            showActionTip(e, ChangelogBundle.message("commit.regenerate.select.at.least.one.commit"));
            return;
        }
        List<VcsFullCommitDetails> commits = selection.getCachedFullDetails();
        if (commits.isEmpty()) {
            showActionTip(e, ChangelogBundle.message("commit.regenerate.select.at.least.one.commit"));
            return;
        }

        if (commits.size() == 1) {
            String commitHash = commits.get(0).getId().asString();
            generator.generateForCommitHash(commitHash, commitMessageControl, null, StatisticsUserAction.GIT_LOG_PANEL);
            return;
        }

        List<String> commitHashes = commits.stream().map(it -> it.getId().asString()).collect(Collectors.toList());
        List<String> commitMessages = commits.stream()
            .map(it -> {
                String fullMessage = it.getFullMessage();
                int idx = fullMessage.indexOf('\n');
                return (idx >= 0 ? fullMessage.substring(0, idx) : fullMessage).trim();
            })
            .filter(it -> !it.isBlank())
            .collect(Collectors.toList());
        generator.generateForCommitHashes(commitHashes,
                                          commitMessages,
                                          commitMessageControl,
                                          null,
                                          StatisticsUserAction.GIT_LOG_PANEL);
    }

    /**
     * 显示操作提示气泡
     * <p> 根据当前操作事件获取上下文组件, 并在该组件下方显示一个警告级别的 HTML 内容气泡提示.</p>
     * <p> 若未获取到上下文组件, 则直接返回, 不显示任何提示.</p>
     *
     * @param e       操作事件对象, 用于获取上下文组件
     * @param message 提示内容, 支持 HTML 格式
     * @since 1.0
     */
    private void showActionTip(@NotNull AnActionEvent e, @NotNull String message) {
        Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        if (component == null) {
            return;
        }
        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, MessageType.WARNING, null)
            .setHideOnClickOutside(true)
            .createBalloon()
            .show(getBestPopupPosition(component), Balloon.Position.below);
    }

    /**
     * 获取最佳弹窗位置
     * <p> 根据组件的底部坐标创建相对点, 用于弹窗显示在组件下方 </p>
     *
     * @param component 目标组件, 用于计算相对位置
     * @return 位于组件底部的相对点, 用于弹窗定位
     */
    private @NotNull RelativePoint getBestPopupPosition(@NotNull Component component) {
        Point point = new Point(0, component.getHeight());
        return new RelativePoint(component, point);
    }
}
