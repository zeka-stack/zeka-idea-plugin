package dev.dong4j.zeka.stack.feedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Feedback Server 主应用类
 * <p>
 * 为 IntelliJ IDEA 插件提供反馈接口的后端服务
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
public class FeedbackServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbackServerApplication.class, args);
    }
}

