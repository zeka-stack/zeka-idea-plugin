package dev.dong4j.zeka.stack.idea.javadoc.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.concurrent.ConcurrentHashMap;
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
 * <p>
 * 详细说明:
 * <ul>
 *   <li>Maven 项目优先使用 Maven 插件 API 读取版本号, 这种方式支持多模块与继承.</li>
 *   <li>如果 Maven 项目未链接 (未导入为 Maven), 则只能兜底解析 pom.xml, 该方式属于硬编码,
 *   对父子 POM 继承场景可能不准确.</li>
 *   <li>当 Maven 两种方式均失败时, 尝试 Gradle: 若项目已链接为 Gradle, 则通过外部系统 API 获取版本号;
 *   否则解析 build.gradle / build.gradle.kts 获取版本号, 同样存在不准确的情况.</li>
 *   <li>当通过解析 pom.xml 或 build.gradle 获取版本号时, 只在当前模块范围内查找文件,
 *   查找过程遇到 src/main 即停止, 避免在一个 IDE 中同时存在多个 Maven/Gradle 模块时误匹配.</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
public final class ProjectVersionResolver {
    /** 默认版本号 */
    private static final String DEFAULT_VERSION = "x.x.x";

    /** Maven version 标签正则表达式 */
    private static final Pattern MAVEN_VERSION_PATTERN = Pattern.compile(
        "<version>([^<]+)</version>",
        Pattern.CASE_INSENSITIVE
                                                                        );

