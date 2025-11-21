package dev.dong4j.zeka.stack.idea.plugin.workflow.model;

import org.jetbrains.annotations.NotNull;

/**
 * 工作流类型
 *
 * @author dong4j
 * @version 1.0.0
 */
public enum WorkflowType {
    /** 方法调用工作流 */
    METHOD_CALL_FLOW("方法调用工作流"),
    /** 方法调用链 */
    METHOD_CALLER_CHAIN("方法调用链"),
    /** 类关系链 */
    CLASS_RELATIONSHIP("类关系链");

    private final String displayName;

    WorkflowType(@NotNull String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取显示名称
     *
     * @return 显示名称
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }
}
