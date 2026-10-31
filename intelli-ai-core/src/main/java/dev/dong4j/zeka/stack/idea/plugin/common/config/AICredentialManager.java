package dev.dong4j.zeka.stack.idea.plugin.common.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * AI 凭证管理器
 * <p>
 * 提供 API Key 等敏感凭证的统一存取入口. 凭证的具体存储设施由宿主通过
 * {@link AICredentialStore} SPI 提供(IDE 宿主为 PasswordSafe, MCP Server 宿主为配置文件/环境变量),
 * 本类只负责凭证键的拼装与异步回调的线程调度.
 * <p>
 * 线程模型: {@link #loadApiKeyAsync(String, Consumer)} 在后台执行器中读取凭证,
 * 并在 UI 执行器中回调, 两个执行器默认提供保守实现(同步/公共池),
 * IDE 宿主应在启动时通过 {@link #setExecutors(Executor, Executor)} 注册为
 * IntelliJ 的 pooled thread 与 invokeLater, 以保持与原实现一致的线程行为.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email "mailto:zeka.stack@gmail.com"
 * @date 2025.11.30
 * @since 1.0.0
 */
public final class AICredentialManager {

    /** 宿主注册的凭证存储实现, 全局共享 */
    private static volatile AICredentialStore credentialStore;

    /** UI 线程执行器: 默认同步执行, IDE 宿主应注册为 invokeLater */
    private static volatile Executor uiExecutor = Runnable::run;

    /** 后台执行器: 默认公共线程池, IDE 宿主应注册为 pooled thread */
    private static volatile Executor backgroundExecutor = ForkJoinPool.commonPool();

    /** 凭证服务名称, 用于在凭证设施中标识当前应用 */
    private final String serviceName;

    /** 凭证键前缀, 用于区分同服务下的不同凭证 */
    private final String keyPrefix;

    /**
     * 注册宿主的凭证存储实现
     * <p>
     * 必须在首次凭证读写前调用; 重复注册以最后一次为准.
     *
     * @param store 宿主提供的凭证存储实现, 不能为空
     */
    public static void setCredentialStore(@NotNull AICredentialStore store) {
        credentialStore = Objects.requireNonNull(store);
    }

    /**
     * 注册宿主的线程调度器
     * <p>
     * IDE 宿主应传入 pooled thread 执行器(background)与 invokeLater 执行器(ui),
     * 保证 {@link #loadApiKeyAsync(String, Consumer)} 的回调发生在 UI 线程.
     *
     * @param ui         UI 线程执行器, 不能为空
     * @param background 后台执行器, 不能为空
     */
    public static void setExecutors(@NotNull Executor ui, @NotNull Executor background) {
        uiExecutor = Objects.requireNonNull(ui);
        backgroundExecutor = Objects.requireNonNull(background);
    }

    /**
     * 初始化 AICredentialManager 实例
     * <p>
     * 使用指定的服务名称和密钥前缀创建凭证管理器
     *
     * @param serviceName 服务名称, 用于标识不同的 AI 服务
     * @param keyPrefix   密钥前缀, 用于构建存储凭证的键
     */
    public AICredentialManager(@NotNull String serviceName, @NotNull String keyPrefix) {
        this.serviceName = serviceName;
        this.keyPrefix = keyPrefix;
    }

    /**
     * 根据凭证 ID 获取 API 密钥
     * <p>
     * 通过传入的凭证 ID 拼装凭证键, 委托宿主注册的 {@link AICredentialStore} 读取.
     *
     * @param credentialId 凭证 ID
     * @return API 密钥字符串, 若凭证 ID 为空或无效则返回 null
     */
    @Nullable
    public String getApiKey(@Nullable String credentialId) {
        if (credentialId == null || credentialId.isBlank()) {
            return null;
        }
        return requireStore().getPassword(serviceName, buildKey(credentialId));
    }

    /**
     * 设置 API 密钥信息
     * <p>
     * 根据提供的凭证 ID 拼装凭证键, 委托宿主注册的 {@link AICredentialStore} 写入.
     *
     * @param credentialId 凭证 ID, 不能为空
     * @param apiKey       API 密钥, 不能为空
     */
    public void setApiKey(@NotNull String credentialId, @NotNull String apiKey) {
        requireStore().setPassword(serviceName, buildKey(credentialId), apiKey);
    }

    /**
     * 删除指定凭证的 API 密钥
     * <p>
     * 如果 {@code credentialId} 为 {@code null} 或空字符串, 则不执行任何操作.
     *
     * @param credentialId 要删除的凭证 ID, 可能为 {@code null}
     */
    public void deleteApiKey(@Nullable String credentialId) {
        if (credentialId == null || credentialId.isBlank()) {
            return;
        }
        requireStore().setPassword(serviceName, buildKey(credentialId), null);
    }

    /**
     * 异步加载指定凭证 ID 对应的 API 密钥
     * <p>
     * 在后台执行器中读取凭证, 并在 UI 执行器中通过回调返回结果;
     * 凭证 ID 为空时直接在 UI 执行器中回调 null.
     *
     * @param credentialId 凭证 ID, 可能为 null
     * @param callback     回调函数, 用于接收 API 密钥结果
     */
    public void loadApiKeyAsync(@Nullable String credentialId, @NotNull Consumer<String> callback) {
        if (credentialId == null || credentialId.isBlank()) {
            uiExecutor.execute(() -> callback.accept(null));
            return;
        }
        backgroundExecutor.execute(() -> {
            String apiKey = getApiKey(credentialId);
            uiExecutor.execute(() -> callback.accept(apiKey));
        });
    }

    /**
     * 拼装凭证键
     *
     * @param credentialId 凭证 ID, 不能为空
     * @return 前缀 + 凭证 ID
     */
    @NotNull
    private String buildKey(@NotNull String credentialId) {
        return keyPrefix + Objects.requireNonNull(credentialId);
    }

    /**
     * 获取已注册的凭证存储实现
     *
     * @return 凭证存储实现
     * @throws IllegalStateException 宿主未注册凭证存储时抛出
     */
    @NotNull
    private static AICredentialStore requireStore() {
        AICredentialStore store = credentialStore;
        if (store == null) {
            throw new IllegalStateException(
                "AICredentialStore 未注册: 宿主必须在凭证读写前调用 AICredentialManager.setCredentialStore()");
        }
        return store;
    }
}
