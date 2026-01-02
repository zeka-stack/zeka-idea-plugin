package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.kit.SettingsUtil;

/**
 * 通知工具类
 *
 * @author dong4j
 * @version 1.0.0
 */
public class NotificationUtil {
    public static final String NOTIFICATION_TITLE = EngineContents.PLUGIN_NAME;
    public static final String NOTIFICATION_GROUP_ID = NOTIFICATION_TITLE + " Notifications";

    /**
     * 获取指定 ID 的通知组
     *
     * @return 通知组实例
     */
    @NotNull
    public static NotificationGroup getNotificationGroup() {
        return dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.getNotificationGroup(EngineContents.PLUGIN_NAME);
    }

    /**
     * 显示信息通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showInfo(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showInfo(project, EngineContents.PLUGIN_NAME, message);
    }

    /**
     * 显示警告通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showWarning(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showWarning(project, EngineContents.PLUGIN_NAME, message);
    }

    /**
     * 显示错误通知
     *
     * @param project 项目对象，可为空
     * @param message 通知内容
     */
    public static void showError(@Nullable Project project, @NotNull String message) {
        dev.dong4j.zeka.stack.idea.plugin.kit.NotificationUtil.showError(project, EngineContents.PLUGIN_NAME, message);
    }

    /**
     * 为通知添加一个可配置面板的操作
     * <p> 此方法调用底层实现以创建一个通知操作, 当用户点击该操作时, 会打开指定的配置面板并使通知过期.
     *
     * @param notification 与通知关联的对象, 不能为空
     * @param displayName  操作的显示名称, 不能为空, 用于打开指定的设置页面
     * @param actionName   操作的动作名称, 不能为空
     */
    public static void addOpenConfigurablePanelAction(Notification notification,
                                                      String displayName,
                                                      String actionName) {
        SettingsUtil.addOpenAction(
            notification,
            displayName,
            actionName);
    }
}

