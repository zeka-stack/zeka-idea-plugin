package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.settings.AICommonSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.kit.SettingsUtil;

/**
 * 打开控制台设置的动作类
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-03 17:11:27
 * @since hello.world
 */
public class OpenConsoleSettingsAction extends AnAction {

    /**
     * 构造函数, 初始化 OpenConsoleSettingsAction 对象
     * <p> 设置动作名称, 描述和图标
     *
     * @since hello.world
     */
    public OpenConsoleSettingsAction() {
        super(
            AICommonBundle.message("console.action.open.settings"),
            AICommonBundle.message("console.action.open.settings.description"),
            AllIcons.General.Settings
             );
    }

    /**
     * 执行打开控制台设置的操作
     * <p> 在用户触发此动作时, 检查项目是否存在且未被销毁, 然后打开与 AICommonSettingsConfigurable 相关的设置
     *
     * @param e AnActionEvent 对象, 包含动作执行的相关信息
     * @since hello.world
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }
        SettingsUtil.openSettings(project, AICommonSettingsConfigurable.class);
    }

    /**
     * 更新动作的可用性状态
     * <p> 根据项目和控制台选项卡的选择状态更新动作的可用性
     * <p> 如果项目为空或已销毁, 则禁用动作
     * <p> 如果控制台选项卡未被选中, 则禁用动作
     *
     * @param e 动作事件对象, 不能为 null
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        e.getPresentation().setEnabled(consoleView.isConsoleTabSelected());
    }

    /**
     * 返回动作更新线程
     * <p> 此方法重写父类的方法, 指定此动作在后台线程中进行更新
     *
     * @return 动作更新线程, 始终返回 BGT (Background Thread)
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
