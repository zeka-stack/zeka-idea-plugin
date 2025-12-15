package dev.dong4j.zeka.stack.idea.plugin.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 生成上下文类
 * <p> 用于封装与代码生成相关的上下文信息, 主要存储待生成代码的片段, 便于后续处理和解析.
 *
 * @param classCodeSnippet 类级别代码上下文
 *                         <p>
 *                         通常为当前元素所在类(或 Kotlin 类/对象)的前若干行代码, 包含类注释、字段、方法签名等信息,
 *                         用于在为字段/方法生成注释时提供更完整的语义环境。
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.15
 * @since 1.0.0
 */
public record GenerationContext(@Nullable String classCodeSnippet) {

    /**
     * 使用类级别代码片段构造上下文对象。
     *
     * @param classCodeSnippet 类代码片段(可以为 null)
     */
    public GenerationContext {
    }

    /**
     * 创建仅包含类代码片段的上下文对象。
     *
     * @param classCodeSnippet 类代码片段
     * @return 上下文对象
     */
    @NotNull
    public static GenerationContext ofClassCode(@Nullable String classCodeSnippet) {
        return new GenerationContext(classCodeSnippet);
    }

    /**
     * 创建一个空的 GenerationContext 实例
     * <p>
     * 该方法返回一个使用空字符串初始化的 GenerationContext 对象, 通常用于表示没有内容或初始状态的上下文.
     *
     * @return 新创建的空 GenerationContext 实例
     */
    public static GenerationContext empty() {
        return new GenerationContext("");
    }
}

