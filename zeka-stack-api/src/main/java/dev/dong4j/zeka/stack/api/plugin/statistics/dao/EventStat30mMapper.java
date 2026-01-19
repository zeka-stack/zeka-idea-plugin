package dev.dong4j.zeka.stack.api.plugin.statistics.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mQuery;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.po.EventStat30m;
import dev.dong4j.zeka.starter.mybatis.base.BaseDao;

/**
 * <p> 统计事件 30 分钟聚合表 Dao 接口  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 17:34
 * @since 1.0.0
 */
@Mapper
public interface EventStat30mMapper extends BaseDao<EventStat30m> {

    /**
     * 对指定时间范围内的数据进行 30 分钟粒度的聚合统计
     * <p> 该方法用于将原始事件数据按照 30 分钟的时间窗口进行聚合计算, 适用于生成统计报表或分析数据趋势.
     *
     * @param bucketStart 聚合时间窗口的起始时间
     * @param bucketEnd   聚合时间窗口的结束时间
     * @return 聚合后的数据条数
     */
    int aggregateBucket(@Param("bucketStart") Date bucketStart, @Param("bucketEnd") Date bucketEnd);

    /**
     * 为 WebUI 查询事件统计数据
     * <p> 根据提供的查询条件从事件统计 30 分钟聚合表中查询符合条件的统计数据列表 </p>
     *
     * @param query 查询条件对象, 包含分页, 排序, 筛选等条件
     * @return 事件统计列表, 如果没有符合条件的数据则返回空列表
     */
    List<EventStat30m> selectForWebui(@Param("query") EventStat30mQuery query);
}
