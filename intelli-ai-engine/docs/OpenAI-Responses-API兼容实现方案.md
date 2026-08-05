# OpenAI Responses API 兼容实现方案

## 1. 背景与目标

### 1.1 现状

Engine 的 OpenAI 兼容链路（`AICompatibleProvider` + `BlockingRequestExecutor` / `StreamRequestExecutor`）**硬编码**：

| 环节 | 当前实现 |
|------|----------|
| 端点 | `{baseUrl}/chat/completions` |
| 请求体 | `messages` + `max_tokens` + `temperature` + 可选 `enable_thinking` |
| 阻塞响应 | `choices[0].message.content` |
| 流式响应 | SSE `data:` + `choices[].delta.content` / `reasoning_content`，结束标记 `[DONE]` |
| 思考探测 | 三探针针对厂商扩展字段 `enable_thinking` |

这与绝大多数国内兼容网关（通义、硅基流动、Ollama 兼容模式等）一致，但对 **OpenAI 官方 Responses API**（`POST /v1/responses`）不兼容。

### 1.2 目标（本期）

在**不破坏现有 Chat Completions 默认行为**的前提下，让选定服务商可选用 Responses API，覆盖 Engine 核心能力：

1. 测试连接（阻塞）
2. Chat / 生成任务（阻塞 + 流式）
3. Think / 推理输出展示（映射到 Responses 官方 reasoning 能力）
4. 配置可持久化、可在可用服务商设置对话框中切换

**明确不做（本期）**：内置 tools（web_search / code_interpreter / MCP）、`previous_response_id` 多轮状态、Conversations API、Structured Outputs / function calling 全量迁移。

### 1.3 为何值得做

- OpenAI 推荐新项目用 Responses；推理模型在 Responses 上体验更好（官方 reasoning item / summary）。
- Chat Completions **未宣布废弃**，仍会长期存在；插件主场景是一次性生成（Javadoc / Commit Message），不依赖服务端会话态。
- 部分模型 / 参数组合（尤其 reasoning）会逐步偏向 Responses；提前兼容可降低后续被动迁移成本。

---

## 2. API 差异对照（与本插件相关）

| 维度 | Chat Completions | Responses API |
|------|------------------|---------------|
| 端点 | `/chat/completions` | `/responses` |
| 输入 | `messages[]` | `input`（string 或消息/Item 列表）；系统提示可用 `instructions` |
| 输出上限 | `max_tokens` | `max_output_tokens` |
| 阻塞输出 | `choices[].message.content` | `output[]` 中 `type=message` 的 `output_text`；SDK 有 `output_text` 聚合 |
| 流式协议 | `choices[].delta` + `[DONE]` | 语义事件：`response.output_text.delta`、`response.reasoning_summary_text.delta`、`response.completed` 等 |
| 思考 / 推理 | 厂商扩展 `enable_thinking` 或 `reasoning_content` | 官方 `reasoning` item + `reasoning` 请求参数（effort / summary）；**无 enable_thinking** |
| 存储 | 视账号默认 | 默认 `store=true`；插件应强制 `store: false`（隐私 / 无状态） |
| 多服务商 | 几乎所有兼容网关支持 | 仅部分支持（OpenAI、Azure、OpenRouter、部分本地运行时）；多数国内兼容模式仍只有 Chat |

结论：**不能全局切换为 Responses**，必须按服务商配置 opt-in。

---

## 3. 方案选项

### 选项 A（推荐）：协议枚举 + 双栈实现，默认 Chat Completions

- 新增 `ApiProtocol { CHAT_COMPLETIONS, RESPONSES }`，挂在 `AIProviderConfig`（随可用服务商持久化）。
- 默认 `CHAT_COMPLETIONS`，老配置反序列化为默认值，零破坏。
- OpenAI / Azure OpenAI / Custom（及明确支持的 provider）在高级设置中展示「API 协议」下拉。
- 请求构建、阻塞解析、流式解析按协议分流；共享 HTTP / 鉴权 / 取消逻辑。

| 项 | 评估 |
|----|------|
| 工作量 | 中（约 8–12 个核心文件 + 文档） |
| 风险 | 低：默认路径不变 |
| 依赖现有代码 | 复用 `BlockingRequestExecutor` / `StreamRequestExecutor` 的 URL 重载与连接调谐 |

