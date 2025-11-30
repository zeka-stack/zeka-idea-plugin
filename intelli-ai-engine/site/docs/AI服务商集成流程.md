# AI 服务商集成流程文档

## 📋 概述

本文档描述了如何在 IntelliAI Engine 中集成新的 AI 服务商。流程包括添加服务商类型、创建 Provider 实现类、配置图标资源、注册到工厂等步骤。

## 🎯 前置准备

在开始集成前，请收集以下信息：

- **服务商名称**：显示名称（如：IFlow、ModelScope）
- **服务商 ID**：唯一标识符（如：iflow、modelscope）
- **API 基础 URL**：服务商的 API 端点（如：`https://apis.iflow.cn/v1`）
- **默认模型**：默认使用的模型名称（如：`kimi-k2-0905`）
- **是否需要 API Key**：是否需要用户配置 API Key
- **Base URL 是否可编辑**：是否允许用户修改基础 URL
- **预置模型列表**：默认支持的模型列表（用于下拉框初始值）
- **模型列表接口**：获取可用模型的接口地址和请求方式（GET/POST/PUT）
- **模型列表响应格式**：接口返回的 JSON 结构，特别是模型名称的提取路径
- **图标资源**：16x16 和 64x64 的 SVG 图标文件

## 📁 涉及文件清单

集成新服务商需要修改以下文件：

### 必须修改的文件

1. **AIProviderType.java** - 添加新的服务商枚举类型
2. **AIServiceFactory.java** - 注册新的 Provider 到工厂
3. **AICommonIcons.java** - 添加图标资源引用
4. **新建 Provider 类** - 创建 `{ServiceName}Provider.java`

### 可选修改的文件

5. **AICommonBundle_*.properties** - 国际化资源（如需要特殊提示文案）
6. **includes/pluginChanges.html** - 更新记录
7. **用户手册.md** - 添加使用说明

## 🔧 详细步骤

### 步骤 1：在 AIProviderType 枚举中添加新类型

**文件路径：** `src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/AIProviderType.java`

**操作：**

在枚举中添加新的服务商类型，格式如下：

```java
/**
 * {服务商名称} 模型配置
 * <p>
 * {服务商描述信息}
 */
{服务商ID大写}(
    "{服务商ID小写}",           // providerId
    "{服务商显示名称}",         // displayName
    "{API基础URL}",             // defaultBaseUrl
    "{默认模型名称}",           // defaultModel
    true/false,                 // requiresApiKey
    true/false,                 // baseUrlEditable
    List.of("模型1", "模型2")   // supportedModels
),
```

**示例（IFlow）：**

```java
IFLOW(
    "iflow",
    "IFlow",
    "https://apis.iflow.cn/v1",
    "kimi-k2-0905",
    true,
    true,
    List.of("kimi-k2-0905", "qwen3-coder-plus", "glm-4.6", "deepseek-r1")
),
```

### 步骤 2：创建 Provider 实现类

**文件路径：** `src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/provider/{ServiceName}Provider.java`

**基本结构：**

```java
package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIConsoleLogger;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * {服务商名称} 提供者类
 * <p>
 * 该类继承自 AICompatibleProvider, 用于与 {服务商名称} AI 模型进行交互
 */
public class {ServiceName}Provider extends AICompatibleProvider {

    /**
     * 构造函数
     */
    public {ServiceName}Provider(@NotNull AIProviderConfig config,
                                 @NotNull AIModelParameters modelParameters,
                                 @NotNull AIRuntimeSettings runtimeSettings,
                                 @Nullable AIConsoleLogger consoleLogger) {
        super(config, modelParameters, runtimeSettings, consoleLogger);
    }
}
```

**是否需要重写方法？**

- **标准 OpenAI 兼容接口**：如果服务商使用标准的 OpenAI 兼容格式（`/chat/completions`），通常**不需要重写**任何方法，直接继承即可。
- **自定义接口**：如果服务商的接口格式不同，需要重写相应方法。

#### 需要重写 `getAvailableModels` 的情况

如果服务商的模型列表接口**不是** `GET {baseUrl}/models`，则需要重写该方法。

**重写示例（IFlow）：**

