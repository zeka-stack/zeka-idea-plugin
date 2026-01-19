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
 * <p> 登录会话表 入参实体 (根据业务需求添加字段) </p>
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
@Schema(name = "登录会话表-新增与更新")
public class UserSessionForm extends BaseForm<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @Schema(description = "用户 ID")
    @NotNull(message = "[用户 ID] 必填)")
    private Long userId;
    /** 会话 token */
    @Schema(description = "会话 token")
    @NotBlank(message = "[会话 token] 必填)")
    private String sessionToken;
    /** 过期时间 */
    @Schema(description = "过期时间")
    @NotNull(message = "[过期时间] 必填)")
    private Date expiresAt;
}
