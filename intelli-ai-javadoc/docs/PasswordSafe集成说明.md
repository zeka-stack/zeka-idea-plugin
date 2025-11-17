# PasswordSafe 集成说明

## 概述

本文档说明如何使用 IntelliJ IDEA SDK 推荐的 `PasswordSafe` API 安全地存储和管理 API Key，替代明文存储方式。

## 背景

### 为什么需要 PasswordSafe？

1. **安全性**：API Key 是敏感信息，不应该以明文形式存储在 XML 配置文件中
2. **合规性**：符合安全存储最佳实践，避免密钥泄露风险
3. **平台集成**：利用操作系统的安全密钥存储机制：
    - macOS: Keychain
    - Windows: Credential Store
    - Linux: Secret Service (如 GNOME Keyring、KWallet)

### 废弃的 API

以下 API 已被标记为 `@Deprecated`，不应继续使用：

```java
// ❌ 已废弃
@Deprecated
String get(CredentialAttributes attributes)

// ✅ 推荐使用
Credentials get(CredentialAttributes attributes)
```

## 设计方案

### 核心概念

插件采用**双层配置管理**架构，将服务商配置分为两类：

1. **defaultProviders（默认配置）**：每个服务商类型的默认配置，用于UI切换
2. **availableProviders（可用配置）**：所有可用配置列表，用于性能模式

### 密钥存储策略

采用 **UUID 方案**，为每个服务提供商配置分配唯一标识符：

- **defaultProviders 中的配置**：每个服务商类型有固定的 UUID
- **PasswordSafe 存储键名**：`AI_JAVADOC_API_KEY_{uuid}`
- **availableProviders 中的配置**：复用 defaultProviders 的 UUID

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      SettingsState                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  defaultProviders (Map<AIProviderType, ProviderConfig>)   │
│  ├─ QIANWEN     → ProviderConfig (uuid-1)                 │
│  ├─ OLLAMA      → ProviderConfig (uuid-2)                 │
│  ├─ CUSTOM      → ProviderConfig (uuid-3)                 │
│  └─ ...                                                     │
│                                                             │
│  availableProviders (List<ProviderConfig>)                │
│  ├─ ProviderConfig (uuid-1, QIANWEN, model-a)            │
│  ├─ ProviderConfig (uuid-1, QIANWEN, model-b)            │
│  ├─ ProviderConfig (uuid-2, OLLAMA, model-x)             │
│  └─ ...                                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                     PasswordSafe                            │
├─────────────────────────────────────────────────────────────┤
│  AI_JAVADOC_API_KEY_uuid-1 → "sk-xxx..."                  │
│  AI_JAVADOC_API_KEY_uuid-2 → ""         (Ollama无需Key)   │
│  AI_JAVADOC_API_KEY_uuid-3 → "custom-key..."              │
└─────────────────────────────────────────────────────────────┘
```

### 设计优势

- ✅ **配置隔离**：defaultProviders 和 availableProviders 各司其职
- ✅ **切换无损**：切换服务商时不会丢失配置和 API Key
- ✅ **多配置支持**：同一服务商可以有多个不同的模型配置
- ✅ **UUID 复用**：availableProviders 复用 defaultProviders 的 UUID
- ✅ **逻辑清晰**：数据流向明确，易于维护

### 数据流向

#### 1. 切换服务商

```
用户选择服务商
  ↓
从 defaultProviders 获取该服务商的配置
  ↓
使用配置的 UUID 从 PasswordSafe 读取 API Key
  ↓
加载到 UI 显示
```

#### 2. 保存配置

```
用户修改配置
  ↓
获取当前服务商的 defaultConfig
  ↓
使用 defaultConfig.uuid 存储 API Key 到 PasswordSafe
  ↓
更新 defaultProviders 中的配置
```

#### 3. 添加到可用列表

```
测试连接成功
  ↓
获取当前服务商的 defaultConfig.uuid
  ↓
创建新的 ProviderConfig（复用 UUID）
  ↓
