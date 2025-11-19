package dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import dev.dong4j.zeka.stack.idea.plugin.nacos.action.AddTabAction;
import dev.dong4j.zeka.stack.idea.plugin.nacos.action.CloseTabAction;
import dev.dong4j.zeka.stack.idea.plugin.nacos.action.NacosHelpAction;
import dev.dong4j.zeka.stack.idea.plugin.nacos.action.RefreshAction;
import dev.dong4j.zeka.stack.idea.plugin.nacos.action.SettingAction;

/**
 * Nacos 工具窗口工具栏面板
 * 提供工具窗口顶部的工具栏功能
 *
 * @author dong4j
 * @since 1.0.0
 */
public class ToolBarPanel extends JPanel {
    private final Project project;
    private final DefaultActionGroup actionGroup;

    public ToolBarPanel(@NotNull Project project) {
        this.project = project;
        this.actionGroup = new DefaultActionGroup();
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        // 创建工具栏
        ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("NacosToolbar", actionGroup, true);
        actionToolbar.setTargetComponent(this);

        // 添加工具栏到面板
        add(actionToolbar.getComponent(), BorderLayout.CENTER);
    }

    public void bindActions(@NotNull NacosToolWindow toolWindow) {
        actionGroup.removeAll();
        actionGroup.add(new RefreshAction());
        actionGroup.add(new AddTabAction());
        actionGroup.add(new CloseTabAction());
        actionGroup.add(new SettingAction());
        actionGroup.add(new NacosHelpAction());
    }
}