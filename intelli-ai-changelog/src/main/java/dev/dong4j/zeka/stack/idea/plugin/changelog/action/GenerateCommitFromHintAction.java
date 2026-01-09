package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.vcs.commit.CommitWorkflowHandler;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import dev.dong4j.zeka.stack.idea.plugin.changelog.git.CommitMessageGenerator;
import dev.dong4j.zeka.stack.idea.plugin.changelog.hint.CommitMessageHintService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 从 Hint 触发生成 Commit Message 的 Action
 * <p>
 * 当用户在 Commit Message 编辑器中按 Tab 键时，如果存在 Inlay Hint 提示，
 * 则触发 Commit Message 的生成。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
@Slf4j
public class GenerateCommitFromHintAction extends AnAction {

    /**
     * 更新 Action 状态
     *
     * @param e 动作事件
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();

        // 基本检查
        if (editor == null || project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否启用了"使用提交消息输入作为上下文"设置
        if (!SettingsState.getInstance().useCommitMessageInputAsContext) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否为 Commit Message Editor
        if (!CommitMessageHintService.isCommitMessageEditor(editor)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否在补全状态（避免与代码补全冲突）
        if (isCompletionActive(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否有活跃的 Hint
        CommitMessageHintService service = project.getService(CommitMessageHintService.class);
        if (service == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        var hintManager = service.getHintManager(editor);
        if (hintManager == null || !hintManager.hasActiveHint()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查是否正在生成中
        if (CommitMessageGenerator.isRunning(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查项目是否处于索引模式
        if (DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 所有检查通过，启用 Action
        e.getPresentation().setEnabled(true);
    }

    /**
     * 执行 Action
     *
     * @param e 动作事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();

        if (editor == null || project == null || project.isDisposed()) {
            return;
        }

        // 再次检查设置
        if (!SettingsState.getInstance().useCommitMessageInputAsContext) {
            return;
        }

        // 检查是否有活跃的 Hint
        CommitMessageHintService service = project.getService(CommitMessageHintService.class);
        if (service == null) {
            return;
        }

        var hintManager = service.getHintManager(editor);
        if (hintManager == null || !hintManager.hasActiveHint()) {
            return;
        }

        // 检查项目是否处于索引模式
        if (DumbService.isDumb(project)) {
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.indexing.warning"));
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

        // 获取 CommitWorkflowHandler
        CommitWorkflowHandler commitWorkflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER);
        if (commitWorkflowHandler == null) {
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.selected.changes"));
            return;
        }

        // 获取变更
        Collection<Change> changes = dev.dong4j.zeka.stack.idea.plugin.kit.CommitUtil.getSelectedChanges(commitWorkflowHandler);
        if (changes.isEmpty()) {
            NotificationUtil.showWarning(project, ChangelogBundle.message("commit.no.selected.changes"));
            return;
        }

        log.debug("从 Hint 触发：开始生成提交记录，变更数量: {}", changes.size());

        // 获取 CommitMessageI
        CommitMessageI commitMessageControl = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);

        // 隐藏 Hint
        hintManager.hideHint();

        // 触发生成
        CommitMessageGenerator generator = new CommitMessageGenerator(project);
        generator.generateForChanges(changes, commitMessageControl, null);
    }

    /**
     * 获取更新线程
     *
     * @return ActionUpdateThread.BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * 检查是否在补全状态
     *
     * @param project 项目实例
     * @return 如果正在补全则返回 true
     */
    private boolean isCompletionActive(@NotNull Project project) {
        // 检查 Lookup（代码补全）是否活跃
        com.intellij.codeInsight.lookup.LookupManager lookupManager =
            com.intellij.codeInsight.lookup.LookupManager.getInstance(project);
        return lookupManager.getActiveLookup() != null;
    }

}
