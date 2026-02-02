package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanelProvider;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * Provides the IntelliAI Repairer tab for the Problems tool window.
 */
public class RepairerProblemsViewPanelProvider implements ProblemsViewPanelProvider {
    /** 当前项目实例, 用于访问项目级资源和功能 */
    private final Project project;

    /**
     * 创建 {@code RepairerProblemsViewPanelProvider} 实例并保存 {@code Project} 对象.
     *
     * @param project 当前项目实例, 用于提供后续视图创建所需的项目上下文
     */
    public RepairerProblemsViewPanelProvider(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 创建并返回 IntelliAI 修复器的问题视图标签页
     * <p> 该方法用于在问题工具窗口中初始化并返回一个具体的 {@code RepairerProblemsViewPanel} 实例
     *
     * @return 非 null 的问题视图标签页实例
     */
    @Override
    public @NotNull ProblemsViewTab create() {
        return new RepairerProblemsViewPanel(project);
    }
}