添加到 availableProviders
```

### 替代方案对比

| 方案                      | 优点         | 缺点            | 是否采用 |
|-------------------------|------------|---------------|------|
| 基于索引                    | 简单         | 列表顺序改变会导致密钥混乱 | ❌    |
| 基于配置哈希                  | 基于内容       | 修改配置会丢失原密钥    | ❌    |
| UUID + defaultProviders | 唯一且稳定，配置隔离 | 需要额外字段和Map    | ✅    |
| 配置组合键名                  | 可读性好       | 键名过长，特殊字符需转义  | ❌    |

## 实现细节

### 1. 常量定义

在 `SettingsState` 类中定义 PasswordSafe 相关常量：

```java
public class SettingsState implements PersistentStateComponent<SettingsState> {

    // ==================== PasswordSafe 相关常量 ====================

    /** PasswordSafe 服务名称 */
    private static final String PASSWORD_SAFE_SERVICE_NAME = "IntelliDoc Assistant";

    /** PasswordSafe 默认服务商的存储键名 */
    private static final String PASSWORD_SAFE_KEY_DEFAULT = "AI_JAVADOC_API_KEY_DEFAULT";

    /** PasswordSafe 存储键名前缀 */
    private static final String PASSWORD_SAFE_KEY_PREFIX = "AI_JAVADOC_API_KEY_";
}
```

### 2. 数据结构调整

#### 添加 defaultProviders 字段

```java
/**
 * 默认服务提供商配置映射
 * 
 * Key: AIProviderType（服务商类型）
 * Value: ProviderConfig（该服务商的默认配置）
 */
public Map<AIProviderType, ProviderConfig> defaultProviders = new HashMap<>();
```

#### 移除明文 apiKey 字段

```java
// ❌ 移除外部的明文存储
// public String apiKey = "";
```

#### 为 ProviderConfig 添加 UUID

```java
public static class ProviderConfig {
    /** 唯一标识符，用于关联 PasswordSafe 中的 API 密钥 */
    public String uuid;
    
    /** 提供商标识符 */
    public AIProviderType providerType;
    
    /** 模型名称 */
    public String modelName;
    
    /** 基础请求地址 */
    public String baseUrl;
    
    // ❌ 移除 apiKey 字段
    // public String apiKey;
    
    /** 配置是否已验证的标志 */
    public boolean configurationVerified;
    
    /** 最近一次验证的时间戳，单位为毫秒 */
    public long lastVerifiedTime;

    /**
     * 默认构造函数，自动生成 UUID
     */
    public ProviderConfig() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * 完整构造函数
     * API 密钥将通过 PasswordSafe 单独存储
     */
    public ProviderConfig(AIProviderType providerType, String modelName, 
                          String baseUrl, boolean configurationVerified) {
        this.uuid = UUID.randomUUID().toString();
        this.providerType = providerType;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.configurationVerified = configurationVerified;
        this.lastVerifiedTime = System.currentTimeMillis();
    }

    /**
     * 构造函数（指定 UUID）
     * 用于复用已有的 UUID，确保 API Key 正确关联
     */
    public ProviderConfig(@Nullable String uuid, AIProviderType providerType, 
                          String modelName, String baseUrl, boolean configurationVerified) {
        this.uuid = uuid != null ? uuid : UUID.randomUUID().toString();
        this.providerType = providerType;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.configurationVerified = configurationVerified;
        this.lastVerifiedTime = System.currentTimeMillis();
    }
}
```

### 3. defaultProviders 管理方法

#### 获取默认配置

```java
/**
 * 获取指定服务商类型的默认配置
 * 如果不存在则自动创建
 */
@NotNull
public ProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType) {
    return defaultProviders.computeIfAbsent(providerType, type -> {
        ProviderConfig config = new ProviderConfig();
        config.providerType = type;
        config.modelName = type.getDefaultModel();
        config.baseUrl = type.getDefaultBaseUrl();
        config.configurationVerified = false;
        return config;
    });
}
```

#### 更新默认配置

```java
/**
 * 更新指定服务商类型的默认配置
 */
public void updateDefaultProviderConfig(@NotNull AIProviderType providerType, 
                                        @NotNull ProviderConfig config) {
    defaultProviders.put(providerType, config);
}
```

### 4. PasswordSafe API 封装

#### 创建凭证属性

```java
/**
 * 创建 CredentialAttributes
 * 
 * @param key 存储键名
 * @return CredentialAttributes 对象
 */
