package dev.dong4j.zeka.stack.feedback.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import dev.dong4j.zeka.stack.feedback.client.GitHubGraphQLClient;
import dev.dong4j.zeka.stack.feedback.config.GitHubProperties;
import dev.dong4j.zeka.stack.feedback.dto.FeedbackRequest;
import dev.dong4j.zeka.stack.feedback.dto.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 反馈服务
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final GitHubGraphQLClient gitHubClient;
    private final GitHubProperties gitHubProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 提交反馈
     *
     * @param request 反馈请求
     * @return 反馈响应
     */
    public FeedbackResponse submitFeedback(FeedbackRequest request) {
        try {
            // 构建讨论标题
            String title = buildTitle(request);

            // 构建讨论内容
            String body = buildBody(request);

            // 获取类别 ID
            String categoryId = getCategoryId(request.getCategory());

            // 创建讨论
            FeedbackResponse.DiscussionInfo discussionInfo = gitHubClient.createDiscussion(
                categoryId, title, body);

            return FeedbackResponse.builder()
                .success(true)
                .discussion(discussionInfo)
                .message("反馈已成功提交")
                .build();

        } catch (IOException e) {
            log.error("Failed to submit feedback", e);
            return FeedbackResponse.builder()
                .success(false)
                .error("提交反馈失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 构建讨论标题
     */
    private String buildTitle(FeedbackRequest request) {
        String typeLabel = request.getType().getDescription();
        return String.format("[插件反馈] %s: %s", typeLabel, request.getTitle());
    }

    /**
     * 构建讨论内容
     */
    private String buildBody(FeedbackRequest request) {
        StringBuilder body = new StringBuilder();

        // GitHub 用户名 @ 提及（如果提供）
        FeedbackRequest.UserInfo userInfo = request.getUserInfo();
        if (StringUtils.hasText(userInfo.getGithubUsername())) {
            body.append("**提交人**: @").append(userInfo.getGithubUsername()).append("\n\n");
        }

        // 反馈内容
        body.append("## 反馈内容\n\n");
        body.append(request.getContent()).append("\n\n");

        // 分隔线
        body.append("---\n\n");

        // 提交人信息
        body.append("## 提交人信息\n\n");

        if (StringUtils.hasText(userInfo.getName())) {
            body.append("- **姓名**: ").append(escapeMarkdown(userInfo.getName())).append("\n");
        }
        if (StringUtils.hasText(userInfo.getEmail())) {
            body.append("- **邮箱**: ").append(escapeMarkdown(userInfo.getEmail())).append("\n");
        }
        if (StringUtils.hasText(userInfo.getGithubUsername())) {
            body.append("- **GitHub**: @").append(userInfo.getGithubUsername()).append("\n");
        }
        if (StringUtils.hasText(userInfo.getPluginVersion())) {
            body.append("- **插件版本**: ").append(escapeMarkdown(userInfo.getPluginVersion())).append("\n");
        }
        if (StringUtils.hasText(userInfo.getIdeaVersion())) {
            body.append("- **IDEA 版本**: ").append(escapeMarkdown(userInfo.getIdeaVersion())).append("\n");
        }
        if (StringUtils.hasText(userInfo.getOs())) {
            body.append("- **操作系统**: ").append(escapeMarkdown(userInfo.getOs())).append("\n");
        }

        body.append("- **反馈类型**: ").append(request.getType().getDescription()).append("\n");
        body.append("- **提交时间**: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n");

        return body.toString();
    }

    /**
     * 获取类别 ID
     */
    private String getCategoryId(FeedbackRequest.DiscussionCategory category) {
        if (category == null) {
            category = FeedbackRequest.DiscussionCategory.GENERAL;
        }

        String categoryKey = category.getValue();
        String categoryId = gitHubProperties.getCategory().get(categoryKey);

        if (!StringUtils.hasText(categoryId)) {
            log.warn("Category ID not found for category: {}, using GENERAL", categoryKey);
            categoryId = gitHubProperties.getCategory().get("general");
        }

        if (!StringUtils.hasText(categoryId)) {
            throw new IllegalStateException("Category ID not configured for: " + categoryKey);
        }

        return categoryId;
    }

    /**
     * 转义 Markdown 特殊字符
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
            .replace("*", "\\*")
            .replace("_", "\\_")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("#", "\\#")
            .replace("+", "\\+")
            .replace("-", "\\-")
            .replace(".", "\\.")
            .replace("!", "\\!");
    }
}

