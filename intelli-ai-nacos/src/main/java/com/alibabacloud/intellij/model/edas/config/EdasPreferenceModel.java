// //
// // Source code recreated from a .class file by IntelliJ IDEA
// // (powered by FernFlower decompiler)
// //
//
// package com.alibabacloud.intellij.model.edas.config;
//
// import com.alibabacloud.intellij.model.edas.EdasAppGroupModel;
// import com.alibabacloud.intellij.model.edas.EdasAppModel;
// import com.alibabacloud.intellij.model.edas.EdasK8sSwimLaneGroupModel;
// import com.alibabacloud.intellij.model.edas.EdasK8sSwimLaneModel;
// import com.alibabacloud.intellij.model.edas.EdasNamespaceModel;
// import com.alibabacloud.intellij.model.edas.EdasRegionModel;
// import com.alibabacloud.intellij.model.edas.EdasServiceConnectAdvanceModel;
// import com.alibabacloud.intellij.model.edas.LocalRegistry;
// import com.alibabacloud.intellij.model.edas.MicroserivceProductModel;
// import com.alibabacloud.intellij.model.edas.MseClusterModel;
// import com.alibabacloud.intellij.model.edas.MseClusterTypeModel;
// import com.alibabacloud.intellij.model.edas.SeataGroupModel;
// import com.alibabacloud.intellij.model.edas.SeataInstanceModel;
// import java.util.List;
// import java.util.Objects;
// import java.util.concurrent.ConcurrentHashMap;
//
// public class EdasPreferenceModel {
//     private ConcurrentHashMap<String, EdasServiceConnectModel> serviceConnectMap;
//     private ConcurrentHashMap<String, EdasServiceConnectModel> moduleServiceConnectMap;
//     private ConcurrentHashMap<String, WorkSpace> projectLastConfigTargetMap;
//     private volatile List<EdasProxyProfile> proxyProfiles;
//     private volatile List<EdasProxyProfile> hotCacheProxyProfiles;
//     private volatile boolean isEditingProxyProfile = false;
//
//     public static String generateModuleConfigKey(String projectName, String moduleName) {
//         return projectName + "::" + moduleName;
//     }
//
//     public synchronized EdasProxyProfile getProxyProfileById(String id) {
//         if (id == null) {
//             return null;
//         } else {
//             if (this.proxyProfiles != null) {
//                 for(EdasProxyProfile profile : this.proxyProfiles) {
//                     if (id.equals(profile.getId())) {
//                         return profile;
//                     }
//                 }
//             }
//
//             if (this.hotCacheProxyProfiles != null) {
//                 for(EdasProxyProfile profile : this.hotCacheProxyProfiles) {
//                     if (id.equals(profile.getId())) {
//                         return profile;
//                     }
//                 }
//             }
//
//             return null;
//         }
//     }
//
//     public synchronized List<EdasProxyProfile> getProxyProfilesFromCache() {
//         return this.isEditingProxyProfile ? this.hotCacheProxyProfiles : this.proxyProfiles;
//     }
//
//     public synchronized void setEditingProxyProfile(boolean value) {
//         this.isEditingProxyProfile = value;
//     }
//
//     public synchronized List<EdasProxyProfile> getProxyProfiles() {
//         return this.proxyProfiles;
//     }
//
//     public List<EdasProxyProfile> getHotCacheProxyProfiles() {
//         return this.hotCacheProxyProfiles;
//     }
//
//     public void setHotCacheProxyProfiles(List<EdasProxyProfile> hotCacheProxyProfiles) {
//         this.hotCacheProxyProfiles = hotCacheProxyProfiles;
//     }
//
//     public synchronized void setProxyProfiles(List<EdasProxyProfile> proxyProfiles) {
//         this.proxyProfiles = proxyProfiles;
//     }
//
//     public ConcurrentHashMap<String, WorkSpace> getProjectLastConfigTargetMap() {
//         return this.projectLastConfigTargetMap;
//     }
//
//     public void setProjectLastConfigTargetMap(ConcurrentHashMap<String, WorkSpace> projectLastConfigTargetMap) {
//         this.projectLastConfigTargetMap = projectLastConfigTargetMap;
//     }
//
//     public ConcurrentHashMap<String, EdasServiceConnectModel> getServiceConnectMap() {
//         return this.serviceConnectMap;
//     }
//
//     public void setServiceConnectMap(ConcurrentHashMap<String, EdasServiceConnectModel> serviceConnectMap) {
//         this.serviceConnectMap = serviceConnectMap;
//     }
//
//     public ConcurrentHashMap<String, EdasServiceConnectModel> getModuleServiceConnectMap() {
//         return this.moduleServiceConnectMap;
//     }
//
//     public void setModuleServiceConnectMap(ConcurrentHashMap<String, EdasServiceConnectModel> moduleServiceConnectMap) {
//         this.moduleServiceConnectMap = moduleServiceConnectMap;
//     }
//
//     public static class WorkSpace {
//         private Type type;
//         private String name;
//
//         public WorkSpace() {
//         }
//
//         public WorkSpace(Type type, String name) {
//             this.type = type;
//             this.name = name;
//         }
//
//         public Type getType() {
//             return this.type;
//         }
//
//         public void setType(Type type) {
//             this.type = type;
//         }
//
//         public String getName() {
//             return this.name;
//         }
//
//         public void setName(String name) {
//             this.name = name;
//         }
//
//         public String toString() {
//             if (this.type == EdasPreferenceModel.Type.PROJECT) {
//                 return "项目：" + this.name;
//             } else {
//                 return this.type == EdasPreferenceModel.Type.MODULE ? "模块：" + this.name : this.name;
//             }
//         }
//
//         public boolean equals(Object o) {
//             if (this == o) {
//                 return true;
//             } else if (o != null && this.getClass() == o.getClass()) {
//                 WorkSpace workSpace = (WorkSpace)o;
//                 return this.type == workSpace.type && Objects.equals(this.name, workSpace.name);
//             } else {
//                 return false;
//             }
//         }
//
//         public int hashCode() {
//             return Objects.hash(new Object[]{this.type, this.name});
//         }
//     }
//
//     public static enum Type {
//         PROJECT,
//         MODULE;
//     }
//
//     public static class EdasServiceConnectModel {
//         public static final int UNSET = -1;
//         public static final int NO_REGISTRY = 0;
//         public static final int USE_LOCAL_REGISTRY = 1;
//         public static final int JOIN_CUSTOM_REGISTRY = 2;
//         public static final int JOIN_EDAS_REGISTRY = 3;
//         private volatile MicroserivceProductModel product;
//         private volatile EdasRegionModel region;
//         private volatile EdasNamespaceModel namespace;
//         private volatile MseClusterTypeModel mseClusterType;
//         private volatile MseClusterModel mseCluster;
//         private volatile SeataInstanceModel seataInstance;
//         private volatile SeataGroupModel seataGroup;
//         private volatile String proxyProfileId;
//         private volatile String sshGatewayIp;
//         private volatile String sshUser;
//         private volatile String sshPassword;
//         private volatile String serverPort;
//         private volatile boolean subscribeOnly;
//         private volatile boolean enableFlowControl;
//         private volatile EdasAppModel app;
//         private volatile EdasAppGroupModel appGroup;
//         private volatile EdasK8sSwimLaneGroupModel swimLaneGroup;
//         private volatile EdasK8sSwimLaneModel swimLane;
//         private volatile EdasServiceConnectAdvanceModel advanceModel;
//         private volatile String vmOption;
//         private volatile boolean enable;
//         private volatile int registryType;
//         private String customRegistryAddress;
//         private LocalRegistry localRegistry;
//
//         public String getVmOption() {
//             return this.vmOption;
//         }
//
//         public void setVmOption(String vmOption) {
//             this.vmOption = vmOption;
//         }
//
//         public LocalRegistry getLocalRegistry() {
//             return this.localRegistry;
//         }
//
//         public void setLocalRegistry(LocalRegistry localRegistry) {
//             this.localRegistry = localRegistry;
//         }
//
//         public EdasServiceConnectAdvanceModel getAdvanceModel() {
//             return this.advanceModel;
//         }
//
//         public void setAdvanceModel(EdasServiceConnectAdvanceModel advanceModel) {
//             this.advanceModel = advanceModel;
//         }
//
//         public boolean isEdasOrSaeProduct() {
//             return this.isEdasProduct() || this.isSaeProduct();
//         }
//
//         public boolean isEdasProduct() {
//             return this.product == null || MicroserivceProductModel.EDAS.getProductId().equals(this.product.getProductId());
//         }
//
//         public boolean isSaeProduct() {
//             return this.product != null && MicroserivceProductModel.SAE.getProductId().equals(this.product.getProductId());
//         }
//
//         public boolean isMseProduct() {
//             return this.product != null && MicroserivceProductModel.MSE.getProductId().equals(this.product.getProductId());
//         }
//
//         public boolean isSeataProduct() {
//             return this.product != null && MicroserivceProductModel.SEATA.getProductId().equals(this.product.getProductId());
//         }
//
//         public boolean isUsingNacos() {
//             return this.mseClusterType != null && "Nacos-Ans".equals(this.mseClusterType.getId());
//         }
//
//         public SeataInstanceModel getSeataInstance() {
//             return this.seataInstance;
//         }
//
//         public void setSeataInstance(SeataInstanceModel seataInstance) {
//             this.seataInstance = seataInstance;
//         }
//
//         public SeataGroupModel getSeataGroup() {
//             return this.seataGroup;
//         }
//
//         public void setSeataGroup(SeataGroupModel seataGroup) {
//             this.seataGroup = seataGroup;
//         }
//
//         public MseClusterModel getMseCluster() {
//             return this.mseCluster;
//         }
//
//         public void setMseCluster(MseClusterModel mseCluster) {
//             this.mseCluster = mseCluster;
//         }
//
//         public MseClusterTypeModel getMseClusterType() {
//             return this.mseClusterType;
//         }
//
//         public void setMseClusterType(MseClusterTypeModel mseClusterType) {
//             this.mseClusterType = mseClusterType;
//         }
//
//         public String getProxyProfileId() {
//             return this.proxyProfileId;
//         }
//
//         public void setProxyProfileId(String proxyProfileId) {
//             this.proxyProfileId = proxyProfileId;
//         }
//
//         public MicroserivceProductModel getProduct() {
//             return this.product;
//         }
//
//         public void setProduct(MicroserivceProductModel product) {
//             this.product = product;
//         }
//
//         public boolean isSubscribeOnly() {
//             return this.subscribeOnly;
//         }
//
//         public void setSubscribeOnly(boolean subscribeOnly) {
//             this.subscribeOnly = subscribeOnly;
//         }
//
//         public boolean featureEnable() {
//             return this.enable || this.registryType == 1 || this.registryType == 2 || this.registryType == 3;
//         }
//
//         public boolean useLocalRegistryEnable() {
//             return this.registryType == 1;
//         }
//
//         public boolean joinEdasRegistryEnable() {
//             return this.enable || this.registryType == 3;
//         }
//
//         public boolean joinCustomRegistryEnable() {
//             return this.registryType == 2;
//         }
//
//         public int getRegistryType() {
//             return this.registryType;
//         }
//
//         public void setRegistryType(int registryType) {
//             this.registryType = registryType;
//         }
//
//         public String getCustomRegistryAddress() {
//             return this.customRegistryAddress;
//         }
//
//         public void setCustomRegistryAddress(String customRegistryAddress) {
//             this.customRegistryAddress = customRegistryAddress;
//         }
//
//         public String getServerPort() {
//             return this.serverPort;
//         }
//
//         public void setServerPort(String serverPort) {
//             this.serverPort = serverPort;
//         }
//
//         public EdasRegionModel getRegion() {
//             return this.region;
//         }
//
//         public void setRegion(EdasRegionModel region) {
//             this.region = region;
//         }
//
//         public EdasNamespaceModel getNamespace() {
//             return this.namespace;
//         }
//
//         public void setNamespace(EdasNamespaceModel namespace) {
//             this.namespace = namespace;
//         }
//
//         public String getSshGatewayIp() {
//             return this.sshGatewayIp;
//         }
//
//         public void setSshGatewayIp(String sshGatewayIp) {
//             this.sshGatewayIp = sshGatewayIp;
//         }
//
//         public String getSshUser() {
//             return this.sshUser;
//         }
//
//         public void setSshUser(String sshUser) {
//             this.sshUser = sshUser;
//         }
//
//         public String getSshPassword() {
//             return this.sshPassword;
//         }
//
//         public void setSshPassword(String sshPassword) {
//             this.sshPassword = sshPassword;
//         }
//
//         /** @deprecated */
//         @Deprecated
//         public boolean isEnable() {
//             return this.enable;
//         }
//
//         public void setEnable(boolean enable) {
//             this.enable = enable;
//         }
//
//         public boolean isEnableFlowControl() {
//             return this.enableFlowControl;
//         }
//
//         public void setEnableFlowControl(boolean enableFlowControl) {
//             this.enableFlowControl = enableFlowControl;
//         }
//
//         public EdasAppModel getApp() {
//             return this.app;
//         }
//
//         public void setApp(EdasAppModel app) {
//             this.app = app;
//         }
//
//         public EdasAppGroupModel getAppGroup() {
//             return this.appGroup;
//         }
//
//         public void setAppGroup(EdasAppGroupModel appGroup) {
//             this.appGroup = appGroup;
//         }
//
//         public EdasK8sSwimLaneGroupModel getSwimLaneGroup() {
//             return this.swimLaneGroup;
//         }
//
//         public void setSwimLaneGroup(EdasK8sSwimLaneGroupModel swimLaneGroup) {
//             this.swimLaneGroup = swimLaneGroup;
//         }
//
//         public EdasK8sSwimLaneModel getSwimLane() {
//             return this.swimLane;
//         }
//
//         public void setSwimLane(EdasK8sSwimLaneModel swimLane) {
//             this.swimLane = swimLane;
//         }
//     }
// }
