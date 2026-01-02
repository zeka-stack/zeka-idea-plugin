package dev.dong4j.zeka.stack.feedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 反馈服务器应用启动类
 * <p> 该类是 Spring Boot 应用程序的入口点, 负责启动整个反馈服务器应用
 * <p> 通过调用 SpringApplication.run 方法启动应用, 传入当前类作为参数
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@SpringBootApplication
public class FeedbackServerApplication {

    /**
     * 应用程序入口点
     * <p> 启动 Spring Boot 应用程序
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FeedbackServerApplication.class, args);
    }
}

