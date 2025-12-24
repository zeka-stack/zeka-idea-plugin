package dev.dong4j.zeka.stack.idea.plugin.common.agent;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.util.RunOnceUtil;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeFrame;

import org.jetbrains.annotations.NotNull;

import java.awt.Window;
import java.nio.file.Files;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;

/**
 * Intelli Agent Lifecycle Listener
 * <a href="https://plugins.jetbrains.com/docs/intellij/plugin-components.html#subscribing-to-events">...</a>
 * <a href="https://plugins.jetbrains.com/docs/intellij/ide-infrastructure.html#running-tasks-once">...</a>
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-24 23:26:28
 * @since hello.world
 */
public final class IntelliAgentLifecycleListener implements AppLifecycleListener, ApplicationActivationListener {
    private static final Logger LOG = Logger.getInstance(IntelliAgentLifecycleListener.class);
    private static final boolean shutdownHookRegistered = false;

    @Override
    public void appFrameCreated(@NotNull List<String> commandLineArgs) {
        RunOnceUtil.runOnceForApp("intelli-agent-lifecycle-listener", () -> {
            // 在应用启动时执行的初始化逻辑
            LOG.info("应用启动时执行的初始化逻辑");
        });
    }

    @Override
    public void welcomeScreenDisplayed() {
        RunOnceUtil.runOnceForApp("intelli-agent-lifecycle-listener", () -> {
            // 在欢迎屏幕显示时执行的初始化逻辑
            LOG.info("在欢迎屏幕显示时执行的初始化逻辑");
        });
    }

    @Override
    public void projectFrameClosed() {
        RunOnceUtil.runOnceForApp("intelli-agent-lifecycle-listener", () -> {
            // 在项目框架关闭时执行的清理逻辑
            LOG.info("在项目框架关闭时执行的清理逻辑");
        });
    }

    @Override
    public void projectOpenFailed() {
        RunOnceUtil.runOnceForApp("intelli-agent-lifecycle-listener", () -> {
            // 在项目打开失败时执行的清理逻辑
            LOG.info("在项目打开失败时执行的清理逻辑");
        });
    }

    @Override
    public void appClosing() {
        RunOnceUtil.runOnceForApp("intelli-agent-lifecycle-listener", () -> {
            // 在应用关闭时执行的清理逻辑
            LOG.info("在应用关闭时执行的清理逻辑");
        });
    }

    @Override
    public void appWillBeClosed(boolean isRestart) {
        RunOnceUtil.runOnceForApp("intelli-agent-lifecycle-listener", () -> {
            // 在应用即将关闭时执行的清理逻辑
            LOG.info("在应用即将关闭时执行的清理逻辑");
        });
    }

