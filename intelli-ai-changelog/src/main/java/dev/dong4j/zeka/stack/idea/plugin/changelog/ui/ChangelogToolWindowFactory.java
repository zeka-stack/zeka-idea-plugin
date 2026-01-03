package dev.dong4j.zeka.stack.idea.plugin.changelog.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import org.jetbrains.annotations.NotNull;

/**
 * Changelog Tool Window Factory
 *
 * @author dong4j
 * @version hello.world
 * @date 2025-12-31 17:19:21
 * @since hello.world
 */
public class ChangelogToolWindowFactory implements ToolWindowFactory, DumbAware {

    /**
     * 创建工具窗口的内容
     * <p> 此方法用于初始化工具窗口的内容. 由于工具窗口内容由服务动态创建, 因此这里不创建任何内容,
     * 工具窗口默认隐藏, 只在有输出时才显示. anchor 和布局设置将在首次使用时动态完成.
     *
     * @param project    当前项目
     * @param toolWindow 工具窗口实例
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 工具窗口内容由服务动态创建，这里只初始化 History 内容
        ChangelogToolWindowService.getInstance(project).initHistoryContent(toolWindow);
    }

}
