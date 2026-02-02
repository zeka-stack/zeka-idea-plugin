package dev.dong4j.zeka.stack.idea.plugin.repairer.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * Repairer 插件设置状态类
 * <p>
 * 用于管理 Repairer 插件的用户配置和持久化状态，包括 AI 服务商配置与系统提示词。
 * 该类实现了 {@link PersistentStateComponent} 接口，支持与 IntelliJ 平台的持久化机制集成。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@State(
    name = "RepairerPluginSettings",
    storages = @Storage("zeka.stack.repairer.plugin.xml")
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
     * 是否显示高级设置（提示词）
     * <p>
     * 默认 false，勾选后显示系统提示词编辑区域。
     */
    public boolean showAdvancedSettings = false;

    /**
     * 系统提示词，用于代码修复时作为 system 消息发给 AI。
     * <p>
     * 未设置或为空时使用 {@link #getDefaultSystemPrompt()}。
     */
    public String systemPrompt = getDefaultSystemPrompt();

    /**
     * 用户提示词模板，占位符：{tool}、{ruleId}、{message}、{snippet}。
     * <p>
     * 未设置或为空时使用 {@link #getDefaultUserPromptTemplate()}。
     */
    public String userPromptTemplate = getDefaultUserPromptTemplate();

    /**
     * 增强版用户提示词模板，占位符：{tool}、{ruleId}、{message}、{snippet}、{context}。
     * <p>
     * 未设置或为空时使用 {@link #getDefaultEnhancedUserPromptTemplate()}。
     */
    public String enhancedUserPromptTemplate = getDefaultEnhancedUserPromptTemplate();

    /**
     * 获取默认系统提示词
     *
     * @return 默认系统提示词
     */
    @NotNull
    public static String getDefaultSystemPrompt() {
        return """
            你是一个 IntelliJ IDEA 插件中的代码修复引擎。
            你只允许修改提供的代码片段，不得假设其他上下文。
            你必须输出统一 diff（unified diff），不要输出解释。
            diff 必须只包含对给定代码片段的修改，文件名固定为 a/snippet 和 b/snippet。
            """;
    }

    /**
     * 获取默认用户提示词模板（占位符：{tool}、{ruleId}、{message}、{snippet}）
     *
     * @return 默认用户提示词模板
     */
    @NotNull
    public static String getDefaultUserPromptTemplate() {
        return """
            以下是静态代码分析工具检测出的代码问题。

            【规则信息】
            - 工具：{tool}
            - Rule：{ruleId}
            - 描述：{message}

            【原始代码片段】
            <<<CODE>>>
            {snippet}
            <<<END>>>

            【要求】
            - 仅修改上述代码片段
            - 保持原有语义不变
            - 修复该规则问题
            - 不引入额外格式化
            - 仅输出统一 diff（unified diff）
            - diff 只针对上述片段，文件名使用 a/snippet 和 b/snippet
            - 不要输出任何解释或额外文本
            """;
    }

    /**
     * 获取默认增强版用户提示词模板（占位符：{tool}、{ruleId}、{message}、{snippet}、{context}）
     *
     * @return 默认增强版用户提示词模板
     */
    @NotNull
    public static String getDefaultEnhancedUserPromptTemplate() {
        return """
            以下是静态代码分析工具检测出的代码问题。

            【规则信息】
            - 工具：{tool}
            - Rule：{ruleId}
            - 描述：{message}

            【原始代码片段】
            <<<CODE>>>
            {snippet}
            <<<END>>>

            【上下文信息】
            <<<CONTEXT>>>
            {context}
            <<<END>>>

            【要求】
            - 仅修改上述代码片段，不得修改上下文
            - 保持原有语义不变
            - 修复该规则问题
            - 保持与原始代码相同的缩进和格式
            - 不引入额外的变量或方法
            - 不改变代码的执行逻辑
            - 仅输出统一 diff（unified diff）
            - diff 只针对上述片段，文件名使用 a/snippet 和 b/snippet
            - 不要输出任何解释或额外文本
            """;
    }

    /**
     * 获取 SettingsState 的单例实例
     *
     * @return SettingsState 的单例实例
     */
    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    /**
     * 获取当前状态实例
     * <p> 返回当前 SettingsState 的实例对象, 用于持久化存储和恢复插件配置.
     *
     * @return 当前 SettingsState 实例, 如果未初始化则返回 null
     */
    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    /**
     * 加载持久化状态
     * <p> 将传入的状态对象中的数据复制到当前对象中
     *
     * @param state 状态对象
     */
    @Override
    public void loadState(@NotNull SettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
