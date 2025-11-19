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

/**
 * Nacos 安全代理
 * 用于处理 Nacos 服务器的认证和授权
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ConsumSecurityProxy {
    private static final Logger LOGGER = LogUtils.logger(ConsumSecurityProxy.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String accessKey;
    private final String secretKey;
    private final String ramRoleName;
    private final Properties properties;
    private final ConsumServerHttpAgent httpAgent;
    private final AtomicReference<String> accessToken = new AtomicReference<>("");
    private volatile long tokenExpireAt = -1L;
    private volatile boolean globalAdmin;
    private String username;
    private String password;

    public ConsumSecurityProxy(Properties properties, ConsumServerHttpAgent httpAgent) {
        this.properties = properties;
        this.httpAgent = httpAgent;
        this.accessKey = properties.getProperty(PropertyKeyConst.ACCESS_KEY);
        this.secretKey = properties.getProperty(PropertyKeyConst.SECRET_KEY);
        this.ramRoleName = properties.getProperty(PropertyKeyConst.RAM_ROLE_NAME);
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

        // AccessKey/SecretKey 模式
        try {
            if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
                // 使用 AccessKey 和 SecretKey 登录（如需签名可在此扩展）
                LOGGER.info("Login with AccessKey and SecretKey: {}", accessKey);
                return true;
            }
            if (StringUtils.isNotBlank(ramRoleName)) {
                // 使用 RAM 角色登录
                LOGGER.info("Login with RAM role: {}", ramRoleName);
                return true;
            }

            if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                LOGGER.info("No authentication configured, using anonymous access");
                return true;
            }

            Map<String, String> params = new HashMap<>();
            params.put("username", username);
            params.put("password", password);
            HttpRestResult<String> result = httpAgent.httpPost("/v1/auth/users/login", null, params, "");
            if (result.getCode() == 200 && StringUtils.isNotBlank(result.getData())) {
                JsonNode node = OBJECT_MAPPER.readTree(result.getData());
                String token = node.path("accessToken").asText("");
                long ttl = node.path("tokenTtl").asLong(18000L);
                this.globalAdmin = node.path("globalAdmin").asBoolean(false);
                if (StringUtils.isNotBlank(token)) {
                    accessToken.set(token);
                    tokenExpireAt = System.currentTimeMillis() + ttl * 1000L - 5_000L;
                    LOGGER.info("Login success, token ttl {}s", ttl);
                    return true;
                }
            }
            LOGGER.error("Login failed, code: {}, msg: {}", result.getCode(), result.getMessage());
        } catch (Exception e) {
            LOGGER.error("Login failed: {}", e.getMessage(), e);
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
     * 获取 AccessKey
     *
     * @return AccessKey
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * 获取 SecretKey
     *
     * @return SecretKey
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * 是否为全局管理员
     *
     * @return 全局管理员标识
     */
    public boolean isGlobalAdmin() {
        return globalAdmin;
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
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return false;
        }
        return login(username, password);
    }

    private boolean isTokenValid() {
        String token = accessToken.get();
        return StringUtils.isNotBlank(token) && tokenExpireAt > System.currentTimeMillis();
    }
}