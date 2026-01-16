# GitHub Discussions API 参数整理

## 概述

GitHub Discussions API 主要通过 **GraphQL** 提供接口来创建和管理讨论。REST API 仅支持团队讨论（Team Discussions），对于仓库讨论（Repository
Discussions）需要使用 GraphQL API。

## 目标仓库信息

- **仓库**: [zeka-stack/zeka-idea-plugin](https://github.com/zeka-stack/zeka-idea-plugin)
- **Discussions 地址**: https://github.com/zeka-stack/zeka-idea-plugin/discussions
- **已启用的讨论类别**:
    - 📣 Announcements（公告）
    - 💬 General（一般讨论）
    - 💡 Ideas（想法建议）
    - 🗳️ Polls（投票）
    - 🙏 Q&A（问答）
    - 🙌 Show and tell（展示）

## 创建讨论（Create Discussion）

### GraphQL Mutation

```graphql
mutation CreateDiscussion($repositoryId: ID!, $categoryId: ID!, $title: String!, $body: String!) {
  createDiscussion(input: {
    repositoryId: $repositoryId
    categoryId: $categoryId
    title: $title
    body: $body
  }) {
    discussion {
      id
      number
      url
      title
      body
      createdAt
      author {
        login
      }
      category {
        id
        name
      }
    }
  }
}
```

### 必需参数

#### 1. `repositoryId` (ID!)

- **类型**: ID（必需）
- **说明**: 仓库的全局 ID
- **获取方式**:
    - 通过 GraphQL 查询获取：`repository(owner: "OWNER", name: "REPO") { id }`
    - 或通过 REST API 获取仓库信息后转换为 ID

#### 2. `categoryId` (ID!)

- **类型**: ID（必需）
- **说明**: 讨论类别的 ID（如 "General"、"Ideas"、"Q&A" 等）
- **获取方式**:
    - 通过 GraphQL 查询获取：`repository(owner: "OWNER", name: "REPO") { discussionCategories { nodes { id, name } } }`

#### 3. `title` (String!)

- **类型**: String（必需）
- **说明**: 讨论的标题
- **限制**:
    - 不能为空
    - 建议长度不超过 255 个字符

#### 4. `body` (String!)

- **类型**: String（必需）
- **说明**: 讨论的内容（支持 Markdown）
- **限制**:
    - 不能为空

### 查询仓库 ID 和类别 ID

#### GraphQL 查询

```graphql
query GetRepositoryInfo($owner: String!, $name: String!) {
  repository(owner: $owner, name: $name) {
    id
    discussionCategories(first: 10) {
      nodes {
        id
        name
        description
        emoji
      }
    }
  }
}
```

#### 查询参数

- `owner`: `"zeka-stack"`
- `name`: `"zeka-idea-plugin"`

#### cURL 命令示例

```bash
curl -X POST https://api.github.com/graphql \
  -H "Authorization: Bearer YOUR_GITHUB_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query GetRepositoryInfo($owner: String!, $name: String!) { repository(owner: $owner, name: $name) { id discussionCategories(first: 10) { nodes { id name description emoji } } } }",
    "variables": {
      "owner": "zeka-stack",
      "name": "zeka-idea-plugin"
    }
  }'
```

执行此命令后会返回：

- `repository.id` - 仓库的全局 ID（如 `R_xxxxxxxxxxxxx`）
- `discussionCategories.nodes[].id` - 每个类别的 ID（如 `DIC_kwDOxxxxxxxxx`）
- `discussionCategories.nodes[].name` - 类别名称

### 返回字段

创建讨论成功后会返回以下字段：

- `discussion.id` - 讨论的全局 ID
- `discussion.number` - 讨论的编号
- `discussion.url` - 讨论的 URL
- `discussion.title` - 标题
- `discussion.body` - 内容
- `discussion.createdAt` - 创建时间
- `discussion.author.login` - 作者用户名
- `discussion.category.id` - 类别 ID
- `discussion.category.name` - 类别名称

## 认证要求

### 访问令牌（Access Token）

- **必需范围**:
    - 私有仓库: `repo` 范围
    - 公共仓库: `public_repo` 范围
    - 或者使用 Fine-grained personal access token，需要 `Discussions: Write` 权限

### HTTP 请求头

```
Authorization: Bearer YOUR_GITHUB_TOKEN
Content-Type: application/json
Accept: application/vnd.github+json
```

## GraphQL 端点

```
POST https://api.github.com/graphql
```

## 示例请求

### cURL 示例

```bash
curl -X POST https://api.github.com/graphql \
  -H "Authorization: Bearer YOUR_GITHUB_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mutation CreateDiscussion($repositoryId: ID!, $categoryId: ID!, $title: String!, $body: String!) { createDiscussion(input: { repositoryId: $repositoryId, categoryId: $categoryId, title: $title, body: $body }) { discussion { id number url title } } }",
    "variables": {
      "repositoryId": "R_xxxxxxxxxxxxx",
      "categoryId": "DIC_kwDOxxxxxxxxx",
      "title": "插件反馈：功能建议",
      "body": "## 问题描述\n\n这里是详细的问题描述...\n\n## 期望行为\n\n期望的行为...\n\n## 环境信息\n- 插件版本: 1.0.0\n- IDEA 版本: 2023.3"
    }
  }'
```

### Java 示例（使用 Spring Boot + OkHttp）

```java
public class GitHubDiscussionsClient {
    private static final String GITHUB_GRAPHQL_ENDPOINT = "https://api.github.com/graphql";

    public CreateDiscussionResponse createDiscussion(
            String token,
            String repositoryId,
            String categoryId,
            String title,
            String body) throws IOException {

        String query = """
            mutation CreateDiscussion($repositoryId: ID!, $categoryId: ID!, $title: String!, $body: String!) {
              createDiscussion(input: {
                repositoryId: $repositoryId
                categoryId: $categoryId
                title: $title
                body: $body
              }) {
                discussion {
                  id
                  number
                  url
                  title
                  body
                  createdAt
                  author {
                    login
                  }
                  category {
                    id
                    name
                  }
                }
              }
            }
            """;

        Map<String, Object> variables = Map.of(
            "repositoryId", repositoryId,
            "categoryId", categoryId,
            "title", title,
            "body", body
        );

        Map<String, Object> requestBody = Map.of(
            "query", query,
            "variables", variables
        );

        // 使用 HTTP 客户端发送请求
        // ...
    }
}
```

## 错误处理

### 常见错误

1. **认证失败**
    - HTTP 401 Unauthorized
    - 错误信息: `"Bad credentials"`

2. **权限不足**
    - HTTP 403 Forbidden
    - 错误信息: `"Resource not accessible by integration"`

3. **仓库不存在**
    - GraphQL 错误: `"Could not resolve to a Repository"`

4. **类别不存在**
    - GraphQL 错误: `"Could not resolve to a DiscussionCategory"`

5. **标题或内容为空**
    - GraphQL 错误: `"Title is required"` 或 `"Body is required"`

### 错误响应格式

```json
{
  "errors": [
    {
      "type": "UNPROCESSABLE",
      "path": ["createDiscussion"],
      "locations": [{"line": 2, "column": 3}],
      "message": "Title is required"
    }
  ],
  "data": {
    "createDiscussion": null
  }
}
```

## 注意事项

1. **GraphQL vs REST**: GitHub Discussions API 主要通过 GraphQL 提供，REST API 仅支持团队讨论
2. **仓库必须启用 Discussions**: 目标仓库必须在设置中启用 Discussions 功能
3. **类别必须先存在**: 必须使用仓库中已配置的讨论类别 ID
4. **速率限制**: GitHub API 有速率限制，认证用户的限制更高
5. **内容格式**: `body` 字段支持 Markdown 格式，可以包含代码块、链接等

## 相关资源

- [GitHub GraphQL API 文档](https://docs.github.com/en/graphql)
- [GitHub Discussions API 文档](https://docs.github.com/en/graphql/guides/using-the-graphql-api-for-discussions)
- [GitHub API 速率限制](https://docs.github.com/en/rest/overview/resources-in-the-rest-api#rate-limiting)

