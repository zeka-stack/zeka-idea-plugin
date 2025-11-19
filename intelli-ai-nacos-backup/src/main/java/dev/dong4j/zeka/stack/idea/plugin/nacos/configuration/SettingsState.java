package dev.dong4j.zeka.stack.idea.plugin.nacos.configuration;

import lombok.Data;

/**
 * 插件配置状态
 *
 * <p>存储插件的配置信息，包括 Nacos 服务器地址、用户名等。
 * 使用 PersistentStateComponent 进行持久化存储。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
@Data
public class SettingsState {
    /**
     * Nacos 服务器地址
     */
    public String serverAddr = "";

    /**
     * Nacos 认证用户名
     */
    public String username = "";

    /**
     * 配置类型（YAML/JSON）
     */
    public Type type = Type.YAML;

    /**
     * 是否为全局管理员
     */
    public boolean globalAdmin = false;

    /**
     * 是否已认证
     */
    public boolean isAuthed = false;

    /**
     * 配置类型枚举
     */
    public enum Type {
        /** YAML 类型 */
        YAML,
        /** JSON 类型 */
        JSON
    }

    /**
     * 检查是否已认证
     *
     * @return 如果已认证返回 true
     */
    public boolean isAuthed() {
        return isAuthed;
    }
}

