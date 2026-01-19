package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 统计快照数据类
 * <p> 用于聚合和存储系统运行期间的各类统计信息, 包括请求记录, 插件统计, 事件类型统计, 提供者统计, 项目统计, 结果状态统计, 用户行为统计, 总令牌数, 输入输出令牌数, 总延迟等. 支持通过 {@code addRecord} 方法动态累加统计项,
 * 并通过 {@code toMap} 方法转换为结构化 Map 用于序列化或展示.
 * <p> 该类通常用于监控系统性能, 分析用户行为, 评估插件使用情况等场景, 是系统日志或仪表盘数据聚合的核心载体.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19
 * @since 1.0.0
 */
@Getter
@Setter
public class StatisticsSnapshot {

    /** 统计记录列表, 存储所有统计条目数据 */
    private List<StatisticsRecord> records = new ArrayList<>();
    /** 总记录条数 */
    private int totalCount;
    /** 按插件 ID 统计的调用次数映射表, 键为插件 ID, 值为调用次数 */
    private Map<String, Long> byPlugin = new HashMap<>();
    /** 按事件类型统计的计数映射, 键为事件类型名, 值为出现次数 */
    private Map<String, Long> byEventType = new HashMap<>();
    /** 按服务提供商统计的调用次数映射, 键为提供商标识, 值为调用次数 */
    private Map<String, Long> byProvider = new HashMap<>();
    /** 按项目统计的记录数量映射, 键为项目名称, 值为对应项目记录数 */
    private Map<String, Long> byProject = new HashMap<>();
    /** 按结果状态统计的计数映射, 用于记录不同结果状态的请求次数 */
    private Map<String, Long> byResultStatus = new HashMap<>();
    /** 用户操作统计映射, 键为用户操作代码, 值为操作次数 */
    private Map<String, Long> byUserAction = new HashMap<>();
    /** 总 token 数量, 用于统计所有记录的 token 消耗总量 */
    private long totalTokenCount;
    /** 总输入 Token 数量, 用于统计请求输入部分的总字数或标记数 */
    private long totalInputToken;
    /** 总输出 Token 数量 */
    private long totalOutputToken;
    /** 总延迟时间 (毫秒), 用于统计所有记录的响应耗时总和 */
    private long totalLatencyMs;

    /**
     * 初始化统计快照对象
     * <p> 创建一个空的 StatisticsSnapshot 实例, 用于后续收集和聚合统计信息
     */
    public StatisticsSnapshot() {
    }

    /**
     * 添加一条统计记录并更新聚合数据
     * <p> 将指定的统计记录添加到记录列表中, 并根据记录内容更新各维度的统计聚合值, 包括插件, 事件类型, 提供者, 项目, 结果状态, 用户操作等维度的计数, 以及总令牌数, 输入令牌数, 输出令牌数和总延迟毫秒数.
     *
     * @param record 要添加的统计记录对象, 不能为空
     */
    public void addRecord(StatisticsRecord record) {
        records.add(record);
        totalCount++;

        // 聚合统计
        byPlugin.merge(record.getPluginId(), 1L, Long::sum);
        byEventType.merge(record.getEventType(), 1L, Long::sum);
        if (record.getProvider() != null) {
            byProvider.merge(record.getProvider(), 1L, Long::sum);
        }
        if (record.getProjectName() != null) {
            byProject.merge(record.getProjectName(), 1L, Long::sum);
        }
        if (record.getResultStatus() != null) {
            byResultStatus.merge(record.getResultStatus(), 1L, Long::sum);
        }
        if (record.getUserAction() != null) {
            byUserAction.merge(record.getUserActionCode(), 1L, Long::sum);
        }
        totalTokenCount += record.getTokenCount();
        totalInputToken += record.getInputToken();
        totalOutputToken += record.getOutputToken();
        totalLatencyMs += record.getLatencyMs();
    }

    /**
     * 将统计快照数据转换为包含所有统计信息的 Map 对象
     * <p>该方法将当前统计快照中的各项统计数据 (如总记录数, 按插件 / 事件类型 / 提供者等分组的计数, 总 Token 数, 总延迟等) 以及原始记录列表封装到一个 Map 中, 便于序列化或传输.
     *
     * @return 包含所有统计字段的 Map 对象, 键包括 "total", "byPlugin", "byEventType", "byProvider", "byProject", "byResultStatus", "byUserAction",
     * "totalTokenCount", "totalInputToken", "totalOutputToken", "totalLatencyMs", "records"
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", totalCount);
        map.put("byPlugin", byPlugin);
        map.put("byEventType", byEventType);
        map.put("byProvider", byProvider);
        map.put("byProject", byProject);
        map.put("byResultStatus", byResultStatus);
        map.put("byUserAction", byUserAction);
        map.put("totalTokenCount", totalTokenCount);
        map.put("totalInputToken", totalInputToken);
        map.put("totalOutputToken", totalOutputToken);
        map.put("totalLatencyMs", totalLatencyMs);
        map.put("records", records);
        return map;
    }
}
