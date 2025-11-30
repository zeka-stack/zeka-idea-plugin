package dev.dong4j.zeka.stack.idea.plugin.component;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.util.MavenUtil;

/**
 * Maven 缓存清理监听器
 * <p>
 * 该监听器实现了启动活动和项目管理器监听器接口, 负责在项目关闭时清理 Maven 缓存,
 * 确保项目资源的正确释放和缓存的一致性. 采用单例模式确保监听器只注册一次.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class MavenCacheCleanupListener implements StartupActivity, ProjectManagerListener {

    /** 是否已注册监听器（确保只注册一次） */
    private static volatile boolean listenerRegistered = false;

    /**
     * 启动活动：注册项目关闭监听器
     * <p>
     * 在插件启动时注册全局的项目关闭监听器。
     *
     * @param project 启动的项目
     */
    @Override
    public void runActivity(@NotNull Project project) {
        // 确保只注册一次（使用双重检查锁定）
        if (!listenerRegistered) {
            synchronized (MavenCacheCleanupListener.class) {
                if (!listenerRegistered) {
                    // 注册全局监听器，使用应用级别的 Disposable 管理生命周期
                    ApplicationManager.getApplication().getMessageBus()
                        .connect(ApplicationManager.getApplication())
                        .subscribe(ProjectManager.TOPIC, this);
                    listenerRegistered = true;
                }
            }
        }
    }

    /**
     * 项目关闭时调用
     * <p>
     * 清理该项目的 Maven 版本号缓存。
     *
     * @param project 关闭的项目
     */
    @Override
    public void projectClosing(@NotNull Project project) {
        MavenUtil.clearCache(project);
    }
}

