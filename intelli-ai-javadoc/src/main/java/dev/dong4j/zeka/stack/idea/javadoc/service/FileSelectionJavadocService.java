package dev.dong4j.zeka.stack.idea.javadoc.service;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.javadoc.util.PluginUtil;
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
                tasks.addAll(collector.collectFromDirectory(file));
            } else if (PluginUtil.isSupportedFile(file)) {
                tasks.addAll(collector.collectFromVirtualFile(file));
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
}
