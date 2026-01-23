package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.impl.NotificationsManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.BalloonImpl;
import com.intellij.ui.BalloonLayoutData;
import com.intellij.ui.awt.RelativePoint;

import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Point;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.event.HyperlinkEvent;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.common.action.DonateAction;
import dev.dong4j.zeka.stack.idea.plugin.common.action.QuickStartAction;

/**
 * 通知管理类
 * <p> 提供创建和显示不同类型的通知功能, 包括气泡通知, 粘性通知和日志通知等.
 * <p> 该类通过获取通知组管理器来创建和管理通知, 支持自定义标题, 内容和动作.
 * <p> 主要功能包括:
 * <ul>
 * <li> 获取不同类型的通知组 (如气泡通知, 日志通知, 粘性通知等)</li>
 * <li> 显示普通通知和日志通知 </li>
 * <li> 显示欢迎通知, 带有捐赠按钮 </li>
 * <li> 计算通知在界面上的显示位置 </li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.04
 * @since 1.0.0
 */
public class Notifications {

    /**
     * 获取通知组管理器实例
     * <p> 该方法返回全局的 NotificationGroupManager 实例, 用于管理通知组
     *
     * @return 通知组管理器实例
     */
    private static NotificationGroupManager getNotificationGroupManager() {
        return NotificationGroupManager.getInstance();
    }

    /**
     * 获取普通气泡通知组
     * <p> 返回用于显示普通类型瞬态气泡通知的通知组实例, 该通知组基于插件名称进行标识
     *
     * @return 通知组实例, 用于创建和显示普通气泡通知
     */
    public static NotificationGroup getBalloonNotificationGroup() {
        return getNotificationGroupManager().getNotificationGroup(EngineContents.PLUGIN_NAME);
    }

    /**
     * 获取用于显示日志类型气泡通知的通知组
     * <p> 该方法返回一个专门用于显示日志信息的气泡通知组, 通常用于在界面中展示带有日志记录功能的通知.
     *
     * @return 日志类型气泡通知组
     */
    public static NotificationGroup getBalloonLogNotificationGroup() {
        return getNotificationGroupManager().getNotificationGroup(EngineContents.PLUGIN_NAME + " Log");
    }

    /**
     * 获取粘性气泡通知组
     * <p> 返回一个用于显示粘性气泡通知的通知组, 此类通知会一直显示在界面上直到用户手动关闭
     *
     * @return 粘性气泡通知组
     */
    public static NotificationGroup getStickyNotificationGroup() {
        return getNotificationGroupManager().getNotificationGroup(EngineContents.PLUGIN_NAME + " Sticky");
    }

    /**
     * 获取粘性气泡通知的日志通知组
     * <p> 通过通知组管理器获取指定名称的粘性气泡通知组, 该名称包含插件名和 "Sticky Log" 后缀
     *
     * @return 粘性气泡通知的日志通知组
     */
    public static NotificationGroup getStickyLogNotificationGroup() {
        return getNotificationGroupManager().getNotificationGroup(EngineContents.PLUGIN_NAME + " Sticky Log");
    }

    /**
     * 展示瞬态通知
     * <p> 创建一个普通气泡通知并立即显示
     *
     * @param content          通知内容
     * @param notificationType 通知类型
     * @param project          当前项目
     */
    public static void showNotification(String content, NotificationType notificationType, Project project) {
        getBalloonNotificationGroup().createNotification(content, notificationType).notify(project);
    }

    /**
     * 展示通知 (瞬态通知)
     * <p> 使用指定的标题, 内容, 通知类型和项目创建并显示一个瞬态通知
     * <p> 使用示例:
     * <pre>{@code
     * Notifications.showNotification("操作成功", "文件已保存", NotificationType.INFORMATION, project);
     * }</pre>
     *
     * @param title            通知标题, 不能为空
     * @param content          通知内容, 不能为空
     * @param notificationType 通知类型, 不能为空
     * @param project          项目上下文, 不能为空
     */
    public static void showNotification(String title, String content, NotificationType notificationType, Project project) {
        getBalloonNotificationGroup().createNotification(content, notificationType).setTitle(title).notify(project);
    }

    /**
     * 展示带操作按钮的通知
     * <p> 创建一个带有标题, 内容, 类型和操作按钮的通知, 并在指定项目中显示
     * <p> 使用示例:
     * <pre>{@code
     * List<AnAction> actions = Arrays.asList(new MyAction(), new AnotherAction());
     * Notifications.showNotification("标题", "内容", NotificationType.WARNING, actions, project);
     * }</pre>
     *
     * @param title            通知标题, 不能为空
     * @param content          通知内容, 不能为空
     * @param notificationType 通知类型, 如信息, 警告, 错误等
     * @param actions          通知的操作按钮列表, 不能为空
     * @param project          显示通知的项目上下文
     */
    public static void showNotification(String title, String content, NotificationType notificationType,
                                        java.util.List<? extends @NotNull AnAction> actions, Project project) {
        Notification notification = getBalloonNotificationGroup().createNotification(content, notificationType).setTitle(title);
        actions.forEach(notification::addAction);
        notification.notify(project);
    }

    /**
     * 展示通知 (通知记录在 Event Log 或 Notifications 中)
     * <p> 通过指定内容, 通知类型和项目上下文显示一条日志通知, 该通知将被记录在事件日志或通知中心中
     *
     * @param content          通知内容
     * @param notificationType 通知类型, 如信息, 警告, 错误等
     * @param project          当前项目上下文
     */
    public static void showLogNotification(String content, NotificationType notificationType, Project project) {
        getBalloonLogNotificationGroup().createNotification(content, notificationType).notify(project);
    }

