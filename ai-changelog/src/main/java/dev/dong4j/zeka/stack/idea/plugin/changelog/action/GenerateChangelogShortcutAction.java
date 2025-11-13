package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import org.jetbrains.annotations.NotNull;

/**
 * 快捷键触发的示例 Action
 * 快捷键：Ctrl+Shift+E (Windows/Linux) 或 Cmd+Shift+E (Mac)
 */
public class GenerateChangelogShortcutAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null) {
            NotificationUtil.showError(project, "No project found");
            return;
        }

        if (editor == null || psiFile == null) {
            NotificationUtil.showError(project, "No editor or file found");
            return;
        }

        // 获取光标位置
        int offset = editor.getCaretModel().getOffset();
        
        // 执行示例服务
        ChangelogService exampleService = ChangelogService.getInstance(project);
        String result = exampleService.processText(psiFile.getText(), offset);
        
        NotificationUtil.showInfo(project, "Example Action executed: " + result);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        // 只有在有项目、编辑器和文件时才启用
        e.getPresentation().setEnabled(project != null && editor != null && psiFile != null);
    }
}
