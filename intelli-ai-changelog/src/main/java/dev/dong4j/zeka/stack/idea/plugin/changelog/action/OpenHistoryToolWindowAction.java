package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.changelog.ui.ChangelogToolWindowService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.ChangelogBundle;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import icons.ChangelogIcons;

/**
 * 打开历史记录工具窗口的动作类
 * <p> 此动作类用于在项目中打开历史记录工具窗口. 它会根据当前项目的状态启用或禁用自身,
 * 并在用户触发时显示指定的历史记录内容.
 * <p> 具体功能包括:
 * <ul>
 * <li> 检查当前项目是否有效并设置动作的可用性 </li>
 * <li> 设置动作的文本, 描述和图标 </li>
 * <li> 在用户触发时调用服务以显示历史记录内容 </li>
 * </ul>
 * <p>
 * 使用示例:
 * <pre>{@code
 * OpenHistoryToolWindowAction action = new OpenHistoryToolWindowAction();
 * // 动作将根据当前项目的状态自动启用或禁用
 * // 当用户点击该动作时, 将显示历史记录内容
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.03
 * @since 1.0.0
 */
public class OpenHistoryToolWindowAction extends AnAction {

    /**
     * 更新动作呈现状态
     * <p> 根据项目的状态更新动作的呈现信息, 包括启用状态, 文本, 描述和图标
     *
     * @param e 动作事件对象, 不能为 null
     * @since hello.world
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // 检查项目是否处于索引模式
        if (project != null && DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }
        
        boolean enabled = project != null && !project.isDisposed();
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setText(ChangelogBundle.message("action.open.history"));
        e.getPresentation().setDescription(ChangelogBundle.message("action.open.history.description"));
        e.getPresentation().setIcon(ChangelogIcons.CHANGELOG_16);
    }

    /**
     * 处理用户触发的打开历史工具窗口的动作
     * <p> 在用户触发动作时, 检查项目是否有效, 然后显示历史内容
     *
     * @param e 表示动作事件的对象, 不能为 null
     * @since hello.world
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

        ChangelogToolWindowService.getInstance(project).showHistoryContent();
    }

    /**
     * 获取动作更新线程
     * <p> 返回 BGT (Background Thread), 表示该动作在后台线程中进行更新
     *
     * @return 动作更新线程, 固定为 BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
