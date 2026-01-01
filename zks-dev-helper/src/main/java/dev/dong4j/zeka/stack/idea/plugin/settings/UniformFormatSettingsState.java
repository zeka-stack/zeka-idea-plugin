package dev.dong4j.zeka.stack.idea.plugin.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Data;

/**
 * ZKS Dev Helper 插件设置状态类
 * <p>
 * 用于保存和管理 ZKS Dev Helper 插件的配置状态，包括代码样式模块（文件模板、Live Template、代码风格配置）等选项的启用状态。
 * 该类实现了持久化状态接口，支持将配置状态存储到 XML 文件中，并在需要时加载配置。
 * 后续会扩展支持 MyBatis、Proxyer 等其他功能模块的配置。
 *
 * @author dong4j
 * @version 1.0.0
 * @date 2025.10.25
 * @since 1.0.0
 */
@Data
@State(
    name = "ZKSDevHelperSettingsState",
    storages = @Storage("zeka.stack.dev.helper.xml")
)
public class UniformFormatSettingsState implements PersistentStateComponent<UniformFormatSettingsState> {
    // ========== 代码样式模块配置 ==========
    /** 是否启用文件模板功能，默认启用 */
    private boolean enableFileTemplates = true;
    /** 是否启用 Live Template 功能，默认启用 */
    private boolean enableLiveTemplates = true;
    /** 是否启用代码风格配置，默认启用 */
    private boolean enableCodeStyle = true;
    /** 代码样式在线更新配置 */
    @Nullable
    private CodeStyleUpdateSettings codeStyleUpdateSettings;

    // ========== 后续功能模块配置将在此添加 ==========
    // MyBatis 模块配置
    // Proxyer 模块配置
    // ...

    /**
     * 代码样式在线更新配置
     */
    @Data
    public static class CodeStyleUpdateSettings {
        /** 是否启用自动更新，默认 false */
        private boolean autoUpdate = false;
        /** 下载地址 */
        @Nullable
        private String downloadUrl;
        /** 是否设置为全局方案（IDE级别），默认 false（仅项目级别） */
        private boolean useGlobalScheme = false;
    }

    /**
     * 获取当前对象的统一格式设置状态
     * <p>
     * 返回当前对象的统一格式设置状态，该方法用于获取格式化配置信息。
     *
     * @return 当前对象的统一格式设置状态，可能为 null
     */
    @Nullable
    @Override
    public UniformFormatSettingsState getState() {
        return this;
    }

    /**
     * 加载状态信息到当前对象
     * <p>
     * 通过复制传入的 UniformFormatSettingsState 对象的状态到当前对象中，实现状态的加载。
     *
     * @param state 要加载状态的 UniformFormatSettingsState 对象
     */
    @Override
    public void loadState(@NotNull UniformFormatSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    /**
     * 获取 UniformFormatSettingsState 的单例实例
     * <p>
     * 通过 ApplicationManager 获取应用实例，并从中获取 UniformFormatSettingsState 服务对象。
     *
     * @return UniformFormatSettingsState 的单例实例
     */
    public static UniformFormatSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(UniformFormatSettingsState.class);
    }
}
