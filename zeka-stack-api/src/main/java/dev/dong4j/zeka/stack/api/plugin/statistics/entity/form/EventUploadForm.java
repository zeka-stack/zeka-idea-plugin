package dev.dong4j.zeka.stack.api.plugin.statistics.entity.form;

import java.io.Serial;
import java.util.List;

import dev.dong4j.zeka.kernel.common.base.BaseForm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * <p> 统计事件批量上报入参 </p>
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
@Schema(name = "统计事件表-批量上报")
public class EventUploadForm extends BaseForm<Long> {
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

    /** 上报事件列表 */
    @Schema(description = "事件列表")
    @NotEmpty(message = "[事件列表] 不能为空)")
    @Valid
    private List<EventUploadItemForm> items;
}
