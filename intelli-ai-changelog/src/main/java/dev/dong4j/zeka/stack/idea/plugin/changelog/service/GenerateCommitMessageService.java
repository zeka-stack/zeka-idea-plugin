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
import com.intellij.vcs.commit.CommitWorkflowHandler;
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
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import icons.ChangelogIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 提交消息动作服务
 * <p>用于复用生成提交消息动作的 UI 更新与执行逻辑，避免在 Action 中直接 new 其他 Action。</p>
 */
@Slf4j
@Service(Service.Level.APP)
public final class GenerateCommitMessageService {

    public static @NotNull GenerateCommitMessageService getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(GenerateCommitMessageService.class);
    }

    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

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

        CommitWorkflowHandler commitWorkflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (commitWorkflowHandler != null) {
            log.debug("Git 提交页面：开始生成提交记录");
            Collection<Change> changes = dev.dong4j.zeka.stack.idea.plugin.kit.CommitUtil.getSelectedChanges(commitWorkflowHandler);
            if (changes.isEmpty()) {
                log.debug("Git 提交页面：未选择任何文件变更");
                showActionTip(e, ChangelogBundle.message("commit.no.selected.changes"));
                return;
            }

            log.debug("Git 提交页面：找到 {} 个文件变更", changes.size());
            generator.generateForChanges(changes, commitMessageControl, null);
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
            generator.generateForCommitHash(commitHash, commitMessageControl, null);
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
        generator.generateForCommitHashes(commitHashes, commitMessages, commitMessageControl, null);
    }

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

    private @NotNull RelativePoint getBestPopupPosition(@NotNull Component component) {
        Point point = new Point(0, component.getHeight());
        return new RelativePoint(component, point);
    }
}
