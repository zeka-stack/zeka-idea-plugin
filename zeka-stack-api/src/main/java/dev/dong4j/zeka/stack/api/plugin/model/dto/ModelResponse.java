package dev.dong4j.zeka.stack.api.plugin.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型响应数据类
 * <p> 用于封装模型查询和检索结果, 包含提供者名称, 模型列表, 总数量等字段. 该类为数据类, 仅用于数据传输, 不负责请求处理或业务逻辑.</p>
 * <p> 通过 {@code @Data},{@code @Builder},{@code @NoArgsConstructor},{@code @AllArgsConstructor} 和 {@code @JsonInclude} 注解自动生成标准
 * getter/setter, 构建器, 无参构造, 全参构造及序列化时忽略空值等行为.</p>
 * <p> 适用于领域层, 避免与基础设施层耦合, 仅作为数据载体使用.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelResponse implements Serializable {

    /** 序列化版本 UID, 用于兼容序列化机制 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 提供商名称 */
    private String provider;

    /** 模型列表 */
    private List<String> models;

    /** 模型总数 */
    private Integer total;
}
