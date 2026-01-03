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
 * Open Console Settings Action
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-03 17:11:27
 * @since hello.world
 */
public class OpenConsoleSettingsAction extends AnAction {

    public OpenConsoleSettingsAction() {
        super(
            AICommonBundle.message("console.action.open.settings"),
            AICommonBundle.message("console.action.open.settings.description"),
            AllIcons.General.Settings
             );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }
        SettingsUtil.openSettings(project, AICommonSettingsConfigurable.class);
    }

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

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
