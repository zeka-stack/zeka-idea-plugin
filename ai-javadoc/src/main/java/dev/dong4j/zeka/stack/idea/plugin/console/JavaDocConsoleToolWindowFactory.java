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
 * AI Javadoc Console 工具窗口工厂
 * <p>
 * 负责创建和初始化 Console 工具窗口，在 IDE 底部显示 AI 接口的请求和响应日志。
 *
 * @author dong4j
 * @version 1.0.0
 * @since 1.0.0
 */
public class JavaDocConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JavaDocConsoleView consoleView = JavaDocConsoleView.getInstance(project);
        ConsoleView console = consoleView.initConsole();

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(console.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);

        // 输出欢迎信息
        consoleView.print("======================================================\n");
    }
}
