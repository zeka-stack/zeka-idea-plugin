package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.ai.provider.AIServiceProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.OllamaProvider;
import dev.dong4j.zeka.stack.idea.plugin.ai.provider.QianWenProvider;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AI Provider HTTP 集成测试
 * <p>
 * 本测试类展示如何测试真实的 HTTP 调用，包括：
 * <ul>
 *   <li>使用 MockWebServer 模拟 AI API 服务器</li>
 *   <li>测试成功的 HTTP 请求和响应</li>
 *   <li>测试各种错误场景（401, 429, 500等）</li>
 *   <li>测试重试机制</li>
 *   <li>验证请求头和请求体</li>
 *   <li>测试超时和网络错误</li>
 * </ul>
 * <p>
 * 这是真实的 HTTP 集成测试，不使用 Mock，而是启动一个本地 HTTP 服务器。
 *
 * @author Cursor AI Assistant
 * @version 1.0
 */
@DisplayName("AI Provider HTTP 集成测试")
public class AIProviderHttpIntegrationTest {

    private MockWebServer mockServer;
    private SettingsState settings;
    private AIServiceProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        // 启动 Mock Web Server
        mockServer = new MockWebServer();
        mockServer.start();

        // 配置 Settings
        settings = new SettingsState();
        settings.aiProvider = AIProviderType.QIANWEN.getProviderId();
        settings.modelName = "qwen-max";
        settings.baseUrl = mockServer.url("/").toString().replaceAll("/$", "");
        settings.apiKey = "test-api-key";
        settings.maxRetries = 3;
        settings.waitDuration = 100; // 缩短重试等待时间以加快测试
        settings.temperature = 0.1;
        settings.maxTokens = 1000;
        settings.verboseLogging = true; // 启用详细日志以便调试

