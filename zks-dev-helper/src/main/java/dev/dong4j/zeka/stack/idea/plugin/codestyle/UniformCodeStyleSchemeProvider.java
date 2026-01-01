package dev.dong4j.zeka.stack.idea.plugin.codestyle;

import com.intellij.openapi.options.SchemeImportException;
import com.intellij.openapi.options.SchemeImportUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemeImpl;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemesImpl;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码风格方案提供者
 * <p>
 * 该类用于为项目提供统一的代码风格方案，包括方案的导入、设置以及状态检查等功能。它通过读取预定义的配置文件，创建并应用统一的代码风格设置，确保项目中代码风格的一致性。
 * 这是 ZKS Dev Helper 插件中代码样式模块的核心组件。
 * <p>
 * 主要功能包括：
 * - 导入统一代码风格配置文件并应用到当前项目
 * - 检查当前项目是否已应用统一代码风格方案
 * - 提供统一的代码风格名称和配置文件路径
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
@SuppressWarnings("D")
@Slf4j
public class UniformCodeStyleSchemeProvider {
    /** 代码风格的配置名称，用于标识启用代码风格的配置项 */
    public static final String CODE_STYLE_NAME = "zeka-stack-codestyle";
    /** 代码风格配置文件路径，由代码风格名称加上 ".xml" 后缀组成 */
    private static final String CODE_STYLE_FILE = CODE_STYLE_NAME + ".xml";

    /**
     * 为指定项目设置代码风格方案
     * <p>
     * 该方法会检查是否存在名为 CODE_STYLE_NAME 的代码风格方案，若已存在则设置为当前方案；
     * 若不存在，则优先从本地下载的文件加载，如果本地文件不存在，则从插件资源文件中加载代码风格配置文件，
     * 并导入到项目中，最后设置为默认方案。
     * <p>
     * 注意：该方法会确保在 EDT 中执行 WriteAction，符合 IntelliJ 平台线程模型要求。
     *
     * @param project 要设置代码风格的项目对象
     */
    public static void provideUniformCodeStyleScheme(@Nullable Project project) {
        // WriteAction 必须在 EDT 中执行，如果当前不在 EDT，先切换到 EDT
        try {
            CodeStyleSchemes codeStyleSchemes = CodeStyleSchemes.getInstance();

            // 检查是否已经存在该方案
            CodeStyleScheme existingScheme = codeStyleSchemes.findSchemeByName(CODE_STYLE_NAME);
            if (existingScheme != null) {
                log.info("Code style '{}' already exists, setting as current", CODE_STYLE_NAME);
                codeStyleSchemes.setCurrentScheme(existingScheme);
                return;
            }

            // 获取配置，判断是否使用全局方案
            boolean useGlobalScheme = false;
            CodeStyleSettingsState settings =
                CodeStyleSettingsState.getInstance();
            if (settings.getCodeStyleUpdateSettings() != null) {
                useGlobalScheme = settings.getCodeStyleUpdateSettings().isUseGlobalScheme();
            }

            // 1. 优先检查本地下载的文件
            Path localFile = CodeStyleDownloadManager.getLocalCodeStyleFile();
            if (localFile != null && Files.exists(localFile)) {
                VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(localFile.toString());
                if (vFile != null) {
                    importScheme(project, vFile, useGlobalScheme);
                    log.info("Code style '{}' loaded from local file: {}", CODE_STYLE_NAME, localFile);
                    return;
                }
            }

            // 2. 回退到插件内置的资源文件
            URL resource = UniformCodeStyleSchemeProvider.class.getClassLoader().getResource(CODE_STYLE_FILE);
            if (resource != null) {
                VirtualFile vFile = VfsUtil.findFileByURL(resource);
                if (vFile != null) {
                    importScheme(project, vFile, useGlobalScheme);
                    log.info("Code style '{}' loaded from plugin resource", CODE_STYLE_NAME);
                    return;
                }
            }

            log.error("Code style file not found: {}", CODE_STYLE_FILE);

        } catch (Exception e) {
            log.error("Failed to provide uniform code style scheme", e);
        }
    }

