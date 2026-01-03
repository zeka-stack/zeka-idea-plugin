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
 * 刷新日志输出 Action
 * <p>
 * 点击后将设置页面中的"允许输出日志"（verboseLogging）设置为 true
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class RefreshLoggingAction extends AnAction {

    public RefreshLoggingAction() {
        super(
            AICommonBundle.message("console.action.refresh.logging"),
            AICommonBundle.message("console.action.refresh.logging.description"),
            AllIcons.Actions.Execute
             );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AIProviderSettings settings = AIProviderSettings.getInstance();
        settings.verboseLogging = true;
        ApplicationManager.getApplication().saveSettings();
        Project project = e.getProject();
        if (project != null) {
            AIConsoleView.getInstance(project).refreshPanelBySettings();
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
        boolean isSelected = consoleView.isConsoleTabSelected();
        if (!isSelected) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        AIProviderSettings settings = AIProviderSettings.getInstance();
        e.getPresentation().setEnabledAndVisible(!settings.verboseLogging);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
