# Feedback Server

为 IntelliJ IDEA 插件提供反馈接口的后端服务，内部对接 GitHub Discussions API。

## 功能概述

- 提供 REST API 接口接收插件反馈
- 自动将反馈提交到 GitHub Discussions
- 支持多种反馈类型（Bug 报告、功能建议、使用问题等）
- 支持 GitHub 用户名 @ 提及功能

## 快速开始

### 1. 获取 GitHub 配置信息

使用提供的脚本获取仓库 ID 和类别 ID：

```bash
# Linux/macOS
cd scripts
./get-repo-info.sh YOUR_GITHUB_TOKEN

# Windows
cd scripts
get-repo-info.bat YOUR_GITHUB_TOKEN
```

脚本会返回：

- `repository.id` - 仓库的全局 ID（用于 `GITHUB_REPOSITORY_ID`）
- `discussionCategories.nodes[].id` - 各个类别的 ID（用于 `GITHUB_CATEGORY_ID_*`）

### 2. 配置环境变量

创建 `.env` 文件或设置环境变量：

```bash
# GitHub Token（必需）
GITHUB_TOKEN=your_github_token

# 仓库 ID（必需）
GITHUB_REPOSITORY_ID=repository_id_from_step_1

# 讨论类别 ID（必需，至少配置一个）
GITHUB_CATEGORY_ID_GENERAL=category_id_for_general
GITHUB_CATEGORY_ID_IDEAS=category_id_for_ideas
GITHUB_CATEGORY_ID_QA=category_id_for_qa
GITHUB_CATEGORY_ID_ANNOUNCEMENTS=category_id_for_announcements
```

或直接在 `application.yml` 中配置：

```yaml
github:
  token: your_github_token
  repository-id: your_repository_id
  repository: zeka-stack/zeka-idea-plugin
  category:
    general: category_id_for_general
    ideas: category_id_for_ideas
    qa: category_id_for_qa
```

### 3. 构建项目

```bash
mvn clean package
```

### 4. 启动服务

```bash
mvn spring-boot:run
```

或者：

```bash
java -jar target/feedback-server-1.0.0.jar
```

服务将在 `http://localhost:8080` 启动。

## API 文档

### 提交反馈

**端点**: `POST /api/plugin/feedback/discussion`

**请求体**:

```json
{
  "title": "反馈标题",
  "content": "反馈详细内容",
  "type": "bug",
  "category": "general",
  "userInfo": {
    "name": "用户姓名（可选）",
    "email": "user@example.com（可选）",
    "githubUsername": "github_username（可选，用于 @ 提及）",
    "pluginVersion": "1.0.0",
    "ideaVersion": "2023.3",
    "os": "macOS 14.0"
  },
  "metadata": {
    "clientId": "客户端唯一标识（可选）",
    "timestamp": 1704067200000
  }
}
```

### 提交反馈为 Issue

**端点**: `POST /api/plugin/feedback/issue`

**请求体**: 与 `/api/plugin/feedback/discussion` 相同

**响应**:

```json
{
  "success": true,
  "issue": {
    "id": "I_kwDOxxxxxxxxx",
    "number": 1,
    "url": "https://github.com/zeka-stack/zeka-idea-plugin/issues/1",
    "title": "[插件反馈] Bug 报告: 反馈标题"
  },
  "message": "反馈已成功提交"
}
```

**反馈类型 (`type`)**:

- `bug` - Bug 报告
- `feature` - 功能建议
- `question` - 使用问题
- `other` - 其他

**讨论类别 (`category`)**:

- `general` - 一般讨论
- `ideas` - 想法建议
- `qa` - 问答
- `announcements` - 公告

**响应**:

```json
{
  "success": true,
  "discussion": {
    "id": "D_kwDOxxxxxxxxx",
    "number": 1,
    "url": "https://github.com/zeka-stack/zeka-idea-plugin/discussions/1",
    "title": "[插件反馈] Bug 报告: 反馈标题"
  },
  "message": "反馈已成功提交"
}
```

**错误响应**:

```json
{
  "success": false,
  "error": "错误信息"
}
```

### 健康检查

**端点**: `GET /api/plugin/feedback/health`

**响应**: `OK`

## 技术栈

- Spring Boot 3.2.0
- Java 17
- OkHttp 4.12.0（GitHub GraphQL API 客户端）
- Jackson（JSON 处理）
- Lombok

## 项目结构

```
feedback-server/
├── src/main/java/dev/dong4j/zeka/stack/feedback/
│   ├── FeedbackServerApplication.java    # 主应用类
│   ├── controller/
│   │   └── FeedbackController.java       # REST 控制器
│   ├── service/
│   │   └── FeedbackService.java          # 业务逻辑
│   ├── client/
│   │   └── GitHubGraphQLClient.java      # GitHub GraphQL 客户端
│   ├── dto/
│   │   ├── FeedbackRequest.java          # 请求 DTO
│   │   └── FeedbackResponse.java         # 响应 DTO
│   ├── config/
│   │   ├── GitHubProperties.java         # GitHub 配置
│   │   ├── HttpClientConfig.java         # HTTP 客户端配置
│   │   └── WebConfig.java                # Web 配置
│   └── exception/
│       └── GlobalExceptionHandler.java   # 全局异常处理
├── src/main/resources/
│   └── application.yml                   # 应用配置
├── scripts/
│   ├── get-repo-info.sh                  # 获取仓库信息脚本（Linux/macOS）
│   └── get-repo-info.bat                 # 获取仓库信息脚本（Windows）
└── docs/
    ├── README.md                         # 文档
    ├── GitHub Discussions API 参数整理.md
    └── 用户身份识别方案.md
```

## 开发

### 运行测试

```bash
mvn test
```

### 构建 JAR

```bash
mvn clean package
```

构建产物位于 `target/feedback-server-1.0.0.jar`

### 运行 JAR

```bash
java -jar build/libs/feedback-server-1.0.0.jar
```

## 参考文档

- [GitHub Discussions API 参数整理](../feedback-server/docs/GitHub%20Discussions%20API%20参数整理.md)
- [用户身份识别方案](../feedback-server/docs/用户身份识别方案.md)
- [API 使用示例](../feedback-server/docs/API使用示例.md)

## 常见问题

### 1. 如何获取 GitHub Token？

访问 [GitHub Settings > Personal access tokens](https://github.com/settings/tokens) 创建新的 Token。

需要的权限：

- 对于公共仓库: `public_repo` 或 Fine-grained token 的 `Discussions: Write`
- 对于私有仓库: `repo` 或 Fine-grained token 的 `Discussions: Write`

### 2. 如何获取仓库 ID 和类别 ID？

使用项目提供的脚本：

```bash
cd scripts
./get-repo-info.sh YOUR_GITHUB_TOKEN
```

### 3. 服务启动失败？

检查环境变量是否配置正确，特别是：

- `GITHUB_TOKEN` - 不能为空
- `GITHUB_REPOSITORY_ID` - 不能为空
- 至少配置一个类别 ID（如 `GITHUB_CATEGORY_ID_GENERAL`）

### 4. API 返回 401 错误？

检查 GitHub Token 是否正确，以及是否具有足够的权限。