@NotNull
private static CredentialAttributes createCredentialAttributes(@NotNull String key) {
    return new CredentialAttributes(
        CredentialAttributesKt.generateServiceName(PASSWORD_SAFE_SERVICE_NAME, key)
    );
}
```

#### 默认服务商的密钥管理

```java
/**
 * 获取默认服务商的 API Key
 * 
 * @return API Key，如果不存在则返回 null
 */
@Nullable
public String getDefaultApiKey() {
    Credentials credentials = PasswordSafe.getInstance().get(
        createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT)
    );
    return credentials != null ? credentials.getPasswordAsString() : null;
}

/**
 * 设置默认服务商的 API Key
 * 
 * @param apiKey API 密钥，如果为 null 或空字符串则删除已存储的密钥
 */
public void setDefaultApiKey(@Nullable String apiKey) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
        // 删除密钥
        PasswordSafe.getInstance().set(
            createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT),
            null
        );
    } else {
        // 存储密钥
        PasswordSafe.getInstance().set(
            createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT),
            new Credentials("default", apiKey)
        );
    }
}

/**
 * 删除默认服务商的 API Key
 */
public void deleteDefaultApiKey() {
    PasswordSafe.getInstance().set(
        createCredentialAttributes(PASSWORD_SAFE_KEY_DEFAULT),
        null
    );
}
```

#### 列表服务商的密钥管理

```java
/**
 * 获取指定 ProviderConfig 的 API Key
 * 
 * @param uuid 提供商配置的 UUID
 * @return API Key，如果不存在则返回 null
 */
@Nullable
public static String getApiKey(@Nullable String uuid) {
    if (uuid == null || uuid.trim().isEmpty()) {
        return null;
    }

    Credentials credentials = PasswordSafe.getInstance().get(
        createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid)
    );
    return credentials != null ? credentials.getPasswordAsString() : null;
}

/**
 * 设置指定 ProviderConfig 的 API Key
 * 
 * @param uuid   提供商配置的 UUID
 * @param apiKey API 密钥，如果为 null 或空字符串则删除已存储的密钥
 */
public static void setApiKey(@Nullable String uuid, @Nullable String apiKey) {
    if (uuid == null || uuid.trim().isEmpty()) {
        return;
    }

    if (apiKey == null || apiKey.trim().isEmpty()) {
        // 删除密钥
        PasswordSafe.getInstance().set(
            createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid),
            null
        );
    } else {
        // 存储密钥
        PasswordSafe.getInstance().set(
            createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid),
            new Credentials(uuid, apiKey)
        );
    }
}

/**
 * 删除指定 ProviderConfig 的 API Key
 * 
 * @param uuid 提供商配置的 UUID
 */
