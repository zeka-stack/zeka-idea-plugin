package dev.dong4j.zeka.stack.idea.plugin.common.agent;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Files;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;

/**
 * Intelli Agent Lifecycle Listener
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-24 23:26:28
 * @since hello.world
 */
@Service(Service.Level.APP)
public final class IntelliAgentLifecycleListener implements Disposable {
    private static final Logger LOG = Logger.getInstance(IntelliAgentLifecycleListener.class);
    private static volatile boolean shutdownHookRegistered = false;

    /**
     * 构造函数
     * <p>
     * 在服务创建时注册应用关闭监听器，并在应用启动时检查是否需要自动启动代理。
     */
    public IntelliAgentLifecycleListener() {
        // 使用 Runtime.addShutdownHook 作为最可靠的关闭钩子（JVM 级别）
        if (!shutdownHookRegistered) {
            synchronized (IntelliAgentLifecycleListener.class) {
                if (!shutdownHookRegistered) {
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        try {
                            LOG.info("JVM 关闭钩子执行：停止 IntelliAI Agent");
                            IntelliAgentManager manager = IntelliAgentManager.getInstance();
                            if (manager.isRunning()) {
                                manager.stopAgent();
                            }
                        } catch (Exception e) {
                            LOG.warn("JVM 关闭钩子中停止 IntelliAI Agent 失败", e);
                        }
                    }, "IntelliAgentShutdownHook"));
                    shutdownHookRegistered = true;
                    LOG.info("已注册 JVM 关闭钩子");
                }
            }
        }

        // 同时注册 AppLifecycleListener 作为备用方案
        try {
            ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
                AppLifecycleListener.TOPIC,
                new AppLifecycleListener() {
                    @Override
                    public void appClosing() {
                        LOG.info("AppLifecycleListener.appClosing 执行：停止 IntelliAI Agent");
                        dispose();
                    }
                });
            LOG.info("已注册 AppLifecycleListener");
        } catch (Exception e) {
            LOG.warn("注册 AppLifecycleListener 失败", e);
        }

        // 延迟启动检查，确保配置已加载
        ApplicationManager.getApplication().invokeLater(() -> {
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
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOG.warn("自动启动 IntelliAI Agent 失败", e);
                }
            });
        });
    }

    /**
     * 应用关闭时的清理操作
     * <p>
     * 通过 Disposable 机制在应用关闭时自动调用。
     */
    @Override
    public void dispose() {
        try {
            LOG.info("dispose() 方法被调用：停止 IntelliAI Agent");
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
