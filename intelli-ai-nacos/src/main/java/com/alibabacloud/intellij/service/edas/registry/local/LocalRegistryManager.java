//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibabacloud.intellij.service.edas.registry.local;

import com.alibabacloud.intellij.UserCancelException;
import com.alibabacloud.intellij.model.edas.LocalRegistry;
import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryConstants;
import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryContext;
import com.alibabacloud.intellij.service.edas.common.UrlTestManager;
import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectLogger;
import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectLogger.EmptyLogger;
import com.alibabacloud.intellij.service.edas.registry.EdasServiceConnectUtils;
import com.alibabacloud.intellij.service.edas.registry.PortManager;
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
import java.util.Set;

import javax.swing.JPanel;

@SuppressWarnings("All")
public class LocalRegistryManager {
    private static final Object downloadLock = new Object();

    public static boolean isRegisterDownloaded(LocalRegistry registry) {
        String registerPath = getRegisterStartupFilePath(registry);
        return checkRegisterExists(registerPath);
    }

    private static boolean checkRegisterExists(String path) {
        File startupFile = new File(path);
        return startupFile.exists();
    }

    public static String getRegisterStartupFilePath(LocalRegistry registry) {
        return EdasServiceConnectUtils.isWindows() ? LocalRegistryConstants.NACOS_START_UP_FILE_WIN :
               LocalRegistryConstants.NACOS_START_UP_FILE_MAC;
    }

    public static String getRegisterPackageFilePath(LocalRegistry registry) {
        return EdasServiceConnectUtils.isWindows() ? LocalRegistryConstants.NACOS_LOCAL_PATH_FOR_WIN :
               LocalRegistryConstants.NACOS_LOCAL_PATH_FOR_MAC;
    }

