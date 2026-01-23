package dev.dong4j.zeka.stack.api.plugin.feedback.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.manager.github.FeedbackService;
import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackRequest;
import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackResponse;
import dev.dong4j.zeka.starter.rest.annotation.RestControllerWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 反馈控制器类
 * <p> 负责处理与用户反馈相关的 HTTP 请求, 包括提交反馈和健康检查接口. 该控制器不直接处理请求细节, 而是将业务逻辑委托给 {@code FeedbackService}, 确保关注点分离, 符合面向对象设计原则.</p>
 * <p> 控制器仅作为业务逻辑的入口, 不包含基础设施或请求处理逻辑, 确保系统架构清晰, 职责单一.</p>
 * <p> 支持的接口路径:<pre>{@code
 * POST /api/plugin/feedback/discussion
 * POST /api/plugin/feedback/issue
 * GET /api/plugin/feedback/health
 * }</pre></p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.16
 * @since 1.0.0
 */
@Slf4j
@RestControllerWrapper("/plugin")
@RequiredArgsConstructor
public class FeedbackController {

    /** 反馈服务实例, 用于处理反馈提交及相关业务逻辑 */
    private final FeedbackService feedbackService;

    /**
     * 提交反馈信息
     * <p> 接收反馈请求对象, 记录调试日志, 调用服务层处理反馈提交, 并根据处理结果返回对应的 HTTP 响应.
     * 若提交成功, 返回状态码 200 和成功响应; 若失败, 返回状态码 500 和错误响应.
     *
     * @param request 反馈请求对象, 包含反馈类型, 标题及用户信息 (如 GitHub 用户名)
     * @return 包含提交结果的 HTTP 响应, 类型为 {@code ResponseEntity<FeedbackResponse>}
     */
    @PostMapping("/feedback/discussion")
    public FeedbackResponse submitDiscussion(@Valid @RequestBody FeedbackRequest request) {
        log.debug("Received discussion submission: type={}, title={}, githubUsername={}",
                  request.getType(), request.getTitle(),
                  request.getUserInfo() != null ? request.getUserInfo().getGithubUsername() : "N/A");

        FeedbackResponse response = feedbackService.submitFeedback(request);
        BaseCodes.SUCCESS.isTrue(response.getSuccess());
        return response;
    }

    /**
     * 提交反馈为 Issue
     *
     * @param request 反馈请求对象
     * @return 包含提交结果的 HTTP 响应
     */
    @PostMapping("/feedback/issue")
    public FeedbackResponse submitIssue(@Valid @RequestBody FeedbackRequest request) {
        log.debug("Received issue submission: type={}, title={}, githubUsername={}",
                  request.getType(), request.getTitle(),
                  request.getUserInfo() != null ? request.getUserInfo().getGithubUsername() : "N/A");

        FeedbackResponse response = feedbackService.submitIssue(request);
        BaseCodes.SUCCESS.isTrue(response.getSuccess());
        return response;
    }

}
