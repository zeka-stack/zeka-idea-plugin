package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.nacos.ui.toolwindow.NacosToolWindowFactory;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * Nacos Action 抽象基类
 * 提供通用的 Action 功能和工具方法
 *
 * @author dong4j
 * @since 1.0.0
 */
public abstract class AbstractNacosAction extends AnAction {

    public AbstractNacosAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }

    public AbstractNacosAction() {
        super();
    }

    /**
     * 操作执行方法
     *
     * @param e Action 事件
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            NotificationUtil.showError(null, NacosBundle.message("error.no.project"));
            return;
        }

        try {
            actionPerformed(e, project);
        } catch (Exception ex) {
            NotificationUtil.showError(project, NacosBundle.message("error.general", ex.getMessage()));
            ex.printStackTrace();
        }
    }

    /**
     * 子类需要实现的具体操作方法
     *
     * @param e       Action 事件
     * @param project 项目实例
     * @throws Exception 异常
     */
    protected abstract void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception;

    /**
     * 更新 Action 状态
     *
     * @param e Action 事件
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null && isAvailable(project));
        // 确保图标和文本都正确显示
        Icon icon = getTemplatePresentation().getIcon();
        if (icon != null) {
            e.getPresentation().setIcon(icon);
        }
        String text = getTemplatePresentation().getText();
        if (text != null && !text.isEmpty()) {
            e.getPresentation().setText(text);
        }
    }

    /**
     * 检查 Action 是否可用
     *
     * @param project 项目实例
     * @return 是否可用
     */
    protected boolean isAvailable(@NotNull Project project) {
        // 默认实现：检查 Nacos 配置是否完整
        SettingsState settings = SettingsState.getInstance();
        return settings.serverAddr != null && !settings.serverAddr.isEmpty();
    }

    /**
     * 获取默认的 Nacos 客户端
     *
     * @param project 项目实例
     * @return Nacos 客户端，如果配置不完整则返回 null
     */
    @Nullable
    protected NacosClient getNacosClient(@NotNull Project project) {
        NacosClient client = NacosClientUtils.getDefaultClient();
        if (client == null) {
            NotificationUtil.showError(project, "Nacos client not configured properly");
            return null;
        }
        return client;
    }

    /**
     * 显示 Nacos 工具窗口
     *
     * @param project 项目实例
     */
    protected void showNacosToolWindow(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(NacosToolWindowFactory.TOOL_WINDOW_ID);
        if (toolWindow != null) {
            toolWindow.show();
        }
    }

    /**
     * 获取当前项目文件
     *
     * @param e Action 事件
     * @return 当前文件路径，如果不存在则返回 null
     */
    @Nullable
    protected String getCurrentFilePath(@NotNull AnActionEvent e) {
        com.intellij.openapi.vfs.VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        return virtualFile != null ? virtualFile.getPath() : null;
    }

    /**
     * 获取当前项目目录
     *
     * @param e Action 事件
     * @return 当前目录路径，如果不存在则返回 null
     */
    @Nullable
    protected String getCurrentDirectoryPath(@NotNull AnActionEvent e) {
        com.intellij.openapi.vfs.VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile != null) {
            com.intellij.openapi.vfs.VirtualFile parent = virtualFile.getParent();
            return parent != null ? parent.getPath() : null;
        }
        return null;
    }
}