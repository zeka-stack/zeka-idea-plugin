package dev.dong4j.zeka.stack.api.plugin.statistics.entity.converter;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import dev.dong4j.zeka.kernel.common.mapstruct.ExtendConverter;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventStat30mDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.po.EventStat30m;

/**
 * <p> 统计事件 30 分钟聚合表 实体转换器 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 17:34
 * @since 1.0.0
 */
@Mapper
public interface EventStat30mConverter extends ExtendConverter<EventStat30mForm, EventStat30mDTO, EventStat30m> {
    /** INSTANCE */
    EventStat30mConverter INSTANCE = Mappers.getMapper(EventStat30mConverter.class);
}