    /**
     * 应用程序失焦/聚焦时触发
     * <p> 当应用程序被激活时, 检查是否有打开的项目. 如果有, 则在后台线程中进行以下操作:
     * 1. 暂停 1 秒钟
     * 2. 获取 IntelliAI Agent 的设置
     * 3. 如果设置了自动启动且对应的 jar 文件存在, 则启动 IntelliAI Agent
     * 4. 如果设置了自动更新, 则启动更新检查器
     *
     * @param ideFrame 激活的应用程序框架
     */
    @Override
    public void applicationActivated(@NotNull IdeFrame ideFrame) {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            LOG.warn("No open projects found");
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 等待一小段时间，确保配置已完全加载
                Thread.sleep(1000);
                AIProviderSettings settings = AIProviderSettings.getInstance();
                IntelliAgentSettings intelliAgentSettings = settings.intelliAgentSettings;
                if (intelliAgentSettings != null && intelliAgentSettings.autoStart) {
                    IntelliAgentManager manager = IntelliAgentManager.getInstance();
                    // 检查 jar 文件是否存在
                    if (Files.exists(manager.resolveJarPath(intelliAgentSettings))) {
                        LOG.info("自动启动 IntelliAI Agent（应用启动时）");
                        manager.startAgent(intelliAgentSettings);
                    } else {
                        LOG.warn("IntelliAI Agent jar 文件不存在，跳过自动启动");
                    }
                }

                // 启动自动更新检查器
                if (intelliAgentSettings != null && intelliAgentSettings.autoUpdate) {
                    IntelliAgentUpdateChecker updateChecker = IntelliAgentUpdateChecker.getInstance();
                    updateChecker.start();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.warn("自动启动 IntelliAI Agent 失败", e);
            }
        });
    }

    @Override
    public void applicationDeactivated(@NotNull IdeFrame ideFrame) {
    }

    @Override
    public void delayedApplicationDeactivated(@NotNull Window ideFrame) {
    }

    /**
     * 构造函数
     * <p>
     * 在服务创建时注册应用关闭监听器，并在应用启动时检查是否需要自动启动代理。
     */
    // public IntelliAgentLifecycleListener() {
    //     // 使用 Runtime.addShutdownHook 作为最可靠的关闭钩子（JVM 级别）
    //     if (!shutdownHookRegistered) {
    //         synchronized (IntelliAgentLifecycleListener.class) {
    //             if (!shutdownHookRegistered) {
    //                 Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    //                     try {
    //                         LOG.info("JVM 关闭钩子执行：停止 IntelliAI Agent");
    //                         IntelliAgentManager manager = IntelliAgentManager.getInstance();
    //                         if (manager.isRunning()) {
    //                             manager.stopAgent();
    //                         }
    //                     } catch (Exception e) {
    //                         LOG.warn("JVM 关闭钩子中停止 IntelliAI Agent 失败", e);
    //                     }
    //                 }, "IntelliAgentShutdownHook"));
    //                 shutdownHookRegistered = true;
    //                 LOG.info("已注册 JVM 关闭钩子");
    //             }
    //         }
    //     }
    //
    //     // 同时注册 AppLifecycleListener 作为备用方案
    //     try {
    //         ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
    //             AppLifecycleListener.TOPIC,
    //             new AppLifecycleListener() {
    //                 @Override
    //                 public void appClosing() {
    //                     LOG.info("AppLifecycleListener.appClosing 执行：停止 IntelliAI Agent");
    //                     dispose();
    //                 }
    //             });
    //         LOG.info("已注册 AppLifecycleListener");
    //     } catch (Exception e) {
    //         LOG.warn("注册 AppLifecycleListener 失败", e);
    //     }
    //
    //     // 延迟启动检查，确保配置已加载
    //     ApplicationManager.getApplication().invokeLater(() -> {
    //         ApplicationManager.getApplication().executeOnPooledThread(() -> {
    //             try {
    //                 // 等待一小段时间，确保配置已完全加载
    //                 Thread.sleep(1000);
    //                 AIProviderSettings settings = AIProviderSettings.getInstance();
    //                 IntelliAgentSettings intelliAgentSettings = settings.intelliAgentSettings;
    //                 if (intelliAgentSettings != null && intelliAgentSettings.autoStart) {
    //                     IntelliAgentManager manager = IntelliAgentManager.getInstance();
    //                     // 检查 jar 文件是否存在
    //                     if (Files.exists(manager.resolveJarPath(intelliAgentSettings))) {
    //                         LOG.info("自动启动 IntelliAI Agent（应用启动时）");
    //                         manager.startAgent(intelliAgentSettings);
    //                     } else {
    //                         LOG.warn("IntelliAI Agent jar 文件不存在，跳过自动启动");
    //                     }
    //                 }
    //
    //                 // 启动自动更新检查器
    //                 if (intelliAgentSettings != null && intelliAgentSettings.autoUpdate) {
    //                     IntelliAgentUpdateChecker updateChecker = IntelliAgentUpdateChecker.getInstance();
    //                     updateChecker.start();
    //                 }
    //             } catch (InterruptedException e) {
    //                 Thread.currentThread().interrupt();
    //             } catch (Exception e) {
    //                 LOG.warn("自动启动 IntelliAI Agent 失败", e);
    //             }
    //         });
    //     });
    // }

    /**
     * 应用关闭时的清理操作
     * <p>
     * 通过 Disposable 机制在应用关闭时自动调用。
     */
    public void dispose() {
        try {
            LOG.info("dispose() 方法被调用：停止 IntelliAI Agent 和更新检查器");

            // 停止更新检查器
            IntelliAgentUpdateChecker updateChecker = IntelliAgentUpdateChecker.getInstance();
            updateChecker.stop();

            // 停止 Agent
            IntelliAgentManager manager = IntelliAgentManager.getInstance();
            if (manager.isRunning()) {
                LOG.info("检测到 IntelliAI Agent 正在运行，执行停止操作");
                manager.stopAgent();
                LOG.info("IntelliAI Agent 已停止");
            } else {
                LOG.info("IntelliAI Agent 未运行，无需停止");
            }
        } catch (Exception e) {
            LOG.warn("停止 IntelliAI Agent 失败", e);
        }
    }
}
