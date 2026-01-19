package dev.dong4j.zeka.stack.api.plugin.statistics.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.starter.mybatis.base.BaseWithTimePO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p> 统计事件 30 分钟聚合表 实体类  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 17:34
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("event_stat_30m")
public class EventStat30m extends BaseWithTimePO<Long, EventStat30m> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;
    /** 统计窗口开始时间（30分钟）-表字段 */
    public static final String BUCKET_START = "bucket_start";
    /** 统计窗口结束时间（30分钟）-表字段 */
    public static final String BUCKET_END = "bucket_end";
    /** 设备 ID-表字段 */
    public static final String DEVICE_ID = "device_id";
    /** 项目名称-表字段 */
    public static final String PROJECT_NAME = "project_name";
    /** 插件标识-表字段 */
    public static final String PLUGIN_ID = "plugin_id";
    /** 事件类型-表字段 */
    public static final String EVENT_TYPE = "event_type";
    /** AI 服务商-表字段 */
    public static final String PROVIDER = "provider";
    /** 模型名称-表字段 */
    public static final String MODEL = "model";
    /** 触发入口-表字段 */
    public static final String USER_ACTION = "user_action";
    /** 总次数-表字段 */
    public static final String TOTAL_COUNT = "total_count";
    /** 成功次数-表字段 */
    public static final String SUCCESS_COUNT = "success_count";
    /** 失败次数-表字段 */
    public static final String FAILED_COUNT = "failed_count";
    /** 总 token 数-表字段 */
    public static final String TOKEN_TOTAL = "token_total";
    /** 输入 token 总数-表字段 */
    public static final String INPUT_TOKEN_TOTAL = "input_token_total";
    /** 输出 token 总数-表字段 */
    public static final String OUTPUT_TOKEN_TOTAL = "output_token_total";
    /** 总耗时(毫秒)-表字段 */
    public static final String LATENCY_TOTAL_MS = "latency_total_ms";
    /** 平均耗时(毫秒)-表字段 */
    public static final String LATENCY_AVG_MS = "latency_avg_ms";
    /** 最大耗时(毫秒)-表字段 */
    public static final String LATENCY_MAX_MS = "latency_max_ms";
    /** 最小耗时(毫秒)-表字段 */
    public static final String LATENCY_MIN_MS = "latency_min_ms";

    /** 统计窗口开始时间（30分钟） */
    @TableField("`bucket_start`")
    private Date bucketStart;
    /** 统计窗口结束时间（30分钟） */
    @TableField("`bucket_end`")
    private Date bucketEnd;
    /** 设备 ID */
    @TableField("`device_id`")
    private String deviceId;
    /** 项目名称 */
    @TableField("`project_name`")
    private String projectName;
    /** 插件标识 */
    @TableField("`plugin_id`")
    private String pluginId;
    /** 事件类型 */
    @TableField("`event_type`")
    private String eventType;
    /** AI 服务商 */
    @TableField("`provider`")
    private String provider;
    /** 模型名称 */
    @TableField("`model`")
    private String model;
    /** 触发入口 */
    @TableField("`user_action`")
    private String userAction;
    /** 总次数 */
    @TableField("`total_count`")
    private Long totalCount;
    /** 成功次数 */
    @TableField("`success_count`")
    private Long successCount;
    /** 失败次数 */
    @TableField("`failed_count`")
    private Long failedCount;
    /** 总 token 数 */
    @TableField("`token_total`")
    private Long tokenTotal;
    /** 输入 token 总数 */
    @TableField("`input_token_total`")
    private Long inputTokenTotal;
    /** 输出 token 总数 */
    @TableField("`output_token_total`")
    private Long outputTokenTotal;
    /** 总耗时(毫秒) */
    @TableField("`latency_total_ms`")
    private Long latencyTotalMs;
    /** 平均耗时(毫秒) */
    @TableField("`latency_avg_ms`")
    private Long latencyAvgMs;
    /** 最大耗时(毫秒) */
    @TableField("`latency_max_ms`")
    private Long latencyMaxMs;
    /** 最小耗时(毫秒) */
    @TableField("`latency_min_ms`")
    private Long latencyMinMs;
}
