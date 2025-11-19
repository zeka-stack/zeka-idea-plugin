// //
// // Source code recreated from a .class file by IntelliJ IDEA
// // (powered by FernFlower decompiler)
// //
//
// package com.alibabacloud.intellij.runner.edas.registry.local;
//
// import com.alibabacloud.intellij.config.EdasStore;
// import com.alibabacloud.intellij.model.edas.LocalRegistry;
// import com.alibabacloud.intellij.model.edas.config.EdasPreferenceModel;
// import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryConstants;
// import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryContext;
// import com.alibabacloud.intellij.runner.edas.registry.PatchResult;
// import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectLogger;
// import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectUtils;
// import com.alibabacloud.intellij.service.edas.registry.local.LocalRegistryJvmParamManager;
// import com.alibabacloud.intellij.service.edas.registry.local.LocalRegistryManager;
// import com.intellij.execution.Executor;
// import com.intellij.openapi.application.ApplicationManager;
// import com.intellij.openapi.application.ModalityState;
// import com.intellij.openapi.components.ServiceManager;
// import com.intellij.openapi.progress.ProgressIndicator;
// import com.intellij.openapi.progress.ProgressManager;
// import com.intellij.openapi.progress.Task;
// import com.intellij.openapi.progress.util.ProgressIndicatorBase;
// import com.intellij.openapi.project.Project;
// import com.intellij.openapi.util.Condition;
// import java.io.File;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.Semaphore;
// import org.jetbrains.annotations.NotNull;
//
// @SuppressWarnings("All")
// public class LocalRegistryPatcher {
//     public static PatchResult patch(Executor executor, Project project, String configName, List<String> vmParams, Map<String, String>
//     environments, boolean isTomcatProfile, EdasPreferenceModel.WorkSpace workSpace, EdasPreferenceModel.EdasServiceConnectModel
//     connectModel) {
//         EdasServiceConnectLogger logger = null;
//         PatchResult result = new PatchResult();
//
//         try {
//             LocalRegistryContext localRegistryContext = new LocalRegistryContext();
//             localRegistryContext.setProject(project);
//             localRegistryContext.setConfigName(configName);
//             localRegistryContext.setExecutor(executor);
//             if (connectModel.getLocalRegistry() != null && connectModel.getLocalRegistry() != LocalRegistry.LIGHT_WEIGHT) {
//                 localRegistryContext.setRegistry(LocalRegistry.NACOS);
//             } else {
//                 localRegistryContext.setRegistry(LocalRegistry.LIGHT_WEIGHT);
//             }
//
//             String logFile = getLogPath(project.getName(), configName);
//             localRegistryContext.setLogFile(logFile);
//             logger = new EdasServiceConnectLogger(logFile);
//             downloadRegistry(localRegistryContext.getRegistry(), project, logger);
//             LocalRegistryManager.startRegistryFromAppStart(executor, localRegistryContext, project, logger);
//             if (localRegistryContext.getStartedByOtherOwner()) {
//                 showJoiningDialog(project);
//             }
//
//             LocalRegistryJvmParamManager.setupJVMParams(vmParams, environments);
//             if (localRegistryContext.getStartedByOtherOwner()) {
//                 result.setReentrant(true);
//             }
//
//             result.setContext(localRegistryContext);
//             result.setSuccess(true);
//         } catch (Exception e) {
//             e.printStackTrace();
//             String msg = "Failed to start: " + e.getMessage();
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
//     private static void showJoiningDialog(final Project project) {
//         final Runnable runnable = new Runnable() {
//             public void run() {
//                 try {
//                     ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
//                     indicator.setText("Joining local registry...");
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
//             ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Use Local Registry", true, project);
//         } else {
//             ApplicationManager.getApplication().invokeLater(new Runnable() {
//                 public void run() {
//                     ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Use Local Registry", true, project);
//                 }
//             }, ModalityState.any());
//         }
//
//     }
//
//     private static EdasPreferenceModel.EdasServiceConnectModel getPersistModel(String projectName) {
//         EdasStore edasStore = (EdasStore)ServiceManager.getService(EdasStore.class);
//         EdasPreferenceModel preferenceModel = edasStore.getModel();
//         return preferenceModel != null && preferenceModel.getServiceConnectMap() != null ? (EdasPreferenceModel
//         .EdasServiceConnectModel)preferenceModel.getServiceConnectMap().get(projectName) : null;
//     }
//
//     private static String getLogPath(String projectName, String configName) {
//         return LocalRegistryConstants.LOCAL_REGISTRY_LOG_DIR + File.separator + "local_registry_" + projectName + "_" + configName + "
//         .log";
//     }
//
//     private static void downloadRegistry(final LocalRegistry registry, final Project project, final EdasServiceConnectLogger logger)
//     throws Exception {
//         if (LocalRegistryManager.isRegisterDownloaded(registry)) {
//             logger.info("registry is already downloaded, ignore");
//         } else {
//             logger.info("Download registry...");
//             final Semaphore started = new Semaphore(0);
//             final Semaphore finish = new Semaphore(0);
//             final Runnable runnable = new Runnable() {
//                 public void run() {
//                     try {
//                         started.release(1);
//                         ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
//                         if (registry == LocalRegistry.LIGHT_WEIGHT) {
//                             indicator.setText("Downloading EDAS lightweight server (45MB)");
//                         } else {
//                             indicator.setText("Downloading Nacos Server (72MB)");
//                         }
//
//                         LocalRegistryManager.downloadRegistry(registry, indicator, logger);
//                         logger.info("Indicator exit");
//                     } catch (Exception e) {
//                         logger.info(e.getMessage());
//                         logger.info(ExceptionUtils.getExceptionStack(e));
//                     } finally {
//                         finish.release(1);
//                     }
//
//                 }
//             };
//             final long fallbackToAsyncProgress = System.currentTimeMillis() + 3000L;
//             Condition expired = new Condition() {
//                 public boolean value(Object o) {
//                     return System.currentTimeMillis() >= fallbackToAsyncProgress;
//                 }
//             };
//
//             try {
//                 if (ApplicationManager.getApplication().isDispatchThread()) {
//                     ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Local Registry", true, project);
//                 } else {
//                     ApplicationManager.getApplication().invokeLater(new Runnable() {
//                         public void run() {
//                             ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Local Registry", true, project);
//                         }
//                     }, ModalityState.any(), expired);
//                 }
//             } catch (Exception e) {
//                 finish.release(1);
//                 logger.info(ExceptionUtils.getExceptionStack(e));
//             }
//
//             boolean isStarted = false;
//
//             while(true) {
//                 isStarted = started.tryAcquire(1);
//                 if (isStarted || System.currentTimeMillis() > fallbackToAsyncProgress) {
//                     if (!isStarted) {
//                         logger.info("Intellij IDEA UI thread seems freezing, fall back to background task to download registry...");
//                         Task.Backgroundable backgroundable = new Task.Backgroundable(project, "Local Registry", true) {
//                             public void run(@NotNull ProgressIndicator indicator) {
//                                 indicator.setText("Downloading Etrans (10MB)");
//
//                                 try {
//                                     LocalRegistryManager.downloadRegistry(registry, indicator, logger);
//                                 } catch (Exception e) {
//                                     logger.info(ExceptionUtils.getExceptionStack(e));
//                                 } finally {
//                                     finish.release(1);
//                                 }
//
//                             }
//                         };
//                         ProgressManager.getInstance().runProcessWithProgressAsynchronously(backgroundable, new ProgressIndicatorBase());
//                     }
//
//                     finish.acquire(1);
//                     if (LocalRegistryManager.isRegisterDownloaded(registry)) {
//                         logger.info("Download registry successfully, you can check it in " + LocalRegistryManager
//                         .getRegisterPackageFilePath(registry));
//                         return;
//                     } else {
//                         throw new Exception("Failed to download EDAS light weight server");
//                     }
//                 }
//
//                 Thread.sleep(1000L);
//             }
//         }
//     }
//
//     private static String getCachedProjectConfigurationKey(String projectName, String configurationName) {
//         return projectName + ":" + configurationName;
//     }
// }
