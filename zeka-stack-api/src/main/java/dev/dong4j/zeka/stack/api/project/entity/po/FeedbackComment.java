package dev.dong4j.zeka.stack.api.project.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.starter.mybatis.base.BasePO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import static dev.dong4j.zeka.starter.mybatis.base.AuditTime.CREATE_TIME;


/**
 * <p> 反馈评论表 实体类  </p>
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
@TableName("feedback_comment")
public class FeedbackComment extends BasePO<Long, FeedbackComment> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;
    /** 关联的需求ID-表字段 */
    public static final String FEEDBACK_ID = "feedback_id";
    /** 评论内容-表字段 */
    public static final String CONTENT = "content";

    /** 关联的需求ID */
    @TableField("`feedback_id`")
    private Long feedbackId;
    /** 评论内容 */
    @TableField("`content`")
    private String content;
    /**
     * 创建时间, 仅在插入时自动填充
     * <p> 用于记录评论创建的精确时间戳 </p>
     *
     * @see FieldFill
     */
    @TableField(value = CREATE_TIME, fill = FieldFill.INSERT)
    private Date createTime;
}
