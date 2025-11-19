package dev.dong4j.zeka.stack.idea.plugin.nacos.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.ToString;

/**
 * Nacos API 响应结果通用模型
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result<T> {
    /**
     * 是否成功
     */
    @JsonProperty("success")
    private boolean success;

    /**
     * 响应数据
     */
    @JsonProperty("data")
    private T data;

    /**
     * 错误码
     */
    @JsonProperty("code")
    private int code;

    /**
     * 错误消息
     */
    @JsonProperty("message")
    private String message;
}