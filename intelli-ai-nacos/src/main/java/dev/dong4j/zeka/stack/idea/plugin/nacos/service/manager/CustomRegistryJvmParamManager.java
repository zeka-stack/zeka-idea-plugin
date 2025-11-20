package dev.dong4j.zeka.stack.idea.plugin.nacos.service.manager;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义注册中心 JVM 参数管理器
 * 用于配置自定义远程 Nacos 注册中心相关的 JVM 参数
 *
 * @author dong4j
 * @since 1.0.0
 */
@SuppressWarnings("All")
public class CustomRegistryJvmParamManager {

    /**
     * 设置自定义注册中心相关的 JVM 参数
     *
     * @param javaParameters Java 参数列表
     * @param host           注册中心主机地址
     * @return JVM 参数字符串
     * @throws Exception 异常
     */
    public static String setupJvmParams(List<String> javaParameters, String host) throws Exception {
        ArrayList<String> appendParams = new ArrayList();
        String nacosRegistry = host + ":8848";
        appendParams.add("-Dspring.cloud.nacos.discovery.server-addr=" + nacosRegistry);
        appendParams.add("-Ddubbo.registry.address=nacos://" + nacosRegistry);
        appendParams.add("-Ddubbo.metadata-report.address=nacos://" + nacosRegistry);
        appendParams.add("-Ddubbo.config-center.address=nacos://" + nacosRegistry);
        String vipRegistry = host + ":8080";
        appendParams.add("-Dcom.alibaba.vipserver.jmenv=" + vipRegistry);
        appendParams.add("-Daddress.server.domain=" + host);
        appendParams.add("-Daddress.server.port=8080");
        javaParameters.addAll(appendParams);
        return String.join(" ", appendParams);
    }
}
