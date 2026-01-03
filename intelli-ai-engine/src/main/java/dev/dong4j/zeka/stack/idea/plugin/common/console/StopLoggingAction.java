package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 停止日志输出 Action
 * <p>
 * 点击后将设置页面中的 "允许输出日志"(verboseLogging) 设置为 false
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class StopLoggingAction extends AnAction {

    /**
     * 构造函数, 初始化停止日志输出 Action
     * <p> 设置 Action 的名称, 描述和图标
     *
     */
    public StopLoggingAction() {
        super(
            AICommonBundle.message("console.action.stop.logging"),
            AICommonBundle.message("console.action.stop.logging.description"),
            AllIcons.Actions.Suspend
             );
    }

    /**
     * 处理停止日志输出的动作
     * <p> 当用户触发该动作时, 将设置页面中的 "允许输出日志"(verboseLogging) 设置为 false, 并保存设置.
     * 如果项目不为空, 则刷新项目的控制台视图面板.
     *
     * @param e AnActionEvent 对象, 包含动作事件的相关信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AIProviderSettings settings = AIProviderSettings.getInstance();
        settings.verboseLogging = false;
        ApplicationManager.getApplication().saveSettings();
        Project project = e.getProject();
        if (project != null) {
            AIConsoleView.getInstance(project).refreshPanelBySettings();
        }
    }

    /**
     * 更新动作呈现状态
     * <p> 根据项目是否存在以及控制台选项卡是否被选中来更新动作的可用性和可见性
     * <p> 如果项目为空或控制台选项卡未选中, 则禁用并隐藏动作
     * <p> 否则, 根据设置中的“允许输出日志”选项来启用或禁用动作
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
        e.getPresentation().setEnabledAndVisible(settings.verboseLogging);
    }

    /**
     * 返回操作更新线程
     * <p> 此方法重写自父类, 指定此动作在后台线程进行更新
     *
     * @return 操作更新线程, 始终返回 BGT (Background Thread)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
