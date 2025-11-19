package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigInfoListResponse;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.Namespace;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.NamespaceListResponse;

/**
 * Nacos 客户端核心类
 * 提供与 Nacos 服务器交互的高级 API
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosClient {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, NacosClient> CLIENT_CACHE = new HashMap<>();

    private final ConsumServerHttpAgent httpAgent;
    private final ConsumSecurityProxy securityProxy;
    private final String serverAddr;
    private final String username;
    private final String password;
    private boolean isLoggedIn = false;

    private NacosClient(String serverAddr, String username, String password) throws NacosException {
        this.serverAddr = serverAddr;
        this.username = username;
        this.password = password;

        // 初始化 Properties
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);
        properties.setProperty(PropertyKeyConst.USERNAME, username);
        properties.setProperty(PropertyKeyConst.PASSWORD, password);

        // 创建 HTTP 代理和安全代理
        this.httpAgent = new ConsumServerHttpAgent(properties);
        this.securityProxy = new ConsumSecurityProxy(properties);
    }

    /**
     * 获取 Nacos 客户端实例（单例模式）
     *
     * @param serverAddr Nacos 服务器地址
     * @param username   用户名
     * @param password   密码
     * @return Nacos 客户端实例
     * @throws NacosException Nacos 异常
     */
    public static synchronized NacosClient getInstance(String serverAddr, String username, String password) throws NacosException {
        String key = serverAddr + ":" + username;
        NacosClient client = CLIENT_CACHE.get(key);
        if (client == null) {
            client = new NacosClient(serverAddr, username, password);
            CLIENT_CACHE.put(key, client);
        }
        return client;
    }

    /**
     * 登录到 Nacos 服务器
     *
     * @return 是否登录成功
     * @throws Exception 异常
     */
    public boolean login() throws Exception {
        // 简化登录逻辑，实际实现需要调用 Nacos 的登录 API
        // 这里只是示例，实际需要实现完整的登录流程
        this.isLoggedIn = true;
        return true;
    }

    /**
     * 获取命名空间列表
     *
     * @return 命名空间列表
     * @throws Exception 异常
     */
    public List<Namespace> getNamespaces() throws Exception {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in");
        }

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("show", "all");

        HttpRestResult<String> result = httpAgent.httpGet("/v1/console/namespaces", headers, params);

        if (result.getCode() == 200) {
            NamespaceListResponse response = OBJECT_MAPPER.readValue(result.getData(), NamespaceListResponse.class);
            return response.getNamespaces();
        } else {
            throw new Exception("Failed to get namespaces: " + result.getMessage());
        }
    }

    /**
     * 获取所有配置
     *
     * @param namespaceId 命名空间 ID
     * @param pageNo      页码
     * @param pageSize    页面大小
     * @return 配置列表
     * @throws Exception 异常
     */
    public ConfigInfoListResponse getConfigs(String namespaceId, int pageNo, int pageSize) throws Exception {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in");
        }

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);
        params.put("pageNo", String.valueOf(pageNo));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("search", "accurate");
        params.put("dataId", "");
        params.put("group", "");
        params.put("appName", "");
        params.put("config_tags", "");
        params.put("pageNo", String.valueOf(pageNo));
        params.put("pageSize", String.valueOf(pageSize));

        HttpRestResult<String> result = httpAgent.httpGet("/v1/cs/configs", headers, params);

        if (result.getCode() == 200) {
            return OBJECT_MAPPER.readValue(result.getData(), ConfigInfoListResponse.class);
        } else {
            throw new Exception("Failed to get configs: " + result.getMessage());
        }
    }

    /**
     * 拉取配置
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @return 配置内容
     * @throws Exception 异常
     */
    public String getConfig(String namespaceId, String group, String dataId) throws Exception {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in");
        }

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("tenant", namespaceId);
        params.put("group", group);
        params.put("dataId", dataId);

        HttpRestResult<String> result = httpAgent.httpGet("/v1/cs/configs", headers, params);

        if (result.getCode() == 200) {
            return result.getData();
        } else {
            throw new Exception("Failed to get config: " + result.getMessage());
        }
    }

    /**
     * 发布配置
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param content     配置内容
     * @param type        配置类型
     * @return 是否发布成功
     * @throws Exception 异常
     */
    public boolean publishConfig(String namespaceId, String group, String dataId, String content, String type) throws Exception {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in");
        }

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        params.put("tenant", namespaceId);
        params.put("group", group);
        params.put("dataId", dataId);
        params.put("content", content);
        params.put("type", type);

        HttpRestResult<String> result = httpAgent.httpPost("/v1/cs/configs", headers, params, "");

        if (result.getCode() == 200) {
            return Boolean.parseBoolean(result.getData());
        } else {
            throw new Exception("Failed to publish config: " + result.getMessage());
        }
    }

    /**
     * 删除配置
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @return 是否删除成功
     * @throws Exception 异常
     */
    public boolean deleteConfig(String namespaceId, String group, String dataId) throws Exception {
        if (!isLoggedIn) {
            throw new IllegalStateException("Not logged in");
        }

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("tenant", namespaceId);
        params.put("group", group);
        params.put("dataId", dataId);

        HttpRestResult<String> result = httpAgent.httpDelete("/v1/cs/configs", headers, params);

        if (result.getCode() == 200) {
            return Boolean.parseBoolean(result.getData());
        } else {
            throw new Exception("Failed to delete config: " + result.getMessage());
        }
    }

    /**
     * 检查是否已登录
     *
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    /**
     * 获取服务器地址
     *
     * @return 服务器地址
     */
    public String getServerAddr() {
        return serverAddr;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }
}