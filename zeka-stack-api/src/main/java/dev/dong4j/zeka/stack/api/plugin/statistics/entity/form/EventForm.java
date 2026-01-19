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
 * <p> 统计事件表 入参实体 (根据业务需求添加字段) </p>
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
@Schema(name = "统计事件表-新增与更新")
public class EventForm extends BaseForm<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备 ID */
    @Schema(description = "设备 ID")
    @NotBlank(message = "[设备 ID] 必填)")
    private String deviceId;
    /** 客户端上报时间戳(毫秒) */
    @Schema(description = "客户端上报时间戳(毫秒)")
    @NotNull(message = "[客户端上报时间戳(毫秒)] 必填)")
    private Long clientTimestamp;
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
    /** 总 token 数 */
    @Schema(description = "总 token 数")
    @NotNull(message = "[总 token 数] 必填)")
    private Long tokenCount;
    /** 结果状态 */
    @Schema(description = "结果状态")
    @NotBlank(message = "[结果状态] 必填)")
    private String resultStatus;
    /** 耗时(毫秒) */
    @Schema(description = "耗时(毫秒)")
    @NotNull(message = "[耗时(毫秒)] 必填)")
    private Long latencyMs;
    /** 输入 token 数 */
    @Schema(description = "输入 token 数")
    @NotNull(message = "[输入 token 数] 必填)")
    private Long inputToken;
    /** 输出 token 数 */
    @Schema(description = "输出 token 数")
    @NotNull(message = "[输出 token 数] 必填)")
    private Long outputToken;
    /** 触发入口 */
    @Schema(description = "触发入口")
    @NotBlank(message = "[触发入口] 必填)")
    private String userAction;
    /** 服务端接收时间 */
    @Schema(description = "服务端接收时间")
    @NotNull(message = "[服务端接收时间] 必填)")
    private Date receivedTime;
}
