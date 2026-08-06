# Anthropic 思考参数策略补齐方案

## 1. 背景

Phase 1/2 已覆盖主要 **OpenAI 兼容 ** 思考策略；Anthropic 路径仅 `DEEPSEEK_ANTHROPIC` 有实现，其余均为 `NoOpThinkingStrategy`（不写任何思考字段）。

待补齐的 Anthropic 入口：

| ProviderType                        | 现状   | 官方 / 文档字段形态                                                                                                |
|-------------------------------------|------|------------------------------------------------------------------------------------------------------------|
| `ANTHROPIC`                         | NoOp | `thinking.type=enabled` + ** 必填 ** `budget_tokens`；关 =`disabled`；新模型另有 `adaptive` + `output_config.effort` |
| `DEEPSEEK_ANTHROPIC`                | ✅ 已有 | `thinking.type` + `output_config.effort`（无 budget）                                                         |
| `MOONSHOT_ANTHROPIC`                | NoOp | 与 OpenAI 口同形：`thinking.type` / K3 `reasoning_effort`                                                       |
| `DOUBAO_ANTHROPIC`                  | NoOp | 与 OpenAI 口同形：`thinking.type` + `reasoning_effort`                                                          |
| `ZHIPU_ANTHROPIC` / `ZAI_ANTHROPIC` | NoOp | 与 OpenAI 口同形：`thinking.type` + `reasoning_effort`                                                          |
| `HUNYUAN_ANTHROPIC`                 | NoOp | `thinking.type` + 可选 `budget_tokens`                                                                       |
| `MODELSCOPE_ANTHROPIC`              | NoOp | 混合上游模型，字段不统一                                                                                               |

约束（延续既有约定）：

- 配置层只表达意图：`enableThinking` + `thinkingEffort`
- Custom ** 不做 ** 模型名启发式
- 不向用户展示内部策略名
- 先方案、后编码

---

## 2. 方案选项

### 选项 A（推荐）：官方 budget + 厂商复用 OpenAI 策略

1. 新增 `AnthropicBudgetThinkingStrategy`（官方 Anthropic + 混元 Anthropic）
    - 关：`{"thinking":{"type":"disabled"}}`
    - 开：`{"thinking":{"type":"enabled","budget_tokens":N}}`
    - `N` 由强度映射：LOW=1024，HIGH=4096，MAX=8192（AUTO→HIGH）
    - 若 body 已有 `max_tokens`，将 `N` clamp 为 `[1024, max_tokens - 1]`
    - UI：`TOGGLE_AND_EFFORT`；无网络探针（合成 OPTIONAL 摘要）
2. Registry 将厂商 Anthropic ** 直接指向已有 OpenAI 策略 **（请求体字段相同，协议路径不同）：
    - `MOONSHOT_ANTHROPIC` → `MoonshotThinkingStrategy`
    - `DOUBAO_ANTHROPIC` → `DoubaoThinkingStrategy`
    - `ZHIPU_ANTHROPIC` / `ZAI_ANTHROPIC` → `ZhipuThinkingStrategy`
3. `MODELSCOPE_ANTHROPIC` → 新增轻量 `ThinkingTypeToggleStrategy`（仅 `thinking.type`，无 budget / effort），降低混合模型误伤
4. `DEEPSEEK_ANTHROPIC` 不变

| 项   | 评估                                         |
|-----|--------------------------------------------|
| 工作量 | 小～中（1～2 个新类 + Registry + 文档）               |
| 风险  | 低：厂商复用已验证 OpenAI 字段；官方用文档主路径 budget_tokens |
| 回滚  | 仅改 Registry / 策略类，配置字段不变                   |

** 不采用官方 `adaptive` 的原因 **：插件模型列表含 Claude 3.5/3.7/4 等，`adaptive` 仅新模型支持，易 400。后续可再按模型代际分支。

### 选项 B：官方改用 `adaptive` + `output_config.effort`

- 更贴近 Anthropic 新文档（Opus 4.6+ / Opus 5）
- 旧模型与部分兼容网关易失败
- ** 不推荐作为首期 **

### 选项 C：全部 Anthropic 入口统一成 DeepSeek 形（`thinking.type` + `output_config.effort`）

- 与官方必填 `budget_tokens` 冲突
- ** 否决 **

---

## 3. 推荐落地明细（选项 A）

### 3.1 Registry 目标映射

```text
ANTHROPIC              → AnthropicBudgetThinkingStrategy
HUNYUAN_ANTHROPIC      → AnthropicBudgetThinkingStrategy
DEEPSEEK_ANTHROPIC     → DeepSeekAnthropicThinkingStrategy（不变）
MOONSHOT_ANTHROPIC     → MoonshotThinkingStrategy
DOUBAO_ANTHROPIC       → DoubaoThinkingStrategy
ZHIPU_ANTHROPIC        → ZhipuThinkingStrategy
ZAI_ANTHROPIC          → ZhipuThinkingStrategy
MODELSCOPE_ANTHROPIC   → ThinkingTypeToggleStrategy
其他 isAnthropicCompatible → NoOp（兜底）
```

### 3.2 涉及文件

| 动作 | 路径                                                             |
|----|----------------------------------------------------------------|
| 新增 | `.../ai/thinking/AnthropicBudgetThinkingStrategy.java`         |
| 新增 | `.../ai/thinking/ThinkingTypeToggleStrategy.java`              |
| 改  | `.../ai/thinking/ThinkingParamStrategyRegistry.java`           |
| 改  | `AnthropicLikeProvider` 注释（说明已非仅 DeepSeek）                     |
| 改  | `docs/ 思考参数策略接口设计方案.md`（Phase 3 Anthropic 勾掉）                  |
| 改  | `includes/pluginChanges.html`、`site/docs/ 用户手册.md`、i18n（若有新文案） |

### 3.3 明确不做

- 官方 `adaptive` / `display`（summarized/omitted）
- 增强 Anthropic 响应 thinking block 的业务展示（流式已有 `delta.thinking` 解析则不动）
- 混元 OpenAI 口从 `EnableThinkingStrategy` 迁出（可另开一轮）
- Responses API
- 向用户展示策略内部 id

### 3.4 验证

1. `./compile.sh intelli-ai-engine`
2. 手工：Anthropic / DeepSeek Anthropic / 任一厂商 Anthropic 入口，开关 Think 后抓请求体字段是否符合上表
3. 关闭 Think：必须显式 `disabled`（budget 策略 / thinking.type 系）

### 3.5 攻击面检查

| 角度   | 结论                                                             |
|------|----------------------------------------------------------------|
| 依赖失效 | 某厂商 Anthropic 口拒收字段 → 用户关 Think 或换 OpenAI 口；Registry 可单点回 NoOp |
| 规模   | 无                                                              |
| 回滚   | 配置意图字段不变，回滚策略即可                                                |
| 前提崩塌 | 若用户只用仅支持 `adaptive` 的 Claude → 可能 400；首期选 budget，后续再加模型代际分支    |

---

## 4. 确认与状态

已确认按 ** 选项 A** 实施并完成编码（2026-08-06）。
