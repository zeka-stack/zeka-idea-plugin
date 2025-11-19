package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import org.jetbrains.annotations.NotNull;

/**
 * Nacos 工具窗口工厂
 * 负责创建和初始化 Nacos 工具窗口
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosToolWindowFactory implements ToolWindowFactory {

    /**
     * 工具窗口 ID
     */
    public static final String TOOL_WINDOW_ID = "Nacos";

    /**
     * 创建工具窗口内容
     *
     * @param project    项目实例
     * @param toolWindow 工具窗口实例
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建 Nacos 工具窗口主面板
        NacosToolWindow nacosToolWindow = new NacosToolWindow(project);

        // 创建内容面板
        Content content = ContentFactory.getInstance().createContent(nacosToolWindow.getMainPanel(), "", false);

        // 将内容添加到工具窗口
        toolWindow.getContentManager().addContent(content);

        // 保存工具窗口引用
        nacosToolWindow.setToolWindow(toolWindow);
    }
}