package dev.dong4j.zeka.stack.idea.javadoc.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;

/**
 * 提交 Javadoc 检查器类
 * <p> 该类提供了对项目中 Java 和 Kotlin 文件的 Javadoc 缺失检测功能. 通过过滤出项目中的 Java 和 Kotlin 文件,
 * 并检测这些文件中缺少 Javadoc 的类, 方法和字段, 生成检测报告.
 * <p>
 * 类的主要职责包括:
 * - 过滤出项目中的 Java 和 Kotlin 文件
 * - 检测文件中缺少 Javadoc 的类, 方法和字段
 * - 生成检测总结报告
 * <p>
 * 该类为单例类, 所有方法均为静态方法, 不对外暴露构造函数.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.11
 * @since 1.0.0
 */
public final class CommitJavadocChecker {
    /**
     * 私有构造方法, 防止外部实例化 CommitJavadocChecker 类
     * <p> 该类仅包含静态方法, 因此不提供公共的构造函数以避免创建不必要的实例
     */
    private CommitJavadocChecker() {
    }

    /**
     * 过滤出项目中的 Java 或 Kotlin 文件
     * <p> 遍历给定的变化集合, 检查每个文件是否为 Java 或 Kotlin 文件, 并且在项目中有效.
     * 如果项目已销毁, 则返回一个空的文件列表.
     *
     * @param project 项目对象
     * @param changes 变化集合, 包含文件更改信息
     * @return 包含 Java 或 Kotlin 文件的虚拟文件列表
     */
    @NotNull
    public static List<VirtualFile> filterJavaFiles(@NotNull Project project,
                                                    @NotNull Collection<Change> changes) {
        if (project.isDisposed()) {
            return new ArrayList<>();
        }

        return ApplicationManager.getApplication().runReadAction(
            (Computable<List<VirtualFile>>) () -> {
                ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
                List<VirtualFile> javaFiles = new ArrayList<>();
                for (Change change : changes) {
                    VirtualFile file = change.getVirtualFile();
                    if (file == null) {
                        continue;
                    }
                    if (!isJavaOrKotlinFile(file)) {
                        continue;
                    }
                    if (!isFileInProject(project, file, fileIndex)) {
                        continue;
                    }
                    javaFiles.add(file);
                }
                return javaFiles;
            }
                                                                );
    }

    /**
     * 检测指定 Java 文件中缺失的 Javadoc 文档
     * <p> 遍历传入的 Java 文件列表, 对每个文件执行文档缺失检测, 并收集所有缺失的文档任务
     *
     * @param project   当前项目对象, 用于获取 Psi 文件和任务收集器
     * @param javaFiles 待检测的 Java 文件列表, 必须非空
     * @param indicator 进度指示器, 用于显示当前处理文件的进度和状态, 可为 null
     * @return 包含所有缺失 Javadoc 文档任务的列表, 若无缺失则返回空列表
     */
    @NotNull
    public static List<DocumentationTask> detectMissingJavaDoc(@NotNull Project project,
                                                               @NotNull List<VirtualFile> javaFiles,
                                                               @Nullable ProgressIndicator indicator) {
        if (javaFiles.isEmpty()) {
            return new ArrayList<>();
        }

        List<DocumentationTask> tasks = new ArrayList<>();
        TaskCollector collector = new TaskCollector(project);

        for (int i = 0; i < javaFiles.size(); i++) {
            ProgressManager.checkCanceled();
            VirtualFile virtualFile = javaFiles.get(i);
            if (indicator != null) {
                indicator.setText(JavadocBundle.message("commit.detecting.file", virtualFile.getName()));
                indicator.setFraction((double) i / javaFiles.size());
            }

            List<DocumentationTask> fileTasks = ApplicationManager.getApplication().runReadAction(
                (Computable<List<DocumentationTask>>) () -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile instanceof PsiJavaFile) {
                        return collector.collectMissingJavaDocFromFile(psiFile);
                    }
                    return new ArrayList<>();
                }
                                                                                                 );
            tasks.addAll(fileTasks);
        }

        return tasks;
    }

    /**
     * 根据文档检查任务列表生成检测汇总信息
     * <p> 统计任务列表中缺失文档注释的类, 方法, 字段数量, 并生成可读的汇总描述
     *
     * @param tasks 需要检测的文档任务列表, 不能为空
     * @return DetectionSummary 包含各类统计数量和汇总信息的记录对象
     */
    @NotNull
    public static DetectionSummary buildDetectionSummary(@NotNull List<DocumentationTask> tasks) {
        int classCount = 0;
        int methodCount = 0;
        int fieldCount = 0;

        for (DocumentationTask task : tasks) {
            switch (task.getType()) {
                case CLASS -> classCount++;
                case METHOD, TEST_METHOD -> methodCount++;
                case FIELD -> fieldCount++;
            }
        }

        List<String> parts = new ArrayList<>();
        if (classCount > 0) {
            parts.add(JavadocBundle.message("commit.detection.classes", classCount));
        }
        if (methodCount > 0) {
            parts.add(JavadocBundle.message("commit.detection.methods", methodCount));
        }
        if (fieldCount > 0) {
            parts.add(JavadocBundle.message("commit.detection.fields", fieldCount));
        }

        String summary = parts.isEmpty()
                         ? JavadocBundle.message("commit.no.missing.javadoc")
                         : String.join("、", parts);

        return new DetectionSummary(classCount, methodCount, fieldCount, summary);
    }

    /**
     * 判断给定文件是否为 Java 或 Kotlin 文件
     * <p> 通过检查文件扩展名是否为 java 或 kotlin 来判断文件类型
     *
     * @param file 要判断的文件对象
     * @return 如果文件是 Java 或 Kotlin 文件, 返回 true; 否则返回 false
     */
    private static boolean isJavaOrKotlinFile(@NotNull VirtualFile file) {
        return PluginContents.JAVA.equalsIgnoreCase(file.getExtension())
               || PluginContents.KOTLIN_EXTENSION.equalsIgnoreCase(file.getExtension());
    }

    /**
     * 判断指定文件是否在当前项目范围内
     * <p> 检查文件系统类型是否为本地文件系统, 并确认文件是否属于当前项目
     *
     * @param project   当前项目对象
     * @param file      要检查的虚拟文件
     * @param fileIndex 项目文件索引, 用于判断文件是否在项目中
     * @return 如果文件是本地文件系统且属于当前项目, 则返回 true, 否则返回 false
     */
    private static boolean isFileInProject(@NotNull Project project,
                                           @NotNull VirtualFile file,
                                           @NotNull ProjectFileIndex fileIndex) {
        if (file.getFileSystem() instanceof JarFileSystem) {
            return false;
        }

        if (!(file.getFileSystem() instanceof LocalFileSystem)) {
            return false;
        }

        return fileIndex.isInProject(file);
    }

    /**
     * 检测摘要记录类
     * <p> 用于封装检测结果的汇总信息, 包含类数量, 方法数量, 字段数量以及检测总结描述. 该类采用 record 关键字定义, 适用于需要不可变数据结构的场景.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.11
     * @since 1.0.0
     */
    public record DetectionSummary(int classCount, int methodCount, int fieldCount, @NotNull String summary) {
    }
}
