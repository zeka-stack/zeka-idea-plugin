package dev.dong4j.zeka.stack.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Feedback Response
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-23 09:40:06
 * @since hello.world
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedbackResponse {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 讨论信息
     */
    private DiscussionInfo discussion;

    /**
     * 消息
     */
    private String message;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 讨论信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscussionInfo {
        /**
         * 讨论 ID
         */
        private String id;

        /**
         * 讨论编号
         */
        private Integer number;

        /**
         * 讨论 URL
         */
        private String url;

        /**
         * 讨论标题
         */
        private String title;
    }
}

