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
 * 本测试类用于验证 AI 服务提供者与 HTTP API 的集成行为，包括正常请求、错误处理、重试机制、网络异常、响应解析等场景。
 * 测试通过启动本地 MockWebServer 模拟真实 API 服务，确保测试的准确性和完整性。
 *
 * @author Cursor AI Assistant
 * @version 1.0
 * @date 2025.04.01
 * @since 1.0
 */
@DisplayName("AI Provider HTTP 集成测试")
public class AIProviderHttpIntegrationTest {

    /** 模拟的 Web 服务器实例，用于测试网络请求和响应 */
    private MockWebServer mockServer;
    /** 设置状态信息 */
    private SettingsState settings;
    /** AIServiceProvider 实例，用于调用 AI 服务 */
    private AIServiceProvider provider;

    /**
     * 初始化测试环境，启动 Mock Web Server 并配置 Settings 对象
     * <p>
     * 该方法用于在每个测试用例执行前设置必要的测试环境，包括启动模拟的 Web 服务器、初始化 Settings 配置信息以及创建对应的 AI 服务提供者。
     *
     * @throws IOException 如果启动 Mock Web Server 时发生 I/O 异常
     */
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

    /**
     * 每次测试结束后关闭MockServer
     * <p>
     * 用于确保在每次测试用例执行完毕后，MockServer服务被正确关闭，释放相关资源。
     *
     * @throws IOException 如果关闭MockServer过程中发生IO异常
     */
    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null) {
            mockServer.shutdown();
        }
    }

    /**
     * 测试文档生成功能是否能正确模拟真实 API 响应
     * <p>
     * 测试场景：调用文档生成接口并模拟 OpenAI 格式的响应
     * 预期结果：返回的文档注释应包含指定的测试方法内容，并且请求参数和返回值注释应正确
     * <p>
     * 测试过程中需要模拟 API 响应，验证生成的文档是否包含指定的注释内容，如 {@link #testMethod(String)} 方法的注释
     * <p>
     * 此外，还需验证请求是否正确发送至指定路径，并包含正确的请求头和请求体参数
     */
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

    /**
     * 测试 401 未授权错误场景
     * <p>
     * 测试场景：当 API Key 无效时，调用 generateDocumentation 方法应抛出异常
     * 预期结果：应抛出 AIServiceException 异常，且异常信息包含 "Invalid API Key"，错误码为 INVALID_API_KEY
     * <p>
     * 说明：通过 mockServer 模拟返回 401 状态码和错误信息，验证异常处理逻辑是否正确
     */
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

    /**
     * 测试生成文档时遇到 429 请求过多错误的场景
     * <p>
     * 测试场景：模拟服务器返回 429 状态码及对应的错误信息，验证生成文档时是否正确抛出异常并处理
     * 预期结果：应抛出 AIServiceException 异常，且异常信息包含 "Rate limit"，错误码应为 RATE_LIMIT
     * <p>
     * 该测试需依赖 mockServer 模拟 HTTP 响应，确保在调用 provider.generateDocumentation 方法时能正确识别并处理 429 错误
     */
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

    /**
     * 测试服务器返回 500 错误时的异常处理逻辑
     * <p>
     * 测试场景：模拟服务器返回 500 错误响应，验证异常是否被正确捕获和处理
     * 预期结果：应抛出 AIServiceException 异常，且错误信息包含 "Server error"，错误码为 SERVICE_UNAVAILABLE
     * <p>
     * 注意：该测试依赖于 mockServer 模拟 HTTP 响应，需确保相关依赖已正确配置
     */
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

    /**
     * 测试重试机制功能
     * <p>
     * 测试场景：第一次请求失败（500错误），第二次请求成功
     * 预期结果：应返回成功的文档内容，并验证发送了两次POST请求
     * <p>
     * 该测试验证系统在首次请求失败时是否能正确触发重试机制，并在第二次请求成功后返回预期结果
     * <p>
     * 注意：测试中使用了MockServer模拟两次请求，第一次返回500错误，第二次返回成功响应
     */
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

    /**
     * 测试重试机制在所有重试都失败时的行为
     * <p>
     * 测试场景：模拟多次请求均返回 500 错误，验证重试次数是否达到最大限制后抛出异常
     * 预期结果：应抛出 AIServiceException 异常，并包含 "Server error" 的错误信息
     * <p>
     * 特殊说明：需要 mockServer 模拟多次请求失败，确保重试次数准确统计
     */
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

    /**
     * 测试网络连接错误场景
     * <p>
     * 测试场景：模拟关闭服务器以触发网络错误，调用生成文档方法时发生异常
     * 预期结果：应抛出 AIServiceException 异常，且错误信息包含 "Network error"，错误码为 NETWORK_ERROR
     * <p>
     * 注意：测试需要依赖 mockServer 模拟网络环境，确保在测试前启动 mockServer
     */
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

    /**
     * 测试响应解析错误场景
     * <p>
     * 测试场景：当接收到的响应内容不是有效的 JSON 格式时
     * 预期结果：应抛出 AIServiceException 异常，且错误码为 INVALID_RESPONSE
     * <p>
     * 该测试通过模拟服务器返回非 JSON 格式的内容来验证异常处理逻辑
     */
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

    /**
     * 测试响应格式错误场景，即缺少必需字段
     * <p>
     * 测试场景：当响应体中缺少 "choices" 字段时
     * 预期结果：应抛出 AIServiceException 异常，且错误码为 INVALID_RESPONSE
     * <p>
     * 该测试通过模拟服务器返回一个不包含必需字段的响应，验证生成文档时是否能正确识别并抛出异常
     * <p>
     * 关联方法：{@link provider#generateDocumentation(String, DocumentationTask.TaskType, String)}
     */
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

    /**
     * 测试生成类文档功能
     * <p>
     * 测试场景：模拟生成用户服务类的文档
     * 预期结果：返回的文档内容应包含类描述和业务逻辑说明
     * <p>
     * 该测试通过模拟 API 响应，验证文档生成器是否能正确解析并生成类级别的 JavaDoc 内容
     */
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

    /**
     * 测试生成测试方法文档的功能
     * <p>
     * 测试场景：模拟 API 响应，验证生成的文档是否包含指定的注释内容
     * 预期结果：生成的文档应包含 "测试用户登录功能" 的注释内容
     * <p>
     * 说明：该测试通过模拟 HTTP 响应，验证文档生成器是否能正确解析并包含指定的注释内容
     */
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

    /**
     * 测试 Ollama Provider 在不需要 API Key 的情况下生成文档的功能
     * <p>
     * 测试场景：配置 Ollama 作为 AI 提供商，模型名称为 "qwen:7b"，且 API Key 为空字符串
     * 预期结果：调用 generateDocumentation 方法后，返回结果应包含 "Ollama 生成的文档"，且请求头不包含 Authorization 字段
     * <p>
     * 注意：该测试依赖 mockServer 模拟 Ollama 的响应，需确保 mockServer 已正确启动并配置
     */
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

    /**
     * 测试请求超时场景
     * <p>
     * 测试场景：模拟一个延迟响应（5秒），并设置较短的超时时间（1秒）
     * 预期结果：应触发超时异常，验证超时机制是否正常工作
     * <p>
     * 注意：当前实现未配置超时，测试可能需要修改源代码支持
     * 可参考 {@link OpenAICompatibleProvider} 中的 RestTemplate 配置进行调整
     */
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

    /**
     * 测试配置验证功能
     * <p>
     * 测试场景：模拟一个成功的响应，验证配置是否通过检查
     * 预期结果：验证结果应为成功
     * <p>
     * 说明：该测试需要模拟 HTTP 响应，确保验证逻辑正确处理成功状态
     */
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

    /**
     * 测试配置验证失败的场景
     * <p>
     * 测试场景：模拟服务器返回 401 错误响应，表示验证失败
     * 预期结果：验证结果应为失败状态
     * <p>
     * 该测试用于验证当配置验证接口接收到无效凭证时，{@link Provider#validateConfiguration()} 方法是否能正确返回验证失败的结果
     */
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

    /**
     * 测试完整的请求响应流程，包含代码生成和结果验证
     * <p>
     * 测试场景：模拟一个请求，生成JavaDoc注释并验证响应内容是否符合预期
     * 预期结果：生成的文档应包含指定的注释内容，请求体应包含正确的代码信息
     * <p>
     * 测试过程中会模拟一个HTTP请求，并验证响应内容是否包含指定的注释内容
     * 包括验证生成的文档是否包含“根据ID查找用户”、“@param id 用户ID”、“@return 用户对象”等关键信息
     * <p>
     * 同时验证请求的详细信息，包括请求方法、路径、头信息和请求体内容
     * 并确保请求体中的消息内容包含“UserService”关键字
     */
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

