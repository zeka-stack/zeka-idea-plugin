package dev.dong4j.zeka.stack.idea.plugin.nacos.model;

/**
 * 注册中心上下文抽象类
 * 定义了资源清理的基本契约
 *
 * @author dong4j
 * @since 1.0.0
 */
public abstract class RegistryContext {

    /**
     * 清理资源
     */
    public abstract void cleanResource();
}
