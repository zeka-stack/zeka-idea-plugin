# AI Common 作为 IntelliJ 插件扩展的能力分析与设计方案

## 1. 当前状态分析

### 1.1 现有架构

`ai-common` 目前是一个 **Java Library** 项目，通过 Maven 发布，作为库依赖被其他插件使用。

**当前结构**：

- ✅ 完整的 AI 服务提供商实现（支持多种 AI 服务）
- ✅ 统一的配置管理（`AIProviderSettings`、`AIProviderConfig` 等）
- ✅ 安全的 API Key 管理（`AICredentialManager`）
- ✅ 可复用的 UI 组件（`AIProviderConfigPanel`）
- ✅ 状态栏组件（`AIProviderStatusBarWidget`）
- ✅ 完整的国际化资源（图标、文本等）
- ❌ **缺少 `plugin.xml`，无法作为 IntelliJ 插件发布**
- ❌ **缺少全局配置服务，每个插件需要自己管理配置**
- ❌ **API 使用复杂，需要创建多个配置对象**

### 1.2 当前使用方式

外部开发者使用 `ai-common` 需要：

```java
// 1. 创建多个配置对象
AIProviderConfig config = new AIProviderConfig(AIProviderType.QIANWEN);
config.modelName = "qwen3-8b";
config.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

AIModelParameters modelParams = new AIModelParameters();
modelParams.temperature = 0.7;
modelParams.maxTokens = 2000;

AIRuntimeSettings runtimeSettings = new AIRuntimeSettings();
runtimeSettings.maxRetries = 3;
runtimeSettings.timeout = 30000;

// 2. 创建凭证管理器
AICredentialManager credentialManager = new AICredentialManager("MyPlugin", "MY_PLUGIN_API_KEY_");
String apiKey = credentialManager.getApiKey(config.credentialId);

// 3. 创建控制台日志器（可选）
AIConsoleLogger consoleLogger = new MyConsoleLogger();

// 4. 创建服务提供者
AIServiceProvider provider = AIServiceFactory.createProvider(
    config,
    modelParams,
    runtimeSettings,
    consoleLogger,
    false
);

// 5. 构建请求
AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

// 6. 生成内容
String result = provider.generateContent(request, apiKey, null);
```

**问题**：

- 步骤繁琐，需要创建多个对象
- 配置复杂，外部开发者需要了解所有配置细节
- 没有统一的入口点

## 2. 作为插件扩展的能力分析

### 2.1 可行性分析

✅ **完全可行**，`ai-common` 具备作为独立插件发布的所有条件：

1. **功能完整性**：
    - ✅ 支持多种 AI 服务提供商
    - ✅ 完整的配置管理
    - ✅ 安全的凭证管理
    - ✅ 错误处理和重试机制

2. **架构设计**：
    - ✅ 模块化设计，职责清晰
    - ✅ 接口抽象良好
    - ✅ 可扩展性强

3. **缺失部分**：
    - ❌ 缺少 `plugin.xml` 配置文件
    - ❌ 缺少简洁的对外 API
    - ❌ 缺少统一的入口服务

### 2.2 IntelliJ 插件依赖机制

IntelliJ 平台支持插件之间的依赖关系：

```xml
<!-- 在依赖插件的 plugin.xml 中 -->
<depends>dev.dong4j.zeka.stack.idea.plugin.common.ai</depends>
```

依赖插件可以：

- 访问被依赖插件的公开 API
- 使用被依赖插件提供的服务
- 扩展被依赖插件的功能点

## 3. 设计方案

### 3.1 架构设计

#### 3.1.1 创建 plugin.xml

```xml
<idea-plugin>
    <id>dev.dong4j.zeka.stack.idea.plugin.common.ai</id>
    <name>AI Common</name>
    <version>1.0.0</version>
    <vendor>dong4j</vendor>
    
    <description>通用的 AI 服务插件，为其他 IntelliJ 插件提供 AI 能力</description>
    
    <idea-version since-build="223" until-build="252.*"/>
    
    <depends>com.intellij.modules.platform</depends>
    
    <extensions defaultExtensionNs="com.intellij">
        <!-- 应用级服务：全局 AI 配置 -->
        <applicationService 
            serviceImplementation="dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings"/>
        
        <!-- 应用级服务：统一的 AI 服务入口 -->
        <applicationService 
            serviceImplementation="dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService"/>
        
        <!-- 设置页面：AI Common 的全局配置 -->
        <projectConfigurable
            parentId="tools"
            instance="dev.dong4j.zeka.stack.idea.plugin.common.settings.AICommonSettingsConfigurable"
            id="dev.dong4j.zeka.stack.idea.plugin.common.settings.AICommonSettingsConfigurable"
            displayName="AI Common"/>
        
        <!-- 状态栏组件 -->
        <statusBarWidgetFactory 
            implementation="dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarWidgetFactory"/>
    </extensions>
</idea-plugin>
```

