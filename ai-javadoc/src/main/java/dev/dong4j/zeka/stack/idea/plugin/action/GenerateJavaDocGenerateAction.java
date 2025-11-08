package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
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
import dev.dong4j.zeka.stack.idea.plugin.util.PsiElementLocator;
import lombok.extern.slf4j.Slf4j;

/**
 * Generate (Command+N) 菜单中的 JavaDoc 生成
 *
 * <p>在 Generate 弹出菜单中添加"Generate Javadoc with AI"选项。
 * 用户可以通过 Command+N (Mac) 或 Alt+Insert (Win/Linux) 快捷键调用。
 * 作为插件的主要入口点之一，提供便捷的文档生成功能。
 *
 * <p>核心功能：
 * <ul>
 *   <li>集成到 IntelliJ 的 Generate 菜单</li>
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
public class GenerateJavaDocGenerateAction extends AnAction {

    /**
     * 执行动作
     *
     * <p>当用户从 Generate 菜单选择此选项时调用。
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
     * <p>异常处理：
     * <ul>
     *   <li>无效环境：直接返回，不显示错误</li>
     *   <li>无任务：显示友好的提示信息</li>
     *   <li>处理错误：由 TaskExecutor 统一处理</li>
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

        log.info("通过 Generate 菜单为文件生成 JavaDoc: {}", psiFile.getName());

        if (editor == null) {
            // 没有编辑器, 直接退出逻辑
            log.info("[编辑器 Generate 菜单] 没有编辑器对象, 退出逻辑");
            return;
        }

        PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElement(editor, psiFile);
        if (locateResult == null) {
            // 无法定位，直接退出逻辑
            log.info("[编辑器 Generate 菜单] 无法定位，退出逻辑");
            return;
        }

        // 智能定位：根据光标位置确定要生成文档的元素
        TaskCollector collector = new TaskCollector(project);
        List<DocumentationTask> tasks;
        String targetDescription = PsiElementLocator.getElementDescription(locateResult.element());
        log.info("智能定位到: {}", targetDescription);
        tasks = collector.collectFromElement(locateResult.element());

        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        if (service.checkEmptyTasks(project, tasks, JavaDocBundle.message("notification.no.task.default"))) {
            return;
        }

        // 使用服务生成文档
        service.generateDocumentation(project, tasks, targetDescription);
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
     *   <li>可见性：仅在 Java 文件中可见</li>
     *   <li>显示文本：从资源文件获取国际化文本</li>
     *   <li>描述信息：从资源文件获取国际化描述</li>
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
        e.getPresentation().setVisible(psiFile instanceof PsiJavaFile);
        e.getPresentation().setText(JavaDocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavaDocBundle.message("action.generate.javadoc.description"));
    }
}

