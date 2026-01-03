package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;

/**
 * 滚动到末尾 Action
 * <p>
 * 点击后将 ConsoleView 滚动到末尾
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class ScrollToEndAction extends AnAction {

    public ScrollToEndAction() {
        super(
            AICommonBundle.message("console.action.scroll.to.end"),
            AICommonBundle.message("console.action.scroll.to.end.description"),
            AllIcons.Actions.MoveDown
             );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        ConsoleView console = consoleView.getConsoleView();
        if (console != null) {
            // 尝试调用scrollToEnd方法
            try {
                Method scrollToEndMethod = console.getClass().getMethod("scrollToEnd");
                scrollToEndMethod.invoke(console);
            } catch (Exception ignored) {
                // 如果方法不存在，尝试通过Editor来实现
                try {
                    Method getEditorMethod = console.getClass().getMethod("getEditor");
                    Object editorObj = getEditorMethod.invoke(console);
                    if (editorObj != null) {
                        Method scrollToEndEditorMethod = editorObj.getClass().getMethod("getScrollingModel");
                        Object scrollingModel = scrollToEndEditorMethod.invoke(editorObj);
                        if (scrollingModel != null) {
                            Method scrollToEndMethod = scrollingModel.getClass().getMethod("scrollToEnd");
                            scrollToEndMethod.invoke(scrollingModel);
                        }
                    }
                } catch (Exception ignored2) {
                    // 静默处理
                }
            }
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
        ConsoleView console = consoleView.getConsoleView();
        e.getPresentation().setEnabledAndVisible(console != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
