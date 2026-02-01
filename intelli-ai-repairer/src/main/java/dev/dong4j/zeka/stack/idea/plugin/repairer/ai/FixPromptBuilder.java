package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 代码修复提示构造器
 * <p> 此工具类提供用于 IntelliJ IDEA 插件中代码修复引擎的两类提示文本: 系统提示和用户提示.</p>
 * <p> 系统提示告知修复引擎的基本行为约束, 用户提示则包含静态分析工具检测出的违规信息以及需要修改的代码片段, 确保修复过程仅针对指定片段且保持原有逻辑.</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email mailto:dong4j@gmail.com
 * @date 2026.01.20
 * @since 2025.3.1200
 */
public final class FixPromptBuilder {
    /**
     * 私有构造函数, 用于防止外部实例化
     * <p> 该构造函数为私有, 确保 FixPromptBuilder 类不能被外部直接创建实例
     */
    private FixPromptBuilder() {
    }

    /**
     * 返回 IntelliJ IDEA 插件中代码修复引擎使用的系统提示字符串.<br>
     * 该提示说明了引擎的角色, 使用限制以及响应格式, 所有信息均为多行文本.<br>
     *
     * @return 系统提示字符串
     */
    public static String systemPrompt() {
        return """
            你是一个 IntelliJ IDEA 插件中的代码修复引擎。
            你只允许修改提供的代码片段，不得假设其他上下文。
            不要输出解释，只返回修复后的代码片段本身。
            """;
    }

    /**
     * 生成用户提示信息字符串
     * <p> 根据给定的 CodeViolation 对象和代码片段, 生成一个包含规则信息和原始代码片段的提示信息字符串.
     * 提示信息用于指导代码修复, 确保仅修改指定的代码片段, 并保持原有语义不变.
     *
     * @param violation 包含工具名, 规则 ID 和描述的 CodeViolation 对象
     * @param snippet   原始代码片段
     * @return 包含规则信息和原始代码片段的提示信息字符串
     */
    public static String userPrompt(CodeViolation violation, String snippet) {
        return """
            以下是静态代码分析工具检测出的代码问题。

            【规则信息】
            - 工具：%s
            - Rule：%s
            - 描述：%s

            【原始代码片段】
            <<<CODE>>>
            %s
            <<<END>>>

            【要求】
            - 仅修改上述代码片段
            - 保持原有语义不变
            - 修复该规则问题
            - 不引入额外格式化
            - 返回完整替换后的代码片段
            """.formatted(
            violation.tool,
            violation.ruleId,
            violation.message,
            snippet
                         );
    }

    /**
     * 生成增强版用户提示信息字符串, 用于指导代码修复引擎在保留上下文的前提下仅修改指定代码片段
     * <p> 该方法结合规则信息, 原始代码片段和上下文信息, 生成结构化提示, 确保修复过程精准, 安全, 语义不变 </p>
     *
     * @param violation          包含工具名, 规则 ID 和描述的 CodeViolation 对象
     * @param snippet            原始代码片段, 将被替换或修复的部分
     * @param surroundingContext 与原始代码片段相邻的上下文内容, 用于提供语义背景, 但不得被修改
     * @return 包含规则信息, 原始代码片段, 上下文信息及修复要求的完整提示字符串
     */
    public static String enhancedUserPrompt(CodeViolation violation, String snippet, String surroundingContext) {
        return """
            以下是静态代码分析工具检测出的代码问题。

            【规则信息】
            - 工具：%s
            - Rule：%s
            - 描述：%s

            【原始代码片段】
            <<<CODE>>>
            %s
            <<<END>>>

            【上下文信息】
            <<<CONTEXT>>>
            %s
            <<<END>>>

            【要求】
            - 仅修改上述代码片段，不得修改上下文
            - 保持原有语义不变
            - 修复该规则问题
            - 保持与原始代码相同的缩进和格式
            - 不引入额外的变量或方法
            - 不改变代码的执行逻辑
            - 返回完整替换后的代码片段，不要添加任何解释
            """.formatted(
            violation.tool,
            violation.ruleId,
            violation.message,
            snippet,
            surroundingContext
                         );
    }
}
