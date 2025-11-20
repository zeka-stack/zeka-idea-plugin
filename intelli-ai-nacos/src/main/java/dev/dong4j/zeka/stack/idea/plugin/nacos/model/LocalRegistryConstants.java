package dev.dong4j.zeka.stack.idea.plugin.nacos.model;

import java.io.File;

/**
 * 本地注册中心常量定义
 * 包含本地 Nacos 注册中心相关的路径、URL、端口等配置常量
 *
 * @author dong4j
 * @since 1.0.0
 */
public class LocalRegistryConstants {

    /** 应用主目录 */
    public static final String EDAS_HOME;
    /** 注册中心根目录 */
    public static final String REGISTRY_DIR;
    /** 本地注册中心目录 */
    public static final String LOCAL_REGISTRY_DIR;
    /** 本地注册中心包目录 */
    public static final String LOCAL_REGISTRY_PKG_DIR;
    /** 本地注册中心日志目录 */
    public static final String LOCAL_REGISTRY_LOG_DIR;
    /** 本地注册中心临时日志文件 */
    public static final String LOCAL_REGISTRY_TMP_LOG;
    /** Nacos 目录 */
    public static final String NACOS_DIR;
    /** Nacos bin 目录 */
    public static final String NACOS_BIN_DIR;
    /** Nacos 日志目录 */
    public static final String NACOS_LOG_DIR;
    /** Nacos 远程下载地址 */
    public static final String LOCAL_USERNAME;

    /**
     * 获取 Nacos 远程下载地址
     *
     * @param version Nacos 版本号
     * @return 远程下载地址
     */
    public static String getNacosRemotePath(String version) {
        return "https://github.com/alibaba/nacos/releases/download/" + version + "/nacos-server-" + version + ".zip";
    }

    /**
     * 获取 Nacos Mac 本地路径
     *
     * @param version Nacos 版本号
     * @return Mac 本地路径
     */
    public static String getNacosLocalPathForMac(String version) {
        return LOCAL_REGISTRY_PKG_DIR + File.separator + "nacos-server-" + version + ".zip";
    }

    /**
     * 获取 Nacos Windows 本地路径
     *
     * @param version Nacos 版本号
     * @return Windows 本地路径
     */
    public static String getNacosLocalPathForWin(String version) {
        return LOCAL_REGISTRY_PKG_DIR + File.separator + "nacos-server-" + version + ".zip";
    }

    /** Nacos Windows 启动文件 */
    public static final String NACOS_START_UP_FILE_WIN;
    /** Nacos Mac 启动文件 */
    public static final String NACOS_START_UP_FILE_MAC;
    /** Nacos 启动日志文件 */
    public static final String NACOS_START_LOG;
    /** Nacos 测试 URL */
    public static final String NACOS_TEST_URL = "http://127.0.0.1:8848/nacos/index.html";
    /** Nacos 使用的端口列表 */
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
        NACOS_START_UP_FILE_WIN = NACOS_BIN_DIR + File.separator + "startup.cmd";
        NACOS_START_UP_FILE_MAC = NACOS_BIN_DIR + File.separator + "startup.sh";
        NACOS_START_LOG = NACOS_LOG_DIR + File.separator + "start.out";
        NACOS_PORTS = new int[] {8848};
        LOCAL_USERNAME = "localman";
    }
}
