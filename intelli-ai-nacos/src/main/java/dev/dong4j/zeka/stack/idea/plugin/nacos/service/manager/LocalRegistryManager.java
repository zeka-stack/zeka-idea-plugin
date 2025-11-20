package dev.dong4j.zeka.stack.idea.plugin.nacos.service.manager;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.actions.StopProcessAction;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.actions.CloseAction;
import com.intellij.icons.AllIcons.RunConfigurations;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.exception.UserCancelException;
import dev.dong4j.zeka.stack.idea.plugin.nacos.model.LocalRegistry;
import dev.dong4j.zeka.stack.idea.plugin.nacos.model.LocalRegistryConstants;
import dev.dong4j.zeka.stack.idea.plugin.nacos.model.LocalRegistryContext;
import dev.dong4j.zeka.stack.idea.plugin.nacos.process.LocalRegistryProcessHandler;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.PortManager;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.RegistryLogger;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.RegistryLogger.EmptyLogger;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.RegistryUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.UrlTestManager;
import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;

/**
 * 本地注册中心管理器
 * 负责本地 Nacos 注册中心的下载、启动、停止等生命周期管理
 *
 * @author dong4j
 * @since 1.0.0
 */
@SuppressWarnings("All")
public class LocalRegistryManager {

    private static final Object downloadLock = new Object();

    /**
     * 检查注册中心是否已下载
     *
     * @param registry 注册中心类型
     * @return true 如果已下载
     */
    public static boolean isRegisterDownloaded(LocalRegistry registry) {
        String registerPath = getRegisterStartupFilePath(registry);
        return checkRegisterExists(registerPath);
    }

    /**
     * 检查注册中心文件是否存在
     *
     * @param path 文件路径
     * @return true 如果文件存在
     */
    private static boolean checkRegisterExists(String path) {
        File startupFile = new File(path);
        return startupFile.exists();
    }

    /**
     * 获取注册中心启动文件路径
     *
     * @param registry 注册中心类型
     * @return 启动文件路径
     */
    public static String getRegisterStartupFilePath(LocalRegistry registry) {
        return RegistryUtils.isWindows() ? LocalRegistryConstants.NACOS_START_UP_FILE_WIN :
               LocalRegistryConstants.NACOS_START_UP_FILE_MAC;
    }


    /**
     * 获取注册中心安装包文件路径（支持版本号）
     *
     * @param registry 注册中心类型
     * @param version  Nacos 版本号
     * @return 安装包文件路径
     */
    public static String getRegisterPackageFilePath(LocalRegistry registry, String version) {
        return RegistryUtils.isWindows() ? LocalRegistryConstants.getNacosLocalPathForWin(version) :
               LocalRegistryConstants.getNacosLocalPathForMac(version);
    }

    /**
     * 下载注册中心
     * <p>
     * 从设置中获取当前选择的版本号
     *
     * @param registry  注册中心类型
     * @param indicator 进度指示器
     * @param logger    日志记录器
     * @throws InterruptedException 中断异常
     */
    public static void downloadRegistry(final LocalRegistry registry, ProgressIndicator indicator, final RegistryLogger logger) throws InterruptedException {
        dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState settings =
            dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState.getInstance();
        String version = settings.localNacosVersion != null && !settings.localNacosVersion.isEmpty()
                         ? settings.localNacosVersion : "2.4.3";
        downloadRegistry(registry, version, indicator, logger);
    }

