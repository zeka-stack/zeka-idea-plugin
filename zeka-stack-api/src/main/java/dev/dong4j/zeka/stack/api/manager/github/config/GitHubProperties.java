package dev.dong4j.zeka.stack.api.manager.github.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * GitHub 配置属性类
 * <p> 用于绑定和管理 GitHub 相关的配置属性, 包括访问令牌, 仓库 ID 以及分类映射等. 该类通过 {@code @ConfigurationProperties} 注解从配置文件中读取前缀为 "github" 的属性, 并通过 {@code
 *
 * @author dong4j
 * @version 1.0.0
 * @Validated} 进行参数校验, 确保关键字段如令牌和仓库 ID 非空.</p>
 *     <p> 本类为 Spring Boot 的配置类, 配合 {@code @Component} 注册为 Spring 容器中的 Bean, 供其他组件通过依赖注入方式获取配置值. 不负责请求处理, 仅作为配置数据载体, 避免基础设施关注, 符合面向对象设计原则
 *     .</p>
 *     <p> 示例配置片段:</p>
 *     <pre>{@code
 *     github.token=your-github-token
 *     github.repositoryId=your-repo-id
 *     github.category.key1=value1
 *     }</pre>
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "github")
@Validated
public class GitHubProperties {

    /** GitHub Token 值, 不能为空 */
    @NotBlank(message = "GitHub Token 不能为空")
    private String token;

    /** 仓库 ID, 用于标识 GitHub 仓库, 不能为空 */
    @NotBlank(message = "仓库 ID 不能为空")
    private String repositoryId;

    /** 仓库全名 (owner/repo), 用于 GitHub Issues API */
    private String repository;

    /**
     * 讨论类别 ID 映射
     * <p> 用于存储不同讨论类别对应的 ID 映射关系, 支持动态配置和扩展
     *
     * @see <a href="https://docs.github.com/en/rest/reference/issues#list-repository-issues">GitHub Issues API</a>
     */
    private Map<String, String> category;
}
