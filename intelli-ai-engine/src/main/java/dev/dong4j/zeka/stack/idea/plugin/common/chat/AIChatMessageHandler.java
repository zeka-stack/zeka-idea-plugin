package dev.dong4j.zeka.stack.idea.plugin.common.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * AI 聊天消息处理器
 * <p> 负责处理与 AI 聊天前端交互的消息事件, 包括初始化, 配置设置, 消息发送, 流式响应管理以及文件和浏览器操作等功能.
 * 该类作为聊天功能的核心控制器, 协调 AI 服务, 前端界面和项目上下文之间的通信.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.29
 * @since 1.0.0
 */
final class AIChatMessageHandler {
    /** 日志记录器, 用于记录 AI 聊天消息处理器的运行时信息 */
    private static final Logger LOG = Logger.getInstance(AIChatMessageHandler.class);

    /** 当前项目实例 */
    private final Project project;
    /** 浏览器组件, 用于显示和交互 */
    private final JBCefBrowser browser;
    /** AI 服务实例, 用于处理与 AI 相关的操作和请求 */
    private final AIService aiService;
    /** 用于管理 AI 服务商的设置信息 */
    private final AIProviderSettings providerSettings;
    /** AI 供应商配置映射表, 键为供应商 ID, 值为对应的 AIProviderConfig 对象 */
    private final Map<String, AIProviderConfig> providersById = new HashMap<>();

    /** 当前使用的 AI 服务商 ID */
    private String currentProviderId;
    /** 当前使用的 AI 模型名称 */
    private String currentModel;
    /** 是否启用流式响应模式 */
    private boolean streamingEnabled = true;
    /** 发送消息的快捷键, 默认为 "enter" */
    private String sendShortcut = "enter";
    /** 当前 AI 响应流的取消令牌 */
    private StreamCancellationToken currentToken;
    /** 用于同步流处理状态的锁对象 */
    private final Object streamLock = new Object();
    /**
     * 最近一次接收到数据块的时间戳
     * <p> 用于跟踪和管理流式处理中的数据块接收时间
     */
    private volatile long lastChunkAt = 0L;
    /**
     * 标记流是否已完成
     * <p> 用于控制是否继续处理流式数据, 防止重复或无效操作
     */
    private volatile boolean streamFinished = false;
    /** 是否已开始流式响应, 用于控制前端流式开始事件的触发 */
    private volatile boolean streamStarted = false;

    /**
     * 构造函数, 用于初始化 AI 聊天消息处理器
     * <p> 该构造函数接收项目和浏览器实例, 并初始化相关服务和配置.
     *
     * @param project 项目实例
     * @param browser 浏览器实例
     */
    AIChatMessageHandler(@NotNull Project project, @NotNull JBCefBrowser browser) {
        this.project = project;
        this.browser = browser;
        this.aiService = AIServiceImpl.getInstance();
        this.providerSettings = AIProviderSettings.getInstance();
    }

    /**
     * 处理前端就绪事件
     * <p> 当前端界面准备好后, 发送一系列初始化信息, 包括提供商列表,IDE 主题, 依赖状态, 选中内容信息, 流式传输启用状态以及发送快捷键设置
     *
     */
    void onFrontendReadyInjected() {
        sendProviders();
        sendIdeTheme();
        sendDependencyStatus();
        sendSelectionInfo();
        sendStreamingEnabled();
        sendSendShortcut();
    }

    /**
     * 处理接收到的消息
     * <p> 根据消息中的事件类型执行相应的操作. 消息格式为 "事件: 负载", 其中事件和负载之间由冒号分隔.
     * 如果消息为空白, 则直接返回. 对于非心跳和统计事件, 记录日志并进行处理.
     *
     * @param raw 接收到的原始消息字符串
     */
    void handleMessage(@NotNull String raw) {
        if (raw.isBlank()) {
            return;
        }
        int idx = raw.indexOf(':');
        String event = idx >= 0 ? raw.substring(0, idx) : raw;
        String payload = idx >= 0 ? raw.substring(idx + 1) : "";

        if (!"heartbeat".equals(event) && !"get_usage_statistics".equals(event)) {
            AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] event=" + event);
            LOG.info("[Chat] event=" + event);
        }

