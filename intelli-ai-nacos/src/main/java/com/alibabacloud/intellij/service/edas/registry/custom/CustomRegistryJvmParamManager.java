//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibabacloud.intellij.service.edas.registry.custom;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("All")
public class CustomRegistryJvmParamManager {
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
