package dev.dong4j.zeka.stack.idea.plugin.nacos.client;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.impl.ConfigHttpClientManager;
import com.alibaba.nacos.common.http.HttpClientConfig;
import com.alibaba.nacos.common.http.HttpRestResult;
import com.alibaba.nacos.common.http.client.NacosRestTemplate;
import com.alibaba.nacos.common.http.param.Header;
import com.alibaba.nacos.common.http.param.Query;
import com.alibaba.nacos.common.utils.ConvertUtils;
import com.alibaba.nacos.common.utils.StringUtils;

import java.util.Map;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Nacos 服务器 HTTP 代理
 * 用于处理与 Nacos 服务器的 HTTP 通信
 *
 * @author dong4j
 * @since 1.0.0
 */
@Slf4j
public class ConsumServerHttpAgent {
    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";

    /**
     * -- GETTER --
     * 获取命名空间 ID
     */
    @Setter
    @Getter
    private String namespaceId;
    /**
     * -- GETTER --
     * 获取服务器地址
     */
    @Getter
    private String serverUrl;
    private String server;
    private final NacosRestTemplate nacosRestTemplate;
    private final Properties properties;
    /**
     * -- GETTER --
     * 获取编码格式
     *
     * @return 编码格式
     */
    @Getter
    private final String encode;
    private final String appName;
    private final String accessKey;
    private final String secretKey;
    private final String serverAddress;

    public ConsumServerHttpAgent(Properties properties) throws NacosException {
        this.properties = properties;
        this.serverAddress = properties.getProperty(PropertyKeyConst.SERVER_ADDR);
        this.encode = properties.getProperty(PropertyKeyConst.ENCODE, "UTF-8");
        this.appName = properties.getProperty(PropertyKeyConst.USERNAME);
        this.accessKey = properties.getProperty(PropertyKeyConst.ACCESS_KEY);
        this.secretKey = properties.getProperty(PropertyKeyConst.SECRET_KEY);

        // Initialize HTTP client
        HttpClientConfig httpClientConfig = HttpClientConfig.builder()
            .setConTimeOutMillis(ConvertUtils.toInt(properties.getProperty(PropertyKeyConst.CONFIG_LONG_POLL_TIMEOUT, "3000")))
            .setReadTimeOutMillis(ConvertUtils.toInt(properties.getProperty(PropertyKeyConst.CONFIG_RETRY_TIME, "3000")))
            .setMaxRedirects(ConvertUtils.toInt(properties.getProperty(PropertyKeyConst.MAX_RETRY, "5")))
            .build();
        this.nacosRestTemplate = ConfigHttpClientManager.getInstance().getNacosRestTemplate();

        initServerList();
    }

    private void initServerList() throws NacosException {
        if (StringUtils.isNotBlank(serverAddress)) {
            // Direct server address mode
            String[] serverAddrs = serverAddress.split(",");
            if (serverAddrs.length > 0) {
                server = serverAddrs[0].trim();
                serverUrl = normalizeServerUrl(server);
            }
        }

        if (StringUtils.isBlank(serverUrl)) {
            throw new NacosException(NacosException.CLIENT_INVALID_PARAM, "No server address defined.");
        }
    }

    private String normalizeServerUrl(String serverAddr) {
        if (serverAddr.startsWith(HTTPS) || serverAddr.startsWith(HTTP)) {
            return appendContextIfMissing(serverAddr);
        }
        return appendContextIfMissing(HTTP + serverAddr);
    }

    private String appendContextIfMissing(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        // Nacos OpenAPI 默认挂载在 /nacos
        if (!baseUrl.endsWith("/nacos")) {
            return baseUrl + "/nacos";
        }
        return baseUrl;
    }

    /**
     * HTTP GET 请求
     *
     * @param path    请求路径
     * @param headers 请求头
     * @param params  请求参数
     * @return 响应结果
     * @throws Exception 异常
     */
    public HttpRestResult<String> httpGet(String path, Map<String, String> headers, Map<String, String> params) throws Exception {
        String url = serverUrl + path;
        Header header = Header.newInstance();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header.addParam(entry.getKey(), entry.getValue());
            }
        }
        Query query = Query.newInstance();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                query.addParam(entry.getKey(), entry.getValue());
            }
        }
        return nacosRestTemplate.get(url, header, query, String.class);
    }

    /**
     * HTTP POST 请求
     *
     * @param path    请求路径
     * @param headers 请求头
     * @param params  请求参数
     * @param body    请求体
     * @return 响应结果
     * @throws Exception 异常
     */
    public HttpRestResult<String> httpPost(String path, Map<String, String> headers, Map<String, String> params, String body) throws Exception {
        String url = serverUrl + path;
        Header header = Header.newInstance();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header.addParam(entry.getKey(), entry.getValue());
            }
        }
        Query query = Query.newInstance();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                query.addParam(entry.getKey(), entry.getValue());
            }
        }
        return nacosRestTemplate.post(url, header, query, body, String.class);
    }

    /**
     * HTTP PUT 请求
     *
     * @param path    请求路径
     * @param headers 请求头
     * @param params  请求参数
     * @param body    请求体
     * @return 响应结果
     * @throws Exception 异常
     */
    public HttpRestResult<String> httpPut(String path, Map<String, String> headers, Map<String, String> params, String body) throws Exception {
        String url = serverUrl + path;
        Header header = Header.newInstance();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header.addParam(entry.getKey(), entry.getValue());
            }
        }
        Query query = Query.newInstance();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                query.addParam(entry.getKey(), entry.getValue());
            }
        }
        return nacosRestTemplate.put(url, header, query, body, String.class);
    }

    /**
     * HTTP DELETE 请求
     *
     * @param path    请求路径
     * @param headers 请求头
     * @param params  请求参数
     * @return 响应结果
     * @throws Exception 异常
     */
    public HttpRestResult<String> httpDelete(String path, Map<String, String> headers, Map<String, String> params) throws Exception {
        String url = serverUrl + path;
        Header header = Header.newInstance();
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header.addParam(entry.getKey(), entry.getValue());
            }
        }
        Query query = Query.newInstance();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                query.addParam(entry.getKey(), entry.getValue());
            }
        }
        return nacosRestTemplate.delete(url, header, query, String.class);
    }

    /**
     * 获取原始 serverAddr
     *
     * @return server address
     */
    public String getServerAddress() {
        return server;
    }

}
