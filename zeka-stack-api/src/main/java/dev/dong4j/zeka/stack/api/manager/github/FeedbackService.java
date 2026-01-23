package dev.dong4j.zeka.stack.api.manager.github;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import dev.dong4j.zeka.stack.api.manager.github.client.GitHubGraphQLClient;
import dev.dong4j.zeka.stack.api.manager.github.client.GitHubIssueClient;
import dev.dong4j.zeka.stack.api.manager.github.config.GitHubProperties;
import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackRequest;
import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 反馈服务类
 * <p> 负责处理用户提交的反馈信息, 将反馈内容以 Markdown 格式构建并提交至 GitHub 讨论区, 支持分类管理与信息收集. 该服务不负责请求处理, 仅作为业务逻辑层组件, 依赖外部 GitHub GraphQL 客户端完成实际提交操作.</p>
 * <p> 主要功能包括:</p>
 * <ul>
 *   <li> 根据反馈请求构建标题与正文内容, 支持 Markdown 格式转义 </li>
 *   <li> 根据反馈分类获取对应的 GitHub 讨论区分类 ID</li>
 *   <li> 调用 <code>{@code GitHubGraphQLClient}</code> 创建讨论帖并返回结果 </li>
 *   <li> 异常情况下记录日志并返回失败响应 </li>
 * </ul>
 * <p> 该服务适用于内部系统中用户反馈收集场景, 通过集成 GitHub 讨论区实现结构化反馈管理.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    /** GitHub GraphQL 客户端实例, 用于与 GitHub 的 GraphQL API 进行交互, 执行如创建讨论等操作. */
    private final GitHubGraphQLClient gitHubClient;
    /** GitHub Issue 客户端实例, 用于创建 Issue */
    private final GitHubIssueClient gitHubIssueClient;
    /** GitHub 配置属性, 包含与 GitHub 相关的配置信息, 例如讨论类别映射等 */
    private final GitHubProperties gitHubProperties;

    /** 日期格式化器, 用于将本地日期时间格式化为 "yyyy-MM-dd HH:mm:ss" 格式 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 提交反馈
     * <p> 根据反馈请求构建标题和内容, 并在 GitHub 上创建一个新的讨论, 返回反馈响应.
     * <p> 如果提交过程中出现异常, 则记录错误日志并返回失败的反馈响应.
     *
     * @param request 包含反馈信息的请求对象, 不能为空
     * @return 反馈响应对象, 包含提交结果和相关信息
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
            log.debug("Failed to submit feedback", e);
            return FeedbackResponse.builder()
                .success(false)
                .error("提交反馈失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 提交反馈为 Issue
     * <p> 根据反馈请求构建标题和内容, 并在 GitHub 上创建一个新的 Issue, 返回反馈响应.</p>
     *
     * @param request 包含反馈信息的请求对象, 不能为空
     * @return 反馈响应对象, 包含提交结果和相关信息
     */
    public FeedbackResponse submitIssue(FeedbackRequest request) {
        try {
            String title = buildTitle(request);
            String body = buildBody(request);

            FeedbackResponse.IssueInfo issueInfo = gitHubIssueClient.createIssue(title, body);

            return FeedbackResponse.builder()
                .success(true)
                .issue(issueInfo)
                .message("反馈已成功提交")
                .build();
        } catch (IOException e) {
            log.debug("Failed to submit issue feedback", e);
            return FeedbackResponse.builder()
                .success(false)
                .error("提交反馈失败: " + e.getMessage())
                .build();
        }
    }

    /**
     * 构建讨论标题
     * <p> 根据反馈请求中的类型描述和插件名称生成讨论标题
     * <p> 如果用户信息中的插件名称为空, 则使用默认值“插件”
     *
     * @param request 反馈请求
     * @return 构建好的讨论标题
     */
    private String buildTitle(FeedbackRequest request) {
        String typeLabel = request.getType().getDescription();
        FeedbackRequest.UserInfo userInfo = request.getUserInfo();
        String pluginName = userInfo != null && StringUtils.hasText(userInfo.getPluginName())
                            ? userInfo.getPluginName()
                            : "插件";
        return String.format("[%s 反馈] %s: %s", pluginName, typeLabel, request.getTitle());
    }

    /**
     * 构建反馈讨论的内容
     * <p> 根据反馈请求中的用户信息和反馈内容, 生成详细的反馈讨论内容文本
     * <p> 内容包括提交人的 GitHub 用户名, 反馈内容, 提交人信息 (如姓名, 邮箱,GitHub 用户名, 插件名称, 插件版本,IDEA 版本, 操作系统等), 反馈类型和提交时间
     *
     * @param request 包含反馈信息和用户信息的反馈请求对象
     * @return 构建好的反馈讨论内容文本
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
        if (StringUtils.hasText(userInfo.getPluginName())) {
            body.append("- **插件名称**: ").append(escapeMarkdown(userInfo.getPluginName())).append("\n");
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
     * 获取讨论类别对应的 ID
     * <p> 根据传入的反馈讨论类别, 查找其在 GitHub 配置中对应的类别 ID. 若类别为空, 则默认使用“GENERAL”类别.
     * <p> 若未找到指定类别的 ID, 则记录调试日志并尝试使用默认的“general”类别 ID; 若仍失败, 则抛出非法状态异常.
     *
     * @param category 反馈请求中的讨论类别, 若为 null 则默认使用 GENERAL 类别
     * @return 对应的类别 ID
     */
    private String getCategoryId(FeedbackRequest.DiscussionCategory category) {
        if (category == null) {
            category = FeedbackRequest.DiscussionCategory.GENERAL;
        }

        String categoryKey = category.getValue();
        String categoryId = gitHubProperties.getCategory().get(categoryKey);

        if (!StringUtils.hasText(categoryId)) {
            log.debug("Category ID not found for category: {}, using GENERAL", categoryKey);
            categoryId = gitHubProperties.getCategory().get("general");
        }

        if (!StringUtils.hasText(categoryId)) {
            throw new IllegalStateException("Category ID not configured for: " + categoryKey);
        }

        return categoryId;
    }

    /**
     * 转义 Markdown 特殊字符
     * <p> 将可能导致 Markdown 解析错误的特殊字符进行转义, 确保文本在 Markdown 中正确显示
     *
     * @param text 需要转义的文本
     * @return 转义后的文本
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
