package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.util.PsiElementLocator;
import lombok.extern.slf4j.Slf4j;

/**
 * JavaDoc 生成动作的抽象基类
 *
 * <p>提供统一的文档生成逻辑，减少代码重复。
 * 子类可以根据不同的入口点（右键菜单、快捷键、Intention 等）实现特定的行为。
 *
 * <p>核心功能：
 * <ul>
 *   <li>统一的文档生成流程</li>
 *   <li>智能元素定位（有 editor 时）</li>
 *   <li>文件级别处理（无 editor 时）</li>
 *   <li>任务收集和文档生成</li>
 * </ul>
 *
 * <p>处理策略：
 * <ul>
 *   <li>有 editor：根据光标位置智能定位元素，使用 collectFromElement</li>
 *   <li>无 editor：处理整个文件，使用 collectFromFile</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.4.0
 * @since 1.4.0
 */
@Slf4j
public abstract class AbstractGenerateJavaDocAction extends AnAction {

    public void process(@NotNull AnActionEvent e, boolean needEditor) {
        Project project = e.getProject();
        if (project == null || project.isDisposed() || isDumbMode(project)) {
            return;
        }
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        process(project, editor, psiFile, needEditor);
    }

    protected void process(@NotNull Project project,
                           Editor editor,
                           @NotNull PsiFile psiFile,
                           boolean needEditor) {

        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        if (needEditor && editor == null) {
            // 没有编辑器, 直接退出逻辑
            log.info("[编辑器右键菜单] 没有编辑器对象, 退出逻辑");
            return;
        }

        // 收集任务
        TaskCollector collector = new TaskCollector(project);
        List<DocumentationTask> tasks;
        String targetDescription = psiFile.getName();

        // 如果为 null, 就为整个文件生成 javadoc
        if (editor == null) {
            log.info("为文件生成 JavaDoc: {}", psiFile.getName());
            tasks = collector.collectFromElement(psiFile.getOriginalElement());
        } else {
            log.info("为正在编辑的 Java 文件生成 JavaDoc: {}", psiFile.getName());
            // 定位元素
            PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElement(editor, psiFile);
            if (locateResult == null) {
                // 无法定位，直接退出逻辑
                log.info("[编辑器] 无法定位，退出逻辑");
                onLocateFailed(project);
                return;
            }
            targetDescription = PsiElementLocator.getElementDescription(locateResult.element());
            log.info("智能定位到: {}", targetDescription);
            tasks = collector.collectFromElement(locateResult.element());
        }
        // 生成文档
        generateDocumentation(project, tasks, targetDescription);

    }

    /**
     * 生成文档
     *
     * <p>统一的文档生成逻辑，包括任务检查和文档生成服务调用。
     *
     * @param project           项目对象
     * @param tasks             文档生成任务列表
     * @param targetDescription 目标描述（用于进度显示）
     * @return 如果成功生成返回 true，否则返回 false
     */
    protected boolean generateDocumentation(@NotNull Project project,
                                            @NotNull List<DocumentationTask> tasks,
                                            @NotNull String targetDescription) {
        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        String emptyTaskMessage = getEmptyTaskMessage();
        if (service.checkEmptyTasks(project, tasks, emptyTaskMessage)) {
            return false;
        }

        // 使用服务生成文档
        service.generateDocumentation(project, tasks, targetDescription);
        return true;
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
        e.getPresentation().setVisible(psiFile instanceof PsiJavaFile);
        e.getPresentation().setText(JavaDocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavaDocBundle.message("action.generate.javadoc.description"));
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
     * 获取空任务时的提示消息
     *
     * <p>子类可以重写此方法以提供特定的提示消息。
     *
     * @return 空任务时的提示消息
     */
    @NotNull
    protected String getEmptyTaskMessage() {
        return JavaDocBundle.message("notification.no.task.default");
    }

    /**
     * 定位失败时的回调
     *
     * <p>当无法定位到有效元素时调用此方法。
     * 子类可以重写此方法以提供特定的处理逻辑（如显示通知）。
     *
     * @param project 项目对象
     */
    protected void onLocateFailed(@NotNull Project project) {
        // 默认不做任何处理，子类可以重写
    }

    /**
     * 检查项目是否处于"Dumb Mode"（索引模式）
     *
     * <p>在 IntelliJ IDEA 中，当项目正在进行索引（扫描和分析代码结构）时，
     * 会进入"Dumb Mode"。在此模式下，许多需要代码分析的功能会被禁用。
     *
     * <p>如果项目处于 Dumb Mode，会显示提示对话框告知用户当前无法生成 JavaDoc。
     *
     * @param project 当前项目对象
     * @return 如果处于 Dumb Mode 返回 true，否则返回 false
     */
    private static boolean isDumbMode(Project project) {
        if (DumbService.isDumb(project)) {
            NotificationUtil.notifyIndexing(project);
            return true;
        }
        return false;
    }
}

