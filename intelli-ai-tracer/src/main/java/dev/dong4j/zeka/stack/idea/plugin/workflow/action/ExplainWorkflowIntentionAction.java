package dev.dong4j.zeka.stack.idea.plugin.workflow.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.workflow.service.WorkflowExplainerService;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.PSIUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;

/**
 * 工作流解释意图操作
 * <p>
 * 支持通过 Alt+Enter 快速触发工作流分析
 *
 * @author dong4j
 * @version 1.0.0
 */
public class ExplainWorkflowIntentionAction implements IntentionAction {

    @Override
    public @NotNull String getText() {
        return WorkflowBundle.message("intention.explain.workflow");
    }

    @Override
    public @NotNull String getFamilyName() {
        return WorkflowBundle.message("intention.family.workflow");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        // 只在 Java 文件中启用
        if (!(file instanceof PsiJavaFile)) {
            return false;
        }

        try {
            // 检查光标位置是否支持工作流分析
            int offset = editor.getCaretModel().getOffset();
            PSIUtil.ElementContext context = PSIUtil.detectElementType(file, offset);
            return context.type() != PSIUtil.ElementType.UNKNOWN;
        } catch (Exception e) {
            // 如果 PSI 访问出现问题，返回 false
            return false;
        }
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int caretOffset = editor.getCaretModel().getOffset();

        // 异步执行工作流分析，避免阻塞 UI
        ProgressManager.getInstance().run(new Task.Backgroundable(project, WorkflowBundle.message("progress.analyzing"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(WorkflowBundle.message("progress.analyzing.call.chain"));

                try {
                    // 创建服务实例并执行分析
                    WorkflowExplainerService service = new WorkflowExplainerService(project);
                    indicator.setText(WorkflowBundle.message("progress.calling.ai"));
                    service.explainWorkflow(file, caretOffset);
                } catch (Exception e) {
                    // 在 EDT 中显示错误通知
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project, WorkflowBundle.message("error.analysis.failed", e.getMessage()));
                    });
                }
            }
        });
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
