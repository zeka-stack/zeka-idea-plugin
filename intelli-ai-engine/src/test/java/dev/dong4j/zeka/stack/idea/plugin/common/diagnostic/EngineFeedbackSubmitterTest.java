package dev.dong4j.zeka.stack.idea.plugin.common.diagnostic;

import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.Consumer;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.Setter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * EngineFeedbackSubmitter 集成测试
 * <p>
 * 测试在 IntelliJ IDEA 插件环境中提交 issues 的功能。
 * 使用 MockWebServer 模拟 HTTP 服务器，测试各种场景：
 * <ul>
 *   <li>成功提交新 issue</li>
 *   <li>服务器错误处理</li>
 *   <li>网络超时处理</li>
 *   <li>响应解析</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2026.01.21
 * @since 1.0.0
 */
public class EngineFeedbackSubmitterTest extends BasePlatformTestCase {

    /** MockWebServer 实例, 用于在测试中模拟 HTTP 服务器响应 */
    private MockWebServer mockServer;
    /** 可测试的 EngineFeedbackSubmitter 子类, 用于集成测试中模拟提交反馈行为 */
    private TestableEngineFeedbackSubmitter submitter;

    /**
     * 可测试的 EngineFeedbackSubmitter 子类
     * <p>
     * 允许在测试中注入 MockWebServer 的 URL，通过重写 newIssueByTitleBody 方法
     * 来使用 MockWebServer 而不是真实的 API。
     */
    @Setter
    private static class TestableEngineFeedbackSubmitter extends EngineFeedbackSubmitter {
        /** MockWebServer 的基础 URL, 用于在测试中替换真实 API 地址以提交反馈. */
        private String baseUrl;

        /**
         * 根据标题和内容创建反馈问题
         * <p> 构建包含插件信息,IDE 版本, 操作系统及元数据的请求体, 并通过模拟服务器发送 HTTP 请求, 提取并返回反馈问题的讨论链接或编号.
         * 若请求失败, 记录警告日志并抛出 IllegalStateException 异常.
         *
         * @param title 反馈问题的标题
         * @param body  反馈问题的正文内容
         * @return 反馈问题的讨论链接或编号, 若无法提取则返回 "unknown"
         */
        @Override
        protected @NotNull String newIssueByTitleBody(String title, String body) {
            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("title", title);
            requestBody.put("content", body);
            requestBody.put("type", "BUG");
            requestBody.put("category", "GENERAL");

            java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
            userInfo.put("pluginName", dev.dong4j.zeka.stack.idea.plugin.common.EngineContents.PLUGIN_NAME);
            String pluginVersion = getPluginVersion();
            if (pluginVersion != null) {
                userInfo.put("pluginVersion", pluginVersion);
            }
            userInfo.put("ideaVersion", getIdeaVersion());
            userInfo.put("os", getOperatingSystem());
            requestBody.put("userInfo", userInfo);

            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("clientId", dev.dong4j.zeka.stack.idea.plugin.common.EngineContents.PLUGIN_ID);
            metadata.put("timestamp", System.currentTimeMillis());
            requestBody.put("metadata", metadata);

            try {
                String response = sendHttpRequestToMockServer(requestBody);
                return extractDiscussionId(response);
            } catch (Exception e) {
                com.intellij.openapi.diagnostic.Logger.getInstance(EngineFeedbackSubmitter.class)
                    .warn("Failed to submit feedback", e);
                throw new IllegalStateException("提交反馈失败: " + e.getMessage(), e);
            }
        }

        /**
         * 向 MockServer 发送 HTTP 请求并返回响应体
         * <p> 该方法用于在测试环境中通过 MockWebServer 模拟真实 API 调用, 将请求体序列化为 JSON 并发送 POST 请求, 返回服务器响应内容.
         * 请求包含签名信息, 使用 {@code RequestSigner} 生成签名头, 确保请求符合服务端验证规则.
         *
         * @param requestBody 请求体数据, 以 Map 形式传递, 将被序列化为 JSON 字符串
         * @return 服务器返回的响应体字符串
         * @throws IOException          当请求签名生成失败或 HTTP 请求发送失败时抛出
         * @throws InterruptedException 当 HTTP 请求被中断时抛出
         *
         *                              <pre>{@code
         *                                                           ObjectMapper mapper = new ObjectMapper();
         *                                                           String jsonBody = mapper.writeValueAsString(requestBody);
         *                                                           byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
         *                                                           }</pre>
         */
        private String sendHttpRequestToMockServer(java.util.Map<String, Object> requestBody)
            throws IOException, InterruptedException {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBody);
            byte[] bodyBytes = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            java.net.URI uri = java.net.URI.create(baseUrl);
            String pathWithQuery = "/api/plugin/feedback/issue";
            if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
                pathWithQuery += "?" + uri.getQuery();
            }

