package dev.dong4j.zeka.stack.idea.plugin.workflow.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * 工作流结果工具窗口工厂
 * <p>
 * 负责创建和初始化工作流结果展示工具窗口，在 IDE 右侧显示 AI 生成的工作流说明。
 *
 * @author dong4j
 * @version 1.0.0
 */
public class WorkflowResultToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * 创建工具窗口内容
     * <p>
     * 该方法用于创建工作流结果视图，并将其内容添加到指定的工具窗口中。
     *
     * @param project    项目对象，用于获取工具窗口视图实例
     * @param toolWindow 工具窗口对象，用于添加视图内容
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        WorkflowResultToolWindow workflowView = WorkflowResultToolWindow.getInstance(project);
        JComponent content = workflowView.getContent();

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content windowContent = contentFactory.createContent(content, "", false);
        toolWindow.getContentManager().addContent(windowContent);
    }
}

