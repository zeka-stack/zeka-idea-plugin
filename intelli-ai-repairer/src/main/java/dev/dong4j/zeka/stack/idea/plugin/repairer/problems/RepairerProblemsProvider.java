package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * 修复者问题提供者类
 * <p>实现 {@link ProblemsProvider} 接口, 用于在 IntelliJ Platform 环境中为修复者 (Repairer) 提供与项目相关的错误或问题列表.
 * 该类封装了对当前项目 (Project) 的引用, 并在生命周期管理中提供 dispose 方法以释放资源.
 * 通常用于插件开发中, 为特定功能模块 (如代码修复, 静态分析) 提供问题数据源.
 *
 * @param project 当前项目的实例, 用于获取与项目相关的各种服务和组件
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.02.02
 * @since 1.0.0
 */
public record RepairerProblemsProvider(Project project) implements ProblemsProvider {
    /**
     * 构造一个 {@code RepairerProblemsProvider} 实例
     *
     * @param project 用于初始化的 {@link Project} 对象, 不能为空
     */
    public RepairerProblemsProvider(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 处理资源释放和清理操作
     * <p> 在对象不再使用时调用此方法进行资源清理
     */
    @Override
    public void dispose() {
        // Nothing to dispose.
    }

    /**
     * 获取当前项目实例
     * <p> 返回构造时传入的 {@link Project} 对象, 用于访问项目相关的服务和组件
     *
     * @return 当前项目实例, 非空
     */
    @Override
    public @NotNull Project project() {
        return project;
    }

    /**
     * 获取当前项目实例
     * <p> 返回构造时传入的 {@link Project} 对象, 用于访问项目相关的服务和组件
     *
     * @return 当前项目实例, 非空
     */
    @Override
    public @NotNull Project getProject() {
        return project;
    }
}
