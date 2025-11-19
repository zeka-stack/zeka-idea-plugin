package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

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
            null // TODO: 添加添加图标
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        // TODO: 实现添加标签页逻辑
        // 1. 创建新的空标签页
        // 2. 显示在工具窗口中
        // 3. 聚焦到新标签页

        showNacosToolWindow(project);
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return super.isAvailable(project);
    }
}