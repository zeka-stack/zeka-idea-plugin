package dev.dong4j.zeka.stack.idea.plugin.workflow.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.workflow.model.ClassRelationshipContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.MethodCallerChainContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.WorkflowContext;

/**
 * JSON 序列化工具
 *
 * @author dong4j
 * @version 1.0.0
 */
public final class JSONSerializer {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private JSONSerializer() {
        // 工具类，禁止实例化
    }

    /**
     * 将工作流上下文序列化为 JSON
     *
     * @param context 工作流上下文
     * @return JSON 字符串
     */
    @NotNull
    public static String toJson(@NotNull WorkflowContext context) {
        return GSON.toJson(context);
    }

    /**
     * 将方法调用链上下文序列化为 JSON
     *
     * @param context 方法调用链上下文
     * @return JSON 字符串
     */
    @NotNull
    public static String toJson(@NotNull MethodCallerChainContext context) {
        return GSON.toJson(context);
    }

    /**
     * 将类关系上下文序列化为 JSON
     *
     * @param context 类关系上下文
     * @return JSON 字符串
     */
    @NotNull
    public static String toJson(@NotNull ClassRelationshipContext context) {
        return GSON.toJson(context);
    }

    /**
     * 将任意对象序列化为 JSON（泛型方法）
     *
     * @param object 要序列化的对象
     * @return JSON 字符串
     */
    @NotNull
    public static String toJson(@NotNull Object object) {
        return GSON.toJson(object);
    }
}

