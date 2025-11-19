package dev.dong4j.zeka.stack.idea.plugin.nacos.local;

import com.alibabacloud.intellij.UserCancelException;
import com.alibabacloud.intellij.model.edas.LocalRegistry;
import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryConstants;
import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryContext;
import com.alibabacloud.intellij.service.edas.registry.local.LocalRegistryManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;

import java.util.concurrent.CompletableFuture;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * 本地 Nacos 注册中心服务
 * <p>
 * 负责下载、启动与停止嵌入式 Nacos，并通过通知反馈结果。
 *
 * @author dong4j
 * @since 1.2.0
 */
public final class LocalNacosService {
    private static final Logger LOG = Logger.getInstance(LocalNacosService.class);
    private static final LocalNacosService INSTANCE = new LocalNacosService();

    private LocalNacosService() {
    }

    /**
     * 获取服务实例
     *
     * @return 单例
     */
    public static LocalNacosService getInstance() {
        return INSTANCE;
    }

    /**
     * 启动本地 Nacos 注册中心
     *
     * @return 异步任务
     */
    public CompletableFuture<Void> startLocalRegistry() {
        return CompletableFuture.runAsync(() -> {
            LocalRegistryContext context = new LocalRegistryContext();
            context.setRegistry(LocalRegistry.NACOS);
            try {
                LocalRegistryManager.startRegistryFromPreferencePage(context);
                boolean startedByOthers = Boolean.TRUE.equals(context.getStartedByOtherOwner());
                if (startedByOthers) {
                    NotificationUtil.showInfo(null,
                                              NacosBundle.message("notification.local.nacos.start.running",
                                                                  LocalRegistryConstants.NACOS_TEST_URL));
                } else {
                    NotificationUtil.showInfo(null,
                                              NacosBundle.message("notification.local.nacos.start.success",
                                                                  LocalRegistryConstants.NACOS_TEST_URL));
                }
            } catch (UserCancelException e) {
                NotificationUtil.showWarning(null, NacosBundle.message("notification.local.nacos.start.cancel"));
            } catch (Exception e) {
                LOG.warn("Failed to start local Nacos", e);
                NotificationUtil.showError(null,
                                           NacosBundle.message("notification.local.nacos.start.failed", e.getMessage()));
            }
        }, AppExecutorUtil.getAppExecutorService());
    }

    /**
     * 停止本地 Nacos 注册中心
     *
     * @return 异步任务
     */
    public CompletableFuture<Void> stopLocalRegistry() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!LocalRegistryManager.isRegisterDownloaded(LocalRegistry.NACOS)) {
                    NotificationUtil.showWarning(null,
                                                 NacosBundle.message("notification.local.nacos.stop.not.ready"));
                    return;
                }

                if (!LocalRegistryManager.localRegistryStarted(LocalRegistry.NACOS)) {
                    NotificationUtil.showInfo(null,
                                              NacosBundle.message("notification.local.nacos.stop.not.running"));
                    return;
                }

                LocalRegistryManager.stopRegistry(LocalRegistry.NACOS);
                NotificationUtil.showInfo(null, NacosBundle.message("notification.local.nacos.stop.success"));
            } catch (Exception e) {
                LOG.warn("Failed to stop local Nacos", e);
                NotificationUtil.showError(null,
                                           NacosBundle.message("notification.local.nacos.stop.failed", e.getMessage()));
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
}

