package dev.dong4j.zeka.stack.idea.plugin.nacos.process;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

import dev.dong4j.zeka.stack.idea.plugin.nacos.model.LocalRegistryContext;
import dev.dong4j.zeka.stack.idea.plugin.nacos.runner.RegistryJavaProgramPatcher;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.manager.LocalRegistryManager;

/**
 * 本地注册中心进程处理器
 * 负责管理本地 Nacos 注册中心进程的生命周期
 *
 * @author dong4j
 * @since 1.0.0
 */
@SuppressWarnings("All")
public class LocalRegistryProcessHandler extends ProcessHandler {

    protected LocalRegistryContext localRegistryContext;

    public LocalRegistryProcessHandler(LocalRegistryContext localRegistryContext) {
        this.localRegistryContext = localRegistryContext;
        this.startNotify();
    }

    /**
     * 打印消息到控制台
     *
     * @param message 消息内容
     * @param type    输出类型
     */
    private void printMessage(String message, Key type) {
        if (!this.isProcessTerminating() && !this.isProcessTerminated()) {
            this.notifyTextAvailable(message, type);
        }
    }

    /**
     * 打印信息级别消息
     *
     * @param message 消息内容
     */
    public void printInfo(String message) {
        this.printMessage(String.format("[INFO] %s\n", message), ProcessOutputTypes.STDOUT);
    }

    @Override
    public void notifyProcessTerminated(int exitCode) {
        super.notifyProcessTerminated(exitCode);
    }

    @Override
    protected void destroyProcessImpl() {
        System.out.println("Destroy...");
        this.notifyProcessDetached();
    }

    @Override
    protected void detachProcessImpl() {
        System.out.println("Detach...");
        this.notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        System.out.println("DetachIsDefault...");
        if (this.localRegistryContext.getRegisterProcess() != null) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                public void run() {
                    if (!LocalRegistryProcessHandler.this.localRegistryContext.getStartedByOtherOwner()) {
                        try {
                            LocalRegistryManager.stopRegistry(LocalRegistryProcessHandler.this.localRegistryContext.getRegistry());
                            LocalRegistryProcessHandler.this.localRegistryContext.getRegisterProcess().destroy();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        LocalRegistryProcessHandler.this.localRegistryContext.setRegisterProcess((Process) null);
                        RegistryJavaProgramPatcher.notifyShutdown(
                            LocalRegistryProcessHandler.this.localRegistryContext.getProject().getName(),
                            LocalRegistryProcessHandler.this.localRegistryContext.getConfigName()
                                                                 );
                        LocalRegistryProcessHandler.this.localRegistryContext.getConsoleToolbarActions().removeAll();
                        // updateActionsImmediately() 已过时，工具栏会在下次显示时自动更新
                        RunContentManager.getInstance(LocalRegistryProcessHandler.this.localRegistryContext.getProject())
                            .removeRunContent(
                                LocalRegistryProcessHandler.this.localRegistryContext.getExecutor(),
                                LocalRegistryProcessHandler.this.localRegistryContext.getRunDescriptor()
                                             );
                        System.out.println("detach local registry");
                    }
                }
            }, ModalityState.any());
        }

        this.destroyProcess();
        this.detachProcess();
        this.notifyProcessDetached();
        this.notifyProcessTerminated(0);
        return true;
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        return null;
    }
}
