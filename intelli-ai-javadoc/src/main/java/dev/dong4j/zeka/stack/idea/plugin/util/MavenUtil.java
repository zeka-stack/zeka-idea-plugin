package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Maven 工具类
 * <p>
 * 提供与 Maven 项目相关的实用功能, 主要负责从项目中提取版本信息并进行处理, 适用于需要动态获取或修改 Maven 项目版本号的场景.
 * <p>
 * 版本号查询使用缓存机制，避免在批量任务中重复查询。缓存基于 pom.xml 文件的修改时间自动失效。
 *
 * @author dong4j
 * @version 0.0.1
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.05.13
 * @since 1.0.0
 */
public final class MavenUtil {
    private MavenUtil() {}

    /**
     * 版本号缓存条目
     * <p>
     * 存储缓存的版本号、pom.xml 文件和文件最后修改时间。
     *
     * @param version      缓存的版本号
     * @param pomFile      pom.xml 文件
     * @param lastModified 文件最后修改时间
     */
        private record VersionCacheEntry(String version, VirtualFile pomFile, long lastModified) {
    }

    /**
     * 版本号缓存
     * <p>
     * Key: Project 对象
     * Value: VersionCacheEntry 缓存条目
     */
    private static final ConcurrentHashMap<Project, VersionCacheEntry> versionCache = new ConcurrentHashMap<>();

    /**
     * 获取指定项目的缓存锁对象
     * <p>
     * 用于双重检查锁定，确保线程安全。
     *
     * @param project 项目对象
     * @return 锁对象
     */
    @NotNull
    private static Object getCacheLock(@NotNull Project project) {
        // 使用项目对象作为锁，确保同一项目的缓存操作是同步的
        return project;
    }

    /**
     * 获取指定 PsiElement 所属项目的 Maven 版本号（带缓存）
     * <p>
     * 通过 PsiElement 获取其所在的虚拟文件, 进而查找对应的 Maven 项目, 并从项目模型中获取版本号信息.
     * 使用缓存机制避免重复查询，缓存基于 pom.xml 文件的修改时间自动失效。
     * <p>
     * 缓存策略：
     * <ul>
     *   <li>首次查询：读取并缓存版本号和 pom.xml 的修改时间</li>
     *   <li>后续查询：检查 pom.xml 的修改时间，如果未变化则直接返回缓存值</li>
     *   <li>文件变化：重新读取版本号并更新缓存</li>
     * </ul>
     * <p>
     * 若获取失败或版本号中包含 "-SNAPSHOT" 或 ".RELEASE" 后缀, 则将其移除后返回.
     * 若发生异常或未找到版本号, 则返回默认版本 "1.0.0".
     *
     * @param element 需要获取版本号的 PsiElement
     * @return 项目版本号, 格式为去除 "-SNAPSHOT" 和 ".RELEASE" 后的字符串
     */
    public static String getVersion(PsiElement element) {
        Project project = element.getProject();

        // 1. 检查缓存
        VersionCacheEntry cached = versionCache.get(project);
        if (cached != null && cached.pomFile != null && cached.pomFile.isValid()) {
            // 2. 检查文件是否修改
            long currentModified = cached.pomFile.getTimeStamp();
            if (currentModified == cached.lastModified) {
                return cached.version; // 缓存命中
            }
        }

        // 3. 重新查询并更新缓存（使用双重检查锁定）
        synchronized (getCacheLock(project)) {
            // 双重检查：再次检查缓存（可能其他线程已经更新）
            cached = versionCache.get(project);
            if (cached != null && cached.pomFile != null && cached.pomFile.isValid()) {
                long currentModified = cached.pomFile.getTimeStamp();
                if (currentModified == cached.lastModified) {
                    return cached.version;
                }
            }

            // 查询版本号
            String version = queryVersionFromMaven(element);
            VirtualFile pomFile = getPomFile(element);

            // 更新缓存
            long lastModified = pomFile != null && pomFile.isValid() ? pomFile.getTimeStamp() : 0;
            versionCache.put(project, new VersionCacheEntry(version, pomFile, lastModified));

            return version;
        }
    }

    /**
     * 从 Maven 项目查询版本号
     * <p>
     * 实际的版本号查询逻辑，不包含缓存。
     *
     * @param element PsiElement 元素
     * @return 版本号，如果查询失败返回 "1.0.0"
     */
    private static String queryVersionFromMaven(PsiElement element) {
        try {
            VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
            MavenProjectsManager manager = MavenProjectsManager.getInstance(element.getProject());
            MavenProject containingProject = manager.findContainingProject(virtualFile);
            if (containingProject == null) {
                return "1.0.0";
            }
            String version = containingProject.getModelMap().get("version");
            if (version == null || version.isEmpty()) {
                return "1.0.0";
            }
            return version.replace("-SNAPSHOT", "").replace(".RELEASE", "");
        } catch (Exception ignored) {
            return "1.0.0";
        }
    }

    /**
     * 获取 pom.xml 文件
     * <p>
     * 从 Maven 项目中获取 pom.xml 文件。
     *
     * @param element PsiElement 元素
     * @return pom.xml 文件，如果获取失败返回 null
     */
    private static VirtualFile getPomFile(PsiElement element) {
        try {
            VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
            MavenProjectsManager manager = MavenProjectsManager.getInstance(element.getProject());
            MavenProject containingProject = manager.findContainingProject(virtualFile);
            if (containingProject != null) {
                return containingProject.getFile();
            }
        } catch (Exception ignored) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 清理指定项目的版本号缓存
     * <p>
     * 在项目关闭时调用，避免内存泄漏。
     * 该方法为公共方法，可在其他地方使用。
     *
     * @param project 要清理缓存的项目
     */
    public static void clearCache(@NotNull Project project) {
        versionCache.remove(project);
    }

    /**
     * 清理所有项目的版本号缓存
     * <p>
     * 清理所有已缓存的版本号信息。
     * 该方法为公共方法，可在其他地方使用。
     */
    public static void clearAllCache() {
        versionCache.clear();
    }

    /**
     * 将作者信息放入参数映射中
     * <p>
     * 从系统属性中获取作者名称, 如果未设置且为类模板模式, 则使用默认作者名称, 并将作者信息存入参数映射中.
     */
    public static String getAuthor(String author) {
        if (author == null || author.isEmpty()) {
            author = SystemUtils.getProperty("ZEKA_NAME_SPACE");
            return author == null || author.isEmpty() ? "zeka.stack.team" : author;
        }
        return author;
    }

}
