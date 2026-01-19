package dev.dong4j.zeka.stack.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.dong4j.zeka.kernel.common.api.R;
import dev.dong4j.zeka.kernel.common.api.Result;
import dev.dong4j.zeka.starter.launcher.ZekaStarter;

/**
 * ZekaStack API 应用启动类
 * <p> 继承自 ZekaStarter, 用于启动 ZekaStack API 服务, 负责初始化 Spring 应用上下文并加载核心配置.
 * 本类不处理具体请求, 仅作为应用入口, 确保基础设施层与业务逻辑层分离, 符合面向对象设计原则.
 * 适用于内部系统集成, 避免基础设施关注, 专注于业务逻辑的封装与执行.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@RestController
@EnableScheduling
@SpringBootApplication
public class ZekaStackApiApplication extends ZekaStarter {

    /**
     * 健康检查接口
     * <p> 用于检查服务是否正常运行, 返回 "OK" 表示服务健康
     *
     * @return 健康状态响应, 包含状态码和 "OK" 字符串
     */
    @GetMapping("/health")
    public Result<String> health() {
        return R.succeed("healthy");
    }

}
