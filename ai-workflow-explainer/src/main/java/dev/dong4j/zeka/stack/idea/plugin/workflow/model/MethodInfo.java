package dev.dong4j.zeka.stack.idea.plugin.workflow.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 方法信息
 *
 * @author dong4j
 * @version 1.0.0
 */
public class MethodInfo {
    /** 方法名 */
    @NotNull
    public String name = "";
    /** 方法签名 */
    @NotNull
    public String signature = "";
    /** 所属类名 */
    @NotNull
    public String className = "";
    /** 完整限定类名 */
    @NotNull
    public String qualifiedClassName = "";
    /** 返回类型 */
    @NotNull
    public String returnType = "";
    /** 参数列表 */
    @NotNull
    public List<ParameterInfo> parameters = new ArrayList<>();
    /** 注解列表 */
    @NotNull
    public List<String> annotations = new ArrayList<>();
    /** 文档注释 */
    @Nullable
    public String docComment;
    /** 方法体摘要（关键步骤） */
    @NotNull
    public List<String> bodySummary = new ArrayList<>();

    /**
     * 参数信息
     */
    public static class ParameterInfo {
        /** 参数名 */
        @NotNull
        public String name = "";
        /** 参数类型 */
        @NotNull
        public String type = "";
        /** 参数描述（从注释中提取） */
        @Nullable
        public String description;
    }
}

