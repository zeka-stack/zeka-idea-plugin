package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

/**
 * 通过右键菜单发布配置 Action
 * 用于在项目视图中右键发布配置文件到 Nacos
 *
 * @author dong4j
 * @since 1.0.0
 */
public class PublishConfigByMenuAction extends AbstractNacosAction {

    public PublishConfigByMenuAction() {
        super(
            NacosBundle.message("action.nacos.publish.menu.title"),
            NacosBundle.message("action.nacos.publish.menu.description"),
            null // TODO: 添加发布图标
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            //NotificationUtil.showError(project, NacosBundle.message("error.no.file"));
            return;
        }

        // TODO: 实现发布配置逻辑
        // 1. 解析选中的文件
        // 2. 提取配置信息（namespace, group, dataId）
        // 3. 发布到 Nacos

        showNacosToolWindow(project);
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return super.isAvailable(project);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        // 只在选中文件时启用
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile != null) {
            // 检查是否是配置文件
            String fileName = virtualFile.getName();
            boolean isConfigFile = fileName.startsWith("application") &&
                                   (fileName.endsWith(".yml") || fileName.endsWith(".yaml") ||
                                    fileName.endsWith(".properties") || fileName.endsWith(".json"));
            e.getPresentation().setVisible(isConfigFile);
        } else {
            e.getPresentation().setVisible(false);
        }
    }
}