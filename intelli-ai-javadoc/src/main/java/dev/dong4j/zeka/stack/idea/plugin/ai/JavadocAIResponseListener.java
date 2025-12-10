package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * JavaDocAIResponseListener 类
 * <p>
 * 用于监听 AI 请求和响应过程, 提供详细的日志记录功能. 该类实现了 AIResponseListener 接口, 主要职责是在 AI 请求, 响应和使用 Token 时进行日志输出, 便于调试和监控 AI 操作流程.
 * <p>
 * 当启用详细日志模式时, 会记录请求内容, 响应内容以及 Token 使用情况, 日志信息包含提供者名称, 模型名称和相关数据, 有助于追踪 AI 调用的完整生命周期.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
public class JavadocAIResponseListener implements AIResponseListener {

    /**
     * 当前操作的项目对象
     * <p>
     * 该字段用于存储与当前操作相关的项目信息, 不可变
     */
    private final Project project;

    /**
     * 初始化 JavaDocAIResponseListener 实例
     * <p>
     * 创建一个用于处理 Javadoc AI 响应的监听器, 关联到指定的项目并设置日志详细程度.
     *
     * @param project 关联的项目对象, 用于操作项目相关资源
     * @since 1.0
     */
    public JavadocAIResponseListener(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理请求并记录相关信息
     * <p>
     * 当启用详细日志时, 该方法会打印请求的提供者名称和模型名称, 并在请求体不为空时打印请求体内容.
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容, 可能为 null 或空字符串
     * @param validation   是否进行验证
     * @since 1.0
     */
    @Override
    public void onRequest(String providerName, String modelName, String requestBody, boolean validation) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("请求: %s - %s", providerName, modelName));
        if (requestBody != null && !requestBody.isEmpty()) {
            AIConsoleLoggerUtil.print(project, requestBody);
        }
    }

    /**
     * 处理来自指定提供者的模型响应
     * <p>
     * 当接收到响应时, 若启用了详细日志记录, 则打印带有时间戳的响应信息及响应体内容.
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param responseBody 响应体内容, 可能为 null 或空字符串
     * @param validation   是否进行了验证
     * @since 1.0
     */
    @Override
    public void onResponse(String providerName, String modelName, String responseBody, boolean validation) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("响应: %s - %s", providerName, modelName));
        if (responseBody != null && !responseBody.isEmpty()) {
            AIConsoleLoggerUtil.print(project, responseBody);
        }
    }

    /**
     * 记录指定模型的 token 使用情况
     * <p>
     * 当启用详细日志时, 将提供者名称, 模型名称及各类 token 数量打印到控制台.
     *
     * @param providerName     提供者名称
     * @param modelName        模型名称
     * @param promptTokens     提示词使用的 token 数量
     * @param completionTokens 完成内容使用的 token 数量
     * @param totalTokens      总共使用的 token 数量
     */
    @Override
    public void onUsage(String providerName, String modelName,
                        int promptTokens, int completionTokens, int totalTokens) {
        AIConsoleLoggerUtil.print(project,
                                  String.format("Token 使用: %s | %s | Prompt: %d | Completion: %d | Total: %d",
                                                providerName, modelName, promptTokens, completionTokens, totalTokens));
    }
}

