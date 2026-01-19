package dev.dong4j.zeka.stack.api.auth.entity.dto;

import java.io.Serial;

import dev.dong4j.zeka.kernel.common.base.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * OAuth 登录地址
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OAuth 登录地址")
public class OAuthLoginDTO extends BaseDTO<Long> {
    /** serialVersionUID 用于序列化版本控制, 确保类的兼容性 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** GitHub 授权地址 */
    @Schema(description = "GitHub 授权地址")
    private String url;
}
