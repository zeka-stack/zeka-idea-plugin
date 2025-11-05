package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskExecutor;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.util.PsiElementLocator;
import lombok.extern.slf4j.Slf4j;

/**
 * 为当前文件生成 JavaDoc（编辑器右键菜单）
 *
 * <p>在编辑器右键菜单中提供，根据光标位置智能决定生成范围。
 * 作为插件的主要入口点之一，提供便捷的文档生成功能。
 * 与 GenerateJavaDocGenerateAction 功能相似，但入口不同。
 *
 * <p>核心功能：
 * <ul>
 *   <li>集成到编辑器右键菜单</li>
 *   <li>智能元素定位</li>
 *   <li>异步任务处理</li>
 *   <li>进度显示和结果通知</li>
 * </ul>
 *
 * <p>根据光标位置智能决定生成范围：
 * <ul>
 *   <li>光标在方法上 → 只为该方法生成</li>
 *   <li>光标在字段上 → 只为该字段生成</li>
 *   <li>光标在类声明上 → 只为该类生成</li>
 *   <li>光标在类内部（但不在特定成员上）→ 为整个类及所有成员生成</li>
 *   <li>其他情况 → 为整个文件生成</li>
 * </ul>
 *
 * <p>执行流程：
 * <ol>
 *   <li>获取当前项目、编辑器和 PSI 文件</li>
 *   <li>使用 PsiElementLocator 智能定位元素</li>
 *   <li>通过 TaskCollector 收集文档生成任务</li>
 *   <li>在后台任务中使用 TaskExecutor 处理任务</li>
 *   <li>显示处理结果和统计信息</li>
 * </ol>
 *
 * @author dong4j
 * @version 1.0.0
 * @see AnAction
 * @see PsiElementLocator
 * @see TaskCollector
 * @see TaskExecutor
 * @since 1.0.0
 */
@SuppressWarnings("DuplicatedCode")
@Slf4j
public class GenerateJavaDocForFileAction extends AnAction {
    /**
     * 执行动作
     *
     * <p>当用户从编辑器右键菜单选择此选项时调用。
     * 负责协调整个文档生成流程。
     *
     * <p>处理流程：
     * <ol>
     *   <li>验证环境：检查项目、编辑器和文件有效性</li>
     *   <li>智能定位：根据光标位置确定处理范围</li>
     *   <li>任务收集：收集需要生成文档的代码元素</li>
     *   <li>异步处理：在后台任务中生成文档</li>
     *   <li>结果通知：显示处理结果给用户</li>
     * </ol>
     *
     * <p>与 GenerateJavaDocGenerateAction 的区别：
     * <ul>
     *   <li>使用不同的通知方法：notifyTargetCompletion</li>
     *   <li>入口点不同：右键菜单 vs Generate 菜单</li>
     * </ul>
     *
     * @param e 动作事件，包含上下文信息
     * @see #update(AnActionEvent)
     * @see PsiElementLocator#locateElement(Editor, PsiFile)
     * @see TaskCollector#collectFromElement(PsiElement)
     * @see TaskExecutor#processTasks(List)
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || !(psiFile instanceof PsiJavaFile)) {
            return;
        }

        log.info("为文件生成 JavaDoc: {}", psiFile.getName());

        // 智能定位：根据光标位置确定要生成文档的元素
        TaskCollector collector = new TaskCollector(project);
        List<DocumentationTask> tasks;
        String targetDescription = "文件";

        if (editor != null) {
            PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElement(editor, psiFile);

            if (locateResult != null) {
                targetDescription = PsiElementLocator.getElementDescription(locateResult.element());
                log.info("智能定位到: {}", targetDescription);
                tasks = collector.collectFromElement(locateResult.element());
            } else {
                // 无法定位，为整个文件生成
                tasks = collector.collectFromFile(psiFile);
            }
        } else {
            // 没有编辑器，为整个文件生成
            tasks = collector.collectFromFile(psiFile);
        }

        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        if (service.checkEmptyTasks(project, tasks, JavaDocBundle.message("notification.no.task.default"))) {
            return;
        }

        String finalTargetDesc = targetDescription;

        // 使用服务生成文档，带自定义完成回调
        service.generateDocumentation(project, tasks, finalTargetDesc, stats -> {
            showCompletionMessage(project, stats, finalTargetDesc);
        });
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

    /**
     * 更新动作状态
     *
     * <p>根据当前上下文更新动作的可用性和显示信息。
     * 只在 Java 文件中启用，确保功能的正确性。
     *
     * <p>更新内容：
     * <ul>
     *   <li>启用状态：仅在 Java 文件中启用</li>
     *   <li>显示文本：从资源文件获取国际化文本</li>
     *   <li>描述信息：从资源文件获取文件特定的描述</li>
     * </ul>
     *
     * @param e 动作事件，包含上下文信息
     * @see CommonDataKeys#PSI_FILE
     * @see JavaDocBundle
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(psiFile instanceof PsiJavaFile);
        e.getPresentation().setText(JavaDocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavaDocBundle.message("action.generate.javadoc.file.description"));
    }

    /**
     * 显示完成消息
     *
     * <p>在事件调度线程中显示任务完成的通知消息。
     * 包含目标描述和详细的统计信息。
     *
     * <p>与 GenerateJavaDocGenerateAction 的区别：
     * <ul>
     *   <li>使用 notifyTargetCompletion 显示目标信息</li>
     *   <li>包含目标描述（如"方法: getX()"）</li>
     * </ul>
     *
     * @param project 项目对象
     * @param stats   任务统计信息
     * @param target  目标描述
     * @see NotificationUtil#notifyTargetCompletion(Project, String, int, int, int)
     */
    private void showCompletionMessage(Project project, TaskExecutor.TaskStatistics stats, String target) {
        ApplicationManager.getApplication().invokeLater(() -> {
            NotificationUtil.notifyTargetCompletion(project, target, stats.completed(), stats.failed(), stats.skipped());
        });
    }
}

