package dev.dong4j.zeka.stack.idea.javadoc.git;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Git 提交页面 Javadoc 生成器
 * <p>
 * 该类负责在 Git 提交页面中检测和生成缺失的 Javadoc 注释, 提供对 Java 文件中类, 方法, 字段等元素的 Javadoc 自动生成功能.
 * 通过分析指定的 Java 文件集合, 检测其中缺少 Javadoc 的元素, 并提供批量生成 Javadoc 的交互式操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class CommitJavadocGenerator {
    /** 项目对象, 用于表示当前操作所关联的项目信息 */
    private final Project project;

    /**
     * 初始化 CommitJavadocGenerator 实例
     * <p>
     * 通过传入的 Project 对象进行初始化, 用于后续生成 Javadoc 注释的相关操作.
     *
     * @param project 项目对象, 用于存储项目相关信息
     */
    public CommitJavadocGenerator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理代码变更, 生成 Javadoc 注释
     * <p>
     * 该方法用于处理提交的代码变更, 检查是否有需要生成 Javadoc 的任务.
     * 如果存在任务, 会提示用户确认是否生成, 并执行生成操作.
     *
     * @param javaFiles Java 文件列表
     */
    public void generateForChanges(@NotNull List<VirtualFile> javaFiles) {
        if (javaFiles.isEmpty()) {
            log.debug("Git 提交页面：没有 Java 文件需要处理");
            return;
        }

        // 在后台任务中执行检测和生成
        ProgressManager.getInstance().run(
            new Task.Backgroundable(project, JavadocBundle.message("commit.detecting.progress"), true) {
                /**
                 * 执行检测缺失 Javadoc 的任务
                 * <p>
                 * 检测指定 Java 文件中缺失 Javadoc 的类, 方法和字段, 并根据检测结果提示用户是否生成缺失的 Javadoc.
                 *
                 * @param indicator 进度指示器, 用于显示任务执行进度
                 */
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    // 检测缺少 Javadoc 的元素
                    List<DocumentationTask> tasks = CommitJavadocChecker.detectMissingJavaDoc(project, javaFiles, indicator);

                    log.debug("Git 提交页面：检测到 {} 个缺少 Javadoc 的元素", tasks.size());

                    if (tasks.isEmpty()) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            NotificationUtil.showWarning(project,
                                                          JavadocBundle.message("commit.no.missing.javadoc"));
                        });
                        return;
                    }

                    // 显示检测结果
                    CommitJavadocChecker.DetectionSummary summary = CommitJavadocChecker.buildDetectionSummary(tasks);

                    // 在 EDT 中显示检测结果并询问用户是否继续
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String message = JavadocBundle.message("commit.detection.message", summary.summary());
                        int result = Messages.showYesNoDialog(
                            project,
                            message,
                            JavadocBundle.message("commit.detection.title"),
                            Messages.getQuestionIcon());

                        if (result != Messages.YES) {
                            return;
                        }

                        // 用户确认后，开始生成文档
                        generateForTasks(tasks);
                    });
                }
            });
    }

    /**
     * 生成项目文档的注释信息
     * <p>
     * 根据提供的文档任务列表, 调用文档生成服务生成注释, 并在任务完成后通知用户.
     * 同时记录文档生成的日志信息.
     *
     * @param tasks 文档生成任务列表
     */
    void generateForTasks(@NotNull List<DocumentationTask> tasks) {
        DocumentationGenerationService service = new DocumentationGenerationService();
        service.generateDocumentation(project, tasks, JavadocBundle.message("commit.target.description"),
                                      stats -> {
                                          // 显示生成结果
                                          NotificationUtil.notifyTargetCompletion(project,
                                                                                  "Git Commit Record",
                                                                                  stats.completed(),
                                                                                  stats.failed(),
                                                                                  stats.skipped());

                                          log.debug("Git 提交页面：文档生成完成 - {}", stats);
                                      });
    }

}
