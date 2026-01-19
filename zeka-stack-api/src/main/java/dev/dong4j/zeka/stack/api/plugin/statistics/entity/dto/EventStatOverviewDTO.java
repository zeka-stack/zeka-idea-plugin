package dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p> 统计概览数据 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 18:30
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "统计概览数据")
public class EventStatOverviewDTO implements Serializable {
    /** 序列化版本号 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 总事件数 */
    @Schema(description = "总事件数")
    private long totalCount;
    /** 成功次数 */
    @Schema(description = "成功次数")
    private long successCount;
    /** 失败事件的总数 */
    @Schema(description = "失败次数")
    private long failedCount;

    /** 总 token 数 */
    @Schema(description = "总 token 数")
    private long tokenTotal;
    /** 输入 Token 总数 */
    @Schema(description = "输入 token 总数")
    private long inputTokenTotal;
    /**
     * 输出 token 总数
     *
     * @see #inputTokenTotal
     * @see #tokenTotal
     */
    @Schema(description = "输出 token 总数")
    private long outputTokenTotal;

    /** 平均耗时 (毫秒) */
    @Schema(description = "平均耗时(毫秒)")
    private long latencyAvgMs;
    /** 最大耗时 (毫秒) */
    @Schema(description = "最大耗时(毫秒)")
    private long latencyMaxMs;
    @Schema(description = "最小耗时(毫秒)")
    private long latencyMinMs;

    @Schema(description = "事件类型次数")
    private Map<String, Long> countByEventType;
    @Schema(description = "事件类型 token")
    private Map<String, Long> tokenByEventType;
    /**
     * 服务商次数
     * <p> 表示不同服务商处理事件的次数统计 </p>
     *
     * @see Map
     */
    @Schema(description = "服务商次数")
    private Map<String, Long> countByProvider;
    /** 入口次数统计, 键为入口标识, 值为对应次数 */
    @Schema(description = "入口次数")
    private Map<String, Long> countByUserAction;
    /**
     * 项目 token
     *
     * <p> 表示每个项目的 token 总数.</p>
     *
     * @see Map
     */
    @Schema(description = "项目 token")
    private Map<String, Long> tokenByProject;
    /**
     * 按项目统计的事件次数
     *
     * @see Map
     */
    @Schema(description = "项目次数")
    private Map<String, Long> countByProject;
    /** 按天次数 */
    @Schema(description = "按天次数")
    private Map<String, Long> countByDay;
    /** 按天统计的 token 数量 */
    @Schema(description = "按天 token")
    private Map<String, Long> tokenByDay;
    /**
     * 按天统计的事件类型次数, 用于展示每日不同事件类型的调用频次分布
     * <p> 外层键为日期, 内层键为事件类型, 值为对应次数 </p>
     * <p> 示例:{"2026-01-19": {"API_CALL": 120, "ERROR": 5}}</p>
     *
     * @see Map
     * @see String
     * @see Long
     */
    @Schema(description = "按天事件类型次数")
    private Map<String, Map<String, Long>> countByDayEventType;

    /**
     * 最近窗口统计
     * <p> 包含最近时间段内的事件统计数据, 以列表形式存储.</p>
     *
     * @see List<EventStat30mDTO>
     */
    @Schema(description = "最近窗口统计")
    private List<EventStat30mDTO> recentBuckets;
}
