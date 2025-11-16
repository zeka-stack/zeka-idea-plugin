package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.Objects;

/**
 * Maven 工具类
 * <p>
 * 提供与 Maven 项目相关的实用功能, 主要负责从项目中提取版本信息并进行处理, 适用于需要动态获取或修改 Maven 项目版本号的场景.
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
     * 获取指定 PsiElement 所属项目的 Maven 版本号
     * <p>
     * 通过 PsiElement 获取其所在的虚拟文件, 进而查找对应的 Maven 项目, 并从项目模型中获取版本号信息.
     * 若获取失败或版本号中包含 "-SNAPSHOT" 或 ".RELEASE" 后缀, 则将其移除后返回.
     * 若发生异常或未找到版本号, 则返回默认版本 "1.0.0".
     *
     * @param element 需要获取版本号的 PsiElement
     * @return 项目版本号, 格式为去除 "-SNAPSHOT" 和 ".RELEASE" 后的字符串
     */
    public static String getVersion(PsiElement element) {
        try {
            VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
            MavenProjectsManager manager = MavenProjectsManager.getInstance(element.getProject());
            MavenProject containingProject = manager.findContainingProject(virtualFile);
            String version = Objects.requireNonNull(containingProject).getModelMap().get("version");
            return version.replace("-SNAPSHOT", "").replace(".RELEASE", "");
        } catch (Exception ignored) {
        }
        return "1.0.0";
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