### 选项 B：测试连接时自动探测 `/responses` vs `/chat/completions`

- 在连通性成功后额外探测端点可用性并自动写入协议。
- 优点：用户无感；缺点：误判（代理透传 404/200）、多一次请求、与三探针叠加耗时。
- 建议作为 **Phase 2**，本期不做。

### 选项 C：仅 OPENAI 服务商硬切 Responses

- 工作量小，但破坏现有 OpenAI Chat 用户；Azure / 代理 / 兼容网关易踩坑。
- **不推荐**。

**推荐：选项 A。**

---

## 4. 推荐方案详细设计

### 4.1 配置模型

```text
AIProviderConfig.apiProtocol = ApiProtocol.CHAT_COMPLETIONS | RESPONSES
```

- 默认：`CHAT_COMPLETIONS`
- UI：`AvailableProviderSettingsDialog`（及测试前模板）增加「API 协议」
- `copy()` / `contentEquals()` / `isModified` 同步

### 4.2 请求构建

#### Chat Completions（保持现状）

```json
{
  "model": "...",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "stream": true,
  "max_tokens": 2048,
  "enable_thinking": true
}
```

（`enable_thinking` 仍由三探针 + 用户勾选决定。）

#### Responses

```json
{
  "model": "...",
  "instructions": "<systemPrompt>",
  "input": "<userPrompt 或 [{role,content}]>",
  "stream": true,
  "max_output_tokens": 2048,
  "store": false,
  "reasoning": { "effort": "medium", "summary": "auto" }
}
```

映射规则：

| 现有配置 | Responses 字段 |
|----------|----------------|
| systemPrompt | `instructions` |
| userPrompt | `input`（首期用 string；若需严格对齐可改为 message 列表） |
| maxTokens | `max_output_tokens`（K 单位换算逻辑复用） |
| temperature / topP 等 | 仍按「非 auto 才写入」；若某模型拒绝再按错误提示收敛 |
| enableThinking / Think 勾选 | **不写 `enable_thinking`**；勾选时写 `reasoning`（effort/summary）；关闭时省略 `reasoning` 或 `effort: "none"`（以实现时官方文档为准，默认「省略」） |
| 隐私 | 始终 `store: false` |

### 4.3 端点选择

```text
CHAT_COMPLETIONS → baseUrl + "/chat/completions"
RESPONSES        → baseUrl + "/responses"
```

Azure：继续走现有 `buildRequestUrl`，后缀由 `/chat/completions` 改为 `/responses`（部署名路径规则需在实现时对照 Azure 文档验证）。

### 4.4 阻塞响应解析

- Chat：保持 `choices[0].message.content`
- Responses：遍历 `output[]`：
  - `type == "message"` → 拼接 `content[].type == "output_text"` 的 `text`
  - `type == "reasoning"` → 可选提取 `summary[].text` 写入日志（Think 展示）
- usage：兼容 `usage.input_tokens` / `output_tokens`（Responses 命名）与旧字段

### 4.5 流式解析

在 `StreamRequestExecutor` / `StreamParseEngine` 旁增加 **Responses 事件策略**（或独立 `ResponsesStreamParser`）：

| 事件 type | 行为 |
|-----------|------|
| `response.output_text.delta` | `listener.onChunk(delta)` |
| `response.reasoning_summary_text.delta`（及必要时 `response.reasoning_text.delta`） | 日志区 Think 样式输出 |
| `response.completed` | 结束流，汇总 usage |
| `error` / `response.failed` | `onError` |
| 其他事件 | 忽略 |

不再依赖 `[DONE]` 作为唯一结束条件（可同时兼容）。

### 4.6 与思考能力三探针的关系

| 协议 | 探测策略 |
|------|----------|
| Chat Completions | 保持现有 `enable_thinking` 三探针 |
| Responses | **跳过** `enable_thinking` 三探针；改为轻量连通即可。Think UI 文案改为「使用官方 reasoning 参数」；探测结果可写 `capability=OPTIONAL` 或新增 `NATIVE_REASONING`（若引入枚举需谨慎序列化兼容） |

建议本期：Responses 下 `thinkingProbeResult` 标记为跳过或合成「原生 reasoning」，避免误导性的 `enable_thinking` 失败摘要。

### 4.7 适用范围（UI 可见性）

首期对以下类型展示协议开关：

