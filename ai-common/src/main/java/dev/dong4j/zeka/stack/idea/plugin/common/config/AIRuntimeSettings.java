package dev.dong4j.zeka.stack.idea.plugin.common.config;

/**
 * AI 请求运行时设置。
 */
public class AIRuntimeSettings {
    /** 最大重试次数, 用于控制在发生异常时最多尝试的次数 */
    public int maxRetries = 2;
    /** 超时时间, 单位为毫秒, 默认值为 10000 */
    public int timeout = 10000;
    /**
     * 等待持续时间, 单位为毫秒
     */
    public long waitDuration = 5000;
    /** 是否启用详细日志记录,true 表示启用,false 表示禁用 */
    public boolean verboseLogging = false;

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
        settings.verboseLogging = this.verboseLogging;
        return settings;
    }
}
