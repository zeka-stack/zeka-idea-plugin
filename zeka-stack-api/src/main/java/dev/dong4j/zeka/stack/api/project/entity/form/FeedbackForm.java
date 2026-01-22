package dev.dong4j.zeka.stack.api.project.entity.form;

import java.io.Serial;

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
 * <p> 反馈表 入参实体 (根据业务需求添加字段) </p>
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
@Schema(name = "反馈表-新增与更新")
public class FeedbackForm extends BaseForm<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联的项目ID */
    @Schema(description = "关联的项目ID")
    @NotNull(message = "[关联的项目ID] 必填)")
    private Long projectId;
    /** 标题 */
    @Schema(description = "标题")
    @NotBlank(message = "[标题] 必填)")
    private String title;
    /** 详细描述 */
    @Schema(description = "详细描述")
    @NotNull(message = "[详细描述] 必填)")
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
    @NotBlank(message = "[优先级: Low, Medium, High] 必填)")
    private String priority;
    /** 点赞数 */
    @Schema(description = "点赞数")
    private Integer voteCount;
    /** 评论数 */
    @Schema(description = "评论数")
    private Integer commentCount;
}