**关键点**：

- `AIProviderSettings` 作为**应用级服务**，维护**可用供应商列表**（所有插件共享）
- 每个插件维护自己的**默认供应商选择**（从全局可用列表中选取）
- `AIService` 作为**应用级服务**，提供统一的 AI 能力
- 提供**全局设置页面**，用户可以在一个地方管理所有可用供应商
- 其他插件依赖后，**自动获得**可用供应商列表，但各自维护默认供应商选择

#### 3.1.2 配置隔离设计

**设计理念**：全局共享可用供应商列表，每个插件维护自己的默认供应商选择。

**配置层次**：

1. **全局配置（应用级服务 - AIProviderSettings）**：
    - ✅ 维护**可用供应商列表**（`availableProviders`）- 所有插件共享
    - ✅ 每个供应商的详细配置（baseUrl、模型列表等）- 所有插件共享
    - ❌ **不维护**"默认供应商"（避免插件间相互影响）

2. **插件配置（项目级服务 - 每个插件的 SettingsState）**：
    - ✅ 维护**当前插件使用的默认供应商类型**（`providerType`）
    - ✅ 从全局可用供应商列表中选取
    - ✅ 可以有自己的模型参数、运行时设置等（可选）

**优势**：

- ✅ **配置隔离**：每个插件的默认供应商互不影响
- ✅ **资源共享**：所有插件共享可用供应商列表，避免重复配置
- ✅ **灵活选择**：每个插件可以选择最适合的供应商

#### 3.1.3 设计简洁的 API

创建一个统一的 `AIService` 接口，**自动从插件配置中读取默认供应商**：

```java
package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;

/**
 * AI 服务统一入口
 * <p>
 * 为外部插件提供简洁的 AI 能力，自动从插件配置中读取默认供应商。
 * 
 * @author dong4j
 * @version 1.0.0
 */
public interface AIService {
    
    /**
     * 生成 AI 内容（最简单的方式 - 使用插件配置的默认供应商）
     * <p>
     * 自动从插件的配置中读取默认供应商，从全局配置中读取供应商详情。
     * 外部开发者只需要提供提示词和项目对象即可。
     *
     * @param project      项目对象（用于获取插件配置）
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param listener     响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                          @NotNull String systemPrompt,
                          @NotNull String userPrompt,
                          @Nullable AIResponseListener listener) throws AIServiceException;
    
    /**
     * 生成 AI 内容（使用插件配置的默认供应商）
     * <p>
     * 自动从插件的配置中读取默认供应商。
     *
     * @param project 项目对象（用于获取插件配置）
     * @param request AI 聊天请求
     * @param listener 响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                          @NotNull AIChatRequest request,
                          @Nullable AIResponseListener listener) throws AIServiceException;
    
    /**
     * 生成 AI 内容（指定供应商类型）
     * <p>
     * 临时使用指定的供应商类型，从全局配置中读取该供应商的详情。
     *
     * @param project      项目对象
     * @param providerType 供应商类型（从全局可用列表中选取）
     * @param request      AI 聊天请求
     * @param listener     响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                          @NotNull AIProviderType providerType,
                          @NotNull AIChatRequest request,
                          @Nullable AIResponseListener listener) throws AIServiceException;
    
    /**
     * 生成 AI 内容（自定义配置）
     * <p>
     * 支持临时覆盖配置，适合特殊场景。
     *
     * @param project 项目对象
     * @param request AI 聊天请求
     * @param config  临时配置（可选，为 null 时使用插件配置的默认供应商）
     * @param listener 响应监听器（可选，用于处理请求、响应和 token 使用量）
     * @return 生成的文本内容
     * @throws AIServiceException 当生成失败时抛出
     */
    @NotNull
    String generateContent(@NotNull Project project,
                          @NotNull AIChatRequest request,
                          @Nullable AIServiceConfig config,
                          @Nullable AIResponseListener listener) throws AIServiceException;
    
    /**
     * 获取全局配置（可用供应商列表）
     * <p>
     * 返回全局的 AI 配置，包含所有可用供应商。
     *
     * @return 全局 AI 配置
     */
    @NotNull
    AIProviderSettings getGlobalSettings();
    
    /**
     * 验证插件配置
     * <p>
     * 验证插件配置的默认供应商是否有效。
     *
     * @param project 项目对象（用于获取插件配置）
     * @return 验证结果
     */
    @NotNull
    ValidationResult validatePluginConfiguration(@NotNull Project project);
}
```

**关键设计理念**：

- **配置隔离**：每个插件维护自己的默认供应商，互不影响
- **自动读取**：从插件配置中自动读取默认供应商，从全局配置中读取供应商详情
- **灵活选择**：支持使用插件默认供应商，也支持临时指定供应商
- **回调支持**：通过 `AIResponseListener` 处理请求、响应和 token 使用量，实现自定义日志、统计等功能

