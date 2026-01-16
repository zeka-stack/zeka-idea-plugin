package dev.dong4j.zeka.stack.api.plugin.feedback.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 * <p>用于配置 Spring MVC 的跨域资源共享 (CORS) 策略, 允许指定路径下的 API 接口被跨域访问.
 * 本类不负责请求处理, 仅作为基础设施配置组件, 避免业务逻辑与基础设施关注点耦合.
 * 适用于需要开放 API 接口供前端或其他服务调用的场景, 支持多种 HTTP 方法和请求头.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置跨域资源共享 (CORS)
     * <p> 允许所有来源访问 `/api/**` 路径下的接口, 并支持 `GET`,`POST`,`PUT`,`DELETE` 和 `OPTIONS` 方法.
     * <p> 允许所有请求头, 并设置预检请求的最大缓存时间为 3600 秒.
     *
     * @param registry CorsRegistry 对象, 用于注册 CORS 配置规则
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

