package dev.dong4j.zeka.stack.idea.plugin.workflow.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流上下文
 *
 * @author dong4j
 * @version 1.0.0
 */
public class WorkflowContext {
    /** 项目信息 */
    @NotNull
    public ProjectInfo project = new ProjectInfo();
    /** 当前类信息 */
    @NotNull
    public ClassInfo currentClass = new ClassInfo();
    /** 当前方法信息 */
    @NotNull
    public MethodInfo currentMethod = new MethodInfo();
    /** 调用者列表（谁调用了当前方法） */
    @NotNull
    public List<MethodInfo> callers = new ArrayList<>();
    /** 被调用者列表（当前方法调用了哪些方法） */
    @NotNull
    public List<MethodInfo> callees = new ArrayList<>();

    /**
     * 项目信息
     */
    public static class ProjectInfo {
        /** 项目名 */
        @NotNull
        public String name = "";
        /** 包路径 */
        @Nullable
        public String packagePath;
    }
}