#### 3.1.4 AIResponseListener 回调接口

`AIResponseListener` 接口允许外部开发者处理 AI 请求、响应和 token 使用量：

```java
package dev.dong4j.zeka.stack.idea.plugin.common.ai;

/**
 * AI 响应监听器接口
 * <p>
 * 用于处理 AI 服务请求、响应以及使用量统计的回调方法。
 */
public interface AIResponseListener {
    
    /**
     * 处理请求回调
     * <p>
     * 在发送请求到 AI 服务时调用。
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param requestBody  请求体内容
     * @param validation   是否进行参数校验
     */
    default void onRequest(String providerName, String modelName, 
                          String requestBody, boolean validation) {}
    
    /**
     * 处理响应回调
     * <p>
     * 在收到 AI 服务响应时调用。
     *
     * @param providerName 提供者名称
     * @param modelName    模型名称
     * @param responseBody 响应体内容
     * @param validation   验证结果标志
     */
    default void onResponse(String providerName, String modelName, 
                           String responseBody, boolean validation) {}
    
    /**
     * 处理 token 使用量回调
     * <p>
     * 在解析响应后调用，提供 token 使用量统计。
     *
     * @param providerName     提供者名称
     * @param modelName        模型名称
     * @param promptTokens     提示部分使用的令牌数
     * @param completionTokens 完成部分使用的令牌数
     * @param totalTokens      总共使用的令牌数
     */
    default void onUsage(String providerName, String modelName,
                        int promptTokens, int completionTokens, int totalTokens) {}
}
```

**使用场景**：

- ✅ **日志记录**：记录请求和响应内容
- ✅ **统计监控**：统计 token 使用量，用于计费或监控
- ✅ **错误处理**：实现统一的错误处理或重试机制
- ✅ **数据保存**：保存请求和响应数据用于分析

#### 3.1.5 配置对象简化

创建一个简化的配置对象，隐藏内部复杂性：

```java
package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

/**
 * AI 服务配置（简化版）
 * <p>
 * 为外部开发者提供简化的配置选项，内部自动转换为完整的配置对象。
 */
public class AIServiceConfig {
    /** AI 服务提供商类型 */
    @NotNull
    public AIProviderType providerType = AIProviderType.QIANWEN;
    
    /** 模型名称 */
    @NotNull
    public String modelName;
    
    /** 基础 URL（可选，使用默认值） */
    @Nullable
    public String baseUrl;
    
    /** API 密钥 */
    @NotNull
    public String apiKey;
    
    /** 温度参数（0.0-2.0，默认 0.7） */
    public double temperature = 0.7;
    
    /** 最大 Token 数（默认 2000） */
    public int maxTokens = 2000;
    
    /** 最大重试次数（默认 3） */
    public int maxRetries = 3;
    
    /** 请求超时时间（毫秒，默认 30000） */
    public int timeout = 30000;
    
    /**
     * 快速创建配置（使用默认值）
     */
    public static AIServiceConfig createDefault(@NotNull AIProviderType providerType,
                                                @NotNull String apiKey) {
        AIServiceConfig config = new AIServiceConfig();
        config.providerType = providerType;
        config.apiKey = apiKey;
        config.modelName = providerType.getDefaultModel();
        config.baseUrl = providerType.getDefaultBaseUrl();
        return config;
    }
}
```

### 3.2 实现方案

#### 3.2.1 实现 AIService 接口

