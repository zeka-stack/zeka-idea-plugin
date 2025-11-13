package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIModelParameters;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIRuntimeSettings;

/**
 * Ollama 服务提供商。
 */
public class OllamaProvider extends AICompatibleProvider {

    public OllamaProvider(AIProviderConfig config,
                          AIModelParameters modelParameters,
                          AIRuntimeSettings runtimeSettings) {
        super(config, modelParameters, runtimeSettings);
    }

    @NotNull
    @Override
    public List<String> getAvailableModels(String apiKey) {
        List<String> models = super.getAvailableModels(apiKey);
        if (models.isEmpty()) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
        return models;
    }

    @Override
    protected List<String> parseModelsResponse(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("models") && json.get("models").isJsonArray()) {
                JsonArray modelsArray = json.getAsJsonArray("models");
                for (JsonElement element : modelsArray) {
                    JsonObject modelObj = element.getAsJsonObject();
                    String modelName = null;
                    if (modelObj.has("name")) {
                        modelName = modelObj.get("name").getAsString();
                    } else if (modelObj.has("model")) {
                        modelName = modelObj.get("model").getAsString();
                    }
                    if (modelName != null && !modelName.trim().isEmpty()) {
                        models.add(modelName.trim());
                    }
                }
            } else {
                models = super.parseModelsResponse(responseBody);
            }
        } catch (Exception ignored) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
        if (models.isEmpty()) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
        return models;
    }
}
