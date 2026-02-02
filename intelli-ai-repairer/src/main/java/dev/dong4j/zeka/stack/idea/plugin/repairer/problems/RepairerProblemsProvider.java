package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.ProblemsProvider;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * Problems provider for IntelliAI Repairer problems.
 *
 * @param project 当前项目的实例
 *                <p> 用于获取与项目相关的各种服务和组件
 */
public record RepairerProblemsProvider(Project project) implements ProblemsProvider {
    /**
     * 构造一个 {@code RepairerProblemsProvider} 实例.
     *
     * @param project 用于初始化的 {@link Project} 对象, 不能为空
     */
    public RepairerProblemsProvider(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 返回当前项目实例
     * <p> 获取与此问题提供者关联的 Project 对象
     *
     * @return 当前项目实例, 不为 null
     */
    @Override
    public @NotNull Project project() {
        return project;
    }

    /**
     * 处理资源释放和清理操作
     * <p> 在对象不再使用时调用此方法进行资源清理
     */
    @Override
    public void dispose() {
        // Nothing to dispose.
    }
}