```java
package dev.dong4j.zeka.stack.idea.plugin.common.ai.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceFactory;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AICredentialManager;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * AI 服务实现
 * <p>
 * 自动从插件配置中读取默认供应商，从全局配置中读取供应商详情。
 */
@Service(Service.Level.APPLICATION)
public final class AIServiceImpl implements AIService {
    
    private static final AICredentialManager GLOBAL_CREDENTIAL_MANAGER = 
        new AICredentialManager("AI Common", "AI_COMMON_");
    
    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                 @NotNull String systemPrompt,
                                 @NotNull String userPrompt,
                                 @Nullable AIResponseListener listener) throws AIServiceException {
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);
        return generateContent(project, request, null, listener);
    }
    
    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                 @NotNull AIChatRequest request,
                                 @Nullable AIResponseListener listener) throws AIServiceException {
        return generateContent(project, request, null, listener);
    }
    
    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                 @NotNull AIProviderType providerType,
                                 @NotNull AIChatRequest request,
                                 @Nullable AIResponseListener listener) throws AIServiceException {
        // 使用指定的供应商类型，从全局配置中读取详情
        return generateContentWithProvider(project, providerType, request, listener);
    }
    
    @Override
    @NotNull
    public String generateContent(@NotNull Project project,
                                 @NotNull AIChatRequest request,
                                 @Nullable AIServiceConfig config,
                                 @Nullable AIResponseListener listener) throws AIServiceException {
        if (config != null) {
            // 使用临时配置
            return generateContentWithConfig(project, request, config, listener);
        } else {
            // 从插件配置中读取默认供应商
            AIProviderSettings pluginSettings = getPluginSettings(project);
            return generateContentWithProvider(project, pluginSettings.providerType, request, listener);
        }
    }
    
    /**
     * 使用指定供应商生成内容
     */
    private String generateContentWithProvider(@NotNull Project project,
                                              @NotNull AIProviderType providerType,
                                              @NotNull AIChatRequest request,
                                              @Nullable AIResponseListener listener) throws AIServiceException {
        // 从全局配置中获取供应商详情
        AIProviderSettings globalSettings = getGlobalSettings();
        AIProviderConfig providerConfig = globalSettings.getDefaultProviderConfig(providerType);
        
        // 从插件配置中获取模型参数和运行时设置（可选，如果没有则使用全局默认值）
        AIProviderSettings pluginSettings = getPluginSettings(project);
        AIModelParameters modelParams = pluginSettings.modelParameters;
        AIRuntimeSettings runtimeSettings = pluginSettings.runtimeSettings;
        
        // 从全局凭证管理器获取 API Key
        String apiKey = GLOBAL_CREDENTIAL_MANAGER.getApiKey(providerConfig.credentialId);
        if (apiKey == null) {
            throw new AIServiceException(
                "API Key not configured for provider: " + providerType.getDisplayName() + 
                ". Please configure it in Settings → Tools → AI Common");
        }
        
        // 创建服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(
            providerConfig,
            modelParams,
            runtimeSettings,
            null,
            pluginSettings.performanceMode
        );
        
        if (provider == null) {
            throw new AIServiceException("Failed to create AI service provider");
        }
        
        // 生成内容（传递 listener）
        return provider.generateContent(request, apiKey, listener);
    }
    
    /**
     * 使用临时配置生成内容
     */
    private String generateContentWithConfig(@NotNull Project project,
                                            @NotNull AIChatRequest request,
                                            @NotNull AIServiceConfig config,
                                            @Nullable AIResponseListener listener) throws AIServiceException {
        // 使用临时配置
        AIProviderConfig providerConfig = convertToProviderConfig(config);
        AIModelParameters modelParams = convertToModelParameters(config);
        AIRuntimeSettings runtimeSettings = convertToRuntimeSettings(config);
        
        // 创建服务提供者
        AIServiceProvider provider = AIServiceFactory.createProvider(
            providerConfig,
            modelParams,
            runtimeSettings,
            null,
            false
        );
        
        if (provider == null) {
            throw new AIServiceException("Failed to create AI service provider");
        }
        
        // 生成内容（传递 listener）
        return provider.generateContent(request, config.apiKey, listener);
    }
    
    /**
     * 获取插件配置（需要插件实现）
     * <p>
     * 这里需要插件提供自己的 SettingsState，包含 providerType 等配置。
     * 可以通过反射或接口方式获取。
     */
    @NotNull
    private AIProviderSettings getPluginSettings(@NotNull Project project) {
        // 方式1：通过接口获取（推荐）
        // PluginSettingsProvider provider = project.getService(PluginSettingsProvider.class);
        // return provider.getAIProviderSettings();
        
        // 方式2：通过反射获取（兼容性更好）
        // 这里需要插件在 SettingsState 中包含 AIProviderSettings providerSettings 字段
        // 暂时返回全局默认配置
        AIProviderSettings globalSettings = getGlobalSettings();
        AIProviderSettings pluginSettings = new AIProviderSettings();
        pluginSettings.providerType = globalSettings.providerType; // 默认使用全局的
        pluginSettings.modelParameters = globalSettings.modelParameters.copy();
        pluginSettings.runtimeSettings = globalSettings.runtimeSettings.copy();
        return pluginSettings;
    }
    
    @Override
    @NotNull
    public AIProviderSettings getGlobalSettings() {
        return ApplicationManager.getApplication().getService(AIProviderSettings.class);
    }
    
    @Override
    @NotNull
    public ValidationResult validatePluginConfiguration(@NotNull Project project) {
        AIProviderSettings pluginSettings = getPluginSettings(project);
        AIProviderSettings globalSettings = getGlobalSettings();
        AIProviderConfig config = globalSettings.getDefaultProviderConfig(pluginSettings.providerType);
        String apiKey = GLOBAL_CREDENTIAL_MANAGER.getApiKey(config.credentialId);
        
        if (apiKey == null) {
            return ValidationResult.failure("API Key not configured for provider: " + pluginSettings.providerType.getDisplayName());
        }
        
        AIServiceProvider provider = AIServiceFactory.createProvider(
            config,
            pluginSettings.modelParameters,
            pluginSettings.runtimeSettings,
            null,
            false
        );
        
        if (provider == null) {
            return ValidationResult.failure("Failed to create provider");
        }
        
        return provider.validateConfiguration(apiKey);
    }
    
    /**
     * 获取服务实例
     */
    public static AIService getInstance() {
        return ApplicationManager.getApplication().getService(AIService.class);
    }
    
    // 内部转换方法（仅在需要临时配置时使用）
    private AIProviderConfig convertToProviderConfig(AIServiceConfig config) {
        AIProviderConfig providerConfig = new AIProviderConfig(config.providerType);
        providerConfig.modelName = config.modelName;
        if (config.baseUrl != null) {
            providerConfig.baseUrl = config.baseUrl;
        }
        return providerConfig;
    }
    
    private AIModelParameters convertToModelParameters(AIServiceConfig config) {
        AIModelParameters params = new AIModelParameters();
        params.temperature = config.temperature;
        params.maxTokens = config.maxTokens;
        return params;
    }
    
    private AIRuntimeSettings convertToRuntimeSettings(AIServiceConfig config) {
        AIRuntimeSettings settings = new AIRuntimeSettings();
        settings.maxRetries = config.maxRetries;
        settings.timeout = config.timeout;
        return settings;
    }
}
```

