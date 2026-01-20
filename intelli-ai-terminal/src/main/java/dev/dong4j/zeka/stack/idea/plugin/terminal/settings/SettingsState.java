package dev.dong4j.zeka.stack.idea.plugin.terminal.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;

/**
 * 设置状态类
 * <p> 用于存储和管理终端插件的配置信息, 包括 AI 提供商配置, 提示模板版本, 触发前缀等参数.
 * 该类实现了持久化功能, 支持从 XML 存储中加载和保存状态.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.20
 * @since 1.0.0
 */
@State(
    name = "TerminalPluginSettings",
    storages = @Storage("zeka.stack.terminal.plugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {
    /** 默认提示词版本号 (仅在默认提示词更新时递增) */
    public static final int PROMPT_TEMPLATE_VERSION = 4;

    /** AI 服务商配置 <p> 插件使用的默认服务商, 从全局可用服务商列表中选取. 全局服务商配置在 Settings → Tools → IntelliAI Engine 中管理. */
    public AIProviderConfig providerConfig;

    /** 是否显示高级设置 (包含 Prompt 模板配置), 默认为 false(隐藏) */
    public boolean showAdvancedSettings = false;

    /** 是否启用 Terminal AI, 控制是否在 Terminal 中拦截触发前缀并执行 AI 生成逻辑. */
    public boolean enableTerminalAI = true;

    /**
     * 触发前缀
     * <p> 只有当当前输入行以该前缀开头时才触发 AI 生成.
     */
    public String triggerPrefix = "#";

    /**
     * 系统提示词模板
     * <p> 用于设定 AI 角色和行为准则的系统提示词.
     * 这个提示词会作为 system 消息发送给 AI 服务,
     * 用于建立 AI 的基本角色和响应风格.
     *
     * <p> 默认值: getDefaultSystemPrompt()
     */
    public String systemPrompt = getDefaultSystemPrompt();

    /**
     * Terminal 提示词模板
     * <p> 用于生成终端内容的提示词模板, 使用 {content} 作为占位符.
     * <p> 默认值: {@link #getDefaultUserPrompt()}
     */
    public String terminalTemplate = getDefaultUserPrompt();

    /** 当前提示词版本号 (用户自定义不改变此值) */
    public int promptTemplateVersion = PROMPT_TEMPLATE_VERSION;
    /** 提示通知已展示过的版本号, 用于避免重复提示 */
    public int promptTemplateNoticeVersion = 0;

    /**
     * 获取 SettingsState 的单例实例
     * <p> 返回插件的配置状态实例, 用于访问和修改插件的全局设置.
     *
     * @return 当前插件的配置状态实例
     */
    public static SettingsState getInstance() {
        return ApplicationManager.getApplication().getService(SettingsState.class);
    }

    /**
     * 获取 SettingsState 的单例实例
     * <p> 返回当前配置状态对象的实例, 用于访问插件的配置信息.
     *
     * @return 当前 SettingsState 实例, 不会为 null
     */
    @Override
    public @Nullable SettingsState getState() {
        return this;
    }

    /**
     * 加载插件配置状态数据
     * <p> 从给定的状态对象中复制配置信息到当前实例, 用于恢复插件的配置状态.
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
     * <p> 用于设定 AI 角色和行为准则的系统提示词. 这个提示词会作为 system 消息发送给 AI 服务, 用于建立 AI 的基本角色和响应风格.</p>
     *
     * @return 默认的系统提示词
     */
    @NotNull
    public static String getDefaultSystemPrompt() {
        return
            """
                你是一个命令行助手.

                你的任务是：只生成【可以直接执行的 shell 命令】

                【强制规则】
                - 只输出 shell 命令本身，不允许输出任何解释、说明或提示文字
                - 不允许输出注释（包括 #、//、/* */）
                - 不允许输出 Markdown、代码块标记（```）
                - 不允许输出多余的空行或空格
                - 输出内容必须可以直接在 bash / zsh（macOS / Linux）中执行

                【格式规则】
                - 如果命令较长，必须使用 "\\" 进行换行
                - 使用 "\\" 换行时，每一行在拼接后必须是合法的 shell 命令
                - 不要为了换行而改变命令语义
                - 除非 shell 语法必须，否则不要额外转义字符

                【行为约束】
                - 不允许向用户提问
                - 不允许给出多个备选方案
                - 如果需要多个步骤，使用 "&&" 串联为一条命令
                - 不允许输出 <path>、<file>、<xxx> 等占位符

                【兜底规则】
                - 如果用户请求无法转换为明确可执行的 shell 命令，请直接输出空内容
                """;
    }

    /**
     * 获取默认的 Terminal 用户提示词模板
     * <p> 用于生成终端内容的提示词模板, 使用 {content} 作为占位符.
     * <p> 该模板会发送给 AI 服务, 用于根据用户描述生成合适的 shell 命令.
     *
     * @return 默认的 Terminal 用户提示词模板
     */
    @NotNull
    public static String getDefaultUserPrompt() {
        return """
            根据下面的描述，生成最合适的一条 shell 命令:
            {content}
            """;
    }

    /**
     * 判断是否使用默认提示词
     * <p> 比较当前配置的系统提示词和终端提示词与默认提示词是否一致, 若一致则返回 true.
     *
     * @return 使用默认提示词则返回 true, 否则返回 false
     */
    public boolean isUsingDefaultPrompts() {
        return getDefaultSystemPrompt().equals(systemPrompt)
               && getDefaultUserPrompt().equals(terminalTemplate);
    }
}
