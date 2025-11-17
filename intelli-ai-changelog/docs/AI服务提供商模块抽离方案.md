# AI 服务提供商模块抽离方案

## 一、方案概述

### 1.1 目标

将 `intelli-ai-javadoc` 中与 AI 服务提供商相关的逻辑抽离到独立的 `intelli-ai-engine` 模块中，实现代码复用，使得 `intelli-ai-javadoc` 和
`intelli-ai-changelog` 等插件可以共享相同的
AI 服务提供商配置和功能。

### 1.2 设计原则

- **单一职责**：`intelli-ai-engine` 模块只负责 AI 服务提供商的通用逻辑
- **开闭原则**：对扩展开放，对修改关闭，便于添加新的 AI 提供商
- **依赖倒置**：高层模块（业务逻辑）依赖抽象（接口），不依赖具体实现
- **最小依赖**：`intelli-ai-engine` 模块尽量少依赖其他模块，保持独立性

### 1.3 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    intelli-ai-engine 模块                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  AI 服务提供商核心                                  │  │
│  │  - AIProviderType (枚举)                          │  │
│  │  - AIServiceFactory (工厂类)                      │  │
│  │  - AIServiceProvider (接口)                       │  │
│  │  - AICompatibleProvider (抽象基类)                │  │
│  │  - Provider 实现类 (QianWen, Ollama 等)           │  │
│  │  - ValidationResult                               │  │
│  │  - AIServiceException                             │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  配置管理                                          │  │
│  │  - AIProviderConfig (配置类)                      │  │
│  │  - AIProviderConfigState (状态管理)               │  │
│  │  - ProviderConfigUtils (工具类)                   │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  UI 组件                                          │  │
│  │  - AIProviderConfigPanel (配置面板)               │  │
│  │  - ProviderListCellRenderer (列表渲染器)          │  │
│  │  - AvailableProvidersTableModel (表格模型)         │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  资源文件                                          │  │
│  │  - AICommonBundle (国际化)                        │  │
│  │  - messages.properties                            │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
         ▲                          ▲
         │                          │
         │ 依赖                      │ 依赖
         │                          │
┌────────┴────────┐        ┌────────┴────────┐
│   intelli-ai-javadoc    │        │  intelli-ai-changelog   │
│                 │        │                 │
│  - 业务逻辑      │        │  - 业务逻辑      │
│  - 提示词模板    │        │  - 提示词模板    │
│  - 输出处理      │        │  - 输出处理      │
└─────────────────┘        └─────────────────┘
```

## 二、模块结构设计

### 2.1 intelli-ai-engine 模块包结构

```
intelli-ai-engine/
├── src/main/java/dev/dong4j/zeka/stack/idea/plugin/common/
│   ├── ai/
│   │   ├── AIProviderType.java                    # 提供商类型枚举
│   │   ├── AIServiceFactory.java                  # 服务工厂
│   │   ├── AIServiceException.java                # 异常类
│   │   ├── ValidationResult.java                  # 验证结果
│   │   └── provider/
│   │       ├── AIServiceProvider.java             # 服务提供商接口
│   │       ├── AICompatibleProvider.java          # 兼容提供商基类
│   │       ├── QianWenProvider.java                # 通义千问实现
│   │       ├── OllamaProvider.java                 # Ollama 实现
│   │       ├── LMStudioProvider.java              # LM Studio 实现
│   │       ├── SiliconFlowProvider.java           # 硅基流动实现
│   │       └── CustomProvider.java                # 自定义提供商实现
│   ├── config/
│   │   ├── AIProviderConfig.java                  # 提供商配置类
│   │   ├── AIProviderConfigState.java             # 配置状态管理
│   │   └── ProviderConfigUtils.java               # 配置工具类
│   ├── ui/
│   │   ├── AIProviderConfigPanel.java             # AI 提供商配置面板
│   │   ├── ProviderListCellRenderer.java          # 提供商列表渲染器
│   │   └── AvailableProvidersTableModel.java      # 可用提供商表格模型
│   └── util/
│       └── AICommonBundle.java                    # 国际化资源包
└── src/main/resources/
    ├── META-INF/
    │   └── plugin.xml                             # 插件配置（如果需要）
    └── messages/
        ├── AICommonBundle.properties              # 英文资源
        └── AICommonBundle_zh_CN.properties        # 中文资源
```

### 2.2 核心接口设计

#### 2.2.1 AIServiceProvider 接口（通用化）

```java
public interface AIServiceProvider {
    /**
     * 调用 AI 服务生成内容（通用方法）
     * 
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 生成的响应内容
     * @throws AIServiceException 当调用失败时抛出
     */
    @NotNull
    String generateContent(@NotNull String systemPrompt, 
                           @NotNull String userPrompt) 
        throws AIServiceException;
    