    /**
     * 导入代码样式方案
     * <p>
     * 从指定的文件中加载代码样式配置，并创建新的代码样式方案，将其添加到方案列表中，并设置为当前方案。
     *
     * @param project         当前项目对象，可以为 null（在设置页面中）
     * @param selectedFile    选择的文件对象，用于加载样式配置
     * @param useGlobalScheme 是否设置为全局方案（IDE级别），true 表示添加到 IDE 级别，false 表示仅项目级别
     * @throws SchemeImportException 如果导入样式方案过程中发生异常
     */
    private static void importScheme(@Nullable Project project,
                                     @NotNull VirtualFile selectedFile,
                                     boolean useGlobalScheme) throws SchemeImportException {
        Element rootElement = SchemeImportUtil.loadSchemeDom(selectedFile);
        Element schemeRoot = findSchemeRoot(rootElement);

        CodeStyleScheme derivedScheme = CodeStyleSchemes
            .getInstance()
            .createNewScheme(CODE_STYLE_NAME, null);

        readSchemeFromDom(schemeRoot, derivedScheme);

        // 将方案添加到 IDE 级别（方案必须存在才能被使用）
        CodeStyleSchemes.getInstance().addScheme(derivedScheme);

        if (useGlobalScheme) {
            // 设置为 IDE 的当前方案（全局方案，所有项目默认使用）
            CodeStyleSchemesImpl.getSchemeManager().setCurrent(derivedScheme);
            log.info("Code style '{}' added as global scheme (IDE level, default for all projects)", CODE_STYLE_NAME);
        } else {
            // 不设置为全局当前方案，仅项目级别使用
            log.info("Code style '{}' added to IDE level but not set as global default (project-level only)", CODE_STYLE_NAME);
        }

        // 设置为项目的首选代码样式（项目级别）
        if (project != null) {
            CodeStyleSettingsManager.getInstance(project).PREFERRED_PROJECT_CODE_STYLE = derivedScheme.getName();
        }
    }

    /**
     * 查找方案根元素
     * <p>
     * 根据传入的根元素查找方案的根节点。根据不同的根元素名称（如"project"或"component"）执行不同的查找逻辑，最终返回找到的根元素。
     *
     * @param rootElement 根元素对象
     * @return 找到的方案根元素
     * @throws SchemeImportException 当无法找到有效的方案根元素时抛出异常
     */
    @NotNull
    private static Element findSchemeRoot(@NotNull Element rootElement) throws SchemeImportException {
        String rootName = rootElement.getName();

        // Project code style 172.x and earlier
        if ("project".equals(rootName)) {
            Element child = rootElement.getChild("component");
            if (child != null && "ProjectCodeStyleSettingsManager".equals(child.getAttributeValue("name"))) {
                child = child.getChild("option");
                if (child != null && "PER_PROJECT_SETTINGS".equals(child.getAttributeValue("name"))) {
                    child = child.getChild("value");
                    if (child != null) {
                        return child;
                    }
                }
            }
            throw new SchemeImportException("Invalid scheme root: " + rootName);
        }
        // Project code style 173.x and later
        else if ("component".equals(rootName)) {
            if ("ProjectCodeStyleConfiguration".equals(rootElement.getAttributeValue("name"))) {
                Element child = rootElement.getChild("code_scheme");
                if (child != null) {
                    return child;
                }
            }
            throw new SchemeImportException("Invalid scheme root: " + rootName);
        }
        return rootElement;
    }

    /**
     * 从 DOM 元素中读取代码风格方案的配置信息
     * <p>
     * 该方法用于解析传入的 DOM 元素，加载代码风格设置，并更新指定的代码风格方案。
     *
     * @param rootElement DOM 根元素，包含方案的配置信息
     * @param scheme      要更新的代码风格方案对象
     * @throws SchemeImportException 如果在读取过程中发生异常
     */
    private static void readSchemeFromDom(@NotNull Element rootElement, @NotNull CodeStyleScheme scheme)
        throws SchemeImportException {
        CodeStyleSettings newSettings = new CodeStyleSettings();
        loadSettings(rootElement, newSettings);
        newSettings.resetDeprecatedFields();
        ((CodeStyleSchemeImpl) scheme).setCodeStyleSettings(newSettings);
    }

    /**
     * 加载代码样式设置
     * <p>
     * 从给定的根元素中读取代码样式设置，并应用到指定的设置对象中。
     *
     * @param rootElement 根元素，用于读取设置数据
     * @param settings    目标设置对象，用于存储读取的配置
     * @throws SchemeImportException 如果加载设置过程中发生异常
     */
    private static void loadSettings(@NotNull Element rootElement, @NotNull CodeStyleSettings settings) throws SchemeImportException {
        try {
            settings.readExternal(findSchemeRoot(rootElement));
        } catch (InvalidDataException e) {
            throw new SchemeImportException("Failed to load code style settings: " + e.getMessage());
        }
    }

    /**
     * 检查是否已提供代码风格方案
     * <p>
     * 该方法用于判断当前项目中是否已经配置了名为 CODE_STYLE_NAME 的代码风格方案，并且该方案是否为当前默认方案。
     *
     * @param project 项目对象，用于获取代码风格方案信息
     * @return 如果存在且为当前默认方案，返回 true；否则返回 false
     */
    public static boolean isUniformCodeStyleSchemeProvided(Project project) {
        try {
            CodeStyleSchemes codeStyleSchemes = CodeStyleSchemes.getInstance();
            CodeStyleScheme scheme = codeStyleSchemes.findSchemeByName(CODE_STYLE_NAME);

            if (scheme != null) {
                // 检查是否为当前默认方案
                CodeStyleScheme currentScheme = codeStyleSchemes.getCurrentScheme();
                return currentScheme != null && CODE_STYLE_NAME.equals(currentScheme.getName());
            }

            return false;

        } catch (Exception e) {
            log.warn("Failed to check code style scheme status", e);
            return false;
        }
    }
}
