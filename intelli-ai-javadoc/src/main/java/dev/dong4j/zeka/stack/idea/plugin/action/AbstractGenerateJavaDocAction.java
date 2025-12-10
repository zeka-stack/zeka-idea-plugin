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
import org.jetbrains.kotlin.psi.KtFile;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.util.PsiElementLocator;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象 Javadoc 生成动作类
 * <p>
 * 该类继承自 AnAction, 提供 Javadoc 生成的核心功能实现. 主要负责处理 IDE 中的 Javadoc 生成请求,
 * 包括获取当前项目, 文件和编辑器信息, 定位需要生成 Javadoc 的元素, 并执行具体的生成任务.
 * 支持在编辑器中定位到具体元素进行 Javadoc 生成, 也支持对整个文件进行 Javadoc 生成.
 * 该类采用模板方法模式, 定义了 Javadoc 生成的通用流程, 具体的生成逻辑由子类实现.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractGenerateJavaDocAction extends AnAction {

    /**
     * 处理动作事件, 根据条件执行相应的处理逻辑
     * <p>
     * 该方法首先检查项目是否有效, 若无效则直接返回. 接着获取当前 PsiFile 和 Editor 对象, 若 PsiFile 为空则返回. 最后调用 process 方法进行实际处理.
     *
     * @param e          动作事件对象, 用于获取项目,PsiFile 和 Editor 等信息
     * @param needEditor 是否需要 Editor 对象参与处理
     */
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

    /**
     * 处理生成 Javadoc 的逻辑
     * <p>
     * 根据传入的项目, 编辑器,Psi 文件和是否需要编辑器标志, 判断是否生成 Javadoc.
     * 如果文件不是 Java 文件, 则直接返回. 若需要编辑器但未提供, 则记录日志并返回.
     * 否则, 根据编辑器是否存在, 定位 Psi 元素并收集文档任务, 最后调用生成文档方法.
     *
     * @param project    项目对象
     * @param editor     编辑器对象
     * @param psiFile    Psi 文件对象
     * @param needEditor 是否需要编辑器
     */
    @SuppressWarnings("D")
    protected void process(@NotNull Project project,
                           Editor editor,
                           @NotNull PsiFile psiFile,
                           boolean needEditor) {

        // 检查 AI Provider 配置
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (!AIProviderUtils.hasAIProvider(project, config, PluginContents.PLUGIN_NAME)) {
            return;
        }

        // 检查是否为支持的文件类型（Java 或 Kotlin）
        if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
            return;
        }

        // 检查是否支持 Kotlin
        if (psiFile instanceof KtFile) {
            SettingsState settings = SettingsState.getInstance();
            if (!settings.isLanguageSupported("kotlin")) {
                return;
            }
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

        // 如果为 null, 就为整个文件生成 Javadoc
        if (editor == null) {
            String fileType = psiFile instanceof KtFile ? "Kotlin" : "Java";
            log.info("为文件生成文档: {} ({})", psiFile.getName(), fileType);
            tasks = collector.collectFromElement(psiFile.getOriginalElement());
        } else {
            String fileType = psiFile instanceof KtFile ? "Kotlin" : "Java";
            log.info("为正在编辑的 {} 文件生成文档: {}", fileType, psiFile.getName());
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
     * 生成文档信息
     * <p>
     * 检查任务列表是否为空, 若为空则返回 false; 否则调用服务生成文档.
     *
     * @param project           项目对象
     * @param tasks             任务列表
     * @param targetDescription 目标描述信息
     */
    protected void generateDocumentation(@NotNull Project project,
                                         @NotNull List<DocumentationTask> tasks,
                                         @NotNull String targetDescription) {
        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        String emptyTaskMessage = getEmptyTaskMessage();
        if (service.checkEmptyTasks(project, tasks, emptyTaskMessage)) {
            return;
        }

        // 使用服务生成文档
        service.generateDocumentation(project, tasks, targetDescription);
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
     * @see JavadocBundle
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        boolean isSupportedFile = psiFile instanceof PsiJavaFile || psiFile instanceof KtFile;

        // 如果是 Kotlin 文件，检查是否支持
        if (psiFile instanceof KtFile) {
            SettingsState settings = SettingsState.getInstance();
            isSupportedFile = settings.isLanguageSupported("kotlin");
        }

        e.getPresentation().setEnabled(isSupportedFile);
        e.getPresentation().setVisible(isSupportedFile);
        e.getPresentation().setText(JavadocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavadocBundle.message("action.generate.javadoc.description"));
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
        return JavadocBundle.message("notification.no.task.default");
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
     * <p>如果项目处于 Dumb Mode，会显示提示对话框告知用户当前无法生成 Javadoc。
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

