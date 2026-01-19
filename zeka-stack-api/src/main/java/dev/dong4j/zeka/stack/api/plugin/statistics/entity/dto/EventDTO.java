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
 * <p> 统计事件表 数据传输实体 (根据业务需求添加字段) </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.19 11:55
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "统计事件表-数传传输对象")
public class EventDTO extends BaseDTO<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备 ID */
    @Schema(description = "设备 ID")
    private String deviceId;
    /** 客户端上报时间戳(毫秒) */
    @Schema(description = "客户端上报时间戳(毫秒)")
    private Long clientTimestamp;
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
    /** 总 token 数 */
    @Schema(description = "总 token 数")
    private Long tokenCount;
    /** 结果状态 */
    @Schema(description = "结果状态")
    private String resultStatus;
    /** 耗时(毫秒) */
    @Schema(description = "耗时(毫秒)")
    private Long latencyMs;
    /** 输入 token 数 */
    @Schema(description = "输入 token 数")
    private Long inputToken;
    /** 输出 token 数 */
    @Schema(description = "输出 token 数")
    private Long outputToken;
    /** 触发入口 */
    @Schema(description = "触发入口")
    private String userAction;
    /** 服务端接收时间 */
    @Schema(description = "服务端接收时间")
    private Date receivedTime;
}
