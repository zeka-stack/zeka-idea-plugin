package dev.dong4j.zeka.stack.api.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * GitHub OAuth 配置
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "oauth.github")
public class GitHubOAuthProperties {
    /** GitHub OAuth Client ID */
    private String clientId;
    /** GitHub OAuth Client Secret */
    private String clientSecret;
    /** GitHub OAuth 回调地址 */
    private String redirectUri;
    /** 授权地址 */
    private String authorizeUrl = "https://github.com/login/oauth/authorize";
    /** Access Token 地址 */
    private String accessTokenUrl = "https://github.com/login/oauth/access_token";
    /** GitHub API 地址 */
    private String apiBaseUrl = "https://api.github.com";
    /** 授权 scope */
    private String scope = "read:user user:email";
    /** 登录成功跳转 */
    private String successRedirect;
    /** 登录失败跳转 */
    private String failureRedirect;
    /** 会话有效期(天) */
    private int sessionDays = 30;
}
