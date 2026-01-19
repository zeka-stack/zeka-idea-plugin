package dev.dong4j.zeka.stack.api.auth.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.io.IOException;

import dev.dong4j.zeka.stack.api.auth.config.GitHubOAuthProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * GitHub OAuth 客户端
 */
@Slf4j
@Component
@AllArgsConstructor
public class GitHubOAuthClient {
    /** 用于发送 HTTP 请求的客户端 */
    private final OkHttpClient httpClient;
    /** ObjectMapper 对象, 用于 JSON 序列化和反序列化 */
    private final ObjectMapper objectMapper;
    /** GitHub OAuth 相关配置属性, 用于获取客户端 ID, 密钥, 授权地址等关键参数 */
    private final GitHubOAuthProperties properties;

    /**
     * 交换授权码以获取访问令牌
     * <p> 通过发送请求到 GitHub 的授权服务器, 使用客户端 ID, 客户端密钥, 授权码和重定向 URI 获取访问令牌.
     *
     * @param code 授权码
     * @return 访问令牌
     * @throws IOException 如果请求失败或无法解析响应, 则抛出此异常
     */
    public String exchangeAccessToken(String code) throws IOException {
        RequestBody body = new FormBody.Builder()
            .add("client_id", properties.getClientId())
            .add("client_secret", properties.getClientSecret())
            .add("code", code)
            .add("redirect_uri", properties.getRedirectUri())
            .build();

        Request request = new Request.Builder()
            .url(properties.getAccessTokenUrl())
            .addHeader("Accept", "application/json")
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("GitHub token response status: {}, body: {}", response.code(), responseBody);
            if (!response.isSuccessful()) {
                throw new IOException("GitHub token request failed with status " + response.code() + ": " + responseBody);
            }
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.path("access_token").asText(null);
        }
    }

    /**
     * 获取 GitHub 用户信息
     * <p> 通过指定的 accessToken 调用 GitHub API <code>/user</code> 接口, 解析返回的 JSON 数据并封装为 {@link GitHubUser} 对象.
     *
     * @param accessToken GitHub OAuth 访问令牌
     * @return {@link GitHubUser} 对象, 包含 id,login,name,avatarUrl 与 email 等字段
     * @throws IOException 当 HTTP 请求失败或 JSON 解析异常时抛出
     */
    public GitHubUser fetchUser(String accessToken) throws IOException {
        Request request = new Request.Builder()
            .url(properties.getApiBaseUrl() + "/user")
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept", "application/vnd.github+json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("GitHub user response status: {}, body: {}", response.code(), responseBody);
            if (!response.isSuccessful()) {
                throw new IOException("GitHub user request failed with status " + response.code() + ": " + responseBody);
            }
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            GitHubUser user = new GitHubUser();
            user.setId(jsonNode.path("id").asLong());
            user.setLogin(jsonNode.path("login").asText());
            user.setName(jsonNode.path("name").asText(""));
            user.setAvatarUrl(jsonNode.path("avatar_url").asText(""));
            user.setEmail(jsonNode.path("email").asText(""));
            return user;
        }
    }

    /**
     * 获取用户的主电子邮件地址
     * <p> 通过访问 GitHub API 获取用户的电子邮件列表, 并从中找出主电子邮件地址.
     * 如果没有找到主电子邮件地址, 则返回第一个电子邮件地址. 如果没有电子邮件地址, 则返回空字符串.
     *
     * @param accessToken GitHub 访问令牌
     * @return 主电子邮件地址, 如果没有找到则返回空字符串
     * @throws IOException 网络请求失败或解析 JSON 失败时抛出
     */
    public String fetchPrimaryEmail(String accessToken) throws IOException {
        Request request = new Request.Builder()
            .url(properties.getApiBaseUrl() + "/user/emails")
            .addHeader("Authorization", "Bearer " + accessToken)
            .addHeader("Accept", "application/vnd.github+json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.debug("GitHub emails response status: {}", response.code());
            if (!response.isSuccessful()) {
                return "";
            }
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.isArray()) {
                for (JsonNode item : jsonNode) {
                    if (item.path("primary").asBoolean(false)) {
                        return item.path("email").asText("");
                    }
                }
                if (jsonNode.size() > 0) {
                    return jsonNode.get(0).path("email").asText("");
                }
            }
            return "";
        }
    }

    /**
     * GitHub 用户数据类
     * <p> 用于封装从 GitHub API 获取的用户信息, 包括用户 ID, 登录名, 姓名, 头像 URL 和邮箱地址等字段.
     * 该类为数据类 (data class), 主要用于数据传输和序列化, 支持自动实现 equals,hashCode,toString 等方法.
     * 通常在与 GitHub OAuth 服务交互时, 用于接收和传递用户数据.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.19
     * @since 1.0.0
     */
    @Data
    public static class GitHubUser {
        /** 用户的唯一标识符 */
        private long id;
        /** 登录名 */
        private String login;
        /** 用户全名 */
        private String name;
        /** 头像 URL 地址 */
        private String avatarUrl;
        /** 用户电子邮箱地址 */
        private String email;
    }
}
