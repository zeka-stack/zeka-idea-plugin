package dev.dong4j.zeka.stack.api.project.entity.converter;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import dev.dong4j.zeka.kernel.common.mapstruct.ExtendConverter;
import dev.dong4j.zeka.stack.api.project.entity.dto.FeedbackDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackForm;
import dev.dong4j.zeka.stack.api.project.entity.po.Feedback;

/**
 * <p> 反馈表 实体转换器 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Mapper
public interface FeedbackConverter extends ExtendConverter<FeedbackForm, FeedbackDTO, Feedback> {
    /** INSTANCE */
    FeedbackConverter INSTANCE = Mappers.getMapper(FeedbackConverter.class);
}
