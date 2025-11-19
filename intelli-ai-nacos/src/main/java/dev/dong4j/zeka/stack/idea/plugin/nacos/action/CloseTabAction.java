package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

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
            null // TODO: 添加关闭图标
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        // TODO: 实现关闭标签页逻辑
        // 1. 获取当前选中的标签页
        // 2. 检查是否有未保存的修改
        // 3. 关闭标签页

        showNacosToolWindow(project);
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return super.isAvailable(project);
    }
}