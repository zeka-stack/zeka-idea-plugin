package dev.dong4j.zeka.stack.api.plugin.feedback.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GitHub Issues 客户端
 * <p>封装与 GitHub REST API 交互的逻辑, 用于创建 Issue.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
@Slf4j
@Component
public class GitHubIssueClient {
    private static final String API_BASE_URL = "https://api.github.com";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String githubToken;
    private final String repository;

    public GitHubIssueClient(
        OkHttpClient httpClient,
        ObjectMapper objectMapper,
        @Value("${github.token}") String githubToken,
        @Value("${github.repository:}") String repository) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.githubToken = githubToken;
        this.repository = repository;
    }

    public FeedbackResponse.IssueInfo createIssue(String title, String body) throws IOException {
        if (repository == null || repository.isBlank()) {
            throw new IOException("GitHub repository is not configured");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);

        String jsonBody = objectMapper.writeValueAsString(payload);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        String url = API_BASE_URL + "/repos/" + repository + "/issues";
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + githubToken)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/vnd.github+json")
            .post(requestBody)
            .build();

        log.debug("Sending issue request to {}", url);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("Issue response status: {}, body: {}", response.code(), responseBody);

            if (!response.isSuccessful()) {
                throw new IOException("GitHub API request failed with status " + response.code() + ": " + responseBody);
            }

            JsonNode jsonNode = objectMapper.readTree(responseBody);
            FeedbackResponse.IssueInfo info = FeedbackResponse.IssueInfo.builder()
                .id(jsonNode.hasNonNull("id") ? jsonNode.get("id").asText() : null)
                .number(jsonNode.get("number").asInt())
                .url(jsonNode.get("html_url").asText())
                .title(jsonNode.get("title").asText())
                .build();

            log.debug("Successfully created issue: {}", info.getUrl());
            return info;
        }
    }
}
