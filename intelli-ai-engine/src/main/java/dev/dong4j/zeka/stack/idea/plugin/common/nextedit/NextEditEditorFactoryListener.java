package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * 编辑器工厂监听器类
 * <p> 用于在编辑器创建和释放时注册或注销 NextEdit 功能模块, 确保其只在有效项目上下文中启用.
 * <p> 该类实现了 EditorFactoryListener 接口, 并在 editorCreated 和 editorReleased 方法中分别调用 enableEditor 和 disableEditor 方法, 以控制功能的生命周期.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
public final class NextEditEditorFactoryListener implements EditorFactoryListener {
    /**
     * 当编辑器创建时的回调处理
     * <p> 在编辑器实例化后, 获取项目上下文并启用该编辑器的 NextEdit 功能
     * <p> 如果项目为空则直接返回, 不进行任何操作
     *
     * @param event 编辑器创建事件, 不能为 null
     */
    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        if (project == null) {
            return;
        }
        NextEditService.getInstance(project).enableEditor(editor);
    }

    /**
     * 当编辑器释放时调用此方法
     * <p> 在编辑器被释放时禁用指定项目的 NextEdit 服务
     *
     * @param event 编辑器工厂事件, 包含编辑器信息
     */
    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Project project = editor.getProject();
        if (project == null) {
            return;
        }
        NextEditService.getInstance(project).disableEditor(editor);
    }
}
