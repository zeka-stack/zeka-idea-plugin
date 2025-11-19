package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.icons.NacosIcons;
import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow.NacosToolWindow;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * 刷新 Nacos 配置 Action
 * 用于刷新工具窗口中的配置树和标签页
 *
 * @author dong4j
 * @since 1.0.0
 */
public class RefreshAction extends AbstractNacosAction {

    public RefreshAction() {
        super(
            NacosBundle.message("action.nacos.refresh.title"),
            NacosBundle.message("action.nacos.refresh.description"),
            NacosIcons.NACOS_16
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        NacosToolWindow toolWindow = NacosToolWindow.getInstance(project);
        if (toolWindow == null) {
            showNacosToolWindow(project);
            toolWindow = NacosToolWindow.getInstance(project);
        }
        if (toolWindow != null) {
            toolWindow.refresh();
            NotificationUtil.showInfo(project, NacosBundle.message("success.action.executed", "Refresh"));
        }
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return super.isAvailable(project);
    }
}