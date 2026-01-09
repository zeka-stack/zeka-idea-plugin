package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.javadoc.service.JavadocDeletionService;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;

/**
 * 抽象删除 Javadoc 动作类
 * <p>
 * 提供删除 Javadoc 的基础功能, 子类可以继承此类实现具体的删除逻辑.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.6.0
 */
public abstract class AbstractDeleteJavadocAction extends AnAction {

    /** 删除服务实例 */
    protected final JavadocDeletionService deletionService = new JavadocDeletionService();

    /**
     * 检查项目是否处于索引模式
     * <p> 在索引期间，某些操作可能不可用。子类应该在 update 和 actionPerformed 方法中调用此方法进行检查。
     *
     * @param project 项目对象
     * @return 如果项目正在索引则返回 true，否则返回 false
     */
    protected static boolean isIndexing(@Nullable Project project) {
        return project != null && DumbService.isDumb(project);
    }

    /**
     * 检查并处理索引模式
     * <p> 如果项目正在索引，显示警告通知并返回 true。子类应该在 actionPerformed 方法开始时调用此方法。
     *
     * @param project 项目对象
     * @return 如果项目正在索引则返回 true，否则返回 false
     */
    protected static boolean checkAndHandleIndexing(@NotNull Project project) {
        if (isIndexing(project)) {
            NotificationUtil.notifyIndexing(project);
            return true;
        }
        return false;
    }
}

