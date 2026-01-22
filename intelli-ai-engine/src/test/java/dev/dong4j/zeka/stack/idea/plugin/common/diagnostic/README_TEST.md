# EngineFeedbackSubmitter 测试说明

## 概述

`EngineFeedbackSubmitterTest` 是一个集成测试类，用于测试在 IntelliJ IDEA 插件环境中提交 issues 的功能。

## 测试环境

- **测试框架**: JUnit 5 + IntelliJ Platform Test Framework
- **HTTP Mock**: MockWebServer (OkHttp)
- **Mock 框架**: Mockito

## 测试场景

### 1. 成功提交新 issue (`testSubmitNewIssueSuccess`)

测试成功提交新 issue 的完整流程：

- ✅ 创建 `IdeaLoggingEvent` mock 对象
- ✅ 配置 MockWebServer 返回成功响应
- ✅ 验证请求方法、路径、请求头
- ✅ 验证请求体包含必要字段
- ✅ 验证返回的 `SubmittedReportInfo` 状态为 `NEW_ISSUE`

### 2. 服务器错误处理 (`testSubmitIssueServerError`)

测试服务器返回错误时的处理：

- ✅ 配置 MockWebServer 返回错误响应
- ✅ 验证错误被正确处理
- ✅ 验证返回状态为 `FAILED`

### 3. 网络超时处理 (`testSubmitIssueTimeout`)

测试网络超时场景：

- ✅ 配置 MockWebServer 延迟响应（超过超时时间）
- ✅ 验证超时被正确处理

### 4. 响应解析 - URL 格式 (`testExtractDiscussionIdWithUrl`)

测试响应中包含 URL 格式的 issue ID：

- ✅ 验证 URL 被正确提取
- ✅ 验证 `getLinkText()` 返回 "Issue"

### 5. 响应解析 - 数字格式 (`testExtractDiscussionIdWithNumber`)

测试响应中包含数字格式的 issue ID：

- ✅ 验证数字 ID 被正确提取
- ✅ 验证 `getLinkText()` 返回 "Issue #789"

### 6. 空异常文本处理 (`testSubmitWithEmptyThrowableText`)

测试空异常文本的情况：

- ✅ 验证返回 `false`
- ✅ 验证 consumer 不会被调用

## 运行测试

### 方法 1: 在 IntelliJ IDEA 中运行（推荐）

1. 打开 `EngineFeedbackSubmitterTest.java`
2. 点击类名旁边的绿色运行图标
3. 选择 "Run 'EngineFeedbackSubmitterTest'"

或者运行单个测试方法：

1. 点击方法名旁边的绿色运行图标
2. 选择 "Run 'testMethodName()'"

### 方法 2: 使用 Gradle 命令行

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests "EngineFeedbackSubmitterTest"

# 运行特定测试方法
./gradlew test --tests "EngineFeedbackSubmitterTest.testSubmitNewIssueSuccess"
```

### 方法 3: 查看测试报告

```bash
./gradlew test
open build/reports/tests/test/index.html
```

## 测试架构

### TestableEngineFeedbackSubmitter

由于 `EngineFeedbackSubmitter` 使用了硬编码的 API URL (`SiteContents.ISSUE_API_URL`)，我们创建了一个可测试的子类
`TestableEngineFeedbackSubmitter`：

- 重写 `newIssueByTitleBody()` 方法
- 使用 MockWebServer 的 URL 而不是真实 API
- 保持与原始实现相同的逻辑和签名验证

### MockWebServer 配置

```java
@BeforeEach
void setUp() throws Exception {
    super.setUp();
    // 启动 MockWebServer
    mockServer = new MockWebServer();
    mockServer.start();

    // 创建可测试的 submitter
    submitter = new TestableEngineFeedbackSubmitter();
    submitter.setBaseUrl(mockServer.url("/").toString());
}
```

### IdeaLoggingEvent Mock

使用 Mockito 创建 `IdeaLoggingEvent` 的 mock 对象：

```java
private IdeaLoggingEvent createTestLoggingEvent(String message, String throwableText) {
    IdeaLoggingEvent event = Mockito.mock(IdeaLoggingEvent.class);
    when(event.getMessage()).thenReturn(message);
    when(event.getThrowableText()).thenReturn(throwableText);
    return event;
}
```

## 注意事项

1. **线程安全**: 测试中使用 `CountDownLatch` 等待异步回调完成
2. **超时设置**: 某些测试可能需要调整超时时间
3. **IntelliJ Platform**: 测试需要完整的 IntelliJ Platform 环境，包括 PSI、Document 等组件

## 故障排除

### 测试失败：找不到类

确保项目依赖正确配置：

```kotlin
dependencies {
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.mockito:mockito-core:5.2.0")
}
```

### 测试超时

如果测试超时，可以增加等待时间：

```java
assertTrue(latch.await(10, TimeUnit.SECONDS)); // 从 5 秒增加到 10 秒
```

### MockWebServer 端口冲突

MockWebServer 会自动选择可用端口，通常不会有问题。如果遇到端口冲突，可以手动指定：

```java
mockServer = new MockWebServer();
mockServer.start(0); // 0 表示自动选择端口
```

## 相关文档

- [IntelliJ Platform Test Framework](https://plugins.jetbrains.com/docs/intellij/testing-plugins.html)
- [MockWebServer 文档](https://github.com/square/okhttp/tree/master/mockwebserver)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
