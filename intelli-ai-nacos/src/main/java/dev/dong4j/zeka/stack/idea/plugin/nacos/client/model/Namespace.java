package dev.dong4j.zeka.stack.idea.plugin.nacos.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Nacos 命名空间模型
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Namespace {
    /**
     * 命名空间 ID
     */
    @JsonProperty("namespace")
    private String namespaceId;

    /**
     * 命名空间名称
     */
    @JsonProperty("namespaceShowName")
    private String namespaceName;

    /**
     * 命名空间描述
     */
    @JsonProperty("namespaceDesc")
    private String description;

    /**
     * 配置数量
     */
    @JsonProperty("configCount")
    private int configCount;

    /**
     * 类型
     */
    @JsonProperty("type")
    private int type;
}