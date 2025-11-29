# ModelScope 服务商接入实现方案

## 📋 背景与目标

- `IntelliAI Engine` 目前支持 QianWen、SiliconFlow、Ollama、LM Studio、自定义等服务商。
- 需要新增 **ModelScope**，并在设置页可选择、配置、刷新模型，同时兼容现有调用链。
- ModelScope 提供的模型列表接口与其他兼容接口不同：需要发送 `PUT https://modelscope.cn/api/v1/dolphin/models`，并在响应中从 `
  Data.Model.Models[].BackendSupport.model_id` 位置提取可用模型。因此必须重写获取模型列表的实现。

## ✅ 功能范围

- 在 `AIProviderType` 中加入 ModelScope 类型，配置默认 BaseURL、默认模型、是否需要 API Key、是否可编辑 URL、预置模型列表占位等。
- `AIServiceFactory` 能根据配置创建 `ModelScopeProvider`。
- 新增 `ModelScopeProvider`，继承 `AICompatibleProvider`，覆盖必要方法（至少 `getAvailableModels`，如有需要可调整 `sendRequest` 相关参数）。
- 在 `AIProviderConfigPanel` 中正确展示新服务商：包含图标/标题显示、默认 URL 带入、刷新模型调用 ModelScope 定制逻辑。
- 国际化资源新增 ModelScope 文案（服务商名称、错误提示等）。
- 方案仅涉及 Engine 模块（无需修改其他模块）。

## 🔧 实现方案

### 1. Provider 类型扩展

- `AIProviderType`
    - 新增枚举项 `MODELSCOPE`：
        - `providerId = "modelscope"`
        - `displayName = "ModelScope"`
        - `defaultBaseUrl = "https://modelscope.cn/api/v1"`
        - `defaultModel = "Qwen/Qwen3-8B"`（占位，可根据后续确认调整）
        - `requiresApiKey = true`
        - `baseUrlEditable = true`
        - `supportedModels = List.of("Qwen/Qwen3-8B")`（仅用于下拉框初始值，刷新后覆盖）

### 2. Provider 工厂

- `AIServiceFactory#createProvider` switch-case 中补充 `MODELSCOPE -> new ModelScopeProvider(...)`。
- 确保 `ModelScopeProvider` 的构造函数签名与其他 Provider 保持一致（config + modelParameters + runtimeSettings + consoleLogger）。

### 3. ModelScopeProvider 设计

- 新建 `ModelScopeProvider extends AICompatibleProvider`。
- 默认聊天/推理接口若可沿用兼容 `/chat/completions` 规范，则直接继承父类逻辑；若后续需求发现不同，再在此类中逐步调整。
- **重写 `getAvailableModels(String apiKey)`**：
    - 校验 API Key：ModelScope 需要 `Bearer token`。若为空，则直接返回空列表并写日志。
    - 构建请求：
      ```json
      {
        "PageSize": 100,
        "PageNumber": 1,
        "SortBy": "Default",
        "Target": "",
        "Criterion": [{
          "category": "tasks",
          "predicate": "contains",
          "values": ["text-generation"],
          "sub_values": []
        }],
        "SingleCriterion": [{
          "category": "inference_type",
          "DateType": "int",
          "predicate": "equal",
          "IntValue": 1
        }]
      }
      ```
    - 通过 `HttpRequests.request(url).method("PUT")` 或 `HttpRequests.put`（IntelliJ API）发送，设置 `Content-Type: application/json`、
      `Authorization: Bearer ${apiKey}`、复用运行时超时时间。
    - 解析响应：
        - JSON 路径：`Data -> Model -> Models` 数组
        - 每个元素取 `BackendSupport.model_id`
        - 过滤空值、去重、排序（忽略大小写）
    - 日志：沿用 `consoleLogger` 的 verbose 输出，打印请求/响应状态与模型数量。
    - 异常处理：捕获 `IOException`、`JsonParseException`，打印日志并返回空列表，保持 UI 侧提示逻辑。

### 4. UI & 设置联动

- `AIProviderConfigPanel`
    - Provider 下拉框：使用 `AIProviderType.getAllDisplayNames()` 已自动包含 ModelScope，无需额外 UI 调整。
    - 当选择 ModelScope 时，`baseUrlField` 应填充默认值，API Key 输入框保持必填逻辑（`requiresApiKey()` 已满足）。
    - 刷新按钮逻辑无需额外判断，因 `AIServiceFactory` 会生成正确 Provider，ModelScope 的 `getAvailableModels` 会被调用。
    - 如需显示独特提示（例如“需使用 ModelScope AccessToken”），可在 `AICommonBundle` 新增提示文本并在 panel 中根据 providerType 切换 `JBLabel`。

### 5. 国际化

- `messages/AICommonBundle_en_US.properties` & `_zh_CN.properties`
    - 新增：
        - `settings.provider.modelscope=ModelScope`
        - 可选：`settings.error.modelscope.missing.token`
        - 如增加提示，需双语同步。

### 6. 变更同步

- `includes/pluginChanges.html`：记录“新增 ModelScope 服务商支持，使用 model_id 列表”等（编码完成后进行）。
- `用户手册.md`：新增“ModelScope 配置”小节，说明 API Key、刷新模型等操作。

## 📁 涉及文件

- `intelli-ai-engine/src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/AIProviderType.java`
- `.../common/ai/AIServiceFactory.java`
- `.../common/ai/provider/ModelScopeProvider.java`（新文件）
- `.../common/ui/AIProviderConfigPanel.java`
- `.../resources/messages/AICommonBundle_*.properties`
- 文档与更新记录（编码阶段处理）

## 🧪 测试计划

1. **Provider 切换**：设置页选择 ModelScope，确认默认 BaseURL/模型回填正常，API Key 输入为必填。
2. **刷新模型成功**：输入有效 Token，点击“刷新模型”，模型列表更新且提示成功，`modelComboBox` 使用 `model_id` 赋值。
3. **刷新失败路径**：空 Token、网络错误、API 错误码，按钮状态与提示一致，日志记录错误信息。
4. **生成调用**：配置 ModelScope 模型后执行一次 JavaDoc 生成，确保请求走 ModelScopeProvider 并成功返回内容（可使用 mock Token）。
5. **回退到其他 Provider**：切换回 QianWen，确认既有功能无回归。

## ⚠️ 风险与缓解

- **接口变化**：ModelScope API 若后续调整参数/响应，需要在 `ModelScopeProvider` 中集中维护；通过封装请求/解析方法降低影响面。
- **Token 管理**：沿用 credentialId + CredentialManager，确保不同服务商凭证互不覆盖。
- **网络超时**：使用运行时超时时间，若 ModelScope 响应较慢，可指导用户在设置中调整秒数。

请确认以上方案，确认后将按照该方案进入编码阶段。确认后也会同步更新 TODO 列表，进入实现步骤。

