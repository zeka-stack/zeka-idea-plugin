package dev.dong4j.zeka.stack.idea.plugin.statusbar;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import dev.dong4j.zeka.stack.idea.plugin.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.JavaDocBundle;

/**
 * 状态栏服务商切换模型
 * <p>
 * 负责提供状态栏控件所需的数据处理逻辑, 包括当前默认服务商的显示名称、
 * 可用服务商列表构建以及默认服务商切换等功能。
 * <p>
 * 设计目标：
 * <ul>
 *   <li>与 UI 解耦, 便于单元测试</li>
 *   <li>封装 `SettingsState` 访问逻辑</li>
 *   <li>确保只返回已验证的服务商配置</li>
 * </ul>
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
     * @param settings 插件配置
     * @return 默认服务商的显示名称
     */
    @NotNull
    public static String getCurrentProviderDisplayName(@NotNull SettingsState settings) {
        AIProviderType providerType = settings.providerType != null ? settings.providerType : AIProviderType.QIANWEN;
        SettingsState.ProviderConfig defaultConfig = settings.defaultProviders.get(providerType);
        if (defaultConfig == null) {
            defaultConfig = settings.getDefaultProviderConfig(providerType);
        }
        return getProviderDisplayText(defaultConfig);
    }

    /**
     * 构建展示用的可用服务商列表。
     *
     * <p>数据来源为 {@link SettingsState#getAvailableProviders()}。与设置页保持一致，列表中允许存在
     * 同一服务商的多个实例（例如不同模型或 API Key）。
     *
     * @param settings 插件配置
     * @return 已验证的服务商配置列表
     */
    @NotNull
    public static List<SettingsState.ProviderConfig> buildProviderItems(@NotNull SettingsState settings) {
        return new java.util.ArrayList<>(settings.getAvailableProviders());
    }

    /**
     * 查找当前默认服务商在候选列表中的索引。
     *
     * <p>如果未找到, 则返回 0。
     *
     * @param items    服务商候选列表
     * @param settings 插件配置
     * @return 索引位置
     */
    public static int findCurrentProviderIndex(@NotNull List<SettingsState.ProviderConfig> items,
                                               @NotNull SettingsState settings) {
        AIProviderType providerType = settings.providerType != null ? settings.providerType : AIProviderType.QIANWEN;
        SettingsState.ProviderConfig defaultConfig = settings.defaultProviders.get(providerType);
        if (defaultConfig != null) {
            for (int i = 0; i < items.size(); i++) {
                SettingsState.ProviderConfig config = items.get(i);
                if (config.providerType == providerType && Objects.equals(config.md5, defaultConfig.md5)) {
                    return i;
                }
            }
        }

        for (int i = 0; i < items.size(); i++) {
            SettingsState.ProviderConfig config = items.get(i);
            if (config.providerType == providerType) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 切换默认服务商。
     *
     * <p>该方法会更新 {@link SettingsState#providerType} 字段，并将 {@code selectedConfig} 写入
     * {@link SettingsState#defaultProviders} 中对应的条目。写入时会创建 {@link SettingsState.ProviderConfig}
     * 的深拷贝, 避免引用共享导致的数据污染。
     *
     * @param settings       插件配置
     * @param selectedConfig 被选中的服务商配置
     * @throws IllegalArgumentException 当服务商类型缺失时抛出
     */
    public static void switchDefaultProvider(@NotNull SettingsState settings,
                                             @NotNull SettingsState.ProviderConfig selectedConfig) {
        AIProviderType providerType = selectedConfig.providerType;
        if (providerType == null) {
            throw new IllegalArgumentException(JavaDocBundle.message("statusbar.provider.error.missing.type"));
        }

        SettingsState.ProviderConfig configCopy = new SettingsState.ProviderConfig(selectedConfig);
        configCopy.providerType = providerType;

        settings.providerType = providerType;
        settings.updateDefaultProviderConfig(providerType, configCopy);
    }

    /**
     * 构建服务商展示名称（服务商名:模型名）。
     *
     * @param config 服务商配置
     * @return 展示名称
     */
    @NotNull
    public static String getProviderDisplayText(@NotNull SettingsState.ProviderConfig config) {
        AIProviderType providerType = config.providerType != null ? config.providerType : AIProviderType.QIANWEN;
        String providerName = providerType.getDisplayName();
        String modelName = config.modelName;
        if (modelName == null || modelName.trim().isEmpty()) {
            return providerName;
        }
        return providerName + ":" + modelName;
    }
}
