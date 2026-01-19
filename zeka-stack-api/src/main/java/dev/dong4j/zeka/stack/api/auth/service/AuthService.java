package dev.dong4j.zeka.stack.api.auth.service;

import dev.dong4j.zeka.stack.api.auth.entity.dto.AuthSessionDTO;
import dev.dong4j.zeka.stack.api.auth.entity.dto.AuthStatusDTO;
import dev.dong4j.zeka.stack.api.auth.entity.dto.UserAccountDTO;

/**
 * 认证服务接口
 * <p> 提供与身份验证相关的方法, 支持构建 GitHub 授权 URL, 处理回调, 获取认证状态, 登出, 更新设备信息和创建会话等功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
public interface AuthService {
    /**
     * 构建 GitHub 授权 URL
     * <p> 根据设备 ID 构建 GitHub 授权页面的 URL, 用户可以通过该 URL 进行授权操作
     *
     * @param deviceId 设备 ID
     * @return GitHub 授权 URL
     */
    String buildGithubAuthorizeUrl(String deviceId);

    /**
     * 处理 GitHub OAuth 回调请求.
     * <p> 接收 GitHub 返回的授权码 {@code code} 与状态码 {@code state}, 根据其完成授权流程并返回访问令牌.
     *
     * @param code  GitHub 返回的授权码
     * @param state 之前请求时生成的状态值, 用于防止 CSRF 攻击
     * @return 对应的访问令牌 (如 JWT 或自定义 token)
     */
    String handleGithubCallback(String code, String state);

    /**
     * 获取当前认证状态
     * <p> 根据提供的 token 查询并返回用户的认证状态信息
     *
     * @param token 用户的认证令牌
     * @return 包含认证状态信息的数据对象
     */
    AuthStatusDTO getStatus(String token);

    /**
     * 注销用户会话
     * <p>根据提供的令牌 (token) 注销用户的当前会话, 释放相关资源
     *
     * @param token 用户会话令牌, 用于标识要注销的会话
     */
    void logout(String token);

    /**
     * 更新用户的设备信息
     * <p> 根据提供的令牌和新设备 ID, 更新用户账户关联的设备信息
     *
     * @param token    用户身份验证令牌
     * @param deviceId 新设备的唯一标识符
     * @return 更新后的用户账户信息
     */
    UserAccountDTO updateDevice(String token, String deviceId);

    /**
     * 删除账户
     * <p> 根据访问令牌删除用户账户信息, 该操作不可逆
     *
     * @param token 访问令牌, 用于验证用户身份
     */
    void deleteAccount(String token);

    /**
     * 为 GitHub 登录创建会话
     * <p> 根据授权码和状态参数, 生成并返回对应的认证会话数据
     *
     * @param code  授权码, 用于换取访问令牌
     * @param state 状态参数, 用于防止 CSRF 攻击
     * @return 认证会话数据对象, 包含会话标识, 过期时间等信息
     */
    AuthSessionDTO createSessionForGithub(String code, String state);
}