**关键实现点**：

- **配置隔离**：从插件配置中读取默认供应商，从全局配置中读取供应商详情
- **自动读取**：自动从插件配置中读取默认供应商类型
- **资源共享**：所有插件共享全局可用供应商列表和供应商详情

### 3.3 外部开发者使用示例

#### 3.3.1 在 plugin.xml 中声明依赖

```xml
<idea-plugin>
    <id>com.example.myplugin</id>
    <name>My Plugin</name>
    
    <!-- 依赖 AI Common 插件 -->
    <depends>dev.dong4j.zeka.stack.idea.plugin.common.ai</depends>
    
    <!-- 其他配置 -->
</idea-plugin>
```

#### 3.3.2 在代码中使用（最简单的方式 - 使用插件配置的默认供应商）

```java
import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;

public class MyService {
    
    public void generateContent(Project project) {
        // 获取 AI 服务实例
        AIService aiService = AIServiceImpl.getInstance();
        
        // 使用插件配置的默认供应商生成内容（最简单！）
        // 默认供应商从插件的 SettingsState 中读取
        // 供应商详情从全局配置中读取
        String result = aiService.generateContent(
            project,
            "你是一个代码助手",
            "请解释这段代码的功能",
            null  // 不使用监听器
        );
        
        System.out.println(result);
    }
}
```

#### 3.3.2.1 使用响应监听器处理 AI 响应

```java
import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;

public class MyService {
    
    public void generateContentWithListener(Project project) {
        AIService aiService = AIServiceImpl.getInstance();
        
        // 创建响应监听器，处理请求、响应和 token 使用量
        AIResponseListener listener = new AIResponseListener() {
            @Override
            public void onRequest(String providerName, String modelName, 
                                 String requestBody, boolean validation) {
                System.out.println("发送请求到: " + providerName + " / " + modelName);
                // 可以记录请求日志、统计等
            }
            
            @Override
            public void onResponse(String providerName, String modelName, 
                                  String responseBody, boolean validation) {
                System.out.println("收到响应 from: " + providerName + " / " + modelName);
                // 可以记录响应日志、保存响应等
            }
            
            @Override
            public void onUsage(String providerName, String modelName,
                               int promptTokens, int completionTokens, int totalTokens) {
                System.out.println(String.format(
                    "Token 使用量: Prompt=%d | Completion=%d | Total=%d",
                    promptTokens, completionTokens, totalTokens
                ));
                // 可以统计 token 使用量、计费等
            }
        };
        
        // 使用监听器生成内容
        String result = aiService.generateContent(
            project,
            "你是一个代码助手",
            "请解释这段代码的功能",
            listener  // 传入监听器
        );
        
        System.out.println("生成的内容: " + result);
    }
}
```

**说明**：

- ✅ **配置隔离**：使用插件自己的默认供应商，不影响其他插件
- ✅ **资源共享**：供应商详情从全局配置中读取，无需重复配置
- ✅ **自动读取**：自动从插件配置中读取默认供应商类型
- ✅ **回调支持**：通过 `AIResponseListener` 处理请求、响应和 token 使用量
- ✅ **只需 3 行代码**：获取服务 → 调用方法 → 获得结果（不使用监听器时）

#### 3.3.3 在代码中使用（指定供应商类型）

```java
import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

public class MyService {
    
    public void generateContent(Project project) {
        AIService aiService = AIServiceImpl.getInstance();
        
        // 临时使用指定的供应商类型（从全局可用列表中选取）
        AIChatRequest request = new AIChatRequest(
            "你是一个代码助手",
            "请解释这段代码的功能"
        );
        
        // 使用指定的供应商类型，供应商详情从全局配置中读取
        String result = aiService.generateContent(
            project,
            AIProviderType.QIANWEN,  // 临时使用通义千问
            request,
            null  // 不使用监听器
        );
        
        System.out.println(result);
    }
}
```

