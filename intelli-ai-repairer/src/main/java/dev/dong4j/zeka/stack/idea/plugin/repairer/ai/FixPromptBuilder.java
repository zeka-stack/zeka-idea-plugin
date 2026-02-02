package dev.dong4j.zeka.stack.idea.plugin.repairer.ai;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.repairer.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * 代码修复提示构造器
 * <p>
 * 从 {@link SettingsState} 读取提示词模板，替换占位符后返回最终提示文本。
 * 系统提示、用户提示、增强版用户提示均可在设置页中配置。
 * </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 2025.3.1200
 */
public final class FixPromptBuilder {
    /**
     * 私有构造函数, 防止外部实例化此类
     * <p> 这是一个工具类, 所有方法都是静态的, 因此不需要实例化
     */
    private FixPromptBuilder() {
    }

    /**
     * 返回当前生效的系统提示词
     * <p>
     * 优先使用 Repairer 设置中的系统提示词；未配置或为空时使用默认。
     * </p>
     *
     * @return 系统提示词，不为 null
     */
    @NotNull
    public static String systemPrompt() {
        SettingsState settings = SettingsState.getInstance();
        if (settings != null && settings.systemPrompt != null && !settings.systemPrompt.isBlank()) {
            return settings.systemPrompt;
        }
        return SettingsState.getDefaultSystemPrompt();
    }

    /**
     * 根据用户提示词模板与违规、代码片段生成用户提示
     * <p>
     * 模板占位符：{tool}、{ruleId}、{message}、{snippet}。
     * </p>
     *
     * @param violation 违规信息
     * @param snippet   原始代码片段
     * @return 替换占位符后的用户提示
     */
    @NotNull
    public static String userPrompt(@NotNull CodeViolation violation, @NotNull String snippet) {
        String template = getEffectiveUserPromptTemplate();
        return template
            .replace("{tool}", violation.tool)
            .replace("{ruleId}", violation.ruleId)
            .replace("{message}", violation.message)
            .replace("{snippet}", snippet);
    }

    /**
     * 根据增强版用户提示词模板与违规、代码片段、上下文生成用户提示
     * <p>
     * 模板占位符：{tool}、{ruleId}、{message}、{snippet}、{context}。
     * </p>
     *
     * @param violation          违规信息
     * @param snippet            原始代码片段
     * @param surroundingContext 上下文内容
     * @return 替换占位符后的用户提示
     */
    @NotNull
    public static String enhancedUserPrompt(@NotNull CodeViolation violation,
                                            @NotNull String snippet,
                                            @NotNull String surroundingContext) {
        String template = getEffectiveEnhancedUserPromptTemplate();
        return template
            .replace("{tool}", violation.tool)
            .replace("{ruleId}", violation.ruleId)
            .replace("{message}", violation.message)
            .replace("{snippet}", snippet)
            .replace("{context}", surroundingContext);
    }

    /**
     * 获取当前生效的用户提示词模板
     * <p>
     * 优先使用 {@link SettingsState} 中配置的用户提示词模板; 若未配置或为空, 则返回默认模板.
     * </p>
     *
     * @return 生效的用户提示词模板, 不为 null
     */
    @NotNull
    private static String getEffectiveUserPromptTemplate() {
        SettingsState settings = SettingsState.getInstance();
        if (settings != null && settings.userPromptTemplate != null && !settings.userPromptTemplate.isBlank()) {
            return settings.userPromptTemplate;
        }
        return SettingsState.getDefaultUserPromptTemplate();
    }

    /**
     * 获取当前生效的增强版用户提示词模板
     * <p> 优先使用设置中配置的增强版用户提示词模板; 若配置为空, 则返回默认模板.
     *
     * @return 当前生效的增强版用户提示词模板, 不为 null
     */
    @NotNull
    private static String getEffectiveEnhancedUserPromptTemplate() {
        SettingsState settings = SettingsState.getInstance();
        if (settings != null && settings.enhancedUserPromptTemplate != null
            && !settings.enhancedUserPromptTemplate.isBlank()) {
            return settings.enhancedUserPromptTemplate;
        }
        return SettingsState.getDefaultEnhancedUserPromptTemplate();
    }
}
