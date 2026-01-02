package dev.dong4j.zeka.stack.idea.plugin.workflow.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.workflow.service.WorkflowExplainerService;
import dev.dong4j.zeka.stack.idea.plugin.workflow.settings.SettingsState;

/**
 * 工作流分析工具类
 * <p>
 * 提供统一的工作流分析执行逻辑，避免代码重复
 *
 * @author dong4j
 * @version 1.0.0
 */
public final class WorkflowAnalysisUtil {
    private WorkflowAnalysisUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 异步执行工作流分析
     * <p>
     * 该方法封装了工作流分析的公共逻辑，包括：
     * <ul>
     *   <li>AI Provider 检查</li>
     *   <li>进度指示器设置</li>
     *   <li>服务创建和调用</li>
     *   <li>异常处理和错误通知</li>
     * </ul>
     *
     * @param project     项目对象
     * @param psiFile     PSI 文件
     * @param caretOffset 光标偏移量
     */
    public static void executeWorkflowAnalysis(@NotNull Project project,
                                               @NotNull PsiFile psiFile,
                                               int caretOffset) {
        // 异步执行工作流分析，避免阻塞 UI
        ProgressManager.getInstance().run(new Task.Backgroundable(project, WorkflowBundle.message("progress.analyzing"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // 检查 AI Provider 配置
                AIProviderConfig config = SettingsState.getInstance().providerConfig;
                if (!AIProviderUtils.hasAIProvider(project,
                                                   config,
                                                   WorkflowBundle.message("settings.display.name"),
                                                   WorkflowBundle.message("settings.ai.provider.selection"))) {
                    return;
                }

                indicator.setIndeterminate(true);
                indicator.setText(WorkflowBundle.message("progress.analyzing.call.chain"));

                try {
                    // 创建服务实例并执行分析
                    WorkflowExplainerService service = new WorkflowExplainerService(project);
                    indicator.setText(WorkflowBundle.message("progress.calling.ai"));
                    service.explainWorkflow(psiFile, caretOffset);
                } catch (Exception e) {
                    // 在 EDT 中显示错误通知
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project, PluginContents.PLUGIN_NAME, WorkflowBundle.message("error.analysis.failed",
                                                                                                               e.getMessage()));
                    });
                }
            }
        });
    }
}

