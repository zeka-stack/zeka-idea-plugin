package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 滚动到末尾 Action
 * <p>
 * 点击后将 ConsoleView 滚动到末尾.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class ScrollToEndAction extends ToggleAction {

    /**
     * 构造函数, 初始化滚动到末尾的动作
     * <p> 设置动作名称, 描述和图标
     *
     */
    public ScrollToEndAction() {
        super(
            AICommonBundle.message("console.action.scroll.to.end"),
            AICommonBundle.message("console.action.scroll.to.end.description"),
            AllIcons.Actions.MoveDown
             );
    }

    /**
     * 执行滚动到末尾的操作
     * <p> 此方法在用户触发动作时被调用, 尝试通过 ConsoleView 对象的 scrollToEnd 方法将控制台滚动到末尾.
     * 如果 ConsoleView 对象没有提供 scrollToEnd 方法, 则通过获取 Editor 对象的滚动模型来实现滚动到末尾.
     * <p> 如果在操作过程中出现异常, 则会被静默处理.
     *
     * @param e 表示动作事件的对象
     * @since 1.0.0
     */
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return false;
        }

        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        return consoleView.isAutoScrollToEndEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        consoleView.setAutoScrollToEndEnabled(state);
        if (state) {
            consoleView.scrollToEnd();
        }
    }

    /**
     * 更新动作的可用性和可见性
     * <p> 根据项目和控制台视图的状态更新动作的可用性和可见性
     *
     * @param e 动作事件对象, 不能为 null
     * @since 1.0.0
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        if (!consoleView.isConsoleTabSelected()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        ConsoleView console = consoleView.getConsoleView();
        e.getPresentation().setEnabledAndVisible(console != null);
    }

    /**
     * 返回动作更新线程
     * <p> 此方法重写父类的方法, 指定此动作在后台线程中进行更新
     *
     * @return 动作更新线程, 始终返回 BGT(后台线程)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
