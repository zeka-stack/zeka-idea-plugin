package com.alibabacloud.intellij.runner.edas.registry;


import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.JavaProgramPatcher;

import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("All")
public class ServiceConnectJavaProgramPatcher extends JavaProgramPatcher {
    private static final ConcurrentHashMap<String, Boolean> runningProjectConfiguration = new ConcurrentHashMap();
    private static volatile long tomcatCoolingTime = 0L;



    public static void notifyShutdown(String projectName, String configurationName) {
        runningProjectConfiguration.remove(getCachedProjectConfigurationKey(projectName, configurationName));
    }

    private static String getCachedProjectConfigurationKey(String projectName, String configurationName) {
        return projectName + ":" + configurationName;
    }

    @Override
    public void patchJavaParameters(Executor executor, RunProfile runProfile, JavaParameters javaParameters) {

    }
}
