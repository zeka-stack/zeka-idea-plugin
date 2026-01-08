package dev.dong4j.zeka.stack.idea.plugin.common.update;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.updateSettings.impl.PluginDownloader;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.concurrency.annotations.RequiresEdt;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;

/**
 * 插件更新器类
 * <p> 用于检查和管理插件的更新. 该类提供了静态方法来检查可用的插件更新, 并在检测到更新时通知用户.
 * 包含一个内部任务类 `CheckUpdatesTask`, 用于在后台线程中执行更新检查.
 * <p>
 * 主要功能包括:
 * - 检查插件更新
 * - 提供更新通知
 * - 安装插件更新
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.27
 * @since 1.0.0
 */
public class PluginUpdater {
    /**
     * 记录插件更新检查过程中的日志信息
     * <p>
     * 使用 Logger 类来记录调试, 错误等日志信息, 帮助开发者追踪插件更新检查的行为和问题
     *
     * @see Logger
     */
    private static final Logger LOG = Logger.getInstance(PluginUpdater.class);
    /**
     * 插件的唯一标识符
     * <p>
     * 用于标识和区分不同的插件
     *
     * @see PluginId
     */
    private static final PluginId PLUGIN_ID = PluginId.getId(EngineContents.PLUGIN_ID);

    /**
     * 扩展点名称：插件更新信息提供者
     * <p>
     * 用于获取所有注册的子插件信息，以便检查这些插件的更新
     */
    private static final ExtensionPointName<PluginUpdateInfoProvider> EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.ai.pluginUpdateInfoProvider");

