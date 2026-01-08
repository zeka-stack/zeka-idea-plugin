package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 流式数据解析器接口
 * <p> 用于解析流式传输的 JSON 数据块, 支持从 JSON 对象中提取并构建 StreamChunk 对象.
 * <p> 提供默认方法 isDone, 用于判断当前 JSON 数据是否表示流式处理已完成.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public interface StreamChunkParser {
    /**
     * 解析 JSON 数据并返回对应的流块对象
     * <p> 该方法用于将传入的 JSON 对象解析为一个 {@code StreamChunk} 实例, 可能返回 null 表示解析失败或无结果
     *
     * @param json 需要解析的 JSON 对象, 不能为 null
     * @return 解析得到的流块对象, 如果解析失败或无效则返回 null
     */
    @Nullable
    StreamChunk parse(@NotNull JsonObject json);

    /**
     * 判断解析是否完成
     * <p> 检查给定的 JSON 对象是否表示解析已经完成. 通过读取第一个 choice 并检查 finish_reason 字段是否为 "stop" 来判断.
     *
     * @param json 要检查的 JSON 对象, 不能为 null
     * @return 如果解析完成返回 true, 否则返回 false
     */
    default boolean isDone(@NotNull JsonObject json) {
        JsonObject choice = readFirstChoice(json);
        if (choice == null) {
            return false;
        }
        String finishReason = readStringValue(choice, "finish_reason");
        return "stop".equalsIgnoreCase(finishReason);
    }

    /**
     * 从 JSON 对象中读取第一个选择项 (choice)
     * <p> 该方法用于解析响应中的 choices 数组, 返回第一个 choice 对象. 如果 choices 不存在或为空, 则返回 null.
     *
     * @param json 包含 choices 数组的 JSON 对象, 不能为 null
     * @return 第一个 choice 对象, 如果不存在或为空则返回 null
     */
    @Nullable
    static JsonObject readFirstChoice(@NotNull JsonObject json) {
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).getAsJsonObject();
    }

    /**
     * 从指定的 JSON 对象中读取第一个 "delta" 字段内容
     * <p> 该方法首先调用 {@link #readFirstChoice(JsonObject)} 方法获取第一个 "choices" 数组中的对象,
     * 然后从中提取名为 "delta" 的子对象. 如果找不到或为 null, 则返回 null.
     *
     * @param json 包含 AI 流式响应数据的 JSON 对象, 不能为 null
     * @return 第一个 "delta" 字段对应的 JSON 对象, 如果未找到或解析失败则返回 null
     */
    @Nullable
    static JsonObject readFirstDelta(@NotNull JsonObject json) {
        JsonObject choice = readFirstChoice(json);
        if (choice == null) {
            return null;
        }
        return choice.getAsJsonObject("delta");
    }

    /**
     * 从 JSON 对象中安全地读取字符串值.
     * <p>
     * 该方法会检查指定键是否存在以及对应的 JSON 元素是否为 null 或 JsonNull,
     * 只有在所有检查都通过的情况下, 才会返回元素的字符串值.
     * </p>
     *
     * @param delta JSON 对象, 包含要读取的键值对
     * @param key   要读取的键名
     * @return 如果键存在且对应的值为非空字符串, 则返回该字符串值; 否则返回 null
     */
    @Nullable
    static String readStringValue(@NotNull JsonObject delta, @NotNull String key) {
        if (!delta.has(key)) {
            return null;
        }
        JsonElement element = delta.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }
}
