package dev.dong4j.zeka.stack.idea.plugin.workflow.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.workflow.service.WorkflowExplainerService;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;
import icons.TracerIcons;

/**
 * 解释工作流 Action
 *
 * @author dong4j
 * @version 1.0.0
 */
public class ExplainWorkflowAction extends AnAction {

    public ExplainWorkflowAction() {
        super(WorkflowBundle.messagePointer("action.explain.workflow"),
              WorkflowBundle.messagePointer("action.explain.workflow.description"),
              TracerIcons.WORKFLOW_ACTION);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 在 EDT 中获取 Editor 和光标位置（必须在 EDT 中执行）
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        com.intellij.psi.PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (editor == null || psiFile == null) {
            NotificationUtil.showError(project, WorkflowBundle.message("error.no.editor"));
            return;
        }

        // 获取光标位置（在 EDT 中安全）
        int caretOffset = editor.getCaretModel().getOffset();

        // 异步执行，避免阻塞 UI
        ProgressManager.getInstance().run(new Task.Backgroundable(project, WorkflowBundle.message("progress.analyzing"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(WorkflowBundle.message("progress.analyzing.call.chain"));

                try {
                    // 创建服务实例
                    WorkflowExplainerService service = new WorkflowExplainerService(project);

                    // 分析工作流（传递 PSI 文件和光标位置）
                    // 服务内部会处理两阶段写入：先创建文件并写入元数据，然后调用 AI 并追加结果
                    indicator.setText(WorkflowBundle.message("progress.calling.ai"));
                    service.explainWorkflow(psiFile, caretOffset);
                } catch (Exception ex) {
                    // 在 EDT 中显示错误
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project, WorkflowBundle.message("error.analysis.failed", ex.getMessage()));
                    });
                }
            }
        });
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只在 Java 文件中启用
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    /**
     * 获取动作更新线程
     *
     * <p>指定 update 方法在后台线程中执行，避免阻塞事件调度线程(EDT)。
     * 提高 UI 响应性，防止界面卡顿。
     *
     * @return ActionUpdateThread.BGT 后台线程
     * @see ActionUpdateThread#BGT
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 在后台线程中执行 update，避免阻塞 EDT
        return ActionUpdateThread.BGT;
    }
}

