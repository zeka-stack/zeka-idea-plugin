package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.client.utils.LogUtils;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import dev.dong4j.zeka.stack.idea.plugin.nacos.model.LocalRegistryConstants;
import lombok.Getter;

/**
 * Nacos 安全代理
 * 用于处理 Nacos 服务器的认证和授权
 *
 * @author dong4j
 * @since 1.0.0
 */
@SuppressWarnings("D")
public class ConsumSecurityProxy {
    private static final Logger LOGGER = LogUtils.logger(ConsumSecurityProxy.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Getter
    private final String accessKey;

    @Getter
    private final String secretKey;
    private final ConsumServerHttpAgent httpAgent;
    private final AtomicReference<String> accessToken = new AtomicReference<>("");
    private volatile long tokenExpireAt = -1L;
    private String username;
    private String password;

    public ConsumSecurityProxy(Properties properties, ConsumServerHttpAgent httpAgent) {
        this.httpAgent = httpAgent;
        this.accessKey = properties.getProperty(PropertyKeyConst.ACCESS_KEY);
        this.secretKey = properties.getProperty(PropertyKeyConst.SECRET_KEY);
    }

    /**
     * 登录到 Nacos 服务器
     *
     * @param username 用户名
     * @param password 密码
     * @return 是否登录成功
     */
    public synchronized boolean login(String username, String password) {
        this.username = username;
        this.password = password;

        // 若已经拥有 token 且未过期, 直接复用
        if (isTokenValid()) {
            return true;
        }

        try {
            if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
                // 使用 AccessKey 和 SecretKey 登录（如需签名可在此扩展）
                LOGGER.info("Login with AccessKey and SecretKey: {}", accessKey);
                return true;
            }

            // 如果使用特定的用户名登录, 且本地 nacos 正常启动,则不校验密码
            if (StringUtils.isNotBlank(username) && username.equals(LocalRegistryConstants.LOCAL_USERNAME)) {
                accessToken.set("not_need_auth");
                tokenExpireAt = System.currentTimeMillis() + 18000L * 1000L - 5_000L;
                return true;
            }

            // 即使用户名和密码为空，也尝试调用登录接口
            // 如果服务器允许匿名访问，会返回成功；如果需要认证，会返回失败
            Map<String, String> params = new HashMap<>();
            // 如果用户名或密码为空，使用空字符串，让服务器决定是否允许匿名访问
            params.put("username", StringUtils.isBlank(username) ? "" : username);
            params.put("password", StringUtils.isBlank(password) ? "" : password);
            HttpRestResult<String> result = httpAgent.httpPost("/v1/auth/users/login", null, params, "");
            if (result.getCode() == 200 && StringUtils.isNotBlank(result.getData())) {
                JsonNode node = OBJECT_MAPPER.readTree(result.getData());
                if (node.has("code")) {
                    int apiCode = node.path("code").asInt(0);
                    if (apiCode != 0) {
                        LOGGER.debug("Login failed, apiCode: {}, msg: {}", apiCode, node.path("message").asText());
                        return false;
                    }
                    node = node.path("data");
                }
                String token = node.path("accessToken").asText("");
                long ttl = node.path("tokenTtl").asLong(18000L);
                if (StringUtils.isNotBlank(token)) {
                    accessToken.set(token);
                    tokenExpireAt = System.currentTimeMillis() + ttl * 1000L - 5_000L;
                    LOGGER.debug("Login success, token ttl {}s", ttl);
                    return true;
                }
            }
            LOGGER.debug("Login failed, code: {}, msg: {}", result.getCode(), result.getMessage());
        } catch (Exception e) {
            LOGGER.debug("Login failed: {}", e.getMessage(), e);
        }
        accessToken.set("");
        tokenExpireAt = -1L;
        return false;
    }

    /**
     * 获取认证头
     *
     * @return 认证头 Map
     */
    public Map<String, String> getAuthorizationHeader() {
        Map<String, String> headers = new HashMap<>();

        String token = accessToken.get();
        if (StringUtils.isNotBlank(token)) {
            headers.put("Authorization", "Bearer " + token);
        }
        if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
            headers.put("accessKey", accessKey);
            headers.put("secretKey", secretKey);
        }

        return headers;
    }

    /**
     * 确保 token 有效
     *
     * @return 登录是否有效
     */
    public boolean ensureLogin() {
        if (isTokenValid()) {
            return true;
        }
        // 即使用户名和密码为空，也尝试登录，让服务器决定是否允许匿名访问
        return login(username, password);
    }

    private boolean isTokenValid() {
        String token = accessToken.get();
        return StringUtils.isNotBlank(token) && tokenExpireAt > System.currentTimeMillis();
    }
}