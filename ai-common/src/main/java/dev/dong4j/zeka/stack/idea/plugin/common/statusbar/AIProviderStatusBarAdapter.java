package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * 用于适配 AI 提供者状态栏的接口
 * <p>
 * 该接口定义了与 AI 提供者状态栏交互所需的方法, 包括获取当前提供者类型, 默认配置, 可用提供者列表, 切换默认提供者, 获取图标和通知信息等, 适用于在 IDE 状态栏中展示 AI 提供者相关信息的组件.
 *
 * @author 未知
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
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

