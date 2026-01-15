package dev.dong4j.zeka.stack.idea.plugin.changelog.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * 项目版本号解析器
 * <p>
 * 支持从多种来源自动解析项目版本号，按优先级顺序尝试：
 * <ol>
 *   <li>Maven 项目（从 pom.xml 读取）</li>
 *   <li>Gradle 项目（从 build.gradle 或 build.gradle.kts 读取）</li>
 *   <li>Git 标签（从最新 Git 标签读取）</li>
 *   <li>默认值（"Unreleased"）</li>
 * </ol>
 * <p>
 * 所有解析方法都是可选的，如果失败会静默返回 null，不会影响插件正常使用。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class ProjectVersionResolver {
    /** 默认版本号 */
    private static final String DEFAULT_VERSION = "Unreleased";

    /** Maven version 标签正则表达式 */
    private static final Pattern MAVEN_VERSION_PATTERN = Pattern.compile(
        "<version>([^<]+)</version>",
        Pattern.CASE_INSENSITIVE
                                                                        );

    /** Gradle version 属性正则表达式（支持单引号、双引号、无引号） */
    private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile(
        "(?:^|\\s)(?:version\\s*[=:]\\s*['\"]?)([^'\"\\s,]+)(?:['\"]?)(?:\\s|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                                                                         );

    /** Gradle version 属性正则表达式（Kotlin DSL，支持字符串插值） */
    private static final Pattern GRADLE_KTS_VERSION_PATTERN = Pattern.compile(
        "(?:^|\\s)(?:version\\s*[=:]\\s*[\"'])([^\"']+)(?:[\"'])(?:\\s|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                                                                             );

    /**
     * 私有构造函数，工具类禁止实例化
     */
    private ProjectVersionResolver() {
        // 工具类，禁止实例化
    }

    /**
     * 解析项目版本号
     * <p>
     * 按优先级顺序尝试不同的解析方式，返回第一个成功的结果。
     * 如果所有方式都失败，返回默认版本号。
     *
     * @param project 项目对象，不能为 null
     * @return 版本号字符串，不会为 null
     */
    @NotNull
    public static String resolveVersion(@NotNull Project project) {
        if (project.isDisposed()) {
            return DEFAULT_VERSION;
        }

        // 1. 尝试从 Maven 项目读取
        String version = resolveFromMaven(project);
        if (version != null && !version.isEmpty()) {
            log.debug("从 Maven 项目解析版本号: {}", version);
            return normalizeVersion(version);
        }

        // 2. 尝试从 Gradle 项目读取
        version = resolveFromGradle(project);
        if (version != null && !version.isEmpty()) {
            log.debug("从 Gradle 项目解析版本号: {}", version);
            return normalizeVersion(version);
        }

        // 3. 尝试从 Git 标签读取
        version = resolveFromGitTag(project);
        if (version != null && !version.isEmpty()) {
            log.debug("从 Git 标签解析版本号: {}", version);
            return normalizeVersion(version);
        }

        // 4. 使用默认值
        log.debug("使用默认版本号: {}", DEFAULT_VERSION);
        return DEFAULT_VERSION;
    }

    /**
     * 从 Maven 项目解析版本号
     * <p>
     * 优先使用 IntelliJ Maven 插件 API，如果不可用则直接解析 pom.xml 文件。
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败则返回 null
     */
    @Nullable
    private static String resolveFromMaven(@NotNull Project project) {
        // 方式一：使用 Maven 插件 API（如果可用）
        String version = resolveFromMavenPlugin(project);
        if (version != null && !version.isEmpty()) {
            return version;
        }

        // 方式二：直接解析 pom.xml 文件
        return resolveFromPomXml(project);
    }

    /**
     * 使用 IntelliJ Maven 插件 API 解析版本号
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败或插件不可用则返回 null
     */
    @Nullable
    private static String resolveFromMavenPlugin(@NotNull Project project) {
        try {
            // 检查 Maven 插件是否可用
            Class<?> mavenProjectsManagerClass = Class.forName(
                "org.jetbrains.idea.maven.project.MavenProjectsManager"
                                                              );
            Object mavenProjectsManager = mavenProjectsManagerClass
                .getMethod("getInstance", Project.class)
                .invoke(null, project);

            if (mavenProjectsManager == null) {
                return null;
            }

            // 获取根项目
            Object rootProject = mavenProjectsManagerClass
                .getMethod("getRootProjects")
                .invoke(mavenProjectsManager);
            if (rootProject == null) {
                return null;
            }

            // 获取第一个根项目的版本号
            if (rootProject instanceof List<?> rootProjects && !rootProjects.isEmpty()) {
                Object firstProject = rootProjects.get(0);
                if (firstProject != null) {
                    Object version = firstProject.getClass()
                        .getMethod("getVersion")
                        .invoke(firstProject);
                    if (version != null) {
                        return version.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Maven 插件不可用或解析失败，静默处理
            log.debug("Maven 插件 API 不可用或解析失败", e);
        }
        return null;
    }

    /**
     * 直接解析 pom.xml 文件获取版本号
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败则返回 null
     */
    @Nullable
    private static String resolveFromPomXml(@NotNull Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        VirtualFile pomFile = baseDir.findChild("pom.xml");
        if (pomFile == null || !pomFile.exists()) {
            return null;
        }

        try {
            String content = new String(pomFile.contentsToByteArray());
            Matcher matcher = MAVEN_VERSION_PATTERN.matcher(content);
            if (matcher.find()) {
                String version = matcher.group(1).trim();
                // 跳过父 POM 的 version 标签（通常在 <parent> 标签内）
                // 简单处理：查找第一个不在 <parent> 标签内的 <version>
                int versionIndex = matcher.start();
                int parentStart = content.lastIndexOf("<parent>", versionIndex);
                int parentEnd = content.lastIndexOf("</parent>", versionIndex);
                if (parentStart < 0 || (parentEnd > 0 && parentEnd > parentStart)) {
                    // 不在 <parent> 标签内，返回这个版本号
                    return version;
                }
                // 在 <parent> 标签内，继续查找下一个
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
        } catch (Exception e) {
            log.debug("解析 pom.xml 失败", e);
        }
        return null;
    }

    /**
     * 从 Gradle 项目解析版本号
     * <p>
     * 优先使用 IntelliJ Gradle 插件 API，如果不可用则直接解析 build.gradle 文件。
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败则返回 null
     */
    @Nullable
    private static String resolveFromGradle(@NotNull Project project) {
        // 方式一：使用 Gradle 插件 API（如果可用）
        String version = resolveFromGradlePlugin(project);
        if (version != null && !version.isEmpty()) {
            return version;
        }

        // 方式二：直接解析 build.gradle 或 build.gradle.kts 文件
        return resolveFromGradleFile(project);
    }

    /**
     * 使用 IntelliJ Gradle 插件 API 解析版本号
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败或插件不可用则返回 null
     */
    @Nullable
    private static String resolveFromGradlePlugin(@NotNull Project project) {
        try {
            // 检查 Gradle 插件是否可用
            Class<?> externalSystemManagerClass = Class.forName(
                "com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager"
                                                               );
            Object externalProjectsManager = externalSystemManagerClass
                .getMethod("getInstance", Project.class)
                .invoke(null, project);

            if (externalProjectsManager == null) {
                return null;
            }

            // 获取 Gradle 项目设置
            Class<?> projectDataManagerClass = Class.forName(
                "com.intellij.openapi.externalSystem.service.project.ProjectDataManager"
                                                            );
            Object projectDataManager = projectDataManagerClass
                .getMethod("getInstance")
                .invoke(null);

            if (projectDataManager == null) {
                return null;
            }

            // 获取项目数据
            Object projectData = projectDataManagerClass
                .getMethod("getExternalProjectData", Project.class, String.class)
                .invoke(projectDataManager, project, "GRADLE");

            if (projectData != null) {
                // 尝试获取版本号
                Object version = projectData.getClass()
                    .getMethod("getVersion")
                    .invoke(projectData);
                if (version != null) {
                    return version.toString();
                }
            }
        } catch (Exception e) {
            // Gradle 插件不可用或解析失败，静默处理
            log.debug("Gradle 插件 API 不可用或解析失败", e);
        }
        return null;
    }

    /**
     * 直接解析 build.gradle 或 build.gradle.kts 文件获取版本号
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败则返回 null
     */
    @Nullable
    private static String resolveFromGradleFile(@NotNull Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        // 优先查找 build.gradle.kts，其次 build.gradle
        VirtualFile gradleFile = baseDir.findChild("build.gradle.kts");
        if (gradleFile == null || !gradleFile.exists()) {
            gradleFile = baseDir.findChild("build.gradle");
        }
        if (gradleFile == null || !gradleFile.exists()) {
            return null;
        }

        try {
            String content = new String(gradleFile.contentsToByteArray());
            Pattern pattern = gradleFile.getName().endsWith(".kts")
                              ? GRADLE_KTS_VERSION_PATTERN
                              : GRADLE_VERSION_PATTERN;
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("解析 build.gradle 失败", e);
        }
        return null;
    }

    /**
     * 从 Git 标签解析版本号
     * <p>
     * 获取最新的 Git 标签作为版本号。
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败则返回 null
     */
    @Nullable
    private static String resolveFromGitTag(@NotNull Project project) {
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) {
            return null;
        }

        Path gitDir = Paths.get(baseDir.getPath(), ".git");
        if (!Files.exists(gitDir)) {
            return null;
        }

        try (Repository repository = new FileRepositoryBuilder()
            .setGitDir(gitDir.toFile())
            .build()) {

            // 获取所有标签
            List<org.eclipse.jgit.lib.Ref> tags = repository.getRefDatabase()
                .getRefsByPrefix("refs/tags/");

            if (tags.isEmpty()) {
                return null;
            }

            // 按提交时间排序，获取最新的标签
            try (org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
                return tags.stream()
                    .max(Comparator.comparing(ref -> {
                        try {
                            org.eclipse.jgit.revwalk.RevObject object = walk.parseAny(ref.getObjectId());
                            if (object instanceof org.eclipse.jgit.revwalk.RevTag) {
                                object = walk.parseAny(((org.eclipse.jgit.revwalk.RevTag) object).getObject());
                            }
                            if (object instanceof org.eclipse.jgit.revwalk.RevCommit) {
                                return ((org.eclipse.jgit.revwalk.RevCommit) object).getCommitTime();
                            }
                        } catch (Exception e) {
                            log.debug("解析 Git 标签时间失败", e);
                        }
                        return 0;
                    }))
                    .map(ref -> {
                        String tagName = org.eclipse.jgit.lib.Repository.shortenRefName(ref.getName());
                        // 去除 'v' 前缀（如果存在）
                        return tagName.startsWith("v") ? tagName.substring(1) : tagName;
                    })
                    .orElse(null);
            }
        } catch (Exception e) {
            log.debug("从 Git 标签解析版本号失败", e);
        }
        return null;
    }

    /**
     * 规范化版本号
     * <p>
     * 去除版本号中的 'v' 前缀，并处理其他格式问题。
     *
     * @param version 原始版本号
     * @return 规范化后的版本号
     */
    @NotNull
    private static String normalizeVersion(@NotNull String version) {
        String normalized = version.trim();
        // 去除 'v' 前缀（如果存在）
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
