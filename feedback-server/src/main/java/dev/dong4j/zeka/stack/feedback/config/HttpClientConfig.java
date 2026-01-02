package dev.dong4j.zeka.stack.feedback.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * HTTP 客户端配置类
 * <p> 该类使用 Spring 的 {@code @Configuration} 注解来配置 HTTP 客户端和 JSON 序列化工具.
 * <p> 主要功能包括:
 * <ul>
 * <li> 配置一个带有超时设置的 OkHttp 客户端实例 </li>
 * <li> 配置一个支持 Java 8 时间日期类型的 Jackson ObjectMapper 实例 </li>
 * </ul>
 * <p> 通过 Spring 的依赖注入机制, 可以在其他组件中使用这些配置好的客户端和服务.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
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

