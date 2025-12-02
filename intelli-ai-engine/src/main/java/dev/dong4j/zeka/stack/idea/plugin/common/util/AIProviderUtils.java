package dev.dong4j.zeka.stack.idea.plugin.common.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;

/**
 * AI 提供者工具类
 * <p>
 * 提供 AI 服务提供者的配置管理功能, 包括获取已验证的提供者列表和验证提供者是否存在
 * 主要用于在执行 AI 相关操作前检查配置有效性, 并在缺失配置时触发提示通知
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.02
 * @since 1.0.0
 */
public class AIProviderUtils {

    public static List<AIProviderConfig> getProviders() {
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return new ArrayList<>(globalSettings.getVerifiedProviders());
    }

    /**
     * 检查项目中是否存在可用的 AI 提供者配置
     * <p>
     * 如果没有可用的 AI 提供者, 将创建错误通知并添加配置面板打开动作
     * 返回布尔值表示是否存在有效提供者
     *
     * @param project    当前项目实例
     * @param configName 配置面板的标识名称
     * @return true 表示存在可用提供者,false 表示不存在且已创建配置提示
     */
    public static boolean hasAIProvider(Project project, String configName) {
        if (AIProviderUtils.getProviders().isEmpty()) {
            return notify(project, configName);
        }
        return true;
    }

    /**
     * 检查项目中是否存在可用的 AI 提供者配置
     * <p>
     * 如果未配置 AI 提供者, 则会显示错误通知并添加打开配置面板的操作
     *
     * @param project 当前项目实例, 用于获取配置和添加通知操作
     * @return 如果存在可用 AI 提供者配置返回 true, 否则返回 false
     */
    public static boolean hasAIProvider(Project project, AIProviderConfig config, String configName) {
        // 获取 AI 配置
        if (config == null) {
            return notify(project, configName);
        }
        return true;
    }

    private static boolean notify(Project project, String configName) {
        Notification notification = new Notification(NotificationUtil.NOTIFICATION_GROUP_ID,
                                                     AICommonBundle.message("settings.ai.provider.no.provider.available"),
                                                     AICommonBundle.message("settings.ai.provider.no.available.warning"),
                                                     NotificationType.ERROR);
        // 添加设置动作
        NotificationUtil.addOpenConfigurablePanelAction(notification, project, configName);
        return false;
    }
}
