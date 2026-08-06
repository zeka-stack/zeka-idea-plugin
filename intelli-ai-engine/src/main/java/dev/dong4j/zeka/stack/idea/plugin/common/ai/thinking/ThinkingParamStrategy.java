package dev.dong4j.zeka.stack.idea.plugin.common.ai.thinking;

import com.google.gson.JsonObject;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.HttpURLConnection;
import java.util.function.BiConsumer;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ThinkingProbeResult;

/**
 * 思考参数策略：将用户意图写入厂商/协议相关的请求扩展字段
 * <p>
 * 约束：只修改思考相关字段，不改动 messages / model / stream 等公共字段。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 * @see ThinkingParamStrategyRegistry
 */
public interface ThinkingParamStrategy {

    /**
     * 策略标识（日志 / 调试）
     *
     * @return 稳定 id
     */
    @NotNull
    String id();

    /**
     * UI 能力描述
     *
     * @param context 上下文
     * @return UI 能力
     */
    @NotNull
    ThinkingUiCapability uiCapability(@NotNull ThinkingContext context);

    /**
     * 将思考相关字段写入已构建的请求体
     *
     * @param body    请求体
     * @param intent  用户意图
     * @param context 上下文
     */
    void apply(@NotNull JsonObject body,
               @NotNull ThinkingIntent intent,
               @NotNull ThinkingContext context);

    /**
     * 测连成功后的思考能力探测（可在网络探测或返回合成结论）
     * <p>
     * 必须在后台线程调用。
     *
     * @param project         项目
     * @param config          配置
     * @param apiKey          API Key
     * @param connectionTuner HTTP 连接调谐（OpenAI 兼容探针使用）
     * @return 探测或合成结果
     */
    @NotNull
    ThinkingProbeResult probe(@NotNull Project project,
                              @NotNull AIProviderConfig config,
                              @Nullable String apiKey,
                              @NotNull BiConsumer<HttpURLConnection, String> connectionTuner);

    /**
     * 用户选择 {@link ThinkingEffort#AUTO} 时的默认强度
     *
     * @return 默认 effort
     */
    @NotNull
    ThinkingEffort defaultEffort();
}
