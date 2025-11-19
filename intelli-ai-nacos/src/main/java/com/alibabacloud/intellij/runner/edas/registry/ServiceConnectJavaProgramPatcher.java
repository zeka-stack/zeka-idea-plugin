//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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

    // public void patchJavaParameters(Executor executor, RunProfile configuration, JavaParameters javaParameters) {
    //     try {
    //         if (!(configuration instanceof RunConfiguration)) {
    //             return;
    //         }
    //
    //         RunConfiguration runConfiguration = (RunConfiguration)configuration;
    //         ConfigurationType type = runConfiguration.getType();
    //         if (type instanceof MavenRunConfigurationType) {
    //             return;
    //         }
    //
    //         if (EdasServiceConnectUtils.isTomcatRunConfiguration(runConfiguration)) {
    //             if (ApplicationManager.getApplication().isDispatchThread() && ApplicationManager.getApplication()
    //             .getCurrentModalityState() != ModalityState.NON_MODAL) {
    //                 return;
    //             }
    //
    //             if (System.currentTimeMillis() < tomcatCoolingTime) {
    //                 return;
    //             }
    //         }
    //
    //         Project project = runConfiguration.getProject();
    //         if (project == null) {
    //             return;
    //         }
    //
    //         String projectName = project.getName();
    //         String configName = configuration.getName();
    //         Pair<EdasPreferenceModel.WorkSpace, EdasPreferenceModel.EdasServiceConnectModel> pair = PatchUtils.getPersistModel
    //         (runConfiguration, project);
    //         if (pair == null || pair.getRight() == null || !((EdasPreferenceModel.EdasServiceConnectModel)pair.getRight())
    //         .featureEnable()) {
    //             return;
    //         }
    //
    //         if (runningProjectConfiguration.containsKey(getCachedProjectConfigurationKey(projectName, configName))) {
    //             return;
    //         }
    //
    //         EdasPreferenceModel.WorkSpace workSpace = (EdasPreferenceModel.WorkSpace)pair.getLeft();
    //         EdasPreferenceModel.EdasServiceConnectModel connectModel = (EdasPreferenceModel.EdasServiceConnectModel)pair.getRight();
    //         PatchResult result = patch(executor, configuration, javaParameters, workSpace, connectModel);
    //         if (result.isSuccess()) {
    //             if (!result.isReentrant()) {
    //                 runningProjectConfiguration.put(getCachedProjectConfigurationKey(projectName, configName), true);
    //             }
    //         } else if (EdasServiceConnectUtils.isTomcatRunConfiguration(runConfiguration)) {
    //             tomcatCoolingTime = System.currentTimeMillis() + 3000L;
    //         }
    //     } catch (Throwable t) {
    //         t.printStackTrace();
    //     }
    //
    // }

    // private static PatchResult patch(Executor executor, RunProfile configuration, JavaParameters javaParameters, EdasPreferenceModel
    // .WorkSpace workSpace, EdasPreferenceModel.EdasServiceConnectModel connectModel) {
    //     RunConfiguration runConfiguration = (RunConfiguration)configuration;
    //     Project project = runConfiguration.getProject();
    //     String configName = runConfiguration.getName();
    //     List<String> vmParams = new ArrayList();
    //     Map<String, String> environments = new HashMap();
    //     boolean isTomcatProfile = EdasServiceConnectUtils.isTomcatRunConfiguration(runConfiguration);
    //     PatchResult result;
    //     if (connectModel.joinEdasRegistryEnable()) {
    //         result = EdasRegistryPatcher.patch(executor, project, configName, vmParams, environments, isTomcatProfile, workSpace,
    //         connectModel);
    //     } else if (connectModel.useLocalRegistryEnable()) {
    //         result = LocalRegistryPatcher.patch(executor, project, configName, vmParams, environments, isTomcatProfile, workSpace,
    //         connectModel);
    //     } else if (connectModel.joinCustomRegistryEnable()) {
    //         result = CustomRegistryPatcher.patch(executor, project, configName, vmParams, environments, isTomcatProfile, workSpace,
    //         connectModel);
    //     } else {
    //         result = new PatchResult();
    //         result.setReentrant(true);
    //     }
    //
    //     javaParameters.getVMParametersList().addAll(vmParams);
    //     javaParameters.getEnv().putAll(environments);
    //     return result;
    // }

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
