package dev.dong4j.zeka.stack.idea.plugin.common.whatsnew;

import java.util.Collection;

/**
 * 新鲜事提供者服务接口
 * <p> 定义了获取新鲜事提供者的接口方法. 实现该接口的服务类需提供获取新鲜事提供者集合的功能.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.12.31
 * @since 1.0.0
 */
public interface WhatsNewProviderService {
    /**
     * 获取所有已注册的提供者集合
     *
     * @return 提供者集合
     */
    Collection<WhatsNewProvider> getWhatsNewProviders();
}
