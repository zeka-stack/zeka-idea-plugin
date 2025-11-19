//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.alibabacloud.intellij.service.edas.registry.local;

import com.alibabacloud.intellij.model.edas.registry.local.LocalRegistryContext;
import com.alibabacloud.intellij.runner.edas.registry.ServiceConnectJavaProgramPatcher;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;

@SuppressWarnings("All")
public class LocalRegistryProcessHandler extends ProcessHandler {
    protected LocalRegistryContext localRegistryContext;

    public LocalRegistryProcessHandler(LocalRegistryContext localRegistryContext) {
        this.localRegistryContext = localRegistryContext;
        this.startNotify();
    }

    private void printMessage(String message, Key type) {
        if (!this.isProcessTerminating() && !this.isProcessTerminated()) {
            this.notifyTextAvailable(message, type);
        }

    }

    public void printInfo(String message) {
        this.printMessage(String.format("[INFO] %s\n", message), ProcessOutputTypes.STDOUT);
    }

    public void notifyProcessTerminated(int exitCode) {
        super.notifyProcessTerminated(exitCode);
    }

    protected void destroyProcessImpl() {
        System.out.println("Destroy...");
        this.notifyProcessDetached();
    }

    protected void detachProcessImpl() {
        System.out.println("Detach...");
        this.notifyProcessDetached();
    }

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
                        ServiceConnectJavaProgramPatcher.notifyShutdown(LocalRegistryProcessHandler.this.localRegistryContext.getProject().getName(), LocalRegistryProcessHandler.this.localRegistryContext.getConfigName());
                        LocalRegistryProcessHandler.this.localRegistryContext.getConsoleToolbarActions().removeAll();
                        LocalRegistryProcessHandler.this.localRegistryContext.getConsoleActionToolbar().updateActionsImmediately();
                        ExecutionManager.getInstance(LocalRegistryProcessHandler.this.localRegistryContext.getProject()).getContentManager().removeRunContent(LocalRegistryProcessHandler.this.localRegistryContext.getExecutor(), LocalRegistryProcessHandler.this.localRegistryContext.getRunDescriptor());
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

    public @Nullable OutputStream getProcessInput() {
        return null;
    }
}
