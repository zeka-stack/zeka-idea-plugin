package dev.dong4j.zeka.stack.idea.plugin.ai.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;

/**
 * Ollama 服务提供商
 *
 * <p>Ollama 是一个本地运行大语言模型的工具，支持多种开源模型。
 * 不需要 API Key，所有模型在本地运行，数据完全私有。
 * 适合对数据隐私有严格要求或希望离线使用的用户。
 *
 * <p>服务优势：
 * <ul>
 *   <li>数据隐私：所有处理都在本地进行，数据不会上传到外部服务器</li>
 *   <li>无 API 成本：无需支付 API 调用费用</li>
 *   <li>离线可用：网络连接不影响使用</li>
 *   <li>开源模型：支持多种开源大语言模型</li>
 * </ul>
 *
 * <p>使用说明：
 * <ol>
 *   <li>安装 Ollama：<a href="https://ollama.ai">https://ollama.ai</a></li>
 *   <li>拉取模型：{@code ollama pull qwen:7b}</li>
 *   <li>启动服务：{@code ollama serve}</li>
 *   <li>在插件中配置 Base URL（默认：http://localhost:11434/v1）</li>
 *   <li>在设置面板中输入模型名称（可输入任何已安装的模型）</li>
 * </ol>
 *
 * <p><b>查看已安装模型：</b> {@code ollama list}
 *
 * <p>推荐模型（仅供参考，您可以使用任何模型）：
 * <ul>
 *   <li>{@code qwen:7b} - 通义千问 7B，中文支持好</li>
 *   <li>{@code codellama:7b} - 代码专用模型，适合代码文档生成</li>
 *   <li>{@code deepseek-coder:6.7b} - 深度求索代码模型</li>
 *   <li>{@code llama2:7b} - 通用模型</li>
 * </ul>
 *
 * <p>性能考虑：
 * <ul>
 *   <li>模型大小影响内存占用和推理速度</li>
 *   <li>较大的模型通常提供更好的生成质量</li>
 *   <li>需要足够的系统资源支持模型运行</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see <a href="https://ollama.ai/library">Ollama 模型库（查看所有可用模型）</a>
 * @see AICompatibleProvider
 * @since 1.0.0
 */
public class OllamaProvider extends AICompatibleProvider {

    /**
     * 构造函数，用于初始化 OllamaProvider 实例
     * <p>
     * 通过传入的 SettingsState 对象初始化父类，并设置相关配置
     *
     * @param settings 用于初始化的配置状态对象
     */
    public OllamaProvider(SettingsState settings, SettingsState.ProviderConfig providerConfig) {
        super(settings, providerConfig);
    }

    /**
     * 获取可用的模型列表
     *
     * <p>Ollama 的模型列表 API 响应格式与标准 OpenAI 格式不同。
     * Ollama 返回的格式：
     * <pre>
     * {
     *   "models": [
     *     {
     *       "name": "qwen:7b",
     *       "model": "qwen:7b",
     *       "modified_at": "2024-01-01T00:00:00Z",
     *       "size": 1234567890,
     *       "digest": "sha256:...",
     *       "details": {
     *         "parent_model": "",
     *         "format": "gguf",
     *         "family": "qwen",
     *         "families": ["qwen"],
     *         "parameter_size": "7B",
     *         "quantization_level": "Q4_0"
     *       }
     *     }
     *   ]
     * }
     * </pre>
     *
     * <p>解析策略：
     * <ul>
     *   <li>提取 models 数组</li>
     *   <li>遍历每个模型对象</li>
     *   <li>提取 name 字段作为模型名称</li>
     *   <li>如果 name 字段不存在，使用 model 字段</li>
     * </ul>
     *
     * @return 可用模型名称列表，如果获取失败返回空列表
     */
    @NotNull
    @Override
    public List<String> getAvailableModels(String apiKey) {
        try {
            // 调用父类方法获取模型列表
            List<String> models = super.getAvailableModels(apiKey);

            // 如果父类方法失败，尝试解析 Ollama 特定格式
            if (models.isEmpty()) {
                models = parseOllamaModelsResponse();
            }

            // 如果仍然为空，返回推荐的模型列表
            if (models.isEmpty()) {
                return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
            }

            return models;

        } catch (Exception e) {
            // 发生异常时返回推荐的模型列表
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
    }

    /**
     * 解析 Ollama 特定的模型列表响应
     *
     * <p>专门处理 Ollama 的响应格式，提取模型名称。
     * 这个方法在父类的标准解析失败时被调用。
     *
     * @return 模型名称列表
     */
    private List<String> parseOllamaModelsResponse() {
        List<String> models = new ArrayList<>();

        try {
            // 这里需要重新发送请求，因为父类方法可能已经处理了响应
            // 在实际实现中，可能需要缓存响应或重新设计方法结构
            // 目前返回推荐的模型列表作为备选方案
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());

        } catch (Exception e) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }
    }

    /**
     * 重写解析方法以支持 Ollama 格式
     *
     * <p>Ollama 的响应格式与标准 OpenAI 格式不同，
     * 需要特殊处理 models 字段而不是 data 字段。
     *
     * @param responseBody JSON 响应体
     * @return 模型名称列表
     */
    @SuppressWarnings("D")
    @Override
    protected List<String> parseModelsResponse(String responseBody) {
        List<String> models = new ArrayList<>();

        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

            // 尝试 Ollama 格式 (models 字段)
            if (json.has("models") && json.get("models").isJsonArray()) {
                JsonArray modelsArray = json.getAsJsonArray("models");
                for (JsonElement element : modelsArray) {
                    JsonObject modelObj = element.getAsJsonObject();
                    String modelName = null;

                    // 优先使用 name 字段
                    if (modelObj.has("name")) {
                        modelName = modelObj.get("name").getAsString();
                    }
                    // 如果没有 name 字段，使用 model 字段
                    else if (modelObj.has("model")) {
                        modelName = modelObj.get("model").getAsString();
                    }

                    if (modelName != null && !modelName.trim().isEmpty()) {
                        models.add(modelName.trim());
                    }
                }
            }
            // 如果 Ollama 格式解析失败，尝试标准格式
            else if (json.has("data") && json.get("data").isJsonArray()) {
                return super.parseModelsResponse(responseBody);
            }

        } catch (Exception e) {
            // 解析失败，返回推荐的模型列表
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }

        // 如果没有找到任何模型，返回推荐的模型列表
        if (models.isEmpty()) {
            return new ArrayList<>(AIProviderType.OLLAMA.getSupportedModels());
        }

        return models;
    }
}

