package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

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
            null // TODO: 添加刷新图标
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        // TODO: 实现刷新逻辑
        // 1. 刷新配置树
        // 2. 刷新当前标签页内容
        // 3. 更新状态显示

        // 暂时显示通知
        //NotificationUtil.showInfo(project, NacosBundle.message("success.action.executed", "Refresh"));
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return super.isAvailable(project);
    }
}