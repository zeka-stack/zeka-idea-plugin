package dev.dong4j.zeka.stack.idea.plugin.common.ai.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.UUID;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.DeviceIdGenerator;
import dev.dong4j.zeka.stack.idea.plugin.kit.PluginUtil;

/**
 * Dong4j 云端短期 Token 管理器
 */
public final class Dong4jAuthTokenManager {
    private static final String AUTH_URL = "https://api.dong4j.site/auth";
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 3600L;
    private static final long REFRESH_SKEW_MILLIS = 30_000L;
    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 20_000;

    private static final Object LOCK = new Object();
    private static volatile String cachedToken;
    private static volatile long expireAtMillis;

    private Dong4jAuthTokenManager() {
    }

    @NotNull
    public static String getToken() throws AIServiceException {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < (expireAtMillis - REFRESH_SKEW_MILLIS)) {
            return cachedToken;
        }
        synchronized (LOCK) {
            now = System.currentTimeMillis();
            if (cachedToken != null && now < (expireAtMillis - REFRESH_SKEW_MILLIS)) {
                return cachedToken;
            }
            TokenResponse token = fetchToken();
            cachedToken = token.token;
            expireAtMillis = now + (token.expiresInSeconds * 1000L);
            return cachedToken;
        }
    }

    @NotNull
    private static TokenResponse fetchToken() throws AIServiceException {
        JsonObject payload = new JsonObject();
        payload.addProperty("deviceId", DeviceIdGenerator.generateDeviceId());
        payload.addProperty("timestamp", System.currentTimeMillis());
        payload.addProperty("nonce", UUID.randomUUID().toString().replace("-", ""));
        payload.addProperty("pluginId", EngineContents.PLUGIN_ID);
        String version = PluginUtil.getVersion(EngineContents.PLUGIN_ID);
        if (version != null && !version.isBlank()) {
            payload.addProperty("pluginVersion", version);
        }

        String responseBody;
        try {
            responseBody = HttpRequests.post(AUTH_URL, "application/json")
                .tuner(connection -> {
                    HttpURLConnection conn = (HttpURLConnection) connection;
                    conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
                    conn.setReadTimeout(READ_TIMEOUT_MILLIS);
                })
                .connect(request -> {
                    request.write(payload.toString());
                    return request.readString();
                });
        } catch (HttpRequests.HttpStatusException e) {
            AIServiceException.ErrorCode code = switch (e.getStatusCode()) {
                case 401, 403 -> AIServiceException.ErrorCode.INVALID_API_KEY;
                case 429 -> AIServiceException.ErrorCode.RATE_LIMIT;
                case 500, 502, 503, 504 -> AIServiceException.ErrorCode.SERVICE_UNAVAILABLE;
                default -> AIServiceException.ErrorCode.INVALID_RESPONSE;
            };
            throw new AIServiceException("鉴权失败: " + e.getMessage(), code, e);
        } catch (IOException e) {
            throw new AIServiceException("鉴权网络错误: " + e.getMessage(), AIServiceException.ErrorCode.NETWORK_ERROR, e);
        }

        TokenResponse token = parseTokenResponse(responseBody);
        if (token.token == null || token.token.isBlank()) {
            throw new AIServiceException("鉴权响应无效", AIServiceException.ErrorCode.INVALID_RESPONSE);
        }
        return token;
    }

    @NotNull
    private static TokenResponse parseTokenResponse(@NotNull String responseBody) throws AIServiceException {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            String token = null;
            if (json.has("access_token")) {
                token = json.get("access_token").getAsString();
            } else if (json.has("token")) {
                token = json.get("token").getAsString();
            }
            long expiresIn = DEFAULT_EXPIRES_IN_SECONDS;
            if (json.has("expires_in")) {
                expiresIn = json.get("expires_in").getAsLong();
            } else if (json.has("expiresIn")) {
                expiresIn = json.get("expiresIn").getAsLong();
            }
            if (expiresIn <= 0) {
                expiresIn = DEFAULT_EXPIRES_IN_SECONDS;
            }
            return new TokenResponse(token, expiresIn);
        } catch (Exception e) {
            throw new AIServiceException("鉴权响应解析失败", AIServiceException.ErrorCode.INVALID_RESPONSE, e);
        }
    }

    private record TokenResponse(String token, long expiresInSeconds) {
    }
}