**说明**：

- 大多数情况下，使用插件配置的默认供应商即可
- 只在特殊场景下需要临时使用其他供应商时才指定
- 供应商详情（baseUrl、模型列表等）从全局配置中自动读取

## 4. 实施步骤

### 4.1 第一阶段：创建 plugin.xml

1. 在 `ai-common/src/main/resources/META-INF/` 创建 `plugin.xml`
2. 配置插件基本信息
3. 注册 `AIService` 为应用级服务

### 4.2 第二阶段：设计并实现简洁 API

1. 创建 `AIService` 接口
2. 创建 `AIServiceConfig` 配置类
3. 实现 `AIServiceImpl` 服务类
4. 实现配置转换逻辑

### 4.3 第三阶段：更新构建配置

1. 修改 `build.gradle.kts`，支持插件构建
2. 配置插件发布信息
3. 测试插件打包和安装

### 4.4 第四阶段：文档和示例

1. 编写 API 文档
2. 创建使用示例
3. 编写集成指南

## 5. 优势分析

### 5.1 对 ai-common 的优势

1. **独立发布**：可以作为独立插件发布到 IntelliJ 插件市场
2. **版本管理**：独立的版本号，便于管理和更新
3. **用户基础**：可以被更多插件使用，扩大影响力

### 5.2 对外部开发者的优势

1. **使用简单**：只需几行代码即可集成 AI 能力
2. **配置灵活**：支持快速使用和精细配置两种模式
3. **功能完整**：支持多种 AI 服务提供商
4. **安全可靠**：API Key 安全存储，错误处理完善

### 5.3 对生态系统的优势

1. **标准化**：统一的 AI 服务接口，促进插件生态发展
2. **复用性**：避免每个插件都实现自己的 AI 集成
3. **维护性**：集中维护，统一更新和修复

## 6. 兼容性考虑

### 6.1 向后兼容

- 保留现有的 `AIServiceFactory` 和配置类
- 新的 `AIService` 作为简化 API，内部仍使用现有实现
- 现有使用 `ai-common` 作为库的插件可以继续使用

### 6.2 迁移路径

1. **阶段一**：同时支持库依赖和插件依赖两种方式
2. **阶段二**：推荐新插件使用插件依赖方式
3. **阶段三**：逐步迁移现有插件到插件依赖方式

## 7. 实施建议

### 7.1 优先级

1. **高优先级**：创建 `plugin.xml` 和 `AIService` 接口
2. **中优先级**：实现 `AIServiceImpl` 和配置转换
3. **低优先级**：文档和示例完善

### 7.2 注意事项

1. **API 稳定性**：确保 `AIService` 接口稳定，避免频繁变更
2. **性能考虑**：配置转换可能有性能开销，考虑缓存
3. **错误处理**：提供清晰的错误信息和异常类型
4. **版本兼容**：确保不同版本的插件可以正常工作

## 8. 完整使用示例

### 8.1 最简单的使用方式

```java
import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;

public class SimpleExample {
    public void generateText(Project project) {
        AIService aiService = AIServiceImpl.getInstance();
        
        // 使用插件配置的默认供应商（不使用监听器）
        String result = aiService.generateContent(
            project,
            "你是一个代码助手",
            "请解释什么是单例模式",
            null
        );
        
        System.out.println(result);
    }
}
```

### 8.1.1 使用响应监听器

```java
import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;

public class SimpleExampleWithListener {
    public void generateText(Project project) {
        AIService aiService = AIServiceImpl.getInstance();
        
        // 创建响应监听器
        AIResponseListener listener = new AIResponseListener() {
            @Override
            public void onRequest(String providerName, String modelName, 
                                 String requestBody, boolean validation) {
                // 处理请求：记录日志、统计等
                System.out.println("请求: " + providerName + " / " + modelName);
            }
            
            @Override
            public void onResponse(String providerName, String modelName, 
                                  String responseBody, boolean validation) {
                // 处理响应：记录日志、保存响应等
                System.out.println("响应: " + providerName + " / " + modelName);
            }
            
            @Override
            public void onUsage(String providerName, String modelName,
                               int promptTokens, int completionTokens, int totalTokens) {
                // 处理 token 使用量：统计、计费等
                System.out.println(String.format(
                    "Token: Prompt=%d | Completion=%d | Total=%d",
                    promptTokens, completionTokens, totalTokens
                ));
            }
        };
        
        // 使用监听器生成内容
        String result = aiService.generateContent(
            project,
            "你是一个代码助手",
            "请解释什么是单例模式",
            listener
        );
        
        System.out.println(result);
    }
}
```

### 8.2 带自定义配置的使用方式

