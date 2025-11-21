package dev.dong4j.zeka.stack.idea.plugin.nacos.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.Data;

/**
 * Nacos 配置历史版本列表响应模型
 *
 * @author dong4j
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigHistoryListResponse {
    /**
     * 总数
     */
    @JsonProperty("totalCount")
    private Integer totalCount;

    /**
     * 当前页
     */
    @JsonProperty("pageNumber")
    private Integer pageNumber;

    /**
     * 总页数
     */
    @JsonProperty("pagesAvailable")
    private Integer pagesAvailable;

    /**
     * 历史配置项列表
     */
    @JsonProperty("pageItems")
    private List<ConfigHistoryItem> pageItems;
}

