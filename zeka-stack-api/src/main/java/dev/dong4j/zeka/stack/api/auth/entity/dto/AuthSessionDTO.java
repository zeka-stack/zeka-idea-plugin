package dev.dong4j.zeka.stack.api.auth.entity.dto;

import java.io.Serial;

import dev.dong4j.zeka.kernel.common.base.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 登录会话响应
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "登录会话响应")
public class AuthSessionDTO extends BaseDTO<Long> {
    /** 序列化版本标识符, 用于兼容序列化版本控制 */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话 token
     *
     * @see @Schema
     */
    @Schema(description = "会话 token")
    private String sessionToken;

    /**
     * 过期时间, 以毫秒为单位.
     * 表示当前会话在该时间后失效.
     */
    @Schema(description = "过期时间(毫秒)")
    private Long expiresAt;

    /**
     * 用户信息
     * <p> 包含当前登录用户的账户详细信息
     *
     * @see UserAccountDTO
     */
    @Schema(description = "用户信息")
    private UserAccountDTO user;
}
