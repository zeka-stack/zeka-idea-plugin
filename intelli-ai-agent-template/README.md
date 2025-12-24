# IntelliAI Agent Template

这是一个模板项目，展示如何开发一个 JAR 包，将非标准协议（如 WebSocket、gRPC）的 AI 服务转换为标准的 OpenAI API，供 IntelliAI Engine 插件集成使用。

## 📖 项目说明

本项目展示了以下核心功能：

- ✅ 实现标准的 OpenAI API 接口（`/health`, `/v1/models`, `/v1/chat/completions`）
- ✅ 固定监听 8765 端口
- ✅ 协议转换层设计（原始协议 ↔ OpenAI API）
- ✅ 支持流式和非流式响应
- ✅ 可执行 JAR 包打包配置

## 🏗️ 项目结构

```
intelli-ai-agent-template/
├── pom.xml                          # Maven 配置
├── README.md                        # 本文件
└── src/
    └── main/
        ├── java/
        │   └── dev/
        │       └── dong4j/
        │           └── zeka/
        │               └── stack/
        │                   └── agent/
        │                       ├── ServerLauncher.java          # 主启动类
        │                       ├── api/
        │                       │   └── OpenAiApiServer.java    # OpenAI API 服务器
        │                       └── client/
        │                           └── YourAIServiceClient.java # 原始协议客户端（示例）
        └── resources/
            └── META-INF/
                └── MANIFEST.MF                         # 清单文件（Maven 会自动生成）
```

## 🚀 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6+

### 2. 构建项目

```bash
cd intelli-ai-agent-template
mvn clean package
```

构建完成后，会在 `target/` 目录下生成：

- `intelli-ai-agent-1.0.0.jar` - 包含所有依赖的可执行 JAR

### 3. 运行服务

```bash
java -jar target/intelli-ai-agent-1.0.0.jar
```

服务启动后会监听 `127.0.0.1:8765`。

### 4. 测试服务

#### 健康检查

```bash
curl http://127.0.0.1:8765/health
```

预期响应：

```json
{"status":"ok"}
```

#### 模型列表

```bash
curl http://127.0.0.1:8765/v1/models
```

#### 聊天完成（非流式）

```bash
curl -X POST http://127.0.0.1:8765/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "your-model-id",
    "messages": [
      {"role": "user", "content": "Hello"}
    ],
    "stream": false
  }'
```

#### 聊天完成（流式）

```bash
curl -X POST http://127.0.0.1:8765/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "your-model-id",
    "messages": [
      {"role": "user", "content": "Hello"}
    ],
    "stream": true
  }'
```

## 📝 开发指南

### 1. 实现原始协议客户端

本项目中的 `YourAIServiceClient.java` 是一个示例接口，展示了如何封装原始协议的客户端。

**你需要根据实际的原始协议实现以下方法：**

- `connect()` - 连接到原始 AI 服务
- `ask(String question)` - 发送非流式请求
- `askStream(String question, Consumer<String> onChunk, Runnable onComplete)` - 发送流式请求
- `disconnect()` - 断开连接

### 2. 协议转换逻辑

`OpenAiApiServer.java` 中的 `ChatCompletionsHandler` 负责：

1. 接收 OpenAI 格式的请求
2. 转换为原始协议的请求格式
3. 调用原始协议客户端
4. 将原始协议的响应转换为 OpenAI 格式

### 3. 添加依赖

根据你使用的原始协议，在 `pom.xml` 中添加相应的依赖：

- **WebSocket**: `Java-WebSocket`
- **gRPC**: `grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`
- **HTTP**: `okhttp`
- **其他协议**: 根据实际情况添加

## 📚 详细文档

请参考 [非标准AI服务集成jar开发指南](../intelli-ai-engine/site/docs/非标准AI服务集成jar开发指南.md) 了解完整的开发规范和最佳实践。

## ⚠️ 注意事项

### 1. 端口固定

- 服务必须监听 **8765** 端口（固定端口）
- Engine 会固定使用此端口访问服务

### 2. 主类配置

- 主类必须在 `pom.xml` 的 `maven-shade-plugin` 中正确配置
- 确保 `MANIFEST.MF` 中包含 `Main-Class` 属性

### 3. 健康检查

- 必须实现 `/health` 端点
- Engine 通过此端点检查服务状态

### 4. 错误处理

- 实现完善的错误处理机制
- 返回标准的错误响应格式

### 5. 资源清理

- 实现 `shutdown` 钩子
- 确保服务关闭时正确清理资源

## 🔧 版本更新机制

### 版本检查端点

你的下载服务器需要提供 `/version` 端点：

```
GET https://your-server.com/version
响应: intelli-ai-agent-1.0.0.jar
```

### JAR 下载端点

```
GET https://your-server.com/intelli-ai-agent-1.0.0.jar
响应: [JAR 文件二进制数据]
```

### Engine 配置

在 Engine 设置页面配置：

- **下载地址（Base URL）：** `https://your-server.com/agent`
- Engine 会自动检查版本、下载并启动服务

## 📄 许可证

本项目仅供学习和参考使用。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**开始开发你的 AI Agent Service！** 🎉

