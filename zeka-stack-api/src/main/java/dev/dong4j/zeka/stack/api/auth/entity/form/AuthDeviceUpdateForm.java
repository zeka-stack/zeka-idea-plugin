package dev.dong4j.zeka.stack.api.auth.entity.form;

import java.io.Serial;

import dev.dong4j.zeka.kernel.common.base.BaseForm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备 ID 变更
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "设备 ID 变更")
public class AuthDeviceUpdateForm extends BaseForm<Long> {
    /**
     * 序列化版本 UID
     * <p>
     * 该字段用于支持 Java 对象的序列化和反序列化过程.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 新的 deviceId */
    @Schema(description = "新的 deviceId")
    @NotBlank(message = "[deviceId] 必填")
    private String deviceId;
}
