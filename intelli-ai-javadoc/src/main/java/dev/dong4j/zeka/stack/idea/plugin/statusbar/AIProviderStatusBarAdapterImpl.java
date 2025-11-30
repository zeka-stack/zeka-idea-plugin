package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIProviderStatusBarAdapter;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.NotificationUtil;
import icons.AIJicons;

/**
 * AI 提供商状态栏适配器实现类
 * <p>
 * 该类实现了 AIProviderStatusBarAdapter 接口, 提供 AI 提供商的状态栏相关功能,
 * 包括当前提供商类型获取, 提供商配置管理, 状态栏图标显示, 消息提示等操作.
 * 采用单例模式确保全局唯一实例, 统一管理 AI 提供商的切换和配置.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIProviderStatusBarAdapterImpl implements AIProviderStatusBarAdapter {

    /** 单例实例, 用于提供 AI 服务状态栏适配器的全局访问点 */
    private static final AIProviderStatusBarAdapterImpl INSTANCE = new AIProviderStatusBarAdapterImpl();

    /**
     * 私有构造函数, 用于实例化 AIProviderStatusBarAdapterImpl 类
     * <p>
     * 该构造函数为私有, 防止外部直接实例化该类, 确保通过工厂方法或单例模式创建实例
     */
    private AIProviderStatusBarAdapterImpl() {
    }

    /**
     * 获取 AIProviderStatusBarAdapterImpl 的单例实例
     * <p>
     * 返回已经初始化的 AIProviderStatusBarAdapterImpl 单例对象
     *
     * @return AIProviderStatusBarAdapterImpl 单例实例
     */
    public static AIProviderStatusBarAdapterImpl getInstance() {
        return INSTANCE;
    }

    /**
     * 获取当前配置的 AI 服务提供商类型
     * <p>
     * 从设置状态中读取提供商类型配置, 若配置为空则默认返回 QIANWEN 类型
     *
     * @return 当前 AI 服务提供商类型, 若未配置则返回默认值 AIProviderType.QIANWEN
     */
    @Override
    @NotNull
    public AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    /**
     * 获取默认的 AI 服务提供商配置
     * <p>
     * 根据指定的 AI 服务提供商类型, 获取默认的配置信息
     *
     * @param providerType AI 服务提供商类型
     * @return 默认的 AI 服务提供商配置
     * @since 1.0
     */
    @Override
    @NotNull
    public AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType) {
        // 从全局配置中获取提供商配置
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return globalSettings.getDefaultProviderConfig(providerType);
    }

    /**
     * 获取可用的 AI 服务提供商配置列表
     * <p>
     * 从设置状态中获取已验证的 AI 服务提供商配置, 并返回其副本
     *
     * @return 可用的 AI 服务提供商配置列表
     */
    @Override
    @NotNull
    public List<AIProviderConfig> getAvailableProviders() {
        // 从全局配置中获取可用提供商列表
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        return new ArrayList<>(globalSettings.getVerifiedProviders());
    }

    /**
     * 切换默认的 AI 服务提供商配置
     * <p>
     * 根据指定的 AI 服务提供商类型和配置信息, 更新应用的默认提供商设置.
     *
     * @param providerType AI 服务提供商类型
     * @param config       AI 服务提供商配置信息
     */
    @Override
    public void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        // 更新插件配置中的默认提供商选择
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        // 更新全局配置中的提供商配置
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    /**
     * 获取主图标
     * <p>
     * 该方法返回 AIJ 的主图标, 通常用于 UI 组件的显示.
     *
     * @return AIJ 的主图标
     */
    @Override
    @NotNull
    public Icon getMainIcon() {
        return AIJicons.AIJ_16;
    }

    /**
     * 根据指定的键和参数获取本地化的消息字符串
     * <p>
     * 使用给定的键和参数从资源包中查找并返回对应的消息字符串
     *
     * @param key    消息键, 用于定位对应的资源字符串
     * @param params 可变参数, 用于替换消息中的占位符
     * @return 对应的本地化消息字符串
     * @throws IllegalArgumentException 如果键不存在或参数不匹配
     * @since 1.0
     */
    @Override
    @NotNull
    public String getMessage(@NotNull String key, Object... params) {
        return JavaDocBundle.message(key, params);
    }

    /**
     * 获取通知组的唯一标识符
     * <p>
     * 返回系统预设的通知组 ID, 用于标识 AI 相关的通知组
     *
     * @return 通知组 ID
     */
    @Override
    @NotNull
    public String getNotificationGroupId() {
        return "IntelliAI JavaDoc Notifications";
    }

    /**
     * 打开 JavaDoc 设置面板
     * <p>
     * 调用 ShowSettingsUtil 工具类, 显示 JavaDoc 设置对话框.
     *
     * @param project 项目对象
     * @since 1.0
     */
    @Override
    public void openSettingsPanel(@NotNull Project project) {
        ShowSettingsUtil.getInstance().editConfigurable(null, "IntelliAI JavaDoc");
    }

    /**
     * 显示错误通知
     * <p>
     * 根据指定的项目, 标题和内容创建并显示一个错误通知.
     *
     * @param project 项目对象
     * @param title   通知标题
     * @param content 通知内容
     */
    @Override
    public void showErrorNotification(@NotNull Project project, @NotNull String title, @NotNull String content) {
        Notification notification = new Notification(
            NotificationUtil.NOTIFICATION_GROUP_ID,
            title,
            content,
            NotificationType.ERROR
        );
        NotificationUtil.addOpenConfigurablePanelAction(notification, project);
        notification.notify(project);
    }
}

