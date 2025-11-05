package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.ide.presentation.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskExecutor;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.util.PsiElementLocator;
import lombok.extern.slf4j.Slf4j;

/**
 * Intention Action - 在光标位置按 Option+Enter (Mac) 或 Alt+Enter (Win/Linux) 时显示的快捷菜单
 *
 * <p>这个 Action 会出现在 IDEA 的"快速修复"（Quick Fix）菜单中，
 * 让用户可以方便地为当前光标位置的元素生成 JavaDoc。
 * 作为最便捷的入口点，提供上下文相关的文档生成功能。
 *
 * <p>核心功能：
 * <ul>
 *   <li>集成到 IntelliJ 的 Intention 系统</li>
 *   <li>智能上下文识别</li>
 *   <li>条件性显示</li>
 *   <li>异步任务处理</li>
 * </ul>
 *
 * <p>显示条件：
 * <ul>
 *   <li>当前文件是 Java 文件</li>
 *   <li>光标在可以添加 JavaDoc 的元素上（类、方法、字段）</li>
 *   <li>该元素还没有 JavaDoc 注释</li>
 * </ul>
 *
 * <p>智能定位：
 * <ul>
 *   <li>光标在方法上 → 为该方法生成</li>
 *   <li>光标在字段上 → 为该字段生成</li>
 *   <li>光标在类上 → 为该类及所有成员生成</li>
 * </ul>
 *
 * <p>与其他入口点的区别：
 * <ul>
 *   <li>只在元素没有文档时显示</li>
 *   <li>不处理整个文件</li>
 *   <li>更精确的上下文感知</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see PsiElementBaseIntentionAction
 * @see IntentionAction
 * @since 1.0.0
 */
@Slf4j
@Presentation(icon = "AllIcons.Actions.IntentionBulb")
public class GenerateJavaDocIntentionAction extends PsiElementBaseIntentionAction {

    /**
     * 获取 Action 显示的文本
     *
     * <p>返回在 Intention 菜单中显示的文本。
     * 使用国际化资源文件获取文本，支持多语言。
     *
     * @return Action 名称
     * @see JavaDocBundle#message(String, Object...)
     */
    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getText() {
        return JavaDocBundle.message("action.generate.javadoc");
    }

