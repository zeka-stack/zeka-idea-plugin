package dev.dong4j.zeka.stack.idea.plugin.nacos.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

import dev.dong4j.zeka.stack.idea.plugin.kit.StorageUtil;
import dev.dong4j.zeka.stack.idea.plugin.nacos.PluginContents;

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
     * 获取 Nacos 远程下载地址（支持代理）
     *
     * @param version     Nacos 版本号
     * @param enableProxy 是否启用代理
     * @param proxyUrl    代理地址
     * @return 远程下载地址
     */
    public static String getNacosRemotePath(String version, boolean enableProxy, String proxyUrl) {
        String baseUrl = getNacosRemotePath(version);
        if (enableProxy && proxyUrl != null && !proxyUrl.trim().isEmpty()) {
            String proxy = proxyUrl.trim();
            // 确保代理地址以 / 结尾
            if (!proxy.endsWith("/")) {
                proxy += "/";
            }
            return proxy + baseUrl;
        }
        return baseUrl;
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

    /** Nacos 测试 URL */
    public static final String NACOS_TEST_URL = "http://127.0.0.1:8848/nacos/index.html";
    /** Nacos 使用的端口列表 */
    public static final int[] NACOS_PORTS;

    static {
        String var10000 = System.getProperty("user.home");
        EDAS_HOME = var10000 + File.separator + ".zeka-stack";
        REGISTRY_DIR = EDAS_HOME + File.separator + "registry";
        LOCAL_REGISTRY_DIR = REGISTRY_DIR + File.separator + "local";
        Path pluginDir = getPluginDir();
        LOCAL_REGISTRY_PKG_DIR = pluginDir.resolve("dists").toString();
        LOCAL_REGISTRY_LOG_DIR = LOCAL_REGISTRY_DIR + File.separator + "logs";
        LOCAL_REGISTRY_TMP_LOG = LOCAL_REGISTRY_DIR + File.separator + "temp_log";
        NACOS_PORTS = new int[] {8848};
        LOCAL_USERNAME = "localman";
    }

    /**
     * 获取 Nacos 目录（根据版本号）
     *
     * @param version Nacos 版本号
     * @return Nacos 目录路径
     */
    public static String getNacosDir(String version) {
        return getPluginDir().resolve(version).toString();
    }

    /**
     * 获取 Nacos bin 目录（根据版本号）
     *
     * @param version Nacos 版本号
     * @return Nacos bin 目录路径
     */
    public static String getNacosBinDir(String version) {
        return getNacosDir(version) + File.separator + "bin";
    }

    /**
     * 获取 Nacos 日志目录（根据版本号）
     *
     * @param version Nacos 版本号
     * @return Nacos 日志目录路径
     */
    public static String getNacosLogDir(String version) {
        return getNacosDir(version) + File.separator + "logs";
    }

    /**
     * 获取 Nacos Windows 启动文件路径（根据版本号）
     *
     * @param version Nacos 版本号
     * @return Windows 启动文件路径
     */
    public static String getNacosStartUpFileWin(String version) {
        return getNacosBinDir(version) + File.separator + "startup.cmd";
    }

    /**
     * 获取 Nacos Mac 启动文件路径（根据版本号）
     *
     * @param version Nacos 版本号
     * @return Mac 启动文件路径
     */
    public static String getNacosStartUpFileMac(String version) {
        return getNacosBinDir(version) + File.separator + "startup.sh";
    }

    /**
     * 获取 Nacos 启动日志文件路径（根据版本号）
     *
     * @param version Nacos 版本号
     * @return 启动日志文件路径
     */
    public static String getNacosStartLog(String version) {
        return getNacosLogDir(version) + File.separator + "start.out";
    }

    @NotNull
    private static Path getPluginDir() {
        return StorageUtil.getPluginStorageDir(PluginContents.PLUGIN_SIMPLE_NAME);
    }
}
