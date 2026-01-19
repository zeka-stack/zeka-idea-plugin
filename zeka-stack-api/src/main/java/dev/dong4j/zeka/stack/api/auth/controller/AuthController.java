package dev.dong4j.zeka.stack.api.auth.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import dev.dong4j.zeka.stack.api.auth.entity.dto.AuthStatusDTO;
import dev.dong4j.zeka.stack.api.auth.entity.dto.UserAccountDTO;
import dev.dong4j.zeka.stack.api.auth.entity.form.AuthDeviceUpdateForm;
import dev.dong4j.zeka.stack.api.auth.service.AuthService;
import dev.dong4j.zeka.starter.rest.annotation.RestControllerWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

/**
 * 登录状态与账户管理
 */
@Tag(name = "账号与会话接口")
@AllArgsConstructor
@RestControllerWrapper("/auth")
public class AuthController {
    /**
     * 提供认证服务的实例
     *
     * @see AuthService
     */
    private final AuthService authService;

    /**
     * 获取当前登录状态
     * <p> 根据请求头中的 {@code Authorization} 字段提取 Token, 并调用 {@link AuthService#getStatus(String)} 获取当前用户的登录状态. 若请求头为空或缺少 Token, 提取方法会返回
     * {@code null}, 本接口将返回 {@code null}.
     *
     * @param authorization 请求头中可选的 {@code Authorization} 字段, 支持「Bearer token」或仅包含 token 的形式, 允许为空
     * @return 当前登录用户的状态信息, 若 Token 无效或用户未登录则返回 {@code null}
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前登录状态")
    public AuthStatusDTO me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return authService.getStatus(extractToken(authorization));
    }

    /**
     * 注销当前登录会话
     * <p> 根据请求头中的 Authorization 信息提取 token, 并调用认证服务注销该登录会话.
     *
     * @param authorization 请求头中的 Authorization 字段, 用于获取 token, 可为空
     */
    @PostMapping("/logout")
    @Operation(summary = "注销登录")
    public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractToken(authorization));
    }

    /**
     * 更换设备标识符 (deviceId)
     * <p> 根据提供的授权令牌和设备信息更新用户的设备标识符.
     *
     * @param authorization 授权令牌, 用于验证用户身份
     * @return 更新后的用户账户信息
     */
    @PostMapping("/device")
    @Operation(summary = "更换 deviceId")
    public UserAccountDTO updateDevice(@RequestHeader(value = "Authorization") String authorization,
                                       @Valid @RequestBody AuthDeviceUpdateForm form) {
        return authService.updateDevice(extractToken(authorization), form.getDeviceId());
    }

    /**
     * 删除账号
     *
     * <p> 根据请求头中的 <code>Authorization</code> 令牌删除对应账号. 方法会先调用 <code>extractToken</code>
     * 对令牌进行处理, 然后将其传递给 {@code authService.deleteAccount} 执行删除操作.
     *
     * @param authorization 用户授权令牌, 必填
     */
    @DeleteMapping("/account")
    @Operation(summary = "删除账号")
    public void deleteAccount(@RequestHeader(value = "Authorization") String authorization) {
        authService.deleteAccount(extractToken(authorization));
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token
     * <p> 解析 HTTP Authorization 头, 提取 Bearer 令牌. 如果输入为空或不包含 Bearer 前缀,
     * 则直接返回处理后的字符串
     *
     * @param authorization HTTP 请求头中的 Authorization 值, 可能为 null
     * @return 提取出的 token 字符串, 如果输入为 null 或空白则返回 null
     */
    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (authorization.startsWith(prefix)) {
            return authorization.substring(prefix.length()).trim();
        }
        return authorization.trim();
    }
}
