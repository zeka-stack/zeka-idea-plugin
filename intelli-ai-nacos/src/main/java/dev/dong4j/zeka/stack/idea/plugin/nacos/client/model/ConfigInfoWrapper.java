package dev.dong4j.zeka.stack.idea.plugin.nacos.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Nacos 配置信息包装器模型
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigInfoWrapper extends ConfigInfo {
    /**
     * 租户（命名空间）
     */
    @JsonProperty("tenant")
    private String tenant;

    /**
     * 是否为 beta 配置
     */
    @JsonProperty("beta")
    private boolean beta;

    /**
     * Schema
     */
    @JsonProperty("schema")
    private String schema;
}