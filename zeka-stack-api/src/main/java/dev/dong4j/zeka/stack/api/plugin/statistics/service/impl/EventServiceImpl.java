package dev.dong4j.zeka.stack.api.plugin.statistics.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.plugin.statistics.dao.EventMapper;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.converter.EventConverter;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventUploadForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventUploadItemForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.po.Event;
import dev.dong4j.zeka.stack.api.plugin.statistics.service.EventService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 统计事件表 服务接口实现类 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 11:55
 * @since 1.0.0
 */
@Slf4j
@Service
@AllArgsConstructor
public class EventServiceImpl extends BaseServiceImpl<EventMapper, Event> implements EventService {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public EventDTO detail(Long id) {
        final Event po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return EventConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(EventForm form) {
        final Event po = EventConverter.INSTANCE.f2p(form);
        final int savedCount = this.baseMapper.insertIgnore(po);
        BaseCodes.OPTION_FAILURE.isTrue(savedCount == 1);
    }

    /**
     * 批量上报
     *
     * @param form 批量上报参数
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(EventUploadForm form) {
        List<EventUploadItemForm> items = form.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        Date now = new Date();
        List<Event> events = new ArrayList<>(items.size());
        for (EventUploadItemForm item : items) {
            Event event = new Event();
            event.setDeviceId(form.getDeviceId());
            event.setClientTimestamp(form.getClientTimestamp());
            event.setProjectName(nullToEmpty(item.getProjectName()));
            event.setPluginId(item.getPluginId());
            event.setEventType(item.getEventType());
            event.setProvider(nullToEmpty(item.getProvider()));
            event.setModel(nullToEmpty(item.getModel()));
            event.setTokenCount(item.getTokenCount());
            event.setResultStatus(nullToEmpty(item.getResultStatus()));
            event.setLatencyMs(item.getLatencyMs());
            event.setInputToken(item.getInputToken());
            event.setOutputToken(item.getOutputToken());
            event.setUserAction(item.getUserAction());
            event.setCreateTime(new Date(item.getCreatedAt()));
            event.setReceivedTime(now);
            events.add(event);
        }

        baseMapper.insert(events);
    }

    /**
     * 将 null 值转换为空字符串
     * <p> 如果输入参数为 null, 则返回空字符串 "", 否则返回原值 </p>
     *
     * @param value 待处理的字符串值, 可能为 null
     * @return 如果 value 为 null, 则返回空字符串; 否则返回原值
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(EventForm form) {
        final int updatedCount = this.baseMapper.updateById(EventConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);
    }
}
