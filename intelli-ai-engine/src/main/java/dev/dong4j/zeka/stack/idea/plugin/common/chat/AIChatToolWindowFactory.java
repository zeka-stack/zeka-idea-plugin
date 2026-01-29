package dev.dong4j.zeka.stack.idea.plugin.common.chat;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * AI 聊天工具窗口工厂类
 * <p> 实现 IDE 工具窗口的创建和配置, 用于在 IntelliJ 平台中提供 AI 聊天功能界面 </p>
 * <p> 该类负责创建和管理 AI 聊天面板, 并将其集成到 IDE 的工具窗口中 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email mailto:dong4j@gmail.com
 * @date 2026.01.29
 * @since 1.0.0
 */
public final class AIChatToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * 创建工具窗口内容
     * <p> 在指定项目和工具窗口中初始化 AI 聊天面板, 并将其添加到窗口内容管理器中
     *
     * @param project    项目实例, 用于初始化聊天面板
     * @param toolWindow 工具窗口实例, 用于添加内容
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        AIChatPanel panel = new AIChatPanel(project);
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel.getComponent(), "", false);
        content.setDisposer(panel);
        toolWindow.getContentManager().addContent(content);
    }
}
