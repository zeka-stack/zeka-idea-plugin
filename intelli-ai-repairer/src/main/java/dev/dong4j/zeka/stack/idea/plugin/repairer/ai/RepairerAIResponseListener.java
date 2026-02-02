package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * AI 响应日志监听器
 * <p> 输出请求、响应与 token 使用信息到控制台。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.02.02
 * @since 1.0.0
 */
public class RepairerAIResponseListener implements AIResponseListener {
    /** 当前项目实例, 用于日志输出和上下文管理 */
    private final Project project;

    /**
     * 构造函数
     *
     * @param project 当前项目
     */
    public RepairerAIResponseListener(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理 AI 请求事件
     * <p> 在接收到 AI 请求时, 输出请求信息到控制台, 包括提供者名称, 模型名称和请求体内容 (如果存在).
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容, 可能为 null 或空字符串
     * @param validation   是否进行验证
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
     * 处理 AI 响应事件
     * <p> 在接收到 AI 响应时, 打印响应标识信息到控制台, 并在响应内容非空时输出完整响应体
     *
     * @param providerName 服务提供商名称
     * @param modelName    模型名称
     * @param responseBody 响应内容字符串, 可能为 null 或空字符串
     * @param validation   是否已验证响应内容
     */
    @Override
    public void onResponse(String providerName, String modelName, String responseBody, boolean validation) {
        AIConsoleLoggerUtil.printWithTimestamp(project,
                                               String.format("响应: %s - %s", providerName, modelName));
        if (responseBody == null || responseBody.isEmpty()) {
            return;
        }
        String content = extractContent(responseBody);
        if (content.isBlank()) {
            return;
        }
        String structured = formatDiffContent(content);
        AIConsoleLoggerUtil.print(project, structured);
    }

    /**
     * 记录 Token 使用情况到控制台
     * <p> 输出当前 AI 服务提供商名称, 模型名称, 提示词 Token 数, 完成 Token 数, 总 Token 数
     *
     * @param providerName     服务提供商名称
     * @param modelName        模型名称
     * @param promptTokens     提示词使用的 Token 数量
     * @param completionTokens 完成响应使用的 Token 数量
     * @param totalTokens      总共使用的 Token 数量
     */
    @Override
    public void onUsage(String providerName, String modelName,
                        int promptTokens, int completionTokens, int totalTokens) {
        AIConsoleLoggerUtil.print(project,
                                  String.format("Token 使用: %s | %s | Prompt: %d | Completion: %d | Total: %d",
                                                providerName, modelName, promptTokens, completionTokens, totalTokens));
    }

    /**
     * 从 JSON 响应体中提取 content 字段的值
     * <p> 根据固定标记 <code>"content":</code> 定位内容起始位置, 然后解析后续的 JSON 字符串内容, 支持转义字符处理, 直到遇到结束引号为止.
     * <p> 若未找到标记或起始引号, 则返回空字符串.
     *
     * @param responseBody 原始响应体字符串, 可能包含 JSON 格式数据
     * @return 提取的 content 字段内容字符串, 若未找到或解析失败则返回空字符串
     */
    private static String extractContent(String responseBody) {
        String marker = "\"content\":";
        int idx = responseBody.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        int start = responseBody.indexOf('"', idx + marker.length());
        if (start < 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < responseBody.length(); i++) {
            char c = responseBody.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 格式化差异内容为特定结构的字符串
     * <p> 将原始差异内容进行处理, 过滤出 diff 块, 文件头和代码变更行, 并统计 hunk 数量.
     *
     * @param content 需要格式化的原始差异内容字符串
     * @return 格式化后的字符串, 包含 "[AI Diff]" 标识, 差异内容以及 "[Hunks] X" 的统计信息, 其中 X 为 hunk 数量
     */
    private static String formatDiffContent(String content) {
        String diff = content.trim();
        if (diff.isEmpty()) {
            return "";
        }
        String[] lines = diff.split("\n");
        StringBuilder out = new StringBuilder();
        out.append("[AI Diff]\n");
        int hunkCount = 0;
        for (String line : lines) {
            if (line.startsWith("diff --git") || line.startsWith("---") || line.startsWith("+++")) {
                out.append(line).append("\n");
                continue;
            }
            if (line.startsWith("@@")) {
                hunkCount++;
                out.append(line).append("\n");
                continue;
            }
            if (line.startsWith("+") || line.startsWith("-") || line.startsWith(" ")) {
                out.append(line).append("\n");
                continue;
            }
        }
        out.append("[Hunks] ").append(hunkCount);
        return out.toString();
    }
}
