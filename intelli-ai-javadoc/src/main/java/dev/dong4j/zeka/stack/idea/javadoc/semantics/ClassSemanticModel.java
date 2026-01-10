package dev.dong4j.zeka.stack.idea.javadoc.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 类语义模型
 * <p>
 * 用于存储通过 PSI 分析得到的类的语义信息，包括架构位置、职责、使用场景等。
 * 该模型作为 PSI 分析和 Prompt 生成之间的中间层，便于扩展和复用。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.8.0
 */
public class ClassSemanticModel {
    /** 类在分层架构中的位置 */
    private String layer;

    /** 类的主要职责和业务领域 */
    private String responsibility;

    /** 类的暴露范围（内部使用 / 对外暴露） */
    private String exposure;

    /** 调用者类型集合（如：REST控制器、定时任务） */
    private Set<String> callerTypes = new LinkedHashSet<>();

    /** 依赖关系集合（如：UserRepository (数据库访问)） */
    private Set<String> dependencies = new LinkedHashSet<>();

    /** 副作用行为集合（如：发布领域事件） */
    private Set<String> sideEffects = new LinkedHashSet<>();

    /** 设计意图集合（如：封装业务规则） */
    private Set<String> designIntents = new LinkedHashSet<>();

    /**
     * 获取类在分层架构中的位置
     *
     * @return 返回类所在的架构层级, 例如 "领域层","基础设施层" 等, 如果未设置则返回 null
     */
    @Nullable
    public String getLayer() {
        return layer;
    }

    /**
     * 设置类在分层架构中的位置
     * <p> 此方法用于更新类在分层架构中的位置信息
     *
     * @param layer 类在分层架构中的位置
     */
    public void setLayer(@Nullable String layer) {
        this.layer = layer;
    }

    /**
     * 获取类的主要职责和业务领域
     * <p> 返回当前类在业务中承担的核心职责和所属的业务领域描述
     *
     * @return 类的主要职责和业务领域描述, 如果未设置则返回 null
     */
    @Nullable
    public String getResponsibility() {
        return responsibility;
    }

    /**
     * 设置类的业务职责
     * <p> 更新类的语义模型中记录的主要职责和业务领域信息
     *
     * @param responsibility 类的业务职责描述, 可以为 null
     */
    public void setResponsibility(@Nullable String responsibility) {
        this.responsibility = responsibility;
    }

    /**
     * 获取类的暴露范围
     * <p> 返回类的暴露范围, 表示该类是内部使用还是对外暴露.
     *
     * @return 类的暴露范围, 可能为 null
     */
    @Nullable
    public String getExposure() {
        return exposure;
    }

    /**
     * 设置类的暴露范围
     * <p> 用于指定该类在系统中的可见性或使用范围, 例如“内部使用”或“对外暴露”
     *
     * @param exposure 类的暴露范围, 可以为 null
     */
    public void setExposure(@Nullable String exposure) {
        this.exposure = exposure;
    }

    /**
     * 获取调用者类型集合
     * <p> 返回该类被哪些类型的调用者使用, 例如:REST 控制器, 定时任务等
     *
     * @return 调用者类型集合, 永远不会为 null
     * @since 2.8.0
     */
    @NotNull
    public Set<String> getCallerTypes() {
        return callerTypes;
    }

    /**
     * 设置调用者类型集合
     * <p> 该方法用于定义哪些类型的组件或模块可以调用此类, 例如 REST 控制器, 定时任务等.
     *
     * @param callerTypes 调用者类型集合, 不能为 null
     * @since 2.8.0
     */
    public void setCallerTypes(@NotNull Set<String> callerTypes) {
        this.callerTypes = callerTypes;
    }

    /**
     * 获取类的依赖关系集合
     * <p> 返回该类所依赖的组件或服务集合, 例如:UserRepository(数据库访问)
     *
     * @return 依赖关系集合, 永远不会为 null
     */
    @NotNull
    public Set<String> getDependencies() {
        return dependencies;
    }

    /**
     * 设置类的依赖关系集合
     * <p> 用于指定该类所依赖的其他组件或服务, 如数据库访问层, 消息队列等.
     * <p> 注意: 传入的集合不能为 null, 否则将抛出 IllegalArgumentException.
     *
     * @param dependencies 依赖关系集合, 不能为空
     */
    public void setDependencies(@NotNull Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * 获取类的副作用行为集合
     * <p> 返回该类在执行过程中可能产生的副作用行为描述, 例如发布领域事件, 修改外部状态等
     *
     * @return 副作用行为集合, 不可为 null
     */
    @NotNull
    public Set<String> getSideEffects() {
        return sideEffects;
    }

    /**
     * 设置类的副作用行为集合
     * <p> 更新类的副作用行为集合, 例如发布领域事件等.
     *
     * @param sideEffects 副作用行为集合, 不能为空
     */
    public void setSideEffects(@NotNull Set<String> sideEffects) {
        this.sideEffects = sideEffects;
    }

    /**
     * 获取设计意图集合
     * <p> 返回该类的设计意图集合, 用于描述类的设计目的和实现方式.
     *
     * @return 设计意图集合, 不能为空
     */
    @NotNull
    public Set<String> getDesignIntents() {
        return designIntents;
    }

    /**
     * 设置类的设计意图集合
     * <p> 用于指定该类在设计时所承载的意图, 如封装业务规则, 实现特定架构模式等
     *
     * @param designIntents 设计意图集合, 不能为 null
     * @since 2.8.0
     */
    public void setDesignIntents(@NotNull Set<String> designIntents) {
        this.designIntents = designIntents;
    }
}
