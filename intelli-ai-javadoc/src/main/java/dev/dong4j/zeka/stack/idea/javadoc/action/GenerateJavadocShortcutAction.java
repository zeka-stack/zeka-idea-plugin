package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

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
     * 处理动作事件
     * <p>
     * 该方法用于处理用户触发的动作事件.
     * 首先尝试获取当前编辑器, 如果存在编辑器, 则调用父类的 process 方法处理当前文件.
     * 如果没有编辑器, 则尝试获取选中的文件或目录, 并委托给 GenerateJavadocForFilesAction 处理.
     *
     * @param e 动作事件对象
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
