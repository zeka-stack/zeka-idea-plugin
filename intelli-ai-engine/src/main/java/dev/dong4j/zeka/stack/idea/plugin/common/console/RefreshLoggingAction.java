package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 刷新日志输出 Action
 * <p>
 * 点击后将设置页面中的 "允许输出日志"(verboseLogging) 设置为 true
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class RefreshLoggingAction extends AnAction {

    /**
     * 构造函数, 初始化刷新日志输出操作
     * <p> 设置动作名称, 描述和图标
     *
     * @since 1.0.0
     */
    public RefreshLoggingAction() {
        super(
            AICommonBundle.message("console.action.refresh.logging"),
            AICommonBundle.message("console.action.refresh.logging.description"),
            AllIcons.Actions.Execute
             );
    }

    /**
     * 处理刷新日志输出的动作
     * <p> 当动作被触发时, 该方法会被调用. 它会检查当前项目是否存在, 并在存在的情况下启用日志输出功能并显示控制台.
     *
     * @param e AnActionEvent 对象, 包含动作执行的相关信息
     * @since 1.0.0
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            AIConsoleView.getInstance(project).enableVerboseLoggingAndShowConsole();
        }
    }

    /**
     * 更新动作的状态
     * <p> 根据项目是否存在以及控制台选项卡是否选中来更新动作的状态和可见性
     * <p> 如果项目为空或控制台选项卡未选中, 则禁用并隐藏动作
     * <p> 如果当前日志输出设置为不允许输出日志, 则启用动作, 否则禁用
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
        boolean isSelected = consoleView.isConsoleTabSelected();
        if (!isSelected) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        AIProviderSettings settings = AIProviderSettings.getInstance();
        e.getPresentation().setEnabledAndVisible(!settings.verboseLogging);
    }

    /**
     * 返回动作更新线程
     * <p> 此方法重写父类的实现, 指定此动作在后台线程进行更新
     *
     * @return 动作更新线程, 始终返回 BGT(后台线程)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
