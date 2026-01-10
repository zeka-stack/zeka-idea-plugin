package dev.dong4j.zeka.stack.idea.plugin.common.agent;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.config.IntelliAgentSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.kit.SettingsUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * IntelliAgent 更新检查器
 * <p> 用于定期检查 IntelliAgent 的最新版本, 并在发现新版本时通知用户进行更新. 该类通过定时任务机制,
 * 在应用启动后每隔一小时检查一次是否有可用的新版本, 并根据配置的下载地址进行更新操作.
 * <p>
 * 主要功能包括:
 * - 启动和停止定时更新检查任务
 * - 检查当前版本与最新版本是否一致
 * - 提供更新通知并支持手动下载最新版本
 * - 支持本地路径和远程 URL 的下载地址配置
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.25
 * @since 1.0.0
 */
@Slf4j
@Service(Service.Level.APP)
public final class IntelliAgentUpdateChecker {

    /** 首次检查延迟时间：1 分钟 */
    private static final long INITIAL_DELAY_MS = TimeUnit.MINUTES.toMillis(1);
    /** 检查间隔：1 小时 */
    private static final long CHECK_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);

    private final IntelliAgentManager agentManager = IntelliAgentManager.getInstance();
    private Timer timer;
    /** 上次已通知的版本，用于避免重复通知同一版本 */
    private String lastNotifiedVersion;

    /**
     * 启动更新检查
     * <p> 如果配置启用了自动更新, 则启动定时器进行定期检查. 首先停止之前的定时器, 然后根据配置的下载地址设置定时任务,
     * 每隔一小时检查一次是否有可用的新版本.
     *
     * @since 1.0.0
     */
    public void start() {
        stop(); // 确保先停止之前的定时器

        AIProviderSettings settings = AIProviderSettings.getInstance();
        IntelliAgentSettings agentSettings = settings.intelliAgentSettings;

        if (agentSettings == null || !agentSettings.autoUpdate) {
            log.debug("自动更新检查未启用，跳过启动");
            return;
        }

        String downloadUrl = agentSettings.downloadUrl != null ? agentSettings.downloadUrl.trim() : "";
        if (downloadUrl.isEmpty() || (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://"))) {
            log.debug("下载地址未配置或为本地路径，跳过自动更新检查");
            return;
        }

        log.debug("启动 IntelliAI Agent 自动更新检查器，首次检查将在 1 分钟后执行，之后每 1 小时检查一次");

        timer = new Timer("IntelliAgentUpdateChecker", true);
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
            log.debug("停止 IntelliAI Agent 自动更新检查器");
        }
    }

    /**
     * 检查是否有新版本的 IntelliAgent 可用
     * <p> 此方法会尝试从指定的下载地址获取最新的 JAR 文件名, 并与本地 JAR 文件名进行比较. 如果发现新版本,
     * 则会调用 {@link #showUpdateNotification} 方法显示更新通知, 并更新已通知的版本记录.
     * 如果本地 JAR 文件已经是最新版本, 则清除已通知的版本记录.
     *
     */
    private void checkForUpdate() {
        // 每次检查时都重新读取最新配置
        AIProviderSettings settings = AIProviderSettings.getInstance();
        IntelliAgentSettings agentSettings = settings.intelliAgentSettings;

        if (agentSettings == null || !agentSettings.autoUpdate) {
            // 如果配置已禁用，停止定时器
            stop();
            return;
        }

        String downloadUrl = agentSettings.downloadUrl != null ? agentSettings.downloadUrl.trim() : "";
        if (downloadUrl.isEmpty() || (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://"))) {
            return;
        }

        try {
            // 获取最新版本名称
            String latestJarName = agentManager.fetchLatestJarName(downloadUrl);
            if (latestJarName.isEmpty()) {
                log.debug("无法获取最新版本信息");
                return;
            }

            // 获取本地 JAR 信息
            IntelliAgentManager.JarInfo localJarInfo = agentManager.resolveLocalJarInfo(agentSettings);
            String localJarName = localJarInfo != null ? localJarInfo.fileName() : null;

            // 比较版本
            if (localJarName == null || !localJarName.equals(latestJarName)) {
                // 避免重复通知同一版本
                if (!latestJarName.equals(lastNotifiedVersion)) {
                    log.debug("发现新版本 Agent JAR: " + latestJarName + " (当前版本: " + (localJarName != null ? localJarName : "无") + ")");
                    showUpdateNotification(latestJarName, localJarName, agentSettings);
                    lastNotifiedVersion = latestJarName;
                } else {
                    log.debug("已通知过该版本，跳过: " + latestJarName);
                }
            } else {
                log.debug("Agent JAR 已是最新版本: " + latestJarName);
                // 如果本地已更新到最新版本，清除已通知版本记录
                lastNotifiedVersion = null;
            }
        } catch (Exception e) {
            log.debug("检查 Agent 更新失败", e);
        }
    }

    /**
     * 显示更新通知
     *
     * @param latestJarName 最新版本 JAR 文件名
     * @param localJarName  本地 JAR 文件名
     * @param settings      Agent 配置
     */
    private void showUpdateNotification(@NotNull String latestJarName,
                                        @Nullable String localJarName,
                                        @NotNull IntelliAgentSettings settings) {
        String message = localJarName != null
                         ? AICommonBundle.message("settings.agent.update.available.with.current", latestJarName, localJarName)
                         : AICommonBundle.message("settings.agent.update.available", latestJarName);

        Notification notification = NotificationUtil.getNotificationGroup()
            .createNotification(
                AICommonBundle.message("settings.agent.update.title"),
                message,
                NotificationType.INFORMATION
                               );

        // 添加更新操作
        notification.addAction(new NotificationAction(
            AICommonBundle.message("settings.agent.update.now")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e,
                                        @NotNull Notification notification) {
                // 触发后台下载更新
                Project project = ProjectManager.getInstance().getDefaultProject();
                downloadLatestVersion(project, latestJarName, settings);
                notification.expire();
            }
        });

        // 添加打开设置操作
        Project defaultProject = ProjectManager.getInstance().getDefaultProject();
        SettingsUtil.addOpenAction(
            notification,
            AICommonBundle.message("settings.display.name"),
            AICommonBundle.message("settings.ai.provider.open.engine.settings"));

        notification.notify(defaultProject);
    }

    /**
     * 下载最新版本
     *
     * @param project       项目对象
     * @param latestJarName 最新版本 JAR 文件名
     * @param settings      Agent 配置
     */
    private void downloadLatestVersion(@Nullable Project project,
                                       @NotNull String latestJarName,
                                       @NotNull IntelliAgentSettings settings) {
        String downloadUrl = settings.downloadUrl != null ? settings.downloadUrl.trim() : "";
        if (downloadUrl.isEmpty()) {
            if (project != null) {
                NotificationUtil.showError(project, AICommonBundle.message("settings.agent.error.no.url"));
            }
            return;
        }

        if (!downloadUrl.startsWith("http://") && !downloadUrl.startsWith("https://")) {
            if (project != null) {
                NotificationUtil.showInfo(project, AICommonBundle.message("settings.agent.download.local.path"));
            }
            return;
        }

        String jarDownloadUrl = agentManager.buildDownloadUrl(downloadUrl, latestJarName);
        IntelliAgentSettings downloadSettings = settings.copy();
        downloadSettings.downloadUrl = jarDownloadUrl;

        // 在后台线程执行下载
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ProgressIndicator indicator = new EmptyProgressIndicator();
            try {
                agentManager.downloadJar(downloadSettings, latestJarName, indicator, null);

                // 更新配置中的 jar 文件名
                AIProviderSettings providerSettings = AIProviderSettings.getInstance();
                providerSettings.intelliAgentSettings.jarFileName = latestJarName;

                // 清除已通知版本记录，因为已经更新到该版本
                lastNotifiedVersion = null;

                // 显示成功通知
                if (project != null) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showInfo(project,
                                                  AICommonBundle.message("settings.agent.update.success", latestJarName));
                    });
                }

                log.debug("Agent JAR 更新成功: " + latestJarName);
            } catch (Exception e) {
                log.debug("下载 Agent JAR 失败: " + latestJarName, e);
                if (project != null) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationUtil.showError(project,
                                                   AICommonBundle.message("settings.agent.update.failed", e.getMessage()));
                    });
                }
            }
        });
    }

    /**
     * 获取单例实例
     * <p> 返回 {@link IntelliAgentUpdateChecker} 的唯一实例.
     *
     * @return IntelliAgentUpdateChecker 实例
     */
    @NotNull
    public static IntelliAgentUpdateChecker getInstance() {
        return ApplicationManager.getApplication().getService(IntelliAgentUpdateChecker.class);
    }
}

