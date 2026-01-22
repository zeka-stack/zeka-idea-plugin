package dev.dong4j.zeka.stack.api.manager.github.webhook;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dev.dong4j.zeka.stack.api.project.config.WebhookProperties;
import dev.dong4j.zeka.stack.api.project.entity.po.Feedback;
import dev.dong4j.zeka.stack.api.project.entity.po.Project;
import dev.dong4j.zeka.stack.api.project.service.FeedbackService;
import dev.dong4j.zeka.stack.api.project.service.ProjectService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub Webhook 控制器
 * <p> 处理 GitHub webhook 事件，同步 issues 状态到 feedback 表
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.23
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@AllArgsConstructor
public class WebhookController {

    /** 项目服务, 用于获取和管理项目相关数据 */
    private final ProjectService projectService;
    /** 反馈服务, 用于操作 feedback 表数据 */
    private final FeedbackService feedbackService;
    /** JSON 序列化与反序列化工具, 用于处理 webhook 请求体的结构化数据 */
    private final ObjectMapper objectMapper;
    /** GitHub Webhook 配置属性, 用于获取密钥和事件处理规则 */
    private final WebhookProperties webhookProperties;

    /** 幂等处理：记录已处理的事件 ID */
    private static final Map<String, Long> PROCESSED_EVENTS = new ConcurrentHashMap<>();

    /**
     * 处理 GitHub Webhook 事件
     *
     * @param signature GitHub webhook 签名
     * @param event     GitHub 事件类型
     * @param payload   请求体
     * @return 响应
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
        @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
        @RequestHeader(value = "X-GitHub-Event", required = false) String event,
        @RequestBody String payload) {

        try {
            log.debug("Received webhook event: {}, signature: {}", event, signature);

            // 1. 验证签名并获取 secret
            String secret = validateSignature(signature, payload);
            if (secret == null) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // 2. 根据 secret 找到对应的 project
            String projectKey = webhookProperties.getProjectKey(secret);
            if (projectKey == null) {
                log.warn("Unknown webhook secret: {}", secret);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unknown secret");
            }

            Project project = projectService.getOne(
                new LambdaQueryWrapper<Project>().eq(Project::getKey, projectKey));
            if (project == null) {
                log.warn("Project not found for key: {}", projectKey);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Project not found");
            }

            // 3. 只处理 issues 事件
            if (!"issues".equals(event)) {
                log.debug("Ignoring non-issues event: {}", event);
                return ResponseEntity.ok("Event ignored");
            }

            // 4. 解析 payload
            JsonNode jsonNode = objectMapper.readTree(payload);
            String action = jsonNode.get("action").asText();
            JsonNode issueNode = jsonNode.get("issue");
            if (issueNode == null) {
                log.warn("Issue node not found in payload");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
            }

            Long issueId = issueNode.get("id").asLong();
            String eventId = generateEventId(project.getId(), issueId, action);

            // 5. 幂等处理：检查是否已处理过
            Long lastProcessedTime = PROCESSED_EVENTS.get(eventId);
            if (lastProcessedTime != null && System.currentTimeMillis() - lastProcessedTime < 60000) {
                log.debug("Event already processed: {}", eventId);
                return ResponseEntity.ok("Event already processed");
            }

            // 6. 处理事件
            processIssuesEvent(project.getId(), action, issueId);

            // 7. 记录已处理的事件
            PROCESSED_EVENTS.put(eventId, System.currentTimeMillis());

            return ResponseEntity.ok("Event processed successfully");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * 验证签名并返回对应的 secret
     */
    private String validateSignature(String signature, String payload) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return null;
        }

        String receivedHash = signature.substring(7);

        // 尝试所有已知的 secret
        for (Map.Entry<String, String> entry : webhookProperties.getSecrets().entrySet()) {
            String secret = entry.getKey();
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
                mac.init(secretKeySpec);
                byte[] hashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
                String calculatedHash = bytesToHex(hashBytes);

                if (calculatedHash.equals(receivedHash)) {
                    return secret;
                }
            } catch (Exception e) {
                log.error("Error validating signature with secret: {}", secret, e);
            }
        }

        return null;
    }

    /**
     * 处理 issues 事件
     */
    private void processIssuesEvent(Long projectId, String action, Long issueId) {
        switch (action) {
            case "opened" -> onCreated(projectId, issueId);
            case "closed" -> onClosed(projectId, issueId);
            case "reopened" -> onReopened(projectId, issueId);
            default -> log.debug("Ignoring action: {}", action);
        }
    }

    /**
     * Issue 创建事件：设置为 Open 状态
     */
    private void onCreated(Long projectId, Long issueId) {
        updateFeedbackStatus(projectId, issueId, "Open");
    }

    /**
     * Issue 关闭事件：设置为 Complete 状态
     */
    private void onClosed(Long projectId, Long issueId) {
        updateFeedbackStatus(projectId, issueId, "Complete");
    }

    /**
     * Issue 重新打开事件：设置为 Open 状态
     */
    private void onReopened(Long projectId, Long issueId) {
        updateFeedbackStatus(projectId, issueId, "Open");
    }

    /**
     * 更新 feedback 状态
     */
    private void updateFeedbackStatus(Long projectId, Long issueId, String status) {
        // 根据 projectId 和 issuesId 查找 feedback
        Feedback feedback = feedbackService.getOne(
            new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getProjectId, projectId)
                .eq(Feedback::getIssuesId, issueId));

        if (feedback == null) {
            log.warn("Feedback not found for projectId: {}, issueId: {}", projectId, issueId);
            return;
        }

        // 更新状态
        feedbackService.update(new LambdaUpdateWrapper<Feedback>()
                                   .set(Feedback::getStatus, status)
                                   .eq(Feedback::getId, feedback.getId()));

        log.info("Updated feedback {} status to {} for issue {}", feedback.getId(), status, issueId);
    }

    /**
     * 生成事件 ID 用于幂等处理
     */
    private String generateEventId(Long projectId, Long issueId, String action) {
        return projectId + "_" + issueId + "_" + action;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
