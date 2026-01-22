package dev.dong4j.zeka.stack.api.project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * Webhook 配置属性类
 * <p> 用于绑定和管理 GitHub webhook 相关的配置属性，包括 secret 到 project key 的映射关系
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.23
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "webhook")
public class WebhookProperties {

    /**
     * Secret 到 Project Key 的映射
     * <p> 格式：webhook.secrets.7b3cbbfb030d46979aa1a21f6c1cab2e=zeka-idea-plugin
     */
    private Map<String, String> secrets = new HashMap<>();

    /**
     * 根据 secret 获取对应的 project key
     *
     * @param secret webhook secret
     * @return project key，如果不存在则返回 null
     */
    public String getProjectKey(String secret) {
        return secrets.get(secret);
    }

    /**
     * 检查 secret 是否存在
     *
     * @param secret webhook secret
     * @return 如果存在返回 true，否则返回 false
     */
    public boolean hasSecret(String secret) {
        return secrets.containsKey(secret);
    }
}
