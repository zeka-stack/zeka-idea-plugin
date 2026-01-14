package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.javadoc.service.FileSelectionJavadocService;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;

/**
 * 为文件生成 Javadoc 的动作类
 * <p>
 * 该类继承自 AnAction, 用于在 IntelliJ IDEA 等 IDE 中提供生成 Javadoc 的功能.
 * 支持为单个 Java 文件或整个目录中的 Java 文件批量生成 Javadoc 注释.
 * 该动作可以处理选中的虚拟文件数组, 自动识别 Java 文件并为其生成相应的 Javadoc 注释.
 * 当处理大量文件时, 会显示确认对话框以避免误操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
@SuppressWarnings("DuplicatedCode")
public class GenerateJavadocForFilesAction extends AnAction {

    /**
     * 处理动作事件, 用于为选中的文件或目录生成 Javadoc 注释
     * <p>
     * 该方法首先获取当前项目和选中的文件列表, 若项目或文件为空则直接返回.
     * 然后收集所有需要生成 Javadoc 的任务, 检查任务是否为空, 若为空则返回.
     * 若任务数量较多, 会弹出确认对话框, 用户确认后才继续执行.
     * 最后调用文档生成服务, 生成 Javadoc 并显示完成信息.
     * <p>
     * 注意: 该方法继承自 {@code AnAction#actionPerformed}，其声明带有 {@link org.jetbrains.annotations.ApiStatus.OverrideOnly}，
     * 仅用于框架回调或子类重写。外部调用请使用 {@link FileSelectionJavadocService}。
     *
     * @param e 动作事件对象, 包含项目和选中的文件信息
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        new FileSelectionJavadocService().generateForFiles(project, files);
    }

    /**
     * 获取用于更新操作的线程类型
     * <p>
     * 该方法必须在 EDT(Event Dispatch Thread) 中执行, 因为需要访问 VIRTUAL_FILE_ARRAY 数据键.
     * VIRTUAL_FILE_ARRAY 只能在 EDT 线程中安全访问.
     *
     * @return ActionUpdateThread.EDT 表示事件调度线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 必须在 EDT 中执行，因为 VIRTUAL_FILE_ARRAY 只能在 EDT 中访问
        return ActionUpdateThread.EDT;
    }

    /**
     * 更新操作的呈现信息, 设置文本和描述为生成 Javadoc 的相关提示
     * <p>
     * 该方法用于在 IDE 中更新动作的显示文本和描述, 用于提示用户生成 Javadoc
     *
     * @param e 事件对象, 包含当前操作的相关信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        // 检查项目状态
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查项目是否处于索引模式
        if (DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setText(JavadocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavadocBundle.message("action.generate.javadoc.selection.description"));
    }

}