    /**
     * 下载注册中心（支持版本号）
     *
     * @param registry  注册中心类型
     * @param version   Nacos 版本号
     * @param indicator 进度指示器
     * @param logger    日志记录器
     * @throws InterruptedException 中断异常
     */
    public static void downloadRegistry(final LocalRegistry registry, final String version, ProgressIndicator indicator,
                                        final RegistryLogger logger) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (LocalRegistryManager.downloadLock) {
                        // 检查指定版本是否已下载
                        String packagePath = getRegisterPackageFilePath(registry, version);
                        File packageFile = new File(packagePath);
                        if (packageFile.exists() && LocalRegistryManager.isRegisterDownloaded(registry)) {
                            logger.info("Registry version " + version + " is already downloaded");
                            return;
                        }

                        logger.info("Registry version " + version + " not exists, going to download");
                        LocalRegistryManager.download0(registry, version, logger);
                        logger.info("Download registry version " + version + " successfully");
                    }
                } catch (Exception e) {
                    logger.info("Failed to download registry version " + version + ": " + e.getMessage());
                }
            }
        });
        t.start();

        for (; t.isAlive() && !indicator.isCanceled(); Thread.sleep(100L)) {
            File file = new File(getRegisterPackageFilePath(registry, version));
            if (file.exists()) {
                double length = (double) file.length();
                indicator.setFraction(length / (double) 7.2E7F);
            }
        }

        if (t.isAlive()) {
            t.interrupt();
        }
    }

    /**
     * 执行实际的下载操作
     * <p>
     * 从设置中获取当前选择的版本号
     *
     * @param registry 注册中心类型
     * @param logger   日志记录器
     * @throws Exception 异常
     */
    private static void download0(LocalRegistry registry, RegistryLogger logger) throws Exception {
        dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState settings =
            dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState.getInstance();
        String version = settings.localNacosVersion != null && !settings.localNacosVersion.isEmpty()
                         ? settings.localNacosVersion : "2.4.3";
        download0(registry, version, logger);
    }

    /**
     * 执行实际的下载操作（支持版本号）
     *
     * @param registry 注册中心类型
     * @param version  Nacos 版本号
     * @param logger   日志记录器
     * @throws Exception 异常
     */
    private static void download0(LocalRegistry registry, String version, RegistryLogger logger) throws Exception {
        String startupFile = RegistryUtils.isWindows()
                             ? LocalRegistryConstants.NACOS_START_UP_FILE_WIN
                             : LocalRegistryConstants.NACOS_START_UP_FILE_MAC;
        String localPath = RegistryUtils.isWindows()
                           ? LocalRegistryConstants.getNacosLocalPathForWin(version)
                           : LocalRegistryConstants.getNacosLocalPathForMac(version);
        String remotePath = LocalRegistryConstants.getNacosRemotePath(version);

        // 如果启动文件不存在，或者本地 zip 文件不存在，则下载
        if (!checkRegisterExists(startupFile) || !new File(localPath).exists()) {
            // 确保目录存在
            File pkgDir = new File(LocalRegistryConstants.LOCAL_REGISTRY_PKG_DIR);
            if (!pkgDir.exists()) {
                pkgDir.mkdirs();
            }

            RegistryUtils.download(remotePath, localPath, logger);
            logger.info("Registry version " + version + " is downloaded to " + localPath);

            // 如果启动文件不存在，则解压
            if (!checkRegisterExists(startupFile)) {
                if (RegistryUtils.isWindows()) {
                    RegistryUtils.unzip(localPath, LocalRegistryConstants.LOCAL_REGISTRY_DIR);
                } else {
                    String command = String.format("unzip %s -d %s", localPath, LocalRegistryConstants.LOCAL_REGISTRY_DIR);
                    Process extractProcess = Runtime.getRuntime().exec(command, (String[]) null,
                                                                       new File(LocalRegistryConstants.LOCAL_REGISTRY_PKG_DIR));
                    int result = extractProcess.waitFor();
                    if (result != 0) {
                        throw new Exception("Unable to extract registry package, the package maybe broken");
                    }
                }
                logger.info("Registry version " + version + " is extract to " + LocalRegistryConstants.LOCAL_REGISTRY_DIR);
            }
        }
    }

    /**
     * 从应用启动时启动注册中心
     *
     * @param executor             执行器
     * @param localRegistryContext 本地注册中心上下文
     * @param project              项目
     * @param logger               日志记录器
     * @throws Exception 异常
     */
    public static void startRegistryFromAppStart(Executor executor, LocalRegistryContext localRegistryContext, Project project,
                                                 RegistryLogger logger) throws Exception {
        if (localRegistryStarted(localRegistryContext.getRegistry())) {
            localRegistryContext.setStartedByOtherOwner(true);
        } else {
            localRegistryContext.setStartedByOtherOwner(false);
            checkPortInUse(LocalRegistryConstants.NACOS_PORTS);

            LocalRegistryProcessHandler processHandler = new LocalRegistryProcessHandler(localRegistryContext);
            if (executor != null) {
                startRegisterAndDrawConsole(executor, localRegistryContext, project, processHandler, logger);
            }

            waitForRegisterStartResult(localRegistryContext);
            handleRegisterStartResult(localRegistryContext, processHandler);
        }
    }

    /**
     * 从设置页面启动注册中心
     * <p>
     * 从设置中获取当前选择的版本号
     *
     * @param registryContext 注册中心上下文
     * @throws Exception 异常
     */
    public static void startRegistryFromPreferencePage(final LocalRegistryContext registryContext) throws Exception {
        dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState settings =
            dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState.getInstance();
        String version = settings.localNacosVersion != null && !settings.localNacosVersion.isEmpty()
                         ? settings.localNacosVersion : "2.4.3";
        startRegistryFromPreferencePage(registryContext, version);
    }

    /**
     * 从设置页面启动注册中心（支持版本号）
     *
     * @param registryContext 注册中心上下文
     * @param version         Nacos 版本号
     * @throws Exception 异常
     */
    public static void startRegistryFromPreferencePage(final LocalRegistryContext registryContext, final String version) throws Exception {
        if (localRegistryStarted(registryContext.getRegistry())) {
            registryContext.setStartedByOtherOwner(true);
        } else {
            registryContext.setStartedByOtherOwner(false);
            checkPortInUse(LocalRegistryConstants.NACOS_PORTS);
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        Project project =
                            (Project) ((DataContext) DataManager.getInstance().getDataContextFromFocus().getResultSync(3000L)).getData(PlatformDataKeys.PROJECT);
                        registryContext.setProject(project);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    final String target = "Nacos Server " + version;
                    if (!LocalRegistryManager.isRegisterDownloaded(registryContext.getRegistry())) {
                        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                            public void run() {
                                try {
                                    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                                    indicator.setText("Downloading " + target + " (72MB)");
                                    LocalRegistryManager.downloadRegistry(registryContext.getRegistry(), version, indicator,
                                                                          EmptyLogger.instance);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, "Downloading " + target, true, registryContext.getProject());
                    }

                    if (!LocalRegistryManager.isRegisterDownloaded(registryContext.getRegistry())) {
                        throw new RuntimeException("Failed to download " + target);
                    } else {
                        (new Thread(new Runnable() {
                            public void run() {
                                try {
                                    LocalRegistryManager.startRegistryProcess(registryContext, EmptyLogger.getInstance());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        })).start();
                        LocalRegistryManager.waitForRegisterStartResult(registryContext);
                    }
                }
            }, ModalityState.any());
            if (registryContext.getCancel() != null && registryContext.getCancel()) {
                throw new UserCancelException();
            } else if (registryContext.getStartSuccess() != null && !registryContext.getStartSuccess()) {
                throw new RuntimeException("Failed to start local registry");
            }
        }
    }

    /**
     * 停止注册中心
     *
     * @param registry 注册中心类型
     * @throws Exception 异常
     */
    public static void stopRegistry(LocalRegistry registry) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(new String[0]);
        List<String> commands = new LinkedList();
        pb.directory(new File(LocalRegistryConstants.NACOS_BIN_DIR));
        if (RegistryUtils.isWindows()) {
            commands.add("cmd");
            commands.add("/c");
            commands.add("shutdown.cmd");
        } else {
            commands.add("sh");
            commands.add("shutdown.sh");
        }

        pb.command(commands);
        Process shutdownProcess = pb.start();
        shutdownProcess.waitFor();
    }

    /**
     * 检查端口是否被占用
     *
     * @param ports 端口数组
     */
    private static void checkPortInUse(int[] ports) {
        if (RegistryUtils.isWindows()) {
            Set<Integer> portsInuse = PortManager.getPortInUseOnWindowsForTcp();

            for (int port : ports) {
                if (portsInuse.contains(port)) {
                    throw new RuntimeException("Port " + port + " is in used");
                }
            }
        } else {
            for (int port : ports) {
                if (!PortManager.isPortAvailableOnMacForTcp(port)) {
                    throw new RuntimeException("Port " + port + " is in used");
                }
            }
        }
    }

    /**
     * 处理注册中心启动结果
     *
     * @param localRegistryContext 本地注册中心上下文
     * @param processHandler       进程处理器
     * @throws Exception 异常
     */
    private static void handleRegisterStartResult(LocalRegistryContext localRegistryContext, LocalRegistryProcessHandler processHandler) throws Exception {
        if (localRegistryContext.getStartSuccess()) {
            notifySuccessMsg(processHandler);
        } else {
            notifyFailMsg(processHandler);
        }

        if (!localRegistryContext.getStartSuccess()) {
            throw new Exception("Unable to start " + localRegistryContext.getRegistry().name());
        }
    }

    /**
     * 等待注册中心启动结果
     *
     * @param localRegistryContext 本地注册中心上下文
     */
    private static void waitForRegisterStartResult(final LocalRegistryContext localRegistryContext) {
        final Project project = localRegistryContext.getProject();
        final Runnable runnable = new Runnable() {
            public void run() {
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setText("Starting Nacos Server...");
                double cur = (double) 0.0F;
                double step = 0.02;

                for (int i = 0; i < 120; ++i) {
                    if (indicator.isCanceled()) {
                        localRegistryContext.setCancel(true);
                        break;
                    }

                    cur += step;
                    indicator.setFraction(cur);
                    if (localRegistryContext.getStartSuccess() != null) {
                        break;
                    }

                    try {
                        Thread.sleep(1000L);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Use Local Registry", true, project);
        } else {
            final long expiredTime = System.currentTimeMillis() + 120000L;
            Condition expiredCondition = new Condition() {
                public boolean value(Object o) {
                    return System.currentTimeMillis() >= expiredTime;
                }
            };
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run() {
                    ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Use Local Registry", true, project);
                }
            }, ModalityState.any(), expiredCondition);

            for (int i = 0; i < 120 && localRegistryContext.getStartSuccess() == null; ++i) {
                try {
                    Thread.sleep(1000L);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        if (localRegistryContext.getStartSuccess() == null) {
            localRegistryContext.setStartSuccess(false);
        }
    }

    /**
     * 启动注册中心并绘制控制台
     *
     * @param executor       执行器
     * @param connectContext 连接上下文
     * @param project        项目
     * @param processHandler 进程处理器
     * @param logger         日志记录器
     */
    private static void startRegisterAndDrawConsole(final Executor executor, final LocalRegistryContext connectContext,
                                                    final Project project, final LocalRegistryProcessHandler processHandler,
                                                    final RegistryLogger logger) {
        (new Thread(new Runnable() {
            public void run() {
                try {
                    LocalRegistryManager.startRegistryProcess(connectContext, logger);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        })).start();
        if (executor != null) {
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        LocalRegistryManager.drawRegisterConsoleView(executor, project, processHandler, connectContext);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            if (ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any());
            } else {
                ApplicationManager.getApplication().executeOnPooledThread(runnable);
            }
        }
    }

    /**
     * 绘制注册中心控制台视图
     *
     * @param executor             执行器
     * @param project              项目
     * @param processHandler       进程处理器
     * @param localRegistryContext 本地注册中心上下文
     */
    private static void drawRegisterConsoleView(final Executor executor, final Project project,
                                                final LocalRegistryProcessHandler processHandler,
                                                final LocalRegistryContext localRegistryContext) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                ConsoleViewImpl consoleView = new ConsoleViewImpl(project, true);
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(consoleView.getComponent(), "Center");
                DefaultActionGroup toolbarActions = new DefaultActionGroup();
                ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("unknown", toolbarActions, false);
                toolbar.setTargetComponent(consoleView.getComponent());
                panel.add(toolbar.getComponent(), "West");
                RunContentDescriptor runDescriptor = new RunContentDescriptor(consoleView, processHandler, panel, "Local Registry",
                                                                              RunConfigurations.Application);
                AnAction[] consoleActions = consoleView.createConsoleActions();
                toolbarActions.addAll((AnAction[]) Arrays.copyOf(consoleActions, consoleActions.length));
                toolbarActions.add(new StopProcessAction("Stop process", "Stop process", processHandler));
                toolbarActions.add(new CloseAction(executor, runDescriptor, project));
                consoleView.attachToProcess(processHandler);
                ExecutionManager.getInstance(project).getContentManager().showRunContent(executor, runDescriptor);
                localRegistryContext.setConsoleToolbarActions(toolbarActions);
                localRegistryContext.setConsoleActionToolbar(toolbar);
                localRegistryContext.setRunDescriptor(runDescriptor);
            }
        }, ModalityState.any());
    }

    /**
     * 通知启动成功消息
     *
     * @param processHandler 进程处理器
     */
    private static void notifySuccessMsg(LocalRegistryProcessHandler processHandler) {
        processHandler.printInfo("Local registry start successfully.");

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            String hostname = inetAddress.getHostName();
            String ip = inetAddress.getHostAddress();
            processHandler.printInfo(String.format("Your local hostname is %s, ip is %s, others could join this registry by hostname or " +
                                                   "ip.", hostname, ip));
            processHandler.printInfo("Sometimes local ip may change, we recommend using hostname for joining.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 通知启动失败消息
     *
     * @param processHandler 进程处理器
     */
    private static void notifyFailMsg(ProcessHandler processHandler) {
        String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
        processHandler.notifyTextAvailable(date + " Failed to start registry.", ProcessOutputTypes.STDOUT);
    }

    /**
     * 启动注册中心进程
     *
     * @param localRegistryContext 本地注册中心上下文
     * @param logger               日志记录器
     * @throws Exception 异常
     */
    private static void startRegistryProcess(LocalRegistryContext localRegistryContext, RegistryLogger logger) throws Exception {
        if (RegistryUtils.isWindows()) {
            startRegistryProcessOnWindows(localRegistryContext, logger);
        } else {
            startRegistryProcessOnMac(localRegistryContext, logger);
        }
    }

    /**
     * 获取项目 SDK 主目录
     *
     * @param project 项目
     * @return SDK 主目录路径
     */
    public static String getSdkHome(Project project) {
        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (sdk == null) {
            return null;
        } else {
            SdkTypeId sdkType = sdk.getSdkType();
            return sdkType instanceof JavaSdkImpl ? sdk.getHomePath() : null;
        }
    }

    /**
     * 在 Mac 系统上启动注册中心进程
     *
     * @param localRegistryContext 本地注册中心上下文
     * @param logger               日志记录器
     * @throws Exception 异常
     */
    private static void startRegistryProcessOnMac(LocalRegistryContext localRegistryContext, RegistryLogger logger) throws Exception {
        String sdkHome = getSdkHome(localRegistryContext.getProject());
        String targetLog = LocalRegistryConstants.NACOS_START_LOG;
        File logFile = new File(targetLog);
        if (logFile.exists()) {
            logFile.delete();
        }

        ProcessBuilder pb = new ProcessBuilder(new String[0]);
        List<String> commands = new LinkedList();
        pb.directory(new File(LocalRegistryConstants.NACOS_BIN_DIR));
        commands.add("sh");
        commands.add("startup.sh");
        commands.add("-m");
        commands.add("standalone");

        pb.command(commands);
        if (sdkHome != null && !sdkHome.isEmpty()) {
            pb.environment().put("JAVA_HOME", sdkHome);
        }
        applyCustomEnvVariables(pb.environment());

        Process registryProcess = pb.start();
        localRegistryContext.setRegisterProcess(registryProcess);
        waitForRegistryStartOnMac(localRegistryContext, logger);
    }

    /**
     * 在 Windows 系统上启动注册中心进程
     *
     * @param localRegistryContext 本地注册中心上下文
     * @param logger               日志记录器
     * @throws Exception 异常
     */
    private static void startRegistryProcessOnWindows(LocalRegistryContext localRegistryContext, RegistryLogger logger) throws Exception {
        String sdkHome = getSdkHome(localRegistryContext.getProject());
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "startup.cmd", "-m", "standalone");
        pb.directory(new File(LocalRegistryConstants.NACOS_BIN_DIR));
        if (sdkHome != null && !sdkHome.isEmpty()) {
            pb.environment().put("JAVA_HOME", sdkHome);
        }
        applyCustomEnvVariables(pb.environment());
        Process registryProcess = pb.start();
        localRegistryContext.setRegisterProcess(registryProcess);
        waitForRegistryStartOnWindows(localRegistryContext, logger);
    }

    /**
     * 在 Mac 系统上等待注册中心启动
     *
     * @param localRegistryContext 本地注册中心上下文
     * @param logger               日志记录器
     */
    private static void waitForRegistryStartOnMac(LocalRegistryContext localRegistryContext, RegistryLogger logger) {
        BufferedReader reader = null;
        String targetLog = LocalRegistryConstants.NACOS_START_LOG;

        try {
            long end = System.currentTimeMillis() + 120000L;

            while (true) {
                while (true) {
                    if (System.currentTimeMillis() >= end || localRegistryContext.getCancel() != null && localRegistryContext.getCancel()) {
                        localRegistryContext.setStartSuccess(false);
                        return;
                    }

                    Thread.sleep(100L);
                    if (reader != null) {
                        break;
                    }

                    File logFile = new File(targetLog);
                    if (logFile.exists()) {
                        reader = new BufferedReader(new FileReader(logFile));
                        break;
                    }
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                    if (line.contains("started successfully")) {
                        localRegistryContext.setStartSuccess(true);
                        return;
                    }

                    if (line.contains("Failed to initialize component")) {
                        localRegistryContext.setStartSuccess(false);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 在 Windows 系统上等待注册中心启动
     *
     * @param localRegistryContext 本地注册中心上下文
     * @param logger               日志记录器
     * @throws Exception 异常
     */
    private static void waitForRegistryStartOnWindows(LocalRegistryContext localRegistryContext, RegistryLogger logger) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(localRegistryContext.getRegisterProcess().getInputStream()));
        long end = System.currentTimeMillis() + 120000L;

        String line;
        while ((line = reader.readLine()) != null && System.currentTimeMillis() < end) {
            logger.info(line);
            if (line.contains("started successfully")) {
                localRegistryContext.setStartSuccess(true);
                break;
            }
        }

        reader.close();
        if (localRegistryContext.getStartSuccess() == null) {
            localRegistryContext.setStartSuccess(false);
        }
    }

    /**
     * 检查本地注册中心是否已启动
     *
     * @param registry 注册中心类型
     * @return true 如果已启动
     */
    public static boolean localRegistryStarted(LocalRegistry registry) {
        return UrlTestManager.testGetMethod(LocalRegistryConstants.NACOS_TEST_URL);
    }

    /**
     * 删除非当前版本的其他 zip 包
     *
     * @param currentVersion 当前版本号
     * @return 删除的文件数量
     */
    public static int deleteOldVersionZipFiles(String currentVersion) {
        int deletedCount = 0;
        try {
            File pkgDir = new File(LocalRegistryConstants.LOCAL_REGISTRY_PKG_DIR);
            if (!pkgDir.exists() || !pkgDir.isDirectory()) {
                return 0;
            }

            File[] files = pkgDir.listFiles();
            if (files == null) {
                return 0;
            }

            String currentZipName = "nacos-server-" + currentVersion + ".zip";
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("nacos-server-") && file.getName().endsWith(".zip")) {
                    if (!file.getName().equals(currentZipName)) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 静默处理异常
            e.printStackTrace();
        }
        return deletedCount;
    }

    private static void applyCustomEnvVariables(@NotNull Map<String, String> environment) {
        List<SettingsState.EnvVariable> vars = SettingsState.getInstance().localJvmOptions;
        if (vars == null) {
            return;
        }
        for (SettingsState.EnvVariable var : vars) {
            if (var == null) {
                continue;
            }
            String name = var.name != null ? var.name.trim() : "";
            String value = var.value != null ? var.value.trim() : "";
            if (StringUtil.isEmptyOrSpaces(name) || StringUtil.isEmptyOrSpaces(value)) {
                continue;
            }
            environment.put(name, value);
        }
    }
}
