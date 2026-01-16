package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.codex;

import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.AICompatibleProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Codex Provider（OpenAI）
 * <p>
 * 当前实现基于 OpenAI Chat Completions 兼容协议。
 */
public class CodexProvider extends AICompatibleProvider {

    public CodexProvider(@NotNull Project project,
                         @NotNull AIProviderConfig config,
                         @NotNull AIModelParameters modelParameters,
                         @NotNull AIRuntimeSettings runtimeSettings) {
        super(project, config, modelParameters, runtimeSettings);
    }
}

