package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.exception.NacosException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;

/**
 * Nacos 客户端工具类
 * 提供客户端实例管理和常用操作的便捷方法
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosClientUtils {
    private static final ConcurrentHashMap<String, NacosClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取默认的 Nacos 客户端实例
     * 从插件设置中读取配置信息
     *
     * @return Nacos 客户端实例，如果配置不完整则返回 null
     */
    @Nullable
    public static NacosClient getDefaultClient() {
        SettingsState settings = SettingsState.getInstance();
        String serverAddr = settings.serverAddr;
        String username = settings.username;
        String password = settings.getPassword();

        if (serverAddr == null || serverAddr.isEmpty() || username == null || username.isEmpty()) {
            return null;
        }

        return getClient(serverAddr, username, password != null ? password : "");
    }

    /**
     * 获取指定配置的 Nacos 客户端实例
     *
     * @param serverAddr Nacos 服务器地址
     * @param username   用户名
     * @param password   密码
     * @return Nacos 客户端实例
     */
    @Nullable
    public static NacosClient getClient(String serverAddr, String username, String password) {
        if (serverAddr == null || serverAddr.isEmpty() || username == null || username.isEmpty()) {
            return null;
        }

        String key = generateClientKey(serverAddr, username);
        return CLIENT_CACHE.computeIfAbsent(key, k -> {
            try {
                return NacosClient.getInstance(serverAddr, username, password);
            } catch (NacosException e) {
                // 记录错误日志
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * 生成客户端缓存键
     *
     * @param serverAddr 服务器地址
     * @param username   用户名
     * @return 缓存键
     */
    @NotNull
    private static String generateClientKey(String serverAddr, String username) {
        return serverAddr + ":" + username;
    }

    /**
     * 移除指定的客户端实例
     *
     * @param serverAddr 服务器地址
     * @param username   用户名
     */
    public static void removeClient(String serverAddr, String username) {
        String key = generateClientKey(serverAddr, username);
        CLIENT_CACHE.remove(key);
    }

    /**
     * 清空所有客户端实例
     */
    public static void clearAllClients() {
        CLIENT_CACHE.clear();
    }

    /**
     * 测试连接
     *
     * @param serverAddr Nacos 服务器地址
     * @param username   用户名
     * @param password   密码
     * @return 连接是否成功
     */
    public static boolean testConnection(String serverAddr, String username, String password) {
        try {
            NacosClient client = NacosClient.getInstance(serverAddr, username, password);
            return client.login();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 测试默认连接
     *
     * @return 连接是否成功
     */
    public static boolean testDefaultConnection() {
        SettingsState settings = SettingsState.getInstance();
        return testConnection(settings.serverAddr, settings.username, settings.getPassword());
    }
}