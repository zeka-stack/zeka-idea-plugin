package dev.dong4j.zeka.stack.idea.plugin.component;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.impl.FileTemplateConfigurable;
import com.intellij.ide.fileTemplates.impl.FileTemplateManagerImpl;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.dong4j.zeka.stack.idea.plugin.settings.CustomJavaDocTag;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import lombok.extern.slf4j.Slf4j;

/**
 * Javadoc 文件模板处理器
 * <p>
 * 用于在项目启动时自动配置文件模板，包括：
 * <ul>
 *   <li>在 Includes 中添加 "Java Class Header" 模板（如果启用）</li>
 *   <li>修改 "Java Class" 代码模板以使用该 Header 模板</li>
 *   <li>支持从设置中读取配置并动态生成模板</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public class JavaDocFileTemplatesHandler implements StartupActivity {
    /**
     * Java Class Header 模板名称
     */
    private static final String JAVA_CLASS_HEADER_TEMPLATE_NAME = "Java Class Header";
    private static final String JAVA_CLASS_TEMPLATE_NAME = "Javadoc Class";

    /**
     * Java Class 代码模板默认内容（用户禁用模板时的默认配置）
     * <p>
     * 这是 IntelliJ IDEA 默认的 Java Class 代码模板的 Javadoc 部分。
     * 用于恢复模板时的默认值。
     * <p>
     * 注意：这只是 Javadoc 部分，完整的 Java Class 模板应该还包含类定义等其他部分。
     * 但如果获取系统默认模板失败，会使用这个作为备用。
     */
    private static final String DEFAULT_JAVA_CLASS_JAVADOC_TEMPLATE = """
        #foreach($param in $RECORD_COMPONENTS)

         * @param $param

        #end
        #foreach($param in $TYPE_PARAMS)

         * @param <$param>

        #end
        """;

    /**
     * Java Class 代码模板内容（包含对 Java Class Header 模板的引用）
     */
    private static final String JAVA_CLASS_CODE_TEMPLATE = "#parse(\"Java Class Header.java\")\n";

    /**
     * 执行启动活动
     * <p>
     * 在项目启动时自动配置文件模板。
     *
     * @param project 项目对象
     */
    @Override
    public void runActivity(@NotNull Project project) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                SettingsState settings = SettingsState.getInstance();
                applyTemplateConfiguration(project, settings);
            } catch (Exception e) {
                log.error("配置文件模板失败", e);
            }
        });
    }

    /**
     * 应用模板配置
     * <p>
     * 根据设置状态应用或移除类 Javadoc 模板配置。
     *
     * @param project  项目对象
     * @param settings 设置状态
     */
    public static void applyTemplateConfiguration(@NotNull Project project, @NotNull SettingsState settings) {
        if (settings.enableClassJavaDocTemplate) {
            configureFileTemplates(project, settings);
        } else {
            cleanupFileTemplates(project);
        }
    }

    /**
     * 配置文件模板
     * <p>
     * 创建 "Java Class Header" Include 模板，并修改 "Java Class" 代码模板。
     *
     * @param project  项目对象
     * @param settings 设置状态
     */
    private static void configureFileTemplates(@NotNull Project project, @NotNull SettingsState settings) {
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        // 1. 生成模板内容（使用自定义标签中的 author、date、email）
        String templateContent = generateTemplateContent(settings);

        // 2. 创建或更新 "Java Class Header" Include 模板
        createOrUpdateIncludeTemplate(templateManager, project, templateContent);

        // 3. 修改 "Java Class" 代码模板
        updateJavaClassTemplate(templateManager, project);
    }

    /**
     * 生成模板内容
     * <p>
     * 根据设置中的模板内容和自定义标签生成最终的模板内容。
     * 将模板中的 ${author}、${date}、${email} 替换为自定义标签中的实际值。
     * 保留其他模板变量（如 ${description}、${YEAR} 等）不变。
     *
     * @param settings 设置状态
     * @return 生成的模板内容
     */
    private static String generateTemplateContent(@NotNull SettingsState settings) {
        // 获取自定义标签映射（标签名转小写以便匹配）
        Map<String, String> tagMap = new HashMap<>();
        for (CustomJavaDocTag tag : settings.customJavaDocTags) {
            tagMap.put(tag.getTagName().toLowerCase(), tag.getDefaultValue());
        }

        // 从设置中获取模板内容
        String template = settings.classJavaDocTemplate;

        // 替换 ${author}：从自定义标签中获取，如果没有则使用 "zeka.stack.team"
        String author = tagMap.getOrDefault("author", "zeka.stack.team");
        template = template.replace("${author}", author);

        // 替换 ${email}：从自定义标签中获取，如果没有则使用 "mailto:zeka.stack@gmail.com"
        String email = tagMap.getOrDefault("email", "mailto:zeka.stack@gmail.com");
        template = template.replace("${email}", email);

        // 对于 ${date}，自定义标签中的 date 通常是格式字符串（如 "yyyy.MM.dd"），
        // 但模板中需要的是实际日期值，所以保留模板变量 ${YEAR}.${MONTH}.${DAY} 等
        // 如果用户设置了 date 标签，可以考虑在模板中使用它，但目前模板中直接使用 ${YEAR} 等变量更合适
        // 这里不做替换，保留模板中的原始变量

        return template;
    }

    /**
     * 创建或更新 "Java Class Header" Include 模板
     *
     * @param templateManager 模板管理器
     * @param project         项目对象
     * @param templateContent 模板内容
     */
    private static void createOrUpdateIncludeTemplate(@NotNull FileTemplateManager templateManager,
                                                      @NotNull Project project,
                                                      @NotNull String templateContent) {
        // 获取现有的 Include 模板列表
        FileTemplate[] existingTemplates = templateManager.getTemplates(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY);

        // 检查是否已存在 "Java Class Header" 模板
        FileTemplate existingHeaderTemplate = null;
        for (FileTemplate template : existingTemplates) {
            if (JAVA_CLASS_HEADER_TEMPLATE_NAME.equals(template.getName())) {
                existingHeaderTemplate = template;
                break;
            }
        }

        // 如果已存在，检查内容是否需要更新；否则创建新模板
        if (existingHeaderTemplate != null) {
            // 检查内容是否需要更新
            String currentText = existingHeaderTemplate.getText();
            if (!currentText.trim().equals(templateContent.trim())) {
                // 删除旧模板并创建新模板（因为 FileTemplate 没有直接的 setText 方法）
                templateManager.removeTemplate(existingHeaderTemplate);
                // 重新获取模板列表（删除后）
                FileTemplate[] updatedTemplates = templateManager.getTemplates(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY);
                createNewIncludeTemplate(templateManager, project, updatedTemplates, templateContent);
                log.info("已更新文件模板: {}", JAVA_CLASS_HEADER_TEMPLATE_NAME);
            } else {
                log.debug("文件模板已是最新: {}", JAVA_CLASS_HEADER_TEMPLATE_NAME);
            }
        } else {
            // 创建新模板
            createNewIncludeTemplate(templateManager, project, existingTemplates, templateContent);
            log.info("已创建文件模板: {}", JAVA_CLASS_HEADER_TEMPLATE_NAME);
        }
    }

    /**
     * 创建新的 Include 模板
     *
     * @param templateManager   模板管理器
     * @param project           项目对象
     * @param existingTemplates 现有的模板列表
     * @param templateContent   模板内容
     */
    private static void createNewIncludeTemplate(@NotNull FileTemplateManager templateManager,
                                                 @NotNull Project project,
                                                 @NotNull FileTemplate[] existingTemplates,
                                                 @NotNull String templateContent) {
        // 创建新模板
        FileTemplate newTemplate = FileTemplateUtil.createTemplate(
            JAVA_CLASS_HEADER_TEMPLATE_NAME,
            JavaFileType.DEFAULT_EXTENSION,
            templateContent,
            new FileTemplate[0]
                                                                  );

        // 使用 FileTemplateConfigurable 设置模板（参考 uniform-format 的实现）
        FileTemplateConfigurable configurable = new FileTemplateConfigurable(project);
        FileTemplateManagerImpl templateManagerImpl = FileTemplateManagerImpl.getInstanceImpl(project);
        configurable.setTemplate(newTemplate, templateManagerImpl.getDefaultTemplateDescription());

        // 添加到 Include 模板列表
        List<FileTemplate> includeTemplates = new ArrayList<>(List.of(existingTemplates));
        includeTemplates.add(newTemplate);
        templateManager.setTemplates(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY, includeTemplates);
    }

    /**
     * 更新 "Java Class" 代码模板
     * <p>
     * 将 "Java Class" 代码模板的内容修改为包含对 "Java Class Header.java" 模板的引用。
     *
     * @param templateManager 模板管理器
     * @param project         项目对象
     */
    private static void updateJavaClassTemplate(@NotNull FileTemplateManager templateManager,
                                                @NotNull Project project) {
        // 获取 "Java Class" 代码模板
        FileTemplate javaClassTemplate = templateManager.getCodeTemplate(JAVA_CLASS_TEMPLATE_NAME);

        // 检查模板内容是否已经包含我们需要的配置
        String currentText = javaClassTemplate.getText();
        if (currentText.trim().equals(JAVA_CLASS_CODE_TEMPLATE.trim())) {
            log.debug("'{}' 模板已包含所需配置", JAVA_CLASS_TEMPLATE_NAME);
            return;
        }

        // 删除旧模板
        templateManager.removeTemplate(javaClassTemplate);

        // 创建新模板
        FileTemplate newTemplate = FileTemplateUtil.createTemplate(
            JAVA_CLASS_TEMPLATE_NAME,
            JavaFileType.DEFAULT_EXTENSION,
            JAVA_CLASS_CODE_TEMPLATE,
            new FileTemplate[0]
                                                                  );

        // 使用 FileTemplateConfigurable 设置模板
        FileTemplateConfigurable configurable = new FileTemplateConfigurable(project);
        FileTemplateManagerImpl templateManagerImpl = FileTemplateManagerImpl.getInstanceImpl(project);
        configurable.setTemplate(newTemplate, templateManagerImpl.getDefaultTemplateDescription());

        // 重新获取 Code 模板列表（删除后）
        FileTemplate[] remainingTemplates = templateManager.getTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY);
        List<FileTemplate> codeTemplates = new ArrayList<>(List.of(remainingTemplates));
        codeTemplates.add(newTemplate);
        templateManager.setTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY, codeTemplates);

        log.info("已更新 '{}' 代码模板", JAVA_CLASS_TEMPLATE_NAME);
    }

    /**
     * 清理文件模板
     * <p>
     * 当用户禁用类 Javadoc 模板时，删除 "Java Class Header" Include 模板，
     * 并恢复 "Java Class" 代码模板为默认配置。
     *
     * @param project 项目对象
     */
    private static void cleanupFileTemplates(@NotNull Project project) {
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);
        FileTemplateManagerImpl templateManagerImpl = FileTemplateManagerImpl.getInstanceImpl(project);

        // 1. 删除 "Java Class Header" Include 模板
        FileTemplate[] existingTemplates = templateManager.getTemplates(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY);
        FileTemplate headerTemplate = null;
        for (FileTemplate template : existingTemplates) {
            if (JAVA_CLASS_HEADER_TEMPLATE_NAME.equals(template.getName())) {
                headerTemplate = template;
                break;
            }
        }

        if (headerTemplate != null) {
            templateManager.removeTemplate(headerTemplate);
            log.info("已删除文件模板: {}", JAVA_CLASS_HEADER_TEMPLATE_NAME);
        }

        // 2. 恢复 "Java Class" 代码模板为默认配置
        try {
            // 获取默认的 Java Class 模板
            FileTemplate defaultJavaClassTemplate = templateManagerImpl.getDefaultTemplate(JAVA_CLASS_TEMPLATE_NAME);
            String defaultTemplateText = defaultJavaClassTemplate.getText();

            // 获取当前的 Java Class 模板
            FileTemplate javaClassTemplate = templateManager.getCodeTemplate(JAVA_CLASS_TEMPLATE_NAME);
            String currentText = javaClassTemplate.getText();

            // 检查是否需要恢复（如果当前模板是我们设置的模板，则需要恢复）
            if (currentText.trim().equals(JAVA_CLASS_CODE_TEMPLATE.trim())) {
                // 删除当前模板
                templateManager.removeTemplate(javaClassTemplate);

                // 创建默认模板
                FileTemplate restoredTemplate = FileTemplateUtil.createTemplate(
                    JAVA_CLASS_TEMPLATE_NAME,
                    JavaFileType.DEFAULT_EXTENSION,
                    defaultTemplateText,
                    new FileTemplate[0]
                                                                               );

                // 使用 FileTemplateConfigurable 设置模板
                FileTemplateConfigurable configurable = new FileTemplateConfigurable(project);
                configurable.setTemplate(restoredTemplate, templateManagerImpl.getDefaultTemplateDescription());

                // 重新获取 Code 模板列表（删除后）
                FileTemplate[] remainingTemplates = templateManager.getTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY);
                List<FileTemplate> codeTemplates = new ArrayList<>(List.of(remainingTemplates));
                codeTemplates.add(restoredTemplate);
                templateManager.setTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY, codeTemplates);

                log.info("已恢复 '{}' 代码模板为默认配置", JAVA_CLASS_TEMPLATE_NAME);
            } else {
                log.debug("'{}' 模板不是我们设置的模板，无需恢复", JAVA_CLASS_TEMPLATE_NAME);
            }
        } catch (Exception e) {
            log.warn("恢复 '{}' 代码模板失败，尝试使用硬编码的默认模板", JAVA_CLASS_TEMPLATE_NAME, e);
            // 如果获取默认模板失败，使用硬编码的默认模板
            restoreJavaClassTemplateWithHardcodedDefault(templateManager, project);
        }
    }

    /**
     * 使用硬编码的默认模板恢复 Java Class 模板
     * <p>
     * 当无法获取系统默认模板时使用的备用方案。
     *
     * @param templateManager 模板管理器
     * @param project         项目对象
     */
    private static void restoreJavaClassTemplateWithHardcodedDefault(@NotNull FileTemplateManager templateManager,
                                                                     @NotNull Project project) {
        FileTemplate javaClassTemplate = templateManager.getCodeTemplate(JAVA_CLASS_TEMPLATE_NAME);
        String currentText = javaClassTemplate.getText();
        if (currentText.trim().equals(JAVA_CLASS_CODE_TEMPLATE.trim())) {
            templateManager.removeTemplate(javaClassTemplate);

            FileTemplate defaultTemplate = FileTemplateUtil.createTemplate(
                JAVA_CLASS_TEMPLATE_NAME,
                JavaFileType.DEFAULT_EXTENSION,
                DEFAULT_JAVA_CLASS_JAVADOC_TEMPLATE,
                new FileTemplate[0]
                                                                          );

            FileTemplateConfigurable configurable = new FileTemplateConfigurable(project);
            FileTemplateManagerImpl templateManagerImpl = FileTemplateManagerImpl.getInstanceImpl(project);
            configurable.setTemplate(defaultTemplate, templateManagerImpl.getDefaultTemplateDescription());

            FileTemplate[] remainingTemplates = templateManager.getTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY);
            List<FileTemplate> codeTemplates = new ArrayList<>(List.of(remainingTemplates));
            codeTemplates.add(defaultTemplate);
            templateManager.setTemplates(FileTemplateManager.CODE_TEMPLATES_CATEGORY, codeTemplates);

            log.info("已使用硬编码默认模板恢复 'Java Class' 代码模板");
        }
    }
}

