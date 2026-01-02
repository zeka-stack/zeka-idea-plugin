package dev.dong4j.zeka.stack.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈响应类
 * <p> 用于封装反馈请求的结果信息, 包括成功标识, 讨论信息, 消息和错误信息
 * <p> 该类使用 Lombok 库中的多个注解来简化代码, 包括自动生成的 getter,setter, 构造函数和 builder 方法
 * <p> 嵌套的 DiscussionInfo 类用于表示讨论的相关信息, 包括讨论的 ID, 编号,URL 和标题
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedbackResponse {

    /**
     * 是否成功
     * <p> 表示反馈操作是否成功执行
     */
    private Boolean success;

    /**
     * 讨论信息
     * <p>
     * 包含讨论的相关详情, 如讨论 ID, 编号,URL 和标题.
     *
     * @see DiscussionInfo
     */
    private DiscussionInfo discussion;

    /**
     * 消息
     * <p> 包含反馈响应中的相关信息或提示文本
     */
    private String message;

    /**
     * 错误信息
     * <p>
     * 包含在反馈响应中, 当请求失败时提供详细的错误描述.
     */
    private String error;

    /**
     * 讨论信息数据类
     * <p> 用于存储和传递讨论的相关信息, 包括讨论的唯一标识符, 编号,URL 和标题.
     * <p> 该类使用了 Lombok 的相关注解来简化代码, 提供了无参构造函数, 全参构造函数, 构建者模式以及自动的 getter 方法.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscussionInfo {
        /** 讨论 ID */
        private String id;

        /** 讨论编号 */
        private Integer number;

        /**
         * 讨论 URL
         * <p>
         * 该字段表示讨论的网络地址, 用于指向具体的讨论页面.
         */
        private String url;

        /** 讨论标题 */
        private String title;
    }
}

