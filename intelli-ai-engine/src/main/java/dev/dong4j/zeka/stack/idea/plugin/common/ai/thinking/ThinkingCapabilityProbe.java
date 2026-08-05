package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.BlockingRequestExecutor;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingCapability;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingProbeResult;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 思考能力三探针探测器
 * <p>
 * 在基础连通成功后并发探测 OMIT / TRUE / FALSE 三种 {@code enable_thinking} 传法.
 * 该字段为兼容厂商扩展, 非 OpenAI 官方规范.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class ThinkingCapabilityProbe {

    private static final int PROBE_MAX_TOKENS = 64;
    private static final int PROBE_TIMEOUT_CAP_SECONDS = 20;
    private static final int OVERALL_TIMEOUT_SECONDS = 45;
    /** 探测摘要只展示状态码, 避免把乱码/二进制响应体刷进 UI */
    private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("HTTP\\s+(\\d{3})", Pattern.CASE_INSENSITIVE);

    private ThinkingCapabilityProbe() {
    }

    /**
     * 执行三探针探测并汇总结果
     * <p>
     * 必须在后台线程调用; 内部再并发 3 个子任务.
     *
     * @param project         项目 (日志)
     * @param config          已验证可连通的配置
     * @param apiKey          API Key
     * @param connectionTuner HTTP 连接调谐 (超时 / Authorization)
     * @return 探测结果, 不为 null
     */
    @NotNull
    public static ThinkingProbeResult probe(@NotNull Project project,
                                            @NotNull AIProviderConfig config,
                                            @Nullable String apiKey,
                                            @NotNull BiConsumer<HttpURLConnection, String> connectionTuner) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== 开始思考能力三探针探测 ===");

        AIProviderConfig probeConfig = config.copy();
        if (probeConfig.runtimeSettings == null) {
            probeConfig.runtimeSettings = new AIRuntimeSettings();
        }
        probeConfig.runtimeSettings.timeout = Math.max(5, Math.min(PROBE_TIMEOUT_CAP_SECONDS, probeConfig.runtimeSettings.timeout));

        String modelName = probeConfig.modelName != null ? probeConfig.modelName : "";
        BlockingRequestExecutor executor = new BlockingRequestExecutor(project, probeConfig, connectionTuner);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            CompletableFuture<SingleProbe> omitFuture =
                CompletableFuture.supplyAsync(() -> runSingle(executor, apiKey, modelName, null), pool);
            CompletableFuture<SingleProbe> trueFuture =
                CompletableFuture.supplyAsync(() -> runSingle(executor, apiKey, modelName, Boolean.TRUE), pool);
            CompletableFuture<SingleProbe> falseFuture =
                CompletableFuture.supplyAsync(() -> runSingle(executor, apiKey, modelName, Boolean.FALSE), pool);

            CompletableFuture.allOf(omitFuture, trueFuture, falseFuture)
                .get(OVERALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            ThinkingProbeResult result = aggregate(omitFuture.join(), trueFuture.join(), falseFuture.join());
            AIConsoleLoggerUtil.printSuccess(project, "思考能力探测完成: " + result.capability.displayLabel());
            AIConsoleLoggerUtil.print(project, result.formatForDisplay());
            return result;
        } catch (TimeoutException e) {
            log.warn("Thinking probe timed out", e);
            AIConsoleLoggerUtil.printWarning(project, "思考能力探测超时, 结论标记为未知");
            return unknownResult("探测超时 (" + OVERALL_TIMEOUT_SECONDS + "s)");
        } catch (Exception e) {
            log.warn("Thinking probe failed", e);
            AIConsoleLoggerUtil.printWarning(project, "思考能力探测异常: " + e.getMessage());
            return unknownResult("探测异常: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        } finally {
            pool.shutdownNow();
        }
    }

    @NotNull
    private static SingleProbe runSingle(@NotNull BlockingRequestExecutor executor,
                                         @Nullable String apiKey,
                                         @NotNull String modelName,
                                         @Nullable Boolean enableThinking) {
        try {
            JsonObject body = buildProbeBody(modelName, enableThinking);
            String raw = executor.sendRawResponse(body, apiKey);
            return SingleProbe.ok(responseHasThinkingSignals(raw));
        } catch (AIServiceException e) {
            // 完整错误进日志; 摘要只保留 HTTP 状态, 避免乱码响应体出现在 UI
            log.warn("Thinking probe request failed (enable_thinking={}): {}", enableThinking, e.getMessage());
            return SingleProbe.fail(shortenError(e.getMessage()));
        } catch (Exception e) {
            log.warn("Thinking probe request failed (enable_thinking={})", enableThinking, e);
            return SingleProbe.fail(shortenError(e.getMessage()));
        }
    }

    @NotNull
    private static JsonObject buildProbeBody(@NotNull String modelName, @Nullable Boolean enableThinking) {
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Reply with exactly one word: pong");

        JsonArray messages = new JsonArray();
        messages.add(userMessage);

        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        body.addProperty("stream", false);
        body.addProperty("max_tokens", PROBE_MAX_TOKENS);
        body.add("messages", messages);
        if (enableThinking != null) {
            body.addProperty("enable_thinking", enableThinking);
        }
        return body;
    }

    @NotNull
    private static ThinkingProbeResult aggregate(@NotNull SingleProbe omit,
                                                 @NotNull SingleProbe trueProbe,
                                                 @NotNull SingleProbe falseProbe) {
        ThinkingProbeResult result = new ThinkingProbeResult();
        result.probedAt = System.currentTimeMillis();
        result.omitOk = omit.ok;
        result.trueOk = trueProbe.ok;
        result.falseOk = falseProbe.ok;
        result.omitHasThinking = omit.hasThinking;
        result.trueHasThinking = trueProbe.hasThinking;
        result.falseHasThinking = falseProbe.hasThinking;
        result.omitError = omit.error;
        result.trueError = trueProbe.error;
        result.falseError = falseProbe.error;
        result.capability = deriveCapability(result);
        result.summary = buildSummary(result);
        return result;
    }

    @NotNull
    private static ThinkingCapability deriveCapability(@NotNull ThinkingProbeResult r) {
        if (!r.omitOk && !r.trueOk && !r.falseOk) {
            return ThinkingCapability.UNKNOWN;
        }
        if (r.omitOk && !r.trueOk && !r.falseOk) {
            return ThinkingCapability.UNSUPPORTED;
        }
        if (r.trueOk && !r.falseOk) {
            return ThinkingCapability.REQUIRED_TRUE;
        }
        if (r.trueOk && r.falseOk) {
            if (r.trueHasThinking && !r.falseHasThinking) {
                return ThinkingCapability.OPTIONAL;
            }
            if (r.omitOk && r.omitHasThinking) {
                return ThinkingCapability.DEFAULT_ON_NO_PARAM;
            }
            return ThinkingCapability.OPTIONAL;
        }
        if (r.omitOk) {
            return ThinkingCapability.DEFAULT_ON_NO_PARAM;
        }
        return ThinkingCapability.UNKNOWN;
    }

    @NotNull
    private static String buildSummary(@NotNull ThinkingProbeResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("【思考能力探测】\n");
        sb.append("  不传参数: ").append(formatLine(r.omitOk, r.omitHasThinking, r.omitError)).append('\n');
        sb.append("  enable_thinking=true: ").append(formatLine(r.trueOk, r.trueHasThinking, r.trueError)).append('\n');
        sb.append("  enable_thinking=false: ").append(formatLine(r.falseOk, r.falseHasThinking, r.falseError)).append('\n');
        ThinkingCapability cap = r.capability != null ? r.capability : ThinkingCapability.UNKNOWN;
        sb.append("  结论: ").append(cap.displayLabel());
        return sb.toString();
    }

    @NotNull
    private static String formatLine(boolean ok, boolean hasThinking, @Nullable String error) {
        if (ok) {
            return "✅ 成功" + (hasThinking ? "（检测到思考输出）" : "（未检测到思考输出）");
        }
        String err = error != null ? error : "失败";
        if (err.length() > 120) {
            err = err.substring(0, 120) + "...";
        }
        return "❌ " + err;
    }

    @NotNull
    private static ThinkingProbeResult unknownResult(@NotNull String reason) {
        ThinkingProbeResult result = new ThinkingProbeResult();
        result.capability = ThinkingCapability.UNKNOWN;
        result.probedAt = System.currentTimeMillis();
        result.summary = "【思考能力探测】\n  结论: 未知 — " + reason;
        return result;
    }

    /**
     * 启发式判断响应是否包含思考/推理痕迹
     */
    static boolean responseHasThinkingSignals(@Nullable String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return false;
        }
        String body = rawBody.toLowerCase(Locale.ROOT);
        if (body.contains("<think>") || body.contains("</think>")) {
            return true;
        }
        if (body.contains("\"reasoning_content\"") && !body.contains("\"reasoning_content\":null")) {
            return true;
        }
        if (body.contains("\"thinking\":\"") || body.contains("\"thinking\": \"")) {
            return true;
        }
        if (body.contains("\"reasoning\":\"") || body.contains("\"reasoning\": \"")) {
            return true;
        }
        return body.contains("\"type\":\"reasoning\"");
    }

    /**
     * 将探测失败原因压缩为摘要文案
     * <p>
     * 优先只保留 HTTP 状态码 (如 {@code HTTP 400}); 完整错误体仍由调用方写入日志.
     *
     * @param message 原始异常消息
     * @return 简短失败原因
     */
    @NotNull
    private static String shortenError(@Nullable String message) {
        if (message == null || message.isBlank()) {
            return "请求失败";
        }
        Matcher matcher = HTTP_STATUS_PATTERN.matcher(message);
        if (matcher.find()) {
            return "HTTP " + matcher.group(1);
        }
        String trimmed = message.trim().replace('\n', ' ');
        return trimmed.length() > 80 ? trimmed.substring(0, 80) + "..." : trimmed;
    }

    private record SingleProbe(boolean ok, boolean hasThinking, @Nullable String error) {
        static SingleProbe ok(boolean hasThinking) {
            return new SingleProbe(true, hasThinking, null);
        }

        static SingleProbe fail(String error) {
            return new SingleProbe(false, false, error);
        }
    }
}
