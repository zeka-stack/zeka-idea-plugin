package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 修复提示词构建器.
 */
public final class FixPromptBuilder {
    private FixPromptBuilder() {
    }

    public static String systemPrompt() {
        return """
            你是一个 IntelliJ IDEA 插件中的代码修复引擎。
            你只允许修改提供的代码片段，不得假设其他上下文。
            不要输出解释，只返回修复后的代码片段本身。
            """;
    }

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
}
