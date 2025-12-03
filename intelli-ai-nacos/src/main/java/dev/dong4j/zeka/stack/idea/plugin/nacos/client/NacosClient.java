package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigHistoryItem;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigHistoryListResponse;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigInfo;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigInfoListResponse;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigInfoWrapper;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.Namespace;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.NamespaceListResponse;
import lombok.Getter;

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
    /** 获取服务器地址 */
    @Getter
    private final String serverAddr;
    /** 获取用户名 */
    @Getter
    private final String username;
    private final String password;
    /** 检查是否已登录 */
    @Getter
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
        this.securityProxy = new ConsumSecurityProxy(properties, httpAgent);
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
     */
    public boolean login() {
        if (isLoggedIn && securityProxy.ensureLogin()) {
            return true;
        }
        boolean success = securityProxy.login(username, password);
        this.isLoggedIn = success;
        return success;
    }

    private void ensureLoggedIn() {
        if (!login()) {
            throw new IllegalStateException("Failed to login Nacos server");
        }
    }

    /**
     * 获取命名空间列表
     *
     * @return 命名空间列表
     * @throws Exception 异常
     */
    public List<Namespace> getNamespaces() throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();

        HttpRestResult<String> result = httpAgent.httpGet("/v2/console/namespace/list", headers, null);

        String body = ensureHttpSuccess(result, "get namespaces");
        JsonNode dataNode = extractDataNode(body, "get namespaces");
        if (dataNode == null || dataNode.isNull()) {
            NamespaceListResponse response = OBJECT_MAPPER.readValue(body, NamespaceListResponse.class);
            return response.getNamespaces() != null ? response.getNamespaces() : Collections.emptyList();
        }
        if (dataNode.isArray()) {
            return OBJECT_MAPPER.readerFor(new TypeReference<List<Namespace>>() {}).readValue(dataNode);
        }
        NamespaceListResponse response = OBJECT_MAPPER.treeToValue(dataNode, NamespaceListResponse.class);
        return response.getNamespaces() != null ? response.getNamespaces() : Collections.emptyList();
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
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("tenant", namespaceId);
        params.put("pageNo", String.valueOf(pageNo));
        params.put("pageSize", String.valueOf(pageSize));
        params.put("search", "accurate");
        params.put("dataId", "");
        params.put("group", "");
        params.put("appName", "");
        params.put("config_tags", "");

        HttpRestResult<String> result = httpAgent.httpGet("/v1/cs/configs", headers, params);

        String body = ensureHttpSuccess(result, "get configs");
        JsonNode dataNode = extractDataNode(body, "get configs");
        if (dataNode == null || dataNode.isNull()) {
            return OBJECT_MAPPER.readValue(body, ConfigInfoListResponse.class);
        }
        if (dataNode.isObject()) {
            return OBJECT_MAPPER.treeToValue(dataNode, ConfigInfoListResponse.class);
        }
        return OBJECT_MAPPER.readValue(body, ConfigInfoListResponse.class);
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
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);
        params.put("group", group);
        params.put("dataId", dataId);

        HttpRestResult<String> result = httpAgent.httpGet("/v2/cs/config", headers, params);

        String body = ensureHttpSuccess(result, "get config");
        JsonNode dataNode = extractDataNode(body, "get config");
        if (dataNode == null || dataNode.isNull()) {
            return body;
        }
        if (dataNode.isTextual() || dataNode.isNumber() || dataNode.isBoolean()) {
            return dataNode.asText();
        }
        return dataNode.toString();
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
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);
        params.put("group", group);
        params.put("dataId", dataId);
        params.put("content", content);
        params.put("type", type);

        HttpRestResult<String> result = httpAgent.httpPost("/v2/cs/config", headers, params, "");

        String body = ensureHttpSuccess(result, "publish config");
        JsonNode dataNode = extractDataNode(body, "publish config");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
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
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);
        params.put("group", group);
        params.put("dataId", dataId);

        HttpRestResult<String> result = httpAgent.httpDelete("/v2/cs/config", headers, params);

        String body = ensureHttpSuccess(result, "delete config");
        JsonNode dataNode = extractDataNode(body, "delete config");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 获取配置历史版本列表
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param pageNo      页码（可选，默认 1）
     * @param pageSize    页大小（可选，默认 100，最大 500）
     * @return 配置历史版本列表
     * @throws Exception 异常
     */
    public List<ConfigHistoryItem> getConfigHistoryList(String namespaceId, String group, String dataId,
                                                        Integer pageNo, Integer pageSize) throws Exception {
        ensureLoggedIn();

        NacosEndpoint endpoint = new NacosEndpoint(serverAddr, username, password);
        JsonNode dataNode = endpoint.getConfigHistoryList(namespaceId, group, dataId, pageNo, pageSize);

        if (dataNode.isNull()) {
            return Collections.emptyList();
        }

        ConfigHistoryListResponse response = OBJECT_MAPPER.treeToValue(dataNode, ConfigHistoryListResponse.class);
        return response.getPageItems() != null ? response.getPageItems() : Collections.emptyList();
    }

    /**
     * 获取指定版本的历史配置
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param nid         历史配置 ID
     * @return 历史配置信息
     * @throws Exception 异常
     */
    public ConfigHistoryItem getConfigHistory(String namespaceId, String group, String dataId, long nid) throws Exception {
        ensureLoggedIn();

        NacosEndpoint endpoint = new NacosEndpoint(serverAddr, username, password);
        JsonNode dataNode = endpoint.getConfigHistory(namespaceId, group, dataId, nid);

        if (dataNode.isNull()) {
            throw new Exception("Config history not found");
        }

        return OBJECT_MAPPER.treeToValue(dataNode, ConfigHistoryItem.class);
    }

    /**
     * 拉取命名空间下所有配置（自动分页）
     */
    public List<ConfigInfoWrapper> listAllConfigs(String namespaceId) throws Exception {
        ensureLoggedIn();
        int pageNo = 1;
        int pageSize = 200;
        List<ConfigInfoWrapper> result = new java.util.ArrayList<>();
        while (true) {
            ConfigInfoListResponse response = getConfigs(namespaceId, pageNo, pageSize);
            if (response.getConfigInfos() != null) {
                result.addAll(response.getConfigInfos());
            }
            if (response.getConfigInfos() == null || response.getConfigInfos().isEmpty() ||
                result.size() >= response.getTotalCount()) {
                break;
            }
            pageNo++;
        }
        return result;
    }

    /**
     * 获取指定命名空间下的所有分组
     */
    public List<String> listGroups(String namespaceId) throws Exception {
        return listAllConfigs(namespaceId).stream()
            .map(ConfigInfo::getGroup)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .toList();
    }

    /**
     * 获取指定命名空间+分组的 dataId 列表
     */
    public List<String> listDataIds(String namespaceId, String group) throws Exception {
        return listAllConfigs(namespaceId).stream()
            .filter(config -> group == null || group.equals(config.getGroup()))
            .map(ConfigInfo::getDataId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .sorted(String::compareToIgnoreCase)
            .toList();
    }

    private String ensureHttpSuccess(HttpRestResult<String> result, String action) throws Exception {
        if (result.getCode() != 200) {
            throw new Exception("Failed to " + action + ": HTTP " + result.getCode() + " " + result.getMessage());
        }
        return result.getData();
    }

    private JsonNode extractDataNode(String body, String action) throws Exception {
        if (body == null || body.isEmpty()) {
            return NullNode.instance;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(body);
            if (root.has("code")) {
                int apiCode = root.path("code").asInt(0);
                if (apiCode != 0) {
                    String message = root.path("message").asText("unknown error");
                    throw new Exception("Failed to " + action + ": " + message);
                }
                return root.path("data");
            }
            return root;
        } catch (JsonProcessingException e) {
            return TextNode.valueOf(body);
        }
    }
}