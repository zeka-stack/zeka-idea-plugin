package dev.dong4j.zeka.stack.feedback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 签名配置属性
 * <p>
 * 管理客户端 ID 和对应的 Secret。
 * 支持多个客户端，每个客户端有独立的 Secret。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.12.23
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "feedback.signature")
public class SignatureProperties {
    /**
     * 客户端 ID 和 Secret 的映射
     * <p>
     * 格式：
     * feedback:
     * signature:
     * clients:
     * idea-plugin: "your-secret-key-here"
     * other-client: "another-secret-key"
     */
    private Map<String, String> clients = new HashMap<>();

    /**
     * 是否启用签名验证
     * <p>
     * 默认启用，开发环境可以设置为 false 来禁用验证
     */
    private boolean enabled = true;

    /**
     * 获取客户端的 Secret
     *
     * @param clientId 客户端 ID
     * @return Secret，如果不存在返回 null
     */
    public String getSecret(String clientId) {
        return clients.get(clientId);
    }

    /**
     * 检查客户端是否存在
     *
     * @param clientId 客户端 ID
     * @return 如果存在返回 true
     */
    public boolean hasClient(String clientId) {
        return clients.containsKey(clientId);
    }
}

