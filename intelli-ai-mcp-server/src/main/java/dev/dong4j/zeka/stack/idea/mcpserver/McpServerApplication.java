package dev.dong4j.zeka.stack.idea.mcpserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.dong4j.zeka.stack.idea.plugin.common.util.CoreEnvironment;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

/**
 * IntelliAI MCP Server 入口
 * <p>
 * 将 intelli-ai-core 的 Javadoc / Changelog 生成能力以 MCP 协议(stdio)暴露给
 * 外部 AI Agent(ZCode / Claude Code / Cursor 等), 作为 IDEA 插件之外的第二种宿主形态.
 * <p>
 * 暴露的工具:
 * <ul>
 *   <li>{@code generate_javadoc}: 源码文本 -> 带 Javadoc 注释的代码</li>
 *   <li>{@code generate_changelog}: 仓库路径 + 提交范围 -> 变更日志 / commit message</li>
 * </ul>
 * 协议约束: stdio 模式下 stdout 专用于 JSON-RPC 报文, 所有日志走 SLF4J 并由
 * logback 配置输出到 stderr(见 resources/logback.xml), 切勿打印到 stdout.
 * <p>
 * 客户端配置示例(ZCode/Claude Code):
 * <pre>
 * {
 *   "mcpServers": {
 *     "intelli-ai": {
 *       "command": "java",
 *       "args": ["-jar", "/path/to/intelli-ai-mcp-server.jar"],
 *       "env": { "INTELLIAI_API_KEY": "sk-xxx" }
 *     }
 *   }
 * }
 * </pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class McpServerApplication {

    /** 服务名称, MCP 客户端据此识别 */
    private static final String SERVER_NAME = "intelli-ai-mcp-server";

    /** 服务版本, 与 gradle.properties 的 serverVersion 保持一致 */
    private static final String SERVER_VERSION = "2026.2.1005";

    /**
     * 私有构造函数, 防止实例化入口类
     */
    private McpServerApplication() {
    }

    /**
     * 程序入口
     * <p> 装配配置 -> 注册 core SPI -> 构建 stdio MCP server 并阻塞监听;
     * 进程生命周期由 MCP 客户端管理(客户端退出时关闭 stdin, server 随之退出).
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        ServerConfig config = ServerConfig.load();

        // 注册 core 宿主 SPI: 版本信息(User-Agent)与凭证存储
        CoreEnvironment.setVersionProvider(() -> SERVER_NAME + "/" + SERVER_VERSION);
        ServerConfig.registerCredentialStore(config.apiKey());

        JavadocTool javadocTool = new JavadocTool(config);
        ChangelogTool changelogTool = new ChangelogTool(config);
        ListProvidersTool listProvidersTool = new ListProvidersTool();

        // stdio 传输: 请求/响应走 stdin/stdout(JSON-RPC 2.0), 日志必须走 stderr
        StdioServerTransportProvider transportProvider =
            new StdioServerTransportProvider(new ObjectMapper());

        McpServer.sync(transportProvider)
            .serverInfo(SERVER_NAME, SERVER_VERSION)
            .capabilities(McpSchema.ServerCapabilities.builder()
                              .tools(true)
                              .logging()
                              .build())
            .tools(
                buildJavadocToolSpec(javadocTool),
                buildChangelogToolSpec(changelogTool),
                buildListProvidersToolSpec(listProvidersTool))
            .build();

        log.info("IntelliAI MCP Server 已启动(name={}, tools=[generate_javadoc, generate_changelog, list_providers])",
                 SERVER_NAME);
        // server 在后台线程监听 stdio, 主线程阻塞等待, 避免 JVM 提前退出;
        // 客户端关闭 stdin 时进程随之结束
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("收到中断信号, MCP Server 退出");
        }
    }

    /**
     * 构建 generate_javadoc 工具规格
     *
     * @param tool 工具处理器
     * @return 同步工具规格
     */
    private static McpServerFeatures.SyncToolSpecification buildJavadocToolSpec(JavadocTool tool) {
        McpSchema.Tool toolDef = new McpSchema.Tool(
            "generate_javadoc",
            "为 Java 源码生成 Javadoc 注释。输入源码文本, 返回带注释的完整代码。"
                + "需要先配置 API Key(环境变量 INTELLIAI_API_KEY 或 ~/.intelli-ai/mcp-server.properties)。",
            """
                {
                  "type": "object",
                  "properties": {
                    "sourceCode": {
                      "type": "string",
                      "description": "待生成 Javadoc 注释的源码文本"
                    },
                    "elementKind": {
                      "type": "string",
                      "enum": ["class", "method", "field", "test"],
                      "description": "代码元素类型, 决定使用的 prompt 模板, 默认 method"
                    },
                    "responseLanguage": {
                      "type": "string",
                      "description": "注释语言, 默认 中文"
                    },
                    "provider": {
                      "type": "string",
                      "description": "AI 服务商枚举名(如 DEEPSEEK/ZHIPU/ORCAROUTER), 覆盖启动配置; 需该平台密钥已配置"
                    },
                    "model": {
                      "type": "string",
                      "description": "模型名, 覆盖启动配置或该服务商默认模型"
                    }
                  },
                  "required": ["sourceCode"]
                }
                """);
        return new McpServerFeatures.SyncToolSpecification(toolDef,
            (exchange, args) -> handleToolCall(toolNameOf(exchange), () -> tool.execute(args)));
    }

    /**
     * 构建 generate_changelog 工具规格
     *
     * @param tool 工具处理器
     * @return 同步工具规格
     */
    private static McpServerFeatures.SyncToolSpecification buildChangelogToolSpec(ChangelogTool tool) {
        McpSchema.Tool toolDef = new McpSchema.Tool(
            "generate_changelog",
            "基于本地 Git 仓库生成变更日志或 commit message。输入仓库路径与提交范围, "
                + "返回 AI 生成的文本。需要先配置 API Key。",
            """
                {
                  "type": "object",
                  "properties": {
                    "repoPath": {
                      "type": "string",
                      "description": "本地 Git 仓库绝对路径"
                    },
                    "commitHashes": {
                      "type": "array",
                      "items": { "type": "string" },
                      "description": "提交哈希列表, 与 range 二选一"
                    },
                    "range": {
                      "type": "string",
                      "description": "提交范围, 如 v1.0.0..HEAD"
                    },
                    "type": {
                      "type": "string",
                      "enum": ["changelog", "commit_message"],
                      "description": "生成类型, 默认 changelog"
                    },
                    "version": {
                      "type": "string",
                      "description": "版本号, 写入 changelog 标题, 默认 unreleased"
                    },
                    "responseLanguage": {
                      "type": "string",
                      "description": "输出语言, 默认 中文"
                    },
                    "provider": {
                      "type": "string",
                      "description": "AI 服务商枚举名(如 DEEPSEEK/ZHIPU/ORCAROUTER), 覆盖启动配置; 需该平台密钥已配置"
                    },
                    "model": {
                      "type": "string",
                      "description": "模型名, 覆盖启动配置或该服务商默认模型"
                    }
                  },
                  "required": ["repoPath"]
                }
                """);
        return new McpServerFeatures.SyncToolSpecification(toolDef,
            (exchange, args) -> handleToolCall(toolNameOf(exchange), () -> tool.execute(args)));
    }

    /**
     * 构建 list_providers 工具规格
     *
     * @param tool 工具处理器
     * @return 同步工具规格
     */
    private static McpServerFeatures.SyncToolSpecification buildListProvidersToolSpec(ListProvidersTool tool) {
        McpSchema.Tool toolDef = new McpSchema.Tool(
            "list_providers",
            "列出 MCP Server 支持的全部 AI 服务商(INTELLIAI_PROVIDER 取值)、默认模型与端点。"
                + "无需 API Key, 用于配置前自助查询。",
            "{\"type\":\"object\",\"properties\":{}}");
        return new McpServerFeatures.SyncToolSpecification(toolDef,
            (exchange, args) -> handleToolCall("list_providers", tool::execute));
    }

    /**
     * 统一的工具调用包装
     * <p> 业务异常转为 isError 结果返回给客户端(而非协议错误),
     * 便于 agent 读到明确的失败原因并自行调整参数.
     *
     * @param request  工具调用请求
     * @param supplier 业务执行体
     * @return 工具调用结果
     */
    private static McpSchema.CallToolResult handleToolCall(String toolName,
                                                           ToolInvoker supplier) {
        try {
            String content = supplier.invoke();
            return new McpSchema.CallToolResult(
                java.util.List.of(new McpSchema.TextContent(content == null ? "" : content)), null);
        } catch (Exception e) {
            log.error("工具调用失败: {}", toolName, e);
            return new McpSchema.CallToolResult(
                java.util.List.of(new McpSchema.TextContent("工具执行失败: " + e.getMessage())), true);
        }
    }

    /**
     * 获取工具名称(仅用于日志)
     *
     * @param exchange 同步交换对象
     * @return 工具名, 不可用时返回 unknown
     */
    private static String toolNameOf(McpSyncServerExchange exchange) {
        try {
            var spec = exchange;
            return spec != null ? "tool" : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 业务执行体函数式接口
     *
     * @throws Exception 业务异常
     */
    @FunctionalInterface
    private interface ToolInvoker {
        /**
         * 执行业务逻辑
         *
         * @return 结果文本
         * @throws Exception 业务异常
         */
        String invoke() throws Exception;
    }
}
