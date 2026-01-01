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
 * 插件设置状态管理
 * 使用 @State 注解自动持久化配置
 *
 * @author dong4j
 * @since 1.0.0
 */
@State(
    name = "SwaggerPluginSettings",
    storages = @Storage("zeka.stack.swagger.plugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    /**
     * AI 服务商配置
     * <p>
     * 插件使用的默认服务商，从全局可用服务商列表中选取。
     * 全局服务商配置在 Settings → Tools → IntelliAI Engine 中管理。
     */
    public AIProviderConfig providerConfig;

    /**
     * 是否显示高级设置
     * <p>
     * 控制设置页面中高级设置区域的显示/隐藏。
     * 高级设置包括 Prompt 模板配置。
     * 用户可以通过复选框控制是否显示高级设置，减少设置页面长度。
     *
     * <p>默认值: false（默认隐藏，减少页面长度）
     */
    public boolean showAdvancedSettings = false;

    /**
     * 系统提示词模板
     *
     * <p>用于设定 AI 角色和行为准则的系统提示词。
     * 这个提示词会作为 system 消息发送给 AI 服务，
     * 用于建立 AI 的基本角色和响应风格。
     *
     * <p>默认值: getDefaultSystemPrompt()
     */
    public String systemPrompt = getDefaultSystemPrompt();

    /**
     * Swagger 提示词模板
     *
     * <p>用于生成 Swagger 内容的提示词模板。
     * 使用 {content} 作为占位符。
     *
     * <p>默认值: getDefaultSwaggerTemplate()
     */
    public String swaggerTemplate = getDefaultSwaggerTemplate();

    /**
     * 获取 SettingsState 的单例实例
     *
     * @return SettingsState 的实例
     */
    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    // ==================== 默认提示词模板方法 ====================

    /**
     * 获取默认的系统提示词
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
     *
     * @return 默认的 Swagger 模板
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
