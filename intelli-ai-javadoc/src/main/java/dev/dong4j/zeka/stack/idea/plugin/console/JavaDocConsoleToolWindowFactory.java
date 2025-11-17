package dev.dong4j.zeka.stack.idea.plugin.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import org.jetbrains.annotations.NotNull;

/**
 * IntelliAI JavaDoc Console 工具窗口工厂
 * <p>
 * 负责创建和初始化 Console 工具窗口，在 IDE 底部显示 AI 接口的请求和响应日志。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class JavaDocConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * 初始化并添加 Java 文档控制台内容到工具窗口
     * <p>
     * 该方法用于创建 Java 文档控制台视图, 并将其内容添加到指定的工具窗口中.
     *
     * @param project    项目对象, 用于获取控制台视图实例
     * @param toolWindow 工具窗口对象, 用于添加控制台内容
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JavaDocConsoleView consoleView = JavaDocConsoleView.getInstance(project);
        ConsoleView console = consoleView.initConsole();

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(console.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);

        // 输出欢迎信息
        printWelcomeMessage(consoleView);
    }

    /**
     * 输出欢迎信息和使用说明
     *
     * @param consoleView 控制台视图实例
     */
    private void printWelcomeMessage(@NotNull JavaDocConsoleView consoleView) {
        consoleView.printWelcomeMessage();
    }
}