    public static void downloadRegistry(final LocalRegistry registry, ProgressIndicator indicator, final EdasServiceConnectLogger logger) throws InterruptedException {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (LocalRegistryManager.downloadLock) {
                        if (LocalRegistryManager.isRegisterDownloaded(registry)) {
                            logger.info("Registry is already downloaded before");
                            return;
                        }

                        logger.info("Registry not exists, going to download");
                        LocalRegistryManager.download0(registry, logger);
                        logger.info("Download registry successfully");
                    }
                } catch (Exception e) {
                    logger.info("Failed to download registry: " + e.getMessage());
                }

            }
        });
        t.start();

        for (; t.isAlive() && !indicator.isCanceled(); Thread.sleep(100L)) {
            File file = new File(getRegisterPackageFilePath(registry));
            if (file.exists()) {
                double length = (double) file.length();
                indicator.setFraction(length / (double) 7.2E7F);
            }
        }

        if (t.isAlive()) {
            t.interrupt();
        }

    }

    private static void download0(LocalRegistry registry, EdasServiceConnectLogger logger) throws Exception {
        String startupFile = EdasServiceConnectUtils.isWindows()
                             ? LocalRegistryConstants.NACOS_START_UP_FILE_WIN
                             : LocalRegistryConstants.NACOS_START_UP_FILE_MAC;
        String localPath = EdasServiceConnectUtils.isWindows()
                           ? LocalRegistryConstants.NACOS_LOCAL_PATH_FOR_WIN
                           : LocalRegistryConstants.NACOS_LOCAL_PATH_FOR_MAC;
        if (!checkRegisterExists(startupFile)) {
            EdasServiceConnectUtils.download(LocalRegistryConstants.NACOS_REMOTE_PATH, localPath, logger);
            logger.info("Registry is downloaded to " + localPath);
            if (EdasServiceConnectUtils.isWindows()) {
                EdasServiceConnectUtils.unzip(localPath, LocalRegistryConstants.LOCAL_REGISTRY_DIR);
            } else {
                String command = String.format("unzip %s -d %s", localPath, LocalRegistryConstants.LOCAL_REGISTRY_DIR);
                Process extractProcess = Runtime.getRuntime().exec(command, (String[]) null,
                                                                   new File(LocalRegistryConstants.LOCAL_REGISTRY_PKG_DIR));
                int result = extractProcess.waitFor();
                if (result != 0) {
                    throw new Exception("Unable to extract registry package, the package maybe broken");
                }
            }
            logger.info("Registry is extract to " + LocalRegistryConstants.LOCAL_REGISTRY_DIR);
        }

    }

    public static void startRegistryFromAppStart(Executor executor, LocalRegistryContext localRegistryContext, Project project,
                                                 EdasServiceConnectLogger logger) throws Exception {
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

    public static void startRegistryFromPreferencePage(final LocalRegistryContext registryContext) throws Exception {
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

                    final String target = "Nacos Server";
                    if (!LocalRegistryManager.isRegisterDownloaded(registryContext.getRegistry())) {
                        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                            public void run() {
                                try {
                                    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                                    indicator.setText("Downloading " + target + " (72MB)");
                                    LocalRegistryManager.downloadRegistry(registryContext.getRegistry(), indicator, EmptyLogger.instance);
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

    public static void stopRegistry(LocalRegistry registry) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(new String[0]);
        List<String> commands = new LinkedList();
        pb.directory(new File(LocalRegistryConstants.NACOS_BIN_DIR));
        if (EdasServiceConnectUtils.isWindows()) {
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

    private static void checkPortInUse(int[] ports) {
        if (EdasServiceConnectUtils.isWindows()) {
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

    private static void startRegisterAndDrawConsole(final Executor executor, final LocalRegistryContext connectContext,
                                                    final Project project, final LocalRegistryProcessHandler processHandler,
                                                    final EdasServiceConnectLogger logger) {
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

    private static void notifyFailMsg(ProcessHandler processHandler) {
        String date = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
        processHandler.notifyTextAvailable(date + " Failed to start registry.", ProcessOutputTypes.STDOUT);
    }

    private static void startRegistryProcess(LocalRegistryContext localRegistryContext, EdasServiceConnectLogger logger) throws Exception {
        if (EdasServiceConnectUtils.isWindows()) {
            startRegistryProcessOnWindows(localRegistryContext, logger);
        } else {
            startRegistryProcessOnMac(localRegistryContext, logger);
        }

    }

    public static String getSdkHome(Project project) {
        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        if (sdk == null) {
            return null;
        } else {
            SdkTypeId sdkType = sdk.getSdkType();
            return sdkType instanceof JavaSdkImpl ? sdk.getHomePath() : null;
        }
    }

    private static void startRegistryProcessOnMac(LocalRegistryContext localRegistryContext, EdasServiceConnectLogger logger) throws Exception {
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

        Process registryProcess = pb.start();
        localRegistryContext.setRegisterProcess(registryProcess);
        waitForRegistryStartOnMac(localRegistryContext, logger);
    }

    private static void startRegistryProcessOnWindows(LocalRegistryContext localRegistryContext, EdasServiceConnectLogger logger) throws Exception {
        String sdkHome = getSdkHome(localRegistryContext.getProject());
        String[] envp = null;
        if (sdkHome != null && !sdkHome.isEmpty()) {
            envp = new String[] {"JAVA_HOME=" + sdkHome};
        }

        Process registryProcess = Runtime.getRuntime().exec("cmd /c startup.cmd -m standalone", envp,
                                                            new File(LocalRegistryConstants.NACOS_BIN_DIR));

        localRegistryContext.setRegisterProcess(registryProcess);
        waitForRegistryStartOnWindows(localRegistryContext, logger);
    }

    private static void waitForRegistryStartOnMac(LocalRegistryContext localRegistryContext, EdasServiceConnectLogger logger) {
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

    private static void waitForRegistryStartOnWindows(LocalRegistryContext localRegistryContext, EdasServiceConnectLogger logger) throws Exception {
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

    public static boolean localRegistryStarted(LocalRegistry registry) {
        return UrlTestManager.testGetMethod(LocalRegistryConstants.NACOS_TEST_URL);
    }
}
