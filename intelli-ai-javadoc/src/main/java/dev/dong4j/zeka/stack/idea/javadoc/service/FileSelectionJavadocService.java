package dev.dong4j.zeka.stack.idea.javadoc.service;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.javadoc.util.PluginUtil;
import dev.dong4j.zeka.stack.idea.plugin.common.statistics.StatisticsUserAction;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件 Javadoc 生成服务类
 * <p> 用于为选中的文件或目录批量生成 Javadoc 注释, 支持目录递归扫描和单文件处理, 适用于 IDE 插件环境中的文档自动化生成场景.
 * 该服务在执行前会检查项目状态, 索引状态, 并对任务数量进行提示与确认, 确保用户操作安全.
 * 服务不负责请求处理, 仅专注于文档生成逻辑, 符合面向对象设计原则, 避免与基础设施层耦合.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.14
 * @since 1.0.0
 */
@Slf4j
public class FileSelectionJavadocService {

    /**
     * 为选中的文件或目录生成 Javadoc 注释
     *
     * @param project 当前项目
     * @param files   选中的文件或目录
     */
    public void generateForFiles(@Nullable Project project, @Nullable VirtualFile[] files) {
        generateForFiles(project, files, StatisticsUserAction.UNKNOWN, StatisticsUserAction.UNKNOWN);
    }

    /**
     * 为选中的文件或目录生成 Javadoc 注释
     *
     * @param project    当前项目
     * @param files      选中的文件或目录
     * @param dirAction  目录触发入口
     * @param fileAction 文件触发入口
     */
    public void generateForFiles(@Nullable Project project,
                                 @Nullable VirtualFile[] files,
                                 @NotNull StatisticsUserAction dirAction,
                                 @NotNull StatisticsUserAction fileAction) {
        if (project == null || project.isDisposed() || files == null || files.length == 0) {
            return;
        }

        // 检查项目是否处于 Dumb Mode（索引模式）
        if (DumbService.isDumb(project)) {
            NotificationUtil.notifyIndexing(project);
            return;
        }

        log.debug("为 {} 个文件/目录生成 Javadoc", files.length);

        // 收集任务
        TaskCollector collector = new TaskCollector(project);
        List<DocumentationTask> tasks = new ArrayList<>();

        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                List<DocumentationTask> dirTasks = collector.collectFromDirectory(file);
                applyUserAction(dirTasks, dirAction);
                tasks.addAll(dirTasks);
            } else if (PluginUtil.isSupportedFile(file)) {
                List<DocumentationTask> fileTasks = collector.collectFromVirtualFile(file);
                applyUserAction(fileTasks, fileAction);
                tasks.addAll(fileTasks);
            }
        }

        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        if (service.checkEmptyTasks(project, tasks, JavadocBundle.message("notification.no.task.selection"))) {
            return;
        }

        // 确认是否继续（如果任务很多）
        if (tasks.size() > 50) {
            int result = Messages.showYesNoDialog(
                project,
                JavadocBundle.message("confirmation.batch.generation.message", tasks.size()),
                JavadocBundle.message("confirmation.batch.generation.title"),
                Messages.getQuestionIcon()
                                                 );

            if (result != Messages.YES) {
                return;
            }
        }

        // 使用服务生成文档
        service.generateDocumentation(project, tasks, JavadocBundle.message("task.target.selection"));
    }

    /**
     * 为指定的文档任务列表应用用户操作行为
     * <p> 遍历任务列表, 将每个任务的用户操作行为设置为指定的用户操作类型, 用于记录用户触发来源或行为类型
     *
     * @param tasks      任务列表, 必须非空, 包含待设置用户操作行为的文档任务
     * @param userAction 用户操作行为类型, 必须非空, 表示用户触发该任务的具体行为类型
     */
    private void applyUserAction(@NotNull List<DocumentationTask> tasks,
                                 @NotNull StatisticsUserAction userAction) {
        for (DocumentationTask task : tasks) {
            task.setUserAction(userAction);
        }
    }
}
