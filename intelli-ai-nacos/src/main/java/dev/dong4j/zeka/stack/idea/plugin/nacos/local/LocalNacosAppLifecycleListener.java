package dev.dong4j.zeka.stack.idea.plugin.nacos.local;

import com.alibabacloud.intellij.model.edas.LocalRegistry;
import com.alibabacloud.intellij.service.edas.registry.local.LocalRegistryManager;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;

/**
 * IDE 生命周期监听器，在 IDE 关闭或重启时停止本地 Nacos。
 *
 * @author dong4
 * @since 1.2.0
 */
public class LocalNacosAppLifecycleListener implements AppLifecycleListener {
    private static final Logger LOG = Logger.getInstance(LocalNacosAppLifecycleListener.class);
    private final AtomicBoolean shutdownTriggered = new AtomicBoolean(false);

    @Override
    public void appClosing() {
        triggerShutdown();
    }

    @Override
    public void appWillBeClosed(boolean isRestart) {
        triggerShutdown();
    }

    private void triggerShutdown() {
        if (!shutdownTriggered.compareAndSet(false, true)) {
            return;
        }
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
                LOG.warn("Failed to stop local Nacos during IDE shutdown", ex);
            }
        });
    }
}

