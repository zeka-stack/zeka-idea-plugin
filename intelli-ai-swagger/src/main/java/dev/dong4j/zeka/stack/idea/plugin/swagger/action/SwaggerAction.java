package dev.dong4j.zeka.stack.idea.plugin.swagger.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.swagger.service.SwaggerGenerationService;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerBundle;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerMethodLocator;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerSpringUtil;
import icons.SwaggerIcons;

/**
 * Swagger 操作类
 * <p> 提供在 IDE 中通过 AI 生成 Swagger 接口文档的功能, 支持从当前文件内容提取信息并调用 AI 服务进行转换
 * <p> 主要功能包括:
 * <ul>
 *     <li> 检查项目和文件是否存在 </li>
 *     <li> 读取配置的 AI 提供商设置 </li>
 *     <li> 调用 AI 服务生成接口文档 </li>
 *     <li> 显示生成结果并处理异常情况 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
public class SwaggerAction extends AnAction {

    /**
     * SwaggerAction 构造函数
     * <p> 初始化 SwaggerAction 实例, 设置动作标题, 描述和图标
     *
     */
    public SwaggerAction() {
        super(
            SwaggerBundle.message("action.swagger.title"),
            SwaggerBundle.message("action.swagger.description"),
            SwaggerIcons.SWAGGER_16
             );
    }

    /**
     * 执行右键菜单操作, 调用 AI 生成 Swagger 文档
     * <p> 此方法在用户选择右键菜单选项时被调用, 读取设置中的提示词,
     * 使用 AI 服务生成 Swagger 文档内容, 并将结果输出到通知或控制台.
     *
     * @param e 表示动作事件的对象
     *          <p> 首先检查项目和文件是否存在, 然后获取 AI 提供者配置.
     *          如果缺少必要的信息, 则显示错误通知.
     *          接着启动后台任务, 在任务中生成 Swagger 文档内容, 并根据生成结果显示成功或失败的通知.
     *
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || project.isDisposed()) {
            NotificationUtil.showError(project, SwaggerBundle.message("error.no.project"));
            return;
        }
        if (DumbService.isDumb(project)) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.indexing"));
            return;
        }

        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        if (!(psiFile instanceof PsiJavaFile)) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.not.java"));
            return;
        }

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        PsiMethod targetMethod = SwaggerMethodLocator.findTargetMethod(psiFile, editor, element);
        if (targetMethod == null) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.no.method"));
            return;
        }

        if (!SwaggerSpringUtil.isSpringControllerMethod(targetMethod)) {
            NotificationUtil.showWarning(project, SwaggerBundle.message("error.not.spring.controller"));
            return;
        }

        SettingsState settings = SettingsState.getInstance();
        if (!AIProviderUtils.hasAIProvider(project,
                                           settings.providerConfig,
                                           SwaggerBundle.message("settings.display.name"),
                                           SwaggerBundle.message("settings.ai.provider.selection"))) {
            return;
        }

        AIProviderConfig providerConfig = settings.providerConfig;

        ProgressManager.getInstance().run(new Task.Backgroundable(
            project,
            SwaggerBundle.message("action.swagger.progress"),
            true
        ) {
            /**
             * 执行 Swagger AI 生成任务
             * <p> 在后台线程中执行 Swagger 文档的 AI 生成过程, 包括设置进度指示器, 构建用户提示, 调用 AI 服务生成内容, 并根据结果展示通知.
             * <p> 该任务会将生成的 Swagger 内容摘要显示为成功通知, 如果发生错误则显示错误通知.
             *
             * @param indicator 进度指示器, 用于控制任务的进度和取消状态
             */
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(SwaggerBundle.message("action.swagger.progress"));
                SwaggerGenerationService service = new SwaggerGenerationService();
                service.generateForMethod(project, targetMethod, settings, providerConfig);
            }
        });
    }

    /**
     * 更新操作的可用状态
     * <p> 根据当前的编辑上下文判断该操作是否可用. 当项目和文件都存在时, 操作可用; 否则不可用.
     *
     * @param e 操作事件, 包含当前的编辑环境信息, 不能为 null
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        boolean enabled = project != null && psiFile instanceof PsiJavaFile;
        if (project != null && DumbService.isDumb(project)) {
            enabled = false;
        }
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
