package dev.dong4j.zeka.stack.idea.plugin.terminal.settings;

import com.intellij.ide.util.PropertiesComponent;

/**
 * 提示词版本存储
 * <p>使用 PropertiesComponent 持久化默认提示词版本与提醒版本，避免因 SettingsState 未落盘导致版本丢失。</p>
 */
public final class PromptTemplateVersionStore {

    /** 提示词模板版本键名, 用于持久化存储默认提示词版本. */
    private static final String KEY_PROMPT_VERSION = "terminal.prompt.template.version";
    /** 用于存储提示词模板通知版本的键名, 持久化在 PropertiesComponent 中, 避免 SettingsState 未落盘导致版本丢失. */
    private static final String KEY_PROMPT_NOTICE_VERSION = "terminal.prompt.template.notice.version";

    /**
     * 私有构造函数, 防止外部实例化该工具类
     * <p> 该类为工具类, 所有方法均为静态, 通过类名直接调用, 禁止创建实例 </p>
     */
    private PromptTemplateVersionStore() {
    }

    /**
     * 获取提示词模板版本号
     * <p> 从持久化配置中读取当前提示词模板的版本号, 若未设置则默认返回 0</p>
     *
     * @return 提示词模板版本号, 若未设置则返回 0
     */
    public static int getPromptTemplateVersion() {
        return PropertiesComponent.getInstance().getInt(KEY_PROMPT_VERSION, 0);
    }

    /**
     * 获取提示词模板通知版本号
     * <p> 从持久化配置中读取提示词模板通知版本号, 若未设置则默认返回 0</p>
     *
     * @return 提示词模板通知版本号, 若未设置则返回 0
     */
    public static int getPromptTemplateNoticeVersion() {
        return PropertiesComponent.getInstance().getInt(KEY_PROMPT_NOTICE_VERSION, 0);
    }

    /**
     * 设置提示词模板版本
     * <p> 将指定版本号持久化存储到配置组件中, 用于后续版本管理与提示更新.</p>
     *
     * @param version 要设置的提示词模板版本号
     */
    public static void setPromptTemplateVersion(int version) {
        PropertiesComponent.getInstance().setValue(KEY_PROMPT_VERSION, version, 0);
    }

    /**
     * 设置提示词模板通知版本
     * <p> 将指定版本号持久化存储到 PropertiesComponent 中, 用于记录提示词模板的最新通知版本.</p>
     *
     * @param version 要设置的提示词模板通知版本号
     */
    public static void setPromptTemplateNoticeVersion(int version) {
        PropertiesComponent.getInstance().setValue(KEY_PROMPT_NOTICE_VERSION, version, 0);
    }
}
