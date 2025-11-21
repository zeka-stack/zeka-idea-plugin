package dev.dong4j.zeka.stack.idea.plugin.workflow.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 类关系上下文
 *
 * @author dong4j
 * @version 1.0.0
 */
public class ClassRelationshipContext {
    /** 项目信息 */
    @NotNull
    public WorkflowContext.ProjectInfo project = new WorkflowContext.ProjectInfo();

    /** 目标类信息 */
    @NotNull
    public ClassInfo targetClass = new ClassInfo();

    /** 继承关系 */
    @NotNull
    public InheritanceInfo inheritance = new InheritanceInfo();

    /** 依赖关系 */
    @NotNull
    public List<ClassDependency> dependencies = new ArrayList<>();

    /** 被依赖关系 */
    @NotNull
    public List<ClassDependency> dependents = new ArrayList<>();

    /** 内部类 */
    @NotNull
    public List<ClassInfo> innerClasses = new ArrayList<>();

    /**
     * 继承关系信息
     */
    public static class InheritanceInfo {
        /** 父类 */
        @Nullable
        public ClassInfo superClass;

        /** 实现的接口 */
        @NotNull
        public List<ClassInfo> interfaces = new ArrayList<>();

        /** 子类 */
        @NotNull
        public List<ClassInfo> subClasses = new ArrayList<>();

        /** 实现类（如果当前是接口） */
        @NotNull
        public List<ClassInfo> implementations = new ArrayList<>();
    }

    /**
     * 类依赖关系
     */
    public static class ClassDependency {
        /** 依赖的类 */
        @NotNull
        public ClassInfo targetClass = new ClassInfo();

        /** 依赖类型 */
        @NotNull
        public DependencyType type;

        /** 依赖位置（字段、方法参数、方法返回值等） */
        @NotNull
        public List<String> locations = new ArrayList<>();

        public ClassDependency() {
            this.type = DependencyType.FIELD;
        }

        public ClassDependency(@NotNull ClassInfo targetClass, @NotNull DependencyType type) {
            this.targetClass = targetClass;
            this.type = type;
        }
    }

    /**
     * 依赖类型
     */
    public enum DependencyType {
        FIELD("字段依赖"),
        METHOD_PARAMETER("方法参数依赖"),
        METHOD_RETURN("方法返回值依赖"),
        METHOD_CALL("方法调用依赖"),
        ANNOTATION("注解依赖");

        private final String displayName;

        DependencyType(@NotNull String displayName) {
            this.displayName = displayName;
        }

        @NotNull
        public String getDisplayName() {
            return displayName;
        }
    }
}
