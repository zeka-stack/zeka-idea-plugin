package dev.dong4j.zeka.stack.feedback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * GitHub 配置属性
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "github")
@Validated
public class GitHubProperties {

    /**
     * GitHub Token
     */
    @NotBlank(message = "GitHub Token 不能为空")
    private String token;

    /**
     * 仓库 ID
     */
    @NotBlank(message = "仓库 ID 不能为空")
    private String repositoryId;

    /**
     * 讨论类别 ID 映射
     */
    private Map<String, String> category;
}

