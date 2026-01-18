package dev.dong4j.zeka.stack.idea.javadoc.ai;

import com.intellij.psi.PsiElement;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.javadoc.settings.CustomJavadocTag;
import dev.dong4j.zeka.stack.idea.javadoc.settings.OverrideMode;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.GenerationContext;
import dev.dong4j.zeka.stack.idea.javadoc.util.ProjectVersionResolver;
import dev.dong4j.zeka.stack.idea.javadoc.util.PsiElementLocator;
import dev.dong4j.zeka.stack.idea.javadoc.util.SystemUtils;
import dev.dong4j.zeka.stack.idea.javadoc.util.TokenCounter;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.ResponseLanguage;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 请求构建器
 * <p>
 * 该类负责根据设置和文档任务构建 AI 聊天请求, 包括系统提示词和用户提示词的生成,
 * 并估算请求的 token 数量. 主要用于代码文档自动生成场景, 根据不同类型的代码元素
 * (类, 接口, 枚举, 字段, 测试方法等) 生成相应的提示词模板.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
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
     * 根据提供的项目, 配置设置和文档任务, 生成包含系统提示词, 用户提示词及预估令牌数的 AI 聊天请求对象.
     * 系统提示词和用户提示词均会根据语言设置进行占位符替换, 最终返回封装好的请求对象.
     *
     * @param settings 配置设置对象, 用于生成系统提示词模板
     * @param task     文档生成任务对象, 用于生成用户提示词模板
     * @return 构建完成的 AIChatRequest 对象, 包含系统提示, 用户提示和令牌预估数量
     * @since 1.0.0
     */
    @NotNull
    public static AIChatRequest compose(@NotNull SettingsState settings,
                                        @NotNull DocumentationTask task) {
        String systemPrompt = resolveSystemPrompt(settings);
        String userPrompt = buildUserPrompt(settings, task);

        // 根据语言选择替换提示词中的语言占位符
        // 必须在所有其他占位符（如 ${author}、${date} 等）替换完成后进行
        AIProviderSettings providerSettings = AIProviderSettings.getInstance();
        ResponseLanguage responseLanguage = providerSettings != null && providerSettings.responseLanguage != null
                                            ? providerSettings.responseLanguage
                                            : ResponseLanguage.ZH;
        // 提示词模板使用中文，因此使用 getDescForPrompt() 获取中文文本
        String languageText = responseLanguage.getDescForPrompt();

        // 替换系统提示词和用户提示词中的所有 ${language} 占位符
        systemPrompt = replaceLanguagePlaceholder(systemPrompt, languageText);
        userPrompt = replaceLanguagePlaceholder(userPrompt, languageText);

        log.debug("systemPrompt: {}", systemPrompt);
        log.debug("userPrompt: {}", userPrompt);
        int tokenEstimate = TokenCounter.estimateTokens(systemPrompt) + TokenCounter.estimateTokens(userPrompt);
        return new AIChatRequest(systemPrompt, userPrompt, tokenEstimate);
    }

    /**
     * 替换提示词中的语言占位符
     * <p>
     * 将提示词中的所有 简体中文占位符替换为实际的语言文本.
     * 支持多次替换, 确保所有占位符都被正确替换.
     *
     * @param prompt       包含占位符的提示词
     * @param languageText 要替换的语言文本 ("中文" 或 "英文")
     * @return 替换后的提示词
     */
    private static String replaceLanguagePlaceholder(@NotNull String prompt, @NotNull String languageText) {
        return prompt.replace("${language}", languageText);
    }

    /**
     * 解析系统提示模板
     * <p>
     * 根据传入的 {@code SettingsState} 对象获取系统提示模板字符串.
     * 如果 {@code systemPromptTemplate} 为 null 或空字符串, 则返回默认模板.
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
     * 根据项目, 配置设置和文档任务构建 AI 聊天请求对象
     * <p>
     * 该方法负责组合生成包含系统提示词, 用户提示词及预估令牌数的 AI 聊天请求对象.
     * 系统提示词和用户提示词均会根据语言设置进行占位符替换, 最终返回封装好的请求对象.
     *
     * @param settings 配置设置对象, 用于生成系统提示词模板
     * @param task     文档生成任务对象, 用于生成用户提示词模板
     * @return 构建完成的 AIChatRequest 对象, 包含系统提示, 用户提示和令牌预估数量
     * @since 1.0.0
     */
    private static String buildUserPrompt(@NotNull SettingsState settings, @NotNull DocumentationTask task) {
        // 如果覆写模式是 FIX（仅修复错误注释），且元素已有注释，使用修复错误 Javadoc 的提示词
        if (settings.overrideExisting && settings.overrideMode == OverrideMode.FIX) {
            // 检查元素是否已有注释
            PsiElement element = task.getElement();
            if (PsiElementLocator.hasJavaDoc(element)) {
                // 有注释，使用修复提示词
                String template = resolveTemplate(settings.fixJavadocPromptTemplate,
                                                  SettingsState.getDefaultFixJavadocPromptTemplate());
                String codeWithContext = mergeContextAndCode(task.getContext(), task.getCode());
                return String.format(template, codeWithContext);
            }
            // 没有注释，继续使用正常的生成提示词（见下面的逻辑）
        }

        // 否则使用正常的提示词模板
        String template = switch (task.getType()) {
            case CLASS, INTERFACE, ENUM -> {
                // 解析类模板
                String classTemplate = resolveClassTemplate(task, settings.classPromptTemplate,
                                                            SettingsState.getDefaultClassPromptTemplate());

                // 合并语义上下文、类代码上下文和代码，并替换模板中的占位符
                yield mergeContextAndCode(classTemplate, task.getContext(), task.getCode());
            }
            case FIELD -> resolveTemplate(settings.fieldPromptTemplate,
                                          SettingsState.getDefaultFieldPromptTemplate());
            case TEST_METHOD -> resolveTemplate(settings.testPromptTemplate,
                                                SettingsState.getDefaultTestPromptTemplate());
            default -> resolveTemplate(settings.methodPromptTemplate,
                                       SettingsState.getDefaultMethodPromptTemplate());
        };

        // 对于非类类型，使用原有逻辑
        if (task.getType() != DocumentationTask.TaskType.CLASS
            && task.getType() != DocumentationTask.TaskType.INTERFACE
            && task.getType() != DocumentationTask.TaskType.ENUM) {
            String codeWithContext = mergeContextAndCode(task.getContext(), task.getCode());
            return String.format(template, codeWithContext);
        }

        return template;
    }

    /**
     * 将上下文信息合并到待处理代码中, 作为用户提示的一部分.
     * <p>
     * 目前仅使用类级别代码片段作为上下文, 通过显式标记块的方式传给模型, 避免与目标代码混淆.
     * 如果上下文为空或仅包含空白, 则仅返回原始代码.
     *
     * @param context 上下文信息
     * @param code    当前元素代码
     * @return 合并后的代码片段, 供模板中的 %s 使用
     */
    @NotNull
    private static String mergeContextAndCode(@NotNull GenerationContext context,
                                              @NotNull String code) {
        String semanticContext = context.semanticContext();
        String classSnippet = context.classCodeSnippet();

        // 如果没有任何上下文，直接返回代码
        boolean hasSemanticContext = semanticContext != null && !semanticContext.isBlank();
        boolean hasClassSnippet = classSnippet != null && !classSnippet.isBlank();

        if (!hasSemanticContext && !hasClassSnippet) {
            return code;
        }

        // 构建完整的上下文和代码
        if (hasSemanticContext && hasClassSnippet) {
            // 同时有语义上下文和类代码上下文
            return """
                %s
                ### 类级上下文（仅供参考，不直接生成注释）
                <CLASS_CONTEXT_START>
                %s
                <CLASS_CONTEXT_END>

                ### 最终需要生成注释的代码片段
                %s
                """.formatted(semanticContext, classSnippet, code);
        } else if (hasSemanticContext) {
            // 只有语义上下文
            return """
                %s
                ### 最终需要生成注释的代码片段
                %s
                """.formatted(semanticContext, code);
        } else {
            // 只有类代码上下文
            return """
                ### 类级上下文（仅供参考，不直接生成注释）
                <CLASS_CONTEXT_START>
                %s
                <CLASS_CONTEXT_END>

                ### 最终需要生成注释的代码片段
                %s
                """.formatted(classSnippet, code);
        }
    }

    /**
     * 合并语义上下文、类代码上下文和代码到模板中
     * <p>
     * 该方法专门用于类注释生成，会将语义上下文、类代码上下文和代码合并为一个字符串，
     * 然后整体作为模板的 %s 占位符的值。
     *
     * @param template 类注释模板，包含 %s 占位符
     * @param context  上下文信息，包含语义上下文和类代码上下文
     * @param code     当前元素代码
     * @return 替换占位符后的完整提示词
     */
    @NotNull
    private static String mergeContextAndCode(@NotNull String template,
                                              @NotNull GenerationContext context,
                                              @NotNull String code) {
        // 合并语义上下文、类代码上下文和代码
        String codeWithContext = mergeContextAndCode(context, code);

        // 使用 %s 占位符替换代码
        return String.format(template, codeWithContext);
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


    /**
     * 根据配置设置和文档任务构建 AI 聊天请求对象
     * <p>
     * 该方法用于组合生成包含系统提示词, 用户提示词及预估令牌数的 AI 聊天请求对象.
     * 系统提示词和用户提示词均会根据语言设置进行占位符替换, 最终返回封装好的请求对象.
     *
     * @param userTemplate 用户提示词
     * @param defaultTemplate 用户提示词为空时的默认提示词
     * @param task     文档生成任务对象, 用于生成用户提示词模板
     * @return 构建完成的 AIChatRequest 对象, 包含系统提示, 用户提示和令牌预估数量
     * @since 1.0.0
     */
    private static String resolveClassTemplate(@NotNull DocumentationTask task,
                                               String userTemplate,
                                               String defaultTemplate) {
        String template = resolveTemplate(userTemplate, defaultTemplate);
        final PsiElement element = task.getElement();

        SettingsState settings = SettingsState.getInstance();
        final Map<String, String> customTagsMap = settings.customJavadocTags.stream()
            .collect(Collectors.toMap(
                CustomJavadocTag::getTagName,
                CustomJavadocTag::getDefaultValue,
                (existing, replacement) -> existing));

        template = template.replace("${author}", getAuthor(customTagsMap.get("author")));
        template = template.replace("${since}", ProjectVersionResolver.resolveVersion(element));

        String date = customTagsMap.get("date");
        if (date == null || date.isEmpty()) {
            date = "yyyy.MM.dd";
        }
        try {
            date = DateFormatUtils.format(new Date(), date);
        } catch (Exception e) {
            date = DateFormatUtils.format(new Date(), "yyyy.MM.dd");
        }

        template = template.replace("${date}", date);

        String email = customTagsMap.get("email");
        if (email == null || email.isEmpty()) {
            email = "mailto:dong4j@gmail.com";
        } else if (!email.startsWith("mailto:")) {
            email = "mailto:" + email;
        }

        template = template.replace("${email}", email);

        return template;
    }

    /**
     * 将作者信息放入参数映射中
     * <p>
     * 从系统属性中获取作者名称, 如果未设置且为类模板模式, 则使用默认作者名称, 并将作者信息存入参数映射中.
     *
     * @param author 传入的作者名称, 如果为 null 或空字符串, 则从系统属性中获取
     * @return 处理后的作者名称, 如果未设置则返回默认值 "zeka.stack.team"
     */
    private static String getAuthor(String author) {
        if (author == null || author.isEmpty()) {
            author = SystemUtils.getProperty("ZEKA_NAME_SPACE");
            return author == null || author.isEmpty() ? "zeka.stack.team" : author;
        }
        return author;
    }
}
