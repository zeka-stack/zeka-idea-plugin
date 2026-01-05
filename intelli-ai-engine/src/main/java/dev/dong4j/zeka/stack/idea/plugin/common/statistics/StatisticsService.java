package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import java.util.List;
import java.util.Map;

/**
 * <p>Description : 统计服务接口.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
public interface StatisticsService {

    /**
     * 上报统计事件
     *
     * @param event 统计事件
     */
    void report(StatisticsEvent event);

    /**
     * 批量上报统计事件
     *
     * @param events 统计事件列表
     */
    void reportBatch(List<StatisticsEvent> events);

    /**
     * 获取统计快照
     *
     * @param date 日期, 格式 yyyy-MM-dd, 为 null 则获取所有
     * @return 统计快照
     */
    StatisticsSnapshot snapshot(String date);

    /**
     * 获取聚合统计数据
     *
     * @return 聚合统计数据 map
     */
    Map<String, Object> getAggregatedStats();

    /**
     * 是否启用统计
     *
     * @return 返回 boolean 值, 表示统计服务是否启用
     */
    boolean isEnabled();

    /** 立即触发写入文件 */
    void flush();

    /** 立即触发上报操作 */
    void uploadNow();

    /** 启动统计服务 */
    void start();

    /** 停止统计服务 */
    void stop();
}
