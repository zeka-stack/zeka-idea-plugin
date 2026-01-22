package dev.dong4j.zeka.stack.api.project.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;

import dev.dong4j.zeka.starter.mybatis.base.BaseWithTimePO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p> 反馈表 实体类  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("feedback")
public class Feedback extends BaseWithTimePO<Long, Feedback> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;
    /** 关联的项目ID-表字段 */
    public static final String PROJECT_ID = "project_id";
    /** 标题-表字段 */
    public static final String TITLE = "title";
    /** 详细描述-表字段 */
    public static final String DESCRIPTION = "description";
    /** GitHub issues URL-表字段 */
    public static final String ISSUES_URL = "issues_url";
    /** GitHub issues ID-表字段 */
    public static final String ISSUES_ID = "issues_id";
    /** 状态: Open, In Progress, Complete, Planned, Under Review-表字段 */
    public static final String STATUS = "status";
    /** 优先级: Low, Medium, High-表字段 */
    public static final String PRIORITY = "priority";
    /** 点赞数-表字段 */
    public static final String VOTE_COUNT = "vote_count";
    /** 评论数-表字段 */
    public static final String COMMENT_COUNT = "comment_count";

    /** 关联的项目ID */
    @TableField("`project_id`")
    private Long projectId;
    /** 标题 */
    @TableField("`title`")
    private String title;
    /** 详细描述 */
    @TableField("`description`")
    private String description;
    /** GitHub issues URL */
    @TableField("`issues_url`")
    private String issuesUrl;
    /** GitHub issues ID */
    @TableField("`issues_id`")
    private Long issuesId;
    /** 状态: Open, In Progress, Complete, Planned, Under Review */
    @TableField("`status`")
    private String status;
    /** 优先级: Low, Medium, High */
    @TableField("`priority`")
    private String priority;
    /** 点赞数 */
    @TableField("`vote_count`")
    private Integer voteCount;
    /** 评论数 */
    @TableField("`comment_count`")
    private Integer commentCount;
}
