package dev.dong4j.zeka.stack.feedback.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dev.dong4j.zeka.stack.feedback.dto.FeedbackResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GitHub GraphQL API 客户端
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class GitHubGraphQLClient {

    private static final String GITHUB_GRAPHQL_ENDPOINT = "https://api.github.com/graphql";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String githubToken;
    private final String repositoryId;

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
     * 创建讨论
     *
     * @param categoryId 类别 ID
     * @param title      标题
     * @param body       内容
     * @return 讨论信息
     * @throws IOException 如果请求失败
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
     * GraphQL 请求体
     */
    @Data
    private static class GraphQLRequest {
        private String query;
        private Map<String, Object> variables;
    }

    /**
     * GraphQL 响应体
     */
    @Data
    private static class GraphQLResponse {
        private JsonNode data;
        private JsonNode[] errors;
    }
}