```java
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;

public class AdvancedExample {
    public void generateText() {
        AIService aiService = AIServiceImpl.getInstance();
        
        // 创建自定义配置
        AIServiceConfig config = new AIServiceConfig();
        config.providerType = AIProviderType.CUSTOM;
        config.modelName = "gpt-4";
        config.baseUrl = "https://api.openai.com/v1";
        config.apiKey = "your-api-key";
        config.temperature = 0.8;
        config.maxTokens = 4000;
        config.maxRetries = 5;
        config.timeout = 60000;
        
        // 创建请求
        AIChatRequest request = new AIChatRequest(
            "你是一个专业的代码审查专家",
            "请审查这段代码并提出改进建议：\n" + code
        );
        
        // 生成内容
        String result = aiService.generateContent(request, config);
        
        System.out.println(result);
    }
}
```

### 8.3 验证插件配置

```java
import com.intellij.openapi.project.Project;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.ValidationResult;

public class ConfigExample {
    public void validatePluginConfig(Project project) {
        AIService aiService = AIServiceImpl.getInstance();
        
        // 验证插件配置是否有效
        ValidationResult result = aiService.validatePluginConfiguration(project);
        
        if (result.isValid()) {
            System.out.println("插件配置验证成功");
            
            // 获取全局配置信息（可用供应商列表）
            AIProviderSettings globalSettings = aiService.getGlobalSettings();
            System.out.println("可用供应商数量：" + globalSettings.availableProviders.size());
        } else {
            System.out.println("插件配置验证失败：" + result.getErrorMessage());
            System.out.println("请在插件的设置页面中配置默认供应商");
        }
    }
}
```

## 9. 与现有实现的对比

### 9.1 使用步骤对比

**现有方式（作为库依赖，7 步）**：

1. 创建 `AIProviderConfig`
2. 创建 `AIModelParameters`
3. 创建 `AIRuntimeSettings`
4. 创建 `AICredentialManager`
5. 获取 API Key
6. 创建 `AIServiceProvider`
7. 构建请求并生成内容

**新方式（作为插件依赖，2 步）**：

1. 获取 `AIService` 实例
2. 调用 `generateContent` 方法（使用插件配置的默认供应商）

### 9.2 代码量对比

**现有方式（作为库）**：约 20-30 行代码
**新方式（作为插件，使用插件配置的默认供应商）**：约 3 行代码

```java
// 新方式：只需要 3 行代码！
AIService aiService = AIServiceImpl.getInstance();
String result = aiService.generateContent(project, "系统提示", "用户提示", null);
System.out.println(result);

// 如果需要处理响应，可以使用监听器
AIResponseListener listener = new AIResponseListener() {
    @Override
    public void onUsage(String providerName, String modelName,
                       int promptTokens, int completionTokens, int totalTokens) {
        // 统计 token 使用量
    }
};
String result2 = aiService.generateContent(project, "系统提示", "用户提示", listener);
```

### 9.3 配置管理对比

**现有方式**：

- 每个插件需要自己管理配置
- 每个插件需要自己的设置页面
- 配置分散，难以统一管理

**新方式**：

- ✅ **配置隔离**：每个插件维护自己的默认供应商，互不影响
- ✅ **资源共享**：所有插件共享可用供应商列表和供应商详情
- ✅ **统一管理**：用户在一个地方管理所有可用供应商
- ✅ **灵活选择**：每个插件可以选择最适合的供应商

### 9.4 用户体验对比

**现有方式**：

- 用户需要在每个插件的设置页面分别配置 AI
- 配置重复，维护困难

**新方式**：

- ✅ 用户只需在 "Settings → Tools → AI Common" 管理可用供应商列表
- ✅ 每个插件在自己的设置页面中选择默认供应商（从全局可用列表中选取）
- ✅ 配置隔离，互不影响；资源共享，避免重复配置

## 10. 实施检查清单

### 10.1 必须完成的任务

- [ ] 创建 `plugin.xml` 配置文件
- [ ] 创建 `AIService` 接口
- [ ] 创建 `AIServiceConfig` 配置类
- [ ] 实现 `AIServiceImpl` 服务类
- [ ] 更新 `build.gradle.kts` 支持插件构建
- [ ] 编写单元测试
- [ ] 编写 API 文档

### 10.2 可选任务

- [ ] 创建使用示例项目
- [ ] 编写集成指南
- [ ] 创建视频教程
- [ ] 发布到 IntelliJ 插件市场

## 11. 风险评估

### 11.1 技术风险

- **低风险**：API 设计简单，实现难度低
- **低风险**：基于现有稳定实现，风险可控

### 11.2 兼容性风险

- **中风险**：需要确保与现有使用方式兼容
- **缓解措施**：保留现有 API，新 API 作为补充

### 11.3 维护风险

- **低风险**：代码结构清晰，易于维护
- **低风险**：有完整的测试覆盖

## 12. 核心设计理念

### 12.1 配置隔离 + 资源共享

**设计目标**：让外部开发者**几乎不需要任何配置**就能使用 AI 能力，同时保证配置隔离。

**实现方式**：

