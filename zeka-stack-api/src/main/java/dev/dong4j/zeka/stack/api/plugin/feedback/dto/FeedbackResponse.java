package dev.dong4j.zeka.stack.api.plugin.feedback.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反馈响应数据结构类
 * <p> 用于封装反馈操作的响应结果, 包含操作是否成功, 讨论信息, 消息内容及错误信息等字段.
 * 该类采用 Lombok 注解自动生成 getter/setter/toString/equals/hashCode/ 构造函数等, 适用于内部服务间数据传递.
 * <p> 其中嵌套类 {@code DiscussionInfo} 用于封装讨论相关的信息, 如讨论 ID, 编号,URL 和标题.
 * 该类不负责请求处理, 仅作为数据载体, 避免基础设施关注, 符合面向对象设计原则.
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
public class FeedbackResponse {

    /** 是否成功 */
    private Boolean success;

    /**
     * 讨论信息
     * <p> 包含讨论的相关详情, 如讨论 ID, 编号,URL 和标题.
     *
     * @see DiscussionInfo
     */
    private DiscussionInfo discussion;

    /**
     * Issue 信息
     * <p> 包含 issue 的相关详情, 如 ID, 编号,URL 和标题.
     *
     * @see IssueInfo
     */
    private IssueInfo issue;

    /** 反馈响应中的相关信息或提示文本 */
    private String message;

    /** 错误信息 <p> 包含在反馈响应中, 当请求失败时提供详细的错误描述. */
    private String error;

    /**
     * 讨论信息数据类
     * <p> 用于封装和传递讨论主题的结构化数据, 包含唯一标识, 编号,URL, 标题等核心字段.
     * 该类采用 Lombok 注解自动生成 getter,setter,toString,equals,hashCode 及构建器方法,
     * 适用于内部系统中讨论信息的封装与传输, 不负责请求处理或业务逻辑.
     * 通常由服务层或组件层用于数据封装和传递, 避免基础设施关注, 符合面向对象设计原则.
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
    public static class DiscussionInfo {
        /** 讨论 ID */
        private String id;

        /** 讨论编号 */
        private Integer number;

        /** 讨论 URL, 该字段表示讨论的网络地址, 用于指向具体的讨论页面. */
        private String url;

        /** 讨论标题 */
        private String title;
    }

    /**
     * Issue 信息数据类
     * <p> 用于封装和传递 Issue 的结构化数据.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.19
     * @since 1.0.0
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueInfo {
        /** Issue ID */
        private String id;

        /** Issue 编号 */
        private Integer number;

        /** Issue URL */
        private String url;

        /** Issue 标题 */
        private String title;
    }
}
