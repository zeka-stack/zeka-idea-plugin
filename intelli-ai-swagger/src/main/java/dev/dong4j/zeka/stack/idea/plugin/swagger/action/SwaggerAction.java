package dev.dong4j.zeka.stack.idea.plugin.swagger.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import dev.dong4j.zeka.stack.idea.plugin.swagger.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.swagger.util.SwaggerBundle;
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
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null) {
            NotificationUtil.showError(project, SwaggerBundle.message("error.no.project"));
            return;
        }

        if (psiFile == null) {
            NotificationUtil.showError(project, SwaggerBundle.message("error.no.file"));
            return;
        }

        SettingsState settings = SettingsState.getInstance();
        AIProviderConfig providerConfig = resolveProviderConfig(settings);
        if (providerConfig == null) {
            NotificationUtil.showError(project, SwaggerBundle.message("error.no.ai.provider"));
            return;
        }

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

                String fileName = psiFile.getName();
                String userPrompt = settings.swaggerTemplate.replace("{content}", fileName);
                AIChatRequest request = new AIChatRequest(settings.systemPrompt, userPrompt);

                AIService aiService = ApplicationManager.getApplication().getService(AIService.class);
                try {
                    AIConsoleLoggerUtil.printWithTimestamp(project, "=== Swagger AI Request ===");
                    String result = aiService.generateContent(project, request, providerConfig, null);
                    AIConsoleLoggerUtil.printSuccess(project, "=== Swagger AI Response ===");
                    AIConsoleLoggerUtil.print(project, result);

                    String summary = shorten(result, 200);
                    NotificationUtil.showInfo(project,
                                              SwaggerBundle.message("success.ai.generated", summary));
                } catch (AIServiceException ex) {
                    String message = AIServiceException.build(ex);
                    AIConsoleLoggerUtil.printError(project, message);
                    NotificationUtil.showError(project,
                                               SwaggerBundle.message("error.ai.failed", message));
                }
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
        e.getPresentation().setEnabled(project != null && psiFile != null);
    }

    /**
     * 对字符串进行截断处理, 确保不超过指定的最大长度
     * <p> 该方法会先去除字符串两端的空白字符, 然后检查其长度. 如果长度超过最大值, 则截断并添加省略号 (...).
     *
     * @param text      需要截断的字符串, 不能为 null
     * @param maxLength 截断后的最大长度, 必须大于 0
     * @return 截断后的字符串, 若原字符串长度小于等于最大长度则返回原字符串, 否则返回截断并添加省略号的字符串
     */
    @NotNull
    private static String shorten(@NotNull String text, int maxLength) {
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    /**
     * 解析并返回经过验证的 AI 提供商配置
     * <p> 首先检查本地设置中是否存在有效的提供商配置, 如果存在则返回其副本.
     * 如果不存在, 则从全局 AI 提供商设置中获取经过验证的提供商列表, 并返回第一个提供商的副本.
     * 如果没有经过验证的提供商, 则返回 null.
     *
     * @param settings 当前设置状态对象, 不能为 null
     * @return 经过验证的 AI 提供商配置对象, 如果不存在则返回 null
     */
    @Nullable
    private static AIProviderConfig resolveProviderConfig(@NotNull SettingsState settings) {
        if (settings.providerConfig != null) {
            return settings.providerConfig.copy();
        }
        AIProviderSettings global = AIProviderSettings.getInstance();
        List<AIProviderConfig> verified = global.getVerifiedProviders();
        if (verified.isEmpty()) {
            return null;
        }
        return verified.get(0).copy();
    }
}