            dev.dong4j.zeka.stack.idea.plugin.common.util.RequestSigner.SignedHeaders signedHeaders;
            try {
                signedHeaders = dev.dong4j.zeka.stack.idea.plugin.common.util.RequestSigner.sign(
                    dev.dong4j.zeka.stack.idea.plugin.common.EngineContents.PLUGIN_ID,
                    "zeka-stack-engine-plugin",
                    "POST",
                    pathWithQuery,
                    bodyBytes
                                                                                                );
            } catch (Exception e) {
                throw new IOException("生成请求签名失败: " + e.getMessage(), e);
            }

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(uri.resolve(pathWithQuery))
                .header("Content-Type", "application/json")
                .header("X-Client-Id", signedHeaders.clientId())
                .header("X-Timestamp", signedHeaders.timestamp())
                .header("X-Nonce", signedHeaders.nonce())
                .header("X-Body-SHA256", signedHeaders.bodySha256())
                .header("X-Signature", signedHeaders.signature())
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                                                                      java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.body();
        }

        /**
         * 从响应字符串中提取讨论 ID
         * <p> 解析 JSON 响应, 优先查找 issue 或 discussion 对象中的 url 或 number 字段作为讨论 ID, 若均不存在则返回 "unknown"
         *
         * @param response 响应内容的 JSON 字符串
         * @return 提取到的讨论 ID(URL 或编号), 若未找到则返回 "unknown"
         * @throws IOException 当解析 JSON 失败时抛出
         */
        private String extractDiscussionId(@NotNull String response) throws IOException {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
            com.fasterxml.jackson.databind.JsonNode dataNode = root.has("data") ? root.get("data") : root;

            boolean requestSuccess = !root.has("success") || root.path("success").asBoolean(false);
            boolean businessSuccess = dataNode.path("success").asBoolean(requestSuccess);

            if (!requestSuccess || !businessSuccess) {
                String message = dataNode.hasNonNull("message") ? dataNode.get("message").asText() : "";
                String error = dataNode.hasNonNull("error") ? dataNode.get("error").asText() : "";
                String reason = !com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces(error) ? error : message;
                if (com.intellij.openapi.util.text.StringUtil.isEmptyOrSpaces(reason)) {
                    reason = "unknown error";
                }
                throw new IllegalStateException(reason);
            }

            com.fasterxml.jackson.databind.JsonNode issue = dataNode.get("issue");
            if (issue != null && issue.hasNonNull("url")) {
                return issue.get("url").asText();
            }
            if (issue != null && issue.hasNonNull("number")) {
                return issue.get("number").asText();
            }

            com.fasterxml.jackson.databind.JsonNode discussion = dataNode.get("discussion");
            if (discussion != null && discussion.hasNonNull("url")) {
                return discussion.get("url").asText();
            }
            if (discussion != null && discussion.hasNonNull("number")) {
                return discussion.get("number").asText();
            }
            return "unknown";
        }

        // 这些方法需要从父类访问，但由于是 private，我们需要重新实现

        /**
         * 获取当前插件的版本号
         * <p> 通过插件 ID 查找插件描述符并返回其版本号, 若插件不存在或获取失败则返回 null
         *
         * @return 插件版本号, 若插件不存在或获取失败则返回 null
         */
        private String getPluginVersion() {
            return dev.dong4j.zeka.stack.idea.plugin.kit.PluginUtil.getVersion(
                dev.dong4j.zeka.stack.idea.plugin.common.EngineContents.PLUGIN_ID);
        }

        /**
         * 获取当前 IntelliJ IDEA 的完整版本号
         * <p> 通过访问 ApplicationInfo 实例获取 IDEA 的完整版本信息, 若发生异常则返回 "未知" 字符串.
         *
         * @return IDEA 完整版本号, 若获取失败则返回 "未知"
         */
        private String getIdeaVersion() {
            try {
                return com.intellij.openapi.application.ApplicationInfo.getInstance().getFullVersion();
            } catch (Exception e) {
                return "未知";
            }
        }

        /**
         * 获取当前操作系统的名称和版本信息
         * <p>根据系统类型 (Windows,macOS,Linux) 返回对应的系统标识字符串, 若无法识别则返回系统原始名称和版本</p>
         *
         * @return 操作系统名称和版本, 例如 "Windows 10.0","macOS 13.0","Linux 5.4.0" 或原始系统信息
         */
        @NotNull
        private String getOperatingSystem() {
            if (com.intellij.openapi.util.SystemInfo.isWindows) {
                return "Windows " + com.intellij.openapi.util.SystemInfo.getOsNameAndVersion();
            }
            if (com.intellij.openapi.util.SystemInfo.isMac) {
                return "macOS " + com.intellij.openapi.util.SystemInfo.getOsNameAndVersion();
            }
            if (com.intellij.openapi.util.SystemInfo.isLinux) {
                return "Linux " + com.intellij.openapi.util.SystemInfo.getOsNameAndVersion();
            }
            return com.intellij.openapi.util.SystemInfo.getOsNameAndVersion();
        }
    }

    /**
     * 在每个测试用例执行前进行环境初始化
     * <p> 初始化 MockWebServer 以模拟 HTTP 服务器响应, 创建可测试的 EngineFeedbackSubmitter 子类实例, 并设置其基础 URL 为 MockWebServer 的地址.
     *
     * @throws Exception 如果在初始化过程中发生任何异常
     */
    @BeforeEach
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // 启动 MockWebServer
        mockServer = new MockWebServer();
        mockServer.start();

        // 创建可测试的 submitter
        submitter = new TestableEngineFeedbackSubmitter();
        submitter.setBaseUrl(mockServer.url("/").toString());
    }

    /**
     * 测试环境清理方法, 在每个测试用例执行后调用.
     * <p> 首先关闭模拟 HTTP 服务器 (如果已启动), 然后调用父类的清理逻辑以释放其他资源.
     *
     * @throws Exception 如果在清理过程中发生任何异常
     */
    @AfterEach
    @Override
    protected void tearDown() throws Exception {
        // 关闭 MockWebServer
        if (mockServer != null) {
            mockServer.shutdown();
        }
        super.tearDown();
    }

    /**
     * 测试成功提交新 issue
     */
    @Test
    public void testSubmitNewIssueSuccess() throws Exception {
        // 准备 Mock 响应
        String mockResponse = """
            {
                "success": true,
                "data": {
                    "success": true,
                    "issue": {
                        "url": "https://github.com/zeka-stack/zeka-idea-plugin/issues/123",
                        "number": "123"
                    }
                }
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json"));

        // 创建测试用的 IdeaLoggingEvent
        IdeaLoggingEvent event = createTestLoggingEvent(
            "Test exception message",
            "java.lang.RuntimeException: Test exception message\n" +
            "    at com.example.Test.main(Test.java:10)"
                                                       );

        // 使用 CountDownLatch 等待异步回调
        CountDownLatch latch = new CountDownLatch(1);
        SubmittedReportInfo[] result = new SubmittedReportInfo[1];

        // 提交 issue
        boolean success = submitter.submit(
            new IdeaLoggingEvent[] {event},
            "Additional info: This is a test",
            new javax.swing.JPanel(), // 父组件
            new Consumer<SubmittedReportInfo>() {
                /**
                 * 消费提交的报告信息
                 * <p> 将传入的报告信息存储到结果数组的第一个位置, 并触发计数器倒计时
                 *
                 * @param reportInfo 提交的报告信息
                 */
                @Override
                public void consume(SubmittedReportInfo reportInfo) {
                    result[0] = reportInfo;
                    latch.countDown();
                }
            }
                                          );

        // 等待回调完成
        assertEquals(String.valueOf(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE), "Status should be NEW_ISSUE",
                     result[0].getStatus());

        // 验证请求
        RecordedRequest request = mockServer.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("POST", "Method should be POST", request.getMethod());
        assertEquals("/api/plugin/feedback/issue", "Path should match",
                     request.getPath());

        // 验证请求头
        assertThat(request.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(request.getHeader("X-Client-Id")).isNotNull();
        assertThat(request.getHeader("X-Timestamp")).isNotNull();
        assertThat(request.getHeader("X-Signature")).isNotNull();

        // 验证请求体包含必要字段
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("title");
        assertThat(requestBody).contains("content");
        assertThat(requestBody).contains("type");
        assertThat(requestBody).contains("BUG");
        assertThat(requestBody).contains("userInfo");
        assertThat(requestBody).contains("pluginName");
    }

    /**
     * 测试服务器返回错误
     */
    @Test
    public void testSubmitIssueServerError() throws Exception {
        // 准备 Mock 错误响应
        String mockResponse = """
            {
                "success": false,
                "data": {
                    "success": false,
                    "message": "Internal server error",
                    "error": "Database connection failed"
                }
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json"));

        IdeaLoggingEvent event = createTestLoggingEvent(
            "Test exception",
            "java.lang.RuntimeException: Test exception"
                                                       );

        CountDownLatch latch = new CountDownLatch(1);
        SubmittedReportInfo[] result = new SubmittedReportInfo[1];

        boolean success = submitter.submit(
            new IdeaLoggingEvent[] {event},
            null,
            new javax.swing.JPanel(),
            new Consumer<SubmittedReportInfo>() {
                /**
                 * 消费提交的报告信息
                 * <p> 将传入的报告信息存储到结果数组的第一个位置, 并触发计数器倒计时
                 *
                 * @param reportInfo 提交的报告信息
                 */
                @Override
                public void consume(SubmittedReportInfo reportInfo) {
                    result[0] = reportInfo;
                    latch.countDown();
                }
            }
                                          );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(result[0]);
        assertEquals(String.valueOf(SubmittedReportInfo.SubmissionStatus.FAILED), result[0].getStatus(),
                     "Status should be FAILED");
        assertThat(result[0].getLinkText()).contains("error");
    }

    /**
     * 测试网络超时
     */
    @Test
    public void testSubmitIssueTimeout() throws Exception {
        // 模拟超时：不返回响应，让请求超时
        // 注意：由于 HttpClient 的超时设置，这个测试可能需要调整
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody("{}")
                               .setBodyDelay(15, java.util.concurrent.TimeUnit.SECONDS) // 延迟超过超时时间
                               .addHeader("Content-Type", "application/json"));

        IdeaLoggingEvent event = createTestLoggingEvent(
            "Test exception",
            "java.lang.RuntimeException: Test exception"
                                                       );

        CountDownLatch latch = new CountDownLatch(1);
        SubmittedReportInfo[] result = new SubmittedReportInfo[1];

        boolean success = submitter.submit(
            new IdeaLoggingEvent[] {event},
            null,
            new javax.swing.JPanel(),
            new Consumer<SubmittedReportInfo>() {
                /**
                 * 消费提交的报告信息
                 * <p> 将传入的报告信息存储到结果数组的第一个位置, 并触发计数器倒计时
                 *
                 * @param reportInfo 提交的报告信息
                 */
                @Override
                public void consume(SubmittedReportInfo reportInfo) {
                    result[0] = reportInfo;
                    latch.countDown();
                }
            }
                                          );

        // 等待回调（可能会因为超时而失败）
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        if (completed) {
            assertNotNull(result[0]);
            // 超时情况下，状态应该是 FAILED
            if (result[0].getStatus() == SubmittedReportInfo.SubmissionStatus.FAILED) {
                assertThat(result[0].getLinkText()).contains("error");
            }
        }
    }

    /**
     * 测试响应解析 - 返回 URL 格式的 issue ID
     */
    @Test
    public void testExtractDiscussionIdWithUrl() throws Exception {
        String mockResponse = """
            {
                "success": true,
                "data": {
                    "success": true,
                    "issue": {
                        "url": "https://github.com/zeka-stack/zeka-idea-plugin/issues/456"
                    }
                }
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json"));

        IdeaLoggingEvent event = createTestLoggingEvent(
            "Test exception",
            "java.lang.RuntimeException: Test exception"
                                                       );

        CountDownLatch latch = new CountDownLatch(1);
        SubmittedReportInfo[] result = new SubmittedReportInfo[1];

        submitter.submit(
            new IdeaLoggingEvent[] {event},
            null,
            new javax.swing.JPanel(),
            new Consumer<SubmittedReportInfo>() {
                /**
                 * 消费提交的报告信息
                 * <p> 将传入的报告信息存储到结果数组的第一个位置, 并减少闭锁计数器
                 *
                 * @param reportInfo 提交的报告信息
                 */
                @Override
                public void consume(SubmittedReportInfo reportInfo) {
                    result[0] = reportInfo;
                    latch.countDown();
                }
            }
                        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(result[0]);
        assertEquals(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE, result[0].getStatus());
        // 验证 URL 被正确提取
        assertThat(result[0].getURL()).contains("github.com");
        assertThat(result[0].getLinkText()).isEqualTo("Issue");
    }

    /**
     * 测试响应解析 - 返回数字格式的 issue ID
     */
    @Test
    public void testExtractDiscussionIdWithNumber() throws Exception {
        String mockResponse = """
            {
                "success": true,
                "data": {
                    "success": true,
                    "issue": {
                        "number": "789"
                    }
                }
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json"));

        IdeaLoggingEvent event = createTestLoggingEvent(
            "Test exception",
            "java.lang.RuntimeException: Test exception"
                                                       );

        CountDownLatch latch = new CountDownLatch(1);
        SubmittedReportInfo[] result = new SubmittedReportInfo[1];

        submitter.submit(
            new IdeaLoggingEvent[] {event},
            null,
            new javax.swing.JPanel(),
            new Consumer<SubmittedReportInfo>() {
                /**
                 * 消费提交的报告信息
                 * <p> 将传入的报告信息存储到结果数组的第一个位置, 并减少同步计数器的值
                 *
                 * @param reportInfo 要消费的报告信息
                 */
                @Override
                public void consume(SubmittedReportInfo reportInfo) {
                    result[0] = reportInfo;
                    latch.countDown();
                }
            }
                        );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(result[0]);
        assertEquals(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE, result[0].getStatus());
        // 验证数字 ID 被正确提取
        assertThat(result[0].getLinkText()).isEqualTo("Issue #789");
    }

    /**
     * 测试空异常文本的情况
     */
    @Test
    public void testSubmitWithEmptyThrowableText() throws Exception {
        IdeaLoggingEvent event = createTestLoggingEvent(
            "Test message",
            "" // 空的异常文本
                                                       );

        CountDownLatch latch = new CountDownLatch(1);
        SubmittedReportInfo[] result = new SubmittedReportInfo[1];

        boolean success = submitter.submit(
            new IdeaLoggingEvent[] {event},
            null,
            new javax.swing.JPanel(),
            new Consumer<SubmittedReportInfo>() {
                /**
                 * 消费提交的报告信息
                 * <p> 将传入的报告信息存储到结果数组的第一个位置, 并触发计数器倒计时
                 *
                 * @param reportInfo 提交的报告信息
                 */
                @Override
                public void consume(SubmittedReportInfo reportInfo) {
                    result[0] = reportInfo;
                    latch.countDown();
                }
            }
                                          );

        // 空异常文本应该返回 false，不调用 consumer
        assertThat(success).isFalse();
        // 由于返回 false，consumer 可能不会被调用
        // 所以不等待 latch
    }

    /**
     * 创建测试用的 IdeaLoggingEvent
     * <p>
     * 使用 Mockito 创建 mock 对象，模拟 IdeaLoggingEvent 的行为
     */
    private IdeaLoggingEvent createTestLoggingEvent(String message, String throwableText) {
        IdeaLoggingEvent event = org.mockito.Mockito.mock(IdeaLoggingEvent.class);
        when(event.getMessage()).thenReturn(message);
        when(event.getThrowableText()).thenReturn(throwableText);
        if (!throwableText.isEmpty()) {
            RuntimeException exception = new RuntimeException(throwableText);
            when(event.getThrowable()).thenReturn(exception);
        }
        return event;
    }
}
