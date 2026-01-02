package dev.dong4j.zeka.stack.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

/**
 * 反馈请求数据模型
 * <p> 用于封装用户提交的反馈信息, 包括标题, 内容, 反馈类型, 讨论分类, 用户信息及附加元数据
 * <p> 该类支持数据校验, 确保输入数据的完整性和有效性, 适用于前端提交反馈或系统间通信场景
 * <p> 使用示例:
 * <pre>{@code
 * FeedbackRequest request = new FeedbackRequest();
 * request.setTitle("功能建议");
 * request.setContent("希望增加一个导出功能");
 * request.setType(FeedbackType.FEATURE);
 * request.setUserInfo(new UserInfo());
 * request.getMetadata().setClientId("client-123");
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@Data
public class FeedbackRequest {

    /** 反馈标题, 最大长度为 255 个字符 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 反馈内容
     * <p> 用户提交的反馈详细信息, 不能为空且长度不能超过 10000 个字符
     *
     * @see #content
     */
    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;

    /** 反馈类型, 不能为空 */
    @NotNull(message = "反馈类型不能为空")
    private FeedbackType type;

    /** 讨论类别 */
    private DiscussionCategory category;

    /**
     * 用户信息, 包含插件名称, 版本,IDEA 版本等环境信息
     * <p> 该字段用于标识提交反馈的用户及其运行环境
     *
     * @see UserInfo
     */
    @Valid
    @NotNull(message = "用户信息不能为空")
    @JsonProperty("userInfo")
    private UserInfo userInfo;

    /** 元数据 */
    @JsonProperty("metadata")
    private Metadata metadata;

    /**
     * 反馈类型枚举
     * <p>用于标识用户反馈的不同类别, 包括 Bug 报告, 功能建议, 使用问题以及其他类型
     * <p>每个枚举值包含一个对应的字符串标识 (value) 和描述信息(description), 可用于系统内部处理或展示
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    @Getter
    public enum FeedbackType {
        /** Bug 报告 */
        BUG("bug", "Bug 报告"),
        /** 功能建议类型的反馈 */
        FEATURE("feature", "功能建议"),
        /** 反馈类型为使用问题 */
        QUESTION("question", "使用问题"),
        /** 其他反馈类型 */
        OTHER("other", "其他");

        /** 该字段表示反馈类型的值, 用于标识不同的反馈类别 */
        private final String value;
        /** 描述反馈类型的详细信息, 例如“Bug 报告”或“功能建议” */
        private final String description;

        /**
         * 构造函数, 初始化反馈类型的值和描述信息
         * <p> 该构造函数用于创建一个反馈类型对象, 设置其值和描述
         *
         * @param value       反馈类型的值
         * @param description 反馈类型的描述
         */
        FeedbackType(String value, String description) {
            this.value = value;
            this.description = description;
        }

    }

    /**
     * 讨论分类枚举
     * <p> 定义了不同类型的讨论主题分类, 用于对社区或论坛中的帖子进行分类管理
     * <p> 支持以下分类:
     * <ul>
     *   <li>{@code GENERAL} - 一般讨论, 用于日常交流和非特定主题的讨论 </li>
     *   <li>{@code IDEAS} - 想法建议, 用于分享新想法或提出改进建议 </li>
     *   <li>{@code QA} - 问答, 用于回答和提出问题 </li>
     *   <li>{@code ANNOUNCEMENTS} - 公告, 用于发布重要通知或官方消息 </li>
     * </ul>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    @Getter
    public enum DiscussionCategory {
        /** 一般讨论类别 */
        GENERAL("general", "一般讨论"),
        /** 表示想法建议类别的枚举值 */
        IDEAS("ideas", "想法建议"),
        /**
         * 问答类别对应的值
         *
         * @see DiscussionCategory
         */
        QA("qa", "问答"),
        /** 公告类别的枚举值 */
        ANNOUNCEMENTS("announcements", "公告");

        /** 该字段表示枚举值的原始字符串标识 */
        private final String value;
        /**
         * 枚举项的描述信息
         * <p> 用于说明该讨论类别的用途或含义
         */
        private final String description;

        /**
         * 构造讨论类别枚举项
         * <p> 初始化讨论类别的值和描述信息
         *
         * @param value       类别值, 用于标识和比较
         * @param description 类别描述, 用于展示给用户
         */
        DiscussionCategory(String value, String description) {
            this.value = value;
            this.description = description;
        }

    }

    /**
     * 用户信息类
     * <p> 用于封装用户相关的详细信息, 包括姓名, 邮箱,GitHub 用户名, 插件名称及版本,IDEA 版本和操作系统信息等字段.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    @Data
    public static class UserInfo {
        /** 用户姓名 (可选) */
        @Size(max = 100, message = "姓名长度不能超过100个字符")
        private String name;

        /** 用户邮箱 (可选) */
        @Size(max = 255, message = "邮箱长度不能超过255个字符")
        private String email;

        /**
         * GitHub 用户名 (可选, 用于 @ 提及)
         * <p>GitHub 用户名用于在平台中进行 @ 提及操作, 长度不能超过 39 个字符.</p>
         *
         * @see Size
         */
        @Size(max = 39, message = "GitHub 用户名长度不能超过39个字符")
        private String githubUsername;

        /**
         * 插件名称
         * <p> 表示当前用户的插件名称, 长度不能超过 100 个字符.</p>
         *
         * @see #setPluginName(String)
         * @see #getPluginName()
         */
        @Size(max = 100, message = "插件名称长度不能超过100个字符")
        private String pluginName;

        /** 插件版本, 最大长度为 50 个字符 */
        @Size(max = 50, message = "插件版本长度不能超过50个字符")
        private String pluginVersion;

        /** IDEA 版本 */
        @Size(max = 50, message = "IDEA 版本长度不能超过50个字符")
        private String ideaVersion;

        /**
         * 操作系统信息
         * <p> 包含操作系统名称, 版本等详细信息
         *
         * @see <a href="https://en.wikipedia.org/wiki/Operating_system"> 操作系统 </a>
         */
        @Size(max = 100, message = "操作系统信息长度不能超过100个字符")
        private String os;
    }

    /**
     * 元数据类
     * <p> 用于封装客户端标识和时间戳信息, 通常用于记录请求来源或审计追踪等场景
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.02
     * @since 1.0.0
     */
    @Data
    public static class Metadata {
        /** 客户端唯一标识 */
        private String clientId;

        /** 提交时间戳 */
        private Long timestamp;
    }
}