    /**
     * 检查插件更新
     * <p> 此方法在后台线程中执行插件更新检查. 如果自动更新检查被禁用, 则直接返回. 否则, 查找可用的插件更新, 并在发现更新时通知用户.
     *
     * @param project 项目对象
     * @see #findAvailableUpdates(ProgressIndicator)
     * @see #findPluginUpdates(Collection)
     * @see #getPluginIdsToCheck()
     * @see #notifyUpdateAvailable(Project, PluginDownloader)
     * @see #installUpdate(Project, PluginDownloader)
     * @since 1.0.0
     */
    @RequiresBackgroundThread
    public static void checkForUpdates(@NotNull Project project) {
        // 检查是否启用自动更新
        AIProviderSettings settings = AIProviderSettings.getInstance();
        if (!settings.lastUpdateCheck) {
            LOG.debug("自动更新检查已禁用，跳过更新检查");
            return;
        }

        try {
            ProgressIndicator indicator = new EmptyProgressIndicator();
            Collection<PluginDownloader> availableUpdates = findAvailableUpdates(indicator);
            Collection<PluginDownloader> pluginUpdates = findPluginUpdates(availableUpdates);

            if (!pluginUpdates.isEmpty()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    for (PluginDownloader pluginUpdate : pluginUpdates) {
                        notifyUpdateAvailable(project, pluginUpdate);
                    }
                });
            }
        } catch (Exception e) {
            LOG.debug("检查插件更新失败", e);
        }
    }

    /**
     * 查找可用的更新
     * <p> 根据当前应用的构建信息, 调用 UpdaterChecker 找到所有可用的插件更新
     *
     * @param indicator 进度指示器, 用于显示查找更新的进度
     * @return 可用的更新列表
     */
    @NotNull
    private static Collection<PluginDownloader> findAvailableUpdates(@NotNull ProgressIndicator indicator) {
        BuildNumber buildNumber = ApplicationInfo.getInstance().getBuild();
        int baselineVersion = buildNumber.getBaselineVersion();
        return UpdaterChecker.findAvailableUpdates(indicator);
    }

    /**
     * 获取所有需要检查更新的插件 ID
     * <p>
     * 包括 engine 插件本身和所有通过扩展点注册的子插件
     *
     * @return 插件 ID 集合
     */
    @NotNull
    private static Set<PluginId> getPluginIdsToCheck() {
        Set<PluginId> pluginIds = new HashSet<>();
        // 添加 engine 插件本身
        pluginIds.add(PLUGIN_ID);
        // 从扩展点获取所有注册的子插件 ID
        try {
            for (PluginUpdateInfoProvider provider : EP_NAME.getExtensionList()) {
                try {
                    PluginId pluginId = provider.getPluginId();
                    pluginIds.add(pluginId);
                    LOG.debug("注册插件更新检查: " + pluginId.getIdString());
                } catch (Exception e) {
                    LOG.debug("获取插件更新信息失败", e);
                }
            }
        } catch (Exception e) {
            // 如果扩展点不可用（例如在插件加载早期），只记录警告，不影响基本功能
            LOG.debug("扩展点不可用，仅检查 engine 插件更新: " + e.getMessage());
        }
        return pluginIds;
    }

    /**
     * 查找所有需要检查的插件的更新
     * <p>
     * 从可用的更新列表中过滤出与需要检查的插件 ID 匹配的更新对象.
     * 包括 engine 插件本身和所有通过扩展点注册的子插件.
     *
     * @param availableUpdates 可用的更新列表
     * @return 匹配的插件更新对象列表
     */
    @NotNull
    private static Collection<PluginDownloader> findPluginUpdates(@NotNull Collection<PluginDownloader> availableUpdates) {
        Set<PluginId> pluginIdsToCheck = getPluginIdsToCheck();
        return availableUpdates.stream()
            .filter(p -> pluginIdsToCheck.contains(p.getId()))
            .collect(Collectors.toList());
    }

    /**
     * 通知用户有关插件更新的信息
     * <p> 创建一个通知, 告知用户有可用的插件更新, 并提供安装或忽略更新的操作选项
     *
     * @param project      项目对象
     * @param pluginUpdate 插件更新对象, 包含更新的相关信息
     * @since 1.0.0
     */
    @RequiresEdt
    private static void notifyUpdateAvailable(@NotNull Project project, @NotNull PluginDownloader pluginUpdate) {
        String title = AICommonBundle.message("plugin.update.available.title");
        String message = AICommonBundle.message("plugin.update.available.message", pluginUpdate.getPluginName());
        Notification notification = NotificationUtil.getNotificationGroup().createNotification(
            title,
            message,
            NotificationType.IDE_UPDATE);

        notification.addAction(NotificationAction.createSimple(
            AICommonBundle.message("plugin.update.install"),
            () -> installUpdate(project, pluginUpdate)
                                                              ));

        notification.addAction(NotificationAction.createSimple(
            AICommonBundle.message("plugin.update.ignore"),
            () -> {
                // 禁用自动更新检查
                AIProviderSettings settings = AIProviderSettings.getInstance();
                settings.lastUpdateCheck = false;
            }));

        notification.notify(project);
    }

    /**
     * 安装插件更新
     * <p> 在后台线程中执行插件更新操作, 并在完成后通知用户更新结果
     *
     * @param project      项目对象
     * @param pluginUpdate 插件更新对象
     */
    private static void installUpdate(@NotNull Project project, @NotNull PluginDownloader pluginUpdate) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                UpdateSettings settingsCopy = new UpdateSettings();
                settingsCopy.getState().copyFrom(UpdateSettings.getInstance().getState());
                settingsCopy.getState().setCheckNeeded(true);
                settingsCopy.getState().setPluginsCheckNeeded(true);
                settingsCopy.getState().setThirdPartyPluginsAllowed(true);
                settingsCopy.getState().setShowWhatsNewEditor(false);

                UpdateChecker.updateAndShowResult(project, settingsCopy);
            } catch (Exception e) {
                LOG.debug("安装插件更新失败", e);
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.showError(project, AICommonBundle.message("plugin.update.install.failed", e.getMessage()));
                });
            }
        });
    }

    /**
     * 检查更新的任务类
     * <p> 该类继承自 Task.Backgroundable, 用于在后台线程中检查插件更新. 在运行时调用 checkForUpdates 方法来执行具体的更新检查逻辑.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2025.12.27
     * @since 1.0.0
     */
    public static class CheckUpdatesTask extends com.intellij.openapi.progress.Task.Backgroundable {
        /**
         * 当前操作所涉及的项目实例
         *
         * @see Project
         */
        private final Project project;

        /**
         * 构造函数, 初始化检查更新任务
         * <p> 调用父类构造函数并设置项目信息
         *
         * @param project 当前项目实例
         */
        public CheckUpdatesTask(@NotNull Project project) {
            super(project, AICommonBundle.message("plugin.update.checking"), true);
            this.project = project;
        }

        /**
         * 执行检查更新的任务
         * <p> 在后台线程中运行, 用于检查更新. 此方法会调用 checkForUpdates 方法来执行具体的更新检查操作.
         *
         * @param indicator 进度指示器, 用于报告任务的进度状态
         */
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            checkForUpdates(project);
        }
    }
}

