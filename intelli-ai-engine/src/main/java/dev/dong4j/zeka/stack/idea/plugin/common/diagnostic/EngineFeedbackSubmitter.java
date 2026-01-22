package dev.dong4j.zeka.stack.idea.plugin.common.diagnostic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.util.RequestSigner;
import dev.dong4j.zeka.stack.idea.plugin.common.util.Urls;
import dev.dong4j.zeka.stack.idea.plugin.kit.SiteContents;

/**
 * 引擎反馈提交器
 * <p> 负责将用户在使用 IDE 引擎插件时遇到的错误报告和反馈信息提交到服务器.
 * 该类继承自 AbstractErrorReportSubmitter, 提供完整的反馈提交流程, 包括错误报告的创建,
 * URL 生成,HTTP 请求发送以及响应处理等功能.</p>
 * <p> 支持通过标题和正文创建新的 Issue, 自动收集插件信息,IDEA 版本和操作系统环境,
 * 并使用签名机制确保请求的安全性. 包含完整的错误处理和日志记录机制.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
public class EngineFeedbackSubmitter extends AbstractErrorReportSubmitter {
    /** 应用日志记录器, 用于记录 EngineFeedbackSubmitter 相关的日志信息 */
    private static final Logger LOG = Logger.getInstance(EngineFeedbackSubmitter.class);
    /** ObjectMapper 实例, 用于 JSON 数据的序列化和反序列化 */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** 提交反馈的 API 路径 */
    private static final String ISSUE_PATH = "/api/plugin/feedback/issue";
    /** 签名密钥, 用于生成请求签名以保证数据传输安全 */
    private static final String SIGN_SECRET = "zeka-stack-engine-plugin";
    /** 请求超时时间, 单位为秒 */
    private static final int REQUEST_TIMEOUT_SECONDS = 10;

    /**
     * 获取示例问题 ID
     * <p> 返回用于测试或示例的预设问题 ID, 通常用于演示错误报告功能
     *
     * @return 示例问题 ID 字符串
     */
    @Override
    protected String getExampleIssueId() {
        return "1";
    }

    /**
     * 获取插件在 GitHub 的 Issues 列表页面 URL.
     *
     * @return GitHub Issues 列表页面的完整 URL
     */
    @Override
    protected String getIssueListPageUrl() {
        return Urls.GITHUB_LINK + "/issues";
    }

    /**
     * 根据 Issue ID 生成用于显示的文本
     * <p> 如果 ID 以 "http" 开头, 则返回 "Issue"; 否则返回格式为 "Issue #ID" 的字符串
     *
     * @param issueId Issue 的 ID 或 URL 地址
     * @return 生成的文本字符串, 不会为 null
     */
    @Override
    protected @NotNull String generateTextByIssueId(String issueId) {
        if (issueId.startsWith("http")) {
            return "Issue";
        }
        return "Issue #" + issueId;
    }

    /**
     * 根据问题 ID 生成对应的 URL 地址
     * <p> 如果传入的 issueId 以 http 开头, 则直接返回该 URL; 否则拼接问题列表页面 URL 与 issueId 形成完整 URL.
     *
     * @param issueId 问题 ID, 可以是纯数字或以 http 开头的完整 URL
     * @return 生成的问题 URL 地址
     */
    @Override
    protected @NotNull String generateUrlByIssueId(String issueId) {
        if (issueId.startsWith("http")) {
            return issueId;
        }
        return getIssueListPageUrl() + "/" + issueId;
    }

    /**
     * 创建一个新的问题并提交反馈
     * <p> 根据给定的标题和内容创建一个新的问题, 并将相关信息发送到指定的 API 进行提交.
     * 如果提交成功, 则提取讨论 ID 并返回; 如果失败, 则记录警告日志并抛出异常.
     *
     * @param title 问题的标题
     * @param body  问题的内容
     * @return 提交问题后返回的讨论 ID
     */
    @Override
    protected @NotNull String newIssueByTitleBody(String title, String body) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", title);
        requestBody.put("content", body);
        requestBody.put("type", "BUG");
        requestBody.put("category", "GENERAL");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("pluginName", EngineContents.PLUGIN_NAME);
        String pluginVersion = getPluginVersion();
        if (pluginVersion != null) {
            userInfo.put("pluginVersion", pluginVersion);
        }
        userInfo.put("ideaVersion", getIdeaVersion());
        userInfo.put("os", getOperatingSystem());
        requestBody.put("userInfo", userInfo);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clientId", EngineContents.PLUGIN_ID);
        metadata.put("timestamp", System.currentTimeMillis());
        requestBody.put("metadata", metadata);

        try {
            String response = sendHttpRequest(requestBody);
            return extractDiscussionId(response);
        } catch (Exception e) {
            LOG.warn("Failed to submit feedback", e);
            throw new IllegalStateException("提交反馈失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据给定的 MD5 哈希值查找问题
     * <p> 此方法目前没有实现具体的查找逻辑, 直接返回 null.
     *
     * @param throwableMd5 抛出异常的 MD5 哈希值
     * @return 查找到的问题 ID, 当前实现始终返回 null
     */
    @Override
    protected String findIssueByMd5(String throwableMd5) {
        return null;
    }

    /**
     * 从响应中提取讨论的标识符 (Discussion ID 或 URL)
     * <p> 解析 JSON 格式响应, 从中获取 issue 或 discussion 的 URL 或编号. 如果请求或业务处理失败, 则抛出异常.
     *
     * @param response JSON 格式的服务器响应字符串
     * @return 提取到的讨论标识符 (URL 或编号), 若无法识别则返回 "unknown"
     * @throws IOException 当 JSON 解析或其他 I/O 操作失败时抛出
     */
    private String extractDiscussionId(@NotNull String response) throws IOException {
        JsonNode root = MAPPER.readTree(response);
        JsonNode dataNode = root.has("data") ? root.get("data") : root;

        boolean requestSuccess = !root.has("success") || root.path("success").asBoolean(false);
        boolean businessSuccess = dataNode.path("success").asBoolean(requestSuccess);

        if (!requestSuccess || !businessSuccess) {
            String message = dataNode.hasNonNull("message") ? dataNode.get("message").asText() : "";
            String error = dataNode.hasNonNull("error") ? dataNode.get("error").asText() : "";
            String reason = !StringUtil.isEmptyOrSpaces(error) ? error : message;
            if (StringUtil.isEmptyOrSpaces(reason)) {
                reason = "unknown error";
            }
            throw new IllegalStateException(reason);
        }

        JsonNode issue = dataNode.get("issue");
        if (issue != null && issue.hasNonNull("url")) {
            return issue.get("url").asText();
        }
        if (issue != null && issue.hasNonNull("number")) {
            return issue.get("number").asText();
        }

        JsonNode discussion = dataNode.get("discussion");
        if (discussion != null && discussion.hasNonNull("url")) {
            return discussion.get("url").asText();
        }
        if (discussion != null && discussion.hasNonNull("number")) {
            return discussion.get("number").asText();
        }
        return "unknown";
    }

    /**
     * 发送 HTTP 请求到指定的 API 端点, 用于提交反馈信息
     * <p> 将请求体转换为 JSON 格式, 并使用签名头信息构造 HTTP 请求, 发送至指定的 API 地址.
     *
     * @param requestBody 请求体, 包含提交反馈所需的数据
     * @return 服务器返回的响应内容
     * @throws IOException          如果请求过程中发生 I/O 错误
     * @throws InterruptedException 如果请求过程中被中断
     */
    private String sendHttpRequest(@NotNull Map<String, Object> requestBody)
        throws IOException, InterruptedException {
        String jsonBody = MAPPER.writeValueAsString(requestBody);
        byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

        URI uri = URI.create(SiteContents.ISSUE_API_URL);
        String pathWithQuery = ISSUE_PATH;
        if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
            pathWithQuery += "?" + uri.getQuery();
        }

        RequestSigner.SignedHeaders signedHeaders;
        try {
            signedHeaders = RequestSigner.sign(EngineContents.PLUGIN_ID, SIGN_SECRET,
                                               "POST", pathWithQuery, bodyBytes);
        } catch (Exception e) {
            throw new IOException("生成请求签名失败: " + e.getMessage(), e);
        }

        try (HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
            .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("X-Client-Id", signedHeaders.clientId())
                .header("X-Timestamp", signedHeaders.timestamp())
                .header("X-Nonce", signedHeaders.nonce())
                .header("X-Body-SHA256", signedHeaders.bodySha256())
                .header("X-Signature", signedHeaders.signature())
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }
    }

    /**
     * 获取插件版本信息
     * <p> 尝试获取当前插件的版本号, 如果获取失败则返回 null.
     *
     * @return 插件版本号, 如果获取失败则返回 null
     */
    @Nullable
    private String getPluginVersion() {
        try {
            PluginId pluginId = PluginId.getId(EngineContents.PLUGIN_ID);
            IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);
            if (pluginDescriptor != null) {
                return pluginDescriptor.getVersion();
            }
        } catch (Exception e) {
            LOG.debug("获取插件版本失败", e);
        }
        return null;
    }

    /**
     * 获取当前 IDEA 的完整版本号
     * <p> 通过 ApplicationInfo 实例获取当前运行的 IntelliJ IDEA 的完整版本字符串, 若获取失败则返回“未知”</p>
     *
     * @return IDEA 完整版本号, 若获取失败则返回“未知”
     */
    @NotNull
    private String getIdeaVersion() {
        try {
            return ApplicationInfo.getInstance().getFullVersion();
        } catch (Exception e) {
            LOG.debug("获取 IDEA 版本失败", e);
            return "未知";
        }
    }

    /**
     * 获取当前操作系统的名称和版本信息
     * <p> 根据系统信息判断操作系统类型, 并返回相应的名称和版本信息. 如果无法确定具体类型, 则返回操作系统的基本名称和版本.
     *
     * @return 操作系统名称和版本信息, 例如 "Windows 10" 或 "macOS 12.3"
     */
    @NotNull
    private String getOperatingSystem() {
        if (SystemInfo.isWindows) {
            return "Windows " + SystemInfo.getOsNameAndVersion();
        }
        if (SystemInfo.isMac) {
            return "macOS " + SystemInfo.getOsNameAndVersion();
        }
        if (SystemInfo.isLinux) {
            return "Linux " + SystemInfo.getOsNameAndVersion();
        }
        return SystemInfo.getOsNameAndVersion();
    }
}
