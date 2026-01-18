package dev.dong4j.zeka.stack.idea.javadoc.component;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Disposer;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.javadoc.PluginDisposable;
import dev.dong4j.zeka.stack.idea.javadoc.util.ProjectVersionResolver;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 版本号缓存清理监听器
 * <p>
 * 该监听器实现了项目活动和项目管理器监听器接口, 负责在项目关闭时清理版本号缓存,
 * 确保项目资源的正确释放和缓存的一致性.
 * <p>
 * 使用项目级别的服务作为 Disposable 父对象, 符合 IntelliJ 平台最佳实践.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class ProjectVersionCacheCleanupListener implements ProjectActivity, ProjectManagerListener {

    /** 标记执行状态, 防止重复初始化监听器 */
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    /**
     * 启动活动: 注册项目关闭监听器
     * <p>
     * 在插件启动时注册项目关闭监听器, 使用项目级别的服务作为 Disposable 管理生命周期.
     * 使用应用级别的 MessageBus 订阅 ProjectManager.TOPIC, 但使用项目服务作为 Disposable.
     * <p>
     * 当项目关闭时, 服务会被自动释放, 监听器也会自动取消订阅.
     *
     * @param project      启动的项目
     * @param continuation Kotlin 协程 continuation
     * @return Unit 对象
     */
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 获取项目级别的服务作为 Disposable 父对象
        Disposable parentDisposable = Disposer.newDisposable(PluginDisposable.getInstance(project), "ProjectVersionCacheCleanupListener");

        // 只在第一次运行时检查更新
        if (!hasRun.compareAndSet(false, true) || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }

        // 使用应用级别的 MessageBus 订阅 ProjectManager.TOPIC（应用级别的话题）
        // 使用项目级别的 Disposable 作为父对象
        // 当项目关闭时，Disposable 会被自动释放，监听器也会自动取消订阅
        ApplicationManager.getApplication().getMessageBus()
            .connect(parentDisposable)
            .subscribe(ProjectManager.TOPIC, this);
        return Unit.INSTANCE;
    }

    /**
     * 项目关闭时调用
     * <p>
     * 清理该项目的版本号缓存。
     *
     * @param project 关闭的项目
     */
    @Override
    public void projectClosing(@NotNull Project project) {
        ProjectVersionResolver.clearCache(project);
    }
}
