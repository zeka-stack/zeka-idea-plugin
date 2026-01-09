package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import lombok.extern.slf4j.Slf4j;

/**
 * 生成 Javadoc 快捷操作类
 * <p>
 * 该类继承自 AbstractGenerateJavaDocAction, 用于处理 Javadoc 生成的快捷操作.
 * 它支持在编辑器中对当前文件生成 Javadoc, 也支持在项目视图中对选中的文件或目录生成 Javadoc.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
@Slf4j
public class GenerateJavadocShortcutAction extends AbstractGenerateJavaDocAction {

    /**
     * 更新动作的可见性和启用状态
     * <p> 根据当前编辑器或文件上下文判断是否支持生成 Javadoc, 设置动作的启用状态和可见性.
     * <p> 如果当前文件是 Java 文件或 Kotlin 文件, 且在 Kotlin 语言支持开启时, 将动作设为可用.
     * <p> 同时设置动作的显示文本和描述信息, 用于 UI 展示.
     *
     * @param e 动作事件对象, 不能为空, 用于获取当前上下文信息
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        
        // 检查项目是否处于索引模式
        if (project != null && DumbService.isDumb(project)) {
            e.getPresentation().setEnabled(false);
            e.getPresentation().setVisible(true);
            return;
        }
        
        boolean isSupportedFile = false;
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (psiFile != null || (files != null && files.length > 0)) {
            isSupportedFile = true;
        }

        e.getPresentation().setEnabled(isSupportedFile);
        e.getPresentation().setVisible(isSupportedFile);
        e.getPresentation().setText(JavadocBundle.message("action.generate.javadoc"));
        e.getPresentation().setDescription(JavadocBundle.message("action.generate.javadoc.description"));
    }

    /**
     * 处理动作事件
     * <p>
     * 该方法用于处理用户触发的动作事件.
     * 首先尝试获取当前编辑器, 如果存在编辑器, 则调用父类的 process 方法处理当前文件.
     * 如果没有编辑器, 则尝试获取选中的文件或目录, 并委托给 GenerateJavadocForFilesAction 处理.
     *
     * @param e 动作事件对象, 不能为空
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            // 如果有编辑器，说明是在编辑区触发，按原有逻辑处理（处理当前文件或元素）
            process(e, true);
        } else {
            // 如果没有编辑器，检查是否有选中的文件（例如在 Project 视图中选中了目录或文件）
            VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
            if (files != null && files.length > 0) {
                // 委托给 GenerateJavadocForFilesAction 处理批量文件/目录
                new GenerateJavadocForFilesAction().actionPerformed(e);
            } else {
                // 既没有编辑器也没有选中文件，尝试按原有逻辑处理（可能会因为没有文件而直接返回）
                process(e, false);
            }
        }
    }
}