    /**
     * 获取 Action 的家族名称（用于分组）
     *
     * <p>返回 Action 的家族名称，用于在 Intention 设置中分组显示。
     * 使用插件名称作为家族名称，便于用户识别和管理。
     *
     * @return 家族名称
     * @see JavaDocBundle#message(String, Object...)
     */
    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return JavaDocBundle.message("plugin.name");
    }

    /**
     * 检查该 Action 是否可用
     *
     * <p>决定 Intention Action 是否在当前上下文中显示。
     * 只有在特定条件下才显示，避免干扰用户。
     *
     * <p>检查流程：
     * <ol>
     *   <li>验证文件类型（必须是 Java 文件）</li>
     *   <li>智能定位元素</li>
     *   <li>排除文件级别的操作</li>
     *   <li>检查元素是否已有文档（如果允许覆盖则忽略）</li>
     * </ol>
     *
     * <p>显示策略：
     * <ul>
     *   <li>只在 Java 文件中显示</li>
     *   <li>只在可添加文档的元素上显示</li>
     *   <li>如果 overrideExisting=false，只在元素没有文档时显示</li>
     *   <li>如果 overrideExisting=true，无论是否有文档都显示</li>
     *   <li>不处理文件级别的操作</li>
     * </ul>
     *
     * @param project 项目
     * @param editor  编辑器
     * @param element PSI 元素
     * @return 如果可用返回 true
     * @see PsiElementLocator#locateElementAtOffset(PsiFile, int)
     */
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();

        // 1. 必须是 Java 文件
        if (!(file instanceof PsiJavaFile)) {
            return false;
        }

        // 2. 智能定位元素
        PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElementAtOffset(file, editor.getCaretModel().getOffset());

        if (locateResult == null) {
            return false;
        }

        // 3. 如果是整个文件，不在 Intention 中显示（避免重复）
        if (locateResult.type() == PsiElementLocator.LocateType.FILE) {
            return false;
        }

        // 4. 检查元素是否可以有 JavaDoc
        if (!(locateResult.element() instanceof PsiDocCommentOwner docOwner)) {
            return false;
        }

        // 5. 获取配置：是否允许覆盖现有文档
        SettingsState settings = SettingsState.getInstance();
        boolean overrideExisting = settings.overrideExisting;

        // 6. 根据 overrideExisting 设置决定是否显示
        if (overrideExisting) {
            // 允许覆盖：无论是否已有 JavaDoc 都显示
            return true;
        } else {
            // 不允许覆盖：只在没有 JavaDoc 时显示
            return docOwner.getDocComment() == null;
        }
    }

    /**
     * 执行 Action
     *
     * <p>当用户从 Intention 菜单选择此选项时调用。
     * 执行文档生成的核心逻辑。
     *
     * <p>执行流程：
     * <ol>
     *   <li>验证环境和文件类型</li>
     *   <li>智能定位元素</li>
     *   <li>收集文档生成任务</li>
     *   <li>异步处理任务</li>
     *   <li>显示结果通知</li>
     * </ol>
     *
     * <p>用户体验优化：
     * <ul>
     *   <li>单个任务成功时不显示统计信息</li>
     *   <li>多个任务时显示详细统计</li>
     *   <li>错误情况显示友好的提示</li>
     * </ul>
     *
     * @param project 项目
     * @param editor  编辑器
     * @param element PSI 元素
     * @throws IncorrectOperationException 操作错误时抛出
     * @see TaskCollector#collectFromElement(PsiElement)
     * @see TaskExecutor#processTasks(List)
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
        throws IncorrectOperationException {

        PsiFile psiFile = element.getContainingFile();

        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        // 智能定位
        PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElement(editor, psiFile);

        if (locateResult == null || locateResult.type() == PsiElementLocator.LocateType.FILE) {
            NotificationUtil.notifyNoTask(project, JavaDocBundle.message("notification.no.task.location"));
            return;
        }

        String elementDesc = PsiElementLocator.getElementDescription(locateResult.element());
        log.info("Intention Action - 智能定位到: {}", elementDesc);

        // 收集任务
        TaskCollector collector = new TaskCollector(project);
        List<DocumentationTask> tasks = collector.collectFromElement(locateResult.element());

        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        if (service.checkEmptyTasks(project, tasks, JavaDocBundle.message("notification.no.task.default"))) {
            return;
        }

        log.info("收集到 {} 个任务", tasks.size());

        // 使用服务生成文档，带自定义完成回调
        service.generateDocumentation(project, tasks, elementDesc, stats -> {
            // 如果只有一个任务且成功，不显示统计（用户体验更好）
            if (tasks.size() > 1) {
                showCompletionMessage(project, stats, elementDesc);
            }
        });
    }

    /**
     * 显示完成消息
     *
     * <p>在事件调度线程中显示任务完成的通知消息。
     * 包含目标描述和详细的统计信息。
     * 只在处理多个任务时显示，避免单个任务的成功通知干扰用户。
     *
     * @param project 项目对象
     * @param stats   任务统计信息
     * @param target  目标描述
     * @see NotificationUtil#notifyTargetCompletion(Project, String, int, int, int)
     */
    private void showCompletionMessage(Project project, TaskExecutor.TaskStatistics stats, String target) {
        ApplicationManager.getApplication().invokeLater(() -> NotificationUtil.notifyTargetCompletion(project,
                                                                                                      target,
                                                                                                      stats.completed(),
                                                                                                      stats.failed(),
                                                                                                      stats.skipped()));
    }
}

