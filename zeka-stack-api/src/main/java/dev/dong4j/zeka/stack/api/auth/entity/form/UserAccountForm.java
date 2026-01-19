package dev.dong4j.zeka.stack.api.auth.entity.form;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.kernel.common.base.BaseForm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * <p> GitHub 账号绑定表 入参实体 (根据业务需求添加字段) </p>
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
@Schema(name = "GitHub 账号绑定表-新增与更新")
public class UserAccountForm extends BaseForm<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** GitHub 用户 ID */
    @Schema(description = "GitHub 用户 ID")
    @NotNull(message = "[GitHub 用户 ID] 必填)")
    private Long githubId;
    /** GitHub 登录名 */
    @Schema(description = "GitHub 登录名")
    @NotBlank(message = "[GitHub 登录名] 必填)")
    private String githubLogin;
    /** GitHub 显示名 */
    @Schema(description = "GitHub 显示名")
    @NotBlank(message = "[GitHub 显示名] 必填)")
    private String githubName;
    /** 头像 URL */
    @Schema(description = "头像 URL")
    @NotBlank(message = "[头像 URL] 必填)")
    private String avatarUrl;
    /** 邮箱(可选) */
    @Schema(description = "邮箱(可选)")
    @NotBlank(message = "[邮箱(可选)] 必填)")
    private String email;
    /** 设备 ID */
    @Schema(description = "设备 ID")
    @NotBlank(message = "[设备 ID] 必填)")
    private String deviceId;
    /** 最后登录时间 */
    @Schema(description = "最后登录时间")
    @NotNull(message = "[最后登录时间] 必填)")
    private Date lastLoginTime;
}
