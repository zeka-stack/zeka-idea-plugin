package dev.dong4j.zeka.stack.feedback.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * <p>该类用于配置 Spring MVC 的跨域资源共享 (CORS) 设置, 允许指定路径下的 API 请求从任意来源发起,
 * 并支持多种 HTTP 方法和请求头. 通过实现 WebMvcConfigurer 接口, 自定义 CORS 行为.
 *
 * <p>具体配置如下:
 * - 允许的路径:`/api/**`
 * - 允许的源:`*`(即所有来源)
 * - 允许的方法:`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
 * - 允许的头部:`*`(即所有头部)
 * - 预检请求缓存时间:3600 秒
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置跨域资源共享 (CORS)
     * <p> 允许所有来源访问 /api/** 路径下的接口, 并支持 GET,POST,PUT,DELETE 和 OPTIONS 方法
     * <p> 允许所有请求头, 并设置预检请求的最大存活时间为 3600 秒
     *
     * @param registry CorsRegistry 对象, 用于配置 CORS 规则
     * @since hello.world
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}

