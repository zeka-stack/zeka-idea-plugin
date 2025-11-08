package dev.dong4j.zeka.stack.idea.plugin.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * JavaDoc 生成工具类
 * <p>
 * 该类用于在 Git 提交时检测并生成缺失的 JavaDoc 注释, 支持对类, 方法和字段的注释生成.
 * 提供了检测 Java 文件中缺失 JavaDoc 的功能, 并在检测完成后提示用户是否生成注释.
 * 支持进度显示和结果通知, 适用于代码提交前的自动化文档生成流程.
 *
 * @author 作者信息
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
@Slf4j
public class CommitJavaDocGenerator {
    /** 项目对象, 用于表示当前操作所关联的项目信息 */
    private final Project project;

    /**
     * 初始化 CommitJavaDocGenerator 实例
     * <p>
     * 通过传入的 Project 对象进行初始化, 用于后续生成 JavaDoc 注释的相关操作
     *
     * @param project 项目对象, 用于存储项目相关信息
     */
    public CommitJavaDocGenerator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理代码变更, 生成 JavaDoc 注释
     * <p>
     * 该方法用于处理提交的代码变更, 检查是否有需要生成 JavaDoc 的任务.
     * 如果存在任务, 会提示用户确认是否生成, 并执行生成操作.
     *
     * @param changes   变更集合
     * @param javaFiles Java 文件列表
     */
    public void generateForChanges(@NotNull Collection<Change> changes,
                                   @NotNull List<VirtualFile> javaFiles) {
        if (javaFiles.isEmpty()) {
            log.warn("Git 提交页面：没有 Java 文件需要处理");
            return;
        }

        // 在后台任务中执行检测和生成
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, JavaDocBundle.message("commit.detecting.progress"), true) {
                /**
                 * 执行检测缺失 JavaDoc 的任务
                 * <p>
                 * 检测指定 Java 文件中缺失 JavaDoc 的类, 方法和字段, 并根据检测结果提示用户是否生成缺失的 JavaDoc.
                 *
                 * @param indicator 进度指示器, 用于显示任务执行进度
                 * @throws IllegalStateException 如果在非 EDT 线程中调用此方法
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    List<DocumentationTask> tasks = detectMissingJavaDoc(javaFiles, indicator);
                    if (tasks.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NotificationUtil.notifyNoTask(project,
                                                          JavaDocBundle.message("commit.no.missing.javadoc"));
                        });
                        return;
                    }
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
                    final int finalClassCount = classCount;
                    final int finalMethodCount = methodCount;
                    final int finalFieldCount = fieldCount;
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String message = buildDetectionMessage(finalClassCount, finalMethodCount, finalFieldCount);
                        int result = Messages.showYesNoDialog(
                            project,
                            message,
                            JavaDocBundle.message("commit.detection.title"),
                            Messages.getQuestionIcon());
                        if (result != Messages.YES) {
                            return;
                        }
                        generateDocumentation(tasks);
                    });
                }
 */

                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    // 检测缺少 JavaDoc 的元素
                    List<DocumentationTask> tasks = detectMissingJavaDoc(javaFiles, indicator);

                    if (tasks.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NotificationUtil.notifyNoTask(project,
                                                          JavaDocBundle.message("commit.no.missing.javadoc"));
                        });
                        return;
                    }

                    // 显示检测结果
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

                    final int finalClassCount = classCount;
                    final int finalMethodCount = methodCount;
                    final int finalFieldCount = fieldCount;

                    // 在 EDT 中显示检测结果并询问用户是否继续
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String message = buildDetectionMessage(finalClassCount, finalMethodCount, finalFieldCount);
                        int result = Messages.showYesNoDialog(
                            project,
                            message,
                            JavaDocBundle.message("commit.detection.title"),
                            Messages.getQuestionIcon()
                                                             );

                        if (result != Messages.YES) {
                            return;
                        }

                        // 用户确认后，开始生成文档
                        generateDocumentation(tasks);
                    });
                }
            }
                                         );
    }

    /**
     * 检测指定 Java 文件中缺少 JavaDoc 的文档任务
     * <p>
     * 遍历给定的 Java 文件列表, 检测其中缺少 JavaDoc 的元素, 并生成对应的文档任务.
     *
     * @param javaFiles 要检测的 Java 文件列表
     * @param indicator 进度指示器, 用于显示检测进度
     * @return 缺少 JavaDoc 的文档任务列表
     */
    @NotNull
    private List<DocumentationTask> detectMissingJavaDoc(@NotNull List<VirtualFile> javaFiles,
                                                         @NotNull ProgressIndicator indicator) {
        List<DocumentationTask> tasks = new ArrayList<>();
        TaskCollector collector = new TaskCollector(project);

        for (int i = 0; i < javaFiles.size(); i++) {
            VirtualFile virtualFile = javaFiles.get(i);
            indicator.setText(JavaDocBundle.message("commit.detecting.file", virtualFile.getName()));
            indicator.setFraction((double) i / javaFiles.size());

            // 在 read-action 中将 VirtualFile 转换为 PsiFile 并收集任务
            List<DocumentationTask> fileTasks = ApplicationManager.getApplication().runReadAction(
                (Computable<List<DocumentationTask>>) () -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile instanceof PsiJavaFile) {
                        // 使用 TaskCollector 收集任务（会自动过滤已有 JavaDoc 的元素）
                        return collector.collectFromFile(psiFile);
                    }
                    return new ArrayList<>();
                }
                                                                                                 );
            tasks.addAll(fileTasks);
        }

        log.info("Git 提交页面：检测到 {} 个缺少 JavaDoc 的元素", tasks.size());
        return tasks;
    }

    /**
     * 生成项目文档的注释信息
     * <p>
     * 根据提供的文档任务列表, 调用文档生成服务生成注释, 并在任务完成后通知用户
     * 同时记录文档生成的日志信息
     *
     * @param tasks 文档生成任务列表
     */
    private void generateDocumentation(@NotNull List<DocumentationTask> tasks) {
        DocumentationGenerationService service = new DocumentationGenerationService();
        service.generateDocumentation(project, tasks, JavaDocBundle.message("commit.target.description"),
                                      stats -> {
                                          // 显示生成结果
                                          NotificationUtil.notifyTargetCompletion(project,
                                                                                  "Git Commit Record",
                                                                                  stats.completed(),
                                                                                  stats.failed(),
                                                                                  stats.skipped());

                                          log.info("Git 提交页面：文档生成完成 - {}", stats);
                                      });
    }

    /**
     * 构建检测消息, 用于提示缺少的 JavaDoc 注释信息
     * <p>
     * 根据传入的类, 方法, 字段数量, 生成对应的检测提示信息. 如果所有数量都为 0, 则返回无 JavaDoc 的提示信息; 否则, 返回包含具体缺失项的提示信息.
     *
     * @param classCount  缺失 JavaDoc 的类数量
     * @param methodCount 缺失 JavaDoc 的方法数量
     * @param fieldCount  缺失 JavaDoc 的字段数量
     * @return 包含缺失 JavaDoc 信息的提示字符串
     * @since 1.0
     */
    @NotNull
    private String buildDetectionMessage(int classCount, int methodCount, int fieldCount) {
        List<String> parts = new ArrayList<>();

        if (classCount > 0) {
            parts.add(JavaDocBundle.message("commit.detection.classes", classCount));
        }
        if (methodCount > 0) {
            parts.add(JavaDocBundle.message("commit.detection.methods", methodCount));
        }
        if (fieldCount > 0) {
            parts.add(JavaDocBundle.message("commit.detection.fields", fieldCount));
        }

        if (parts.isEmpty()) {
            return JavaDocBundle.message("commit.no.missing.javadoc");
        }

        return JavaDocBundle.message("commit.detection.message", String.join("、", parts));
    }
}

