package dev.dong4j.zeka.stack.idea.plugin.workflow.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowAnalysisUtil;
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
            NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, WorkflowBundle.message("error.no.editor"));
            return;
        }

        // 获取光标位置（在 EDT 中安全）
        int caretOffset = editor.getCaretModel().getOffset();

        // 使用工具类执行工作流分析
        WorkflowAnalysisUtil.executeWorkflowAnalysis(project, psiFile, caretOffset);
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

