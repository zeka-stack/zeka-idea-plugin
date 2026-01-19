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
 * <p> 登录会话表 数据传输实体 (根据业务需求添加字段) </p>
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
@Schema(name = "登录会话表-数传传输对象")
public class UserSessionDTO extends BaseDTO<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @Schema(description = "用户 ID")
    private Long userId;
    /** 会话 token */
    @Schema(description = "会话 token")
    private String sessionToken;
    /** 过期时间 */
    @Schema(description = "过期时间")
    private Date expiresAt;
}