    /**
     * 验证配置是否正确
     */
    @NotNull
    ValidationResult validateConfiguration(String apiKey);
    
    // ... 其他方法保持不变
}
```

#### 2.2.2 AIProviderConfig 配置类

```java
public class AIProviderConfig {
    public AIProviderType providerType;
    public String modelName;
    public String baseUrl;
    public String md5;  // 配置唯一标识
    public boolean configurationVerified;
    public String remark;  // 备注
    
    // 模型参数
    public double temperature = 0.7;
    public int maxTokens = 2000;
    public double topP = 0.9;
    public int topK = 50;
    public double presencePenalty = 0.1;
    
    // ... getter/setter
}
```

#### 2.2.3 AIProviderConfigState 状态管理

```java
@State(
    name = "AIProviderConfigState",
    storages = @Storage("intelli-ai-engine-provider-config.xml")
)
public class AIProviderConfigState implements PersistentStateComponent<AIProviderConfigState> {
    // 当前选中的提供商
    public AIProviderType providerType = AIProviderType.QIANWEN;
    
    // 默认提供商配置（按提供商类型分组）
    public Map<AIProviderType, AIProviderConfig> defaultProviders = new HashMap<>();
    
    // 可用提供商列表（用于性能模式）
    public List<AIProviderConfig> availableProviders = new ArrayList<>();
    
    // ... getInstance(), getState(), loadState()
}
```

### 2.3 UI 组件设计

#### 2.3.1 AIProviderConfigPanel

可复用的 AI 提供商配置面板，包含：

- 提供商下拉选择框（带图标）
- 模型下拉框（可编辑）
- Base URL 输入框
- API Key 输入框（密码字段）
- 测试连接按钮
- 刷新模型按钮
- 可用提供商列表（可选显示）
- 模型参数配置（高级设置）

#### 2.3.2 使用方式

```java
// 在 intelli-ai-javadoc 或 intelli-ai-changelog 的设置面板中使用
AIProviderConfigPanel providerPanel = new AIProviderConfigPanel();
providerPanel.loadSettings(configState);
providerPanel.getSettings();  // 获取配置
```

## 三、实施计划

### 阶段一：创建 intelli-ai-engine 模块（1-2 天）

#### 1.1 创建模块结构

- [ ] 创建 `intelli-ai-engine` 目录
- [ ] 复制 `template` 作为基础模板
- [ ] 更新 `build.gradle.kts` 和 `gradle.properties`
- [ ] 创建包结构

#### 1.2 迁移核心类

- [ ] 迁移 `AIProviderType` 枚举
- [ ] 迁移 `AIServiceFactory` 工厂类
- [ ] 迁移 `AIServiceProvider` 接口
- [ ] 迁移 `AICompatibleProvider` 抽象类
- [ ] 迁移所有 Provider 实现类
- [ ] 迁移 `ValidationResult` 和 `AIServiceException`

#### 1.3 创建配置管理

- [ ] 创建 `AIProviderConfig` 配置类
- [ ] 创建 `AIProviderConfigState` 状态管理
- [ ] 迁移 `ProviderConfigUtils` 工具类

#### 1.4 创建 UI 组件

- [ ] 创建 `AIProviderConfigPanel` 配置面板
- [ ] 迁移 `ProviderListCellRenderer`
- [ ] 迁移 `AvailableProvidersTableModel`
- [ ] 提取 UI 相关方法（测试连接、刷新模型等）

#### 1.5 资源文件

- [ ] 创建 `AICommonBundle` 国际化类
- [ ] 提取 AI 提供商相关的资源文件条目
- [ ] 创建 `messages.properties` 和 `messages_zh_CN.properties`

### 阶段二：重构 intelli-ai-javadoc 使用 intelli-ai-engine（2-3 天）

#### 2.1 添加依赖

- [ ] 在 `intelli-ai-javadoc/build.gradle.kts` 中添加对 `intelli-ai-engine` 的依赖
- [ ] 更新 `settings.gradle.kts` 包含 `ai-common` 模块

#### 2.2 重构代码

- [ ] 删除 `intelli-ai-javadoc` 中的 AI 提供商相关类
- [ ] 更新 `SettingsState`，移除 AI 提供商配置，改为引用 `AIProviderConfigState`
- [ ] 重构 `JavaDocSettingsPanel`，使用 `AIProviderConfigPanel`
- [ ] 更新 `AIServiceFactory` 的调用方式
- [ ] 更新所有导入语句

#### 2.3 适配业务逻辑

- [ ] 更新 `AIServiceProvider` 接口调用，使用新的 `generateContent` 方法
- [ ] 保持 `generateDocumentation` 方法在 `intelli-ai-javadoc` 中作为业务层方法
- [ ] 测试所有功能是否正常

### 阶段三：在 intelli-ai-changelog 中集成 ai-common（1-2 天）

#### 3.1 添加依赖

- [ ] 在 `intelli-ai-changelog/build.gradle.kts` 中添加对 `ai-common` 的依赖
- [ ] 更新 `settings.gradle.kts` 包含 `ai-common` 模块

#### 3.2 集成配置面板

- [ ] 在 `ChangelogSettingsPanel` 中集成 `AIProviderConfigPanel`
- [ ] 更新 `SettingsState`，引用 `AIProviderConfigState`
- [ ] 实现 changelog 特定的业务逻辑

#### 3.3 实现业务逻辑

- [ ] 创建 `ChangelogGenerator` 服务类
- [ ] 实现基于 Git 提交历史的提示词生成
- [ ] 调用 `AIServiceProvider.generateContent()` 生成变更日志
- [ ] 实现输出处理和格式化

### 阶段四：测试和优化（1-2 天）

#### 4.1 功能测试

- [ ] 测试 `intelli-ai-javadoc` 的所有功能
- [ ] 测试 `intelli-ai-changelog` 的基本功能
- [ ] 测试 AI 提供商配置的保存和加载
- [ ] 测试测试连接和刷新模型功能

#### 4.2 代码优化

- [ ] 检查代码重复
- [ ] 优化导入语句
- [ ] 更新文档和注释
- [ ] 代码审查

## 四、关键技术点

### 4.1 模块依赖管理

在根目录的 `settings.gradle.kts` 中：

```kotlin
rootProject.name = "zeka-idea-plugin"
include("ai-common")
include("intelli-ai-javadoc")
include("intelli-ai-changelog")
```

在 `intelli-ai-javadoc/build.gradle.kts` 中：

```kotlin
dependencies {
    implementation(project(":ai-common"))
    // ... 其他依赖
}
```

### 4.2 配置状态管理

使用组合模式，让每个插件的 `SettingsState` 包含 `AIProviderConfigState`：

```java
@State(name = "JavaDocAISettings", ...)
public class SettingsState implements PersistentStateComponent<SettingsState> {
    // 引用 ai-common 的配置
    @Transient
    private AIProviderConfigState providerConfigState = AIProviderConfigState.getInstance();
    
