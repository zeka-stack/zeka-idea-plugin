package dev.dong4j.zeka.stack.idea.plugin.common.codefree;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;

import java.nio.file.Files;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.CodefreeAgentSettings;

/**
 * Codefree 代理生命周期监听器
 * <p>
 * 负责在应用启动和关闭时管理 Codefree 代理的自动启动和停止。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
@Service(Service.Level.APP)
public final class CodefreeAgentLifecycleListener implements Disposable {
    private static final Logger LOG = Logger.getInstance(CodefreeAgentLifecycleListener.class);

    /**
     * 构造函数
     * <p>
     * 在服务创建时注册应用关闭监听器，并在应用启动时检查是否需要自动启动代理。
     */
    public CodefreeAgentLifecycleListener() {
        // 注册应用关闭监听器
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
            AppLifecycleListener.TOPIC,
            new AppLifecycleListener() {
                @Override
                public void appClosing() {
                    dispose();
                }
            });

        // 延迟启动检查，确保配置已加载
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // 等待一小段时间，确保配置已完全加载
                    Thread.sleep(1000);
                    AIProviderSettings settings = AIProviderSettings.getInstance();
                    CodefreeAgentSettings codefreeSettings = settings.codefreeSettings;
                    if (codefreeSettings != null && codefreeSettings.autoStart) {
                        CodefreeAgentManager manager = CodefreeAgentManager.getInstance();
                        // 检查 jar 文件是否存在
                        if (Files.exists(manager.resolveJarPath(codefreeSettings))) {
                            LOG.info("自动启动 Codefree 代理（应用启动时）");
                            manager.startAgent(codefreeSettings);
                        } else {
                            LOG.warn("Codefree 代理 jar 文件不存在，跳过自动启动");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOG.warn("自动启动 Codefree 代理失败", e);
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
            CodefreeAgentManager manager = CodefreeAgentManager.getInstance();
            if (manager.isRunning()) {
                LOG.info("应用关闭时停止 Codefree 代理");
                manager.stopAgent();
            }
        } catch (Exception e) {
            LOG.warn("停止 Codefree 代理失败", e);
        }
    }
}
