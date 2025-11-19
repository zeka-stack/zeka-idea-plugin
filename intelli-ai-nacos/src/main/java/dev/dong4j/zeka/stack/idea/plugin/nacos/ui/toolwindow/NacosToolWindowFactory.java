package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;

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
        ContentFactory contentFactory = ContentFactory.getInstance();

        // 创建配置中心面板
        NacosToolWindow configCenterWindow = new NacosToolWindow(project);
        Content configCenterContent = contentFactory.createContent(configCenterWindow.getMainPanel(), "配置中心", false);
        configCenterWindow.setToolWindow(toolWindow);

        // 创建注册中心面板（占位，后续实现）
        JPanel registryCenterPanel = new JPanel(new BorderLayout());
        registryCenterPanel.setBorder(JBUI.Borders.empty(20));
        JLabel placeholderLabel = new JLabel("注册中心功能开发中...", JLabel.CENTER);
        placeholderLabel.setFont(placeholderLabel.getFont().deriveFont(Font.ITALIC));
        registryCenterPanel.add(placeholderLabel, BorderLayout.CENTER);
        Content registryCenterContent = contentFactory.createContent(registryCenterPanel, "注册中心", false);

        // 将内容添加到工具窗口
        toolWindow.getContentManager().addContent(configCenterContent);
        toolWindow.getContentManager().addContent(registryCenterContent);

        // 默认选中配置中心
        toolWindow.getContentManager().setSelectedContent(configCenterContent);
    }
}