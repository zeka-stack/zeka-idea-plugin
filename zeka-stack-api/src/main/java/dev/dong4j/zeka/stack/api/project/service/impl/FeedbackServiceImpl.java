package dev.dong4j.zeka.stack.api.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackRequest;
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
    /** Plugin feedback service for creating GitHub issues */
    private final dev.dong4j.zeka.stack.api.plugin.feedback.service.FeedbackService pluginFeedbackService;

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


        // 异步调用 GitHub 创建 Issue
        CompletableFuture.runAsync(() -> {
            try {
                FeedbackRequest request = convertToFeedbackRequest(form);
                pluginFeedbackService.submitIssue(request);
                log.debug("Successfully created GitHub issue for feedback: {}", form.getTitle());
            } catch (Exception e) {
                log.warn("Failed to create GitHub issue for feedback: {}", form.getTitle(), e);
            }
        });
    }

    /**
     * 将 FeedbackForm 转换为 FeedbackRequest
     * <p> 目前先写死一些数据，后续可以根据实际需求调整
     *
     * @param form 反馈表单
     * @return FeedbackRequest
     */
    private FeedbackRequest convertToFeedbackRequest(FeedbackForm form) {
        FeedbackRequest request = new FeedbackRequest();
        request.setTitle(form.getTitle());
        request.setContent(form.getDescription() != null ? form.getDescription() : "");
        // 默认设置为功能建议类型
        request.setType(FeedbackRequest.FeedbackType.FEATURE);

        // 构建用户信息（目前写死）
        FeedbackRequest.UserInfo userInfo = new FeedbackRequest.UserInfo();
        userInfo.setPluginName("IntelliAI WebUI");
        userInfo.setGithubUsername(""); // 可以从认证信息中获取，目前先写死
        request.setUserInfo(userInfo);

        // 构建元数据
        FeedbackRequest.Metadata metadata = new FeedbackRequest.Metadata();
        metadata.setClientId("zeka-idea-webui");
        metadata.setTimestamp(System.currentTimeMillis());
        request.setMetadata(metadata);

        return request;
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


