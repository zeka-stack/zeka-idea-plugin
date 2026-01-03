package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 清除控制台操作
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-03 18:34:24
 * @since hello.world
 */
public class ClearConsoleAction extends AnAction {

    /**
     * 构造函数, 初始化清空控制台的动作
     * <p> 设置动作名称, 描述和图标
     *
     */
    public ClearConsoleAction() {
        super(
            AICommonBundle.message("console.action.clear"),
            AICommonBundle.message("console.action.clear.description"),
            AllIcons.Actions.GC
             );
    }

    /**
     * 处理清空控制台的操作
     * <p> 在用户触发清空控制台动作时, 调用此方法. 首先检查项目是否存在, 如果存在, 则获取控制台视图并清空控制台内容.
     *
     * @param e 表示动作事件的对象, 不能为 null
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        consoleView.clearConsole();
    }

    /**
     * 更新动作的可用性和可见性状态
     * <p> 根据项目和控制台视图的状态更新动作的可用性和可见性
     * <p> 如果项目为空, 则禁用并隐藏动作
     * <p> 如果控制台选项卡未被选中, 则禁用并隐藏动作
     * <p> 如果控制台视图存在, 则启用并显示动作
     *
     * @param e 动作事件对象, 不能为 null
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
        e.getPresentation().setEnabledAndVisible(consoleView.getConsoleView() != null);
    }

    /**
     * 返回动作更新线程
     * <p> 此方法重写父类的实现, 指定此动作在后台线程中进行更新
     *
     * @return 动作更新线程, 固定返回 BGT (Background Thread)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
