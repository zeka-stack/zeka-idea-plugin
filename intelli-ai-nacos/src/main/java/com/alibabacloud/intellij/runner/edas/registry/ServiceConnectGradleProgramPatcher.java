// //
// // Source code recreated from a .class file by IntelliJ IDEA
// // (powered by FernFlower decompiler)
// //
//
// package com.alibabacloud.intellij.runner.edas.registry;
//
// import com.alibabacloud.intellij.model.edas.Pair;
// import com.alibabacloud.intellij.model.edas.config.EdasPreferenceModel;
// import com.alibabacloud.intellij.model.edas.registry.Context;
// import com.alibabacloud.intellij.runner.edas.registry.cloud.EdasRegistryPatcher;
// import com.alibabacloud.intellij.runner.edas.registry.custom.CustomRegistryPatcher;
// import com.alibabacloud.intellij.runner.edas.registry.local.LocalRegistryPatcher;
// import com.google.common.collect.Sets;
// import com.intellij.execution.ExecutionManager;
// import com.intellij.execution.Executor;
// import com.intellij.execution.configurations.RunConfiguration;
// import com.intellij.execution.process.ProcessAdapter;
// import com.intellij.execution.process.ProcessEvent;
// import com.intellij.execution.process.ProcessHandler;
// import com.intellij.execution.process.ProcessOutputTypes;
// import com.intellij.execution.ui.RunContentDescriptor;
// import com.intellij.openapi.externalSystem.model.ExternalSystemException;
// import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
// import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
// import com.intellij.openapi.project.Project;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import org.jetbrains.annotations.NotNull;
// import org.jetbrains.annotations.Nullable;
// import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;
// import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension;
// import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
//
// @SuppressWarnings("All")
// public class ServiceConnectGradleProgramPatcher implements GradleTaskManagerExtension {
//     private static final Set<String> taskSkipSet = Sets.newHashSet(new String[]{"assemble", "bootJar", "build", "buildDependents",
//     "buildNeeded", "classes", "clean", "jar", "testClasses"});
//     private static final String INIT_SCRIPT_TEMPLATE = "gradle.taskGraph.beforeTask { Task task ->\n    if (task instanceof
//     JavaForkOptions) {  \n        def jvmArgs = task.jvmArgs\n        %s        task.jvmArgs = jvmArgs\n    }\n}";
//
//     public boolean executeTasks(@NotNull ExternalSystemTaskId id, @NotNull List<String> taskNames, @NotNull String projectPath,
//     @Nullable GradleExecutionSettings settings, @Nullable String jvmParametersSetup, @NotNull ExternalSystemTaskNotificationListener
//     listener) throws ExternalSystemException {
//         if (projectPath == null) {
//             $$$reportNull$$$0(2);
//         }
//
//         if (listener == null) {
//             $$$reportNull$$$0(3);
//         }
//
//         if (settings == null) {
//             return false;
//         } else {
//             try {
//                 Project project = id.findProject();
//                 if (project == null) {
//                     return false;
//                 }
//
//                 if (taskNoRun(taskNames)) {
//                     return false;
//                 }
//
//                 String projectName = project.getName();
//                 String configName = "default";
//                 Map<String, String> envs = new HashMap();
//                 List<String> vmParams = new ArrayList();
//                 Pair<EdasPreferenceModel.WorkSpace, EdasPreferenceModel.EdasServiceConnectModel> pair = PatchUtils.getPersistModel(
//                 (RunConfiguration)null, project);
//                 if (pair == null || pair.getRight() == null || !((EdasPreferenceModel.EdasServiceConnectModel)pair.getRight())
//                 .featureEnable()) {
//                     return false;
//                 }
//
//                 List<RunContentDescriptor> descriptors = ExecutionManager.getInstance(project).getContentManager().getAllDescriptors();
//                 if (descriptors.isEmpty()) {
//                     return false;
//                 }
//
//                 ProcessHandler processHandler = null;
//
//                 for(int i = descriptors.size() - 1; i >= 0; --i) {
//                     if (descriptors.get(i) != null && ((RunContentDescriptor)descriptors.get(i)).getProcessHandler() != null && (
//                     (RunContentDescriptor)descriptors.get(i)).getProcessHandler().getExitCode() == null) {
//                         processHandler = ((RunContentDescriptor)descriptors.get(i)).getProcessHandler();
//                         break;
//                     }
//                 }
//
//                 if (processHandler == null) {
//                     return false;
//                 }
//
//                 processHandler.notifyTextAvailable("patching microservice params...\n", ProcessOutputTypes.STDOUT);
//                 final PatchResult result = patch(project, configName, vmParams, envs, (EdasPreferenceModel.WorkSpace)pair.getLeft(),
//                 (EdasPreferenceModel.EdasServiceConnectModel)pair.getRight());
//                 if (!result.isSuccess()) {
//                     processHandler.notifyTextAvailable("patch EDAS params failed\n", ProcessOutputTypes.STDOUT);
//                     return false;
//                 }
//
//                 String appendJvmArgs = generateAppendedJvmArgs(vmParams);
//                 String appendedScript = String.format("gradle.taskGraph.beforeTask { Task task ->\n    if (task instanceof
//                 JavaForkOptions) {  \n        def jvmArgs = task.jvmArgs\n        %s        task.jvmArgs = jvmArgs\n    }\n}",
//                 appendJvmArgs);
//                 String initScript = (String)settings.getUserData(GradleTaskManager.INIT_SCRIPT_KEY);
//                 if (initScript != null) {
//                     initScript = initScript + "\n" + appendedScript;
//                 } else {
//                     initScript = appendedScript;
//                 }
//
//                 settings.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, initScript);
//                 settings.withEnvironmentVariables(envs);
//                 processHandler.addProcessListener(new ProcessAdapter() {
//                     public void processTerminated(@NotNull ProcessEvent event) {
//                         if (result.isSuccess()) {
//                             ServiceConnectGradleProgramPatcher.terminateExternalProcess(result.getContext());
//                         }
//
//                         super.processTerminated(event);
//                     }
//                 });
//             } catch (Throwable t) {
//                 t.printStackTrace();
//             }
//
//             return false;
//         }
//     }
//
//     public boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener) throws
//     ExternalSystemException {
//         return false;
//     }
//
//     private static String generateAppendedJvmArgs(List<String> args) {
//         StringBuilder stringBuilder = new StringBuilder();
//
//         for(String arg : args) {
//             arg = arg.replace("\\", "\\\\");
//             stringBuilder.append(String.format("jvmArgs = jvmArgs + '%s'\n", arg));
//         }
//
//         return stringBuilder.toString();
//     }
//
//     private static boolean taskNoRun(List<String> taskNames) {
//         if (taskNames != null && !taskNames.isEmpty()) {
//             for(String task : taskNames) {
//                 if (!taskSkipSet.contains(task)) {
//                     return false;
//                 }
//             }
//
//             return true;
//         } else {
//             return true;
//         }
//     }
//
//     private static PatchResult patch(Project project, String configName, List<String> vmParams, Map<String, String> environments,
//     EdasPreferenceModel.WorkSpace workSpace, EdasPreferenceModel.EdasServiceConnectModel connectModel) {
//         PatchResult result;
//         if (connectModel.joinEdasRegistryEnable()) {
//             result = EdasRegistryPatcher.patch((Executor)null, project, configName, vmParams, environments, false, workSpace,
//             connectModel);
//         } else if (connectModel.useLocalRegistryEnable()) {
//             result = LocalRegistryPatcher.patch((Executor)null, project, configName, vmParams, environments, false, workSpace,
//             connectModel);
//         } else if (connectModel.joinCustomRegistryEnable()) {
//             result = CustomRegistryPatcher.patch((Executor)null, project, configName, vmParams, environments, false, workSpace,
//             connectModel);
//         } else {
//             result = new PatchResult();
//             result.setReentrant(true);
//         }
//
//         return result;
//     }
//
//     private static void terminateExternalProcess(Context context) {
//         try {
//             if (context == null) {
//                 return;
//             }
//
//             context.cleanResource();
//         } catch (Throwable t) {
//             t.printStackTrace();
//         }
//
//     }
//
//     private static String getCachedProjectConfigurationKey(String projectName, String configurationName) {
//         return projectName + ":" + configurationName;
//     }
// }
