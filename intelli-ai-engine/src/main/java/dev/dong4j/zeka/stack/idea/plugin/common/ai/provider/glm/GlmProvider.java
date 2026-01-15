package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.glm;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AICompatibleProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIConsoleLoggerUtil;

/**
 * GLM Provider（智谱 OpenAI 兼容）
 * <p>
 * 智谱 OpenAI 兼容接口并不稳定提供 /models，这里默认返回固定模型列表。
 */
public class GlmProvider extends AICompatibleProvider {

    public GlmProvider(@NotNull Project project,
                       @NotNull AIProviderConfig config,
                       @NotNull AIModelParameters modelParameters,
                       @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }

    @Override
    @NotNull
    public List<String> getAvailableModels(@Nullable String apiKey) {
        AIConsoleLoggerUtil.printWithTimestamp(project, "=== GLM 获取模型列表 ===");
        List<String> models = new ArrayList<>();
        models.add("glm-4.6");
        models.add("glm-4.5");
        models.add("glm-4.5-flash");
        AIConsoleLoggerUtil.printSuccess(project, "成功获取 " + models.size() + " 个模型");
        return models;
    }
}

