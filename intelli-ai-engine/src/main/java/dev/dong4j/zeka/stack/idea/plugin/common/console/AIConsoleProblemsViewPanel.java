package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.analysis.problemsView.toolWindow.ProblemsViewTab;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;

/**
 * IntelliAI Engine 在 Problems View 中展示的日志 tab.
 * <p>
 * Problems View tab 只需要提供一个可展示组件, 不需要继承问题树专用的 {@code ProblemsViewPanel}.
 * 这里直接实现 {@link ProblemsViewTab}, 并复用现有 Console 根面板作为 tab 内容。
 */
public final class AIConsoleProblemsViewPanel extends JBPanel<AIConsoleProblemsViewPanel> implements ProblemsViewTab {

    /**
     * 当前 tab 的显示名.
     */
    public static final String TAB_NAME = AIConsoleView.CONSOLE_TAB_NAME;

    /**
     * 当前 tab 的唯一标识.
     */
    public static final String TAB_ID = AIConsoleView.PROBLEMS_VIEW_TAB_ID;

    /**
     * 日志视图服务实例.
     */
    private final AIConsoleView consoleView;

    /**
     * 构造 Problems View 日志 tab.
     *
     * @param project 当前项目上下文, 不能为空
     * @param consoleView 日志视图服务, 不能为空
     */
    public AIConsoleProblemsViewPanel(@NotNull Project project, @NotNull AIConsoleView consoleView) {
        super(new BorderLayout());
        this.consoleView = consoleView;
        installConsoleComponent();
    }

    /**
     * 安装现有 Console UI 到 Problems View 容器.
     * <p>
     * 这里直接复用原来的 console 根面板, 避免重新实现日志打印、按钮和占位态逻辑。
     * Problems View 只负责承载这个普通组件 tab, 不改动原有 console 的交互行为。
     */
    private void installConsoleComponent() {
        removeAll();
        add(consoleView.getProblemsViewRootComponent(), BorderLayout.CENTER);
    }

    /**
     * 返回 tab 的显示名称.
     *
     * @param tabIndex tab 索引, 当前实现不区分
     * @return tab 名称
     */
    @Override
    public @NotNull String getName(int tabIndex) {
        return TAB_NAME;
    }

    /**
     * 返回 tab 的唯一 ID.
     *
     * @return tab ID
     */
    @Override
    public @NotNull String getTabId() {
        return TAB_ID;
    }
}
