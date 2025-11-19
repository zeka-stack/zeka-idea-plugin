// //
// // Source code recreated from a .class file by IntelliJ IDEA
// // (powered by FernFlower decompiler)
// //
//
// package com.alibabacloud.intellij.runner.edas.registry;
//
// import com.alibabacloud.intellij.config.EdasStore;
// import com.alibabacloud.intellij.model.edas.Pair;
// import com.alibabacloud.intellij.model.edas.config.EdasPreferenceModel;
// import com.alibabacloud.intellij.model.edas.config.EdasPreferenceModel.Type;
// import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectUtils;
// import com.intellij.execution.configurations.ModuleBasedConfiguration;
// import com.intellij.execution.configurations.RunConfiguration;
// import com.intellij.openapi.components.ServiceManager;
// import com.intellij.openapi.module.Module;
// import com.intellij.openapi.project.Project;
// import java.lang.reflect.Method;
//
// public class PatchUtils {
//     public static Pair<EdasPreferenceModel.WorkSpace, EdasPreferenceModel.EdasServiceConnectModel> getPersistModel(RunConfiguration
//     configuration, Project project) {
//         String projectName = project.getName();
//         EdasStore edasStore = (EdasStore)ServiceManager.getService(EdasStore.class);
//         EdasPreferenceModel preferenceModel = edasStore.getModel();
//         if (preferenceModel == null) {
//             return null;
//         } else {
//             if (configuration != null && preferenceModel.getModuleServiceConnectMap() != null) {
//                 for(Module module : getModules(configuration)) {
//                     String moduleName = module.getName();
//                     String key = EdasPreferenceModel.generateModuleConfigKey(projectName, moduleName);
//                     EdasPreferenceModel.EdasServiceConnectModel model = (EdasPreferenceModel.EdasServiceConnectModel)preferenceModel
//                     .getModuleServiceConnectMap().get(key);
//                     if (model != null && model.getRegistryType() != -1) {
//                         EdasPreferenceModel.WorkSpace workSpace = new EdasPreferenceModel.WorkSpace(Type.MODULE, moduleName);
//                         return new Pair(workSpace, model);
//                     }
//                 }
//             }
//
//             if (preferenceModel.getServiceConnectMap() != null) {
//                 EdasPreferenceModel.WorkSpace workSpace = new EdasPreferenceModel.WorkSpace(Type.PROJECT, projectName);
//                 EdasPreferenceModel.EdasServiceConnectModel model = (EdasPreferenceModel.EdasServiceConnectModel)preferenceModel
//                 .getServiceConnectMap().get(projectName);
//                 return new Pair(workSpace, model);
//             } else {
//                 return null;
//             }
//         }
//     }
//
//     private static Module[] getModules(RunConfiguration configuration) {
//         if (configuration instanceof ModuleBasedConfiguration conf) {
//             return conf.getModules();
//         } else {
//             try {
//                 if (EdasServiceConnectUtils.isTomcatRunConfiguration(configuration)) {
//                     for(Method c : configuration.getClass().getMethods()) {
//                         String n = c.toString();
//                         if ("getModules".equals(c.getName())) {
//                             Object result = c.invoke(configuration);
//                             return (Module[])result;
//                         }
//                     }
//                 }
//             } catch (Throwable t) {
//                 t.printStackTrace();
//             }
//
//             return new Module[0];
//         }
//     }
// }
