package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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
public class ToggleWordWrapAction extends AnAction {

    public ToggleWordWrapAction() {
        super(
            AICommonBundle.message("console.action.toggle.word.wrap"),
            AICommonBundle.message("console.action.toggle.word.wrap.description"),
            AllIcons.Actions.ToggleSoftWrap
             );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        Editor editor = getEditor(project);
        if (editor != null) {
            editor.getSettings().setUseSoftWraps(!editor.getSettings().isUseSoftWraps());
        }
    }

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
        if (editor != null) {
            e.getPresentation().setEnabledAndVisible(true);
            // Presentation没有setSelected方法，使用setText来表示状态
            boolean isSoftWraps = editor.getSettings().isUseSoftWraps();
            // 可以通过图标或文本变化来表示状态，这里我们只启用/禁用
        } else {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    /**
     * 通过反射获取ConsoleView的Editor
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

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
