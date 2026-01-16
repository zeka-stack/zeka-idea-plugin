# Zeka Stack API

Zeka Stack 框架的 REST API 服务，为 IDEA 插件提供后端接口支持。

## 📖 项目说明

本项目提供以下核心功能：

- ✅ 提供 AI 服务商模型列表查询接口
- ✅ 支持智谱 AI (Zhipu) 模型列表获取
- ✅ RESTful API 设计
- ✅ 统一的响应格式

## 🏗️ 项目结构

```
zeka-stack-api/
├── build.gradle.kts              # Gradle 构建配置
├── settings.gradle.kts           # Gradle 设置
├── gradle.properties             # Gradle 属性
├── README.md                     # 项目说明
└── src/
    └── main/
        ├── java/
        │   └── dev/
        │       └── dong4j/
        │           └── zeka/
        │               └── stack/
        │                   └── api/
        │                       ├── ZekaStackApiApplication.java    # 主启动类
        │                       ├── controller/                     # 控制器层
        │                       │   └── ModelController.java        # 模型管理控制器
        │                       ├── service/                        # 服务层
        │                       │   └── ZhipuModelService.java      # 智谱 AI 模型服务
        │                       ├── dto/                            # 数据传输对象
        │                       │   └── ModelResponse.java          # 模型响应 DTO
        │                       └── config/                         # 配置类
        │                           ├── HttpClientConfig.java       # HTTP 客户端配置
        │                           ├── WebConfig.java              # Web 配置
        │                           └── ZhipuProperties.java        # 智谱 AI 配置属性
        └── resources/
            └── application.yml    # 应用配置文件
```

## 🚀 快速开始

### 1. 环境要求

- JDK 17 或更高版本
- Maven 3.6+

### 2. 构建项目

```bash
cd zeka-stack-api
mvn clean package
```

### 3. 运行服务

```bash
mvn spring-boot:run
```

或者：

```bash
java -jar target/zeka-stack-api-1.0.0.jar
```

服务启动后会监听 `http://localhost:8080/api`

### 4. 测试 API

#### 获取智谱 AI 模型列表

```bash
curl http://localhost:8080/api/v1/models/zhipu
```

响应示例：

```json
{
  "provider": "zhipu",
  "models": [
    "glm-4.6",
    "glm-4.5",
    "glm-4.5-air",
    "glm-4.5-x",
    "glm-4.5-airx",
    "glm-4.5-flash",
    "glm-4-plus",
    "glm-4-air-250414",
    "glm-4-airx",
    "glm-4-flashx",
    "glm-4-flashx-250414"
  ],
  "total": 11
}
```

#### 带 API Key 的请求（可选）

```bash
curl "http://localhost:8080/api/v1/models/zhipu?apiKey=your-api-key"
```

## 📝 API 文档

### GET /api/v1/models/zhipu

获取智谱 AI 可用模型列表

**请求参数：**

| 参数名    | 类型     | 必填 | 说明                             |
|--------|--------|----|--------------------------------|
| apiKey | String | 否  | 智谱 AI API Key，用于后续从 API 获取模型列表 |

**响应示例：**

```json
{
  "provider": "zhipu",
  "models": [
    "glm-4.6",
    "glm-4.5",
    ...
  ],
  "total": 11
}
```

## 🔧 配置说明

### application.yml

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: zeka-stack-api

zhipu:
  api:
    base-url: https://open.bigmodel.cn/api/paas/v4
    models-url: ${zhipu.api.base-url}/models
```

## 🛠️ 开发计划

- [ ] 实现从智谱 AI API 动态获取模型列表
- [ ] 添加其他 AI 服务商的模型列表接口（Claude、Gemini、Codex 等）
- [ ] 添加模型缓存机制
- [ ] 添加接口认证和安全控制
- [ ] 添加接口文档（Swagger/OpenAPI）

## 📄 许可证

本项目采用与其他 Zeka Stack 项目相同的许可证。

## 👥 贡献

欢迎提交 Issue 和 Pull Request！
