package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * AI 服务提供商状态栏组件模型类
 * <p>
 * 该类封装了与 AI 服务提供商状态栏组件相关的数据模型和操作方法, 用于获取当前提供商显示名称, 构建可用提供商列表, 查找当前提供商索引以及切换默认提供商等操作.
 * <p>
 * 提供了静态方法实现, 确保类不可实例化, 符合工具类设计规范.
 *
 * @author 作者信息
 * @version 1.0.0
 * @date 2025.10.24
 * @since 1.0.0
 */
public final class AIProviderStatusBarWidgetModel {

    /**
     * 私有构造函数, 防止外部直接创建 {@link AIProviderStatusBarWidgetModel} 实例.
     */
    private AIProviderStatusBarWidgetModel() {
    }

    /**
     * 获取当前默认服务商的显示名称。
     *
     * <p>如果配置中未设置默认服务商, 则返回 {@link AIProviderType#QIANWEN} 的显示名称。
     *
     * @param adapter 状态栏适配器
     * @return 默认服务商的显示名称
     */
    @NotNull
    public static String getCurrentProviderDisplayName(@NotNull AIProviderStatusBarAdapter adapter) {
        AIProviderType providerType = adapter.getCurrentProviderType();
        AIProviderConfig defaultConfig = adapter.getDefaultProviderConfig(providerType);
        // return getProviderDisplayText(defaultConfig);
        return getProviderModelName(defaultConfig);
    }

    /**
     * 构建展示用的可用服务商列表。
     *
     * @param adapter 状态栏适配器
     * @return 已验证的服务商配置列表
     */
    @NotNull
    public static List<AIProviderConfig> buildProviderItems(@NotNull AIProviderStatusBarAdapter adapter) {
        return new java.util.ArrayList<>(adapter.getAvailableProviders());
    }

    /**
     * 查找当前默认服务商在候选列表中的索引。
     *
     * <p>如果未找到, 则返回 0。
     *
     * @param items   服务商候选列表
     * @param adapter 状态栏适配器
     * @return 索引位置
     */
    public static int findCurrentProviderIndex(@NotNull List<AIProviderConfig> items,
                                               @NotNull AIProviderStatusBarAdapter adapter) {
        AIProviderType providerType = adapter.getCurrentProviderType();
        AIProviderConfig defaultConfig = adapter.getDefaultProviderConfig(providerType);

        if (defaultConfig != null) {
            for (int i = 0; i < items.size(); i++) {
                AIProviderConfig config = items.get(i);
                if (config.providerType == providerType && Objects.equals(config.credentialId, defaultConfig.credentialId)) {
                    return i;
                }
            }
        }

        for (int i = 0; i < items.size(); i++) {
            AIProviderConfig config = items.get(i);
            if (config.providerType == providerType) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 切换默认服务商。
     *
     * @param adapter        状态栏适配器
     * @param selectedConfig 被选中的服务商配置
     * @throws IllegalArgumentException 当服务商类型缺失时抛出
     */
    public static void switchDefaultProvider(@NotNull AIProviderStatusBarAdapter adapter,
                                             @NotNull AIProviderConfig selectedConfig) {
        AIProviderType providerType = selectedConfig.providerType;
        if (providerType == null) {
            throw new IllegalArgumentException(adapter.getMessage("statusbar.provider.error.missing.type"));
        }

        AIProviderConfig configCopy = selectedConfig.copy();
        configCopy.providerType = providerType;

        adapter.switchDefaultProvider(providerType, configCopy);
    }

    /**
     * 构建服务商展示名称（服务商名:模型名）。
     *
     * @param config 服务商配置
     * @return 展示名称
     */
    @NotNull
    public static String getProviderDisplayText(@NotNull AIProviderConfig config) {
        AIProviderType providerType = config.providerType != null ? config.providerType : AIProviderType.QIANWEN;
        String providerName = providerType.getDisplayName();
        String modelName = config.modelName;
        if (modelName == null || modelName.trim().isEmpty()) {
            return providerName;
        }
        return providerName + ":" + modelName;
    }

    /**
     * 根据 AI 提供者配置获取模型名称
     * <p>
     * 通过 AI 提供者配置获取对应的模型名称, 若配置中未指定提供者类型, 则默认使用通义千问类型.
     *
     * @param config AI 提供者配置对象
     * @return 模型名称
     */
    public static String getProviderModelName(@NotNull AIProviderConfig config) {
        AIProviderType providerType = config.providerType != null ? config.providerType : AIProviderType.QIANWEN;
        String providerName = providerType.getDisplayName();
        return config.modelName;
    }
}

