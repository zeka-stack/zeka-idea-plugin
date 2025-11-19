package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;

import javax.swing.JPanel;

/**
 * Nacos 工具窗口工具栏面板
 * 提供工具窗口顶部的工具栏功能
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ToolBarPanel extends JPanel {
    private final Project project;

    public ToolBarPanel(@NotNull Project project) {
        this.project = project;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        // 创建动作组
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // TODO: 添加具体的工具栏动作

        // 创建工具栏
        ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("NacosToolbar", actionGroup, true);
        actionToolbar.setTargetComponent(this);

        // 添加工具栏到面板
        add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }
}