public static void deleteApiKey(@Nullable String uuid) {
    if (uuid == null || uuid.trim().isEmpty()) {
        return;
    }

    PasswordSafe.getInstance().set(
        createCredentialAttributes(PASSWORD_SAFE_KEY_PREFIX + uuid),
        null
    );
}
```

### 4. 使用示例

#### 在 UI 面板中保存配置

```java
// JavaDocSettingsPanel.java
@NotNull
public SettingsState getSettings() {
    SettingsState settings = new SettingsState();
    
    // ... 设置其他配置项
    
    // 将 API Key 存储到 PasswordSafe
    String apiKey = new String(apiKeyField.getPassword()).trim();
    settings.setDefaultApiKey(apiKey);
    
    return settings;
}
```

#### 在 UI 面板中加载配置

```java
// JavaDocSettingsPanel.java
public void loadSettings(@NotNull SettingsState settings) {
    // ... 加载其他配置项
    
    // 从 PasswordSafe 读取 API Key
    String apiKey = settings.getDefaultApiKey();
    apiKeyField.setText(apiKey != null ? apiKey : "");
}
```

#### 在服务提供商中使用

```java
// AICompatibleProvider.java
private String sendRequestWithBody(...) throws AIServiceException {
    try {
        // 检查 API Key 配置
        if (requiresApiKey()) {
            String apiKey = settings.getDefaultApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new AIServiceException(
                    "API Key is required but not configured",
                    AIServiceException.ErrorCode.CONFIGURATION_ERROR
                );
            }
        }
        
        // 使用 HttpRequests 发送请求
        String responseBody = HttpRequests.post(url, "application/json")
            .tuner(connection -> {
                // 设置 Authorization 头
                if (requiresApiKey()) {
                    String apiKey = settings.getDefaultApiKey();
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                }
            })
            .connect(request -> {
                request.write(requestBody);
                return request.readString();
            });
        
        return responseBody;
    } catch (Exception e) {
        // 错误处理
    }
}
```

#### 创建服务提供商时读取密钥

```java
// AIServiceFactory.java
public static AIServiceProvider createProvider(@NotNull SettingsState.ProviderConfig config) {
    try {
        // 创建临时的 SettingsState 用于创建提供商实例
        SettingsState tempSettings = new SettingsState();
        tempSettings.providerType = config.providerType;
        tempSettings.modelName = config.modelName;
        tempSettings.baseUrl = config.baseUrl;
        tempSettings.configurationVerified = config.configurationVerified;
        
        // 从 PasswordSafe 获取 API Key 并设置到临时配置中
        String apiKey = SettingsState.getApiKey(config.uuid);
        tempSettings.setDefaultApiKey(apiKey);

        return providerClass.getDeclaredConstructor(SettingsState.class)
            .newInstance(tempSettings);
    } catch (Exception e) {
        LOG.error("Failed to create provider", e);
        return null;
    }
}
```

#### 添加提供商到可用列表

```java
// JavaDocSettingsPanel.java
private void addToAvailableProviders() {
    SettingsState settings = SettingsState.getInstance();
    SettingsState currentSettings = getSettings();
    
    // 创建提供商配置（不包含 apiKey）
    SettingsState.ProviderConfig providerConfig = new SettingsState.ProviderConfig(
        currentSettings.providerType,
        currentSettings.modelName,
        currentSettings.baseUrl,
        true
    );
    
    // 将 API Key 存储到 PasswordSafe
    String apiKey = currentSettings.getDefaultApiKey();
    if (apiKey != null && !apiKey.trim().isEmpty()) {
        SettingsState.setApiKey(providerConfig.uuid, apiKey);
    }

    settings.availableProviders.add(providerConfig);
}
```

## 关键修改点总结

### 修改的文件

1. **SettingsState.java**
    - 添加 PasswordSafe 导入和常量
    - 移除 `apiKey` 字段
    - 为 `ProviderConfig` 添加 `uuid` 字段
    - 添加密钥管理方法
    - 更新 `isValid()` 和 `resetToDefaults()` 方法

2. **JavaDocSettingsPanel.java**
    - 更新 `getSettings()` 使用 `setDefaultApiKey()`
    - 更新 `loadSettings()` 使用 `getDefaultApiKey()`
    - 更新测试连接和刷新模型的代码
    - 更新配置列表管理方法

3. **JavaDocSettingsConfigurable.java**
    - 更新 `isModified()` 使用 PasswordSafe 比较
    - 更新 `apply()` 方法
    - 更新 `validateSettings()` 方法

4. **AIServiceFactory.java**
    - 更新 `createProvider(ProviderConfig)` 方法

5. **CustomProvider.java**
    - 更新配置验证方法

6. **AICompatibleProvider.java**
    - 更新请求发送方法中的密钥获取

### 代码统计

- 修改文件：6 个
- 新增方法：7 个（密钥管理相关）
- 修改方法：约 15 处
- 无 Lint 错误

## 使用注意事项

### 1. 密钥生命周期

- **存储时机**：在用户保存配置时自动存储
- **读取时机**：在创建服务提供商实例时读取
- **删除时机**：
    - 用户清空 API Key 字段时
    - 重置配置时
    - 删除提供商配置时（需手动实现）

### 2. 错误处理

```java
// 获取密钥时的防御性编程
String apiKey = settings.getDefaultApiKey();
if (apiKey == null || apiKey.trim().isEmpty()) {
    // 处理密钥不存在的情况
    throw new ConfigurationException("API Key is required");
}
```

### 3. 配置验证

```java
public boolean isValid() {
    // 检查必需字段
    if (providerType == null || modelName == null || baseUrl == null) {
        return false;
    }

    // 检查是否需要 API Key
    if (!requiresApiKey()) {
        return true;
    }

    // 从 PasswordSafe 读取并验证
    String apiKey = getDefaultApiKey();
    return apiKey != null && !apiKey.trim().isEmpty();
}
```

### 4. 配置比较

```java
// 比较 API Key 时需要从 PasswordSafe 读取
String currentApiKey = currentSettings.getDefaultApiKey();
String panelApiKey = panelSettings.getDefaultApiKey();

