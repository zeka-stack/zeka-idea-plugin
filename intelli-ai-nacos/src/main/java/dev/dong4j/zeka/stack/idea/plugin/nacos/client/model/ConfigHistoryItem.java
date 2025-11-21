package dev.dong4j.zeka.stack.idea.plugin.nacos.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Nacos 配置历史版本信息模型
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigHistoryItem {
    /**
     * 历史配置 ID
     */
    @JsonProperty("id")
    private String id;

    /**
     * 上一版本 ID
     */
    @JsonProperty("lastId")
    private String lastId;

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
     * 租户（命名空间）
     */
    @JsonProperty("tenant")
    private String tenant;

    /**
     * 应用名
     */
    @JsonProperty("appName")
    private String appName;

    /**
     * 配置内容
     */
    @JsonProperty("content")
    private String content;

    /**
     * MD5 值
     */
    @JsonProperty("md5")
    private String md5;

    /**
     * 源 IP
     */
    @JsonProperty("srcIp")
    private String srcIp;

    /**
     * 源用户
     */
    @JsonProperty("srcUser")
    private String srcUser;

    /**
     * 操作类型：I(插入)、U(更新)、D(删除)
     */
    @JsonProperty("opType")
    private String opType;

    /**
     * 创建时间
     */
    @JsonProperty("createdTime")
    private String createdTime;

    /**
     * 最后修改时间
     */
    @JsonProperty("lastModifiedTime")
    private String lastModifiedTime;

    /**
     * 加密数据密钥
     */
    @JsonProperty("encryptedDataKey")
    private String encryptedDataKey;
}

