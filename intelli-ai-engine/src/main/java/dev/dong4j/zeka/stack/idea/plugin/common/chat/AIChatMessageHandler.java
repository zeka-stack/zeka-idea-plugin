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
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.util.ui.StartupUiUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIStreamResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.StreamCancellationToken;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AIChatMessageHandler {
    private static final Logger LOG = Logger.getInstance(AIChatMessageHandler.class);

    private final Project project;
    private final JBCefBrowser browser;
    private final AIService aiService;
    private final AIProviderSettings providerSettings;
    private final Map<String, AIProviderConfig> providersById = new HashMap<>();

    private String currentProviderId;
    private String currentModel;
    private boolean streamingEnabled = true;
    private String sendShortcut = "enter";
    private StreamCancellationToken currentToken;
    private final Object streamLock = new Object();
    private volatile long lastChunkAt = 0L;
    private volatile boolean streamFinished = false;
    private volatile boolean streamStarted = false;

    AIChatMessageHandler(@NotNull Project project, @NotNull JBCefBrowser browser) {
        this.project = project;
        this.browser = browser;
        this.aiService = AIServiceImpl.getInstance();
        this.providerSettings = AIProviderSettings.getInstance();
    }

    void onFrontendReadyInjected() {
        sendProviders();
        sendIdeTheme();
        sendDependencyStatus();
        sendSelectionInfo();
        sendStreamingEnabled();
        sendSendShortcut();
    }

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

    private void setProvider(@NotNull String providerId) {
        String id = providerId.trim();
        if (!id.isEmpty()) {
            currentProviderId = id;
            AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] set_provider=" + id);
            LOG.info("[Chat] set_provider=" + id);
        }
    }

    private void setModel(@NotNull String model) {
        String value = model.trim();
        if (!value.isEmpty()) {
            currentModel = value;
            AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] set_model=" + value);
            LOG.info("[Chat] set_model=" + value);
        }
    }

    private void setStreamingEnabled(@NotNull String payload) {
        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            streamingEnabled = obj.has("streamingEnabled") && obj.get("streamingEnabled").getAsBoolean();
        } catch (Exception ignore) {
        }
    }

    private void setSendShortcut(@NotNull String payload) {
        try {
            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
            if (obj.has("sendShortcut")) {
                sendShortcut = obj.get("sendShortcut").getAsString();
            }
        } catch (Exception ignore) {
        }
    }

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

        AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] send message, provider="
            + config.providerType.getDisplayName() + ", model=" + config.modelName);
        LOG.info("[Chat] send message, provider=" + config.providerType.getDisplayName() + ", model=" + config.modelName);

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
        final java.util.concurrent.atomic.AtomicBoolean hasAnyOutput = new java.util.concurrent.atomic.AtomicBoolean(false);
        lastChunkAt = System.currentTimeMillis();
        streamFinished = false;
        streamStarted = false;
        scheduleStreamTimeout();
        ensureStreamStarted();
        scheduleFallback(request, config, token, hasAnyOutput);

        try {
            aiService.generateContentStream(project, request, config, new AIStreamResponseListener() {
                @Override
                public void onStart() {
                    ensureStreamStarted();
                    AIConsoleLoggerUtil.printWithTimestamp(project, "[Chat] stream start");
                    LOG.info("[Chat] stream start");
                }

                @Override
                public void onChunk(@NotNull String chunk) {
                    if (streamFinished) {
                        return;
                    }
                    ensureStreamStarted();
                    callJs("onContentDelta", chunk);
                    hasAnyOutput.set(true);
                    hasChunk[0] = true;
                    chunkCount[0] += 1;
                    charCount[0] += chunk.length();
                    lastChunkAt = System.currentTimeMillis();
                }

                @Override
                public void onThinkingChunk(@NotNull String chunk) {
                    if (streamFinished) {
                        return;
                    }
                    ensureStreamStarted();
                    callJs("onThinkingDelta", chunk);
                    hasAnyOutput.set(true);
                    lastChunkAt = System.currentTimeMillis();
                }

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
                    if (!fullText.isBlank()) {
                        hasAnyOutput.set(true);
                    }
                    finishStream("[Chat] stream complete, chunks=" + chunkCount[0] + ", len=" + charCount[0]);
                }

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

                @Override
                public void onError(@NotNull String error, @Nullable Throwable exception) {
                    if (streamFinished) {
                        return;
                    }
                    callJs("addErrorMessage", error);
                    finishStream("[Chat] stream error: " + error);
                    LOG.warn("[Chat] stream error: " + error, exception);
                }

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

    private void interruptCurrent() {
        StreamCancellationToken token = currentToken;
        if (token != null) {
            token.cancel();
            AIConsoleLoggerUtil.printWarning(project, "[Chat] interrupt stream");
            LOG.info("[Chat] interrupt stream");
        }
        finishStream("[Chat] interrupt stream");
    }

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

    private void scheduleFallback(@NotNull AIChatRequest request,
                                  @NotNull AIProviderConfig config,
                                  @NotNull StreamCancellationToken token,
                                  @NotNull java.util.concurrent.atomic.AtomicBoolean hasAnyOutput) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                return;
            }
            if (streamFinished || hasAnyOutput.get()) {
                return;
            }
            token.cancel();
            try {
                String result = aiService.generateContent(project, request, config, null);
                if (!result.isBlank()) {
                    callJs("onContentDelta", result);
                    hasAnyOutput.set(true);
                }
                finishStream("[Chat] stream fallback (non-stream)");
            } catch (AIServiceException e) {
                callJs("addErrorMessage", "AI 服务调用失败: " + e.getMessage());
                finishStream("[Chat] fallback error: " + e.getMessage());
                LOG.warn("[Chat] fallback error: " + e.getMessage(), e);
            }
        });
    }

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

    private String buildFallbackId(@NotNull AIProviderConfig config) {
        return config.providerType.getProviderId() + ":" + config.modelName + ":" + config.baseUrl;
    }

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
            item.addProperty("label", config.providerType.getDisplayName());
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

    private void sendIdeTheme() {
        boolean isDark = StartupUiUtil.INSTANCE.isDarkTheme();
        JsonObject obj = new JsonObject();
        obj.addProperty("isDark", isDark);
        callJs("onIdeThemeReceived", obj.toString());
    }

    private void sendDependencyStatus() {
        JsonObject obj = new JsonObject();
        JsonObject engine = new JsonObject();
        engine.addProperty("installed", true);
        engine.addProperty("status", "installed");
        obj.add("engine", engine);
        callJs("updateDependencyStatus", obj.toString());
    }

    private void sendStreamingEnabled() {
        JsonObject obj = new JsonObject();
        obj.addProperty("streamingEnabled", streamingEnabled);
        callJs("updateStreamingEnabled", obj.toString());
    }

    private void sendSendShortcut() {
        JsonObject obj = new JsonObject();
        obj.addProperty("sendShortcut", sendShortcut);
        callJs("updateSendShortcut", obj.toString());
    }

    private void sendSlashCommands() {
        callJs("updateSlashCommands", "[]");
    }

    private void sendAgents() {
        callJs("updateAgents", "[]");
    }

    private void sendSelectedAgent() {
        callJs("onSelectedAgentReceived", "null");
    }

    void sendSelectionInfo() {
        String payload = com.intellij.openapi.application.ReadAction.compute(this::buildSelectionPayload);
        if (payload == null || payload.isBlank()) {
            callJs("clearSelectionInfo", null);
            return;
        }
        callJs("addSelectionInfo", payload);
    }

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

    private void openBrowser(@NotNull String url) {
        String target = url.trim();
        if (!target.isEmpty()) {
            BrowserUtil.browse(target);
        }
    }

    @Nullable
    private JsonObject parsePayload(@NotNull String payload) {
        try {
            return JsonParser.parseString(payload).getAsJsonObject();
        } catch (Exception e) {
            LOG.debug("parse payload failed: " + e.getMessage());
            return null;
        }
    }

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