    // 插件特定的配置
    public boolean generateForClass = true;
    // ...
}
```

### 4.3 UI 组件复用

使用组合模式，在设置面板中嵌入 `AIProviderConfigPanel`：

```java
public class JavaDocSettingsPanel {
    private AIProviderConfigPanel providerConfigPanel;
    
    private void createUI() {
        // 创建 AI 提供商配置面板
        providerConfigPanel = new AIProviderConfigPanel();
        
        // 添加到主面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(providerConfigPanel.getPanel())
            // ... 其他组件
            .getPanel();
    }
}
```

### 4.4 接口通用化

将 `generateDocumentation` 方法改为更通用的 `generateContent` 方法：

```java
// ai-common 中的接口
public interface AIServiceProvider {
    String generateContent(String systemPrompt, String userPrompt);
}

// intelli-ai-javadoc 中的业务层
public class JavaDocServiceProvider {
    private AIServiceProvider provider;
    
    public String generateDocumentation(String code, TaskType type, String language) {
        String systemPrompt = getSystemPrompt(type);
        String userPrompt = buildUserPrompt(code, type, language);
        return provider.generateContent(systemPrompt, userPrompt);
    }
}
```

## 五、风险与挑战

### 5.1 配置迁移

- **风险**：现有用户的配置需要迁移
- **解决方案**：在 `loadState` 方法中实现配置迁移逻辑

### 5.2 版本兼容性

- **风险**：不同插件可能使用不同版本的 `ai-common`
- **解决方案**：使用语义化版本，保持向后兼容

### 5.3 资源文件冲突

- **风险**：不同插件的资源文件可能冲突
- **解决方案**：使用不同的资源包名称（`AICommonBundle` vs `JavaDocBundle`）

## 六、预期收益

1. **代码复用**：AI 提供商相关代码只需维护一份
2. **一致性**：所有插件使用相同的 AI 提供商配置界面
3. **可扩展性**：新增 AI 提供商只需在 `ai-common` 中添加
4. **可维护性**：AI 提供商相关 bug 修复只需在一处进行
5. **开发效率**：新插件可以快速集成 AI 功能

## 七、后续扩展

1. 支持更多 AI 提供商（OpenAI、Claude 等）
2. 支持插件级别的 AI 提供商配置覆盖
3. 支持 AI 提供商性能监控和统计
4. 支持 AI 提供商负载均衡和故障转移

