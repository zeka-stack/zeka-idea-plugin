package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.icons.NacosIcons;
import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow.NacosToolWindow;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

/**
 * 关闭标签页 Action
 * 用于关闭当前选中的配置编辑标签页
 *
 * @author dong4j
 * @since 1.0.0
 */
public class CloseTabAction extends AbstractNacosAction {

    public CloseTabAction() {
        super(
            NacosBundle.message("action.nacos.close.tab.title"),
            NacosBundle.message("action.nacos.close.tab.description"),
            NacosIcons.NACOS_16
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        NacosToolWindow toolWindow = NacosToolWindow.getInstance(project);
        if (toolWindow != null) {
            toolWindow.closeCurrentTab();
        }
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return super.isAvailable(project);
    }
}