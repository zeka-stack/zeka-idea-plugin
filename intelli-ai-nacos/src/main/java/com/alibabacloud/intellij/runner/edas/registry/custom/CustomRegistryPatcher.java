// //
// // Source code recreated from a .class file by IntelliJ IDEA
// // (powered by FernFlower decompiler)
// //
//
// package com.alibabacloud.intellij.runner.edas.registry.custom;
//
// import com.alibabacloud.intellij.model.edas.config.EdasPreferenceModel;
// import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryConstants;
// import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryContext;
// import com.alibabacloud.intellij.runner.edas.registry.PatchResult;
// import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectLogger;
// import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectUtils;
// import com.alibabacloud.intellij.service.edas.registry.custom.CustomRegistryJvmParamManager;
// import com.alibabacloud.intellij.service.edas.registry.custom.CustomRegistryManager;
// import com.intellij.execution.Executor;
// import com.intellij.openapi.application.ApplicationManager;
// import com.intellij.openapi.application.ModalityState;
// import com.intellij.openapi.progress.ProgressIndicator;
// import com.intellij.openapi.progress.ProgressManager;
// import com.intellij.openapi.project.Project;
// import java.io.File;
// import java.util.List;
// import java.util.Map;
//
// public class CustomRegistryPatcher {
//     public static PatchResult patch(Executor executor, Project project, String configName, List<String> vmParams, Map<String, String>
//     environments, boolean isTomcatProfile, EdasPreferenceModel.WorkSpace workSpace, EdasPreferenceModel.EdasServiceConnectModel
//     connectModel) {
//         EdasServiceConnectLogger logger = null;
//         PatchResult result = new PatchResult();
//         result.setReentrant(true);
//
//         try {
//             LocalRegistryContext localRegistryContext = new LocalRegistryContext();
//             localRegistryContext.setProject(project);
//             localRegistryContext.setConfigName(configName);
//             localRegistryContext.setExecutor(executor);
//             String logFile = getLogPath(project.getName(), configName);
//             localRegistryContext.setLogFile(logFile);
//             logger = new EdasServiceConnectLogger(logFile);
//             String host = connectModel.getCustomRegistryAddress();
//             if (!CustomRegistryManager.registryAvailable(host)) {
//                 String url = String.format("http://%s:8848/", host);
//                 String msg = String.format("registry(%s) not working", url);
//                 throw new RuntimeException(msg);
//             }
//
//             showJoiningDialog(project, host);
//             CustomRegistryJvmParamManager.setupJvmParams(vmParams, host);
//             result.setContext(localRegistryContext);
//             result.setSuccess(true);
//         } catch (Exception e) {
//             e.printStackTrace();
//             String msg = "Failed to join registry: " + e.getMessage();
//             EdasServiceConnectUtils.alertError(msg);
//         } finally {
//             if (logger != null) {
//                 logger.close();
//             }
//
//         }
//
//         return result;
//     }
//
//     private static void showJoiningDialog(final Project project, final String host) {
//         final Runnable runnable = new Runnable() {
//             public void run() {
//                 try {
//                     ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
//                     indicator.setText("Joining custom registry: " + host);
//                     indicator.setFraction((double)1.0F);
//                     Thread.sleep(2000L);
//                     if (indicator.isCanceled()) {
//                         EdasServiceConnectUtils.alertError("Failed to cancel joining");
//                     }
//                 } catch (Exception e) {
//                     e.printStackTrace();
//                 }
//
//             }
//         };
//         if (ApplicationManager.getApplication().isDispatchThread()) {
//             ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Join Custom Registry", true, project);
//         } else {
//             ApplicationManager.getApplication().invokeLater(new Runnable() {
//                 public void run() {
//                     ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Join Custom Registry", true, project);
//                 }
//             }, ModalityState.any());
//         }
//
//     }
//
//     private static String getLogPath(String projectName, String configName) {
//         return LocalRegistryConstants.CUSTOM_REGISTRY_DIR + File.separator + "custom_registry_" + projectName + "_" + configName + "
//         .log";
//     }
// }
