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
 * JavaDoc 自动生成动作（快捷键入口）
 *
 * <p>通过快捷键（Ctrl+Shift+D / Cmd+Shift+D）触发 JavaDoc 生成。
 * 根据光标位置智能识别要生成文档的元素（方法、字段、类或整个文件）。
 * 作为插件的核心入口点，提供最快捷的文档生成方式。
 *
 * <p>v2.0 改进：
 * <ul>
 *   <li>使用新的 Task 异步架构，不阻塞 UI</li>
 *   <li>支持进度显示和取消操作</li>
 *   <li>使用 TaskCollector 和 TaskExecutor 统一处理</li>
 *   <li>支持多种 AI 提供商（通义千问、Ollama 等）</li>
 *   <li>自动格式化生成的 JavaDoc</li>
 *   <li>智能定位：根据光标位置自动识别要生成文档的元素（方法、字段、类）</li>
 *   <li>使用通知系统替代弹窗，提升用户体验</li>
 * </ul>
 *
 * <p>主要特性：
 * <ul>
 *   <li>支持普通方法和测试方法的注释生成</li>
 *   <li>自动识别 JUnit 4 和 JUnit 5 的测试方法</li>
 *   <li>跳过已有 JavaDoc 注释的类和方法</li>
 *   <li>在项目索引期间禁用（避免影响性能）</li>
 *   <li>仅在 Java 文件中可用</li>
 * </ul>
 *
 * <p>核心功能：
 * <ul>
 *   <li>快捷键触发：Ctrl+Shift+D (Win/Linux) 或 Cmd+Shift+D (Mac)</li>
 *   <li>智能元素识别：根据光标位置自动识别处理范围</li>
 *   <li>异步处理：后台生成文档，不阻塞 UI</li>
 *   <li>进度显示：实时显示处理进度和统计信息</li>
 *   <li>结果通知：处理完成后显示友好的通知消息</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavaDocShortcutAction extends AnAction {

    /**
     * 执行动作的主方法
     *
     * <p>当用户触发该动作时（通过快捷键），此方法会被调用。
     *
     * <p>v2.0 执行流程（带智能定位）：
     * <ol>
     *   <li>检查项目是否有效以及是否处于索引状态</li>
     *   <li>获取当前编辑器和 PSI 文件</li>
     *   <li>验证当前文件是否为 Java 文件</li>
     *   <li>使用 PsiElementLocator 根据光标位置智能定位元素</li>
     *   <li>使用 TaskCollector 收集需要生成文档的任务</li>
     *   <li>在后台任务中使用 TaskExecutor 处理任务</li>
     *   <li>显示进度和统计信息</li>
     * </ol>
     *
     * <p>智能定位逻辑：
     * <ul>
     *   <li>光标在方法上 → 只为该方法生成</li>
     *   <li>光标在字段上 → 只为该字段生成</li>
     *   <li>光标在类声明上 → 只为该类生成</li>
     *   <li>光标在类内部（但不在特定成员上）→ 为整个类及所有成员生成</li>
     *   <li>其他情况 → 为整个文件生成</li>
     * </ul>
     *
     * <p>注意：使用异步任务处理，不会阻塞 UI 线程。
     *
     * @param e 动作事件对象，包含触发动作的上下文信息（项目、编辑器等）
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || isDumbMode(project)) {
            return;
        }

        log.info("Generate JavaDoc Action (Legacy) for project {} started", project.getName());

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        // 智能定位：根据光标位置确定要生成文档的元素
        TaskCollector collector = new TaskCollector(project);
        List<DocumentationTask> tasks;

        String elementDesc = "文件";
        if (editor == null) {
            // 如果在项目树的文件上使用快捷键则为整个文件生成 javadoc
            tasks = collector.collectFromFile(psiFile);
        } else {
            PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElement(editor, psiFile);
            if (locateResult == null) {
                log.warn("无法定位到有效的 PSI 元素");
                return;
            }

            elementDesc = PsiElementLocator.getElementDescription(locateResult.element());
            log.info("智能定位到: {}", elementDesc);

            // 根据定位结果收集任务
            tasks = collector.collectFromElement(locateResult.element());
        }

        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        if (service.checkEmptyTasks(project, tasks, JavaDocBundle.message("notification.no.task.default"))) {
            return;
        }

        log.info("收集到 {} 个任务", tasks.size());

        // 使用服务生成文档
        service.generateDocumentation(project, tasks, elementDesc);
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

    /**
     * 获取用于更新操作的线程类型
     * <p>
     * 返回在后台线程中执行更新操作的线程类型，以避免阻塞事件调度线程（EDT）。
     *
     * @return 更新操作所使用的线程类型
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 在后台线程中执行 update，避免阻塞 EDT
        return ActionUpdateThread.BGT;
    }

    /**
     * 更新动作的可用状态
     *
     * <p>该方法在 IDE 显示动作之前被调用，用于决定该动作是否应该被启用。
     *
     * <p>启用条件：
     * <ul>
     *   <li>当前有打开的 PSI 文件</li>
     *   <li>文件是 Java 文件</li>
     * </ul>
     *
     * <p>如果条件不满足，该动作会在 UI 中被禁用（灰色显示）。
     *
     * @param e 动作事件对象
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        e.getPresentation().setEnabled(psiFile instanceof PsiJavaFile);
        e.getPresentation().setText(JavaDocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavaDocBundle.message("action.generate.javadoc.shortcut.description"));
    }
}
