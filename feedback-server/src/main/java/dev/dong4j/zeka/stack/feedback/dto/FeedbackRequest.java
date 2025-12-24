package dev.dong4j.zeka.stack.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

/**
 * 反馈请求 DTO
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class FeedbackRequest {

    /**
     * 反馈标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 反馈内容
     */
    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000个字符")
    private String content;

    /**
     * 反馈类型
     */
    @NotNull(message = "反馈类型不能为空")
    private FeedbackType type;

    /**
     * 讨论类别
     */
    private DiscussionCategory category;

    /**
     * 用户信息
     */
    @Valid
    @NotNull(message = "用户信息不能为空")
    @JsonProperty("userInfo")
    private UserInfo userInfo;

    /**
     * 元数据
     */
    @JsonProperty("metadata")
    private Metadata metadata;

    /**
     * 反馈类型枚举
     */
    @Getter
    public enum FeedbackType {
        BUG("bug", "Bug 报告"),
        FEATURE("feature", "功能建议"),
        QUESTION("question", "使用问题"),
        OTHER("other", "其他");

        private final String value;
        private final String description;

        FeedbackType(String value, String description) {
            this.value = value;
            this.description = description;
        }

    }

    /**
     * 讨论类别枚举
     */
    @Getter
    public enum DiscussionCategory {
        GENERAL("general", "一般讨论"),
        IDEAS("ideas", "想法建议"),
        QA("qa", "问答"),
        ANNOUNCEMENTS("announcements", "公告");

        private final String value;
        private final String description;

        DiscussionCategory(String value, String description) {
            this.value = value;
            this.description = description;
        }

    }

    /**
     * 用户信息
     */
    @Data
    public static class UserInfo {
        /**
         * 用户姓名（可选）
         */
        @Size(max = 100, message = "姓名长度不能超过100个字符")
        private String name;

        /**
         * 用户邮箱（可选）
         */
        @Size(max = 255, message = "邮箱长度不能超过255个字符")
        private String email;

        /**
         * GitHub 用户名（可选，用于 @ 提及）
         */
        @Size(max = 39, message = "GitHub 用户名长度不能超过39个字符")
        private String githubUsername;

        /**
         * 插件版本
         */
        @Size(max = 50, message = "插件版本长度不能超过50个字符")
        private String pluginVersion;

        /**
         * IDEA 版本
         */
        @Size(max = 50, message = "IDEA 版本长度不能超过50个字符")
        private String ideaVersion;

        /**
         * 操作系统信息
         */
        @Size(max = 100, message = "操作系统信息长度不能超过100个字符")
        private String os;
    }

    /**
     * 元数据
     */
    @Data
    public static class Metadata {
        /**
         * 客户端唯一标识
         */
        private String clientId;

        /**
         * 提交时间戳
         */
        private Long timestamp;
    }
}

