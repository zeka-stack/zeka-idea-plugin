package dev.dong4j.zeka.stack.api.plugin.statistics.entity.form;

import java.io.Serial;
import java.util.Date;

import dev.dong4j.zeka.kernel.common.base.BaseForm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * <p> 统计事件 30 分钟聚合表 入参实体 (根据业务需求添加字段) </p>
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
@Schema(name = "统计事件 30 分钟聚合表-新增与更新")
public class EventStat30mForm extends BaseForm<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计窗口开始时间（30分钟） */
    @Schema(description = "统计窗口开始时间（30分钟）")
    @NotNull(message = "[统计窗口开始时间（30分钟）] 必填)")
    private Date bucketStart;
    /** 统计窗口结束时间（30分钟） */
    @Schema(description = "统计窗口结束时间（30分钟）")
    @NotNull(message = "[统计窗口结束时间（30分钟）] 必填)")
    private Date bucketEnd;
    /** 设备 ID */
    @Schema(description = "设备 ID")
    @NotBlank(message = "[设备 ID] 必填)")
    private String deviceId;
    /** 项目名称 */
    @Schema(description = "项目名称")
    @NotBlank(message = "[项目名称] 必填)")
    private String projectName;
    /** 插件标识 */
    @Schema(description = "插件标识")
    @NotBlank(message = "[插件标识] 必填)")
    private String pluginId;
    /** 事件类型 */
    @Schema(description = "事件类型")
    @NotBlank(message = "[事件类型] 必填)")
    private String eventType;
    /** AI 服务商 */
    @Schema(description = "AI 服务商")
    @NotBlank(message = "[AI 服务商] 必填)")
    private String provider;
    /** 模型名称 */
    @Schema(description = "模型名称")
    @NotBlank(message = "[模型名称] 必填)")
    private String model;
    /** 触发入口 */
    @Schema(description = "触发入口")
    @NotBlank(message = "[触发入口] 必填)")
    private String userAction;
    /** 总次数 */
    @Schema(description = "总次数")
    @NotNull(message = "[总次数] 必填)")
    private Long totalCount;
    /** 成功次数 */
    @Schema(description = "成功次数")
    @NotNull(message = "[成功次数] 必填)")
    private Long successCount;
    /** 失败次数 */
    @Schema(description = "失败次数")
    @NotNull(message = "[失败次数] 必填)")
    private Long failedCount;
    /** 总 token 数 */
    @Schema(description = "总 token 数")
    @NotNull(message = "[总 token 数] 必填)")
    private Long tokenTotal;
    /** 输入 token 总数 */
    @Schema(description = "输入 token 总数")
    @NotNull(message = "[输入 token 总数] 必填)")
    private Long inputTokenTotal;
    /** 输出 token 总数 */
    @Schema(description = "输出 token 总数")
    @NotNull(message = "[输出 token 总数] 必填)")
    private Long outputTokenTotal;
    /** 总耗时(毫秒) */
    @Schema(description = "总耗时(毫秒)")
    @NotNull(message = "[总耗时(毫秒)] 必填)")
    private Long latencyTotalMs;
    /** 平均耗时(毫秒) */
    @Schema(description = "平均耗时(毫秒)")
    @NotNull(message = "[平均耗时(毫秒)] 必填)")
    private Long latencyAvgMs;
    /** 最大耗时(毫秒) */
    @Schema(description = "最大耗时(毫秒)")
    @NotNull(message = "[最大耗时(毫秒)] 必填)")
    private Long latencyMaxMs;
    /** 最小耗时(毫秒) */
    @Schema(description = "最小耗时(毫秒)")
    @NotNull(message = "[最小耗时(毫秒)] 必填)")
    private Long latencyMinMs;
}
