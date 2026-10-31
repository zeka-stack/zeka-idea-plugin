package dev.dong4j.zeka.stack.idea.mcpserver;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * list_providers 工具处理器
 * <p>
 * 返回 MCP Server 支持的全部 AI 服务商清单(与 IDEA 插件同源的 {@link AIProviderType}
 * 全量枚举), 含每家的默认端点与默认模型, 供外部 agent 自助查询并调整配置,
 * 无需翻阅文档。
 * <p>
 * 本工具无入参, 无需 API Key。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ListProvidersTool {

    /**
     * 返回支持的供应商清单
     * <p> 输出为 Markdown 表格: 枚举名(配置用) / 默认模型 / 默认端点 / 是否需要密钥。
     * Ollama 与 LM Studio 为本地服务, 默认指向本机端口且无需 API Key。
     *
     * @return 供应商清单文本
     */
    public @NotNull String execute() {
        String rows = Arrays.stream(AIProviderType.values())
            .map(type -> {
                boolean local = type == AIProviderType.OLLAMA || type == AIProviderType.LM_STUDIO;
                String key = local ? "否(本地服务)" : "是";
                return String.format("| `%s` | %s | %s | %s |",
                                     type.name(), type.getDefaultModel(),
                                     type.getDefaultBaseUrl(), key);
            })
            .collect(Collectors.joining("\n"));

        return """
            当前支持 %d 家 AI 服务商, 与 IDEA 插件完全同源。

            配置方式(二选一):
            1. 环境变量: INTELLIAI_PROVIDER=<下表枚举名> 与 INTELLIAI_API_KEY=<对应平台密钥>
            2. 配置文件 ~/.intelli-ai/mcp-server.properties 的 provider / api_key 键

            未配置 base_url / model 时自动使用各服务商的默认值(下表)。

            | 枚举名(即 INTELLIAI_PROVIDER 取值) | 默认模型 | 默认端点 | 需要 API Key |
            |---|---|---|---|
            %s
            """.formatted(AIProviderType.values().length, rows);
    }
}