```java
@Override
@NotNull
public List<String> getAvailableModels(@Nullable String apiKey) {
    // 1. 日志输出
    if (consoleLogger != null && runtimeSettings.verboseLogging) {
        consoleLogger.printWithTimestamp("=== {服务商名称} 获取模型列表 ===");
        consoleLogger.print("接口地址: " + MODELS_LIST_URL);
    }

    try {
        // 2. 验证 API Key（如果需要）
        if (requiresApiKey() && (apiKey == null || apiKey.trim().isEmpty())) {
            // 返回默认模型列表或空列表
            return new ArrayList<>(AIProviderType.{SERVICE_ID}.getSupportedModels());
        }

        // 3. 构建请求体（根据接口要求）
        String requestBody = "{}"; // 或构建 JSON 对象
        
        // 4. 发送请求
        String responseBody = HttpRequests.post(MODELS_LIST_URL, "application/json")
            .tuner(connection -> {
                HttpURLConnection conn = (HttpURLConnection) connection;
                conn.setConnectTimeout(runtimeSettings.getTimeoutInMillis());
                conn.setReadTimeout(runtimeSettings.getTimeoutInMillis() * 2);
                conn.setRequestProperty("Content-Type", "application/json");
                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                }
                conn.setFixedLengthStreamingMode(contentLength);
                conn.setRequestProperty("Content-Length", String.valueOf(contentLength));
            })
            .connect(request -> {
                request.write(requestBody);
                return request.readString();
            });

        // 5. 解析响应
        if (!responseBody.trim().isEmpty()) {
            List<String> models = parseModelsResponse(responseBody);
            return models.isEmpty() 
                ? new ArrayList<>(AIProviderType.{SERVICE_ID}.getSupportedModels())
                : models;
        }

        return new ArrayList<>(AIProviderType.{SERVICE_ID}.getSupportedModels());
    } catch (IOException e) {
        LOG.info("{服务商名称} 获取模型列表网络错误", e);
        return new ArrayList<>(AIProviderType.{SERVICE_ID}.getSupportedModels());
    } catch (Exception e) {
        LOG.info("{服务商名称} 获取模型列表失败", e);
        return new ArrayList<>(AIProviderType.{SERVICE_ID}.getSupportedModels());
    }
}
```

#### 需要重写 `parseModelsResponse` 的情况

如果服务商的响应格式**不是**标准的 `{ data: [{ id: "model-name" }] }` 格式，则需要重写该方法。

**重写示例（IFlow - 从按厂商分类的 data 中提取）：**

```java
@Override
protected List<String> parseModelsResponse(String responseBody) {
    Set<String> models = new LinkedHashSet<>();
    try {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        
        // 检查响应是否成功
        if (json.has("success") && !json.get("success").getAsBoolean()) {
            String errorMessage = json.has("message") 
                ? json.get("message").getAsString() 
                : "未知错误";
            LOG.info("{服务商名称} API 返回失败: " + errorMessage);
            return new ArrayList<>();
        }

        // 获取 data 对象
        if (!json.has("data") || !json.get("data").isJsonObject()) {
            LOG.info("{服务商名称} 响应中没有 data 对象");
            return new ArrayList<>();
        }

        JsonObject dataObj = json.getAsJsonObject("data");
        
        // 遍历 data 对象中的所有厂商分类
        for (String vendorKey : dataObj.keySet()) {
            JsonElement vendorElement = dataObj.get(vendorKey);
            if (vendorElement.isJsonArray()) {
                JsonArray vendorArray = vendorElement.getAsJsonArray();
                // 遍历该厂商下的所有模型
                for (JsonElement modelElement : vendorArray) {
                    if (modelElement.isJsonObject()) {
                        JsonObject modelObj = modelElement.getAsJsonObject();
                        // 提取 modelName 字段
                        if (modelObj.has("modelName")) {
                            String modelName = modelObj.get("modelName").getAsString();
                            if (modelName != null && !modelName.trim().isEmpty()) {
                                models.add(modelName.trim());
                            }
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        LOG.info("{服务商名称} 解析模型响应失败", e);
    }
    return new ArrayList<>(models);
}
```

**重写示例（Ollama - 从 models 数组中提取）：**

