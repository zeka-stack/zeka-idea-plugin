package dev.dong4j.zeka.stack.idea.plugin.common.statistics;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.OptionTag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

import dev.dong4j.zeka.stack.idea.plugin.common.EngineContents;
import dev.dong4j.zeka.stack.idea.plugin.kit.StorageUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 描述: 统计设置.</p>
 *
 * @author dong4j
 * @version 1.4.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2025.01.05
 */
@State(
    name = "IntelliAIStatisticsSettings",
    storages = @Storage("intellai-statistics.xml")
)
@Slf4j
public class StatisticsSettings implements PersistentStateComponent<StatisticsSettings.State> {

    /** 配置状态 */
    private State state = new State();

    /**
     * 获取单例实例
     *
     * @return StatisticsSettings 类的单例实例
     */
    public static StatisticsSettings getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(StatisticsSettings.class);
    }

    /**
     * 获取配置状态
     * <p> 返回当前配置状态对象, 用于持久化存储.
     *
     * @return 配置状态对象, 可能为 null
     */
    @Nullable
    @Override
    public State getState() {
        return state;
    }

    /**
     * 加载状态数据
     * <p> 将传入的状态对象应用到当前实例的内部状态中
     *
     * @param state 要加载的状态对象, 不能为 null
     */
    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    /**
     * 是否启用统计
     *
     * @return 是否启用统计的布尔值
     */
    public boolean isEnableStatistics() {
        return state.enableStatistics;
    }

    /**
     * 设置启用统计
     *
     * @param enable true 表示启用统计,false 表示禁用统计
     */
    @OptionTag
    public void setEnableStatistics(boolean enable) {
        state.enableStatistics = enable;
    }

    /**
     * 获取设备 ID, 如果设备 ID 为空则生成一个新的 UUID 并保存
     *
     * @return 设备 ID 字符串
     */
    public String getDeviceId() {
        if (state.deviceId == null || state.deviceId.isEmpty()) {
            state.deviceId = DeviceIdGenerator.generateDeviceId();
        }
        return state.deviceId;
    }

    /**
     * 设置设备 ID
     *
     * @param deviceId 设备 ID
     */
    public void setDeviceId(@NotNull String deviceId) {
        state.deviceId = deviceId == null ? "" : deviceId.trim();
    }

    /**
     * 获取统计数据目录, 如果目录不存在则创建
     *
     * @return 存储统计数据的目录对象
     */
    public File getStatisticsDirectory() {
        String deviceId = DeviceIdGenerator.sanitizeForPath(getDeviceId());
        Path base = StorageUtil.getPluginStorageDir(EngineContents.PLUGIN_SIMPLE_NAME).resolve("statistics");
        File configDir = base.resolve(deviceId).toFile();
        if (!configDir.exists() && configDir.mkdirs()) {
            log.debug("Create statistics directory {}", configDir.getAbsolutePath());
        }
        return configDir;
    }

    /**
     * 是否同意隐私协议
     *
     * @return 是否同意隐私协议的布尔值
     */
    public boolean isPrivacyAgreementAccepted() {
        return state.privacyAgreementAccepted;
    }

    /**
     * 设置同意隐私协议
     *
     * @param accepted true 表示同意隐私协议, false 表示不同意
     */
    @OptionTag
    public void setPrivacyAgreementAccepted(boolean accepted) {
        state.privacyAgreementAccepted = accepted;
    }

    /**
     * 是否允许上报数据
     *
     * @return 是否允许上报数据的布尔值
     */
    public boolean isAllowUpload() {
        return state.allowUpload;
    }

    /**
     * 设置允许上报数据
     *
     * @param allow true 表示允许上报数据, false 表示不允许
     */
    @OptionTag
    public void setAllowUpload(boolean allow) {
        state.allowUpload = allow;
    }

    /** 配置状态类, 包含设备相关的配置信息. */
    public static class State {
        /** 是否同意隐私协议 */
        public boolean privacyAgreementAccepted = false;

        /** 是否启用统计 */
        public boolean enableStatistics = false;

        /** 是否允许上报数据 */
        public boolean allowUpload = false;

        /** 设备 ID */
        public String deviceId;

        /** 最后上报时间 */
        public long lastUploadTimestamp = 0;

        /**
         * 构造函数, 初始化配置状态对象
         * <p> 此构造函数会初始化一个 State 对象, 默认情况下启用统计功能被设置为 false,
         * 设备 ID 和最后上报时间被设置为默认值.
         */
        public State() {
        }
    }
}
