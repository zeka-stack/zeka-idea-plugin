package dev.dong4j.zeka.stack.api.plugin.statistics.service;

import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventStat30mDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventStatOverviewDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mQuery;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.po.EventStat30m;
import dev.dong4j.zeka.starter.mybatis.service.BaseService;

/**
 * <p> 统计事件 30 分钟聚合表 服务接口 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 17:34
 * @since 1.0.0
 */
public interface EventStat30mService extends BaseService<EventStat30m> {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    EventStat30mDTO detail(Long id);

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void create(EventStat30mForm form);

    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    void edit(EventStat30mForm form);

    /**
     * 聚合指定时间区间内的事件数据
     * <p> 根据起始时间和结束时间聚合事件数据, 用于统计指定时间窗口内的事件数量或分布 </p>
     *
     * @param bucketStart 聚合区间的起始时间
     * @param bucketEnd   聚合区间的结束时间
     * @return 聚合结果的整数计数
     * @since 1.0.0
     */
    int aggregateBucket(java.util.Date bucketStart, java.util.Date bucketEnd);

    /**
     * 根据查询条件获取用于 Web 界面展示的事件统计列表
     * <p> 根据传入的查询参数, 返回适用于前端展示的事件统计数据列表
     *
     * @param query 查询参数对象, 包含分页, 过滤等信息
     * @return 事件统计数据列表, 用于 Web 界面展示
     * @since 1.0.0
     */
    java.util.List<EventStat30mDTO> listForWebui(EventStat30mQuery query);

    /**
     * 获取事件统计概览数据
     * <p> 根据查询条件返回事件统计的汇总信息, 用于展示整体统计情况.
     *
     * @param query 查询参数对象, 包含过滤和分页等条件
     * @return 事件统计概览数据对象, 包含关键统计指标
     * @since 1.0.0
     */
    EventStatOverviewDTO overview(EventStat30mQuery query);
}