```java
@Override
protected List<String> parseModelsResponse(String responseBody) {
    List<String> models = new ArrayList<>();
    try {
        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
        if (json.has("models") && json.get("models").isJsonArray()) {
            JsonArray modelsArray = json.getAsJsonArray("models");
            for (JsonElement element : modelsArray) {
                JsonObject modelObj = element.getAsJsonObject();
                String modelName = null;
                if (modelObj.has("name")) {
                    modelName = modelObj.get("name").getAsString();
                } else if (modelObj.has("model")) {
                    modelName = modelObj.get("model").getAsString();
                }
                if (modelName != null && !modelName.trim().isEmpty()) {
                    models.add(modelName.trim());
                }
            }
        } else {
            models = super.parseModelsResponse(responseBody);
        }
    } catch (Exception ignored) {
        return new ArrayList<>(AIProviderType.{SERVICE_ID}.getSupportedModels());
    }
    if (models.isEmpty()) {
        return new ArrayList<>(AIProviderType.{SERVICE_ID}.getSupportedModels());
    }
    return models;
}
```

### 步骤 3：在 AIServiceFactory 中注册 Provider

**文件路径：** `src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/ai/AIServiceFactory.java`

**操作：**

1. **添加 import：**

```java
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.{ServiceName}Provider;
```

2. **在 switch 语句中添加 case：**

```java
return switch (providerType) {
    case CUSTOM -> new CustomProvider(config, modelParameters, runtimeSettings, consoleLogger);
    case QIANWEN -> new QianWenProvider(config, modelParameters, runtimeSettings, consoleLogger);
    // ... 其他服务商
    case {SERVICE_ID_UPPER} -> new {ServiceName}Provider(config, modelParameters, runtimeSettings, consoleLogger);
};
```

**示例：**

```java
case IFLOW -> new IflowProvider(config, modelParameters, runtimeSettings, consoleLogger);
```

### 步骤 4：添加图标资源

#### 4.1 准备图标文件

**文件路径：** `src/main/resources/icons/`

创建两个图标文件：

- `{service_id}_16.svg` - 16x16 尺寸，用于下拉列表、状态栏
- `{service_id}_64.svg` - 64x64 尺寸，用于对话框、错误提示框

**注意事项：**

- 图标必须是 SVG 格式
- 16x16 图标建议保留 2px 边距，图标主体占用 12px 区域
- 64x64 图标建议保留 8px 边距，图标主体占用 48px 区域
- 图标应该在不同主题（浅色/深色）下都清晰可见

#### 4.2 在 AICommonIcons 中添加图标引用

**文件路径：** `src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/icons/AICommonIcons.java`

**操作：**

1. **添加 16x16 图标常量：**

```java
/**
 * {服务商名称} 提供商图标 (16x16)
 * <p>
 * 用于：设置页面 AI 供应商下拉列表、状态栏
 */
public static final Icon PROVIDER_{SERVICE_ID_UPPER} = load("/icons/{service_id}_16.svg");
```

2. **添加 64x64 图标常量：**

```java
/**
 * {服务商名称} 提供商图标 (64x64)
 * <p>
 * 用于：错误提示框、对话框
 */
public static final Icon PROVIDER_{SERVICE_ID_UPPER}_64 = load("/icons/{service_id}_64.svg");
```

3. **在 `getProviderIcon` 方法中添加 case：**

```java
Icon icon = switch (providerType) {
    // ... 其他服务商
    case {SERVICE_ID_UPPER} -> PROVIDER_{SERVICE_ID_UPPER};
};
```

4. **在 `getProviderIcon64` 方法中添加 case：**

```java
return switch (providerType) {
    // ... 其他服务商
    case {SERVICE_ID_UPPER} -> PROVIDER_{SERVICE_ID_UPPER}_64;
};
```

### 步骤 5：国际化资源（可选）

**文件路径：** `src/main/resources/messages/AICommonBundle_zh_CN.properties` 和 `AICommonBundle.properties`

如果需要添加特殊的提示文案，可以在这里添加：

```properties
# 中文
settings.provider.{service_id}={服务商显示名称}
settings.error.{service_id}.missing.token=缺少 {服务商名称} API Key

# 英文
settings.provider.{service_id}={Service Display Name}
settings.error.{service_id}.missing.token=Missing {Service Name} API Key
```

### 步骤 6：更新文档和更新记录（可选）

#### 6.1 更新插件更新记录

**文件路径：** `includes/pluginChanges.html`

在文件顶部添加新版本记录：

```html
<h3>版本号</h3>
<ul>
    <li>新增 {服务商名称} 服务商支持</li>
</ul>
<h3>版本号</h3>
<ul>
    <li>Added {Service Name} provider support</li>
</ul>
```