    /**
     * 展示通知 (通知记录在 Event Log 或 Notifications 中)
     * <p> 使用指定标题, 内容, 通知类型和项目上下文显示一条日志级别的通知
     *
     * @param title            标题, 用于通知的标题栏
     * @param content          通知的具体内容
     * @param notificationType 通知类型 (如 INFORMATION, WARNING, ERROR 等)
     * @param project          当前项目上下文对象
     */
    public static void showLogNotification(String title, String content, NotificationType notificationType, Project project) {
        getBalloonLogNotificationGroup().createNotification(content, notificationType).setTitle(title).notify(project);
    }

    /**
     * 展示欢迎通知
     * <p> 用于在 IDE 中展示插件的欢迎通知, 包含捐赠链接和相关操作按钮. 通知内容会显示在右上角, 并记录到 Event Log 或 Notifications 中.
     *
     * @param project 当前项目对象, 不能为 null
     */
    @SuppressWarnings( {"deprecation", "DuplicatedCode"})
    public static void showWelcomeNotification(@NotNull Project project) {
        Notification notification = Notifications.getBalloonLogNotificationGroup()
            .createNotification(AICommonBundle.message("notification.welcome.content",
                                                       Urls.GITHUB_LINK,
                                                       UrlType.DONATE.getId()
                                                      ) + "<br/>",
                                NotificationType.INFORMATION)
            .setTitle(AICommonBundle.message("notification.welcome.title", EngineContents.PLUGIN_NAME))
            .setImportant(true)
            .setListener(new NotificationListenerImpl())
            .addAction(new QuickStartAction())
            .addAction(new DonateAction(AICommonBundle.message("action.donate.welcome.text")));

        // 尝试使用自定义位置显示
        IdeFrame window = (IdeFrame) NotificationsManagerImpl.findWindowForBalloon(project);
        if (window != null) {
            try {
                Balloon balloon = NotificationsManagerImpl.createBalloon(window,
                                                                         notification,
                                                                         false,
                                                                         false,
                                                                         BalloonLayoutData.fullContent(),
                                                                         project);

                JComponent component = window.getComponent();
                balloon.show(getUpperRightRelativePoint(component, (BalloonImpl) balloon), Balloon.Position.above);
                return;
            } catch (Exception e) {
                // 如果自定义显示失败，使用默认方式
            }
        }

        // 备用方案：使用默认方式显示通知
        notification.notify(project);
    }

    /**
     * 获取右上角相对位置用于显示气泡通知
     * <p> 根据给定的组件和气泡对象, 计算并返回一个相对位置点, 用于在指定组件的右上角显示气泡通知.
     *
     * @param component 显示气泡通知的组件
     * @param balloon   气泡通知对象
     * @return 计算得到的相对位置点
     */
    public static RelativePoint getUpperRightRelativePoint(JComponent component, BalloonImpl balloon) {
        // 在其他平台上，气球提示显示在标题栏的右侧边缘
        JLayeredPane layeredPane = component.getRootPane().getLayeredPane();

        // 查找标题栏组件
        Component titleBar = java.util.Arrays.stream(layeredPane.getComponents())
            .filter(c -> c.getX() == 0 && c.getY() == 0 && c.getWidth() == layeredPane.getWidth() && c.getHeight() > 0)
            .findFirst()
            .orElse(null);

        // 计算垂直偏移量
        int insetTop = balloon.getShadowBorderInsets().top;
        int contentHalfHeight = (int) (balloon.getContent().getPreferredSize().getHeight() / 2);
        int titleBarHeight = titleBar != null ? titleBar.getHeight() : 40;
        int offsetY = titleBarHeight + insetTop + contentHalfHeight;

        // 设置气球提示的显示位置
        Component relativeComponent = titleBar != null ? titleBar : component;

        int insetRight = balloon.getShadowBorderInsets().right;
        int contentHalfWidth = (int) (balloon.getContent().getPreferredSize().getWidth() / 2);
        int offsetX = relativeComponent.getWidth() - (25 + insetRight + contentHalfWidth);

        return new RelativePoint(relativeComponent, new Point(offsetX, offsetY));
    }

    /**
     * 通知监听器实现类
     * <p> 继承自 NotificationListener.Adapter, 用于处理通知中的超链接点击事件.
     * <p> 当用户点击通知中的超链接时, 根据链接类型决定打开网页或显示支持对话框.
     * <p> 支持的链接类型包括捐赠链接 (DONATE), 若网络可达则直接跳转至捐赠页面;
     * 否则弹出支持对话框. 其他链接则直接使用系统浏览器打开.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.04
     * @since 1.0.0
     */
    private static class NotificationListenerImpl extends NotificationListener.Adapter {
        /**
         * 处理超链接激活事件
         * <p> 根据接收到的通知和超链接事件, 执行相应的操作
         * <p> 如果超链接描述符为捐赠链接 ID, 则检查网络可达性:
         * - 如果网络可达, 则打开捐赠链接
         * - 如果网络不可达, 则显示支持对话框
         * <p> 否则, 打开普通超链接
         *
         * @param notification 通知对象, 不能为 null
         * @param e            超链接事件对象, 不能为 null
         */
        @Override
        protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
            String url = e.getDescription();

            if (Objects.equals(UrlType.DONATE.getId(), url)) {
                if (Urls.isReachable()) {
                    BrowserUtil.browse(UrlType.DONATE.getUrl());
                } else {
                    new dev.dong4j.zeka.stack.idea.plugin.common.ui.dialog.SupportDialog().show();
                }
            } else {
                BrowserUtil.browse(url);
            }
        }
    }
}

