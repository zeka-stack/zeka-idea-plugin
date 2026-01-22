package dev.dong4j.zeka.stack.api.project.service;

import dev.dong4j.zeka.stack.api.project.entity.dto.FeedbackCommentDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackCommentForm;
import dev.dong4j.zeka.stack.api.project.entity.po.FeedbackComment;
import dev.dong4j.zeka.starter.mybatis.service.BaseService;

/**
 * <p> 反馈评论表 服务接口 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
public interface FeedbackCommentService extends BaseService<FeedbackComment> {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    FeedbackCommentDTO detail(Long id);

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void create(FeedbackCommentForm form);

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void edit(FeedbackCommentForm form);

}

