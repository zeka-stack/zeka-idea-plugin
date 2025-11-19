package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

/**
 * Nacos 帮助 Action
 * 用于打开 Nacos 插件帮助文档
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosHelpAction extends AbstractNacosAction {

    public NacosHelpAction() {
        super(
            NacosBundle.message("action.nacos.help.title"),
            NacosBundle.message("action.nacos.help.description"),
            null // TODO: 添加帮助图标
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        // TODO: 实现帮助文档打开逻辑
        // 1. 打开帮助文档网页
        // 2. 或者显示帮助对话框

        // 暂时显示通知
        //NotificationUtil.showInfo(project, NacosBundle.message("success.action.executed", "Help"));
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return true; // 帮助功能总是可用
    }
}