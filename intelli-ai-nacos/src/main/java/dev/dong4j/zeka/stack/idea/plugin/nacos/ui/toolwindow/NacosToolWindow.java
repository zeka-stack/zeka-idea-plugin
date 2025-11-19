package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Nacos 工具窗口主面板
 * 整合所有 UI 组件的主容器
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosToolWindow {
    private final Project project;
    private ToolWindow toolWindow;
    private final JPanel mainPanel;
    private final ToolBarPanel toolBarPanel;
    private final TreePanel treePanel;
    private final TabBar tabBar;
    private final JSplitPane splitPane;

    public NacosToolWindow(@NotNull Project project) {
        this.project = project;
        this.toolBarPanel = new ToolBarPanel(project);
        this.treePanel = new TreePanel(project);
        this.tabBar = new TabBar(project);
        this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        this.mainPanel = new JPanel(new BorderLayout());

        initialize();
    }

    private void initialize() {
        // 设置主面板边框
        mainPanel.setBorder(JBUI.Borders.empty(5));

        // 设置分割面板
        splitPane.setLeftComponent(treePanel);
        splitPane.setRightComponent(tabBar);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.3);

        // 添加组件到主面板
        mainPanel.add(toolBarPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
    }

    /**
     * 获取主面板
     *
     * @return 主面板
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * 获取工具窗口
     *
     * @return 工具窗口
     */
    @Nullable
    public ToolWindow getToolWindow() {
        return toolWindow;
    }

    /**
     * 设置工具窗口
     *
     * @param toolWindow 工具窗口
     */
    public void setToolWindow(@NotNull ToolWindow toolWindow) {
        this.toolWindow = toolWindow;
    }

    /**
     * 获取项目实例
     *
     * @return 项目实例
     */
    public Project getProject() {
        return project;
    }

    /**
     * 获取工具栏面板
     *
     * @return 工具栏面板
     */
    public ToolBarPanel getToolBarPanel() {
        return toolBarPanel;
    }

    /**
     * 获取树面板
     *
     * @return 树面板
     */
    public TreePanel getTreePanel() {
        return treePanel;
    }

    /**
     * 获取标签页栏
     *
     * @return 标签页栏
     */
    public TabBar getTabBar() {
        return tabBar;
    }

    /**
     * 刷新工具窗口内容
     */
    public void refresh() {
        // TODO: 实现刷新逻辑
    }

    /**
     * 显示通知消息
     *
     * @param message 消息内容
     * @param type    消息类型 (info, warning, error)
     */
    public void showNotification(String message, String type) {
        // TODO: 实现通知显示逻辑
    }
}