        switch (event) {
            case "frontend_ready" -> onFrontendReadyInjected();
            case "get_providers" -> sendProviders();
            case "get_active_provider" -> sendProviders();
            case "set_provider" -> setProvider(payload);
            case "set_model" -> setModel(payload);
            case "send_message", "send_message_with_attachments" -> handleSendMessage(payload);
            case "interrupt_session" -> interruptCurrent();
            case "get_ide_theme" -> sendIdeTheme();
            case "get_dependency_status" -> sendDependencyStatus();
            case "get_streaming_enabled" -> sendStreamingEnabled();
            case "set_streaming_enabled" -> setStreamingEnabled(payload);
            case "get_send_shortcut" -> sendSendShortcut();
            case "set_send_shortcut" -> setSendShortcut(payload);
            case "refresh_slash_commands" -> sendSlashCommands();
            case "get_agents" -> sendAgents();
            case "get_selected_agent" -> sendSelectedAgent();
            case "list_files" -> sendFileList(payload);
            case "open_file" -> openFile(payload);
            case "open_browser" -> openBrowser(payload);
            default -> {
                // ignore unsupported events for now
            }
        }
    }

    /**
     * 设置当前激活的 AI 服务提供商
     * <p> 根据传入的提供商 ID 更新当前使用的提供商, 并记录日志
     *
     * @param providerId 服务提供商 ID
     */
    private void setProvider(@NotNull String providerId) {
        String id = providerId.trim();
        if (!id.isEmpty()) {
            currentProviderId = id;
            AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] set_provider=" + id);
            LOG.info("[Chat] set_provider=" + id);
        }
    }

    /**
     * 设置当前使用的 AI 模型
     * <p> 该方法用于设置 AI 聊天功能中使用的模型名称, 设置成功后会将模型名称更新到当前配置中
     *
     * @param model 模型名称, 不能为空, 会自动去除前后空白字符
     */
    private void setModel(@NotNull String model) {
        String value = model.trim();
        if (!value.isEmpty()) {
            currentModel = value;
            AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] set_model=" + value);
            LOG.info("[Chat] set_model=" + value);
        }
    }

    /**
     * 设置流式传输启用状态
     * <p> 解析传入的 JSON 字符串, 从中提取 "streamingEnabled" 字段的布尔值, 并更新当前的流式传输启用状态
     *
     * @param payload 包含 "streamingEnabled" 字段的 JSON 字符串
     */
    private void setStreamingEnabled(@NotNull String payload) {
        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            streamingEnabled = obj.has("streamingEnabled") && obj.get("streamingEnabled").getAsBoolean();
        } catch (Exception ignore) {
        }
    }

    /**
     * 设置发送消息的快捷键
     * <p> 从传入的 JSON 负载中提取 sendShortcut 字段, 并更新当前的快捷键配置
     * 如果 JSON 中不存在该字段或解析失败, 则不进行任何操作
     *
     * @param payload 包含快捷键配置的 JSON 字符串
     * @since 1.0
     */
    private void setSendShortcut(@NotNull String payload) {
        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            if (obj.has("sendShortcut")) {
                sendShortcut = obj.get("sendShortcut").getAsString();
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * 处理用户发送的消息
     * <p> 解析传入的消息内容, 构建 AI 请求并调用 AI 服务进行内容生成. 如果消息为空或无效, 则记录警告信息并返回.</p>
     *
     * @param payload 包含消息内容和附件信息的 JSON 字符串
     */
    private void handleSendMessage(@NotNull String payload) {
        JsonObject obj = parsePayload(payload);
        String text = obj != null && obj.has("text") ? obj.get("text").getAsString() : null;
        if ((text == null || text.isBlank()) && obj == null) {
            text = payload;
        }
        String attachmentSummary = buildAttachmentSummary(obj);
        if (text == null || text.isBlank()) {
            text = attachmentSummary;
        } else if (!attachmentSummary.isBlank()) {
            text = text + "\n\n" + attachmentSummary;
        }
        if (text.isBlank()) {
            AIConsoleLoggerUtil.printWarning(project, "[Chat] empty message, ignored");
            LOG.warn("[Chat] empty message, ignored");
            return;
        }
        AIProviderConfig config = getActiveProviderConfig();
        if (config == null) {
            callJs("addErrorMessage", "未找到可用的 AI 服务商配置，请先在设置中验证服务商。");
            AIConsoleLoggerUtil.printError(project, "[Chat] no verified provider config");
            LOG.warn("[Chat] no verified provider config");
            return;
        }
        if (currentModel != null && !currentModel.isBlank()) {
            config.modelName = currentModel;
        }

        String protocol = config.providerType.isAnthropicCompatible() ? "anthropic" : "openai-compatible";
        String providerLog = "[Chat] send message, provider=" + config.providerType.getDisplayName()
                             + ", providerId=" + config.providerType.getProviderId()
                             + ", protocol=" + protocol
                             + ", model=" + config.modelName
                             + ", baseUrl=" + config.baseUrl;
        AIConsoleLoggerUtil.printWithTimestamp(project, providerLog);
        LOG.info(providerLog);

        String systemPrompt = buildSystemPrompt();
        String fileTagContext = buildFileTagContext(obj);
        if (!fileTagContext.isBlank()) {
            systemPrompt = systemPrompt + "\n" + fileTagContext;
        }
        AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] context:\n" + systemPrompt);
        AIChatRequest request = new AIChatRequest(systemPrompt, text);

        StreamCancellationToken token = new StreamCancellationToken();
        currentToken = token;
        final boolean[] hasChunk = {false};
        final int[] chunkCount = {0};
        final int[] charCount = {0};
        lastChunkAt = System.currentTimeMillis();
        streamFinished = false;
        streamStarted = false;
        scheduleStreamTimeout();

        try {
            aiService.generateContentStream(project, request, config, new AIStreamResponseListener() {
                /**
                 * 流式响应开始时的回调处理
                 * <p> 确保流已启动, 并打印流开始的日志信息到控制台和日志系统
                 */
                @Override
                public void onStart() {
                    ensureStreamStarted();
                    AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] stream start");
                    LOG.info("[Chat] stream start");
                }

                /**
                 * 处理接收到的流数据块
                 * <p> 当接收到新的数据块时, 若流尚未完成, 则确保流已启动, 并调用 JavaScript 回调函数通知前端. 同时更新相关统计信息.
                 *
                 * @param chunk 接收到的数据块内容
                 */
                @Override
                public void onChunk(@NotNull String chunk) {
                    if (streamFinished) {
                        return;
                    }
                    ensureStreamStarted();
                    callJs("onContentDelta", chunk);
                    hasChunk[0] = true;
                    chunkCount[0] += 1;
                    charCount[0] += chunk.length();
                    lastChunkAt = System.currentTimeMillis();
                }

                /**
                 * 处理思考阶段的文本增量回调.
                 * <p> 当接收到新的思考分块时, 如果会话已结束则不执行任何操作; 否则确保流已启动, 随后通过 {@code callJs} 推送
                 * {@code onThinkingDelta} 事件, 并更新当前时间为最近一次分块时间戳.
                 *
                 * @param chunk 当前思考文本分块, 不能为空
                 */
                @Override
                public void onThinkingChunk(@NotNull String chunk) {
                    if (streamFinished) {
                        return;
                    }
                    ensureStreamStarted();
                    callJs("onThinkingDelta", chunk);
                    lastChunkAt = System.currentTimeMillis();
                }

                /**
                 * 当流式传输完成时触发, 处理最终的完整文本内容
                 * <p> 如果流尚未完成, 则调用 {@code ensureStreamStarted()} 确保流已启动. 如果尚未接收到任何数据块且完整文本非空, 则通过 JavaScript 调用 {@code onContentDelta} 并更新字符计数. 最后调用 {@code finishStream} 完成流式传输并记录相关信息.</p>
                 *
                 * @param fullText 流式传输完成时接收到的完整文本内容
                 */
                @Override
                public void onComplete(@NotNull String fullText) {
                    if (streamFinished) {
                        return;
                    }
                    ensureStreamStarted();
                    if (!hasChunk[0] && !fullText.isBlank()) {
                        callJs("onContentDelta", fullText);
                        charCount[0] = fullText.length();
                    }
                    finishStream("[Chat] stream complete, chunks=" + chunkCount[0] + ", len=" + charCount[0]);
                }

                /**
                 * 处理 Token 使用情况的回调
                 * <p> 接收并计算 Token 使用量及百分比, 构建包含使用详情的 JSON 数据, 并调用 JavaScript 方法进行更新
                 *
                 * @param providerName     服务提供商名称
                 * @param modelName        模型名称
                 * @param promptTokens     提示词 Token 数量
                 * @param completionTokens 生成的 Token 数量
                 * @param totalTokens      总 Token 数量
                 */
                @Override
                public void onUsage(String providerName,
                                    String modelName,
                                    int promptTokens,
                                    int completionTokens,
                                    int totalTokens) {
                    int maxTokens = resolveMaxTokens(config);
                    int effectiveTotal = Math.max(0, totalTokens);
                    int percentage = maxTokens > 0 ? Math.min(100, (int) Math.round(effectiveTotal * 100.0 / maxTokens)) : 0;
                    JsonObject payload = new JsonObject();
                    payload.addProperty("percentage", percentage);
                    payload.addProperty("usedTokens", effectiveTotal);
                    if (maxTokens > 0) {
                        payload.addProperty("maxTokens", maxTokens);
                    }
                    callJs("onUsageUpdate", payload.toString());
                }

                /**
                 * 处理流处理过程中发生的错误
                 * <p> 当流处理过程中发生错误时, 调用此方法处理错误信息, 并记录错误日志
                 *
                 * @param error     错误信息
                 * @param exception 相关的异常对象, 可以为空
                 */
                @Override
                public void onError(@NotNull String error, @Nullable Throwable exception) {
                    if (streamFinished) {
                        return;
                    }
                    callJs("addErrorMessage", error);
                    finishStream("[Chat] stream error: " + error);
                    LOG.warn("[Chat] stream error: " + error, exception);
                }

                /**
                 * 返回当前流的取消令牌
                 * <p> 该方法用于获取当前流操作的取消令牌, 可用于取消正在进行的流操作.
                 *
                 * @return 不可为空的 {@code StreamCancellationToken} 实例, 表示当前流的取消令牌
                 */
                @Override
                public @NotNull StreamCancellationToken cancellationToken() {
                    return token;
                }
            });
        } catch (AIServiceException e) {
            callJs("addErrorMessage", "AI 服务调用失败: " + e.getMessage());
            finishStream("[Chat] invoke error: " + e.getMessage());
            LOG.warn("[Chat] invoke error: " + e.getMessage(), e);
        }
    }

    /**
     * 中断当前流处理
     * <p> 取消当前流令牌并记录中断信息
     *
     * @since 1.0
     */
    private void interruptCurrent() {
        StreamCancellationToken token = currentToken;
        if (token != null) {
            token.cancel();
            AIConsoleLoggerUtil.printWarning(project, "[Chat] interrupt stream");
            LOG.info("[Chat] interrupt stream");
        }
        finishStream("[Chat] interrupt stream");
    }

    /**
     * 调度流式传输超时检测任务
     * <p> 在后台线程中运行, 如果自上次接收数据块以来空闲时间超过 5 秒, 则自动结束流式传输
     */
    private void scheduleStreamTimeout() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                return;
            }
            if (streamFinished) {
                return;
            }
            long idle = System.currentTimeMillis() - lastChunkAt;
            if (idle >= 5000) {
                finishStream("[Chat] stream timeout, idle=" + idle + "ms");
            }
        });
    }

    /**
     * 结束流处理
     * <p> 同步锁确保流处理完成状态的更新, 并执行结束流的相关操作
     *
     * @param logMessage 记录日志的消息
     */
    private void finishStream(@NotNull String logMessage) {
        synchronized (streamLock) {
            if (streamFinished) {
                return;
            }
            streamFinished = true;
        }
        callJs("onStreamEnd", null);
        AIConsoleLoggerUtil.completeStreamPlain(project);
        AIConsoleLoggerUtil.printWithTimestamp(project, logMessage);
        LOG.info(logMessage);
    }

    /**
     * 确保流式传输已启动
     * <p> 检查流式传输状态标志, 如果尚未启动, 则在同步块中将其设置为启动状态, 并通知前端.
     */
    private void ensureStreamStarted() {
        if (streamStarted) {
            return;
        }
        synchronized (streamLock) {
            if (streamStarted) {
                return;
            }
            streamStarted = true;
        }
        callJs("onStreamStart", null);
    }

    /**
     * 获取当前激活的 AI 服务商配置信息.
     * <p>该方法在调用前会刷新已验证的服务商列表, 并根据 {@code currentProviderId} 选择对应
     * 配置; 若 {@code currentProviderId} 为空或不存在, 则默认选取列表中的第一个配置. 调用后
     * 同时会同步更新 {@code currentProviderId} 与 {@code currentModel}, 使其与选定的配置保持一致.
     *
     * @return 当前激活的 {@link AIProviderConfig} 配置; 若没有可用的服务商 (列表为空) 则返回 {@code null}
     */
    @Nullable
    private AIProviderConfig getActiveProviderConfig() {
        refreshProviders();
        if (providersById.isEmpty()) {
            return null;
        }
        if (currentProviderId != null && providersById.containsKey(currentProviderId)) {
            return providersById.get(currentProviderId).copy();
        }
        AIProviderConfig first = providersById.values().iterator().next();
        currentProviderId = first.credentialId;
        if (currentModel == null || currentModel.isBlank()) {
            currentModel = first.modelName;
        }
        return first.copy();
    }

    /**
     * 刷新可用的 AI 服务提供者配置列表
     * <p> 清空当前缓存的提供者映射, 从设置中获取已验证的提供者配置, 并根据凭证 ID 或回退 ID 建立映射关系.
     * 若当前未设置默认提供者 ID 且存在可用提供者, 则自动设置第一个提供者为默认.
     *
     * @since 1.0
     */
    private void refreshProviders() {
        providersById.clear();
        List<AIProviderConfig> configs = providerSettings.getVerifiedProviders();
        for (AIProviderConfig config : configs) {
            String id = config.credentialId != null ? config.credentialId : buildFallbackId(config);
            providersById.put(id, config.copy());
        }
        if (currentProviderId == null && !providersById.isEmpty()) {
            currentProviderId = providersById.keySet().iterator().next();
        }
    }

    /**
     * 根据 AI 服务商配置生成一个用于标识该配置的唯一字符串
     * <p> 该字符串由服务商类型 ID, 模型名称和基础 URL 拼接而成, 用于在内部映射中作为配置的唯一键.
     * 例如:`providerId:modelName:baseUrl`, 其中各部分以冒号分隔.
     *
     * @param config 非空的 AI 服务商配置对象, 包含服务商类型, 模型名称和基础 URL 等信息
     * @return 由服务商类型 ID, 模型名称和基础 URL 拼接而成的字符串, 用于作为配置映射的键
     */
    private String buildFallbackId(@NotNull AIProviderConfig config) {
        return config.providerType.getProviderId() + ":" + config.modelName + ":" + config.baseUrl;
    }

    /**
     * 发送可用的 AI 服务商列表给前端
     * <p> 刷新服务商列表并构建 JSON 对象, 然后发送给前端
     *
     * @since 1.0
     */
    void sendProviders() {
        refreshProviders();
        AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] send providers size=" + providersById.size());
        LOG.info("[Chat] send providers size=" + providersById.size());
        JsonObject payload = new JsonObject();
        JsonArray providers = new JsonArray();

        for (Map.Entry<String, AIProviderConfig> entry : providersById.entrySet()) {
            AIProviderConfig config = entry.getValue();
            String id = entry.getKey();

            JsonObject item = new JsonObject();
            item.addProperty("id", id);
            item.addProperty("label", buildChatProviderLabel(config.providerType));
            item.addProperty("providerType", config.providerType.getProviderId());
            item.addProperty("model", config.modelName);
            item.addProperty("isActive", id.equals(currentProviderId));

            List<String> supportedModels = new ArrayList<>(config.providerType.getSupportedModels());
            if (!supportedModels.contains(config.modelName)) {
                supportedModels.addFirst(config.modelName);
            }
            JsonArray modelArray = new JsonArray();
            for (String model : supportedModels) {
                modelArray.add(model);
            }
            item.add("models", modelArray);
            providers.add(item);
        }

        payload.add("providers", providers);
        callJs("updateEngineProviders", payload.toString());
    }

    /**
     * 构建 Chat 面板中展示的服务商名称.
     * <p> 部分服务商同时提供 OpenAI 兼容和 Anthropic 兼容入口, 仅展示 displayName 会导致两项同名.
     * 对 Anthropic 兼容入口追加协议标识, 方便用户确认当前接口类型.
     *
     * @param providerType 服务商类型
     * @return Chat 下拉列表展示名称
     */
    @NotNull
    private static String buildChatProviderLabel(@NotNull AIProviderType providerType) {
        String displayName = providerType.getDisplayName();
        return providerType.isAnthropicCompatible() ? displayName + " (Anthropic)" : displayName;
    }

    /**
     * 发送当前 IDE 主题信息至前端.
     * <p> 该方法会检测 IDE 当前是否为深色主题, 并将结果以 JSON 字符串形式通过 {@code callJs} 发送给前端.
     * 调用方应在前端就绪后触发, 如 {@code onFrontendReadyInjected}.
     *
     * @since 2024.6
     */
    private void sendIdeTheme() {
        // 使用公共 API 检测主题: 通过面板背景色判断是否为深色主题
        boolean isDark = ColorUtil.isDark(UIUtil.getPanelBackground());
        JsonObject obj = new JsonObject();
        obj.addProperty("isDark", isDark);
        callJs("onIdeThemeReceived", obj.toString());
    }

    /**
     * 向前端发送依赖项状态信息
     * <p> 构造包含引擎安装状态的 JSON 对象, 并通过 JavaScript 方法更新前端依赖状态显示 </p>
     * <p> 当前引擎状态为已安装, 状态值为 "installed"</p>
     *
     * @see #callJs(String, String)
     */
    private void sendDependencyStatus() {
        JsonObject obj = new JsonObject();
        JsonObject engine = new JsonObject();
        engine.addProperty("installed", true);
        engine.addProperty("status", "installed");
        obj.add("engine", engine);
        callJs("updateDependencyStatus", obj.toString());
    }

    /**
     * 发送流式传输启用状态到前端
     * <p> 创建一个 JSON 对象, 包含当前流式传输是否启用的状态, 并将其传递给前端进行更新.
     *
     */
    private void sendStreamingEnabled() {
        JsonObject obj = new JsonObject();
        obj.addProperty("streamingEnabled", streamingEnabled);
        callJs("updateStreamingEnabled", obj.toString());
    }

    /**
     * 发送发送快捷键设置到前端
     * <p> 将当前设置的发送快捷键值通过 JavaScript 调用传递给前端界面进行更新
     *
     */
    private void sendSendShortcut() {
        JsonObject obj = new JsonObject();
        obj.addProperty("sendShortcut", sendShortcut);
        callJs("updateSendShortcut", obj.toString());
    }

    /**
     * 发送斜杠命令列表到前端
     * <p> 此方法构造一个空的 JSON 数组并发送给前端, 用于更新当前可用的斜杠命令.
     */
    private void sendSlashCommands() {
        callJs("updateSlashCommands", "[]");
    }

    /**
     * 发送代理信息
     * <p> 向前端发送一个空的代理列表
     */
    private void sendAgents() {
        callJs("updateAgents", "[]");
    }

    /**
     * 发送当前选中的代理信息
     * <p> 此方法将调用前端 JavaScript 函数 onSelectedAgentReceived 并传递 null 作为参数
     *
     * @since 1.0
     */
    private void sendSelectedAgent() {
        callJs("onSelectedAgentReceived", "null");
    }

    /**
     * 发送当前编辑器中的选区信息到前端
     * <p> 该方法通过读取当前编辑器的选区内容, 生成一个包含文件路径和行号范围的字符串, 并将其发送到前端. 如果未选中任何内容, 则清除前端的选区信息.
     *
     */
    void sendSelectionInfo() {
        String payload = com.intellij.openapi.application.ReadAction.compute(this::buildSelectionPayload);
        if (payload == null || payload.isBlank()) {
            callJs("clearSelectionInfo", null);
            return;
        }
        callJs("addSelectionInfo", payload);
    }

    /**
     * 构建选中内容的载荷信息
     * <p> 获取当前编辑器中选中的文件路径及行号信息, 生成用于引用的格式化字符串.
     * 如果未选中任何文本, 仅返回文件路径; 如果选中了文本, 返回文件路径及对应的行号范围.
     *
     * @return 格式化的引用字符串, 格式为 "文件路径" 或 "文件路径 #L 起始行 - 结束行",
     * 如果无法获取编辑器或文件则返回 null
     */
    @Nullable
    String buildSelectionPayload() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return null;
        }
        Document document = editor.getDocument();
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return null;
        }
        int start = editor.getSelectionModel().getSelectionStart();
        int end = editor.getSelectionModel().getSelectionEnd();
        if (start == end) {
            return "@" + file.getPath();
        }
        int startLine = document.getLineNumber(start) + 1;
        int endLine = document.getLineNumber(end) + 1;
        return "@" + file.getPath() + "#L" + startLine + "-" + endLine;
    }

    /**
     * 解析并计算最大令牌数量
     * <p> 根据给定的 AIProviderConfig 对象中的模型参数解析最大令牌数量. 如果模型参数为空或最大令牌数量为空, 则返回 - 1.
     * 如果最大令牌数量字符串为 "auto" 或空白字符串, 则返回 - 1.
     * 否则, 将最大令牌数量字符串转换为数字并进行相应的计算, 返回计算后的最大令牌数量.
     *
     * @param config AIProviderConfig 对象
     * @return 计算后的最大令牌数量, 如果无法解析则返回 - 1
     */
    private int resolveMaxTokens(@NotNull AIProviderConfig config) {
        if (config.modelParameters == null || config.modelParameters.maxTokens == null) {
            return -1;
        }
        String maxTokens = config.modelParameters.maxTokens.trim();
        if (maxTokens.isBlank() || "auto".equalsIgnoreCase(maxTokens)) {
            return -1;
        }
        try {
            double value = Double.parseDouble(maxTokens);
            int tokens = (int) Math.round(value * 1000);
            return Math.max(tokens, 1);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 根据传入的 payload 参数发送文件列表信息
     * <p> 解析 payload 中的查询条件和路径, 遍历项目目录并生成符合要求的文件列表 JSON 数据, 最后通过 callJs 方法返回给前端 </p>
     *
     * @param payload 包含查询参数和路径信息的 JSON 字符串
     */
    private void sendFileList(@NotNull String payload) {
        JsonObject response = new JsonObject();
        JsonArray files = new JsonArray();
        response.add("files", files);

        try {
            JsonObject req = JsonParser.parseString(payload).getAsJsonObject();
            String query = req.has("query") ? req.get("query").getAsString() : "";
            String currentPath = req.has("currentPath") ? req.get("currentPath").getAsString() : "";

            String basePath = project.getBasePath();
            if (basePath == null) {
                callJs("onFileListResult", response.toString());
                return;
            }

            Path base = Path.of(basePath);
            Path target = currentPath.isBlank() ? base : base.resolve(currentPath);
            if (!Files.exists(target) || !Files.isDirectory(target)) {
                target = base;
            }

            try (var stream = Files.list(target)) {
                stream.limit(200).forEach(path -> {
                    String name = path.getFileName().toString();
                    if (!query.isBlank() && !name.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                        return;
                    }
                    JsonObject file = new JsonObject();
                    String rel = base.relativize(path).toString();
                    file.addProperty("name", name);
                    file.addProperty("path", rel.replace('\\', '/'));
                    file.addProperty("absolutePath", path.toString());
                    file.addProperty("type", Files.isDirectory(path) ? "directory" : "file");
                    int dot = name.lastIndexOf('.');
                    if (dot > 0 && dot < name.length() - 1) {
                        file.addProperty("extension", name.substring(dot + 1));
                    }
                    files.add(file);
                });
            }
        } catch (Exception e) {
            LOG.debug("file list error: " + e.getMessage(), e);
        }

        callJs("onFileListResult", response.toString());
    }

    /**
     * 打开指定路径的文件
     * <p> 根据给定的文件路径查找文件并在编辑器中打开
     *
     * @param path 文件路径
     */
    private void openFile(@NotNull String path) {
        String filePath = path.trim();
        if (filePath.isEmpty()) {
            return;
        }
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file == null) {
            return;
        }
        FileEditorManager.getInstance(project).openFile(file, true);
    }

    /**
     * 打开浏览器并访问指定的 URL
     * <p> 根据传入的 URL 打开浏览器并跳转到该 URL 对应的页面
     *
     * @param url 要访问的 URL 地址
     */
    private void openBrowser(@NotNull String url) {
        String target = url.trim();
        if (!target.isEmpty()) {
            BrowserUtil.browse(target);
        }
    }

    /**
     * 解析给定的 JSON 字符串并返回对应的 JsonObject 对象
     * <p> 如果解析失败, 则记录调试信息并返回 null
     *
     * @param payload 要解析的 JSON 字符串
     * @return 解析后的 JsonObject 对象, 如果解析失败则返回 null
     */
    @Nullable
    private JsonObject parsePayload(@NotNull String payload) {
        try {
            return JsonParser.parseString(payload).getAsJsonObject();
        } catch (Exception e) {
            LOG.debug("parse payload failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 构建附件摘要字符串
     * <p> 解析包含附件信息的 JSON 对象, 生成格式化的文本摘要. 对于文本类型的附件, 尝试解码并包含其内容;
     * 对于图片, 二进制或过大的文件, 仅包含文件名和类型信息.
     *
     * @param obj 包含附件信息的 JSON 对象, 如果为 null 或不包含 attachments 数组则视为无附件
     * @return 格式化后的附件摘要字符串, 包含文件名, 类型和部分文本内容 (如果适用)
     */
    @NotNull
    private String buildAttachmentSummary(@Nullable JsonObject obj) {
        if (obj == null || !obj.has("attachments") || !obj.get("attachments").isJsonArray()) {
            return "";
        }
        JsonArray attachments = obj.getAsJsonArray("attachments");
        if (attachments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("附件:").append('\n');
        int totalChars = 0;
        for (int i = 0; i < attachments.size(); i++) {
            if (!attachments.get(i).isJsonObject()) {
                continue;
            }
            JsonObject item = attachments.get(i).getAsJsonObject();
            String fileName = item.has("fileName") ? item.get("fileName").getAsString() : "attachment-" + (i + 1);
            String mediaType = item.has("mediaType") ? item.get("mediaType").getAsString() : "application/octet-stream";
            String data = item.has("data") ? item.get("data").getAsString() : null;

            boolean isImage = mediaType.startsWith("image/");
            boolean isText = mediaType.startsWith("text/")
                             || mediaType.contains("json")
                             || mediaType.contains("xml")
                             || mediaType.contains("yaml")
                             || mediaType.contains("yml")
                             || mediaType.contains("csv")
                             || mediaType.contains("markdown");

            if (isImage || !isText || data == null || data.isBlank()) {
                sb.append("- ").append(fileName).append(" (").append(mediaType).append(")").append('\n');
                continue;
            }

            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(data);
                if (bytes.length > 24 * 1024) {
                    sb.append("- ").append(fileName).append(" (").append(mediaType).append(", 已截断)").append('\n');
                    continue;
                }
                String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (content.length() > 4000) {
                    content = content.substring(0, 4000) + "\n...[已截断]";
                }
                if (totalChars + content.length() > 12000) {
                    sb.append("- ").append(fileName).append(" (").append(mediaType).append(", 已截断)").append('\n');
                    continue;
                }
                totalChars += content.length();
                sb.append("- ").append(fileName).append(" (").append(mediaType).append("):").append('\n');
                sb.append(content).append('\n');
            } catch (Exception e) {
                sb.append("- ").append(fileName).append(" (").append(mediaType).append(")").append('\n');
            }
        }
        return sb.toString().trim();
    }

    /**
     * 构建系统提示语 (system prompt), 用于 AI 聊天助手的上下文引导
     * <p> 该方法生成包含当前 IDE 环境信息的系统提示, 包括时间, 项目路径, 项目名称, 当前选中代码段, 已打开文件列表等, 用于增强 AI 回答的上下文相关性.
     *
     * @return 包含系统上下文信息的字符串, 用于作为 AI 请求的系统提示
     */
    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 IntelliAI Engine 的聊天助手，请优先基于当前 IDE 上下文回答。").append('\n');
        sb.append("当前时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date())).append('\n');

        String basePath = project.getBasePath();
        if (basePath != null) {
            sb.append("项目路径: ").append(basePath).append('\n');
        }
        sb.append("项目名称: ").append(project.getName()).append('\n');

        com.intellij.openapi.application.ReadAction.run(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null) {
                Document document = editor.getDocument();
                VirtualFile file = FileDocumentManager.getInstance().getFile(document);
                if (file != null) {
                    sb.append("当前文件: ").append(file.getPath()).append('\n');
                }
                String selectedText = editor.getSelectionModel().getSelectedText();
                if (selectedText != null && !selectedText.isBlank()) {
                    int startLine = document.getLineNumber(editor.getSelectionModel().getSelectionStart()) + 1;
                    int endLine = document.getLineNumber(editor.getSelectionModel().getSelectionEnd()) + 1;
                    sb.append("选中代码 (").append(startLine).append('-').append(endLine).append("):\n");
                    sb.append(selectedText).append('\n');
                }
            }
            VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
            if (openFiles.length > 0) {
                sb.append("已打开文件:").append('\n');
                int count = 0;
                for (VirtualFile file : openFiles) {
                    sb.append("- ").append(file.getPath()).append('\n');
                    count++;
                    if (count >= 20) {
                        break;
                    }
                }
            }
        });

        return sb.toString();
    }

    /**
     * 根据传入的 JSON 对象构建文件标签上下文字符串
     * <p> 从 JSON 对象中提取文件标签信息, 读取对应文件的内容, 并将其格式化为字符串. 会对读取的文件内容进行长度限制和截断处理.
     *
     * @param obj 包含文件标签信息的 JSON 对象, 如果为 null 则返回空字符串
     * @return 格式化后的文件上下文字符串, 包含文件路径和内容
     */
    @NotNull
    private String buildFileTagContext(@Nullable JsonObject obj) {
        if (obj == null || !obj.has("fileTags") || !obj.get("fileTags").isJsonArray()) {
            return "";
        }
        JsonArray fileTags = obj.getAsJsonArray("fileTags");
        if (fileTags.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("文件上下文:").append('\n');
        final int maxPerFileChars = 4000;
        final int maxTotalChars = 12000;
        final long maxBytes = 64 * 1024;
        int totalChars = 0;

        for (int i = 0; i < fileTags.size(); i++) {
            if (totalChars >= maxTotalChars) {
                sb.append("...已达到上下文长度上限").append('\n');
                break;
            }
            if (!fileTags.get(i).isJsonObject()) {
                continue;
            }
            JsonObject tag = fileTags.get(i).getAsJsonObject();
            String displayPath = tag.has("displayPath") ? tag.get("displayPath").getAsString() : null;
            String absolutePath = tag.has("absolutePath") ? tag.get("absolutePath").getAsString() : null;
            if (absolutePath == null || absolutePath.isBlank()) {
                continue;
            }
            String title = displayPath != null && !displayPath.isBlank() ? displayPath : absolutePath;

            String content = com.intellij.openapi.application.ReadAction.compute(() -> {
                com.intellij.openapi.vfs.VirtualFile vf = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                    .findFileByPath(absolutePath);
                if (vf == null || vf.isDirectory()) {
                    return "";
                }
                if (vf.getLength() > maxBytes) {
                    return "";
                }
                com.intellij.openapi.fileTypes.FileType fileType =
                    com.intellij.openapi.fileTypes.FileTypeManager.getInstance().getFileTypeByFile(vf);
                if (fileType.isBinary()) {
                    return "";
                }
                try {
                    return com.intellij.openapi.vfs.VfsUtilCore.loadText(vf);
                } catch (Exception e) {
                    return "";
                }
            });

            if (content.isBlank()) {
                sb.append("- ").append(title).append(" (无法读取或为空)").append('\n');
                continue;
            }
            if (content.length() > maxPerFileChars) {
                content = content.substring(0, maxPerFileChars) + "\n...[已截断]";
            }
            if (totalChars + content.length() > maxTotalChars) {
                int remain = Math.max(0, maxTotalChars - totalChars);
                if (remain < content.length()) {
                    content = content.substring(0, remain) + "\n...[已截断]";
                }
            }
            totalChars += content.length();
            sb.append("- ").append(title).append(":").append('\n');
            sb.append(content).append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * 调用前端 JavaScript 函数
     * <p> 根据给定的函数名和可选的负载数据, 执行对应的 JavaScript 函数. 如果项目已被释放, 则不会执行任何操作.
     *
     * @param function 函数名
     * @param payload  负载数据, 可以为 null
     */
    private void callJs(@NotNull String function, @Nullable String payload) {
        if (project.isDisposed()) {
            return;
        }
        String script;
        if (payload == null) {
            script = "if (window." + function + ") { window." + function + "(); }";
        } else {
            String escaped = StringUtil.escapeStringCharacters(payload);
            script = "if (window." + function + ") { window." + function + "('" + escaped + "'); }";
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (browser.isDisposed()) {
                return;
            }
            browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
        });
    }
}
