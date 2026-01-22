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
 * <p> 反馈表 数据传输实体 (根据业务需求添加字段) </p>
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
@Schema(name = "反馈表-数传传输对象")
public class FeedbackDTO extends BaseDTO<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联的项目ID */
    @Schema(description = "关联的项目ID")
    private Long projectId;
    /** 标题 */
    @Schema(description = "标题")
    private String title;
    /** 详细描述 */
    @Schema(description = "详细描述")
    private String description;
    /** GitHub issues URL */
    @Schema(description = "GitHub issues URL")
    private String issuesUrl;
    /** GitHub issues ID */
    @Schema(description = "GitHub issues ID")
    private Long issuesId;
    /** 状态: Open, In Progress, Complete, Planned, Under Review */
    @Schema(description = "状态: Open, In Progress, Complete, Planned, Under Review")
    private String status;
    /** 优先级: Low, Medium, High */
    @Schema(description = "优先级: Low, Medium, High")
    private String priority;
    /** 点赞数 */
    @Schema(description = "点赞数")
    private Integer voteCount;
    /** 评论数 */
    @Schema(description = "评论数")
    private Integer commentCount;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private java.util.Date createTime;
}
