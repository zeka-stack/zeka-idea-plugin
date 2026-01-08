package dev.dong4j.zeka.stack.idea.plugin.common.ai.provider.completion.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 原始流式数据块类
 * <p> 用于封装从流式响应中解析出的原始数据块, 包含内容, 推理信息, 角色, 结束原因及原始 JSON 字符串.
 * <p> 该类为不可变数据类, 提供访问器方法获取各字段值, 并支持从 JSON 对象反序列化构建实例.
 * <p> 典型使用场景: 处理大模型流式响应数据, 如 ChatGPT 等 API 的流式输出, 用于逐步解析和展示响应内容.
 * <p> 使用示例:
 * <pre>{@code
 * JsonObject json = ...; // 从 API 响应中获取的 JSON 数据
 * RawStreamChunk chunk = RawStreamChunk.fromJson(json);
 * if (chunk.isDone()) {*     System.out.println("流式响应已结束");
 * }
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.08
 * @since 1.0.0
 */
public final class RawStreamChunk {
    /**
     * 内容字符串, 表示原始流式数据块中的内容部分.
     *
     * @see RawStreamChunk
     */
    private final @Nullable String content;
    /** 推理内容 */
    private final @Nullable String reasoning;
    /** 推理内容, 用于存储模型生成的推理过程或中间思考文本 */
    private final @Nullable String reasoningContent;
    /** 消息角色 */
    private final @Nullable String role;
    /** 响应结束原因 */
    private final @Nullable String finishReason;
    /** 原始 JSON 字符串, 用于存储原始输入数据, 不进行语义解析. */
    private final @Nullable String rawJson;

    /**
     * 构造一个 RawStreamChunk 实例
     * <p> 用于初始化流式数据块的各个字段, 包括内容, 推理信息, 角色等
     *
     * @param content          内容字段, 可以为 null
     * @param reasoning        推理信息, 可以为 null
     * @param reasoningContent 推理内容, 可以为 null
     * @param role             角色字段, 可以为 null
     * @param finishReason     结束原因, 可以为 null
     * @param rawJson          原始 JSON 字符串, 可以为 null
     */
    private RawStreamChunk(@Nullable String content,
                           @Nullable String reasoning,
                           @Nullable String reasoningContent,
                           @Nullable String role,
                           @Nullable String finishReason,
                           @Nullable String rawJson) {
        this.content = content;
        this.reasoning = reasoning;
        this.reasoningContent = reasoningContent;
        this.role = role;
        this.finishReason = finishReason;
        this.rawJson = rawJson;
    }

    /**
     * 获取当前流式数据块的内容
     * <p> 返回流式数据块中的内容字段值, 若该字段未设置或为 null, 则返回 null.
     *
     * @return 当前流式数据块的内容, 可能为 null
     */
    public @Nullable String content() {
        return content;
    }

    /**
     * 获取推理内容字段
     * <p>返回当前流式数据块中的推理内容 (reasoning) 字段值, 该值可能为 null
     *
     * @return 推理内容字符串, 若未设置则返回 null
     */
    public @Nullable String reasoning() {
        return reasoning;
    }

    /**
     * 获取推理内容
     * <p> 返回当前流式数据块中包含的推理内容字段, 如果未设置则返回 null
     *
     * @return 推理内容字符串, 如果未设置则返回 null
     */
    public @Nullable String reasoningContent() {
        return reasoningContent;
    }

    /**
     * 获取消息角色
     * <p> 返回当前流式数据块中消息的角色信息, 如 "assistant","user" 等, 若未设置则返回 null
     *
     * @return 消息角色字符串, 若未设置则返回 null
     */
    public @Nullable String role() {
        return role;
    }

    /**
     * 获取原始 JSON 字符串
     * <p> 返回该流式数据块对应的原始 JSON 字符串内容, 用于调试或日志记录.
     *
     * @return 原始 JSON 字符串, 如果未设置则返回 null
     */
    public @Nullable String rawJson() {
        return rawJson;
    }

    /**
     * 检查流式数据块是否已完成
     * <p> 通过比较 finishReason 是否为 "stop"(忽略大小写) 来判断流是否结束
     *
     * @return 如果流已结束返回 true, 否则返回 false
     */
    public boolean isDone() {
        return "stop".equalsIgnoreCase(finishReason);
    }

    /**
     * 从 JSON 对象创建 RawStreamChunk 实例
     * <p> 解析给定的 JSON 对象, 提取流式响应的各个字段内容, 包括对话内容, 推理内容, 角色等,
     * 并构建对应的 RawStreamChunk 对象返回
     *
     * @param json JSON 对象, 包含流式响应数据, 不能为 null
     * @return 解析后的 RawStreamChunk 实例, 包含从 JSON 中提取的各字段值
     */
    public static @NotNull RawStreamChunk fromJson(@NotNull JsonObject json) {
        JsonObject choice = readFirstChoice(json);
        JsonObject delta = readFirstDelta(choice);
        JsonObject message = readMessage(choice);

        String content = readStringValue(delta, "content");
        if (content == null) {
            content = readStringValue(message, "content");
        }
        String reasoning = readStringValue(delta, "reasoning");
        if (reasoning == null) {
            reasoning = readStringValue(delta, "thinking");
        }
        String reasoningContent = readStringValue(delta, "reasoning_content");
        String role = readStringValue(delta, "role");
        if (role == null) {
            role = readStringValue(message, "role");
        }
        String finishReason = readStringValue(choice, "finish_reason");

        return new RawStreamChunk(content,
                                  reasoning,
                                  reasoningContent,
                                  role,
                                  finishReason,
                                  json.toString());
    }

    /**
     * 从 JSON 对象中读取第一个选择项
     * <p> 从 JSON 的 "choices" 数组中获取第一个元素, 如果数组为空或不存在, 则返回 null
     * <p> 示例:
     * <pre>{@code
     * JsonObject json = ...; // 包含 choices 数组的 JSON 对象
     * JsonObject firstChoice = readFirstChoice(json);
     * }</pre>
     *
     * @param json 包含 "choices" 数组的 JSON 对象, 不能为 null
     * @return 第一个选择项的 JSON 对象, 如果不存在则返回 null
     */
    private static @Nullable JsonObject readFirstChoice(@NotNull JsonObject json) {
        JsonArray choices = json.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JsonElement element = choices.get(0);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    /**
     * 从给定的 choice 对象中读取第一个 delta 字段对应的 JsonObject
     * <p> 如果 choice 为 null 或者 delta 字段不存在, 则返回 null
     *
     * @param choice 包含 delta 字段的 JsonObject, 可以为 null
     * @return 如果存在 delta 字段且其值为 JsonObject, 则返回该对象; 否则返回 null
     */
    private static @Nullable JsonObject readFirstDelta(@Nullable JsonObject choice) {
        if (choice == null) {
            return null;
        }
        JsonElement delta = choice.get("delta");
        return delta != null && delta.isJsonObject() ? delta.getAsJsonObject() : null;
    }

    /**
     * 从选择对象中读取消息对象
     * <p> 如果选择对象为空, 则返回 null; 否则, 从选择对象中提取消息对象并返回
     *
     * @param choice 选择对象, 可以为 null
     * @return 消息对象, 如果选择对象为空或没有消息对象则返回 null
     */
    private static @Nullable JsonObject readMessage(@Nullable JsonObject choice) {
        if (choice == null) {
            return null;
        }
        JsonElement message = choice.get("message");
        return message != null && message.isJsonObject() ? message.getAsJsonObject() : null;
    }

    /**
     * 从 JSON 对象中读取指定键的字符串值
     * <p> 如果对象为空, 不包含指定键, 或键对应的值为 null, 则返回 null
     *
     * @param object JSON 对象, 不能为 null
     * @param key    要读取的键名, 不能为 null
     * @return 对应键的字符串值, 如果不存在或为 null 则返回 null
     */
    private static @Nullable String readStringValue(@Nullable JsonObject object, @NotNull String key) {
        if (object == null || !object.has(key)) {
            return null;
        }
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }
}
