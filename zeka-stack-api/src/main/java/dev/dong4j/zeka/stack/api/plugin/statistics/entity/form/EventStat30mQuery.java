package dev.dong4j.zeka.stack.api.plugin.statistics.entity.form;

import java.io.Serial;

import dev.dong4j.zeka.kernel.common.base.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * <p> 统计事件 30 分钟聚合表 分页查询参数实体 (根据业务需求添加字段) </p>
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
@Schema(name = "统计事件 30 分钟聚合表-查询")
public class EventStat30mQuery extends BaseQuery<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备 ID
     * <p> 用于标识产生事件的设备唯一身份 </p>
     */
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

    /** AI 服务商, 用于标识事件所关联的 AI 服务提供方 */
    @Schema(description = "AI 服务商")
    private String provider;

    /** 模型名称 */
    @Schema(description = "模型名称")
    private String model;

    /** 触发入口 */
    @Schema(description = "触发入口")
    private String userAction;
}
