# API 使用示例

## 使用 curl 测试

### 提交 Bug 反馈

```bash
curl -X POST http://localhost:8080/api/plugin/feedback/discussion \
  -H "Content-Type: application/json" \
  -d '{
    "title": "插件在 macOS 上崩溃",
    "content": "当我在 macOS 上使用插件时，点击某个按钮后插件就会崩溃。\n\n**复现步骤：**\n1. 打开 IDEA\n2. 点击插件按钮\n3. 插件崩溃",
    "type": "bug",
    "category": "general",
    "userInfo": {
      "name": "张三",
      "email": "zhangsan@example.com",
      "githubUsername": "zhangsan",
      "pluginVersion": "1.0.0",
      "ideaVersion": "2023.3",
      "os": "macOS 14.0"
    }
  }'
```

### 提交功能建议

```bash
curl -X POST http://localhost:8080/api/plugin/feedback/discussion \
  -H "Content-Type: application/json" \
  -d '{
    "title": "希望支持批量生成 Javadoc",
    "content": "希望能添加批量生成 Javadoc 的功能，可以一次性为整个项目或选中的多个文件生成文档。",
    "type": "feature",
    "category": "ideas",
    "userInfo": {
      "githubUsername": "lisi",
      "pluginVersion": "1.0.0",
      "ideaVersion": "2023.3"
    }
  }'
```

### 提交使用问题

```bash
curl -X POST http://localhost:8080/api/plugin/feedback/discussion \
  -H "Content-Type: application/json" \
  -d '{
    "title": "如何配置 API Key？",
    "content": "我在设置页面找不到 API Key 的配置入口，请问应该在哪里配置？",
    "type": "question",
    "category": "qa",
    "userInfo": {
      "githubUsername": "wangwu",
      "pluginVersion": "1.0.0",
      "ideaVersion": "2023.3"
    }
  }'
```

## 使用 Java（插件端示例）

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FeedbackClient {
    private static final String FEEDBACK_API_URL = "http://localhost:8080/api/plugin/feedback/discussion";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FeedbackClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public FeedbackResponse submitFeedback(FeedbackRequest request) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(FEEDBACK_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        return objectMapper.readValue(response.body(), FeedbackResponse.class);
    }

    // 使用示例
    public void example() throws Exception {
        FeedbackRequest request = new FeedbackRequest();
        request.setTitle("功能建议");
        request.setContent("希望添加某个功能");
        request.setType(FeedbackRequest.FeedbackType.FEATURE);
        request.setCategory(FeedbackRequest.DiscussionCategory.IDEAS);

        FeedbackRequest.UserInfo userInfo = new FeedbackRequest.UserInfo();
        userInfo.setGithubUsername("username");
        userInfo.setPluginVersion("1.0.0");
        userInfo.setIdeaVersion("2023.3");
        request.setUserInfo(userInfo);

        FeedbackResponse response = submitFeedback(request);
        if (response.getSuccess()) {
            System.out.println("反馈已提交: " + response.getDiscussion().getUrl());
        } else {
            System.err.println("提交失败: " + response.getError());
        }
    }
}
```

## 响应示例

### 成功响应

```json
{
  "success": true,
  "discussion": {
    "id": "D_kwDOxxxxxxxxx",
    "number": 1,
    "url": "https://github.com/zeka-stack/zeka-idea-plugin/discussions/1",
    "title": "[插件反馈] Bug 报告: 插件在 macOS 上崩溃"
  },
  "message": "反馈已成功提交"
}
```

### 错误响应（参数验证失败）

```json
{
  "success": false,
  "error": "参数验证失败: 标题不能为空, 内容不能为空"
}
```

### 错误响应（服务器错误）

```json
{
  "success": false,
  "error": "提交反馈失败: GitHub API request failed with status 401: Bad credentials"
}
```

## 在 GitHub Discussions 中生成的讨论格式

当用户提交反馈后，在 GitHub Discussions 中会生成如下格式的讨论：

**标题**: `[插件反馈] Bug 报告: 插件在 macOS 上崩溃`

**内容**:

```markdown
**提交人**: @zhangsan

## 反馈内容

当我在 macOS 上使用插件时，点击某个按钮后插件就会崩溃。

**复现步骤：**
1. 打开 IDEA
2. 点击插件按钮
3. 插件崩溃

---

## 提交人信息

- **姓名**: 张三
- **邮箱**: zhangsan@example.com
- **GitHub**: @zhangsan
- **插件版本**: 1.0.0
- **IDEA 版本**: 2023.3
- **操作系统**: macOS 14.0
- **反馈类型**: Bug 报告
- **提交时间**: 2024-01-01 12:00:00
```

## 注意事项

1. **GitHub 用户名**: 如果提供了 `githubUsername`，系统会在讨论开头使用 `@username` 提及用户，GitHub 会自动通知该用户。

2. **参数验证**: 所有必填字段都会进行验证，如果验证失败会返回详细的错误信息。

3. **Markdown 转义**: 用户输入的内容会进行适当的 Markdown 转义，防止格式混乱，但用户仍可以使用 Markdown 语法。

4. **速率限制**: GitHub API 有速率限制，建议对请求进行适当的速率限制。
