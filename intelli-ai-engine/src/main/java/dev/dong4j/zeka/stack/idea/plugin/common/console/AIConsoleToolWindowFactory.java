package dev.dong4j.zeka.stack.idea.plugin.common.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import org.jetbrains.annotations.NotNull;

/**
 * AI 控制台工具窗口工厂类
 * <p>
 * 该类实现了 ToolWindowFactory 和 DumbAware 接口, 用于创建和初始化 AI 控制台工具窗口.
 * 负责在 IDE 中创建专门用于显示所有基于 IntelliAI Engine 的插件的日志输出的控制台窗口.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 1.0.0
 */
public class AIConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * 初始化并添加控制台内容到工具窗口
     * <p>
     * 该方法用于创建控制台视图, 并将其内容添加到指定的工具窗口中.
     *
     * @param project    项目对象, 用于获取控制台视图实例
     * @param toolWindow 工具窗口对象, 用于添加控制台内容
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        AIConsoleView consoleView = AIConsoleView.getInstance(project);
        ConsoleView console = consoleView.initConsole();

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(console.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

