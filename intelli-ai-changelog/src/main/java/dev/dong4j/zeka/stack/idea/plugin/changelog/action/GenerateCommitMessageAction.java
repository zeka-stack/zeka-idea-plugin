package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.vcs.commit.CommitWorkflowHandler;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Point;
import java.util.Collection;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.CommitMessageGenerator;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.ChangelogIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于生成 Git 提交消息的动作类
 * <p> 该类继承自 AnAction, 并实现了自定义的更新和执行逻辑. 在更新阶段, 检查项目是否存在并且未被销毁, 并设置动作的文本和图标.
 * 在执行阶段, 获取选中的变更集合, 并调用 CommitMessageGenerator 生成提交消息.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
@Slf4j
public class GenerateCommitMessageAction extends AnAction {

    /**
     * 更新动作状态
     * <p> 检查当前项目是否有代码变更, 如果有则启用按钮, 否则禁用. 此操作在后台线程中执行, 以确保不会阻塞 UI.
     *
     * @param e 动作事件
     * @since 1.0.0
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(false);
            return;
        }

        // 检查项目是否处于索引模式
        if (DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(true);
            return;
        }

        // 设置按钮文本和图标
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
     * 获取更新线程
     * <p> 在后台线程中执行更新操作, 避免阻塞 UI. 此方法返回后台线程 BGT.
     *
     * @return ActionUpdateThread.BGT 后台线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 执行动作
     * <p> 当用户点击按钮时, 根据代码变更生成提交记录. 首先检查项目是否存在且未被销毁, 并验证是否配置了 AI 提供商. 然后获取已提交的文件变更, 如果没有变更则记录警告日志并返回. 最后, 使用 CommitMessageGenerator
     * 根据变更生成提交信息.
     *
     * @param e 动作事件
     * @since 1.0.0
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

        if (CommitMessageGenerator.isRunning(project)) {
            CommitMessageGenerator.stop(project);
            return;
        }

        CommitWorkflowHandler commitWorkflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (commitWorkflowHandler == null) {
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.selected.changes"));
            return;
        }

        // 检查 AI Provider 配置
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (!AIProviderUtils.hasAIProvider(project,
                                           config,
                                           ChangelogBundle.message("settings.display.name"),
                                           ChangelogBundle.message("settings.ai.provider.selection"))) {
            return;
        }

        log.debug("Git 提交页面：开始生成提交记录");
        // 获取提交的文件变更
        Collection<Change> changes = dev.dong4j.zeka.stack.idea.plugin.kit.CommitUtil.getSelectedChanges(commitWorkflowHandler);
        if (changes.isEmpty()) {
            log.debug("Git 提交页面：未选择任何文件变更");
            showActionTip(e, ChangelogBundle.message("commit.no.selected.changes"));
            return;
        }

        log.debug("Git 提交页面：找到 {} 个文件变更", changes.size());

        // 读取提交面板的提交信息控件，用于直接写入提交记录
        CommitMessageI commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        // 使用生成器生成提交记录
        CommitMessageGenerator generator = new CommitMessageGenerator(project);
        generator.generateForChanges(changes, commitMessageControl, null);
    }

    /**
     * 显示操作提示气泡
     * <p> 根据当前动作事件获取合适的组件作为气泡显示的参考点, 然后创建并显示一个带有警告信息的 HTML 气泡提示.
     * <p> 优先从事件输入中获取组件, 若失败则从上下文组件数据中获取, 若仍失败则尝试获取项目对应的窗口框架.
     * <p> 如果最终无法获取有效组件, 则直接返回, 不显示气泡.
     *
     * @param e       动作事件, 不能为 null
     * @param message 提示消息内容, 不能为 null
     */
    private void showActionTip(@NotNull AnActionEvent e, @NotNull String message) {
        Component component = null;
        if (e.getInputEvent() != null) {
            component = e.getInputEvent().getComponent();
        }
        if (component == null) {
            component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
        }
        if (component == null) {
            Project project = e.getProject();
            if (project != null) {
                component = WindowManager.getInstance().getFrame(project);
            }
        }
        if (component == null) {
            return;
        }

        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, MessageType.WARNING, null)
            .setFadeoutTime(2500)
            .createBalloon()
            .show(new RelativePoint(component, new Point(component.getWidth(), component.getHeight())), Balloon.Position.below);
    }

}
