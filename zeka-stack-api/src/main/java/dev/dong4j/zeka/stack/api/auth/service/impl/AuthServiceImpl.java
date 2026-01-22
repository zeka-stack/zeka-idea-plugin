package dev.dong4j.zeka.stack.api.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.auth.client.GitHubOAuthClient;
import dev.dong4j.zeka.stack.api.auth.config.GitHubOAuthProperties;
import dev.dong4j.zeka.stack.api.auth.dao.UserAccountMapper;
import dev.dong4j.zeka.stack.api.auth.dao.UserSessionMapper;
import dev.dong4j.zeka.stack.api.auth.entity.converter.UserAccountConverter;
import dev.dong4j.zeka.stack.api.auth.entity.dto.AuthSessionDTO;
import dev.dong4j.zeka.stack.api.auth.entity.dto.AuthStatusDTO;
import dev.dong4j.zeka.stack.api.auth.entity.dto.UserAccountDTO;
import dev.dong4j.zeka.stack.api.auth.entity.po.UserAccount;
import dev.dong4j.zeka.stack.api.auth.entity.po.UserSession;
import dev.dong4j.zeka.stack.api.auth.security.OAuthStateCache;
import dev.dong4j.zeka.stack.api.auth.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth 与会话服务
 */
@Slf4j
@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    /** 用于生成随机字节的安全随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** GitHub OAuth 客户端, 负责与 GitHub 的授权交互 */
    private final GitHubOAuthClient gitHubOAuthClient;
    /** OAuth 相关配置属性, 用于获取授权 URL, 客户端 ID, 重定向地址等关键参数 */
    private final GitHubOAuthProperties properties;
    /**
     * OAuth 状态缓存对象
     * <p> 用于存储授权过程中的状态令牌和设备 ID 的映射关系
     *
     * @see OAuthStateCache
     */
    private final OAuthStateCache stateCache;
    /** 用户账号数据访问对象, 用于操作用户账号相关数据库记录 */
    private final UserAccountMapper userAccountMapper;
    /**
     * 用户会话数据访问接口
     * <p> 用于操作用户会话表, 包括查询, 创建, 删除等数据库操作
     *
     * @see UserSession
     */
    private final UserSessionMapper userSessionMapper;

    /**
     * 生成 GitHub 授权跳转 URL
     * <p> 根据设备 ID 生成包含状态令牌的 GitHub OAuth 授权 URL, 用于引导用户跳转至 GitHub 授权页面 </p>
     * <p>URL 包含客户端 ID, 重定向地址, 作用域及状态令牌, 状态令牌用于后续回调时校验设备上下文 </p>
     *
     * @param deviceId 设备唯一标识, 不能为空且不能为空字符串
     * @return 生成的 GitHub 授权跳转 URL, 包含所有必要参数
     */
    @Override
    public String buildGithubAuthorizeUrl(String deviceId) {
        BaseCodes.PARAM_VERIFY_ERROR.isTrue(deviceId != null && !deviceId.isBlank(), "deviceId 不能为空");
        String state = generateToken(20);
        stateCache.store(state, deviceId);
        return properties.getAuthorizeUrl()
               + "?client_id=" + urlEncode(properties.getClientId())
               + "&redirect_uri=" + urlEncode(properties.getRedirectUri())
               + "&scope=" + urlEncode(properties.getScope())
               + "&state=" + urlEncode(state);
    }

    /**
     * 处理 GitHub OAuth 回调, 完成会话创建并构造返回值.
     *
     * <p> 调用 {@link #createSessionForGithub(String, String)} 生成 {@link AuthSessionDTO}, 随后根据配置中的
     * {@link GitHubOAuthProperties#getSuccessRedirect()} 或 {@link GitHubOAuthProperties#getFailureRedirect()}
     * 构造最终返回字符串:
     * <pre>{@code
     * 1. 若成功并且不配置成功回调地址, 则返回会话 token 字符串.
     * 2. 若成功并已配置成功回调地址, 则返回形如
     *    successRedirect?token=SESSION_TOKEN 的完整 URL.
     * 3. 若创建会话失败或异常抛出, 则使用失败回调地址构造错误信息:
     *    failureRedirect?error=github_login_failed
     *    若未配置失败回调地址, 直接返回字符串 "github_login_failed".
     * }</pre>
     *
     * @param code  GitHub 回调中返回的授权码, 用于换取 access token
     * @param state GitHub OAuth 过程中传递的 state, 用于绑定设备 ID
     * @return 若成功则返回会话 token 或包含 token 的 URL; 若失败则返回错误提示字符串
     */
    @Override
    public String handleGithubCallback(String code, String state) {
        try {
            AuthSessionDTO session = createSessionForGithub(code, state);
            String redirect = properties.getSuccessRedirect();
            String token = session.getSessionToken();
            if (redirect == null || redirect.isBlank()) {
                return token;
            }
            return appendQuery(redirect, "token", token);
        } catch (Exception e) {
            log.warn("GitHub callback failed", e);
            String redirect = properties.getFailureRedirect();
            if (redirect == null || redirect.isBlank()) {
                return "github_login_failed";
            }
            return appendQuery(redirect, "error", "github_login_failed");
        }
    }

    /**
     * 处理 GitHub OAuth 授权回调, 创建用户会话
     * <p> 通过授权码和状态参数, 交换访问令牌, 获取用户信息, 并创建或更新用户账户及会话.
     * 若过程中发生异常, 记录日志并返回 null 表示登录失败.
     *
     * @param code  授权码, 不能为空且非空格
     * @param state 状态参数, 用于验证请求合法性, 不能为空且非空格
     * @return 用户会话信息对象, 包含会话令牌, 过期时间及用户数据; 若登录失败则返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthSessionDTO createSessionForGithub(String code, String state) {
        BaseCodes.PARAM_VERIFY_ERROR.isTrue(code != null && !code.isBlank(), "code 不能为空");
        String deviceId = stateCache.consume(state);
        BaseCodes.PARAM_VERIFY_ERROR.isTrue(deviceId != null && !deviceId.isBlank(), "state 无效或已过期");
        try {
            String accessToken = gitHubOAuthClient.exchangeAccessToken(code);
            BaseCodes.PARAM_VERIFY_ERROR.isTrue(accessToken != null && !accessToken.isBlank(), "GitHub 授权失败");
            GitHubOAuthClient.GitHubUser gitHubUser = gitHubOAuthClient.fetchUser(accessToken);
            String email = gitHubUser.getEmail();
            if (email == null || email.isBlank()) {
                email = gitHubOAuthClient.fetchPrimaryEmail(accessToken);
            }
            return upsertAccountAndSession(gitHubUser, email, deviceId);
        } catch (Exception e) {
            log.warn("GitHub OAuth callback failed", e);
            BaseCodes.OPTION_FAILURE.isTrue(false, "GitHub 登录失败");
            return null;
        }
    }

    /**
     * 获取当前用户的认证状态
     * <p> 通过会话令牌解析用户账号信息, 返回认证状态和用户数据
     *
     * @param token 会话令牌
     * @return 包含认证状态和用户信息的 AuthStatusDTO 对象
     */
    @Override
    public AuthStatusDTO getStatus(String token) {
        UserAccount account = resolveAccountByToken(token);
        if (account == null) {
            return new AuthStatusDTO(false, null);
        }
        return new AuthStatusDTO(true, UserAccountConverter.INSTANCE.p2d(account));
    }

    /**
     * 用户登出操作
     * <p> 根据会话令牌查找并删除对应的用户会话记录. 若会话不存在或已过期, 则直接返回, 不执行任何操作.
     *
     * @param token 会话令牌, 用于定位用户会话记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logout(String token) {
        UserSession session = resolveSession(token);
        if (session == null) {
            return;
        }
        userSessionMapper.deleteById(session.getId());
    }

    /**
     * 更新设备 ID
     * <p> 根据给定的令牌更新用户的设备 ID. 如果设备 ID 已经被其他用户绑定, 则抛出错误.
     *
     * @param token    用户令牌, 用于验证用户身份
     * @param deviceId 新的设备 ID
     * @return 更新后的用户账户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccountDTO updateDevice(String token, String deviceId) {
        BaseCodes.PARAM_VERIFY_ERROR.isTrue(deviceId != null && !deviceId.isBlank(), "deviceId 不能为空");
        UserAccount account = resolveAccountByToken(token);
        BaseCodes.DATA_ERROR.notNull(account, "未登录");

        UserAccount existing = userAccountMapper.selectOne(new QueryWrapper<UserAccount>().eq("device_id", deviceId));
        if (existing != null && !existing.getId().equals(account.getId())) {
            BaseCodes.PARAM_VERIFY_ERROR.isTrue(false, "deviceId 已被绑定");
        }

        account.setDeviceId(deviceId);
        userAccountMapper.updateById(account);
        return UserAccountConverter.INSTANCE.p2d(account);
    }

    /**
     * 删除用户账户及其关联的会话信息
     * <p> 根据提供的 token 解析出对应的用户会话, 若会话有效则删除该用户账户及所有相关会话记录.
     *
     * @param token 用户登录凭证, 用于获取对应用户会话信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(String token) {
        UserSession session = resolveSession(token);
        BaseCodes.DATA_ERROR.notNull(session, "未登录");
        UserAccount account = userAccountMapper.selectById(session.getUserId());
        if (account != null) {
            userAccountMapper.deleteById(account.getId());
        }
        userSessionMapper.delete(new QueryWrapper<UserSession>().eq("user_id", session.getUserId()));
    }

    /**
     * 根据 GitHub 用户信息, 邮箱和设备 ID 上下文创建或更新用户账户并生成会话
     * <p> 如果用户不存在则创建新账户, 如果存在则更新账户信息并绑定设备 ID; 随后创建会话并返回会话数据
     * <p> 此方法确保设备 ID 唯一绑定, 且用户账户信息与 GitHub 账户同步
     *
     * @param user     GitHub 用户信息对象, 包含用户 ID, 登录名, 名称, 头像等
     * @param email    用户邮箱, 可为空, 若为空则尝试从 GitHub 获取主邮箱
     * @param deviceId 设备唯一标识, 用于绑定用户账户, 确保唯一性
     * @return 包含会话令牌, 过期时间及用户账户信息的会话对象 {@code AuthSessionDTO}
     * @since 1.0
     */
    private AuthSessionDTO upsertAccountAndSession(GitHubOAuthClient.GitHubUser user, String email, String deviceId) {
        UserAccount account = userAccountMapper.selectOne(new QueryWrapper<UserAccount>().eq("github_id", user.getId()));
        Date now = new Date();
        if (account == null) {
            UserAccount byDevice = userAccountMapper.selectOne(new QueryWrapper<UserAccount>().eq("device_id", deviceId));
            BaseCodes.PARAM_VERIFY_ERROR.isTrue(byDevice == null, "deviceId 已被绑定");
            account = new UserAccount();
            account.setGithubId(user.getId());
            account.setGithubLogin(user.getLogin());
            account.setGithubName(user.getName() == null ? "" : user.getName());
            account.setAvatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
            account.setEmail(email == null ? "" : email);
            account.setRole(""); // GitHub 登录时默认为空字符串
            account.setDeviceId(deviceId);
            account.setLastLoginTime(now);
            int inserted = userAccountMapper.insert(account);
            BaseCodes.OPTION_FAILURE.isTrue(inserted == 1, "创建账号失败");
        } else {
            if (!deviceId.equals(account.getDeviceId())) {
                UserAccount byDevice = userAccountMapper.selectOne(new QueryWrapper<UserAccount>().eq("device_id", deviceId));
                if (byDevice != null && !byDevice.getId().equals(account.getId())) {
                    BaseCodes.PARAM_VERIFY_ERROR.isTrue(false, "deviceId 已被绑定");
                }
                account.setDeviceId(deviceId);
            }
            account.setGithubLogin(user.getLogin());
            account.setGithubName(user.getName() == null ? "" : user.getName());
            account.setAvatarUrl(user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
            if (email != null && !email.isBlank()) {
                account.setEmail(email);
            }
            account.setLastLoginTime(now);
            userAccountMapper.updateById(account);
        }

        String sessionToken = generateToken(32);
        Date expiresAt = Date.from(Instant.now().plus(properties.getSessionDays(), ChronoUnit.DAYS));
        UserSession session = new UserSession();
        session.setUserId(account.getId());
        session.setSessionToken(sessionToken);
        session.setExpiresAt(expiresAt);
        int saved = userSessionMapper.insert(session);
        BaseCodes.OPTION_FAILURE.isTrue(saved == 1, "创建会话失败");

        return new AuthSessionDTO(sessionToken, expiresAt.getTime(), UserAccountConverter.INSTANCE.p2d(account));
    }

    /**
     * 通过会话令牌解析用户账号
     * <p> 查找与给定会话令牌关联的用户会话, 如果会话存在且未过期, 则返回对应的用户账号; 否则返回 null.
     *
     * @param token 会话令牌
     * @return 对应的用户账号对象, 如果会话无效或不存在则返回 null
     */
    private UserAccount resolveAccountByToken(String token) {
        UserSession session = resolveSession(token);
        if (session == null) {
            return null;
        }
        return userAccountMapper.selectById(session.getUserId());
    }

    /**
     * 根据会话令牌解析并返回对应的用户会话对象
     * <p> 首先验证令牌是否为空或无效, 然后查询数据库获取会话记录. 如果会话已过期, 则删除该记录并返回 null.
     *
     * @param token 会话令牌 (session token)
     * @return 对应的 UserSession 对象, 若不存在或已过期则返回 null
     */
    private UserSession resolveSession(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        UserSession session = userSessionMapper.selectOne(new QueryWrapper<UserSession>().eq("session_token", token));
        if (session == null) {
            return null;
        }
        Date expiresAt = session.getExpiresAt();
        if (expiresAt != null && expiresAt.before(new Date())) {
            userSessionMapper.deleteById(session.getId());
            return null;
        }
        return session;
    }

    /**
     * 生成指定长度的随机十六进制字符串.
     * <p> 调用 {@link java.security.SecureRandom} 生成 {@code length} 个随机字节,
     * 再将每个字节转换为两位十六进制字符串并拼接得到最终结果.
     *
     * @param length 随机字节数
     * @return 长度为 {@code length * 2} 的十六进制字符串
     */
    private static String generateToken(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(length * 2);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 对字符串进行 URL 编码
     * <p> 将输入的字符串使用 UTF-8 编码方式进行 URL 编码, 若输入为 null 则视为空字符串进行处理.
     *
     * @param value 需要编码的字符串, 若为 null 则视为空字符串
     * @return URL 编码后的字符串
     */
    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /**
     * 向 URL 添加查询参数
     * <p> 根据 URL 是否已包含查询字符串 (即是否含有 '?'), 决定使用 '&' 或 '?' 作为连接符, 将指定的键值对追加到 URL 上.
     *
     * @param url   原始 URL
     * @param key   要添加的查询参数的键
     * @param value 要添加的查询参数的值
     * @return 新的包含附加查询参数的 URL
     */
    private static String appendQuery(String url, String key, String value) {
        String connector = url.contains("?") ? "&" : "?";
        return url + connector + key + "=" + urlEncode(value);
    }
}
