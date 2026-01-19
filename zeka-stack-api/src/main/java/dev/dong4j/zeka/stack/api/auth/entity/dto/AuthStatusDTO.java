package dev.dong4j.zeka.stack.api.auth.entity.dto;

import java.io.Serial;

import dev.dong4j.zeka.kernel.common.base.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 登录状态
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "登录状态")
public class AuthStatusDTO extends BaseDTO<Long> {
    /**
     * 序列化版本号
     * <p> 用于支持序列化的版本控制
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否已登录 */
    @Schema(description = "是否已登录")
    private boolean loggedIn;

    /** 用户信息, 包含用户的基本资料和权限配置 <a href="https://example.com/user-account"> 用户账户详情 </a> */
    @Schema(description = "用户信息")
    private UserAccountDTO user;
}
