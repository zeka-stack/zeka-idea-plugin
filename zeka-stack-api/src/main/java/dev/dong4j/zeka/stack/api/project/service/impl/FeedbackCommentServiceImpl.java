package dev.dong4j.zeka.stack.api.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.project.dao.FeedbackCommentMapper;
import dev.dong4j.zeka.stack.api.project.dao.FeedbackMapper;
import dev.dong4j.zeka.stack.api.project.entity.converter.FeedbackCommentConverter;
import dev.dong4j.zeka.stack.api.project.entity.dto.FeedbackCommentDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackCommentForm;
import dev.dong4j.zeka.stack.api.project.entity.po.Feedback;
import dev.dong4j.zeka.stack.api.project.entity.po.FeedbackComment;
import dev.dong4j.zeka.stack.api.project.service.FeedbackCommentService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 反馈评论表 服务接口实现类 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Slf4j
@Service
@AllArgsConstructor
public class FeedbackCommentServiceImpl extends BaseServiceImpl<FeedbackCommentMapper, FeedbackComment> implements FeedbackCommentService {

    /** 反馈主数据映射器, 用于操作反馈相关数据 */
    private final FeedbackMapper feedbackMapper;

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public FeedbackCommentDTO detail(Long id) {
        final FeedbackComment po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return FeedbackCommentConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(FeedbackCommentForm form) {
        final FeedbackComment po = FeedbackCommentConverter.INSTANCE.f2p(form);
        final int savedCount = this.baseMapper.insertIgnore(po);
        BaseCodes.OPTION_FAILURE.isTrue(savedCount == 1);

        // update feedback comment count
        this.feedbackMapper.update(null, new LambdaUpdateWrapper<Feedback>()
            .setSql("comment_count = comment_count + 1")
            .eq(Feedback::getId, po.getFeedbackId()));
    }


    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(FeedbackCommentForm form) {
        final int updatedCount = this.baseMapper.updateById(FeedbackCommentConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);
    }
}


