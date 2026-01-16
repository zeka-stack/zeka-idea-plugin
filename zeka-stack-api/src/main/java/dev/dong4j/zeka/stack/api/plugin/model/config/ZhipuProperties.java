package dev.dong4j.zeka.stack.api.plugin.model.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 知普模型配置属性类
 * <p>用于绑定和管理与知普 (Zhipu) 相关的 API 配置属性, 包括基础 URL、模型服务地址和默认模型列表. 该类通过 Spring 的 {@code @ConfigurationProperties} 注解绑定配置文件中前缀为 "zhipu" 的属性, 并通过
 * {@code @Data} 自动生成 getter/setter/toString/equals/hashCode 方法, 便于在服务层中直接使用配置值.</p>
 * <p>该类为内部组件使用, 由服务组件调用, 不负责请求处理逻辑, 仅提供配置数据的封装与检索功能, 符合面向对象设计原则, 避免基础设施层关注.</p>
 * <p>配置示例:</p>
 * <pre>{@code
 * zhipu:
 *   api:
 *     base-url: https://open.bigmodel.cn/api/paas/v4
 *     models-url: ${zhipu.api.base-url}/models
 *   default-models:
 *     - glm-4.6
 *     - glm-4.5
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhipu")
public class ZhipuProperties {

    /**
     * API 配置
     */
    private Api api = new Api();

    /**
     * 默认模型列表
     */
    private List<String> defaultModels = new ArrayList<>();

    /**
     * API 配置内部类
     */
    @Data
    public static class Api {
        /**
         * 基础 URL
         */
        private String baseUrl;

        /**
         * 模型列表 URL
         */
        private String modelsUrl;
    }
}
