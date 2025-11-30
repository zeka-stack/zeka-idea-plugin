package dev.dong4j.zeka.stack.idea.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.service.DocumentationGenerationService;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 为文件生成 JavaDoc 的动作类
 * <p>
 * 该类继承自 AnAction, 用于在 IntelliJ IDEA 等 IDE 中提供生成 JavaDoc 的功能.
 * 支持为单个 Java 文件或整个目录中的 Java 文件批量生成 JavaDoc 注释.
 * 该动作可以处理选中的虚拟文件数组, 自动识别 Java 文件并为其生成相应的 JavaDoc 注释.
 * 当处理大量文件时, 会显示确认对话框以避免误操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
@SuppressWarnings("DuplicatedCode")
@Slf4j
public class GenerateJavaDocForFilesAction extends AnAction {

    /**
     * 构造函数
     * <p>
     * 设置动作的默认文本和描述。
     * 使用 Supplier 延迟加载国际化资源，确保在插件加载时就能正确显示。
     */
    public GenerateJavaDocForFilesAction() {
        super(() -> JavaDocBundle.message("action.generate.javadoc"),
              () -> JavaDocBundle.message("action.generate.javadoc.selection.description"),
              null);
    }

    /**
     * 处理动作事件，用于为选中的文件或目录生成 JavaDoc 注释
     * <p>
     * 该方法首先获取当前项目和选中的文件列表，若项目或文件为空则直接返回。
     * 然后收集所有需要生成 JavaDoc 的任务，检查任务是否为空，若为空则返回。
     * 若任务数量较多，会弹出确认对话框，用户确认后才继续执行。
     * 最后调用文档生成服务，生成 JavaDoc 并显示完成信息。
     *
     * @param e 动作事件对象，包含项目和选中的文件信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

        if (project == null || project.isDisposed() || files == null || files.length == 0) {
            return;
        }

        // 检查项目是否处于 Dumb Mode（索引模式）
        if (DumbService.isDumb(project)) {
            NotificationUtil.notifyIndexing(project);
            return;
        }

        log.info("为 {} 个文件/目录生成 JavaDoc", files.length);

        // 收集任务
        TaskCollector collector = new TaskCollector(project);
        List<DocumentationTask> tasks = new ArrayList<>();

        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                tasks.addAll(collector.collectFromDirectory(file));
            } else if (isJavaFile(file)) {
                tasks.addAll(collector.collectFromVirtualFile(file));
            }
        }

        // 使用文档生成服务处理任务
        DocumentationGenerationService service = new DocumentationGenerationService();
        if (service.checkEmptyTasks(project, tasks, JavaDocBundle.message("notification.no.task.selection"))) {
            return;
        }

        // 确认是否继续（如果任务很多）
        if (tasks.size() > 50) {
            int result = Messages.showYesNoDialog(
                project,
                JavaDocBundle.message("confirmation.batch.generation.message", tasks.size()),
                JavaDocBundle.message("confirmation.batch.generation.title"),
                Messages.getQuestionIcon()
                                                 );

            if (result != Messages.YES) {
                return;
            }
        }

        // 使用服务生成文档，带自定义完成回调
        service.generateDocumentation(project, tasks, JavaDocBundle.message("task.target.selection"));
    }

    /**
     * 获取用于更新操作的线程类型
     * <p>
     * 必须在 EDT 中执行，因为需要访问 VIRTUAL_FILE_ARRAY 数据键。
     * VIRTUAL_FILE_ARRAY 只能在 EDT 线程中安全访问。
     *
     * @return ActionUpdateThread.EDT 事件调度线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 必须在 EDT 中执行，因为 VIRTUAL_FILE_ARRAY 只能在 EDT 中访问
        return ActionUpdateThread.EDT;
    }

    /**
     * 更新操作的呈现信息, 设置文本和描述为生成 JavaDoc 的相关提示
     * <p>
     * 该方法用于在 IDE 中更新动作的显示文本和描述, 用于提示用户生成 JavaDoc
     *
     * @param e 事件对象, 包含当前操作的相关信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // 检查项目状态
        if (project == null || project.isDisposed() || DumbService.isDumb(project)) {
            return;
        }

        e.getPresentation().setText(JavaDocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavaDocBundle.message("action.generate.javadoc.selection.description"));
    }

    /**
     * 判断给定文件是否为Java文件
     * <p>
     * 通过检查文件的扩展名是否为"java"（不区分大小写）来判断文件类型
     *
     * @param file 要判断的文件对象
     * @return 如果文件是Java文件，返回true；否则返回false
     */
    private boolean isJavaFile(VirtualFile file) {
        return "java".equalsIgnoreCase(file.getExtension());
    }

}

