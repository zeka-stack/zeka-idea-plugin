package dev.dong4j.zeka.stack.api.auth.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.starter.mybatis.base.BaseWithTimePO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p> GitHub 账号绑定表 实体类  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:54
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("user_account")
public class UserAccount extends BaseWithTimePO<Long, UserAccount> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;
    /** GitHub 用户 ID-表字段 */
    public static final String GITHUB_ID = "github_id";
    /** GitHub 登录名-表字段 */
    public static final String GITHUB_LOGIN = "github_login";
    /** GitHub 显示名-表字段 */
    public static final String GITHUB_NAME = "github_name";
    /** 头像 URL-表字段 */
    public static final String AVATAR_URL = "avatar_url";
    /** 邮箱(可选)-表字段 */
    public static final String EMAIL = "email";
    /** 角色标识, 用于标识用户权限或身份 */
    public static final String ROLE = "role";
    /** 设备 ID-表字段 */
    public static final String DEVICE_ID = "device_id";
    /** 最后登录时间-表字段 */
    public static final String LAST_LOGIN_TIME = "last_login_time";

    /** GitHub 用户 ID */
    @TableField("`github_id`")
    private Long githubId;
    /** GitHub 登录名 */
    @TableField("`github_login`")
    private String githubLogin;
    /** GitHub 显示名 */
    @TableField("`github_name`")
    private String githubName;
    /** 头像 URL */
    @TableField("`avatar_url`")
    private String avatarUrl;
    /** 邮箱(可选) */
    @TableField("`email`")
    private String email;
    /** 用户角色, 用于标识账户权限等级 */
    @TableField("`role`")
    private String role;
    /** 设备 ID */
    @TableField("`device_id`")
    private String deviceId;
    /** 最后登录时间 */
    @TableField("`last_login_time`")
    private Date lastLoginTime;
}
