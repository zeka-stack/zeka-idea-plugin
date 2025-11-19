//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibabacloud.intellij.service.edas.registry.local;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("All")
public class LocalRegistryJvmParamManager {
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
