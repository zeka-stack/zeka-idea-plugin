package dev.dong4j.zeka.stack.api.project.entity.dto;

import java.io.Serial;

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
 * <p> 反馈评论表 数据传输实体 (根据业务需求添加字段) </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "反馈评论表-数传传输对象")
public class FeedbackCommentDTO extends BaseDTO<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联的需求ID */
    @Schema(description = "关联的需求ID")
    private Long feedbackId;
    /** 评论内容 */
    @Schema(description = "评论内容")
    private String content;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private java.util.Date createTime;
}