#### 6.2 更新用户手册

**文件路径：** `site/docs/用户手册.md`

在"AI 提供商配置"章节添加新服务商的说明。

## ✅ 检查清单

完成集成后，请确认以下事项：

- [ ] `AIProviderType` 枚举中已添加新类型
- [ ] 创建了 `{ServiceName}Provider` 类并正确继承 `AICompatibleProvider`
- [ ] 如需自定义模型列表获取，已重写 `getAvailableModels` 方法
- [ ] 如需自定义响应解析，已重写 `parseModelsResponse` 方法
- [ ] `AIServiceFactory` 中已注册新 Provider
- [ ] 图标文件已创建（`{service_id}_16.svg` 和 `{service_id}_64.svg`）
- [ ] `AICommonIcons` 中已添加图标引用和 switch case
- [ ] 代码已通过 lint 检查
- [ ] 已测试 Provider 创建和基本功能
- [ ] 已测试模型列表获取功能
- [ ] 已测试生成内容功能（如适用）

## 🧪 测试建议

### 1. Provider 创建测试

在设置页面选择新服务商，确认：

- 服务商名称和图标正确显示
- 默认 Base URL 正确填充
- API Key 输入框状态正确（必填/可选）
- 默认模型正确填充

### 2. 模型列表获取测试

点击"刷新模型"按钮，测试：

- **成功场景**：输入有效 API Key，确认模型列表正确获取并显示
- **失败场景**：
    - 空 API Key（如需要）
    - 无效 API Key
    - 网络错误
    - API 返回错误

### 3. 内容生成测试

使用新服务商生成内容，确认：

- 请求正确发送到服务商 API
- 响应正确解析
- 内容正确返回
- Token 使用情况正确记录（如有）

### 4. 兼容性测试

确认：

- 切换到其他服务商正常工作
- 不影响现有功能
- 配置正确保存和加载

## 📝 常见问题

### Q1: 服务商使用非标准的 OpenAI 格式怎么办？

A: 如果服务商的 API 格式与 OpenAI 完全不同，需要重写更多方法：

- `generateContent` - 重写请求构建逻辑
- `sendRequest` - 重写请求发送逻辑
- `parseResponse` - 重写响应解析逻辑

建议先查看 `AICompatibleProvider` 的源码，了解哪些方法可以重写。

### Q2: 模型列表接口需要认证但返回空列表怎么办？

A: 在 `getAvailableModels` 方法中，如果 API Key 为空或无效，应返回默认模型列表，而不是空列表，这样用户至少可以选择预设的模型。

### Q3: 响应格式复杂，如何解析？

A: 使用 Gson 库解析 JSON：

```java
JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
JsonArray array = json.getAsJsonArray("path.to.array");
for (JsonElement element : array) {
    JsonObject obj = element.getAsJsonObject();
    String value = obj.get("field").getAsString();
}
```

### Q4: 图标在不同主题下显示不佳怎么办？

A: 检查 SVG 文件的颜色定义。建议：

- 使用相对颜色或主题感知的颜色
- 避免使用纯黑色或纯白色
- 在不同主题下预览图标效果

### Q5: 如何调试模型列表获取问题？

A: 启用详细日志：

1. 在设置中启用"详细日志"
2. 查看控制台输出
3. 检查网络请求和响应

或者在代码中添加日志：

```java
if (consoleLogger != null && runtimeSettings.verboseLogging) {
    consoleLogger.print("请求 URL: " + url);
    consoleLogger.print("响应内容: " + responseBody);
}
```

## 🔗 参考示例

可以参考以下现有实现的代码：

- **标准实现（QianWen/SiliconFlow）**：`QianWenProvider.java`、`SiliconFlowProvider.java`
- **自定义模型列表（Ollama）**：`OllamaProvider.java`
- **复杂模型列表（ModelScope）**：`ModelScopeProvider.java`
- **自定义响应解析（IFlow）**：`IflowProvider.java`

## 📚 相关文档

- [ModelScope服务商接入方案](./ModelScope服务商接入方案.md) - ModelScope 集成的详细案例
- [用户手册](./用户手册.md) - 用户使用指南
- [AI扩展插件能力分析与设计方案](./AI扩展插件能力分析与设计方案.md) - 架构设计文档

