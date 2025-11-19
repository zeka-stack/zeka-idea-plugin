package dev.dong4j.zeka.stack.idea.plugin.nacos.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Nacos 配置信息模型
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigInfo {
    /**
     * 数据 ID
     */
    @JsonProperty("dataId")
    private String dataId;

    /**
     * 分组
     */
    @JsonProperty("group")
    private String group;

    /**
     * 内容
     */
    @JsonProperty("content")
    private String content;

    /**
     * 配置类型
     */
    @JsonProperty("type")
    private String type;

    /**
     * 应用名
     */
    @JsonProperty("appName")
    private String appName;

    /**
     * 描述
     */
    @JsonProperty("desc")
    private String description;

    /**
     * 使用标签
     */
    @JsonProperty("useTags")
    private String useTags;

    /**
     * 最后修改时间
     */
    @JsonProperty("lastModified")
    private long lastModified;

    /**
     * 创建时间
     */
    @JsonProperty("createTime")
    private long createTime;

    /**
     * 配置标签列表
     */
    @JsonProperty("configTags")
    private String configTags;
}