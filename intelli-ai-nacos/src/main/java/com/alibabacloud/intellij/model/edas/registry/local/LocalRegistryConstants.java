//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibabacloud.intellij.model.edas.registry.local;

import java.io.File;

public class LocalRegistryConstants {
    public static final String EDAS_HOME;
    public static final String REGISTRY_DIR;
    public static final String LOCAL_REGISTRY_DIR;
    public static final String LOCAL_REGISTRY_PKG_DIR;
    public static final String LOCAL_REGISTRY_LOG_DIR;
    public static final String LOCAL_REGISTRY_TMP_LOG;
    public static final String NACOS_DIR;
    public static final String NACOS_BIN_DIR;
    public static final String NACOS_LOG_DIR;
    public static final String NACOS_REMOTE_PATH = "https://github.com/alibaba/nacos/releases/download/2.4.3/nacos-server-2.4.3.zip";
    public static final String NACOS_LOCAL_PATH_FOR_MAC;
    public static final String NACOS_LOCAL_PATH_FOR_WIN;
    public static final String NACOS_START_UP_FILE_WIN;
    public static final String NACOS_START_UP_FILE_MAC;
    public static final String NACOS_START_LOG;
    public static final String NACOS_TEST_URL = "http://127.0.0.1:8848/nacos/index.html";
    public static final int[] NACOS_PORTS;

    static {
        String var10000 = System.getProperty("user.home");
        EDAS_HOME = var10000 + File.separator + ".zeka-stack";
        REGISTRY_DIR = EDAS_HOME + File.separator + "registry";
        LOCAL_REGISTRY_DIR = REGISTRY_DIR + File.separator + "local";
        LOCAL_REGISTRY_PKG_DIR = LOCAL_REGISTRY_DIR + File.separator + "pkg";
        LOCAL_REGISTRY_LOG_DIR = LOCAL_REGISTRY_DIR + File.separator + "logs";
        LOCAL_REGISTRY_TMP_LOG = LOCAL_REGISTRY_DIR + File.separator + "temp_log";
        NACOS_DIR = LOCAL_REGISTRY_DIR + File.separator + "nacos";
        NACOS_BIN_DIR = NACOS_DIR + File.separator + "bin";
        NACOS_LOG_DIR = NACOS_DIR + File.separator + "logs";
        NACOS_LOCAL_PATH_FOR_MAC = LOCAL_REGISTRY_PKG_DIR + File.separator + "nacos-server-2.4.3.zip";
        NACOS_LOCAL_PATH_FOR_WIN = LOCAL_REGISTRY_PKG_DIR + File.separator + "nacos-server-2.4.3.zip";
        NACOS_START_UP_FILE_WIN = NACOS_BIN_DIR + File.separator + "startup.cmd";
        NACOS_START_UP_FILE_MAC = NACOS_BIN_DIR + File.separator + "startup.sh";
        NACOS_START_LOG = NACOS_LOG_DIR + File.separator + "start.out";
        NACOS_PORTS = new int[] {8848};
    }
}
