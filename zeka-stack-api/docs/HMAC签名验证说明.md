# HMAC 请求签名验证说明

## 概述

反馈接口已实现 HMAC-SHA256 请求签名验证，用于防止请求被伪造和重放攻击。

## 签名方案

### 1. 请求头

每次请求需要包含以下 5 个请求头：

- `X-Client-Id`: 客户端标识（例如：`idea-plugin`）
- `X-Timestamp`: Unix 时间戳（秒），例如：`1735060000`
- `X-Nonce`: 随机串（每次请求都不同），例如：`550e8400-e29b-41d4-a716-446655440000`
- `X-Body-SHA256`: 请求体的 SHA256（十六进制），没有 body 则是空串的 SHA256
- `X-Signature`: 签名字符串（Base64 编码）

### 2. 签名算法

签名串（canonical string）使用 `\n` 拼接：

```
METHOD \n
PATH_WITH_QUERY \n
BODY_SHA256_HEX \n
TIMESTAMP \n
NONCE
```

签名计算：

```
signature = Base64( HMAC_SHA256(secret, canonicalString UTF-8) )
```

### 3. 示例

假设：

- Method: `POST`
- Path: `/api/plugin/feedback/discussion`
- Body: `{"title":"test"}`
- Timestamp: `1735060000`
- Nonce: `550e8400-e29b-41d4-a716-446655440000`
- Secret: `my-secret-key`

计算步骤：

1. 计算 Body SHA256：
   ```
   bodySha256 = sha256("{\"title\":\"test\"}") = "a1b2c3d4..."
   ```

2. 构建规范字符串：
   ```
   POST\n
   /api/plugin/feedback/discussion\n
   a1b2c3d4...\n
   1735060000\n
   550e8400-e29b-41d4-a716-446655440000
   ```

3. 计算 HMAC-SHA256 签名：
   ```
   signature = Base64(HMAC_SHA256("my-secret-key", canonicalString))
   ```

## 服务端配置

### 1. 配置文件

在 `application.yml` 中配置：

```yaml
feedback:
  signature:
    # 是否启用签名验证（生产环境必须启用）
    enabled: true
    # 客户端 ID 和 Secret 的映射
    # 每个插件使用不同的 Secret，与客户端代码中的 Secret 对应
    clients:
      # IntelliAI Javadoc 插件
      dev.dong4j.zeka.stack.idea.plugin: aij-secret-2024-zeka-stack-javadoc-plugin
      # IntelliAI Nacos 插件
      dev.dong4j.zeka.stack.idea.plugin.nacos: aij-secret-2024-zeka-stack-nacos-plugin
      # IntelliAI Tracer 插件
      dev.dong4j.zeka.stack.idea.plugin.workflow: aij-secret-2024-zeka-stack-tracer-plugin
      # IntelliAI Changelog 插件
      dev.dong4j.zeka.stack.idea.plugin.changelog: aij-secret-2024-zeka-stack-changelog-plugin
      # IntelliAI Engine 插件
      dev.dong4j.zeka.stack.idea.plugin.common.ai: aij-secret-2024-zeka-stack-engine-plugin
```

**注意**：

- 每个插件使用自己的 pluginId 作为 clientId
- 每个插件使用不同的硬编码 Secret，与服务端配置对应
- Secret 硬编码在客户端和服务端代码中，用户无需配置

### 2. Secret 配置

**重要**：Secret 已硬编码在客户端和服务端代码中，每个插件使用不同的 Secret。用户无需配置任何环境变量。

### 3. 禁用验证（仅开发环境）

在开发环境中可以临时禁用签名验证：

```yaml
feedback:
  signature:
    enabled: false
```

## 客户端配置

客户端代码会自动使用 `RequestSigner` 工具类生成签名头，无需手动处理。

`RequestSigner` 会根据插件的 `pluginId` 自动查找对应的 Secret，用户无需配置任何环境变量。

## 验证规则

服务端会进行以下验证：

1. **客户端 ID 验证**：检查客户端 ID 是否在配置中存在
2. **时间戳验证**：时间戳必须在当前时间的 ±300 秒内（防重放）
3. **Nonce 验证**：每个 nonce 只能使用一次，5 分钟后自动过期（防重放）
4. **请求体验证**：如果提供了 `X-Body-SHA256`，会验证请求体的 SHA256 是否匹配
5. **签名验证**：重新计算签名，使用常量时间比较，确保签名一致

## 错误处理

如果验证失败，服务端会返回 `401 Unauthorized` 响应：

```json
{
  "success": false,
  "error": "Unauthorized: <错误原因>"
}
```

常见错误原因：

- `Missing required signature headers`: 缺少必需的签名头
- `Unknown client ID`: 客户端 ID 不存在
- `Invalid or duplicate nonce`: Nonce 无效或已使用过
- `Timestamp out of tolerance`: 时间戳超出允许范围
- `Body SHA256 mismatch`: 请求体 SHA256 不匹配
- `Invalid signature`: 签名验证失败

## 安全注意事项

1. **Secret 管理**：
    - 生产环境必须使用强随机 Secret（至少 32 字节）
    - Secret 应该通过环境变量或密钥管理服务配置
    - 不要将 Secret 提交到代码仓库

2. **HTTPS 要求**：
    - 签名方案必须在 HTTPS 环境下使用
    - 在 HTTP 环境下，Secret 可能被中间人攻击获取

3. **时间同步**：
    - 客户端和服务端的时间必须同步（误差在 ±300 秒内）
    - 建议使用 NTP 同步时间

4. **Nonce 存储**：
    - 当前实现使用内存缓存（适合单机部署）
    - 如果需要分布式部署，应使用 Redis 等分布式缓存

## 测试

### 使用 curl 测试（需要手动计算签名）

```bash
# 1. 计算 Body SHA256
echo -n '{"title":"test"}' | sha256sum

# 2. 构建规范字符串并计算签名（需要编写脚本）

# 3. 发送请求（使用插件的 pluginId 作为 clientId）
curl -X POST https://zekastack.dong4j.site/api/plugin/feedback/discussion \
  -H "Content-Type: application/json" \
  -H "X-Client-Id: dev.dong4j.zeka.stack.idea.plugin" \
  -H "X-Timestamp: 1735060000" \
  -H "X-Nonce: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Body-SHA256: <计算的SHA256>" \
  -H "X-Signature: <计算的签名>" \
  -d '{"title":"test"}'
```

**注意**：clientId 应该使用插件的 pluginId，例如：

- `dev.dong4j.zeka.stack.idea.plugin` (IntelliAI Javadoc)
- `dev.dong4j.zeka.stack.idea.plugin.nacos` (IntelliAI Nacos)
- `dev.dong4j.zeka.stack.idea.plugin.workflow` (IntelliAI Tracer)
- `dev.dong4j.zeka.stack.idea.plugin.changelog` (IntelliAI Changelog)
- `dev.dong4j.zeka.stack.idea.plugin.common.ai` (IntelliAI Engine)

### 使用 IDEA 插件测试

IDEA 插件会自动生成签名头，直接使用即可。

## 故障排查

1. **验证失败**：
    - 检查 Secret 是否一致
    - 检查时间是否同步
    - 检查请求头是否正确
    - 查看服务端日志获取详细错误信息

2. **Nonce 重复**：
    - 确保每次请求使用不同的 nonce（UUID）
    - 等待 5 分钟后重试（nonce 会自动过期）

3. **时间戳错误**：
    - 检查客户端和服务端时间是否同步
    - 调整系统时间或使用 NTP 同步