1. `ai-common` 作为插件发布，提供**全局可用供应商列表**（应用级）
2. 用户只需在 "Settings → Tools → AI Common" 管理可用供应商列表
3. 每个插件在自己的设置页面中选择默认供应商（从全局可用列表中选取）
4. 外部插件依赖后，**自动获得**可用供应商列表，但各自维护默认供应商选择
5. 外部开发者只需调用 `generateContent(project, ...)`，自动使用插件配置的默认供应商

### 12.2 完整的闭环

`ai-common` 作为插件，提供完整的闭环：

- ✅ **配置管理**：全局 `AIProviderSettings` 服务（可用供应商列表）
- ✅ **UI 组件**：`AIProviderConfigPanel` 可在设置页面使用
- ✅ **资源文件**：图标、国际化文本等
- ✅ **服务能力**：`AIService` 统一入口
- ✅ **状态栏**：显示当前 AI 提供商状态

外部开发者：

- ✅ **配置隔离**：每个插件维护自己的默认供应商，互不影响
- ✅ **资源共享**：所有插件共享可用供应商列表和供应商详情
- ✅ **无需创建 UI**：可以使用 `AIProviderConfigPanel`（如果需要）
- ✅ **无需管理资源**：图标、文本等都已包含
- ✅ **只需调用接口**：`AIService.generateContent(project, ...)`

### 12.3 使用场景

#### 场景 1：最简单的使用（推荐）

```java
// 外部开发者的代码（不使用监听器）
AIService aiService = AIServiceImpl.getInstance();
String result = aiService.generateContent(project, "系统提示", "用户提示", null);

// 或者使用监听器处理响应
AIResponseListener listener = new AIResponseListener() {
    @Override
    public void onUsage(String providerName, String modelName,
                       int promptTokens, int completionTokens, int totalTokens) {
        // 统计 token 使用量
    }
};
String result2 = aiService.generateContent(project, "系统提示", "用户提示", listener);
```

**前提**：

- 用户已经在 "Settings → Tools → AI Common" 中配置了可用供应商列表
- 插件在自己的设置页面中选择了默认供应商（从全局可用列表中选取）

#### 场景 2：在自己的设置页面中嵌入配置

如果外部插件想要在自己的设置页面中也有 AI 配置（而不是使用全局配置）：

```java
// 在外部插件的设置面板中
AIProviderConfigPanel aiConfigPanel = new AIProviderConfigPanel(
    new AICredentialManager("MyPlugin", "MY_PLUGIN_API_KEY_")
);

// 添加到自己的设置面板
mainPanel.add(aiConfigPanel.getPanel());
```

这样用户可以在外部插件的设置页面中单独配置 AI（与全局配置隔离）。

#### 场景 3：混合使用

外部插件可以：

- 默认使用全局配置（最简单）
- 允许用户在自己的设置页面中覆盖配置（高级用户）

## 13. 结论

`ai-common` **完全具备**作为 IntelliJ 插件扩展的能力，只需要：

1. ✅ 创建 `plugin.xml` 配置文件
2. ✅ 将 `AIProviderSettings` 注册为应用级服务（全局配置）
3. ✅ 设计并实现简洁的 `AIService` API（自动使用全局配置）
4. ✅ 创建全局设置页面（"Settings → Tools → AI Common"）
5. ✅ 更新构建配置支持插件发布

实施后，外部开发者可以：

### 最简单的使用方式（推荐）

```java
// 只需要 3 行代码！
AIService aiService = AIServiceImpl.getInstance();
String result = aiService.generateContent(project, "系统提示", "用户提示", null);
// 完成！默认供应商从插件配置中读取，供应商详情从全局配置中读取

// 如果需要处理响应，可以使用监听器
AIResponseListener listener = new AIResponseListener() {
    @Override
    public void onUsage(String providerName, String modelName,
                       int promptTokens, int completionTokens, int totalTokens) {
        // 统计 token 使用量、计费等
    }
};
String result2 = aiService.generateContent(project, "系统提示", "用户提示", listener);
```

### 优势

- ✅ **配置隔离**：每个插件维护自己的默认供应商，互不影响
- ✅ **资源共享**：所有插件共享可用供应商列表和供应商详情，避免重复配置
- ✅ **代码量最少**：从 20+ 行减少到 3 行
- ✅ **完整的闭环**：配置、UI、资源、服务一应俱全
- ✅ **灵活选择**：每个插件可以选择最适合的供应商

这将**极大降低**其他插件集成 AI 能力的门槛，促进 IntelliJ 插件生态的发展。

## 13. 后续优化方向

1. **流式响应支持**：支持流式返回 AI 生成内容
2. **批量请求支持**：支持批量生成内容
3. **缓存机制**：缓存常用请求结果
4. **性能监控**：提供性能指标和监控
5. **插件市场推广**：发布到 IntelliJ 插件市场，扩大用户基础

