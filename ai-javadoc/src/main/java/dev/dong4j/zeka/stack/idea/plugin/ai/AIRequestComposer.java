package dev.dong4j.zeka.stack.idea.plugin.ai;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.util.TokenCounter;

/**
 * 负责根据当前设置与任务数据构建 {@link AIChatRequest}。
 */
public final class AIRequestComposer {

    /**
     * 私有构造函数, 用于防止外部实例化
     * <p>
     * 该构造函数仅在内部使用, 通常用于单例模式或工具类中
     */
    private AIRequestComposer() {
    }

    /**
     * 组合生成 AI 聊天请求对象
     * <p>
     * 根据提供的设置和文档任务, 生成包含系统提示, 用户提示和预估令牌数的 AI 聊天请求对象.
     *
     * @param settings 配置设置, 用于生成系统提示
     * @param task     文档任务, 用于生成用户提示
     * @return 构建好的 AIChatRequest 对象
     * @since 1.0.0
     */
    @NotNull
    public static AIChatRequest compose(@NotNull SettingsState settings,
                                        @NotNull DocumentationTask task) {
        String systemPrompt = resolveSystemPrompt(settings);
        String userPrompt = buildUserPrompt(settings, task);
        int tokenEstimate = TokenCounter.estimateTokens(systemPrompt) + TokenCounter.estimateTokens(userPrompt);
        return new AIChatRequest(systemPrompt, userPrompt, tokenEstimate);
    }

    /**
     * 解析系统提示模板
     * <p>
     * 根据传入的 {@code SettingsState} 对象获取系统提示模板字符串.
     * 若 {@code systemPromptTemplate} 为 {@code null} 或空字符串, 则返回默认模板.
     *
     * @param settings 包含系统提示模板的设置对象, 不能为空
     * @return 解析得到的系统提示模板字符串
     */
    private static String resolveSystemPrompt(@NotNull SettingsState settings) {
        String userSystemPrompt = settings.systemPromptTemplate;
        if (userSystemPrompt == null || userSystemPrompt.trim().isEmpty()) {
            return SettingsState.getDefaultSystemPromptTemplate();
        }
        return userSystemPrompt;
    }

    /**
     * 根据任务类型构建用户提示信息模板
     * <p>
     * 根据传入的设置状态和任务类型, 选择对应的提示信息模板, 并使用任务代码进行格式化
     *
     * @param settings 设置状态对象, 用于获取模板配置
     * @param task     任务对象, 包含任务类型和代码信息
     * @return 格式化后的提示信息字符串
     */
    private static String buildUserPrompt(@NotNull SettingsState settings, @NotNull DocumentationTask task) {
        String template = switch (task.getType()) {
            case CLASS, INTERFACE, ENUM -> resolveTemplate(settings.classPromptTemplate,
                                                           SettingsState.getDefaultClassPromptTemplate());
            case FIELD -> resolveTemplate(settings.fieldPromptTemplate,
                                          SettingsState.getDefaultFieldPromptTemplate());
            case TEST_METHOD -> resolveTemplate(settings.testPromptTemplate,
                                                SettingsState.getDefaultTestPromptTemplate());
            default -> resolveTemplate(settings.methodPromptTemplate,
                                       SettingsState.getDefaultMethodPromptTemplate());
        };
        return String.format(template, task.getCode());
    }

    /**
     * 根据用户模板优先于默认模板返回对应的模板字符串
     * <p>
     * 如果用户模板不为空, 则返回用户模板; 否则返回默认模板
     *
     * @param userTemplate    用户提供的模板字符串
     * @param defaultTemplate 默认模板字符串
     * @return 用户模板或默认模板字符串
     */
    private static String resolveTemplate(String userTemplate, String defaultTemplate) {
        if (userTemplate == null || userTemplate.isBlank()) {
            return defaultTemplate;
        }
        return userTemplate;
    }
}