- `OPENAI`
- `AZURE_OPENAI`（若枚举存在）
- `CUSTOM` / 明确 OpenAI 兼容自定义
- 可选：`OPENROUTER`、`GITHUB_MODELS`（实现前抽样验证）

**不对**通义 compatible-mode、硅基流动等默认暴露（避免用户误选导致测试失败）。若 Custom 选了 Responses 但网关不支持，测试连接应给出清晰错误（HTTP 404/405 + 文案提示改回 Chat Completions）。

### 4.8 架构示意

```text
AICompatibleProvider
  ├─ resolveEndpoint(protocol)
  ├─ buildRequestBody(...)     ──► ChatCompletionsBodyBuilder
  │                            ──► ResponsesBodyBuilder
  ├─ BlockingRequestExecutor   ──► parseChat / parseResponses
  └─ StreamRequestExecutor     ──► ChatDeltaParser / ResponsesEventParser
```

---

## 5. 攻击面评估（推荐方案）

| 角度 | 结论 |
|------|------|
| 依赖失败 | 默认仍走 Chat；Responses 仅 opt-in，网关不支持时测连失败即可回退 |
| 规模爆炸 | 双解析器按协议分支，无额外状态存储（`store:false`） |
| 回滚成本 | 配置改回 `CHAT_COMPLETIONS`；无需数据迁移 |
| 前提坍塌 | 「多数兼容网关支持 Responses」不成立 → 已用默认 Chat + 白名单 UI 对冲 |

最脆弱前提：Azure / 第三方对 `/responses` 路径与字段不完全一致。缓解：Azure 单独路径拼接 + 测连验证；Custom 失败时错误文案引导。

---

## 6. 涉及文件（预估）

| 文件 | 变更 |
|------|------|
| `common/config/ApiProtocol.java` | 新增 |
| `AIProviderConfig.java` | 字段 + copy/equals + 请求侧辅助 |
| `AICompatibleProvider.java` | 按协议构建 body / 选端点 |
| `BlockingRequestExecutor.java` | 解析分流；probe URL |
| `StreamRequestExecutor.java` + parser 包 | Responses SSE 事件解析 |
| `ThinkingCapabilityProbe.java` / Controller | Responses 下跳过三探针 |
| `AvailableProviderSettingsDialog.java` + i18n | 协议下拉 |
| `AzureOpenAIProvider.java` 等 | URL 后缀 |
| `docs/` + `pluginChanges.html` + 用户手册 | 文档 |

预计 **> 8 个文件**，属中等功能；编码前需你确认本方案。

---

## 7. 测试计划

| 场景 | 预期 |
|------|------|
| 默认配置测连 OpenAI Chat | 行为与现网一致 |
| OpenAI 切换 Responses 测连 | 成功；摘要无 `enable_thinking` 乱码探针 |
| Responses + Think 开启流式 | 可见 reasoning summary（若模型返回）+ 正文 delta |
| Responses + Think 关闭 | 不带 `reasoning`（或 effort none），正文正常 |
| Custom 误选 Responses 且网关无端点 | 失败弹窗含 HTTP 状态 + 提示改回 Chat |
| 通义 / 硅基等未展示协议或保持 Chat | 不受影响 |
| Azure Responses（若首期纳入） | 测连 + 简单生成通过 |

---

## 8. 分期

| 阶段 | 内容 |
|------|------|
| **Phase 1（本期）** | 选项 A：协议开关 + 请求/阻塞/流式双栈 + `store:false` + Think→reasoning 映射 + 跳过 enable_thinking 探针 + 文档 |
| Phase 2 | 测连自动探测协议；usage 字段精细化；OpenRouter 等更多白名单 |
| Phase 3 | tools / previous_response_id（仅 Chat 面板若有多轮需求时再做） |

---

## 9. 待你确认的问题

1. **范围**：Phase 1 是否先只做 `OPENAI` + `CUSTOM`，Azure 放 Phase 1.1？
2. **Think 映射**：勾选 Think 时 `reasoning.effort` 固定 `medium`，还是再加一个简单档位（low/medium/high）？
3. **input 形态**：首期 `instructions` + `input` 字符串，是否足够（推荐足够）？

确认后按 Phase 1 编码；方案文档路径：

`intelli-ai-engine/docs/OpenAI-Responses-API兼容实现方案.md`
