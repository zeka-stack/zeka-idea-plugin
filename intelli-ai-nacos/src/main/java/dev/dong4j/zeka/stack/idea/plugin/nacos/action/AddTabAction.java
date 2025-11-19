package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.icons.NacosIcons;
import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow.NacosToolWindow;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * 添加新标签页 Action
 * 用于在工具窗口中添加新的配置编辑标签页
 *
 * @author dong4j
 * @since 1.0.0
 */
public class AddTabAction extends AbstractNacosAction {

    public AddTabAction() {
        super(
            NacosBundle.message("action.nacos.add.tab.title"),
            NacosBundle.message("action.nacos.add.tab.description"),
            NacosIcons.NACOS_16
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        showNacosToolWindow(project);
        NacosToolWindow toolWindow = NacosToolWindow.getInstance(project);
        if (toolWindow != null) {
            toolWindow.createEmptyTab();
        } else {
            NotificationUtil.showWarning(project, NacosBundle.message("error.nacos.not.configured"));
        }
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return super.isAvailable(project);
    }
}