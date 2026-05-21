package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 项目级的下一步编辑服务实现类
 * <p> 该类用于管理项目级别的下一步编辑功能, 负责启用和禁用编辑器以及跟踪编辑器状态.
 * <p> 主要职责包括初始化协调器, 处理编辑器启禁操作以及清理资源.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
@Service(Service.Level.PROJECT)
public final class NextEditServiceImpl implements NextEditService, Disposable {
    /** 项目实例, 用于获取与当前操作相关的项目信息 */
    private final Project project;
    /**
     * NextEdit 协调器实例
     * <p> 用于管理所有编辑器的 NextEdit 功能启停和跟踪逻辑
     *
     * @see NextEditCoordinator
     */
    private final NextEditCoordinator coordinator;

    /**
     * 初始化 NextEditServiceImpl 实例
     * <p> 通过传入的 Project 对象初始化项目相关资源, 并启用所有已打开的编辑器
     *
     * @param project 项目对象, 不能为 null
     */
    public NextEditServiceImpl(@NotNull Project project) {
        this.project = project;
        this.coordinator = new NextEditCoordinator(project);
        Disposer.register(this, coordinator);
        enableAllOpenEditors();
    }

    /**
     * 启用指定的编辑器
     * <p> 检查编辑器是否属于当前项目, 如果属于, 则启用该编辑器
     *
     * @param editor 要启用的编辑器, 不能为 null
     */
    @Override
    public void enableEditor(@NotNull Editor editor) {
        if (editor.getProject() == null || editor.getProject() != project) {
            return;
        }
        coordinator.enableEditor(editor);
    }

    /**
     * 禁用指定编辑器
     * <p> 调用协调器禁用指定编辑器, 仅当编辑器所属项目与当前服务项目一致时生效
     *
     * @param editor 要禁用的编辑器, 不能为 null
     */
    @Override
    public void disableEditor(@NotNull Editor editor) {
        coordinator.disableEditor(editor);
    }

    /**
     * 获取指定编辑器的编辑跟踪器
     * <p> 根据传入的编辑器对象, 从协调器中获取对应的编辑跟踪器实例
     * <p> 如果该编辑器没有对应的跟踪器, 则返回 null
     *
     * @param editor 编辑器对象, 不能为 null
     * @return 编辑跟踪器实例, 如果不存在则返回 null
     */
    @Override
    public @Nullable NextEditTracker getTracker(@NotNull Editor editor) {
        return coordinator.getTracker(editor);
    }

    /**
     * 启用项目中的所有打开编辑器
     * <p> 遍历所有打开的编辑器, 并启用属于当前项目的编辑器
     *
     * @since 1.0
     */
    private void enableAllOpenEditors() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (project.equals(editor.getProject())) {
                coordinator.enableEditor(editor);
            }
        }
    }

    /**
     * 释放资源
     * <p> 调用协调器的 dispose 方法来释放与当前项目相关的所有资源
     * <p>
     * 该方法通常在服务生命周期结束时被调用, 例如在项目关闭或服务销毁时.
     */
    @Override
    public void dispose() {
        // coordinator 已注册为当前项目级服务的子 disposable, 由 Disposer 统一释放。
    }
}
