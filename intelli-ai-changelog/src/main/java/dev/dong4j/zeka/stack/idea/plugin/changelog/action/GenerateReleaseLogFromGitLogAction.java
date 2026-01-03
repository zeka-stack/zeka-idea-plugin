package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLog;
import com.intellij.vcs.log.VcsLogDataKeys;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import icons.ChangelogIcons;

/**
 * 从 Git 日志生成发布日志的动作类
 * <p> 该类继承自 AbstractReleaseLogAction, 用于在 IntelliJ IDEA 中生成基于 Git 提交的日志的发布日志. 它提供了更新和执行动作的方法,
 * 包括检查项目和日志的有效性, 获取选定的提交, 并调用生成发布日志的逻辑.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.01
 * @since 1.0.0
 */
public class GenerateReleaseLogFromGitLogAction extends AbstractReleaseLogAction {

    /**
     * 更新动作事件的呈现状态
     * <p> 根据项目和日志信息设置动作事件的可用性, 文本, 描述和图标
     *
     * @param e 动作事件对象
     * @since hello.world
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        boolean enabled = project != null && log != null && !log.getSelectedDetails().isEmpty();
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setText(ChangelogBundle.message("action.generate.release.log"));
        e.getPresentation().setDescription(ChangelogBundle.message("action.generate.release.log.description"));
        e.getPresentation().setIcon(ChangelogIcons.RELEASE);
    }

    /**
     * 获取动作的图标
     *
     * @return 动作图标
     */
    @Override
    @NotNull
    protected Icon getIcon() {
        return ChangelogIcons.RELEASE;
    }

    /**
     * 处理用户在 VCS 日志中选择的提交记录, 并生成发布日志
     * <p> 此方法在用户触发生成发布日志的动作时被调用. 首先检查项目是否存在且未被销毁, 然后获取 VCS 日志.
     * 如果日志为空或未选中任何提交, 则显示错误通知. 接着解析 Git 根目录, 如果无法解析, 则显示错误通知.
     * 最后, 调用 `generate` 方法生成发布日志.
     *
     * @param e AnActionEvent 对象, 包含动作事件的相关信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        if (log == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.git.log"));
            return;
        }

        List<VcsFullCommitDetails> selectedCommits = getSelectedCommits(log);
        if (selectedCommits.isEmpty()) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.commits.selected"));
            return;
        }

        Path gitRoot = resolveGitRootForLog(selectedCommits);
        if (gitRoot == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("gitcliff.no.git.repo"));
            return;
        }

        // Git Log 入口不更新 lastUsedTag/lastUsedHash
        generate(project, gitRoot, selectedCommits, false);
    }

    /**
     * 获取选中的提交记录列表
     * <p> 从给定的日志中获取用户选中的提交记录列表. 如果日志为空, 则返回一个空列表.
     *
     * @param log 日志对象, 可以为 null
     * @return 选中的提交记录列表, 如果日志为空则返回空列表
     */
    @NotNull
    protected List<VcsFullCommitDetails> getSelectedCommits(@Nullable VcsLog log) {
        if (log == null) {
            return List.of();
        }
        return log.getSelectedDetails();
    }

}
