package dev.dong4j.zeka.stack.idea.plugin.swagger.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * Swagger 插件设置状态类
 * <p> 用于管理 Swagger 插件的用户配置和持久化状态, 包括 AI 提供商配置, 高级设置开关, 系统提示词和 Swagger 模板等内容.
 * <p> 该类实现了 {@link PersistentStateComponent} 接口, 支持与 IntelliJ 平台的持久化机制集成, 确保设置在 IDE 重启后仍然保留.
 * <p> 通过 {@link #getInstance()} 方法可获取全局唯一的设置实例, 便于在插件的各个模块中访问和修改配置.
 * <p> 支持自定义系统提示词和 Swagger 模板, 以适应不同用户的开发需求和项目规范.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.02
 * @since 1.0.0
 */
@State(
    name = "SwaggerPluginSettings",
    storages = @Storage("zeka.stack.swagger.plugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    /**
     * AI 服务商配置
     * <p> 插件使用的默认服务商, 从全局可用服务商列表中选取.
     * 全局服务商配置在 Settings → Tools → IntelliAI Engine 中管理.
     */
    public AIProviderConfig providerConfig;

    /**
     * 是否显示高级设置
     * <p> 控制设置页面中高级设置区域的显示 / 隐藏.
     * 高级设置包括 Prompt 模板配置.
     * 用户可以通过复选框控制是否显示高级设置, 减少设置页面长度.
     *
     * <p> 默认值: false(默认隐藏, 减少页面长度)
     */
    public boolean showAdvancedSettings = false;

    /** 默认系统提示词, 用于初始化 AI 角色和行为准则, 作为 system 消息发送给 AI 服务. */
    public String systemPrompt = getDefaultSystemPrompt();

    /**
     * Swagger 提示词模板
     * <p>用于生成 Swagger 内容的提示词模板.
     * 使用 {content} 作为占位符.
     * <p>默认值: getDefaultSwaggerTemplate()
     */
    public String swaggerTemplate = getDefaultSwaggerTemplate();

    /**
     * 获取 SettingsState 的单例实例
     *
     * <p> 通过 IntelliJ 平台的 ApplicationManager 获取当前应用上下文中注册的服务实例.
     * 该方法确保在整个应用生命周期中返回同一个配置状态对象.
     *
     * @return SettingsState 的单例实例
     */
    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    /**
     * 获取当前状态实例
     * <p> 实现 PersistentStateComponent 接口, 返回当前 SettingsState 实例
     * <p> 此方法用于持久化组件获取当前状态, 通常由 IDE 持久化框架调用
     *
     * @return 当前 SettingsState 实例, 如果未初始化则返回 null
     */
    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    /**
     * 加载状态数据
     * <p> 从指定的状态对象中加载数据到当前实例, 使用 XmlSerializerUtil.copyBean 进行属性复制
     * <p> 该方法通常由 IDE 持久化框架调用, 用于恢复插件的配置状态
     *
     * @param state 要加载的状态对象, 不能为 null
     */
    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // ==================== 默认提示词模板方法 ====================

    /**
     * 获取默认的系统提示词
     * <p> 返回一个默认的系统提示词模板, 用于初始化 AI 角色和行为准则.
     * 该提示词会作为 system 消息发送给 AI 服务, 用于建立 AI 的基本角色和响应风格.
     * <p> 默认提示词内容:
     * <pre>{@code
     * 你是一位经验丰富的软件开发助手.
     * 你的目标是帮助开发者完成各种开发任务.
     * 你总是提供清晰, 准确, 有用的建议和解决方案.
     * }</pre>
     *
     * @return 默认的系统提示词
     */
    @NotNull
    public static String getDefaultSystemPrompt() {
        return """
            你是一位经验丰富的软件开发助手。
            你的目标是帮助开发者完成各种开发任务。
            你总是提供清晰、准确、有用的建议和解决方案。
            """;
    }

    /**
     * 获取默认的 Swagger 模板
     * <p> 用于生成 Swagger 注解建议的默认模板, 包含内容占位符和格式要求.
     *
     * @return 默认的 Swagger 模板字符串
     */
    @NotNull
    public static String getDefaultSwaggerTemplate() {
        return """
            请根据以下内容生成 Swagger 注解建议：

            {content}

            要求：
            1. 生成的内容要清晰、准确
            2. 符合最佳实践
            3. 仅返回可直接写回源码的注解片段
            """;
    }
}
