package dev.dong4j.zeka.stack.api.plugin.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

/**
 * 反馈请求数据模型类
 * <p>用于封装用户提交的反馈信息, 包括标题, 内容, 反馈类型, 讨论分类, 用户信息及元数据等字段.
 * 该类采用 Lombok 注解简化代码, 支持数据校验 (如非空, 长度限制) 和枚举类型定义.
 * 适用于内部服务组件调用, 用于统一接收和处理用户反馈请求, 避免基础设施关注, 符合面向对象设计原则.
 * <p>主要包含以下嵌套结构:
 * <ul>
 *   <li>{@code FeedbackType}: 反馈类型枚举, 支持 Bug 报告, 功能建议, 使用问题, 其他等分类</li>
 *   <li>{@code DiscussionCategory}: 讨论分类枚举, 支持一般讨论, 想法建议, 问答, 公告等</li>
 *   <li>{@code UserInfo}: 用户信息数据类, 包含姓名, 邮箱,GitHub 用户名, 插件名称, 版本,IDEA 版本, 操作系统等字段</li>
 *   <li>{@code Metadata}: 元数据数据类, 包含客户端标识和时间戳</li>
 * </ul>
 * <p>所有字段均通过校验注解确保数据完整性与格式合规, 如标题和内容的长度限制, 必填字段校验等.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Data
public class FeedbackRequest {

    /** 反馈标题, 最大长度为 255 个字符 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /** 反馈内容, 不能为空且长度不能超过 10000 个字符 */
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

    /** 元数据, 包含客户端标识和时间戳信息, 用于记录请求来源或审计追踪等场景 */
    @JsonProperty("metadata")
    private Metadata metadata;

    /**
     * 反馈类型枚举
     * <p> 用于定义系统中用户提交反馈的类型, 包括 Bug 报告, 功能建议, 使用问题和其他类型. 每个类型包含唯一标识值和中文描述, 便于前端展示和后端分类处理.
     * <p> 该枚举不负责请求处理, 仅作为数据字典使用, 避免基础设施关注, 符合面向对象设计原则.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.16
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

        /** 反馈类型的值, 用于标识不同的反馈类别 */
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
     * 讨论分类枚举类
     * <p> 用于定义和标识不同类型的讨论板块, 支持通过值和描述进行映射, 适用于讨论区, 社区等场景中的分类管理.
     * 本枚举包含以下分类:
     * <ul>
     *   <li><code>GENERAL</code> - 一般讨论, 用于日常交流和非特定主题的讨论 </li>
     *   <li><code>IDEAS</code> - 想法建议, 用于收集用户创意或产品改进建议 </li>
     *   <li><code>QA</code> - 问答, 用于技术或业务问题的解答与交流 </li>
     *   <li><code>ANNOUNCEMENTS</code> - 公告, 用于发布官方通知或重要信息 </li>
     * </ul>
     * 本枚举不负责请求处理, 仅作为数据模型用于分类标识, 由服务组件调用以实现分类逻辑.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.16
     * @since 1.0.0
     */
    @Getter
    public enum DiscussionCategory {
        /** 一般讨论类别, 用于日常交流和非特定主题的讨论 */
        GENERAL("general", "一般讨论"),
        /** 表示想法建议类别的枚举值 */
        IDEAS("ideas", "想法建议"),
        /** 问答类别对应的值 */
        QA("qa", "问答"),
        /** 公告类别的枚举值 */
        ANNOUNCEMENTS("announcements", "公告");

        /** 该字段表示枚举值的原始字符串标识 */
        private final String value;
        /** 枚举项的描述信息, 用于说明该讨论类别的用途或含义 */
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
     * 用户信息数据类
     * <p> 用于封装和校验用户在插件中提交的个人信息, 包括姓名, 邮箱,GitHub 用户名, 插件名称与版本,IDEA 版本及操作系统信息等字段. 该类使用 Lombok @Data 注解自动生成 getter,setter,toString,equals 和
     * hashCode 方法, 并通过 Bean Validation 注解对字段长度进行约束校验.
     * <p> 本类设计为纯数据容器, 不包含业务逻辑, 仅用于数据传输与校验, 避免与基础设施或请求处理逻辑耦合.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.16
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

        /** GitHub 用户名 (可选, 用于 @ 提及), 长度不能超过 39 个字符 */
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

        /** 操作系统信息 <p> 包含操作系统名称, 版本等详细信息 <a href="https://en.wikipedia.org/wiki/Operating_system"> 操作系统 </a></p> */
        @Size(max = 100, message = "操作系统信息长度不能超过100个字符")
        private String os;
    }

    /**
     * 元数据实体类
     * <p> 用于封装和传递系统中与客户端相关联的元数据信息, 包括客户端标识符和时间戳. 该类为数据类, 仅用于数据持有, 不包含业务逻辑.</p>
     * <p> 设计意图: 避免基础设施关注, 面向对象设计, 不负责请求处理, 仅作为内部数据结构使用.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.16
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

