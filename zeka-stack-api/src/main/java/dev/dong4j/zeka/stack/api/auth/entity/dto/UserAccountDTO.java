package dev.dong4j.zeka.stack.api.auth.entity.dto;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.kernel.common.base.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * <p> GitHub 账号绑定表 数据传输实体 (根据业务需求添加字段) </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:54
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "GitHub 账号绑定表-数传传输对象")
public class UserAccountDTO extends BaseDTO<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** GitHub 用户 ID */
    @Schema(description = "GitHub 用户 ID")
    private Long githubId;
    /** GitHub 登录名 */
    @Schema(description = "GitHub 登录名")
    private String githubLogin;
    /** GitHub 显示名 */
    @Schema(description = "GitHub 显示名")
    private String githubName;
    /** 头像 URL */
    @Schema(description = "头像 URL")
    private String avatarUrl;
    /** 邮箱(可选) */
    @Schema(description = "邮箱(可选)")
    private String email;
    /** 用户角色, 用于标识账户权限等级 */
    @Schema(description = "用户角色, 用于标识账户权限等级")
    private String role;
    /** 设备 ID */
    @Schema(description = "设备 ID")
    private String deviceId;
    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    private Date lastLoginTime;
}
