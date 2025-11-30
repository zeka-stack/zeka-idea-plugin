package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLog;
import com.intellij.vcs.log.VcsLogDataKeys;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import icons.ChangelogIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于从 Git 日志生成提交信息的操作类
 * <p>
 * 该类继承自 AbstractGitLogAction, 负责处理从 Git 日志中生成提交信息的逻辑, 包括获取项目上下文, 验证日志数据, 检查选中的提交记录, 并显示相关信息通知.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class GenerateCommitMessageForGitLogAction extends AbstractGitLogAction {

    /**
     * 获取提交操作的文本键
     * <p>
     * 返回用于提交操作的国际化文本键, 用于获取对应的文本资源
     *
     * @return 提交操作的文本键
     */
    @Override
    protected @NotNull String getTextKey() {
        return "commit.action.text";
    }

    /**
     * 获取提交操作的描述键
     * <p>
     * 返回用于标识提交操作描述的键值, 通常用于国际化资源文件中查找对应的描述文本.
     *
     * @return 提交操作的描述键
     */
    @Override
    protected @NotNull String getDescriptionKey() {
        return "commit.action.description";
    }

    /**
     * 获取图标
     * <p>
     * 返回指定的图标对象, 用于表示当前节点的图标.
     *
     * @return 图标对象
     */
    @Override
    protected @NotNull Icon getIcon() {
        return ChangelogIcons.DIFF;
    }

    /**
     * 处理用户操作事件, 用于在 Git 日志中执行提交相关操作
     * <p>
     * 该方法获取当前项目和 Git 日志数据, 并检查是否选择了提交记录. 如果没有选择提交记录, 则显示错误通知.
     * 如果选择了提交记录, 则显示提示信息.
     *
     * @param e 操作事件对象, 包含项目和数据信息
     * @throws IllegalArgumentException 如果事件对象为 null 或项目已被释放
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // 获取选中的提交记录
        VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        if (log == null) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.git.log"));
            return;
        }

        // 获取选中的提交记录 hash
        List<VcsFullCommitDetails> selectedCommits = log.getSelectedDetails();
        if (selectedCommits.isEmpty()) {
            NotificationUtil.showError(project, ChangelogBundle.message("error.no.commits.selected"));
            return;
        }

        // MVP 版本：提示用户此功能需要从已提交的 commit 中获取 diff
        // 后续版本可以实现从 commit 中提取 diff 的功能
        NotificationUtil.showInfo(project, ChangelogBundle.message("commit.gitlog.feature.coming.soon"));

        // TODO: 实现从已提交的 commit 中提取 diff 并生成提交记录的功能
        // 这需要：
        // 1. 从 VcsFullCommitDetails 获取 commit hash
        // 2. 使用 JGit 获取 commit 的 diff
        // 3. 将 diff 转换为 Change 对象或直接使用 CodeDiffUtil
        // 4. 调用 ChangelogService.generateCommitMessageFromDiff
    }

}

