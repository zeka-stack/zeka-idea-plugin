package dev.dong4j.zeka.stack.idea.plugin.common.nextedit;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 下一步编辑协调器
 * <p> 负责管理每个编辑器的下一步编辑追踪器 (NextEditTracker), 确保在编辑器激活或关闭时正确初始化或释放资源.
 * <p> 该类为不可变对象, 使用 final 修饰, 避免被继承. 通过项目上下文创建, 并维护一个线程安全的映射表用于存储编辑器与其对应的追踪器实例.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.05
 * @since 1.0.0
 */
final class NextEditCoordinator implements Disposable {
    /** 项目实例, 用于访问项目相关资源和功能 */
    private final Project project;
    /**
     * 存储编辑器与对应的 NextEditTracker 的映射关系
     * <p> 用于跟踪每个编辑器的下一次编辑操作状态
     *
     * @see Editor
     * @see NextEditTracker
     */
    private final Map<Editor, NextEditTracker> trackers = new ConcurrentHashMap<>();

    /**
     * 初始化下一个编辑协调器
     * <p> 创建一个新的 NextEditCoordinator 实例, 绑定到指定的项目上下文
     *
     * @param project 项目实例, 不能为 null
     */
    NextEditCoordinator(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 启用指定编辑器的下一次编辑跟踪
     * <p> 为指定编辑器创建并注册一个 NextEditTracker 实例, 用于跟踪该编辑器的下一次编辑操作
     *
     * @param editor 要启用跟踪的编辑器, 不能为 null
     */
    void enableEditor(@NotNull Editor editor) {
        trackers.computeIfAbsent(editor, ed -> {
            NextEditTracker tracker = new NextEditTracker(project, ed);
            Disposer.register(this, tracker);
            return tracker;
        });
    }

    /**
     * 禁用指定编辑器的后续编辑跟踪器
     * <p> 从跟踪器映射中移除指定编辑器的跟踪器实例, 若存在则调用其 dispose 方法释放资源
     *
     * @param editor 要禁用的编辑器实例, 不能为 null
     */
    void disableEditor(@NotNull Editor editor) {
        NextEditTracker tracker = trackers.remove(editor);
        if (tracker != null) {
            Disposer.dispose(tracker);
        }
    }

    /**
     * 获取指定编辑器的下一个编辑追踪器
     * <p> 根据编辑器对象查找对应的 NextEditTracker 实例, 如果不存在则返回 null
     *
     * @param editor 编辑器对象, 不能为 null
     * @return 对应的 NextEditTracker 实例, 如果未注册或已禁用则返回 null
     */
    @Nullable
    NextEditTracker getTracker(@NotNull Editor editor) {
        return trackers.get(editor);
    }

    /**
     * 释放资源
     * <p> 遍历并销毁所有编辑器跟踪器, 然后清空跟踪器映射
     * <p>
     * 该方法在当前对象不再被需要时调用, 以确保所有相关资源被正确释放
     *
     * @since 1.0
     */
    @Override
    public void dispose() {
        for (NextEditTracker tracker : trackers.values()) {
            Disposer.dispose(tracker);
        }
        trackers.clear();
    }
}
