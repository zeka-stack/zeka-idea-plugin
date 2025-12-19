package dev.dong4j.zeka.stack.idea.plugin.common.codefree;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.PluginDisposable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

/**
 * 本地 Nacos 应用生命周期监听器
 * <p>
 * 用于监听项目生命周期事件, 并在项目关闭时处理与本地 Nacos 注册中心相关的清理逻辑, 如停止本地注册中心服务.
 * 该类实现了 ProjectActivity 和 ProjectManagerListener 接口, 确保在项目启动和关闭时执行相应的操作.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.11
 * @since 1.0.0
 */
public class LocalAgentAppLifecycleListener implements ProjectActivity, ProjectManagerListener {
    /**
     * 标记是否已注册监听器
     * <p>
     * 用于确保监听器只注册一次, 防止重复注册导致的问题
     */
    private static volatile boolean listenerRegistered = false;

    /**
     * 执行插件生命周期监听逻辑
     * <p>
     * 注册项目管理器的监听器, 并在项目生命周期事件中使用指定的 continuation 处理逻辑.
     * 该方法确保监听器仅注册一次, 避免重复注册.
     *
     * @param project      当前项目实例
     * @param continuation 用于处理生命周期事件的延续对象
     * @return 返回 Unit 实例
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        Disposable parentDisposable = Disposer.newDisposable(PluginDisposable.getInstance(project), "LocalAgentAppLifecycleListener");
        // 确保只注册一次（使用双重检查锁定）
        if (!listenerRegistered) {
            synchronized (LocalAgentAppLifecycleListener.class) {
                if (!listenerRegistered) {
                    // 注册全局监听器，使用应用级别的 Disposable 管理生命周期
                    ApplicationManager.getApplication().getMessageBus()
                        .connect(parentDisposable)
                        .subscribe(ProjectManager.TOPIC, this);
                    listenerRegistered = true;
                }
            }
        }
        return Unit.INSTANCE;
    }

    /**
     * 在项目关闭时执行本地注册中心的清理操作
     * <p>
     * 当项目关闭时, 检查是否启用了本地注册中心, 如果启用了则停止对应的本地注册中心 (NACOS)
     *
     * @param project 当前关闭的项目实例
     */
    @Override
    public void projectClosing(@NotNull Project project) {
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                // todo-dong4j : (2025.12.19 12:20) [在项目关闭时执行, 而不是应用关闭]
                // CodefreeAgentManager manager = CodefreeAgentManager.getInstance();
                // if (manager.isRunning()) {
                //     manager.stopAgent();
                // }
            } catch (Exception ignored) {
            }
        });
    }

}

