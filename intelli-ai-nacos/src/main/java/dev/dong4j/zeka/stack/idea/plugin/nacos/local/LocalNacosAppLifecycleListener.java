package dev.dong4j.zeka.stack.idea.plugin.nacos.local;

import com.alibabacloud.intellij.model.edas.LocalRegistry;
import com.alibabacloud.intellij.service.edas.registry.local.LocalRegistryManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.concurrency.AppExecutorUtil;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;

/**
 * IDE 生命周期监听器，在 IDE 关闭或重启时停止本地 Nacos。
 * <p>
 * 使用 StartupActivity 注册为全局监听器，监听项目关闭事件。
 *
 * @author dong4j
 * @since 1.2.0
 */
public class LocalNacosAppLifecycleListener implements StartupActivity, ProjectManagerListener {
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
            synchronized (LocalNacosAppLifecycleListener.class) {
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
     * 如果启用了本地 Nacos 注册中心，则停止本地 Nacos 服务。
     *
     * @param project 关闭的项目
     */
    @Override
    public void projectClosing(@NotNull Project project) {
        SettingsState settings = SettingsState.getInstance();
        if (!settings.useLocalRegistry) {
            return;
        }
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                if (LocalRegistryManager.localRegistryStarted(LocalRegistry.NACOS)) {
                    LocalRegistryManager.stopRegistry(LocalRegistry.NACOS);
                }
            } catch (Exception ex) {
                // 静默处理异常，避免影响 IDE 关闭
            }
        });
    }
}

