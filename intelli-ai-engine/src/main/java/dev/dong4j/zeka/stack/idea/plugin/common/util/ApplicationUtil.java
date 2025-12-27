
package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;

import org.jetbrains.annotations.Nullable;

/**
 * 应用程序工具类
 * <p>提供与当前项目相关的实用方法, 包括查找当前有效的项目. 该类主要用于在集成开发环境 (IDE) 中获取当前活动项目的实例.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.26
 * @since 1.0.0
 */
public final class ApplicationUtil {
    /**
     * 查找当前有效的项目
     * <p> 首先尝试获取最近聚焦的 IDE 框架中的项目. 如果该项目有效, 则返回该项目.
     * 如果无效, 则遍历所有打开的项目, 返回第一个有效的项目.
     * 如果没有找到有效的项目, 则返回 null.
     *
     * @return 当前有效的项目, 如果没有找到则返回 null
     */
    public static @Nullable Project findCurrentProject() {
        IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
        Project project = frame != null ? frame.getProject() : null;
        if (isValidProject(project)) {
            return project;
        } else {
            for (Project p : ProjectManager.getInstance().getOpenProjects()) {
                if (isValidProject(p)) {
                    return p;
                }
            }

            return null;
        }
    }

    /**
     * 检查项目是否有效
     * <p> 判断给定的项目是否为非空, 未被销毁且不是默认项目
     *
     * @param project 要检查的项目
     * @return 如果项目有效则返回 true, 否则返回 false
     */
    private static boolean isValidProject(@Nullable Project project) {
        return project != null && !project.isDisposed() && !project.isDefault();
    }
}
