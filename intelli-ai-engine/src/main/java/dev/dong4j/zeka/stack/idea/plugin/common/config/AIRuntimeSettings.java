package dev.dong4j.zeka.stack.idea.plugin.common.config;

/**
 * AI 运行时配置类
 * <p>
 * 用于配置 AI 服务运行时的各种参数, 包括重试次数, 超时时间, 等待时长和日志输出等设置
 * 提供了配置复制和超时时间转换等功能
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public class AIRuntimeSettings {
    /** 最大重试次数, 用于控制在发生异常时最多尝试的次数 */
    public int maxRetries = 2;
    /** 超时时间, 单位为秒, 默认值为 10 */
    public int timeout = 10;
    /**
     * 等待持续时间, 单位为毫秒
     */
    public long waitDuration = 5000;

    /**
     * 创建当前运行时设置的副本
     * <p>
     * 复制当前对象的所有属性到一个新的 AIRuntimeSettings 实例中, 并返回该实例.
     *
     * @return 当前运行时设置的副本
     */
    public AIRuntimeSettings copy() {
        AIRuntimeSettings settings = new AIRuntimeSettings();
        settings.maxRetries = this.maxRetries;
        settings.timeout = this.timeout;
        settings.waitDuration = this.waitDuration;
        return settings;
    }

    /**
     * 获取以毫秒为单位的超时时间.
     * <p>
     * 配置页面中使用秒为单位, 在执行网络请求时需要转换为毫秒.
     *
     * @return 以毫秒表示的超时时长
     * @since 1.0.1
     */
    public int getTimeoutInMillis() {
        return Math.max(1, timeout) * 1000;
    }

}