    /** Gradle version 属性正则表达式（支持单引号、双引号、无引号） */
    private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile(
        "(?:^|\\s)version\\s*[=:]\\s*['\"]?([^'\"\\s,]+)['\"]?(?:\\s|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                                                                         );

    /** Gradle version 属性正则表达式（Kotlin DSL，支持字符串插值） */
    private static final Pattern GRADLE_KTS_VERSION_PATTERN = Pattern.compile(
        "(?:^|\\s)version\\s*[=:]\\s*[\"']([^\"']+)[\"'](?:\\s|$)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                                                                             );

    /**
     * 版本号缓存
     * <p>
     * Key: Project 对象
     * Value: VersionCacheEntry 缓存条目
     */
    private static final ConcurrentHashMap<Project, VersionCacheEntry> VERSION_CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，工具类禁止实例化
     */
    private ProjectVersionResolver() {
        // 工具类，禁止实例化
    }

    /**
     * 版本缓存条目记录类
     * <p>
     * 用于存储版本信息, 对应文件和最后修改时间, 以便进行简单的缓存失效判断.
     */
    private record VersionCacheEntry(String version, VirtualFile versionFile, long lastModified) {
    }

    /**
     * 版本解析结果记录类
     * <p>
     * 用于返回解析后的版本号以及可用于缓存失效判断的文件.
     */
    private record VersionResolveResult(String version, VirtualFile versionFile) {
    }

    /**
     * 获取指定项目的缓存锁对象
     *
     * @param project 项目对象
     * @return 锁对象
     */
    @NotNull
    private static Object getCacheLock(@NotNull Project project) {
        return project;
    }

    /**
     * 解析项目版本号
     * <p>
     * 按优先级顺序尝试不同的解析方式，返回第一个成功的结果。
     * 如果所有方式都失败，返回默认版本号。
     *
     * @param project 项目对象，不能为 null
     * @param element 当前文件所在元素, 不能为 null
     * @return 版本号字符串，不会为 null
     */
    @NotNull
    public static String resolveVersion(@NotNull Project project, @NotNull PsiElement element) {
        return ReadAction.compute(() -> resolveVersionWithReadAction(project, element));
    }

    /**
     * 解析项目版本号
     *
     * @param element 当前文件所在元素, 不能为 null
     * @return 版本号字符串，不会为 null
     */
    @NotNull
    public static String resolveVersion(@NotNull PsiElement element) {
        return ReadAction.compute(() -> resolveVersionWithReadAction(element.getProject(), element));
    }

    /**
     * 在读取操作上下文中解析项目版本号
     * <p>
     * 该方法在 IntelliJ Platform 的 ReadAction 作用域内执行, 确保线程安全地获取或缓存版本号.
     * 首先检查项目是否已释放, 若已释放则返回默认版本号; 否则检查缓存命中情况, 若命中则直接返回缓存值;
     * 若未命中, 则通过同步锁确保并发安全地重新解析版本号, 并将结果缓存后返回.
     *
     * @param project 项目对象, 不能为 null, 用于标识当前项目上下文
     * @param element 当前文件所在元素, 不能为 null, 用于定位文件路径以辅助查找配置文件
     * @return 版本号字符串, 永远不会为 null, 若解析失败则返回默认版本号
     */
    @NotNull
    private static String resolveVersionWithReadAction(@NotNull Project project, @NotNull PsiElement element) {
        if (project.isDisposed()) {
            log.debug("项目已释放, 返回默认版本号");
            return DEFAULT_VERSION;
        }

        VersionCacheEntry cached = VERSION_CACHE.get(project);
        if (cached(cached)) {
            log.debug("命中版本号缓存: {}", cached.version);
            return cached.version;
        }

        synchronized (getCacheLock(project)) {
            // 双重检查缓存，避免重复解析
            cached = VERSION_CACHE.get(project);
            if (cached(cached)) {
                log.debug("命中版本号缓存(二次检查): {}", cached.version);
                return cached.version;
            }

            VersionResolveResult resolved = resolveVersionInternal(project, element);
            String version = resolved.version != null ? resolved.version : DEFAULT_VERSION;
            VirtualFile versionFile = resolved.versionFile;
            long lastModified = versionFile != null && versionFile.isValid() ? versionFile.getTimeStamp() : 0;
            VERSION_CACHE.put(project, new VersionCacheEntry(version, versionFile, lastModified));
            log.debug("缓存版本号: {} (file={})", version, versionFile != null ? versionFile.getPath() : "null");
            return version;
        }
    }

    /**
     * 判断缓存条目是否有效
     * <p>
     * 根据缓存条目中的文件最后修改时间与缓存记录的时间戳进行比较, 若文件未修改则认为缓存有效.
     * 如果缓存条目为 null 或文件无效, 则返回 false.
     *
     * @param cached 缓存条目对象, 可能为 null
     * @return 如果缓存有效 (文件未修改) 则返回 true, 否则返回 false
     */
    private static boolean cached(VersionCacheEntry cached) {
        if (cached != null) {
            VirtualFile versionFile = cached.versionFile;
            if (versionFile == null) {
                return true;
            }
            if (versionFile.isValid()) {
                long currentModified = versionFile.getTimeStamp();
                return currentModified == cached.lastModified;
            }
        }
        return false;
    }

    /**
     * 内部解析项目版本号
     * <p>
     * 按优先级顺序尝试从 Maven 或 Gradle 项目中解析版本号, 若均失败则返回默认版本号.
     * 解析成功时, 返回包含版本号及对应配置文件的解析结果对象.
     *
     * @param project 项目对象, 不能为 null
     * @return 版本解析结果, 包含版本号字符串及对应配置文件 (如 pom.xml 或 build.gradle), 若解析失败则使用默认版本号
     */
    @NotNull
    private static VersionResolveResult resolveVersionInternal(@NotNull Project project, @NotNull PsiElement element) {
        // 1. 尝试从 Maven 项目读取
        String version = resolveFromMaven(project, element);
        if (version != null && !version.isEmpty()) {
            log.debug("从 Maven 项目解析版本号: {}", version);
            return new VersionResolveResult(normalizeVersion(version), findPomFile(project, element));
        }

        // 2. 尝试从 Gradle 项目读取
        version = resolveFromGradle(project, element);
        if (version != null && !version.isEmpty()) {
            log.debug("从 Gradle 项目解析版本号: {}", version);
            return new VersionResolveResult(normalizeVersion(version), findGradleFile(element));
        }

        // 3. 使用默认值
        log.debug("使用默认版本号: {}", DEFAULT_VERSION);
        return new VersionResolveResult(DEFAULT_VERSION, null);
    }

    /**
     * 清理指定项目的版本号缓存
     *
     * @param project 要清理缓存的项目对象
     */
    public static void clearCache(@NotNull Project project) {
        VERSION_CACHE.remove(project);
    }

    /**
     * 清理所有项目的版本号缓存
     */
    public static void clearAllCache() {
        VERSION_CACHE.clear();
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
    private static String resolveFromMaven(@NotNull Project project, @NotNull PsiElement element) {
        // 使用 Maven 插件 API（如果可用）
        String version = resolveFromMavenPlugin(project, element);
        if (version != null && !version.isEmpty()) {
            return version;
        }
        log.debug("Maven API 未获取到版本号, 尝试解析 pom.xml");

        // 方式二：直接解析 pom.xml 文件
        return resolveFromPomXml(project, element);
    }

    /**
     * 使用 IntelliJ Maven 插件 API 解析版本号(必须要将项目链接为 maven 项目才能使用 api 获取到版本号)
     *
     * @param project 项目对象
     * @return 版本号，如果解析失败或插件不可用则返回 null
     */
    @Nullable
    private static String resolveFromMavenPlugin(@NotNull Project project, @NotNull PsiElement element) {
        try {
            // 检查 Maven 插件是否可用
            MavenProjectsManager manager = MavenProjectsManager.getInstance(project);
            if (manager == null) {
                log.debug("MavenProjectsManager 不可用");
                return null;
            }

            // 这里如果不是 maven 项目也会基于文件路径来获取 pom.xml, 但是后续 version 还是会返回 null, 有兜底处理: resolveFromPomXml
            VirtualFile pomFile = findPomFile(project, element);
            if (pomFile == null) {
                log.debug("未找到 pom.xml, 无法通过 Maven API 解析版本号");
                return null;
            }

            MavenProject containingProject = manager.findContainingProject(pomFile);
            if (containingProject == null) {
                log.debug("未找到 pom.xml 对应的 MavenProject");
                return null;
            }

            String version = containingProject.getModelMap().get("version");
            if (version != null && !version.isEmpty()) {
                return version;
            }
            log.debug("MavenProject 未提供 version");
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
    private static String resolveFromPomXml(@NotNull Project project, @NotNull PsiElement element) {
        VirtualFile pomFile = findPomFile(project, element);
        if (pomFile == null || !pomFile.exists()) {
            log.debug("未找到 pom.xml, 无法解析版本号");
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
    private static String resolveFromGradle(@NotNull Project project, @NotNull PsiElement element) {
        // 方式一：使用 Gradle 插件 API（如果可用）
        String version = resolveFromGradlePlugin(project);
        if (version != null && !version.isEmpty()) {
            return version;
        }
        log.debug("Gradle API 未获取到版本号, 尝试解析 build.gradle");

        // 方式二：直接解析 build.gradle 或 build.gradle.kts 文件
        return resolveFromGradleFile(element);
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
            ExternalProjectsManager externalProjectsManager = ExternalProjectsManager.getInstance(project);
            if (externalProjectsManager == null) {
                log.debug("ExternalProjectsManager 不可用");
                return null;
            }

            // 获取 Gradle 项目设置
            ProjectDataManager projectDataManager = ProjectDataManager.getInstance();
            if (projectDataManager == null) {
                log.debug("ProjectDataManager 不可用");
                return null;
            }

            // 获取项目数据
            ProjectSystemId gradleSystemId = new ProjectSystemId("GRADLE");
            String externalProjectPath = getExternalProjectPath(project);
            if (externalProjectPath == null || externalProjectPath.isEmpty()) {
                log.debug("外部项目路径为空");
                return null;
            }

            ExternalProjectInfo projectInfo = projectDataManager
                .getExternalProjectData(project, gradleSystemId, externalProjectPath);

            if (projectInfo == null) {
                log.debug("ExternalProjectInfo 为空");
                return null;
            }

            DataNode<ProjectData> projectDataNode = projectInfo.getExternalProjectStructure();
            if (projectDataNode == null) {
                log.debug("ExternalProjectStructure 为空");
                return null;
            }

            ProjectData projectData = projectDataNode.getData();
            if (projectData == null) {
                log.debug("ProjectData 为空");
                return null;
            }

            String version = projectData.getVersion();
            if (version != null && !version.isEmpty()) {
                return version;
            }
            log.debug("ProjectData 未提供 version");
        } catch (Exception e) {
            // Gradle 插件不可用或解析失败，静默处理
            log.debug("Gradle 插件 API 不可用或解析失败", e);
        }
        return null;
    }

    /**
     * 直接解析 build.gradle 或 build.gradle.kts 文件获取版本号
     *
     * @param element 当前文件所在元素
     * @return 版本号，如果解析失败则返回 null
     */
    @Nullable
    private static String resolveFromGradleFile(@NotNull PsiElement element) {
        VirtualFile gradleFile = findGradleFile(element);
        if (gradleFile == null) {
            log.debug("未找到 build.gradle(.kts), 无法解析版本号");
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
     * 规范化版本号
     * <p>
     * 去除版本号中的 'v' 前缀，并处理其他格式问题。
     *
     * @param version 原始版本号
     * @return 规范化后的版本号
     */
    @NotNull
    private static String normalizeVersion(@NotNull String version) {
        String normalized = version.trim().toUpperCase()
            .replace("-snapshot", "")
            .replace(".release", "")
            .replace("-release", "");
        // 去除 'v' 前缀（如果存在）
        if (normalized.startsWith("v")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    /**
     * 估算项目根目录
     * <p>
     * 根据项目对象尝试推断其根目录路径, 若无法推断则返回 null.
     *
     * @param project 项目对象, 不能为 null
     * @return 项目根目录的虚拟文件, 如果无法推断则返回 null
     */
    @Nullable
    private static VirtualFile guessProjectDir(@NotNull Project project) {
        return ProjectUtil.guessProjectDir(project);
    }

    /**
     * 查找项目根目录下的 pom.xml 文件
     * <p>
     * 根据项目对象推断其根目录路径, 然后在该目录下查找名为 "pom.xml" 的文件. 如果文件存在则返回其虚拟文件对象, 否则返回 null.
     *
     * @param project 项目对象, 不能为 null
     * @return pom.xml 文件的虚拟文件对象, 如果未找到或无法推断根目录则返回 null
     */
    @Nullable
    private static VirtualFile findPomFile(@NotNull Project project, @NotNull PsiElement element) {
        VirtualFile pomFile = getPomFile(element);
        if (pomFile != null && pomFile.exists()) {
            log.debug("通过 Maven API 找到 pom.xml: {}", pomFile.getPath());
            return pomFile;
        }

        VirtualFile file = element.getContainingFile().getVirtualFile();
        if (file != null) {
            log.debug("Maven API 未命中, 尝试从文件路径向上查找 pom.xml: {}", file.getPath());
        }
        return findNearestPomFromFile(file);
    }

    /**
     * 获取 pom.xml 文件(必须在 idea 中添加未 maven 项目才能获取到)
     * <p>
     * 从 Maven 项目中获取 pom.xml 文件.
     *
     * @param element PsiElement 元素
     * @return pom.xml 文件, 如果获取失败返回 null
     */
    private static VirtualFile getPomFile(PsiElement element) {
        try {
            VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
            if (virtualFile == null) {
                return null;
            }

            MavenProjectsManager manager = MavenProjectsManager.getInstance(element.getProject());
            MavenProject containingProject = manager.findContainingProject(virtualFile);

            if (containingProject != null) {
                return containingProject.getFile();
            }
            log.debug("findContainingProject 返回 null: {}", virtualFile.getPath());
        } catch (Exception ignored) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 在指定文件的父目录及其上级目录中查找最近的 pom.xml 文件
     * <p>
     * 从传入的文件对象开始, 向上遍历其父目录, 直到根目录为止, 查找名为 "pom.xml" 的文件.
     * 如果找到则返回该文件的虚拟文件对象, 否则返回 null.
     *
     * @param file 起始文件对象, 可能为 null
     * @return 找到的 pom.xml 文件的虚拟文件对象, 如果未找到或起始文件为 null 则返回 null
     */
    @Nullable
    private static VirtualFile findNearestPomFromFile(@Nullable VirtualFile file) {
        return findNearestFileFromFile(file, "pom.xml", "pom.xml");
    }

    /**
     * 查找 Gradle 项目配置文件 (build.gradle 或 build.gradle.kts)
     * <p>
     * 根据当前文件路径向上查找 <code>build.gradle.kts</code> 或 <code>build.gradle</code>,
     * 查找过程中遇到 <code>src/main</code> 即停止, 以避免跨模块误匹配.
     *
     * @param element 当前文件所在元素, 不能为 null
     * @return Gradle 配置文件的虚拟文件对象, 如果未找到或推断失败则返回 null
     */
    @Nullable
    private static VirtualFile findGradleFile(@NotNull PsiElement element) {
        VirtualFile file = element.getContainingFile().getVirtualFile();
        if (file != null) {
            log.debug("开始从文件路径查找 build.gradle(.kts): {}", file.getPath());
        }
        return findNearestGradleFileFromFile(file);
    }

    /**
     * 在指定文件的父目录及其上级目录中查找最近的 Gradle 配置文件 (<code>build.gradle.kts</code> 或 <code>build.gradle</code>)
     * <p>
     * 从传入的文件对象开始, 向上遍历其父目录, 直到根目录为止, 查找名为 <code>build.gradle.kts</code> 或 <code>build.gradle</code> 的文件.
     * 查找过程中, 若遇到 <code>src/main</code> 目录即停止, 以避免在多模块项目中跨模块误匹配.
     *
     * @param file 起始文件对象, 可能为 null
     * @return 找到的 Gradle 配置文件的虚拟文件对象, 如果未找到或起始文件为 null 则返回 null
     */
    @Nullable
    private static VirtualFile findNearestGradleFileFromFile(@Nullable VirtualFile file) {
        return findNearestFileFromFile(file, "build.gradle.kts", "build.gradle");
    }

    /**
     * 判断是否应在遇到 src/main 目录时停止搜索
     * <p>
     * 当当前目录名称为 "main" 且其父目录名称为 "src" 时, 返回 true, 表示应停止向上搜索.
     * 否则返回 false, 继续向上遍历目录.
     *
     * @param current 当前虚拟文件对象, 不能为 null
     * @return 如果当前目录是 src/main 目录结构则返回 true, 否则返回 false
     */
    private static boolean shouldStopSearchAtSrcMain(@NotNull VirtualFile current) {
        if (!"main".equals(current.getName())) {
            return false;
        }
        VirtualFile srcDir = current.getParent();
        return srcDir != null && "src".equals(srcDir.getName());
    }

    /**
     * 在指定文件的父目录及其上级目录中查找最近的指定文件
     * <p>
     * 从传入的文件对象开始, 向上遍历其父目录, 直到根目录为止, 查找名为 <code>primaryFileName</code> 或 <code>secondaryFileName</code> 的文件.
     * 查找过程中, 若遇到 <code>src/main</code> 目录结构则停止搜索, 以避免跨模块误匹配.
     * 若找到文件则返回其虚拟文件对象, 否则返回 null.
     *
     * @param file              起始文件对象, 可能为 null
     * @param primaryFileName   主文件名, 不能为 null
     * @param secondaryFileName 备用文件名, 不能为 null
     * @return 找到的文件的虚拟文件对象, 如果未找到或起始文件为 null 则返回 null
     */
    @Nullable
    private static VirtualFile findNearestFileFromFile(@Nullable VirtualFile file,
                                                       @NotNull String primaryFileName,
                                                       @NotNull String secondaryFileName) {
        if (file == null) {
            return null;
        }
        VirtualFile current = file.isDirectory() ? file : file.getParent();
        while (current != null) {
            if (shouldStopSearchAtSrcMain(current)) {
                log.debug("命中 src/main, 停止查找文件: {}", current.getPath());
                return null;
            }
            VirtualFile target = current.findChild(primaryFileName);
            if (target != null && target.exists()) {
                log.debug("从路径查找到 {}: {}", primaryFileName, target.getPath());
                return target;
            }
            target = current.findChild(secondaryFileName);
            if (target != null && target.exists()) {
                log.debug("从路径查找到 {}: {}", secondaryFileName, target.getPath());
                return target;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 获取外部项目路径
     * <p>
     * 优先通过项目根目录推断外部项目路径, 若失败则返回项目基础路径.
     *
     * @param project 项目对象, 不能为 null
     * @return 外部项目路径字符串, 如果推断失败则返回项目基础路径, 可能为 null
     */
    @Nullable
    private static String getExternalProjectPath(@NotNull Project project) {
        VirtualFile baseDir = guessProjectDir(project);
        if (baseDir != null) {
            return baseDir.getPath();
        }
        return project.getBasePath();
    }
}
