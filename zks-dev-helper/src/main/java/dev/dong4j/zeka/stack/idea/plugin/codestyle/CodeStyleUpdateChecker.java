package dev.dong4j.zeka.stack.idea.plugin.codestyle;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.codestyle.CodeStyleDownloadManager.DownloadProgressListener;
import dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.settings.state.CodeStyleSettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.HelperBundle;

/**
 * Code Style Update Checker
 *
 * @author dong4j
 * @version hello.world
 * @date 2026-01-02 18:30:17
 * @since hello.world
 */
@Service(Service.Level.APP)
public final class CodeStyleUpdateChecker {
    private static final Logger LOG = Logger.getInstance(CodeStyleUpdateChecker.class);

    /** 首次检查延迟时间：1 分钟 */
    private static final long INITIAL_DELAY_MS = TimeUnit.MINUTES.toMillis(10);
    /** 检查间隔：1 小时 */
    private static final long CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);

    private Timer timer;
    /** 上次已通知的版本，用于避免重复通知同一版本 */
    private String lastNotifiedVersion;

    /**
     * 启动更新检查
     * <p>
     * 如果配置启用了自动更新, 则启动定时器进行定期检查. 首先停止之前的定时器, 然后根据配置的下载地址设置定时任务,
     * 每隔一小时检查一次是否有可用的新版本.
     *
     * @since 1.0.0
     */
    public void start() {
        stop(); // 确保先停止之前的定时器

        CodeStyleSettingsState settings = CodeStyleSettingsState.getInstance();
        CodeStyleSettingsState.CodeStyleUpdateSettings updateSettings = settings.getCodeStyleUpdateSettings();

        if (updateSettings == null || !updateSettings.isAutoUpdate()) {
            LOG.debug("代码样式自动更新检查未启用，跳过启动");
            return;
        }

        String downloadUrl = updateSettings.getDownloadUrl() != null ? updateSettings.getDownloadUrl().trim() : "";
        if (downloadUrl.isEmpty() || (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://"))) {
            LOG.debug("下载地址未配置或为本地路径，跳过自动更新检查");
            return;
        }

        LOG.debug("启动代码样式自动更新检查器，首次检查将在 1 分钟后执行，之后每 1 小时检查一次");

        timer = new Timer("CodeStyleUpdateChecker", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForUpdate();
            }
        }, INITIAL_DELAY_MS, CHECK_INTERVAL_MS);
    }

    /**
     * 停止更新检查
     */
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            lastNotifiedVersion = null; // 清除已通知版本记录
            LOG.debug("停止代码样式自动更新检查器");
        }
    }

    /**
     * 检查是否有新版本的代码样式可用
     * <p>
     * 此方法会尝试从指定的下载地址获取最新的代码样式文件名, 并与本地文件名进行比较. 如果发现新版本,
     * 则会调用 {@link #showUpdateNotification} 方法显示更新通知, 并更新已通知的版本记录.
     * 如果本地文件已经是最新版本, 则清除已通知的版本记录.
     */
    private void checkForUpdate() {
        // 每次检查时都重新读取最新配置
        CodeStyleSettingsState settings = CodeStyleSettingsState.getInstance();
        CodeStyleSettingsState.CodeStyleUpdateSettings updateSettings = settings.getCodeStyleUpdateSettings();

        if (updateSettings == null || !updateSettings.isAutoUpdate()) {
            // 如果配置已禁用，停止定时器
            stop();
            return;
        }

        String downloadUrl = updateSettings.getDownloadUrl() != null ? updateSettings.getDownloadUrl().trim() : "";
        if (downloadUrl.isEmpty() || (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://"))) {
            return;
        }

        try {
            // 获取最新版本文件名
            String latestFileName = CodeStyleDownloadManager.fetchLatestFileName(downloadUrl);
            if (latestFileName == null || latestFileName.isEmpty()) {
                LOG.debug("无法获取最新版本信息");
                return;
            }

            // 从文件名中提取版本号
            String latestVersion = CodeStyleDownloadManager.extractVersionFromFileName(latestFileName);
            if (latestVersion == null) {
                LOG.debug("无法从文件名中提取版本号: " + latestFileName);
                return;
            }

            // 获取本地版本
            String localVersion = CodeStyleDownloadManager.getLocalVersion();

            // 比较版本
            if (localVersion == null || !localVersion.equals(latestVersion)) {
                // 避免重复通知同一版本
                if (!latestVersion.equals(lastNotifiedVersion)) {
                    LOG.debug("发现新版本代码样式: " + latestVersion + " (当前版本: " + (localVersion != null ? localVersion : "无") + ")");
                    showUpdateNotification(latestVersion, localVersion, updateSettings);
                    lastNotifiedVersion = latestVersion;
                } else {
                    LOG.debug("已通知过该版本，跳过: " + latestVersion);
                }
            } else {
                LOG.debug("代码样式已是最新版本: " + latestVersion);
                // 如果本地已更新到最新版本，清除已通知版本记录
                lastNotifiedVersion = null;
            }
        } catch (Exception e) {
            LOG.debug("检查代码样式更新失败", e);
        }
    }

    /**
     * 显示更新通知
     *
     * @param latestVersion 最新版本号
     * @param localVersion  本地版本号
     * @param settings      更新配置
     */
    private void showUpdateNotification(@NotNull String latestVersion,
                                        @Nullable String localVersion,
                                        @NotNull CodeStyleSettingsState.CodeStyleUpdateSettings settings) {
        String message = localVersion != null
                         ? HelperBundle.message("settings.codestyle.update.available.with.current", latestVersion, localVersion)
                         : HelperBundle.message("settings.codestyle.update.available", latestVersion);

        Notification notification = NotificationUtil.getNotificationGroup(PluginContents.PLUGIN_NAME)
            .createNotification(
                HelperBundle.message("settings.codestyle.update.title"),
                message,
                NotificationType.INFORMATION);

        // 添加更新操作
        notification.addAction(new NotificationAction(
            HelperBundle.message("settings.codestyle.update.now")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e,
                                        @NotNull Notification notification) {
                // 触发后台下载更新
                Project project = ProjectManager.getInstance().getDefaultProject();
                downloadLatestVersion(project, settings);
                notification.expire();
            }
        });

        notification.notify(ProjectManager.getInstance().getDefaultProject());
    }

    /**
     * 下载最新版本
     *
     * @param project  项目对象
     * @param settings 更新配置
     */
    private void downloadLatestVersion(@Nullable Project project,
                                       @NotNull CodeStyleSettingsState.CodeStyleUpdateSettings settings) {
        String downloadUrl = settings.getDownloadUrl() != null ? settings.getDownloadUrl().trim() : "";
        if (downloadUrl.isEmpty()) {
            if (project != null) {
                NotificationUtil.showError(
                    project,
                    PluginContents.PLUGIN_NAME, HelperBundle.message("settings.codestyle.update.error.no.url"));
            }
            return;
        }

        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            if (project != null) {
                NotificationUtil.showInfo(
                    project,
                    PluginContents.PLUGIN_NAME, HelperBundle.message("settings.codestyle.update.error.local.path"));
            }
            return;
        }

        // 在后台线程执行下载
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressIndicator indicator = new EmptyProgressIndicator();
            try {
                // 检查并更新代码样式
                CodeStyleDownloadManager.checkAndUpdate(
                    project,
                    downloadUrl,
                    indicator,
                    new DownloadProgressListener() {
                        @Override
                        public void onProgress(long downloaded, long totalBytes) {
                            if (totalBytes > 0) {
                                double fraction = Math.min(1.0, downloaded / (double) totalBytes);
                                indicator.setFraction(fraction);
                            }
                        }
                    }
                                                       );

                // 清除已通知版本记录，因为已经更新到该版本
                lastNotifiedVersion = null;

                // 显示成功通知
                if (project != null) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String latestVersion = CodeStyleDownloadManager.getLocalVersion();
                        NotificationUtil.showInfo(project,
                                                  PluginContents.PLUGIN_NAME,
                                                  HelperBundle.message("settings.codestyle.update.success", latestVersion != null ?
                                                                                                            latestVersion : ""));
                    });
                }

                LOG.debug("代码样式更新成功");
            } catch (IOException e) {
                LOG.debug("下载代码样式失败", e);
                if (project != null) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project,
                                                   PluginContents.PLUGIN_NAME,
                                                   HelperBundle.message("settings.codestyle.update.failed", e.getMessage()));
                    });
                }
            }
        });
    }

    /**
     * 获取单例实例
     * <p>
     * 返回 {@link CodeStyleUpdateChecker} 的唯一实例.
     *
     * @return CodeStyleUpdateChecker 实例
     */
    @NotNull
    public static CodeStyleUpdateChecker getInstance() {
        return ApplicationManager.getApplication().getService(CodeStyleUpdateChecker.class);
    }
}

