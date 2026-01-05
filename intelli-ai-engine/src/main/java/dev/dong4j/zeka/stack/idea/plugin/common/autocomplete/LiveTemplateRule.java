package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.project.Project;

final class LiveTemplateRule implements TriggerRule {
    @Override
    public boolean check(TriggerContext context) {
        Project project = context.getEditor().getProject();
        if (project == null) {
            return false;
        }
        return TemplateManager.getInstance(project).getActiveTemplate(context.getEditor()) == null;
    }
}
