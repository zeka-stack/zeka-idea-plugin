package dev.dong4j.zeka.stack.api.plugin.statistics.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.plugin.statistics.dao.EventStat30mMapper;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.converter.EventStat30mConverter;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventStat30mDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto.EventStatOverviewDTO;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mForm;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.form.EventStat30mQuery;
import dev.dong4j.zeka.stack.api.plugin.statistics.entity.po.EventStat30m;
import dev.dong4j.zeka.stack.api.plugin.statistics.service.EventStat30mService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 统计事件 30 分钟聚合表 服务接口实现类 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 17:34
 * @since 1.0.0
 */
@Slf4j
@Service
@AllArgsConstructor
public class EventStat30mServiceImpl extends BaseServiceImpl<EventStat30mMapper, EventStat30m> implements EventStat30mService {

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public EventStat30mDTO detail(Long id) {
        final EventStat30m po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return EventStat30mConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(EventStat30mForm form) {
        final EventStat30m po = EventStat30mConverter.INSTANCE.f2p(form);
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
    public void edit(EventStat30mForm form) {
        final int updatedCount = this.baseMapper.updateById(EventStat30mConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);
    }

    /**
     * 按照指定的时间区间聚合数据桶
     * <p> 根据传入的开始时间和结束时间, 调用基础映射器执行数据聚合操作, 返回受影响的记录数.
     *
     * @param bucketStart 数据桶的起始时间
     * @param bucketEnd   数据桶的结束时间
     * @return 受影响的记录数量
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int aggregateBucket(Date bucketStart, Date bucketEnd) {
        return this.baseMapper.aggregateBucket(bucketStart, bucketEnd);
    }

    /**
     * 根据查询条件获取统计事件列表用于 Web 界面展示
     * <p> 调用数据访问层查询符合条件的统计事件数据, 并将实体对象转换为 DTO 对象返回
     *
     * @param query 查询条件对象, 包含筛选, 排序等参数
     * @return 统计事件 DTO 列表, 如果查询结果为空则返回空列表
     */
    @Override
    public List<EventStat30mDTO> listForWebui(EventStat30mQuery query) {
        List<EventStat30m> list = this.baseMapper.selectForWebui(query);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(EventStat30mConverter.INSTANCE::p2d).collect(Collectors.toList());
    }

    /**
     * 根据查询条件生成事件统计概览数据
     * <p> 该方法从数据库中获取事件统计数据, 并计算各类指标的汇总信息, 包括总次数, 成功 / 失败次数, 令牌数量, 延迟等. 同时, 按事件类型, 提供者, 用户行为, 项目和日期进行分类统计.</p>
     *
     * @param query 查询条件对象, 用于筛选事件统计记录
     * @return 包含所有统计结果的 EventStatOverviewDTO 对象
     * @since 1.0.0
     */
    @Override
    public EventStatOverviewDTO overview(EventStat30mQuery query) {
        List<EventStat30m> list = this.baseMapper.selectForWebui(query);
        if (list == null || list.isEmpty()) {
            return new EventStatOverviewDTO(0, 0, 0,
                                            0, 0, 0,
                                            0, 0, 0,
                                            Map.of(), Map.of(), Map.of(), Map.of(),
                                            Map.of(), Map.of(), Map.of(), Map.of(),
                                            Map.of(), List.of());
        }

        long totalCount = 0;
        long successCount = 0;
        long failedCount = 0;
        long tokenTotal = 0;
        long inputTokenTotal = 0;
        long outputTokenTotal = 0;
        long latencyTotal = 0;
        long latencyMax = 0;
        long latencyMin = Long.MAX_VALUE;

        Map<String, Long> countByEventType = new HashMap<>();
        Map<String, Long> tokenByEventType = new HashMap<>();
        Map<String, Long> countByProvider = new HashMap<>();
        Map<String, Long> countByUserAction = new HashMap<>();
        Map<String, Long> tokenByProject = new HashMap<>();
        Map<String, Long> countByProject = new HashMap<>();
        Map<String, Long> countByDay = new LinkedHashMap<>();
        Map<String, Long> tokenByDay = new LinkedHashMap<>();
        Map<String, Map<String, Long>> countByDayEventType = new LinkedHashMap<>();

        for (EventStat30m item : list) {
            long count = nullSafe(item.getTotalCount());
            long success = nullSafe(item.getSuccessCount());
            long failed = nullSafe(item.getFailedCount());
            long token = nullSafe(item.getTokenTotal());
            long inputToken = nullSafe(item.getInputTokenTotal());
            long outputToken = nullSafe(item.getOutputTokenTotal());
            long latency = nullSafe(item.getLatencyTotalMs());
            long maxLatency = nullSafe(item.getLatencyMaxMs());
            long minLatency = nullSafe(item.getLatencyMinMs());

            totalCount += count;
            successCount += success;
            failedCount += failed;
            tokenTotal += token;
            inputTokenTotal += inputToken;
            outputTokenTotal += outputToken;
            latencyTotal += latency;
            if (maxLatency > latencyMax) {
                latencyMax = maxLatency;
            }
            if (minLatency > 0 && minLatency < latencyMin) {
                latencyMin = minLatency;
            }

            addTo(countByEventType, safeKey(item.getEventType()), count);
            addTo(tokenByEventType, safeKey(item.getEventType()), token);
            addTo(countByProvider, safeKey(item.getProvider()), count);
            addTo(countByUserAction, safeKey(item.getUserAction()), count);
            addTo(tokenByProject, safeKey(item.getProjectName()), token);
            addTo(countByProject, safeKey(item.getProjectName()), count);

            String day = formatDay(item.getBucketStart());
            addTo(countByDay, day, count);
            addTo(tokenByDay, day, token);
            countByDayEventType.computeIfAbsent(day, k -> new HashMap<>());
            addTo(countByDayEventType.get(day), safeKey(item.getEventType()), count);
        }

        long latencyAvg = totalCount > 0 ? Math.round((double) latencyTotal / totalCount) : 0;
        if (latencyMin == Long.MAX_VALUE) {
            latencyMin = 0;
        }

        List<EventStat30mDTO> recentBuckets = list.stream()
            .sorted((a, b) -> {
                Date left = a.getBucketStart();
                Date right = b.getBucketStart();
                if (left == null && right == null) {
                    return 0;
                }
                if (left == null) {
                    return 1;
                }
                if (right == null) {
                    return -1;
                }
                return right.compareTo(left);
            })
            .limit(6)
            .map(EventStat30mConverter.INSTANCE::p2d)
            .collect(Collectors.toList());

        return new EventStatOverviewDTO(
            totalCount,
            successCount,
            failedCount,
            tokenTotal,
            inputTokenTotal,
            outputTokenTotal,
            latencyAvg,
            latencyMax,
            latencyMin,
            countByEventType,
            tokenByEventType,
            countByProvider,
            countByUserAction,
            tokenByProject,
            countByProject,
            countByDay,
            tokenByDay,
            countByDayEventType,
            recentBuckets
        );
    }

    /**
     * 将指定键值对添加到映射中, 若键不存在则初始化为 0 后再累加
     * <p> 该方法用于统计聚合数据, 支持对 Map<String, Long> 类型的映射进行键值累加操作 </p>
     *
     * @param map   映射对象, 键为字符串, 值为长整型
     * @param key   要添加或累加的键
     * @param value 要累加的数值
     * @since 1.0.0
     */
    private static void addTo(Map<String, Long> map, String key, long value) {
        map.put(key, map.getOrDefault(key, 0L) + value);
    }

    /**
     * 将 Long 类型的值转换为 long 类型, 如果传入的值为 null, 则返回 0.
     *
     * @param value 需要转换的 Long 值
     * @return 转换后的 long 值, 若输入为 null 则返回 0
     */
    private static long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 返回一个安全的键值
     * <p> 如果输入的字符串为 null 或者为空白, 则返回 "unknown", 否则返回原字符串
     *
     * @param value 输入的字符串
     * @return 如果输入为 null 或空白, 则返回 "unknown", 否则返回原字符串
     */
    private static String safeKey(String value) {
        return (value == null || value.isBlank()) ? "unknown" : value;
    }

    /**
     * 将指定 {@link java.util.Date} 对象格式化为 [yyyy-MM-dd] 格式的字符串.
     *
     * <p>如果传入的日期为 {@code null}, 则返回字符串 {@code "unknown"}.</p>
     *
     * @param date 需要格式化的日期对象, 可能为 {@code null}
     * @return 格式化后的日期字符串; 若 {@code date} 为 {@code null}, 则返回 {@code "unknown"}
     */
    private static String formatDay(Date date) {
        if (date == null) {
            return "unknown";
        }
        java.time.LocalDate localDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return localDate.toString();
    }
}
