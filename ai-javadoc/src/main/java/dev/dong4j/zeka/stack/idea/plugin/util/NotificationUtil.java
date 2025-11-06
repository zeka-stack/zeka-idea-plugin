package dev.dong4j.zeka.stack.idea.plugin.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 通知工具类
 * <p>提供统一的通知功能，使用 IntelliJ 的通知系统替代弹窗，提供更好的用户体验
 *
 * <p>作为插件用户反馈机制的核心组件，负责向用户显示各种操作结果和状态信息。
 * 使用 IntelliJ Platform 的通知系统，确保与 IDE 风格一致。
 *
 * <p>通知类型：
 * <ul>
 *   <li>INFO - 信息通知（绿色）</li>
 *   <li>WARNING - 警告通知（黄色）</li>
 *   <li>ERROR - 错误通知（红色）</li>
 * </ul>
 *
 * <p>特点：
 * <ul>
 *   <li>非侵入式 - 不阻塞用户操作</li>
 *   <li>自动关闭 - 一段时间后自动消失</li>
 *   <li>可交互 - 用户可以点击查看详情或关闭</li>
 *   <li>统一管理 - 集中处理所有通知</li>
 *   <li>国际化支持 - 使用资源文件管理消息</li>
 * </ul>
 *
 * <p>设计原则：
 * <ul>
 *   <li>单一职责：专门负责通知显示</li>
 *   <li>静态工具：提供静态方法，方便调用</li>
 *   <li>类型安全：使用枚举定义通知类型</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @see Notification
 * @see NotificationGroup
 * @see NotificationType
 * @since 1.0.0
 */
public class NotificationUtil {

    /**
     * 通知组 ID
     *
     * <p>标识插件通知组的唯一字符串。
     * 必须与 plugin.xml 中的 notificationGroup id 保持一致。
     *
     * @see #getNotificationGroup()
     */
    public static final String NOTIFICATION_GROUP_ID = "AI Javadoc Notifications";

