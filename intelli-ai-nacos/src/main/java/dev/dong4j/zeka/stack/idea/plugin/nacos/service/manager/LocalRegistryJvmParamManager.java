package dev.dong4j.zeka.stack.idea.plugin.nacos.service.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地注册中心 JVM 参数管理器
 * 用于配置本地 Nacos 注册中心相关的 JVM 参数
 *
 * @author dong4j
 * @since 1.0.0
 */
@SuppressWarnings("All")
public class LocalRegistryJvmParamManager {

    /**
     * 设置本地注册中心相关的 JVM 参数
     *
     * @param javaParameters Java 参数列表
     * @param environments   环境变量
     * @return JVM 参数字符串
     * @throws Exception 异常
     */
    public static String setupJVMParams(List<String> javaParameters, Map<String, String> environments) throws Exception {
        ArrayList<String> appendParams = new ArrayList();
        appendParams.add("-Dspring.cloud.nacos.discovery.server-addr=127.0.0.1:8848");
        appendParams.add("-Dspring.cloud.nacos.config.server-addr=127.0.0.1:8848");
        appendParams.add("-Ddubbo.registry.address=nacos://127.0.0.1:8848");
        appendParams.add("-Ddubbo.metadata-report.address=nacos://127.0.0.1:8848");
        appendParams.add("-Ddubbo.config-center.address=nacos://127.0.0.1:8848");
        appendParams.add("-Dcom.alibaba.vipserver.jmenv=127.0.0.1:8080");
        appendParams.add("-Daddress.server.domain=127.0.0.1");
        appendParams.add("-Daddress.server.port=8080");
        javaParameters.addAll(appendParams);
        return String.join(" ", appendParams);
    }
}
