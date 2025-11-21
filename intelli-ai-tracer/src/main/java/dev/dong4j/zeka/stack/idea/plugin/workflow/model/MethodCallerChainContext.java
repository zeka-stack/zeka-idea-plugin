package dev.dong4j.zeka.stack.idea.plugin.workflow.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 方法调用链上下文
 *
 * @author dong4j
 * @version 1.0.0
 */
public class MethodCallerChainContext {
    /** 项目信息 */
    @NotNull
    public WorkflowContext.ProjectInfo project = new WorkflowContext.ProjectInfo();

    /** 目标方法信息 */
    @NotNull
    public MethodInfo targetMethod = new MethodInfo();

    /** 直接调用者列表 */
    @NotNull
    public List<MethodInfo> directCallers = new ArrayList<>();

    /** 间接调用者列表（调用链） */
    @NotNull
    public List<CallerChain> callerChains = new ArrayList<>();

    /** 被调用者列表（该方法调用了哪些方法） */
    @NotNull
    public List<MethodInfo> callees = new ArrayList<>();

    /**
     * 调用链
     */
    public static class CallerChain {
        /** 调用链路径 */
        @NotNull
        public List<MethodInfo> chain = new ArrayList<>();

        /** 调用深度 */
        public int depth;

        public CallerChain() {
        }

        public CallerChain(@NotNull List<MethodInfo> chain, int depth) {
            this.chain = new ArrayList<>(chain);
            this.depth = depth;
        }
    }
}
