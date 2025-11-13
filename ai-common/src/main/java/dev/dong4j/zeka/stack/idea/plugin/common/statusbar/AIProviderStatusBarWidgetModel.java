package dev.dong4j.zeka.stack.idea.plugin.common.statusbar;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * 状态栏服务商切换模型
 * <p>
 * 负责提供状态栏控件所需的数据处理逻辑, 包括当前默认服务商的显示名称、
 * 可用服务商列表构建以及默认服务商切换等功能。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AIProviderStatusBarWidgetModel {

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
        return getProviderDisplayText(defaultConfig);
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
}

