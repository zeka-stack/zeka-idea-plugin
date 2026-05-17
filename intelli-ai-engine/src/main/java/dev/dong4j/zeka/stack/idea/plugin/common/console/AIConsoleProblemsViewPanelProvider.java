package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanelProvider;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

/**
 * IntelliAI Engine 日志面板的 Problems View 适配器.
 * <p>
 * IDEA 2026.1 之后, 引擎日志不再可靠地通过旧的 Problems 工具窗口 contentManager 挂载,
 * 这里改为通过 ProblemsViewPanelProvider 注册一个专用子 tab, 让 Problems View 接管 tab 生命周期.
 */
public final class AIConsoleProblemsViewPanelProvider implements ProblemsViewPanelProvider {

    /**
     * 当前项目上下文.
     * <p>
     * 适配器需要基于项目构造对应的日志视图, 因此保留 project 引用.
     */
    private final Project project;

    /**
     * 创建 Problems View 适配器.
     *
     * @param project 当前项目上下文, 不能为空
     */
    public AIConsoleProblemsViewPanelProvider(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 创建实际展示的 Problems View tab.
     *
     * @return Problems View tab 实例
     */
    @Override
    public @NotNull ProblemsViewTab create() {
        return new AIConsoleProblemsViewPanel(project, AIConsoleView.getInstance(project));
    }
}
