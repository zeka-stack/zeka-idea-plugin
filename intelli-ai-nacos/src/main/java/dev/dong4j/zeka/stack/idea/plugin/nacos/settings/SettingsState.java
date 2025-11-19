package dev.dong4j.zeka.stack.idea.plugin.nacos.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Nacos 插件设置状态管理
 * 使用 @State 注解自动持久化配置
 *
 * @author dong4j
 * @since 1.0.0
 */
@State(
    name = "NacosPluginSettings",
    storages = @Storage("zeka.stack.nacos.plugin.xml")
)
public class SettingsState implements PersistentStateComponent<SettingsState> {

    /**
     * Nacos 服务器地址
     * <p>
     * 格式：http://host:port 或 https://host:port
     */
    public String serverAddr = "";

    /**
     * 用户名
     * <p>
     * Nacos 认证用户名
     */
    public String username = "";

    /**
     * 配置类型
     * <p>
     * 默认配置类型，用于自动识别配置文件类型
     */
    public String type = "YAML";

    /**
     * 是否为全局管理员
     * <p>
     * 影响删除配置的权限控制
     */
    public boolean globalAdmin = false;

    /**
     * 是否已认证
     * <p>
     * 标识当前连接是否已通过认证
     */
    public boolean isAuthed = false;

    /**
     * 是否启用本地 Nacos 注册中心
     * <p>
     * 勾选后优先使用本地内置注册中心
     */
    public boolean useLocalRegistry = false;

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

    /**
     * 获取密码
     * <p>
     * 注意：密码通过 PasswordSafe 安全存储，不会序列化到配置文件中
     *
     * @return 密码
     */
    @Transient
    public String getPassword() {
        CredentialAttributes credentialAttributes = createCredentialAttributes();
        Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    /**
     * 设置密码
     * <p>
     * 注意：密码通过 PasswordSafe 安全存储，不会序列化到配置文件中
     *
     * @param password 密码
     */
    @Transient
    public void setPassword(String password) {
        CredentialAttributes credentialAttributes = createCredentialAttributes();
        Credentials credentials = new Credentials(username, password);
        PasswordSafe.getInstance().set(credentialAttributes, credentials);
    }

    /**
     * 创建凭证属性
     *
     * @return 凭证属性
     */
    private CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes("IntelliAI Nacos", serverAddr + ":" + username);
    }
}

