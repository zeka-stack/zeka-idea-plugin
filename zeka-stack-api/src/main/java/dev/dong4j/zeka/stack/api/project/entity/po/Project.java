package dev.dong4j.zeka.stack.api.project.entity.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;

import dev.dong4j.zeka.starter.mybatis.base.BaseWithTimePO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p> 项目表 实体类  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("project")
public class Project extends BaseWithTimePO<Long, Project> {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = 1L;
    /** 项目唯一标识 (如 zeka-idea-plugin)-表字段 */
    public static final String KEY = "key";
    /** 项目显示名称-表字段 */
    public static final String NAME = "name";
    /** 项目描述-表字段 */
    public static final String DESCRIPTION = "description";
    /** GitHub 仓库 URL-表字段 */
    public static final String REPOS = "repos";
    /** 图标标识(前端映射 lucide 图标)-表字段 */
    public static final String ICON = "icon";
    /** 排序权重-表字段 */
    public static final String SORT_ORDER = "sort_order";
    /** 状态: 1-启用, 0-禁用-表字段 */
    public static final String STATUS = "status";

    /** 项目唯一标识 (如 zeka-idea-plugin) */
    @TableField("`key`")
    private String key;
    /** 项目显示名称 */
    @TableField("`name`")
    private String name;
    /** 项目描述 */
    @TableField("`description`")
    private String description;
    /** GitHub 仓库 URL (如 <a href="https://github.com/zeka-stack/zeka-idea-plugin">...</a>) */
    @TableField("`repos`")
    private String repos;
    /** 图标标识(前端映射 lucide 图标) */
    @TableField("`icon`")
    private String icon;
    /** 排序权重 */
    @TableField("`sort_order`")
    private Integer sortOrder;
    /** 状态: 1-启用, 0-禁用 */
    @TableField("`status`")
    private Integer status;
}
