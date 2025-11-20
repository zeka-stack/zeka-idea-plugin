package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.alibaba.nacos.common.utils.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.ConfigInfoListResponse;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.Namespace;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.model.NamespaceListResponse;

/**
 * Nacos OpenAPI 端点封装类
 * <p>
 * 根据 Nacos OpenAPI 文档封装所有接口调用
 * 不改变现有逻辑，仅作为新的 API 封装层
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosEndpoint {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConsumServerHttpAgent httpAgent;
    private final ConsumSecurityProxy securityProxy;
    private final String serverAddr;
    private final String username;
    private final String password;

    /**
     * 构造函数
     *
     * @param serverAddr 服务器地址
     * @param username   用户名
     * @param password   密码
     * @throws NacosException Nacos 异常
     */
    public NacosEndpoint(@NotNull String serverAddr, @Nullable String username, @Nullable String password) throws NacosException {
        this.serverAddr = serverAddr;
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";

        // 初始化 Properties
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);
        properties.setProperty(PropertyKeyConst.USERNAME, this.username);
        properties.setProperty(PropertyKeyConst.PASSWORD, this.password);

        // 创建 HTTP 代理和安全代理
        this.httpAgent = new ConsumServerHttpAgent(properties);
        this.securityProxy = new ConsumSecurityProxy(properties, httpAgent);
    }

    /**
     * 确保已登录
     *
     * @throws Exception 异常
     */
    private void ensureLoggedIn() throws Exception {
        if (!securityProxy.ensureLogin()) {
            boolean success = securityProxy.login(username, password);
            if (!success) {
                throw new IllegalStateException("Failed to login Nacos server");
            }
        }
    }

    /**
     * 提取响应数据节点
     *
     * @param body   响应体
     * @param action 操作名称
     * @return 数据节点
     * @throws Exception 异常
     */
    private JsonNode extractDataNode(String body, String action) throws Exception {
        if (body == null || body.isEmpty()) {
            return null;
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
        } catch (Exception e) {
            throw new Exception("Failed to parse response for " + action + ": " + e.getMessage(), e);
        }
    }

    /**
     * 确保 HTTP 请求成功
     *
     * @param result HTTP 响应结果
     * @param action 操作名称
     * @return 响应体
     * @throws Exception 异常
     */
    private String ensureHttpSuccess(HttpRestResult<String> result, String action) throws Exception {
        if (result.getCode() != 200) {
            throw new Exception("Failed to " + action + ": HTTP " + result.getCode() + " " + result.getMessage());
        }
        return result.getData();
    }

    // ==================== 认证相关 ====================

    /**
     * 登录到 Nacos 服务器
     *
     * @return 是否登录成功
     * @throws Exception 异常
     */
    public boolean login() throws Exception {
        return securityProxy.login(username, password);
    }

    /**
     * 检查是否已登录
     *
     * @return 是否已登录
     */
    public boolean isLoggedIn() {
        try {
            return securityProxy.ensureLogin();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否是全局管理员
     *
     * @return 是否是全局管理员
     */
    public boolean isGlobalAdmin() {
        return securityProxy.isGlobalAdmin();
    }

    // ==================== 配置管理相关 ====================

    /**
     * 获取配置
     * <p>
     * GET /nacos/v2/cs/config
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param tag         标签（可选）
     * @return 配置内容
     * @throws Exception 异常
     */
    @NotNull
    public String getConfig(@Nullable String namespaceId, @NotNull String group, @NotNull String dataId, @Nullable String tag) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        params.put("group", group);
        params.put("dataId", dataId);
        if (StringUtils.isNotBlank(tag)) {
            params.put("tag", tag);
        }

        HttpRestResult<String> result = httpAgent.httpGet("/v2/cs/config", headers, params);
        String body = ensureHttpSuccess(result, "get config");
        JsonNode dataNode = extractDataNode(body, "get config");
        if (dataNode == null || dataNode.isNull()) {
            return body;
        }
        if (dataNode.isTextual()) {
            return dataNode.asText();
        }
        return dataNode.toString();
    }

    /**
     * 发布配置
     * <p>
     * POST /nacos/v2/cs/config
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param content     配置内容
     * @param type        配置类型（可选）
     * @param tag         标签（可选）
     * @param appName     应用名（可选）
     * @param srcUser     源用户（可选）
     * @param configTags  配置标签列表（可选）
     * @param desc        配置描述（可选）
     * @return 是否发布成功
     * @throws Exception 异常
     */
    public boolean publishConfig(@Nullable String namespaceId, @NotNull String group, @NotNull String dataId,
                                 @NotNull String content, @Nullable String type, @Nullable String tag,
                                 @Nullable String appName, @Nullable String srcUser, @Nullable String configTags,
                                 @Nullable String desc) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        params.put("group", group);
        params.put("dataId", dataId);
        params.put("content", content);
        if (StringUtils.isNotBlank(type)) {
            params.put("type", type);
        }
        if (StringUtils.isNotBlank(tag)) {
            params.put("tag", tag);
        }
        if (StringUtils.isNotBlank(appName)) {
            params.put("appName", appName);
        }
        if (StringUtils.isNotBlank(srcUser)) {
            params.put("srcUser", srcUser);
        }
        if (StringUtils.isNotBlank(configTags)) {
            params.put("configTags", configTags);
        }
        if (StringUtils.isNotBlank(desc)) {
            params.put("desc", desc);
        }

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
     * <p>
     * DELETE /nacos/v2/cs/config
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param tag         标签（可选）
     * @return 是否删除成功
     * @throws Exception 异常
     */
    public boolean deleteConfig(@Nullable String namespaceId, @NotNull String group, @NotNull String dataId, @Nullable String tag) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        params.put("group", group);
        params.put("dataId", dataId);
        if (StringUtils.isNotBlank(tag)) {
            params.put("tag", tag);
        }

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
     * 查询配置历史列表
     * <p>
     * GET /nacos/v2/cs/history/list
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param pageNo      页码（可选，默认 1）
     * @param pageSize    页大小（可选，默认 100，最大 500）
     * @return 配置历史列表响应
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getConfigHistoryList(@Nullable String namespaceId, @NotNull String group, @NotNull String dataId,
                                         @Nullable Integer pageNo, @Nullable Integer pageSize) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        params.put("group", group);
        params.put("dataId", dataId);
        if (pageNo != null) {
            params.put("pageNo", String.valueOf(pageNo));
        }
        if (pageSize != null) {
            params.put("pageSize", String.valueOf(pageSize));
        }

        HttpRestResult<String> result = httpAgent.httpGet("/v2/cs/history/list", headers, params);
        String body = ensureHttpSuccess(result, "get config history list");
        return extractDataNode(body, "get config history list");
    }

    /**
     * 查询具体版本的历史配置
     * <p>
     * GET /nacos/v2/cs/history
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param nid         历史配置 ID
     * @return 历史配置信息
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getConfigHistory(@Nullable String namespaceId, @NotNull String group, @NotNull String dataId, long nid) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        params.put("group", group);
        params.put("dataId", dataId);
        params.put("nid", String.valueOf(nid));

        HttpRestResult<String> result = httpAgent.httpGet("/v2/cs/history", headers, params);
        String body = ensureHttpSuccess(result, "get config history");
        return extractDataNode(body, "get config history");
    }

    /**
     * 查询配置上一版本信息
     * <p>
     * GET /nacos/v2/cs/history/previous
     *
     * @param namespaceId 命名空间 ID
     * @param group       分组
     * @param dataId      数据 ID
     * @param id          配置 ID
     * @return 上一版本配置信息
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getConfigPreviousVersion(@Nullable String namespaceId, @NotNull String group, @NotNull String dataId, long id) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        params.put("group", group);
        params.put("dataId", dataId);
        params.put("id", String.valueOf(id));

        HttpRestResult<String> result = httpAgent.httpGet("/v2/cs/history/previous", headers, params);
        String body = ensureHttpSuccess(result, "get config previous version");
        return extractDataNode(body, "get config previous version");
    }

    /**
     * 获取配置列表（分页）
     * <p>
     * GET /nacos/v1/cs/configs
     *
     * @param namespaceId 命名空间 ID
     * @param pageNo      页码
     * @param pageSize    页大小
     * @return 配置列表响应
     * @throws Exception 异常
     */
    @NotNull
    public ConfigInfoListResponse getConfigs(@Nullable String namespaceId, int pageNo, int pageSize) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("tenant", namespaceId != null ? namespaceId : "");
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

    // ==================== 命名空间相关 ====================

    /**
     * 查询命名空间列表
     * <p>
     * GET /nacos/v2/console/namespace/list
     *
     * @return 命名空间列表
     * @throws Exception 异常
     */
    @NotNull
    public List<Namespace> getNamespaces() throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", "");

        HttpRestResult<String> result = httpAgent.httpGet("/v2/console/namespace/list", headers, params);
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
     * 查询具体命名空间
     * <p>
     * GET /nacos/v2/console/namespace
     *
     * @param namespaceId 命名空间 ID
     * @return 命名空间信息
     * @throws Exception 异常
     */
    @NotNull
    public Namespace getNamespace(@NotNull String namespaceId) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);

        HttpRestResult<String> result = httpAgent.httpGet("/v2/console/namespace", headers, params);
        String body = ensureHttpSuccess(result, "get namespace");
        JsonNode dataNode = extractDataNode(body, "get namespace");
        if (dataNode == null || dataNode.isNull()) {
            return OBJECT_MAPPER.readValue(body, Namespace.class);
        }
        return OBJECT_MAPPER.treeToValue(dataNode, Namespace.class);
    }

    /**
     * 创建命名空间
     * <p>
     * POST /nacos/v2/console/namespace
     *
     * @param namespaceId   命名空间 ID
     * @param namespaceName 命名空间名称
     * @param namespaceDesc 命名空间描述（可选）
     * @return 是否创建成功
     * @throws Exception 异常
     */
    public boolean createNamespace(@NotNull String namespaceId, @NotNull String namespaceName, @Nullable String namespaceDesc) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);
        params.put("namespaceName", namespaceName);
        if (StringUtils.isNotBlank(namespaceDesc)) {
            params.put("namespaceDesc", namespaceDesc);
        }

        HttpRestResult<String> result = httpAgent.httpPost("/v2/console/namespace", headers, params, "");
        String body = ensureHttpSuccess(result, "create namespace");
        JsonNode dataNode = extractDataNode(body, "create namespace");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 编辑命名空间
     * <p>
     * PUT /nacos/v2/console/namespace
     *
     * @param namespaceId   命名空间 ID
     * @param namespaceName 命名空间名称
     * @param namespaceDesc 命名空间描述（可选）
     * @return 是否编辑成功
     * @throws Exception 异常
     */
    public boolean updateNamespace(@NotNull String namespaceId, @NotNull String namespaceName, @Nullable String namespaceDesc) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);
        params.put("namespaceName", namespaceName);
        if (StringUtils.isNotBlank(namespaceDesc)) {
            params.put("namespaceDesc", namespaceDesc);
        }

        HttpRestResult<String> result = httpAgent.httpPut("/v2/console/namespace", headers, params, "");
        String body = ensureHttpSuccess(result, "update namespace");
        JsonNode dataNode = extractDataNode(body, "update namespace");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 删除命名空间
     * <p>
     * DELETE /nacos/v2/console/namespace
     *
     * @param namespaceId 命名空间 ID
     * @return 是否删除成功
     * @throws Exception 异常
     */
    public boolean deleteNamespace(@NotNull String namespaceId) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("namespaceId", namespaceId);

        HttpRestResult<String> result = httpAgent.httpDelete("/v2/console/namespace", headers, params);
        String body = ensureHttpSuccess(result, "delete namespace");
        JsonNode dataNode = extractDataNode(body, "delete namespace");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    // ==================== 服务发现相关 ====================
    // 注意：服务发现相关的接口较多，这里先实现主要的几个，后续可以根据需要扩展

    /**
     * 注册实例
     * <p>
     * POST /nacos/v2/ns/instance
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选，默认 DEFAULT_GROUP）
     * @param serviceName 服务名
     * @param ip          IP 地址
     * @param port        端口号
     * @param clusterName 集群名称（可选，默认 DEFAULT）
     * @param healthy     是否健康（可选，默认 true）
     * @param weight      实例权重（可选，默认 1.0）
     * @param enabled     是否可用（可选，默认 true）
     * @param metadata    实例元数据（可选，JSON 格式字符串）
     * @param ephemeral   是否为临时实例（可选）
     * @return 是否注册成功
     * @throws Exception 异常
     */
    public boolean registerInstance(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                    @NotNull String ip, int port, @Nullable String clusterName, @Nullable Boolean healthy,
                                    @Nullable Double weight, @Nullable Boolean enabled, @Nullable String metadata,
                                    @Nullable Boolean ephemeral) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        params.put("ip", ip);
        params.put("port", String.valueOf(port));
        if (StringUtils.isNotBlank(clusterName)) {
            params.put("clusterName", clusterName);
        }
        if (healthy != null) {
            params.put("healthy", String.valueOf(healthy));
        }
        if (weight != null) {
            params.put("weight", String.valueOf(weight));
        }
        if (enabled != null) {
            params.put("enabled", String.valueOf(enabled));
        }
        if (StringUtils.isNotBlank(metadata)) {
            params.put("metadata", metadata);
        }
        if (ephemeral != null) {
            params.put("ephemeral", String.valueOf(ephemeral));
        }

        HttpRestResult<String> result = httpAgent.httpPost("/v2/ns/instance", headers, params, "");
        String body = ensureHttpSuccess(result, "register instance");
        JsonNode dataNode = extractDataNode(body, "register instance");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 注销实例
     * <p>
     * DELETE /nacos/v2/ns/instance
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @param ip          IP 地址
     * @param port        端口号
     * @param clusterName 集群名称（可选）
     * @return 是否注销成功
     * @throws Exception 异常
     */
    public boolean deregisterInstance(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                      @NotNull String ip, int port, @Nullable String clusterName) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        params.put("ip", ip);
        params.put("port", String.valueOf(port));
        if (StringUtils.isNotBlank(clusterName)) {
            params.put("clusterName", clusterName);
        }

        HttpRestResult<String> result = httpAgent.httpDelete("/v2/ns/instance", headers, params);
        String body = ensureHttpSuccess(result, "deregister instance");
        JsonNode dataNode = extractDataNode(body, "deregister instance");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 查询指定服务的实例列表
     * <p>
     * GET /nacos/v2/ns/instance/list
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @param clusterName 集群名称（可选）
     * @param ip          IP 地址（可选）
     * @param port        端口号（可选）
     * @param healthyOnly 是否只获取健康实例（可选）
     * @param app         应用名（可选）
     * @return 实例列表响应（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getInstanceList(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                    @Nullable String clusterName, @Nullable String ip, @Nullable Integer port,
                                    @Nullable Boolean healthyOnly, @Nullable String app) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (StringUtils.isNotBlank(clusterName)) {
            params.put("clusterName", clusterName);
        }
        if (StringUtils.isNotBlank(ip)) {
            params.put("ip", ip);
        }
        if (port != null) {
            params.put("port", String.valueOf(port));
        }
        if (healthyOnly != null) {
            params.put("healthyOnly", String.valueOf(healthyOnly));
        }
        if (StringUtils.isNotBlank(app)) {
            params.put("app", app);
        }

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/instance/list", headers, params);
        String body = ensureHttpSuccess(result, "get instance list");
        return extractDataNode(body, "get instance list");
    }

    /**
     * 更新实例
     * <p>
     * PUT /nacos/v2/ns/instance
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @param ip          IP 地址
     * @param port        端口号
     * @param clusterName 集群名称（可选）
     * @param healthy     是否健康（可选）
     * @param weight      实例权重（可选）
     * @param enabled     是否可用（可选）
     * @param metadata    实例元数据（可选，JSON 格式字符串）
     * @param ephemeral   是否为临时实例（可选）
     * @return 是否更新成功
     * @throws Exception 异常
     */
    public boolean updateInstance(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                  @NotNull String ip, int port, @Nullable String clusterName, @Nullable Boolean healthy,
                                  @Nullable Double weight, @Nullable Boolean enabled, @Nullable String metadata,
                                  @Nullable Boolean ephemeral) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        params.put("ip", ip);
        params.put("port", String.valueOf(port));
        if (StringUtils.isNotBlank(clusterName)) {
            params.put("clusterName", clusterName);
        }
        if (healthy != null) {
            params.put("healthy", String.valueOf(healthy));
        }
        if (weight != null) {
            params.put("weight", String.valueOf(weight));
        }
        if (enabled != null) {
            params.put("enabled", String.valueOf(enabled));
        }
        if (StringUtils.isNotBlank(metadata)) {
            params.put("metadata", metadata);
        }
        if (ephemeral != null) {
            params.put("ephemeral", String.valueOf(ephemeral));
        }

        HttpRestResult<String> result = httpAgent.httpPut("/v2/ns/instance", headers, params, "");
        String body = ensureHttpSuccess(result, "update instance");
        JsonNode dataNode = extractDataNode(body, "update instance");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 查询实例详情
     * <p>
     * GET /nacos/v2/ns/instance
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @param clusterName 集群名称（可选）
     * @param ip          IP 地址
     * @param port        端口号
     * @return 实例详情（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getInstance(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                @Nullable String clusterName, @NotNull String ip, int port) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (StringUtils.isNotBlank(clusterName)) {
            params.put("clusterName", clusterName);
        }
        params.put("ip", ip);
        params.put("port", String.valueOf(port));

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/instance", headers, params);
        String body = ensureHttpSuccess(result, "get instance");
        return extractDataNode(body, "get instance");
    }


    /**
     * 批量更新实例元数据
     * <p>
     * PUT /nacos/v2/ns/instance/metadata/batch
     *
     * @param namespaceId     命名空间 ID（可选）
     * @param groupName       分组名（可选）
     * @param serviceName     服务名
     * @param consistencyType 持久化类型（可选，persist 或 ephemeral）
     * @param instances       需要更新的实例列表（可选，JSON 格式字符串）
     * @param metadata        实例元数据（JSON 格式字符串）
     * @return 是否更新成功
     * @throws Exception 异常
     */
    public boolean batchUpdateInstanceMetadata(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                               @Nullable String consistencyType, @Nullable String instances,
                                               @NotNull String metadata) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (StringUtils.isNotBlank(consistencyType)) {
            params.put("consistencyType", consistencyType);
        }
        if (StringUtils.isNotBlank(instances)) {
            params.put("instances", instances);
        }
        params.put("metadata", metadata);

        HttpRestResult<String> result = httpAgent.httpPut("/v2/ns/instance/metadata/batch", headers, params, "");
        String body = ensureHttpSuccess(result, "batch update instance metadata");
        JsonNode dataNode = extractDataNode(body, "batch update instance metadata");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 批量删除实例元数据
     * <p>
     * DELETE /nacos/v2/ns/instance/metadata/batch
     *
     * @param namespaceId     命名空间 ID（可选）
     * @param groupName       分组名（可选）
     * @param serviceName     服务名
     * @param consistencyType 持久化类型（可选，persist 或 ephemeral）
     * @param instances       需要更新的实例列表（可选，JSON 格式字符串）
     * @param metadata        实例元数据（JSON 格式字符串）
     * @return 是否删除成功
     * @throws Exception 异常
     */
    public boolean batchDeleteInstanceMetadata(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                               @Nullable String consistencyType, @Nullable String instances,
                                               @NotNull String metadata) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (StringUtils.isNotBlank(consistencyType)) {
            params.put("consistencyType", consistencyType);
        }
        if (StringUtils.isNotBlank(instances)) {
            params.put("instances", instances);
        }
        params.put("metadata", metadata);

        HttpRestResult<String> result = httpAgent.httpDelete("/v2/ns/instance/metadata/batch", headers, params);
        String body = ensureHttpSuccess(result, "batch delete instance metadata");
        JsonNode dataNode = extractDataNode(body, "batch delete instance metadata");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 创建服务
     * <p>
     * POST /nacos/v2/ns/service
     *
     * @param namespaceId      命名空间 ID（可选）
     * @param groupName        分组名（可选）
     * @param serviceName      服务名
     * @param metadata         服务元数据（可选，JSON 格式字符串）
     * @param ephemeral        是否为临时实例（可选）
     * @param protectThreshold 保护阈值（可选）
     * @param selector         访问策略（可选，JSON 格式字符串）
     * @return 是否创建成功
     * @throws Exception 异常
     */
    public boolean createService(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                 @Nullable String metadata, @Nullable Boolean ephemeral, @Nullable Float protectThreshold,
                                 @Nullable String selector) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (StringUtils.isNotBlank(metadata)) {
            params.put("metadata", metadata);
        }
        if (ephemeral != null) {
            params.put("ephemeral", String.valueOf(ephemeral));
        }
        if (protectThreshold != null) {
            params.put("protectThreshold", String.valueOf(protectThreshold));
        }
        if (StringUtils.isNotBlank(selector)) {
            params.put("selector", selector);
        }

        HttpRestResult<String> result = httpAgent.httpPost("/v2/ns/service", headers, params, "");
        String body = ensureHttpSuccess(result, "create service");
        JsonNode dataNode = extractDataNode(body, "create service");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 删除服务
     * <p>
     * DELETE /nacos/v2/ns/service
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @return 是否删除成功
     * @throws Exception 异常
     */
    public boolean deleteService(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);

        HttpRestResult<String> result = httpAgent.httpDelete("/v2/ns/service", headers, params);
        String body = ensureHttpSuccess(result, "delete service");
        JsonNode dataNode = extractDataNode(body, "delete service");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 修改服务
     * <p>
     * PUT /nacos/v2/ns/service
     *
     * @param namespaceId      命名空间 ID（可选）
     * @param groupName        分组名（可选）
     * @param serviceName      服务名
     * @param metadata         服务元数据（可选，JSON 格式字符串）
     * @param protectThreshold 保护阈值（可选）
     * @param selector         访问策略（可选，JSON 格式字符串）
     * @return 是否修改成功
     * @throws Exception 异常
     */
    public boolean updateService(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                 @Nullable String metadata, @Nullable Float protectThreshold, @Nullable String selector) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (StringUtils.isNotBlank(metadata)) {
            params.put("metadata", metadata);
        }
        if (protectThreshold != null) {
            params.put("protectThreshold", String.valueOf(protectThreshold));
        }
        if (StringUtils.isNotBlank(selector)) {
            params.put("selector", selector);
        }

        HttpRestResult<String> result = httpAgent.httpPut("/v2/ns/service", headers, params, "");
        String body = ensureHttpSuccess(result, "update service");
        JsonNode dataNode = extractDataNode(body, "update service");
        if (dataNode == null || dataNode.isNull()) {
            return Boolean.parseBoolean(body);
        }
        if (dataNode.isBoolean()) {
            return dataNode.asBoolean();
        }
        return Boolean.parseBoolean(dataNode.asText());
    }

    /**
     * 查询服务详情
     * <p>
     * GET /nacos/v2/ns/service
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @return 服务详情（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getService(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/service", headers, params);
        String body = ensureHttpSuccess(result, "get service");
        return extractDataNode(body, "get service");
    }

    /**
     * 查询服务列表
     * <p>
     * GET /nacos/v2/ns/service/list
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param selector    访问策略（JSON 格式字符串，必填）
     * @param pageNo      页码（可选，默认 1）
     * @param pageSize    页大小（可选，默认 20，最大 500）
     * @return 服务列表响应（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getServiceList(@Nullable String namespaceId, @Nullable String groupName, @NotNull String selector,
                                   @Nullable Integer pageNo, @Nullable Integer pageSize) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("selector", selector);
        if (pageNo != null) {
            params.put("pageNo", String.valueOf(pageNo));
        }
        if (pageSize != null) {
            params.put("pageSize", String.valueOf(pageSize));
        }

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/service/list", headers, params);
        String body = ensureHttpSuccess(result, "get service list");
        return extractDataNode(body, "get service list");
    }

    /**
     * 更新实例健康状态
     * <p>
     * PUT /nacos/v2/ns/health/instance
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @param clusterName 集群名（可选）
     * @param ip          IP 地址
     * @param port        端口号
     * @param healthy     是否健康
     * @return 是否更新成功（返回 "ok" 表示成功）
     * @throws Exception 异常
     */
    @NotNull
    public String updateInstanceHealth(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                       @Nullable String clusterName, @NotNull String ip, int port, boolean healthy) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (StringUtils.isNotBlank(clusterName)) {
            params.put("clusterName", clusterName);
        }
        params.put("ip", ip);
        params.put("port", String.valueOf(port));
        params.put("healthy", String.valueOf(healthy));

        HttpRestResult<String> result = httpAgent.httpPut("/v2/ns/health/instance", headers, params, "");
        String body = ensureHttpSuccess(result, "update instance health");
        JsonNode dataNode = extractDataNode(body, "update instance health");
        if (dataNode == null || dataNode.isNull()) {
            return body;
        }
        return dataNode.asText();
    }

    /**
     * 查询客户端列表
     * <p>
     * GET /nacos/v2/ns/client/list
     *
     * @return 客户端 ID 列表（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getClientList() throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/client/list", headers, null);
        String body = ensureHttpSuccess(result, "get client list");
        return extractDataNode(body, "get client list");
    }

    /**
     * 查询客户端信息
     * <p>
     * GET /nacos/v2/ns/client
     *
     * @param clientId 客户端 ID
     * @return 客户端信息（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getClient(@NotNull String clientId) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("clientId", clientId);

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/client", headers, params);
        String body = ensureHttpSuccess(result, "get client");
        return extractDataNode(body, "get client");
    }

    /**
     * 查询客户端的注册信息
     * <p>
     * GET /nacos/v2/ns/client/publish/list
     *
     * @param clientId 客户端 ID
     * @return 客户端注册的服务列表（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getClientPublishList(@NotNull String clientId) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("clientId", clientId);

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/client/publish/list", headers, params);
        String body = ensureHttpSuccess(result, "get client publish list");
        return extractDataNode(body, "get client publish list");
    }

    /**
     * 查询客户端的订阅信息
     * <p>
     * GET /nacos/v2/ns/client/subscribe/list
     *
     * @param clientId 客户端 ID
     * @return 客户端订阅的服务列表（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getClientSubscribeList(@NotNull String clientId) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        params.put("clientId", clientId);

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/client/subscribe/list", headers, params);
        String body = ensureHttpSuccess(result, "get client subscribe list");
        return extractDataNode(body, "get client subscribe list");
    }

    /**
     * 查询注册指定服务的客户端信息
     * <p>
     * GET /nacos/v2/ns/client/service/publisher/list
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @param ephemeral   是否为临时实例（可选）
     * @param ip          IP 地址（可选）
     * @param port        端口号（可选）
     * @return 客户端列表（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getServicePublisherList(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                            @Nullable Boolean ephemeral, @Nullable String ip, @Nullable Integer port) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (ephemeral != null) {
            params.put("ephemeral", String.valueOf(ephemeral));
        }
        if (StringUtils.isNotBlank(ip)) {
            params.put("ip", ip);
        }
        if (port != null) {
            params.put("port", String.valueOf(port));
        }

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/client/service/publisher/list", headers, params);
        String body = ensureHttpSuccess(result, "get service publisher list");
        return extractDataNode(body, "get service publisher list");
    }

    /**
     * 查询订阅指定服务的客户端信息
     * <p>
     * GET /nacos/v2/ns/client/service/subscriber/list
     *
     * @param namespaceId 命名空间 ID（可选）
     * @param groupName   分组名（可选）
     * @param serviceName 服务名
     * @param ephemeral   是否为临时实例（可选）
     * @param ip          IP 地址（可选）
     * @param port        端口号（可选）
     * @return 客户端列表（JSON 节点）
     * @throws Exception 异常
     */
    @NotNull
    public JsonNode getServiceSubscriberList(@Nullable String namespaceId, @Nullable String groupName, @NotNull String serviceName,
                                             @Nullable Boolean ephemeral, @Nullable String ip, @Nullable Integer port) throws Exception {
        ensureLoggedIn();

        Map<String, String> headers = securityProxy.getAuthorizationHeader();
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(namespaceId)) {
            params.put("namespaceId", namespaceId);
        }
        if (StringUtils.isNotBlank(groupName)) {
            params.put("groupName", groupName);
        }
        params.put("serviceName", serviceName);
        if (ephemeral != null) {
            params.put("ephemeral", String.valueOf(ephemeral));
        }
        if (StringUtils.isNotBlank(ip)) {
            params.put("ip", ip);
        }
        if (port != null) {
            params.put("port", String.valueOf(port));
        }

        HttpRestResult<String> result = httpAgent.httpGet("/v2/ns/client/service/subscriber/list", headers, params);
        String body = ensureHttpSuccess(result, "get service subscriber list");
        return extractDataNode(body, "get service subscriber list");
    }

    // ==================== 工具方法 ====================

    /**
     * 获取服务器地址
     *
     * @return 服务器地址
     */
    @NotNull
    public String getServerAddr() {
        return serverAddr;
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    @NotNull
    public String getUsername() {
        return username;
    }
}

