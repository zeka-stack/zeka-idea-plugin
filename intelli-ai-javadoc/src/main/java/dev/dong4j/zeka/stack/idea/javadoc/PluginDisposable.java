package dev.dong4j.zeka.stack.idea.javadoc;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * 插件资源释放类
 * <p>
 * 提供插件相关资源的释放功能, 支持在应用级别和项目级别获取实例, 并实现资源的清理操作.
 * 该类为最终类, 不允许继承, 适用于需要在插件生命周期中进行资源管理的场景.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.12.11
 * @since 1.0.0
 */
@Service( {Service.Level.APP, Service.Level.PROJECT})
public final class PluginDisposable implements Disposable {
    /**
     * 获取单例的 Disposable 实例
     * <p>
     * 通过 ApplicationManager 获取当前应用的 PluginDisposable 服务实例
     *
     * @return 不可为空的 Disposable 实例
     */
    public static @NotNull Disposable getInstance() {
        return ApplicationManager.getApplication().getService(PluginDisposable.class);
    }

    /**
     * 获取与指定项目关联的 Disposable 实例
     * <p>
     * 通过给定的项目对象获取对应的 PluginDisposable 服务实例
     *
     * @param project 项目对象, 不能为空
     * @return 与项目关联的 Disposable 实例, 不会为 null
     */
    public static @NotNull Disposable getInstance(@NotNull Project project) {
        return project.getService(PluginDisposable.class);
    }

    /**
     * 释放资源
     * <p>
     * 用于清理或释放对象占用的资源, 通常在对象不再使用时调用
     */
    @Override
    public void dispose() {
    }
}
