package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 编辑器后续编辑服务接口
 * <p> 用于管理编辑器的后续编辑功能, 提供启用, 禁用编辑器以及获取跟踪器的功能
 * <p> 该接口通过单例模式获取实例, 并与特定项目绑定
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
public interface NextEditService {
    /**
     * 获取与指定项目关联的 NextEditService 实例
     * <p> 通过 Project 的服务管理机制获取当前项目的 NextEditService 单例对象
     *
     * @param project 目标项目, 不能为 null
     * @return 与该项目关联的 NextEditService 实例
     */
    static NextEditService getInstance(@NotNull Project project) {
        return project.getService(NextEditService.class);
    }

    /**
     * 启用编辑器的下一步编辑功能
     * <p> 为指定编辑器启用下一步编辑跟踪和相关功能, 通常用于支持编辑器的智能补全, 自动完成等特性
     *
     * @param editor 要启用功能的编辑器实例, 不能为 null
     */
    void enableEditor(@NotNull Editor editor);

    /**
     * 禁用指定的编辑器
     * <p> 此方法用于禁用给定的编辑器, 使其不可编辑
     *
     * @param editor 要禁用的编辑器, 不能为 null
     */
    void disableEditor(@NotNull Editor editor);

    /**
     * 获取指定编辑器的跟踪器
     * <p> 根据传入的编辑器对象, 返回对应的跟踪器实例. 如果编辑器未被跟踪, 则返回 null.
     *
     * @param editor 编辑器对象, 不能为 null
     * @return 对应的跟踪器实例, 如果编辑器未被跟踪则返回 null
     */
    @Nullable
    NextEditTracker getTracker(@NotNull Editor editor);
}
