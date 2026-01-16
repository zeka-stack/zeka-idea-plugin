package dev.dong4j.zeka.stack.api.plugin.feedback.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * HTTP 客户端配置类
 * <p> 用于在 Spring 应用中配置 OkHttpClient 和 ObjectMapper, 主要职责是为 HTTP 请求提供基础客户端实例和 JSON 序列化 / 反序列化支持, 不负责实际请求处理逻辑, 仅作为基础设施层的配置组件.</p>
 * <p> 该类通过 @Configuration 注解标识为配置类, 内部定义了两个 Bean: 一个是带有超时设置的 OkHttpClient 实例, 另一个是注册了 JavaTimeModule 的 ObjectMapper 实例, 用于处理日期时间类型的 JSON
 * 序列化.</p>
 * <p> 设计意图: 避免业务层与基础设施细节耦合, 遵循面向对象设计原则, 将客户端配置与请求处理逻辑分离.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Configuration
public class HttpClientConfig {

    /**
     * 创建并配置一个带有超时设置的 OkHttp 客户端实例
     * <p> 设置连接超时, 读取超时和写入超时均为 30 秒
     *
     * @return 配置好的 OkHttpClient 实例
     * @since 1.0.0
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    /**
     * 配置并返回一个经过注册 Java 8 时间日期模块的 ObjectMapper 实例
     * <p> 该方法使用 Jackson 的 ObjectMapper 来创建一个对象映射器, 并注册 JavaTimeModule 以支持 Java 8 的时间日期类型
     *
     * @return 配置好的 ObjectMapper 实例
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}

