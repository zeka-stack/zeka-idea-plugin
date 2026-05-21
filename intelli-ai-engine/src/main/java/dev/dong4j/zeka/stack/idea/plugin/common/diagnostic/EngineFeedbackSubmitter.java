package dev.dong4j.zeka.stack.idea.plugin.common.diagnostic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.util.RequestSigner;
import dev.dong4j.zeka.stack.idea.plugin.kit.PluginUtil;
import dev.dong4j.zeka.stack.idea.plugin.kit.SiteContents;
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
     * 提交错误报告（由 IntelliJ 平台调用）
     * <p> 此方法带有 @ApiStatus.OverrideOnly 注解, 只能被 IntelliJ 平台内部调用,
     * 不应该由客户端代码直接调用. 如需手动触发反馈提交, 请使用 {@link #submitInternal(IdeaLoggingEvent[], String, java.awt.Component, Consumer)}.</p>
     *
     * @param events          日志事件数组, 包含待提交的错误信息
     * @param additionalInfo  附加信息, 可为空, 用于补充报告内容
     * @param parentComponent 父组件, 用于提供 UI 上下文, 可为空
     * @param consumer        提交结果的回调处理器, 用于接收提交后的报告信息
     * @return 提交是否成功, 由父类方法决定
     */
    @Override
    public boolean submit(@NotNull IdeaLoggingEvent @NotNull [] events,
                          @Nullable String additionalInfo,
                          @NotNull java.awt.Component parentComponent,
                          @NotNull Consumer<? super com.intellij.openapi.diagnostic.SubmittedReportInfo> consumer) {
        return submitInternal(events, additionalInfo, parentComponent, consumer);
    }

    /**
     * 内部提交入口, 供手动触发提交流程复用
     * <p> 此方法用于测试或手动触发反馈提交流程. 与 {@link #submit(IdeaLoggingEvent[], String, java.awt.Component, Consumer)} 不同,
     * 此方法可以被客户端代码安全调用.</p>
     * <p> 在提交前会尝试获取当前插件 ID 并解析事件中的插件 ID, 然后将插件 ID 推入上下文, 最后调用内部提交逻辑完成实际提交.</p>
     * <p> 此方法确保在提交过程中使用正确的插件上下文, 避免因插件 ID 缺失导致报告归属错误.</p>
     *
     * @param events          日志事件数组, 包含待提交的错误信息
     * @param additionalInfo  附加信息, 可为空, 用于补充报告内容
     * @param parentComponent 父组件, 用于提供 UI 上下文, 可为空
     * @param consumer        提交结果的回调处理器, 用于接收提交后的报告信息
     * @return 提交是否成功, 由父类方法决定
     */
    @Override
    public boolean submitInternal(@NotNull IdeaLoggingEvent @NotNull [] events,
                                     @Nullable String additionalInfo,
                                     @NotNull java.awt.Component parentComponent,
                                     @NotNull Consumer<? super com.intellij.openapi.diagnostic.SubmittedReportInfo> consumer) {
        String currentPluginId = FeedbackContextHolder.getCurrentPluginId();
        String eventPluginId = StringUtil.isEmptyOrSpaces(currentPluginId) ? resolvePluginId(events) : null;
        try (FeedbackContextHolder.Token ignored = FeedbackContextHolder.pushIfAbsent(eventPluginId)) {
            return super.submitInternal(events, additionalInfo, parentComponent, consumer);
        }
    }

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
        return SiteContents.GITHUB_LINK + "/issues";
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
        FeedbackSource source = resolveFeedbackSource();
        String resolvedTitle = title;
        if (!StringUtil.isEmptyOrSpaces(source.titlePrefix())) {
            resolvedTitle = "[Report][" + source.titlePrefix() + "] " + title;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", resolvedTitle);
        requestBody.put("content", body);
        requestBody.put("type", "BUG");
        requestBody.put("category", "GENERAL");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("pluginName", source.pluginName());
        if (source.pluginVersion() != null) {
            userInfo.put("pluginVersion", source.pluginVersion());
        }
        userInfo.put("ideaVersion", getIdeaVersion());
        userInfo.put("os", getOperatingSystem());
        requestBody.put("userInfo", userInfo);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clientId", EngineContents.PLUGIN_ID);
        metadata.put("timestamp", System.currentTimeMillis());
        metadata.put("sourcePluginId", source.pluginId());
        metadata.put("sourcePluginName", source.pluginName());
        if (source.pluginVersion() != null) {
            metadata.put("sourcePluginVersion", source.pluginVersion());
        }
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
     * 解析事件数组中的插件 ID
     * <p> 遍历事件数组, 获取第一个事件中的异常对象, 并调用 findPluginIdInThrowable 方法查找插件 ID.
     * 如果事件数组为空, 则返回 null.
     *
     * @param events 事件数组
     * @return 插件 ID, 如果未找到则返回 null
     */
    private @Nullable String resolvePluginId(@NotNull IdeaLoggingEvent @NotNull [] events) {
        if (events.length == 0) {
            return null;
        }
        Throwable throwable = events[0].getThrowable();
        return findPluginIdInThrowable(throwable);
    }

    /**
     * 在异常堆栈中查找插件 ID
     * <p> 遍历异常及其原因链, 查找是否存在 {@link PluginException} 类型的异常.
     * 如果找到该类型的异常, 则返回其关联的插件 ID 字符串; 否则返回 null.
     *
     * @param throwable 需要分析的异常对象, 可为 null
     * @return 找到的插件 ID 字符串, 如果未找到或输入为 null 则返回 null
     */
    private @Nullable String findPluginIdInThrowable(@Nullable Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof PluginException pluginException) {
                PluginId pluginId = pluginException.getPluginId();
                if (pluginId != null) {
                    return pluginId.getIdString();
                }
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * 解析并构建反馈来源信息
     * <p> 通过以下逻辑确定反馈来源的完整信息:
     * <ol>
     *   <li> 首先尝试从 FeedbackContextHolder 获取当前插件 ID</li>
     *   <li> 根据插件 ID 获取对应的插件描述符 </li>
     *   <li> 如果插件描述符不存在, 则获取默认的插件描述符 </li>
     *   <li> 解析插件 ID, 如果为空则使用默认插件 ID</li>
     *   <li> 确定插件名称, 优先使用插件描述符中的名称, 若无则使用插件 ID</li>
     *   <li> 获取插件版本信息 (可能为 null)</li>
     *   <li> 查找对应的反馈上下文提供者以获取标题前缀 </li>
     *   <li> 如果标题前缀为空, 则使用插件名称作为标题前缀 </li>
     * </ol>
     *
     * @return 包含完整反馈来源信息的 FeedbackSource 对象
     */
    private FeedbackSource resolveFeedbackSource() {
        String pluginId = FeedbackContextHolder.getCurrentPluginId();
        PluginDescriptor pluginDescriptor = null;
        if (!StringUtil.isEmptyOrSpaces(pluginId)) {
            pluginDescriptor = PluginUtil.getPluginDescriptor(pluginId);
        }
        if (pluginDescriptor == null) {
            pluginDescriptor = getPluginDescriptor();
        }

        String resolvedId = pluginId;
        if (pluginDescriptor != null) {
            pluginDescriptor.getPluginId();
            resolvedId = pluginDescriptor.getPluginId().getIdString();
        }
        if (StringUtil.isEmptyOrSpaces(resolvedId)) {
            resolvedId = EngineContents.PLUGIN_ID;
        }

        String name = pluginDescriptor != null ? pluginDescriptor.getName() : resolvedId;
        if (StringUtil.isEmptyOrSpaces(name)) {
            name = resolvedId;
        }
        String version = pluginDescriptor != null ? pluginDescriptor.getVersion() : null;

        FeedbackContextProvider provider = findProvider(resolvedId);
        String titlePrefix = provider != null ? provider.getTitlePrefix() : null;
        if (StringUtil.isEmptyOrSpaces(titlePrefix)) {
            titlePrefix = name;
        }

        return new FeedbackSource(resolvedId, name, version, titlePrefix);
    }

    /**
     * 根据插件 ID 查找对应的反馈上下文提供者
     * <p> 遍历所有注册的反馈上下文提供者, 查找与给定插件 ID 匹配的提供者实例.</p>
     *
     * @param pluginId 插件 ID, 不能为空
     * @return 匹配的反馈上下文提供者实例, 若未找到则返回 null
     */
    private @Nullable FeedbackContextProvider findProvider(@NotNull String pluginId) {
        for (FeedbackContextProvider provider : FeedbackContextProvider.EP_NAME.getExtensionList()) {
            String id = provider.getPluginId();
            if (!StringUtil.isEmptyOrSpaces(id) && id.equals(pluginId)) {
                return provider;
            }
        }
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
        String trimmedResponse = response.trim();
        if (!(trimmedResponse.startsWith("{") || trimmedResponse.startsWith("["))) {
            throw new IllegalStateException(trimmedResponse);
        }
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
            int statusCode = response.statusCode();
            String responseBody = response.body();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("HTTP " + statusCode + ": " + responseBody);
            }
            return responseBody;
        }
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

    /**
     * 反馈源信息数据类
     * <p> 用于封装与反馈提交相关的插件信息, 包括插件 ID, 名称, 版本以及标题前缀.</p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.28
     * @since 1.0.0
     */
    private record FeedbackSource(
        @NotNull String pluginId,
        @NotNull String pluginName,
        @Nullable String pluginVersion,
        @NotNull String titlePrefix
    ) {
    }
}
