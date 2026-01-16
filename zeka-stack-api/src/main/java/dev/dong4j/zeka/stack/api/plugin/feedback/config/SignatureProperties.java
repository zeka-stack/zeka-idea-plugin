package dev.dong4j.zeka.stack.api.plugin.feedback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * 签名配置属性类
 * <p> 用于绑定和管理与签名相关的配置属性, 主要通过 {@code @ConfigurationProperties(prefix="feedback.signature")} 注解从配置文件中加载属性. 该类封装了签名客户端配置信息,
 * 包括客户端密钥映射和签名功能开关状态. 此类仅用于内部组件调用, 不负责请求处理, 旨在避免基础设施关注, 符合面向对象设计原则.</p>
 * <p> 主要属性包括:<br>
 * <pre>{@code
 * private Map<String, String> clients; // 客户端 ID 与密钥的映射关系
 * private boolean enabled;             // 是否启用签名功能
 * }</pre>
 * </p>
 * <p> 提供方法:<br>
 * <pre>{@code
 * public String getSecret(String clientId); // 根据客户端 ID 获取对应密钥
 * public boolean hasClient(String clientId); // 判断是否存在指定客户端 ID
 * }</pre>
 * </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "feedback.signature")
public class SignatureProperties {
    /** 客户端 ID 和 Secret 的映射, 格式为 feedback.signature.clients.idea-plugin="your-secret-key-here" */
    private Map<String, String> clients = new HashMap<>();

    /** 是否启用签名验证, 默认启用, 开发环境可设置为 false 禁用验证 */
    private boolean enabled = true;

    /**
     * 根据客户端 ID 获取对应的 Secret 密钥
     * <p>
     * 从配置的客户端映射中查找指定客户端 ID 对应的 Secret, 若不存在则返回 null
     *
     * @param clientId 客户端 ID
     * @return Secret 密钥, 如果客户端不存在则返回 null
     */
    public String getSecret(String clientId) {
        return clients.get(clientId);
    }

    /**
     * 检查指定客户端 ID 是否存在于配置中
     * <p>
     * 通过客户端 ID 判断是否存在对应的签名配置
     *
     * @param clientId 客户端 ID
     * @return 如果存在该客户端 ID 则返回 true, 否则返回 false
     */
    public boolean hasClient(String clientId) {
        return clients.containsKey(clientId);
    }
}

