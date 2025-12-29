package dev.dong4j.zeka.stack.idea.plugin.template.file;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.ide.fileTemplates.impl.FileTemplateConfigurable;
import com.intellij.ide.fileTemplates.impl.FileTemplateManagerImpl;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;

/**
 * 统一文件模板处理器
 * <p>
 * 用于统一处理项目中的文件模板配置，确保新建文件时包含公司版权信息和默认模板。
 * 主要功能包括检查默认文件头模板是否包含公司名称，若不包含则进行替换或添加。
 * 支持自动获取当前用户名，并用于生成文件模板的注释信息。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
@Slf4j
public class UniformFileTemplatesHandler implements StartupActivity {
    /** 请求头模板，用于生成包含描述、作者、版本、邮箱、日期和版本信息的注释 */
    public static final String HEADER = """
        /**
         * ${description}
         *
         * @author %s
         * @version %s
         * @email "mailto:%s@163.com"
         * @date ${YEAR}.${MONTH}.${DAY} ${HOUR}:${MINUTE}
         * @since %s
         */""";

    /**
     * 执行活动操作，用于处理项目中的文件模板配置
     * <p>
     * 该方法检查默认文件头模板是否包含指定公司名称，若不包含则移除原模板并创建新的模板，设置到项目配置中。
     *
     * @param project 项目对象，用于获取模板管理器和配置信息
     */
    @Override
    public void runActivity(@NotNull Project project) {
        FileTemplateManager templateManager = FileTemplateManager.getInstance(project);

        FileTemplate defaultTemplate =
            FileTemplateManager.getInstance(project).getDefaultTemplate(FileTemplateManager.FILE_HEADER_TEMPLATE_NAME);

        if (!defaultTemplate.getText().trim().contains("Zeka.Stack")) {
            templateManager.removeTemplate(defaultTemplate);

            String author = getCurrentUserName();
            String version = "1.0.0";

            FileTemplate template = FileTemplateUtil.createTemplate(FileTemplateManager.FILE_HEADER_TEMPLATE_NAME,
                                                                    JavaFileType.DEFAULT_EXTENSION,
                                                                    String.format(HEADER, author, version, author, version),
                                                                    new FileTemplate[0]);

            FileTemplateConfigurable configurable = new FileTemplateConfigurable(project);
            configurable.setTemplate(template, FileTemplateManagerImpl.getInstanceImpl(project).getDefaultTemplateDescription());
            templateManager.setTemplates(FileTemplateManager.INCLUDES_TEMPLATES_CATEGORY, Collections.singletonList(template));
        }

    }

    /**
     * 获取当前用户名
     * <p>
     * 优先从标准 Java 属性中获取用户名，若获取不到则尝试从环境变量中获取。
     * 若所有方式均失败，则返回 "unknown"。
     *
     * @return 当前用户名，若无法获取则返回 "unknown"
     */
    public static String getCurrentUserName() {
        // 首选标准 Java 属性（跨平台）
        String username = System.getProperty("user.name");

        // 兜底方案：尝试从环境变量中获取
        if (username == null || username.isEmpty()) {
            username = System.getenv("USER");      // macOS / Linux
            if (username == null || username.isEmpty()) {
                username = System.getenv("USERNAME"); // Windows
            }
        }

        // 最后兜底
        return username != null ? username : "unknown";
    }

}
