package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

public class AutocompleteTriggerAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null) {
            return;
        }
        AutocompleteTracker tracker = AutocompleteService.getInstance(project).getTracker(editor);
        if (tracker != null) {
            tracker.trigger(AutocompleteTriggerMode.MANUAL);
        }
    }
}
