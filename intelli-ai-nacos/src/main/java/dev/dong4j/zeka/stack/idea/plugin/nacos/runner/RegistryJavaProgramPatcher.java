package dev.dong4j.zeka.stack.idea.plugin.nacos.runner;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册中心服务连接 Java 程序修补器
 * 用于在运行配置启动时注入 JVM 参数
 *
 * @author dong4j
 * @since 1.0.0
 */
@SuppressWarnings("All")
public class RegistryJavaProgramPatcher extends JavaProgramPatcher {
    
    private static final ConcurrentHashMap<String, Boolean> runningProjectConfiguration = new ConcurrentHashMap();
    private static volatile long tomcatCoolingTime = 0L;

    /**
     * 通知配置关闭
     *
     * @param projectName       项目名称
     * @param configurationName 配置名称
     */
    public static void notifyShutdown(String projectName, String configurationName) {
        runningProjectConfiguration.remove(getCachedProjectConfigurationKey(projectName, configurationName));
    }

    /**
     * 获取缓存的项目配置键
     *
     * @param projectName       项目名称
     * @param configurationName 配置名称
     * @return 缓存键
     */
    private static String getCachedProjectConfigurationKey(String projectName, String configurationName) {
        return projectName + ":" + configurationName;
    }

    @Override
    public void patchJavaParameters(Executor executor, RunProfile runProfile, JavaParameters javaParameters) {
        // 预留扩展点：可在此处注入 JVM 参数
    }
}
