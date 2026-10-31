# IntelliAI MCP Server

将 [IntelliAI Engine](../intelli-ai-engine) 插件的 AI 生成能力（Javadoc 注释 / Changelog 变更日志）以
[MCP (Model Context Protocol)](https://modelcontextprotocol.io) 标准协议暴露给外部 AI Agent，作为 IDEA 插件之外的**独立宿主形态**。

- 无需安装 IDEA，单个 fat jar 即可运行
- 与插件共享同一份核心代码（[intelli-ai-core](../intelli-ai-core)）与默认 prompt 模板，行为一致、不会漂移
- stdio 传输，任何支持 MCP 的客户端（ZCode / Claude Code / Cursor / Claude Desktop 等）均可接入

## 暴露的工具

| 工具 | 入参 | 说明 |
|---|---|---|
| `generate_javadoc` | `sourceCode`（必填）、`elementKind`（class/method/field/test）、`responseLanguage` | 源码文本 → 带 Javadoc 注释的完整代码 |
| `generate_changelog` | `repoPath`（必填）、`commitHashes` 或 `range`、`type`（changelog/commit_message）、`version`、`responseLanguage` | 本地 Git 仓库 → AI 变更日志 |
| `list_providers` | 无 | 列出支持的 33 家服务商、默认模型与端点（无需 API Key） |

## 构建

```bash
./gradlew shadowJar
# 产物: build/libs/intelli-ai-mcp-server.jar
```

要求 JDK 21+。

## 配置

配置来源优先级：**环境变量 > 配置文件 > 内置默认值**。

支持的环境变量：

| 环境变量 | 说明 |
|---|---|
| `INTELLIAI_API_KEY` | API 密钥（推荐，避免落盘） |
| `INTELLIAI_PROVIDER` | 供应商标识，**与 IDEA 插件同源的 33 家全量支持**（见下表），默认 `QIANWEN` |
| `INTELLIAI_BASE_URL` | 覆盖默认端点（可选，各服务商自带默认值） |
| `INTELLIAI_MODEL` | 覆盖默认模型（可选，各服务商自带默认值） |

支持的服务商与 `INTELLIAI_PROVIDER` 取值（枚举名，与插件设置页一一对应）：

`FREEAI / OPENAI / ANTHROPIC / NVIDIA / HUGGINGFACE / OPENROUTER / ORCAROUTER / CLOUDFLARE / BEDROCK / AZURE / GITHUB_MODELS / MISTRAL / DEEPSEEK / DEEPSEEK_ANTHROPIC / DOUBAO / DOUBAO_ANTHROPIC / GROK / HUNYUAN / HUNYUAN_ANTHROPIC / MOONSHOT / MOONSHOT_ANTHROPIC / QIANWEN / SILICONFLOW / OLLAMA / LM_STUDIO / QUOTIO / MODELSCOPE / MODELSCOPE_ANTHROPIC / IFLOW / ZHIPU / ZAI / ZHIPU_ANTHROPIC / ZAI_ANTHROPIC`

说明：

- 每家自带默认端点与默认模型，只配 `provider` + `api_key` 即可；`OLLAMA` / `LM_STUDIO` 是本地服务，无需 API Key
- 也可以直接调用 `list_providers` 工具，agent 会返回全部服务商的默认模型与端点表格
- `provider` 值不区分大小写，同时接受常见别名（`dashscope`→`QIANWEN`、`glm`→`ZHIPU` 等）；无法识别时报错会列出全部合法值

也可以使用配置文件 `~/.intelli-ai/mcp-server.properties`：

```properties
provider=qianwen
base_url=https://dashscope.aliyuncs.com/compatible-mode/v1
model=qwen-plus
api_key=sk-xxx
```

## 客户端接入

以 ZCode / Claude Code 的 `mcp.json` 为例：

```json
{
  "mcpServers": {
    "intelli-ai": {
      "command": "java",
      "args": ["-jar", "/path/to/intelli-ai-mcp-server.jar"],
      "env": { "INTELLIAI_API_KEY": "sk-xxx" }
    }
  }
}
```

> 注意：`java` 需为 JDK 21+。若客户端环境默认 JDK 版本较低，可将 `args` 改为
> `["-jar", "..."]` 换成绝对路径的 21+ java 可执行文件。

## 手动协议调试

```bash
{
  echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"t","version":"1"}}}'
  sleep 3
  echo '{"jsonrpc":"2.0","method":"notifications/initialized"}'
  sleep 1
  echo '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'
  sleep 5
} | java -jar build/libs/intelli-ai-mcp-server.jar
```

约束：stdio 模式下 stdout 是 JSON-RPC 协议通道，服务日志全部走 stderr（logback 配置）。

## 架构说明

```
外部 Agent (ZCode/Claude Code/Cursor)
        │  MCP over stdio (JSON-RPC 2.0)
        ▼
intelli-ai-mcp-server.jar   ← 本工程: 协议层 + 工具编排 + 配置装配
        │
        ▼
intelli-ai-core             ← 纯 JVM 核心: AI 执行链 / JGit 读取 / prompt 模板
```

与 IDEA 插件（intelli-ai-javadoc / intelli-ai-changelog）是同一 core 的两种宿主：
插件宿主提供 PSI 语义分析与 IDE 集成，MCP 宿主提供跨工具的标准协议接入。
