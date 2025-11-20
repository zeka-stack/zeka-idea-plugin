package dev.dong4j.zeka.stack.idea.plugin.nacos.model;

import com.intellij.execution.Executor;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;

/**
 * 本地注册中心上下文
 * 保存本地注册中心启动和运行过程中的状态信息
 *
 * @author dong4j
 * @since 1.0.0
 */
public class LocalRegistryContext extends RegistryContext {
    
    private volatile Process registerProcess;
    private volatile Project project;
    private Executor executor;
    private volatile RunContentDescriptor runDescriptor;
    private volatile DefaultActionGroup consoleToolbarActions;
    private volatile ActionToolbar consoleActionToolbar;
    private volatile String configName;
    private volatile LocalRegistry registry;
    private volatile String logFile;
    private volatile Boolean startedByOtherOwner;
    private volatile Boolean startSuccess;
    private volatile Boolean cancel;

    @Override
    public void cleanResource() {
        try {
            this.registerProcess.destroyForcibly();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LocalRegistry getRegistry() {
        return this.registry;
    }

    public void setRegistry(LocalRegistry registry) {
        this.registry = registry;
    }

    public String getLogFile() {
        return this.logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public Boolean getCancel() {
        return this.cancel;
    }

    public void setCancel(Boolean cancel) {
        this.cancel = cancel;
    }

    public Boolean getStartedByOtherOwner() {
        return this.startedByOtherOwner;
    }

    public void setStartedByOtherOwner(Boolean startedByOtherOwner) {
        this.startedByOtherOwner = startedByOtherOwner;
    }

    public Boolean getStartSuccess() {
        return this.startSuccess;
    }

    public void setStartSuccess(Boolean startSuccess) {
        this.startSuccess = startSuccess;
    }

    public String getConfigName() {
        return this.configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    public DefaultActionGroup getConsoleToolbarActions() {
        return this.consoleToolbarActions;
    }

    public void setConsoleToolbarActions(DefaultActionGroup consoleToolbarActions) {
        this.consoleToolbarActions = consoleToolbarActions;
    }

    public ActionToolbar getConsoleActionToolbar() {
        return this.consoleActionToolbar;
    }

    public void setConsoleActionToolbar(ActionToolbar consoleActionToolbar) {
        this.consoleActionToolbar = consoleActionToolbar;
    }

    public RunContentDescriptor getRunDescriptor() {
        return this.runDescriptor;
    }

    public void setRunDescriptor(RunContentDescriptor runDescriptor) {
        this.runDescriptor = runDescriptor;
    }

    public Executor getExecutor() {
        return this.executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Process getRegisterProcess() {
        return this.registerProcess;
    }

    public void setRegisterProcess(Process registerProcess) {
        this.registerProcess = registerProcess;
    }

    public Project getProject() {
        return this.project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
