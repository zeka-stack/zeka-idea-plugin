package dev.dong4j.zeka.stack.api.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

import dev.dong4j.zeka.stack.api.auth.entity.dto.OAuthLoginDTO;
import dev.dong4j.zeka.stack.api.auth.service.AuthService;
import dev.dong4j.zeka.starter.rest.annotation.RestControllerWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

/**
 * OAuth 控制器
 */
@Tag(name = "OAuth 接口")
@AllArgsConstructor
@RestControllerWrapper("/oauth")
public class OAuthController {
    /** 用于处理 OAuth 认证逻辑的服务组件 */
    private final AuthService authService;

    /**
     * 获取 GitHub 登录授权地址
     * <p> 根据设备 ID 生成 GitHub OAuth 授权登录 URL, 并封装到 DTO 对象中返回
     *
     * @param deviceId 设备唯一标识, 用于绑定用户设备和授权状态
     * @return 包含 GitHub 登录授权地址的 DTO 对象
     */
    @GetMapping("/github/login")
    @Operation(summary = "GitHub 登录地址")
    public OAuthLoginDTO login(@RequestParam("deviceId") String deviceId) {
        String url = authService.buildGithubAuthorizeUrl(deviceId);
        return new OAuthLoginDTO(url);
    }

    /**
     * 处理 GitHub 登录回调请求
     * <p> 接收来自 GitHub 的授权回调, 处理用户登录信息, 并重定向到指定的 URL.
     *
     * @param code     授权码, 由 GitHub 返回, 用于获取访问令牌
     * @param state    用于防止 CSRF 攻击的随机字符串
     * @param response HTTP 响应对象, 用于设置重定向地址
     * @throws IOException 如果发生 I/O 错误时抛出
     */
    @GetMapping("/github/callback")
    @Operation(summary = "GitHub 登录回调")
    public void callback(@RequestParam("code") String code,
                         @RequestParam("state") String state,
                         HttpServletResponse response) throws IOException {
        String redirect = authService.handleGithubCallback(code, state);
        response.sendRedirect(redirect);
    }
}
