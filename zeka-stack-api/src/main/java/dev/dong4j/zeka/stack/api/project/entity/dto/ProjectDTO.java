package dev.dong4j.zeka.stack.api.project.entity.dto;

import java.io.Serial;

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
 * <p> 项目表 数据传输实体 (根据业务需求添加字段) </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "项目表-数传传输对象")
public class ProjectDTO extends BaseDTO<Long> {
    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 项目唯一标识 (如 zeka-idea-plugin) */
    @Schema(description = "项目唯一标识 (如 zeka-idea-plugin)")
    private String key;
    /** 项目显示名称 */
    @Schema(description = "项目显示名称")
    private String name;
    /** 项目描述 */
    @Schema(description = "项目描述")
    private String description;
    /** GitHub 仓库 URL (如 https://github.com/zeka-stack/zeka-idea-plugin) */
    @Schema(description = "GitHub 仓库 URL (如 https://github.com/zeka-stack/zeka-idea-plugin)")
    private String repos;
    /** 图标标识(前端映射 lucide 图标) */
    @Schema(description = "图标标识(前端映射 lucide 图标)")
    private String icon;
    /** 排序权重 */
    @Schema(description = "排序权重")
    private Integer sortOrder;
    /** 状态: 1-启用, 0-禁用 */
    @Schema(description = "状态: 1-启用, 0-禁用")
    private Integer status;
}
