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
 * Git 提交页面的 JavaDoc 生成器
 *
 * <p>负责检测提交的 Java 文件中缺少 JavaDoc 的元素，并批量生成文档。
 * 采用非覆盖模式，只为没有 JavaDoc 的元素生成文档。
 *
 * <p>处理流程：
 * <ol>
 *   <li>检测提交的 Java 文件</li>
 *   <li>收集缺少 JavaDoc 的代码元素</li>
 *   <li>显示检测结果统计</li>
 *   <li>批量生成文档</li>
 *   <li>显示生成结果</li>
 * </ol>
 *
 * @author dong4j
 * @version 1.4.0
 * @since 1.4.0
 */
@Slf4j
public class CommitJavaDocGenerator {

    /** 项目对象 */
    private final Project project;

    /**
     * 构造函数
     *
     * @param project 项目对象
     */
    public CommitJavaDocGenerator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 为变更的文件生成 JavaDoc
     *
     * <p>检测提交的 Java 文件中缺少 JavaDoc 的元素，并批量生成文档。
     * 整个过程在后台任务中执行，不会阻塞 UI。
     *
     * @param changes   文件变更列表
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
     * 检测缺少 JavaDoc 的元素
     *
     * <p>遍历所有 Java 文件，收集缺少 JavaDoc 的类、方法、字段。
     * 使用 TaskCollector 收集任务，但只收集缺少 JavaDoc 的元素。
     * 在后台线程中执行，需要使用 read-action 访问 PSI。
     *
     * @param javaFiles Java 文件列表
     * @param indicator 进度指示器
     * @return 文档生成任务列表
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
     * 生成文档
     *
     * <p>调用 DocumentationGenerationService 批量生成文档。
     *
     * @param tasks 文档生成任务列表
     */
    private void generateDocumentation(@NotNull List<DocumentationTask> tasks) {
        DocumentationGenerationService service = new DocumentationGenerationService();
        service.generateDocumentation(project, tasks, JavaDocBundle.message("commit.target.description"),
                                      stats -> {
                                          // 显示生成结果
                                          NotificationUtil.notifyCompletion(project, stats.completed(), stats.failed(), stats.skipped());
                                          log.info("Git 提交页面：文档生成完成 - {}", stats);
                                      });
    }

    /**
     * 构建检测结果消息
     *
     * @param classCount  缺少 JavaDoc 的类数量
     * @param methodCount 缺少 JavaDoc 的方法数量
     * @param fieldCount  缺少 JavaDoc 的字段数量
     * @return 检测结果消息
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

