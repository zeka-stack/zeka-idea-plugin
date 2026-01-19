package dev.dong4j.zeka.stack.api.plugin.statistics.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import dev.dong4j.zeka.stack.api.plugin.statistics.service.EventStat30mService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 统计事件 30 分钟聚合任务
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:40
 * @since 1.0.0
 */
@Slf4j
@Component
@AllArgsConstructor
public class EventStat30mScheduler {

    /** 事件 30 分钟聚合服务, 用于执行定时数据聚合任务 */
    private final EventStat30mService eventStat30mService;

    /**
     * 每 30 分钟聚合一次上一窗口的数据
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void aggregate() {
        LocalDateTime now = LocalDateTime.now();
        int minuteBucket = (now.getMinute() / 30) * 30;
        LocalDateTime bucketEnd = now.withMinute(minuteBucket).withSecond(0).withNano(0);
        LocalDateTime bucketStart = bucketEnd.minusMinutes(30);

        Date start = Date.from(bucketStart.atZone(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(bucketEnd.atZone(ZoneId.systemDefault()).toInstant());

        try {
            int rows = eventStat30mService.aggregateBucket(start, end);
            log.info("event_stat_30m 聚合完成: {} ~ {}, rows={}", start, end, rows);
        } catch (Exception e) {
            log.error("event_stat_30m 聚合失败: {} ~ {}", start, end, e);
        }
    }
}
