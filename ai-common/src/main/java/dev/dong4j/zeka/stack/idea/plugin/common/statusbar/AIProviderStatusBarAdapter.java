package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * AI 提供商状态栏适配器接口
 * <p>
 * 用于将通用的状态栏组件与插件特定的配置和 UI 资源解耦。
 * 每个使用状态栏的插件需要实现此接口。
 */
public interface AIProviderStatusBarAdapter {

    /**
     * 获取当前默认提供商类型
     *
     * @return 当前默认提供商类型，如果未设置返回 QIANWEN
     */
    @NotNull
    AIProviderType getCurrentProviderType();

    /**
     * 获取当前默认提供商配置
     *
     * @param providerType 提供商类型
     * @return 提供商配置
     */
    @NotNull
    AIProviderConfig getDefaultProviderConfig(@NotNull AIProviderType providerType);

    /**
     * 获取所有可用的提供商配置列表
     *
     * @return 可用提供商配置列表
     */
    @NotNull
    List<AIProviderConfig> getAvailableProviders();

    /**
     * 切换默认提供商
     *
     * @param providerType 提供商类型
     * @param config       提供商配置
     */
    void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config);


    /**
     * 获取插件主图标（16x16）
     *
     * @return 插件主图标
     */
    @NotNull
    Icon getMainIcon();

    /**
     * 获取国际化消息
     *
     * @param key    消息键
     * @param params 消息参数
     * @return 国际化后的消息
     */
    @NotNull
    String getMessage(@NotNull String key, Object... params);

    /**
     * 获取通知组 ID
     *
     * @return 通知组 ID
     */
    @NotNull
    String getNotificationGroupId();

    /**
     * 打开设置面板
     *
     * @param project 项目对象
     */
    void openSettingsPanel(@NotNull com.intellij.openapi.project.Project project);

    /**
     * 显示错误通知
     *
     * @param project 项目对象
     * @param title   通知标题
     * @param content 通知内容
     */
    void showErrorNotification(@NotNull com.intellij.openapi.project.Project project,
                               @NotNull String title,
                               @NotNull String content);
}

