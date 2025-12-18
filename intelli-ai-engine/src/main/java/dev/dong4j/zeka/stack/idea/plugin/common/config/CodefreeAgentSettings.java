package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Codefree 本地代理的配置项
 */
public class CodefreeAgentSettings {
    /** 是否在保存后自动尝试启动本地代理 */
    public boolean autoStart = false;
    /** jar 下载地址 */
    public String downloadUrl = "";
    /** 本地已下载的 jar 文件名 */
    public String jarFileName = "";

    /**
     * 创建配置副本
     */
    @NotNull
    public CodefreeAgentSettings copy() {
        CodefreeAgentSettings settings = new CodefreeAgentSettings();
        settings.autoStart = this.autoStart;
        settings.downloadUrl = this.downloadUrl;
        settings.jarFileName = this.jarFileName;
        return settings;
    }

    /**
     * 内容比对
     */
    public boolean contentEquals(@NotNull CodefreeAgentSettings other) {
        return autoStart == other.autoStart
               && Objects.equals(downloadUrl, other.downloadUrl)
               && Objects.equals(jarFileName, other.jarFileName);
    }
}
