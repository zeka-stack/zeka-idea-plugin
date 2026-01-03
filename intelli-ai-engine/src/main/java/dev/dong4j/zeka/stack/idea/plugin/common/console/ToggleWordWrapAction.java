package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 切换自动换行 Action
 * <p>
 * 点击后切换 ConsoleView 编辑器的自动换行状态
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class ToggleWordWrapAction extends ToggleAction {

    /**
     * 构造函数, 初始化 ToggleWordWrapAction
     * <p> 设置动作名称, 描述和图标
     *
     */
    public ToggleWordWrapAction() {
        super(
            AICommonBundle.message("console.action.toggle.word.wrap"),
            AICommonBundle.message("console.action.toggle.word.wrap.description"),
            AllIcons.Actions.ToggleSoftWrap
             );
    }

    /**
     * 处理用户操作事件以切换编辑器的自动换行状态
     * <p> 当用户触发此动作时, 该方法会被调用. 它会检查当前项目是否存在, 并获取对应的编辑器实例.
     * 如果编辑器存在, 则切换其自动换行状态.
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

        return AIConsoleView.getInstance(project).isWordWrapEnabled();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        AIConsoleView.getInstance(project).setWordWrapEnabled(state);
    }

    /**
     * 更新动作的可用性和可见性
     * <p> 根据项目和控制台选项卡的状态更新动作的可用性和可见性
     * <p> 如果项目为空或当前选项卡不是控制台选项卡, 则禁用动作
     * <p> 如果获取到编辑器对象, 则启用动作, 并根据编辑器的自动换行设置更新动作的显示状态
     *
     * @param e 动作事件对象
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

        Editor editor = getEditor(project);
        e.getPresentation().setEnabledAndVisible(editor != null);
    }

    /**
     * 通过反射获取 ConsoleView 的 Editor.
     *
     * @param project 项目对象
     * @return 返回 ConsoleView 的 Editor 对象, 如果获取失败则返回 null
     */
    @Nullable
    private Editor getEditor(@NotNull Project project) {
        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        ConsoleView console = consoleView.getConsoleView();
        if (console == null) {
            return null;
        }

        try {
            Method getEditorMethod = console.getClass().getMethod("getEditor");
            Object editorObj = getEditorMethod.invoke(console);
            if (editorObj instanceof Editor) {
                return (Editor) editorObj;
            }
        } catch (Exception ignored) {
            // 反射失败，静默处理
        }
        return null;
    }

    /**
     * 返回操作更新线程
     * <p> 此方法重写自父类, 指定了此动作的操作更新线程为后台线程 (BGT)
     *
     * @return 操作更新线程, 固定返回 BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
