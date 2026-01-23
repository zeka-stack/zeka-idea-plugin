package dev.dong4j.zeka.stack.api.manager.github.client;

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
    /** GitHub API 基础 URL */
    private static final String API_BASE_URL = "https://api.github.com";
    /** JSON 数据的媒体类型 */
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * HTTP 客户端实例, 用于发起网络请求
     *
     * @see OkHttpClient
     */
    private final OkHttpClient httpClient;
    /** JSON 序列化与反序列化工具, 用于将对象转换为 JSON 字符串或从 JSON 字符串解析为对象 */
    private final ObjectMapper objectMapper;
    /** GitHub Token 用于 API 认证 */
    private final String githubToken;
    /** 仓库名称, 用于指定 GitHub 上的仓库, 格式为 owner/repo, 如 zeka-stack/zeka-idea-plugin */
    private final String repository;

    /**
     * GitHubIssueClient 构造函数
     * <p> 用于初始化 GitHubIssueClient 实例, 注入必要的依赖和配置参数.</p>
     *
     * @param httpClient   用于发送 HTTP 请求的 OkHttpClient 实例
     * @param objectMapper 用于 JSON 序列化和反序列化的 ObjectMapper 实例
     * @param githubToken  GitHub API 访问所需的认证令牌
     * @param repository   GitHub 仓库名称, 格式为 "owner/repo", 若未配置则使用默认值
     */
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

    /**
     * 创建 GitHub Issue
     * <p> 向指定的 GitHub 仓库发送请求, 创建一个新的 Issue, 并返回创建后的 Issue 信息.</p>
     *
     * @param title Issue 的标题
     * @param body  Issue 的正文内容
     * @return 创建成功的 Issue 信息对象
     * @throws IOException 当网络请求失败或仓库未配置时抛出
     */
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

    /**
     * 更新 GitHub Issue 状态
     * <p>通过 PATCH 请求更新指定 Issue 的状态（open 或 closed）</p>
     *
     * @param owner       仓库所有者（如 zeka-stack）
     * @param repo        仓库名称（如 zeka-idea-plugin）
     * @param issueNumber Issue 编号
     * @param state       Issue 状态（"open" 或 "closed"）
     * @throws IOException 当请求失败时抛出
     */
    public void updateIssueState(String owner, String repo, Integer issueNumber, String state) throws IOException {
        if (owner == null || owner.isBlank()) {
            throw new IOException("GitHub owner is required");
        }
        if (repo == null || repo.isBlank()) {
            throw new IOException("GitHub repo is required");
        }
        if (issueNumber == null || issueNumber <= 0) {
            throw new IOException("Issue number is required and must be positive");
        }
        if (state == null || (!state.equals("open") && !state.equals("closed"))) {
            throw new IOException("Issue state must be 'open' or 'closed'");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("state", state);

        String jsonBody = objectMapper.writeValueAsString(payload);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        String url = API_BASE_URL + "/repos/" + owner + "/" + repo + "/issues/" + issueNumber;
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + githubToken)
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/vnd.github+json")
            .method("PATCH", requestBody)
            .build();

        log.debug("Updating issue state: {} to {}", url, state);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("Update issue response status: {}, body: {}", response.code(), responseBody);

            if (!response.isSuccessful()) {
                throw new IOException("GitHub API request failed with status " + response.code() + ": " + responseBody);
            }

            log.debug("Successfully updated issue {} to state: {}", issueNumber, state);
        }
    }
}