    /**
     * 获取通知组
     *
     * <p>通过 NotificationGroupManager 获取插件的通知组。
     * 通知组定义了通知的显示方式和行为。
     *
     * <p>获取流程：
     * <ol>
     *   <li>获取 NotificationGroupManager 实例</li>
     *   <li>根据组 ID 获取通知组</li>
     * </ol>
     *
     * @return 通知组实例
     * @see NotificationGroupManager#getNotificationGroup(String)
     */
    @NotNull
    private static NotificationGroup getNotificationGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID);
    }

    /**
     * 显示信息通知
     *
     * <p>显示 INFORMATION 类型的通知，通常用于普通信息提示。
     * 使用绿色样式，表示操作成功或一般信息。
     *
     * <p>使用场景：
     * <ul>
     *   <li>操作成功完成</li>
     *   <li>一般信息提示</li>
     *   <li>状态更新通知</li>
     * </ul>
     *
     * @param project 项目对象，可为 null
     * @param title   通知标题
     * @param content 通知内容
     * @see #notify(Project, String, String, NotificationType)
     * @see NotificationType#INFORMATION
     */
    public static void notifyInfo(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.INFORMATION);
    }

    /**
     * 显示警告通知
     *
     * <p>显示 WARNING 类型的通知，通常用于警告信息。
     * 使用黄色样式，表示需要注意但非错误的情况。
     *
     * <p>使用场景：
     * <ul>
     *   <li>操作部分成功</li>
     *   <li>配置警告</li>
     *   <li>性能提示</li>
     * </ul>
     *
     * @param project 项目对象，可为 null
     * @param title   通知标题
     * @param content 通知内容
     * @see #notify(Project, String, String, NotificationType)
     * @see NotificationType#WARNING
     */
    public static void notifyWarning(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.WARNING);
    }

    /**
     * 显示错误通知
     *
     * <p>显示 ERROR 类型的通知，通常用于错误信息。
     * 使用红色样式，表示操作失败或严重问题。
     *
     * <p>使用场景：
     * <ul>
     *   <li>操作失败</li>
     *   <li>配置错误</li>
     *   <li>系统异常</li>
     * </ul>
     *
     * @param project 项目对象，可为 null
     * @param title   通知标题
     * @param content 通知内容
     * @see #notify(Project, String, String, NotificationType)
     * @see NotificationType#ERROR
     */
    public static void notifyError(@Nullable Project project, @NotNull String title, @NotNull String content) {
        notify(project, title, content, NotificationType.ERROR);
    }

    /**
     * 显示通知
     *
     * <p>核心通知显示方法，创建并显示通知。
     * 所有其他通知方法最终都调用此方法。
     *
     * <p>显示流程：
     * <ol>
     *   <li>获取通知组</li>
     *   <li>创建通知对象</li>
     *   <li>显示通知</li>
     * </ol>
     *
     * @param project 项目对象，可为 null
     * @param title   通知标题
     * @param content 通知内容
     * @param type    通知类型（INFO/WARNING/ERROR）
     * @see NotificationGroup#createNotification(String, String, NotificationType)
     * @see Notification#notify(Project)
     */
    private static void notify(@Nullable Project project, @NotNull String title, @NotNull String content, @NotNull NotificationType type) {
        Notification notification = getNotificationGroup().createNotification(title, content, type);
        notification.notify(project);
    }

    /**
     * 显示成功完成通知
     *
     * <p>显示文档生成任务的完成统计信息。
     * 根据任务结果动态选择通知类型。
     *
     * <p>类型选择逻辑：
     * <ul>
     *   <li>有失败任务：WARNING 类型</li>
     *   <li>有成功任务：INFO 类型</li>
     *   <li>无成功无失败：WARNING 类型</li>
     * </ul>
     *
     * <p>消息格式：
     * <pre>
     * JavaDoc 生成完成
     * 完成: 5 | 失败: 0 | 跳过: 2
     * </pre>
     *
     * @param project   项目对象
     * @param completed 成功完成的任务数
     * @param failed    失败的任务数
     * @param skipped   跳过的任务数
     * @see JavaDocBundle#message(String, Object...)
     */
    public static void notifyCompletion(@Nullable Project project, int completed, int failed, int skipped) {
        String content = JavaDocBundle.message("notification.completion.format", completed, failed, skipped);

        final NotificationType type = getNotificationType(completed, failed);

        notify(project, JavaDocBundle.message("notification.generation.complete"), content, type);
    }

    /**
     * 根据任务完成和失败的数量确定通知类型
     * <p>
     * 根据传入的完成数量和失败数量判断应使用哪种类型的通知。
     * 如果有失败任务，则使用警告类型；如果有完成任务但无失败，则使用信息类型；否则默认使用警告类型。
     *
     * @param completed 完成的任务数量
     * @param failed    失败的任务数量
     * @return 通知类型，可能为 WARNING、INFORMATION
     */
    @NotNull
    private static NotificationType getNotificationType(int completed, int failed) {
        NotificationType type;
        if (failed > 0) {
            type = NotificationType.WARNING;
        } else if (completed > 0) {
            type = NotificationType.INFORMATION;
        } else {
            type = NotificationType.WARNING;
        }
        return type;
    }

    /**
     * 显示目标完成通知
     *
     * <p>显示针对特定目标的文档生成任务完成统计。
     * 包含目标描述信息，提供更详细的反馈。
     *
     * <p>类型选择逻辑：
     * <ul>
     *   <li>有失败任务：WARNING 类型</li>
     *   <li>有成功任务：INFO 类型</li>
     *   <li>无成功无失败：WARNING 类型</li>
     * </ul>
     *
     * <p>消息格式：
     * <pre>
     * JavaDoc 生成完成
     * 方法: getX()
     * 完成: 1 | 失败: 0 | 跳过: 0
     * </pre>
     *
     * @param project   项目对象
     * @param target    目标描述（如"方法: getX()"）
     * @param completed 成功完成的任务数
     * @param failed    失败的任务数
     * @param skipped   跳过的任务数
     * @see JavaDocBundle#message(String, Object...)
     */
    public static void notifyTargetCompletion(@Nullable Project project, @NotNull String target,
                                              int completed, int failed, int skipped) {
        String content = JavaDocBundle.message("notification.target.completion.format", target, completed, failed, skipped);

        final NotificationType type = getNotificationType(completed, failed);

        notify(project, JavaDocBundle.message("notification.generation.complete"), content, type);
    }

    /**
     * 显示无任务通知
     *
     * <p>当没有找到需要处理的任务时显示的通知。
     * 通常表示用户操作的元素已有文档或不需要生成文档。
     *
     * <p>使用场景：
     * <ul>
     *   <li>元素已有文档且配置为跳过</li>
     *   <li>无法定位到有效元素</li>
     *   <li>选择的文件中无有效元素</li>
     * </ul>
     *
     * @param project 项目对象
     * @param message 提示消息
     * @see JavaDocBundle#message(String, Object...)
     */
    public static void notifyNoTask(@Nullable Project project, @NotNull String message) {
        notify(project, JavaDocBundle.message("notification.title"), message, NotificationType.INFORMATION);
    }

    /**
     * 显示错误消息
     *
     * <p>显示具体的错误信息通知。
     * 使用 ERROR 类型，红色样式突出显示。
     *
     * <p>使用场景：
     * <ul>
     *   <li>AI 服务调用失败</li>
     *   <li>配置验证失败</li>
     *   <li>系统异常</li>
     * </ul>
     *
     * @param project 项目对象
     * @param message 错误消息内容
     * @see JavaDocBundle#message(String, Object...)
     */
    public static void notifyErrorMessage(@Nullable Project project, @NotNull String message) {
        notify(project, JavaDocBundle.message("notification.error.title"), message, NotificationType.ERROR);
    }

    /**
     * 显示索引中通知
     *
     * <p>当项目正在索引时显示的通知。
     * 提示用户当前无法执行文档生成操作。
     *
     * <p>显示条件：
     * <ul>
     *   <li>项目处于 Dumb Mode（索引中）</li>
     *   <li>用户尝试执行文档生成操作</li>
     * </ul>
     *
     * @param project 项目对象
     * @see JavaDocBundle#message(String, Object...)
     */
    public static void notifyIndexing(@Nullable Project project) {
        notify(project, JavaDocBundle.message("notification.title"),
               JavaDocBundle.message("notification.indexing.warning"),
               NotificationType.WARNING);
    }
}

