package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.io.HttpRequests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AICommonBundle;
import dev.dong4j.zeka.stack.idea.plugin.common.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.kit.PluginUtil;
import dev.dong4j.zeka.stack.idea.plugin.kit.SiteContents;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;

/**
 * 新特性启动活动类
 * <p> 该类实现了 ProjectActivity 接口, 用于在项目启动时执行特定的操作. 具体来说, 当项目不是默认项目时,
 * 检查是否有新版本可用, 如果有则显示通知提示用户查看更新内容.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
@Slf4j
public class WhatsNewStartupActivity implements ProjectActivity {
    /** 版本信息的远程 URL */
    private static final String VERSION_URL = SiteContents.VERSION_URL;

    /** 用于确保启动活动逻辑仅执行一次的原子标志 */
    private final AtomicBoolean hasRun = new AtomicBoolean(false);
    /**
     * 执行启动活动的逻辑, 用于在项目启动时检查新版本并显示提示
     * <p> 如果项目是默认项目, 则直接返回 Unit.INSTANCE. 否则, 延迟 5 秒后检查是否有新版本可用,
     * 如果有则显示通知提示用户查看更新内容.
     *
     * @param project      项目对象, 表示当前处理的项目
     * @param continuation 继续执行的上下文, 用于异步操作
     * @return Unit.INSTANCE 表示操作完成
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        if (project.isDefault()) {
            return Unit.INSTANCE;
        }
        // 只在第一次运行时检查更新
        if (!hasRun.compareAndSet(false, true) || ApplicationManager.getApplication().isUnitTestMode()) {
            return Unit.INSTANCE;
        }

        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            if (!project.isDisposed()) {
                checkForNewVersion(project);
            }
        }, 5000L, TimeUnit.MILLISECONDS);

        return Unit.INSTANCE;
    }

    /**
     * 检查是否有新版本可用
     * <p> 在后台线程中获取远程版本号, 与本地版本进行比较, 如果有新版本则显示通知
     *
     * @param project 项目对象
     */
    private void checkForNewVersion(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 获取远程最新版本
                String latestVersion = fetchLatestVersion();
                if (latestVersion == null || latestVersion.isBlank()) {
                    log.debug("无法获取远程版本信息");
                    return;
                }

                // 获取本地插件版本
                String localVersion = getPluginVersion();
                if (localVersion == null) {
                    log.debug("无法获取本地插件版本");
                    return;
                }

                // 检查是否启用新版本通知
                AIProviderSettings settings = AIProviderSettings.getInstance();
                if (!settings.showUpdateNotification) {
                    log.debug("新版本通知已禁用，跳过显示");
                    return;
                }

                // 比较版本
                if (compareVersion(localVersion, latestVersion) < 0) {
                    log.debug("发现新版本: " + latestVersion + " (当前版本: " + localVersion + ")");
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (!project.isDisposed()) {
                            showUpdateNotification(project, latestVersion, localVersion);
                        }
                    });
                } else {
                    log.debug("插件已是最新版本: " + localVersion);
                }
            } catch (Exception e) {
                log.debug("检查新版本失败", e);
            }
        });
    }

    /**
     * 从远程服务器获取最新版本号
     *
     * @return 最新版本号, 如果获取失败则返回 null
     */
    @Nullable
    private String fetchLatestVersion() {
        try {
            String version = HttpRequests.request(VERSION_URL).productNameAsUserAgent().readString();
            return version.trim();
        } catch (Exception e) {
            log.debug("获取远程版本失败", e);
            return null;
        }
    }

    /**
     * 获取插件版本号
     *
     * @return 插件版本号, 如果获取失败则返回 null
     */
    @Nullable
    private String getPluginVersion() {
        return PluginUtil.getVersion(EngineContents.PLUGIN_ID);
    }

    /**
     * 比较两个版本号
     * <p> 版本号格式: x.y.z (如 2025.3.1)
     * 按照 . 分割, 逐段比较数字大小
     *
     * @param version1 版本号1
     * @param version2 版本号2
     * @return 如果 version1 < version2 返回负数, 如果 version1 > version2 返回正数, 相等返回 0
     */
    private int compareVersion(@NotNull String version1, @NotNull String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int part1 = i < parts1.length ? parseInt(parts1[i]) : 0;
            int part2 = i < parts2.length ? parseInt(parts2[i]) : 0;

            if (part1 != part2) {
                return Integer.compare(part1, part2);
            }
        }
        return 0;
    }

    /**
     * 将字符串转换为整数, 转换失败返回 0
     *
     * @param str 字符串
     * @return 整数
     */
    private int parseInt(@NotNull String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 显示更新通知
     *
     * @param project       项目对象
     * @param latestVersion 最新版本号
     * @param localVersion  本地版本号
     */
    private void showUpdateNotification(@NotNull Project project,
                                        @NotNull String latestVersion,
                                        @NotNull String localVersion) {
        String message = AICommonBundle.message("whatsnew.update.available", latestVersion, localVersion);

        Notification notification = NotificationUtil.getNotificationGroup()
            .createNotification(
                AICommonBundle.message("whatsnew.update.title"),
                message,
                NotificationType.INFORMATION
            );

        // 添加查看更新内容操作
        notification.addAction(new NotificationAction(
            AICommonBundle.message("whatsnew.update.view")) {
            /**
             * 处理通知操作事件
             * <p> 当用户点击通知中的操作按钮时, 打开最新功能编辑器并使通知过期
             *
             * @param e 操作事件对象, 包含触发操作的上下文信息
             * @param notification 通知对象, 用于在操作完成后使其过期
             */
            @Override
            public void actionPerformed(@NotNull AnActionEvent e,
                                        @NotNull Notification notification) {
                // 打开 What's New 页面
                WhatsNewEditorOpener.open(project);
                notification.expire();
            }
        });

        // 添加不再显示操作
        notification.addAction(new NotificationAction(
            AICommonBundle.message("whatsnew.update.dont.show.again")) {
            /**
             * 处理通知动作的点击事件
             * <p> 当用户选择“不再显示”时, 将禁用更新通知提示, 并保存设置, 最后使通知过期
             *
             * @param e              动作事件对象, 包含触发事件的上下文信息
             * @param notification   被点击的通知对象, 用于在操作后使其过期
             */
            @Override
            public void actionPerformed(@NotNull AnActionEvent e,
                                        @NotNull Notification notification) {
                // 禁用新版本通知
                AIProviderSettings settings = AIProviderSettings.getInstance();
                settings.showUpdateNotification = false;
                ApplicationManager.getApplication().saveSettings();
                notification.expire();
            }
        });

        notification.notify(project);
    }
}
