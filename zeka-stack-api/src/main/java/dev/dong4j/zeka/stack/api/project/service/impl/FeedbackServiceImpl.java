package dev.dong4j.zeka.stack.api.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.project.dao.FeedbackMapper;
import dev.dong4j.zeka.stack.api.project.entity.converter.FeedbackConverter;
import dev.dong4j.zeka.stack.api.project.entity.dto.FeedbackDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackForm;
import dev.dong4j.zeka.stack.api.project.entity.po.Feedback;
import dev.dong4j.zeka.stack.api.project.service.FeedbackService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 反馈表 服务接口实现类 </p>
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
public class FeedbackServiceImpl extends BaseServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public FeedbackDTO detail(Long id) {
        final Feedback po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return FeedbackConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(FeedbackForm form) {
        final Feedback po = FeedbackConverter.INSTANCE.f2p(form);
        final int savedCount = this.baseMapper.insertIgnore(po);
        BaseCodes.OPTION_FAILURE.isTrue(savedCount == 1);
    }

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(FeedbackForm form) {
        final int updatedCount = this.baseMapper.updateById(FeedbackConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);
    }

    /**
     * 点赞
     *
     * @param id 主键
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void vote(Long id) {
        this.update(new LambdaUpdateWrapper<Feedback>()
                        .setSql("vote_count = vote_count + 1")
                        .eq(Feedback::getId, id));
    }
}


