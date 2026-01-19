package dev.dong4j.zeka.stack.api.plugin.statistics.entity.converter;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import dev.dong4j.zeka.kernel.common.mapstruct.ExtendConverter;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.po.Event;

/**
 * <p> 统计事件表 实体转换器 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 11:55
 * @since 1.0.0
 */
@Mapper
public interface EventConverter extends ExtendConverter<EventForm, EventDTO, Event> {
    /** INSTANCE */
    EventConverter INSTANCE = Mappers.getMapper(EventConverter.class);
}