if (currentApiKey == null && panelApiKey != null) {
    return true; // 配置已修改
}
if (currentApiKey != null && !currentApiKey.equals(panelApiKey)) {
    return true; // 配置已修改
}
```

## 测试建议

### 1. 功能测试

- ✅ 验证密钥能够正确保存到 PasswordSafe
- ✅ 验证密钥能够正确读取
- ✅ 验证密钥能够正确删除
- ✅ 验证不同提供商的密钥不会混淆
- ✅ 验证配置保存和加载的完整性

### 2. 安全测试

- ✅ 验证 XML 配置文件中不包含明文密钥
- ✅ 验证密钥存储在操作系统的安全存储中
- ✅ 验证多个实例之间密钥隔离

### 3. 兼容性测试

- ✅ 在 macOS 上测试（Keychain）
- ✅ 在 Windows 上测试（Credential Store）
- ✅ 在 Linux 上测试（Secret Service）

### 4. 边界测试

- ✅ 空密钥处理
- ✅ 特殊字符密钥
- ✅ 超长密钥
- ✅ 并发访问

## 最佳实践

### 1. 安全性

```java
// ✅ 好的做法：使用 char[] 读取密码
char[] password = apiKeyField.getPassword();
String apiKey = new String(password);
// 使用完后清空
Arrays.fill(password, '\0');

// ❌ 不好的做法：直接使用 getText()
String apiKey = apiKeyField.getText();
```

### 2. 性能优化

```java
// ✅ 好的做法：缓存读取的密钥（在同一个操作流程中）
String apiKey = settings.getDefaultApiKey();
validateApiKey(apiKey);
useApiKey(apiKey);

// ❌ 不好的做法：多次读取
validateApiKey(settings.getDefaultApiKey());
useApiKey(settings.getDefaultApiKey());
```

### 3. 错误处理

```java
// ✅ 好的做法：提供清晰的错误信息
public String getDefaultApiKey() {
    try {
        Credentials credentials = PasswordSafe.getInstance().get(...);
        return credentials != null ? credentials.getPasswordAsString() : null;
    } catch (Exception e) {
        LOG.error("Failed to retrieve API Key from PasswordSafe", e);
        return null;
    }
}
```

### 4. 日志记录

```java
// ✅ 好的做法：脱敏输出
LOG.debug("API Key: " + maskApiKey(apiKey));

// ❌ 不好的做法：直接输出密钥
LOG.debug("API Key: " + apiKey);

// 脱敏方法
private String maskApiKey(String apiKey) {
    if (apiKey == null || apiKey.length() < 8) {
        return "***";
    }
    return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
}
```

## 参考资料

### IntelliJ Platform SDK 文档

- [Persisting Sensitive Data](https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html)
- [PasswordSafe API](https://github.com/JetBrains/intellij-community/blob/master/platform/credential-store/src/PasswordSafe.kt)

### 相关类

- `com.intellij.ide.passwordSafe.PasswordSafe`
- `com.intellij.credentialStore.CredentialAttributes`
- `com.intellij.credentialStore.CredentialAttributesKt`
- `com.intellij.credentialStore.Credentials`

### 操作系统密钥存储

- **macOS**: [Keychain Services](https://developer.apple.com/documentation/security/keychain_services)
- **Windows**: [Windows Credential Manager](https://docs.microsoft.com/en-us/windows/win32/secauthn/credential-manager)
- **Linux**: [Secret Service API](https://specifications.freedesktop.org/secret-service/)

## 版本历史

| 版本    | 日期         | 说明                      |
|-------|------------|-------------------------|
| 1.0.0 | 2025-11-05 | 初始版本，完成 PasswordSafe 集成 |