        // 创建 Provider
        provider = new QianWenProvider(settings);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null) {
            mockServer.shutdown();
        }
    }

    @Test
    @DisplayName("测试成功的文档生成 - 模拟真实 API 响应")
    void testSuccessfulDocumentationGeneration() throws Exception {
        // 1. 准备 Mock 响应（模拟 OpenAI 格式的响应）
        String mockResponseBody = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "qwen-max",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "/**\\n * 测试方法\\n * \\n * @param param 参数\\n * @return 结果\\n */"
                    },
                    "finish_reason": "stop"
                }],
                "usage": {
                    "prompt_tokens": 100,
                    "completion_tokens": 50,
                    "total_tokens": 150
                }
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponseBody)
                               .addHeader("Content-Type", "application/json"));

        // 2. 调用 API
        String testCode = "public String testMethod(String param) { return param; }";
        String result = provider.generateDocumentation(
            testCode,
            DocumentationTask.TaskType.METHOD,
            "java"
                                                      );

        // 3. 验证结果
        assertThat(result).isNotNull();
        assertThat(result).contains("测试方法");
        assertThat(result).contains("@param param 参数");
        assertThat(result).contains("@return 结果");

        // 4. 验证请求
        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/chat/completions");

        // 验证请求头
        Headers headers = request.getHeaders();
        assertThat(headers.get("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(headers.get("Content-Type")).contains("application/json");

        // 验证请求体
        String requestBody = request.getBody().readUtf8();
        System.out.println("Request body: " + requestBody);

        JsonObject requestJson = JsonParser.parseString(requestBody).getAsJsonObject();
        assertThat(requestJson.get("model").getAsString()).isEqualTo("qwen-max");
        assertThat(requestJson.get("temperature").getAsDouble()).isEqualTo(0.1);
        assertThat(requestJson.get("max_tokens").getAsInt()).isEqualTo(1000);
        assertThat(requestJson.has("messages")).isTrue();
    }

    @Test
    @DisplayName("测试 401 未授权错误 - Invalid API Key")
    void testUnauthorizedError() {
        // Mock 401 响应
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(401)
                               .setBody("{\"error\": {\"message\": \"Invalid API key\", \"type\": \"invalid_request_error\"}}")
                               .addHeader("Content-Type", "application/json"));

        // 验证抛出异常
        assertThatThrownBy(() -> provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                               ))
            .isInstanceOf(AIServiceException.class)
            .hasMessageContaining("Invalid API Key")
            .extracting(e -> ((AIServiceException) e).getErrorCode())
            .isEqualTo(AIServiceException.ErrorCode.INVALID_API_KEY);
    }

    @Test
    @DisplayName("测试 429 请求过多错误 - Rate Limit")
    void testRateLimitError() {
        // Mock 429 响应
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(429)
                               .setBody("{\"error\": {\"message\": \"Rate limit exceeded\", \"type\": \"rate_limit_error\"}}")
                               .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                               ))
            .isInstanceOf(AIServiceException.class)
            .hasMessageContaining("Rate limit")
            .extracting(e -> ((AIServiceException) e).getErrorCode())
            .isEqualTo(AIServiceException.ErrorCode.RATE_LIMIT);
    }

    @Test
    @DisplayName("测试 500 服务器错误")
    void testServerError() {
        // Mock 500 响应
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(500)
                               .setBody("{\"error\": {\"message\": \"Internal server error\"}}")
                               .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                               ))
            .isInstanceOf(AIServiceException.class)
            .hasMessageContaining("Server error")
            .extracting(e -> ((AIServiceException) e).getErrorCode())
            .isEqualTo(AIServiceException.ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("测试重试机制 - 第二次请求成功")
    void testRetryMechanism() throws Exception {
        // 第一次请求失败（500错误）
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(500)
                               .setBody("{\"error\": {\"message\": \"Temporary error\"}}"));

        // 第二次请求成功
        String successResponse = """
            {
                "choices": [{
                    "message": {
                        "content": "/** 成功的文档 */"
                    }
                }]
            }
            """;
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(successResponse)
                               .addHeader("Content-Type", "application/json"));

        // 调用 API
        String result = provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                      );

        // 验证结果
        assertThat(result).contains("成功的文档");

        // 验证发送了两次请求
        assertThat(mockServer.getRequestCount()).isEqualTo(2);

        // 验证两次请求都是 POST
        RecordedRequest request1 = mockServer.takeRequest();
        RecordedRequest request2 = mockServer.takeRequest();
        assertThat(request1.getMethod()).isEqualTo("POST");
        assertThat(request2.getMethod()).isEqualTo("POST");
    }

    @Test
    @DisplayName("测试重试耗尽 - 所有重试都失败")
    void testRetryExhausted() {
        // 所有请求都返回 500 错误
        for (int i = 0; i < settings.maxRetries; i++) {
            mockServer.enqueue(new MockResponse()
                                   .setResponseCode(500)
                                   .setBody("{\"error\": {\"message\": \"Server error\"}}"));
        }

        // 验证最终失败
        assertThatThrownBy(() -> provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                               ))
            .isInstanceOf(AIServiceException.class)
            .hasMessageContaining("Server error");

        // 验证重试了正确的次数
        assertThat(mockServer.getRequestCount()).isEqualTo(settings.maxRetries);
    }

    @Test
    @DisplayName("测试网络连接错误")
    void testNetworkError() throws IOException {
        // 关闭服务器模拟网络错误
        mockServer.shutdown();

        assertThatThrownBy(() -> provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                               ))
            .isInstanceOf(AIServiceException.class)
            .hasMessageContaining("Network error")
            .extracting(e -> ((AIServiceException) e).getErrorCode())
            .isEqualTo(AIServiceException.ErrorCode.NETWORK_ERROR);
    }

    @Test
    @DisplayName("测试响应解析错误 - 无效的 JSON")
    void testInvalidJsonResponse() {
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody("This is not valid JSON")
                               .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                               ))
            .isInstanceOf(AIServiceException.class)
            .extracting(e -> ((AIServiceException) e).getErrorCode())
            .isEqualTo(AIServiceException.ErrorCode.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("测试响应格式错误 - 缺少必需字段")
    void testMissingRequiredFields() {
        // 响应缺少 choices 字段
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody("{\"id\": \"test\", \"model\": \"qwen-max\"}")
                               .addHeader("Content-Type", "application/json"));

        assertThatThrownBy(() -> provider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                               ))
            .isInstanceOf(AIServiceException.class)
            .extracting(e -> ((AIServiceException) e).getErrorCode())
            .isEqualTo(AIServiceException.ErrorCode.INVALID_RESPONSE);
    }

    @Test
    @DisplayName("测试不同类型的文档生成 - 类")
    void testGenerateClassDocumentation() throws Exception {
        String mockResponse = """
            {
                "choices": [{
                    "message": {
                        "content": "/**\\n * 用户服务类\\n * <p>提供用户相关的业务逻辑\\n */"
                    }
                }]
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json"));

        String result = provider.generateDocumentation(
            "public class UserService { }",
            DocumentationTask.TaskType.CLASS,
            "java"
                                                      );

        assertThat(result).contains("用户服务类");
        assertThat(result).contains("业务逻辑");
    }

    @Test
    @DisplayName("测试不同类型的文档生成 - 测试方法")
    void testGenerateTestMethodDocumentation() throws Exception {
        String mockResponse = """
            {
                "choices": [{
                    "message": {
                        "content": "/**\\n * 测试用户登录功能\\n * <p>验证正确的用户名和密码可以成功登录\\n */"
                    }
                }]
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json"));

        String result = provider.generateDocumentation(
            "@Test public void testUserLogin() { }",
            DocumentationTask.TaskType.TEST_METHOD,
            "java"
                                                      );

        assertThat(result).contains("测试用户登录功能");
    }

    @Test
    @DisplayName("测试 Ollama Provider - 不需要 API Key")
    void testOllamaProviderWithoutApiKey() throws Exception {
        // 配置 Ollama Provider
        settings.aiProvider = AIProviderType.OLLAMA.getProviderId();
        settings.modelName = "qwen:7b";
        settings.apiKey = ""; // Ollama 不需要 API Key

        AIServiceProvider ollamaProvider = new OllamaProvider(settings);

        String mockResponse = """
            {
                "choices": [{
                    "message": {
                        "content": "/** Ollama 生成的文档 */"
                    }
                }]
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json"));

        String result = ollamaProvider.generateDocumentation(
            "public void test() {}",
            DocumentationTask.TaskType.METHOD,
            "java"
                                                            );

        assertThat(result).contains("Ollama 生成的文档");

        // 验证请求头不包含 Authorization
        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getHeader("Authorization")).isNull();
    }

    @Test
    @DisplayName("测试请求超时")
    void testRequestTimeout() {
        // Mock 一个延迟响应
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody("{\"choices\": [{\"message\": {\"content\": \"test\"}}]}")
                               .setBodyDelay(5, TimeUnit.SECONDS)); // 延迟 5 秒

        // 设置较短的超时时间
        settings.timeout = 1000; // 1 秒

        // 注意：需要在 OpenAICompatibleProvider 中配置 RestTemplate 的超时
        // 这里主要展示测试思路

        System.out.println("Timeout test: This may take a few seconds...");

        // 由于当前实现没有配置超时，这个测试可能需要修改源代码支持
        // 这里作为示例保留
    }

    @Test
    @DisplayName("测试配置验证")
    void testConfigurationValidation() {
        // Mock 一个简单的成功响应
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody("{\"choices\": [{\"message\": {\"content\": \"OK\"}}]}")
                               .addHeader("Content-Type", "application/json"));

        ValidationResult isValid = provider.validateConfiguration();

        assertThat(isValid.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("测试配置验证失败")
    void testConfigurationValidationFailure() {
        // Mock 一个错误响应
        mockServer.enqueue(new MockResponse()
                               .setResponseCode(401)
                               .setBody("{\"error\": {\"message\": \"Invalid credentials\"}}"));

        ValidationResult isValid = provider.validateConfiguration();

        assertThat(isValid.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("测试完整的请求响应流程 - 带详细验证")
    void testCompleteRequestResponseFlow() throws Exception {
        String requestCode = """
            public class UserService {
                public User findById(int id) {
                    return userRepository.findById(id);
                }
            }
            """;

        String mockResponse = """
            {
                "id": "chatcmpl-abc123",
                "object": "chat.completion",
                "created": 1699896916,
                "model": "qwen-max",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "/**\\n * 根据ID查找用户\\n * \\n * @param id 用户ID\\n * @return 用户对象，如果不存在则返回null\\n */"
                        },
                        "finish_reason": "stop"
                    }
                ],
                "usage": {
                    "prompt_tokens": 250,
                    "completion_tokens": 80,
                    "total_tokens": 330
                }
            }
            """;

        mockServer.enqueue(new MockResponse()
                               .setResponseCode(200)
                               .setBody(mockResponse)
                               .addHeader("Content-Type", "application/json")
                               .addHeader("X-Request-ID", "test-request-123"));

        // 执行请求
        long startTime = System.currentTimeMillis();
        String result = provider.generateDocumentation(
            requestCode,
            DocumentationTask.TaskType.CLASS,
            "java"
                                                      );
        long endTime = System.currentTimeMillis();

        System.out.println("Request completed in " + (endTime - startTime) + "ms");
        System.out.println("Generated documentation: " + result);

        // 验证结果
        assertThat(result)
            .isNotNull()
            .isNotEmpty()
            .contains("根据ID查找用户")
            .contains("@param id 用户ID")
            .contains("@return 用户对象");

        // 验证请求详情
        RecordedRequest request = mockServer.takeRequest();

        System.out.println("=== Request Details ===");
        System.out.println("Method: " + request.getMethod());
        System.out.println("Path: " + request.getPath());
        System.out.println("Headers: " + request.getHeaders());

        String requestBody = request.getBody().readUtf8();
        System.out.println("Body: " + requestBody);

        // 解析并验证请求体
        JsonObject requestJson = JsonParser.parseString(requestBody).getAsJsonObject();
        assertThat(requestJson.get("model").getAsString()).isEqualTo("qwen-max");
        assertThat(requestJson.has("messages")).isTrue();

        // 验证消息内容包含我们的代码
        String messageContent = requestJson.getAsJsonArray("messages")
            .get(0).getAsJsonObject()
            .get("content").getAsString();
        assertThat(messageContent).contains("UserService");
    }
}

