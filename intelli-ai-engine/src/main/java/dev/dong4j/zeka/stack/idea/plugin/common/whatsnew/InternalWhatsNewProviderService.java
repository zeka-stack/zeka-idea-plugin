package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.Collection;

/**
 * 内部新特性提供者服务类
 * <p> 实现新特性提供者接口, 用于获取插件中注册的新特性提供者扩展点列表
 * <p> 通过扩展点机制加载所有实现了 WhatsNewProvider 接口的组件
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public class InternalWhatsNewProviderService implements WhatsNewProviderService {
    /**
     * 用于标识和获取 WhatsNewProvider 扩展点的名称
     * <p> 该扩展点用于注册和获取新特性提供者
     *
     * @see ExtensionPointName
     * @see WhatsNewProvider
     */
    private static final ExtensionPointName<WhatsNewProvider> EP_NAME =
        ExtensionPointName.create("dev.dong4j.zeka.stack.idea.plugin.common.ai.whatsNewProvider");

    /**
     * 获取所有 "What's New" 提供者
     * <p> 通过扩展点 {@code EP_NAME} 获取已注册的 {@link WhatsNewProvider} 实现类集合
     *
     * @return 包含所有 What's New 提供者的集合, 可能为空
     * @since hello.world
     */
    @Override
    public Collection<WhatsNewProvider> getWhatsNewProviders() {
        return EP_NAME.getExtensionList();
    }
}
