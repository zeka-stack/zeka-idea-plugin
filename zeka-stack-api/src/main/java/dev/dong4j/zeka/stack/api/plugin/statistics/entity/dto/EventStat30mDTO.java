package dev.dong4j.zeka.stack.api.plugin.statistics.entity.dto;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.kernel.common.base.BaseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * <p> 统计事件 30 分钟聚合表 数据传输实体 (根据业务需求添加字段) </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 17:34
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "统计事件 30 分钟聚合表-数传传输对象")
public class EventStat30mDTO extends BaseDTO<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计窗口开始时间（30分钟） */
    @Schema(description = "统计窗口开始时间（30分钟）")
    private Date bucketStart;
    /** 统计窗口结束时间（30分钟） */
    @Schema(description = "统计窗口结束时间（30分钟）")
    private Date bucketEnd;
    /** 设备 ID */
    @Schema(description = "设备 ID")
    private String deviceId;
    /** 项目名称 */
    @Schema(description = "项目名称")
    private String projectName;
    /** 插件标识 */
    @Schema(description = "插件标识")
    private String pluginId;
    /** 事件类型 */
    @Schema(description = "事件类型")
    private String eventType;
    /** AI 服务商 */
    @Schema(description = "AI 服务商")
    private String provider;
    /** 模型名称 */
    @Schema(description = "模型名称")
    private String model;
    /** 触发入口 */
    @Schema(description = "触发入口")
    private String userAction;
    /** 总次数 */
    @Schema(description = "总次数")
    private Long totalCount;
    /** 成功次数 */
    @Schema(description = "成功次数")
    private Long successCount;
    /** 失败次数 */
    @Schema(description = "失败次数")
    private Long failedCount;
    /** 总 token 数 */
    @Schema(description = "总 token 数")
    private Long tokenTotal;
    /** 输入 token 总数 */
    @Schema(description = "输入 token 总数")
    private Long inputTokenTotal;
    /** 输出 token 总数 */
    @Schema(description = "输出 token 总数")
    private Long outputTokenTotal;
    /** 总耗时(毫秒) */
    @Schema(description = "总耗时(毫秒)")
    private Long latencyTotalMs;
    /** 平均耗时(毫秒) */
    @Schema(description = "平均耗时(毫秒)")
    private Long latencyAvgMs;
    /** 最大耗时(毫秒) */
    @Schema(description = "最大耗时(毫秒)")
    private Long latencyMaxMs;
    /** 最小耗时(毫秒) */
    @Schema(description = "最小耗时(毫秒)")
    private Long latencyMinMs;
}
