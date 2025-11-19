package dev.dong4j.zeka.stack.idea.plugin.nacos.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Data;
import lombok.ToString;

/**
 * Nacos 命名空间列表响应模型
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class NamespaceListResponse {
    /**
     * 命名空间列表
     */
    @JsonProperty("data")
    private List<Namespace> namespaces;

    /**
     * 总数
     */
    @JsonProperty("totalCount")
    private int totalCount;

    /**
     * 页面大小
     */
    @JsonProperty("pageSize")
    private int pageSize;

    /**
     * 当前页码
     */
    @JsonProperty("pageNo")
    private int pageNo;
}