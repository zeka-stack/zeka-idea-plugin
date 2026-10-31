package dev.dong4j.zeka.stack.idea.plugin.common.config;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 凭证存储初始化活动
 * <p>
 * 在项目启动时把 IDE 宿主的凭证实现注册到 core 的 {@link AICredentialManager}:
 * <ul>
 *   <li>{@link PasswordSafeCredentialStore}: 基于 PasswordSafe(系统钥匙串/凭据管理器)存取 API Key</li>
 *   <li>线程执行器: 凭证读取走 pooled thread, 回调经 invokeLater 切回 UI 线程,
 *       与 core 默认实现(同步/公共池)相比保持原 UI 线程回调语义</li>
 * </ul>
 * 注册幂等, 多项目打开时重复执行无副作用. 在首个项目打开前不会发生凭证读写
 * (设置页/生成入口均需项目上下文), 因此 postStartupActivity 时机足够早.
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class CredentialStoreStartupActivity implements ProjectActivity {

    /**
     * 执行凭证存储注册
     *
     * @param project      当前项目对象
     * @param continuation 协程延续对象(Java 桥接 Kotlin suspend 接口, 直接返回 null 表示同步完成)
     * @return 固定返回 null
     */
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        AICredentialManager.setCredentialStore(new PasswordSafeCredentialStore());
        AICredentialManager.setExecutors(
            // UI 线程执行器: 与原 AICredentialManager 的 invokeLater(..., ModalityState.any()) 行为一致
            runnable -> ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any()),
            // 后台执行器: 与原实现的 executeOnPooledThread 行为一致
            runnable -> ApplicationManager.getApplication().executeOnPooledThread(runnable));
        return null;
    }

    /**
     * 基于 PasswordSafe 的凭证存储实现
     * <p>
     * PasswordSafe 底层对接系统级凭证设施(macOS Keychain / Windows Credential Manager /
     * Linux Secret Service), 与原 AICredentialManager 直连 PasswordSafe 的行为等价.
     */
    private static class PasswordSafeCredentialStore implements AICredentialStore {

        /**
         * 读取密码
         *
         * @param serviceName 服务名称
         * @param key         凭证键(已含前缀)
         * @return 密码明文, 不存在时返回 null
         */
        @Override
        public @Nullable String getPassword(@NotNull String serviceName, @NotNull String key) {
            CredentialAttributes attributes = buildAttributes(serviceName, key);
            Credentials credentials = PasswordSafe.getInstance().get(attributes);
            return credentials != null ? credentials.getPasswordAsString() : null;
        }

        /**
         * 写入或删除密码
         * <p>
         * password 为 null 时等价于删除该凭证(与 PasswordSafe 语义一致).
         *
         * @param serviceName 服务名称
         * @param key         凭证键(已含前缀)
         * @param password    密码明文, null 表示删除
         */
        @Override
        public void setPassword(@NotNull String serviceName, @NotNull String key, @Nullable String password) {
            CredentialAttributes attributes = buildAttributes(serviceName, key);
            Credentials credentials = password != null ? new Credentials(key, password) : null;
            PasswordSafe.getInstance().set(attributes, credentials);
        }

        /**
         * 构建凭证属性
         * <p>
         * 与原 AICredentialManager#buildAttributes 的键生成方式保持一致
         * (generateServiceName(serviceName, key) + CredentialAttributes(serviceName, key)),
         * 升级后已保存的 API Key 可继续读取.
         *
         * @param serviceName 服务名称
         * @param key         凭证键
         * @return 凭证属性对象
         */
        private static CredentialAttributes buildAttributes(@NotNull String serviceName, @NotNull String key) {
            String generatedServiceName = CredentialAttributesKt.generateServiceName(serviceName, key);
            return new CredentialAttributes(generatedServiceName, key);
        }
    }
}
