package dev.dong4j.zeka.stack.api.manager.github.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GitHub GraphQL 客户端
 * <p>封装与 GitHub GraphQL API 交互的逻辑, 用于创建讨论 (Discussion) 等操作. 该类不负责请求的底层处理, 而是通过依赖注入的 OkHttpClient 和 ObjectMapper 完成请求构建, 发送与响应解析.
 * 适用于服务组件内部调用, 用于与 GitHub 仓库的讨论功能集成.</p>
 * <p>主要职责包括:</p>
 * <ul>
 *   <li>构建 GraphQL 查询语句并填充变量</li>
 *   <li>通过 HTTP POST 请求发送 GraphQL 请求至 GitHub API</li>
 *   <li>解析响应数据, 提取讨论信息并封装为 {@code FeedbackResponse.DiscussionInfo} 对象</li>
 *   <li>处理错误响应, 包括 HTTP 状态码异常和 GraphQL 层级错误</li>
 * </ul>
 * <p>依赖外部组件:</p>
 * <ul>
 *   <li>{@code OkHttpClient}: 用于发起网络请求</li>
 *   <li>{@code ObjectMapper}: 用于 JSON 数据序列化与反序列化</li>
 * </ul>
 * <p>设计模式:</p>
 * <ul>
 *   <li>依赖注入(Dependency Injection): 通过构造函数注入所需依赖</li>
 *   <li>面向对象封装: 将请求构建, 发送, 解析逻辑封装在类内, 对外提供简洁接口</li>
 * </ul>
 * <p>使用示例:</p>
 * <pre>{@code
 * GitHubGraphQLClient client = new GitHubGraphQLClient(okHttpClient, objectMapper, "token", "repoId");
 * FeedbackResponse.DiscussionInfo info = client.createDiscussion("categoryId", "标题", "内容");
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
@Component
public class GitHubGraphQLClient {

    /** GitHub GraphQL API 的请求端点地址 <a href="https://api.github.com/graphql">https://api.github.com/graphql</a> */
    private static final String GITHUB_GRAPHQL_ENDPOINT = "https://api.github.com/graphql";
    /** JSON 内容类型, 用于 HTTP 请求体格式声明 */
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /** HTTP 客户端, 用于执行 GraphQL 请求 */
    private final OkHttpClient httpClient;
    /** JSON 序列化与反序列化工具, 用于处理 GraphQL 请求体和响应体数据 */
    private final ObjectMapper objectMapper;
    /** GitHub 个人访问令牌, 用于认证 GraphQL 请求 */
    private final String githubToken;
    /** 仓库唯一标识符, 用于指定 GitHub 仓库的 ID, 用于 GraphQL 查询和操作 */
    private final String repositoryId;

    /**
     * 初始化 GitHub GraphQL 客户端
     * <p> 通过指定的 HTTP 客户端,JSON 序列化器,GitHub 个人访问令牌和仓库 ID 初始化客户端实例
     *
     * @param httpClient   用于发送 HTTP 请求的 OkHttpClient 实例
     * @param objectMapper 用于 JSON 序列化和反序列化的 ObjectMapper 实例
     * @param githubToken  GitHub 个人访问令牌, 用于认证 GraphQL 请求
     * @param repositoryId GitHub 仓库的唯一标识符, 用于指定操作的仓库
     */
    public GitHubGraphQLClient(
        OkHttpClient httpClient,
        ObjectMapper objectMapper,
        @Value("${github.token}") String githubToken,
        @Value("${github.repository-id}") String repositoryId) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.githubToken = githubToken;
        this.repositoryId = repositoryId;
    }

    /**
     * 创建 GitHub 讨论
     * <p> 通过 GraphQL API 向指定仓库的类别中创建一条讨论, 返回创建成功的讨论信息
     * <p> 请求体包含 GraphQL 查询语句和变量, 通过 HTTP POST 请求发送至 GitHub API
     *
     * @param categoryId 讨论所属类别的 ID
     * @param title      讨论标题
     * @param body       讨论内容
     * @return 创建成功的讨论信息, 包含 ID, 编号,URL 和标题
     * @throws IOException 当请求失败或响应格式异常时抛出, 例如 HTTP 状态码非 200 或 GraphQL 错误
     */
    public FeedbackResponse.DiscussionInfo createDiscussion(String categoryId, String title, String body)
        throws IOException {
        String mutation = """
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
                }
              }
            }
            """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("repositoryId", repositoryId);
        variables.put("categoryId", categoryId);
        variables.put("title", title);
        variables.put("body", body);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", mutation);
        requestBody.put("variables", variables);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        RequestBody bodyRequest = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
            .url(GITHUB_GRAPHQL_ENDPOINT)
            .addHeader("Authorization", "Bearer " + githubToken)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/vnd.github+json")
            .post(bodyRequest)
            .build();

        log.debug("Sending GraphQL request: {}", jsonBody);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("GraphQL response status: {}, body: {}", response.code(), responseBody);

            if (!response.isSuccessful()) {
                throw new IOException("GitHub API request failed with status " + response.code() + ": " + responseBody);
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 检查 GraphQL 错误
            if (jsonNode.has("errors")) {
                JsonNode errors = jsonNode.get("errors");
                String errorMessage = errors.isArray() && errors.size() > 0
                                      ? errors.get(0).get("message").asText()
                                      : "Unknown GraphQL error";
                throw new IOException("GraphQL error: " + errorMessage);
            }

            // 解析响应
            JsonNode data = jsonNode.get("data");
            if (data == null || !data.has("createDiscussion")) {
                throw new IOException("Invalid response format: missing createDiscussion");
            }

            JsonNode discussionNode = data.get("createDiscussion").get("discussion");
            if (discussionNode == null) {
                throw new IOException("Invalid response format: missing discussion");
            }

            FeedbackResponse.DiscussionInfo discussionInfo = FeedbackResponse.DiscussionInfo.builder()
                .id(discussionNode.get("id").asText())
                .number(discussionNode.get("number").asInt())
                .url(discussionNode.get("url").asText())
                .title(discussionNode.get("title").asText())
                .build();

            log.debug("Successfully created discussion: {}", discussionInfo.getUrl());
            return discussionInfo;
        }
    }

    /**
     * GraphQL 请求数据类
     * <p> 用于封装 GraphQL 查询语句及变量映射, 作为内部数据结构传递给 GraphQL 客户端或服务层进行查询处理.
     * 该类仅用于数据传输, 不负责实际的请求发送或响应解析, 符合面向对象设计原则, 避免将基础设施关注点混入业务逻辑.
     * 通常在内部服务调用中作为参数传递, 支持灵活的查询结构和变量绑定.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.16
     * @since 1.0.0
     */
    @Data
    private static class GraphQLRequest {
        /** GraphQL 查询语句 */
        private String query;
        /** GraphQL 请求的变量参数, 用于传递查询时所需的动态数据. */
        private Map<String, Object> variables;
    }

    /**
     * GraphQL 响应数据结构类
     * <p> 用于封装 GraphQL 查询的响应结果, 包含业务数据字段 <code>data</code> 和错误信息数组 <code>errors</code>.
     * 该类为数据载体, 不负责请求处理或业务逻辑, 仅用于内部数据结构传递.
     * 适用于 GraphQL 客户端或服务端解析响应时使用, 确保数据结构标准化.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.16
     * @since 1.0.0
     */
    @Data
    private static class GraphQLResponse {
        /** GraphQL 响应中的数据部分, 包含实际查询结果 */
        private JsonNode data;
        /** GraphQL 响应中的错误信息数组, 包含所有处理过程中发生的错误详情 */
        private JsonNode[] errors;
    }
}

