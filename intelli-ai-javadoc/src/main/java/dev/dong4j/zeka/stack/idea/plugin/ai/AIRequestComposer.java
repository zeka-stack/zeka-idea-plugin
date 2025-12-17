package dev.dong4j.zeka.stack.idea.plugin.ai;

import com.intellij.psi.PsiElement;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.settings.CommentLanguage;
import dev.dong4j.zeka.stack.idea.plugin.settings.CustomJavadocTag;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.GenerationContext;
import dev.dong4j.zeka.stack.idea.plugin.util.MavenUtil;
import dev.dong4j.zeka.stack.idea.plugin.util.TokenCounter;

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

        // 根据语言选择替换提示词中的语言占位符
        // 必须在所有其他占位符（如 ${author}、${date} 等）替换完成后进行
        CommentLanguage commentLanguage = settings.commentLanguage != null
                                          ? settings.commentLanguage : CommentLanguage.ZH;
        String languageText = commentLanguage.getDesc();

        // 替换系统提示词和用户提示词中的所有 ${commentLanguage} 占位符
        systemPrompt = replaceCommentLanguagePlaceholder(systemPrompt, languageText);
        userPrompt = replaceCommentLanguagePlaceholder(userPrompt, languageText);

        int tokenEstimate = TokenCounter.estimateTokens(systemPrompt) + TokenCounter.estimateTokens(userPrompt);
        return new AIChatRequest(systemPrompt, userPrompt, tokenEstimate);
    }

    /**
     * 替换提示词中的注释语言占位符
     * <p>
     * 将提示词中的所有 ${commentLanguage} 占位符替换为实际的语言文本。
     * 支持多次替换，确保所有占位符都被正确替换。
     *
     * @param prompt       包含占位符的提示词
     * @param languageText 要替换的语言文本（"中文" 或 "英文"）
     * @return 替换后的提示词
     */
    private static String replaceCommentLanguagePlaceholder(@NotNull String prompt, @NotNull String languageText) {
        return prompt.replace("${commentLanguage}", languageText);
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
     * 根据任务类型和设置构建用户提示模板
     * <p>
     * 使用指定的设置和任务类型, 选择对应的模板并填充任务代码内容, 生成最终的用户提示字符串.
     * 如果覆写模式是"fix"（仅修复错误注释），则使用修复错误 Javadoc 的提示词模板。
     *
     * @param settings 配置设置对象, 用于获取模板配置
     * @param task     文档生成任务对象, 包含任务类型和代码内容
     * @return 生成的用户提示字符串
     * @throws NullPointerException 如果 settings 或 task 为 null
     */
    private static String buildUserPrompt(@NotNull SettingsState settings, @NotNull DocumentationTask task) {
        // 如果覆写模式是"fix"（仅修复错误注释），使用修复错误 Javadoc 的提示词
        if (settings.overrideExisting && "fix".equals(settings.overrideMode)) {
            String template = resolveTemplate(settings.fixJavadocPromptTemplate,
                                              SettingsState.getDefaultFixJavadocPromptTemplate());
            String codeWithContext = mergeContextAndCode(task.getContext(), task.getCode());
            return String.format(template, codeWithContext);
        }

        // 否则使用正常的提示词模板
        String template = switch (task.getType()) {
            case CLASS, INTERFACE, ENUM -> resolveClassTemplate(task, settings.classPromptTemplate,
                                                                SettingsState.getDefaultClassPromptTemplate());
            case FIELD -> resolveTemplate(settings.fieldPromptTemplate,
                                          SettingsState.getDefaultFieldPromptTemplate());
            case TEST_METHOD -> resolveTemplate(settings.testPromptTemplate,
                                                SettingsState.getDefaultTestPromptTemplate());
            default -> resolveTemplate(settings.methodPromptTemplate,
                                       SettingsState.getDefaultMethodPromptTemplate());
        };
        String codeWithContext = mergeContextAndCode(task.getContext(), task.getCode());
        return String.format(template, codeWithContext);
    }

    /**
     * 将上下文信息合并到待处理代码中, 作为 user 提示的一部分。
     * <p>
     * 目前仅使用类级别代码片段作为上下文, 通过显式标记块的方式传给模型, 避免与目标代码混淆。
     * 如果上下文为空或仅包含空白, 则仅返回原始代码。
     *
     * @param context 上下文信息
     * @param code    当前元素代码
     * @return 合并后的代码片段, 供模板中的 %s 使用
     */
    @NotNull
    private static String mergeContextAndCode(@NotNull GenerationContext context,
                                              @NotNull String code) {
        String classSnippet = context.classCodeSnippet();
        if (classSnippet == null || classSnippet.isBlank()) {
            return code;
        }

        return """
            # 类级上下文（仅供参考，不直接生成注释）
            <CLASS_CONTEXT_START>
            %s
            <CLASS_CONTEXT_END>

            # 待处理的代码片段
            %s
            """.formatted(classSnippet, code);
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
     * 解析并替换类模板中的占位符
     * <p>
     * 根据给定的任务, 用户模板和默认模板, 解析模板内容并替换其中的占位符, 如作者, 版本, 日期和邮箱等信息.
     *
     * @param task            当前文档生成任务
     * @param userTemplate    用户自定义模板, 若为空则使用默认模板
     * @param defaultTemplate 默认模板
     * @return 替换占位符后的模板字符串
     */
    private static String resolveClassTemplate(@NotNull DocumentationTask task,
                                               String userTemplate, String defaultTemplate) {
        String template = resolveTemplate(userTemplate, defaultTemplate);
        final PsiElement element = task.getElement();

        SettingsState settings = SettingsState.getInstance();
        final Map<String, String> customTagsMap = settings.customJavadocTags.stream()
            .collect(Collectors.toMap(
                CustomJavadocTag::getTagName,
                CustomJavadocTag::getDefaultValue,
                (existing, replacement) -> existing));

        template = template.replace("${author}", MavenUtil.getAuthor(customTagsMap.get("author")));
        template = template.replace("${since}", MavenUtil.getVersion(element));

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
}
