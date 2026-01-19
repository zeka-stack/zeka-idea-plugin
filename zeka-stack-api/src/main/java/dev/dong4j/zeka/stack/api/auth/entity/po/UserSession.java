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
 * <p> 登录会话表 实体类  </p>
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
@TableName("user_session")
public class UserSession extends BaseWithTimePO<Long, UserSession> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;
    /** 用户 ID-表字段 */
    public static final String USER_ID = "user_id";
    /** 会话 token-表字段 */
    public static final String SESSION_TOKEN = "session_token";
    /** 过期时间-表字段 */
    public static final String EXPIRES_AT = "expires_at";

    /** 用户 ID */
    @TableField("`user_id`")
    private Long userId;
    /** 会话 token */
    @TableField("`session_token`")
    private String sessionToken;
    /** 过期时间 */
    @TableField("`expires_at`")
    private Date expiresAt;
}
