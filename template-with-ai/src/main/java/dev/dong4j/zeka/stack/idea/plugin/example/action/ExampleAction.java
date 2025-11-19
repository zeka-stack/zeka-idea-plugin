package dev.dong4j.zeka.stack.idea.plugin.example.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.example.icons.ExampleIcons;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import dev.dong4j.zeka.stack.idea.plugin.example.util.NotificationUtil;

/**
 * 示例 Action - 右键菜单触发
 * <p>
 * 这是一个简化的 Action 示例，只保留右键菜单触发方式。
 * 在编辑器中右键点击文件即可看到此 Action。
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ExampleAction extends AnAction {

    public ExampleAction() {
        super(
            ExampleBundle.message("action.example.title"),
            ExampleBundle.message("action.example.description"),
            ExampleIcons.EXAMPLE_16
        );
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null) {
            NotificationUtil.showError(project, ExampleBundle.message("error.no.project"));
            return;
        }

        if (psiFile == null) {
            NotificationUtil.showError(project, ExampleBundle.message("error.no.file"));
            return;
        }

        // 执行示例操作
        String fileName = psiFile.getName();
        NotificationUtil.showInfo(project, ExampleBundle.message("success.action.executed", fileName));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 只有在有项目和文件时才启用
        e.getPresentation().setEnabled(project != null && psiFile != null);
    }
}

