package dev.dong4j.zeka.stack.idea.plugin.common.config;

/**
 * AI 请求运行时设置。
 */
public class AIRuntimeSettings {
    public int maxRetries = 2;
    public int timeout = 10000;
    public long waitDuration = 5000;
    public boolean verboseLogging = false;

    public AIRuntimeSettings copy() {
        AIRuntimeSettings settings = new AIRuntimeSettings();
        settings.maxRetries = this.maxRetries;
        settings.timeout = this.timeout;
        settings.waitDuration = this.waitDuration;
        settings.verboseLogging = this.verboseLogging;
        return settings;
    }
}
