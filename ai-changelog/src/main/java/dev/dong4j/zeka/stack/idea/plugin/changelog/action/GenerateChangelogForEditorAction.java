package dev.dong4j.zeka.stack.idea.plugin.changelog.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import dev.dong4j.zeka.stack.idea.plugin.changelog.service.ChangelogService;
import dev.dong4j.zeka.stack.idea.plugin.changelog.util.NotificationUtil;
import org.jetbrains.annotations.NotNull;

/**
 * 编辑器右键菜单触发的示例 Action
 */
public class GenerateChangelogForEditorAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null) {
            NotificationUtil.showError(project, "No project found");
            return;
        }

        if (psiFile == null) {
            NotificationUtil.showError(project, "No file found");
            return;
        }

        // 执行示例服务
        ChangelogService exampleService = ChangelogService.getInstance(project);
        String result = exampleService.processFile(psiFile);
        
        NotificationUtil.showInfo(project, "File Action executed for: " + psiFile.getName() + " - " + result);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        
        // 只有在有项目和文件时才启用
        e.getPresentation().setEnabled(project != null && psiFile != null);
    }
}
