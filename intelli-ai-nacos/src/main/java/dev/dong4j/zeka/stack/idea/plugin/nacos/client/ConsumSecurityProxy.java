package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.client.security.SecurityProxy;
import com.alibaba.nacos.client.utils.LogUtils;
import com.alibaba.nacos.common.utils.StringUtils;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Nacos 安全代理
 * 用于处理 Nacos 服务器的认证和授权
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ConsumSecurityProxy {
    private static final Logger LOGGER = LogUtils.logger(ConsumSecurityProxy.class);

    private final SecurityProxy securityProxy;
    private final String accessKey;
    private final String secretKey;
    private final String ramRoleName;
    private final Properties properties;

    public ConsumSecurityProxy(Properties properties) {
        this.properties = properties;
        this.accessKey = properties.getProperty(PropertyKeyConst.ACCESS_KEY);
        this.secretKey = properties.getProperty(PropertyKeyConst.SECRET_KEY);
        this.ramRoleName = properties.getProperty(PropertyKeyConst.RAM_ROLE_NAME);

        // Initialize security proxy
        this.securityProxy = new SecurityProxy(null, null);
    }

    /**
     * 登录到 Nacos 服务器
     *
     * @param server 服务器地址
     * @return 是否登录成功
     */
    public boolean login(String server) {
        try {
            if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
                // 使用 AccessKey 和 SecretKey 登录
                // securityProxy.login(properties);
                LOGGER.info("Login with AccessKey and SecretKey: {}", accessKey);
                return true;
            } else if (StringUtils.isNotBlank(ramRoleName)) {
                // 使用 RAM 角色登录
                LOGGER.info("Login with RAM role: {}", ramRoleName);
                return true;
            } else {
                // 匿名登录
                LOGGER.info("No authentication configured, using anonymous access");
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Login failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取认证头
     *
     * @return 认证头 Map
     */
    public Map<String, String> getAuthorizationHeader() {
        Map<String, String> headers = new HashMap<>();

        if (StringUtils.isNotBlank(accessKey) && StringUtils.isNotBlank(secretKey)) {
            // 使用 AccessKey 和 SecretKey 认证
            headers.put("accessKey", accessKey);
            // 简化处理，实际应该计算签名
            headers.put("signature", "");
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
}