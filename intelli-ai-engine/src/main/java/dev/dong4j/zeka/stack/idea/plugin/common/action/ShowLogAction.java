package dev.dong4j.zeka.stack.idea.plugin.common.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.ActionUpdateThreadAware;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.DumbAwareAction;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ShowLogAction extends DumbAwareAction implements ActionUpdateThreadAware {

    public ShowLogAction() {
        super("Intelli AI: Show Log",
              "Open the idea.log file to view Intelli AI plugin logs",
              AllIcons.Nodes.LogFolder);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        showLog();
    }

    public static void showLog() {
        File logFile = new File(PathManager.getLogPath(), "idea.log");
        RevealFileAction.openFile(logFile);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
