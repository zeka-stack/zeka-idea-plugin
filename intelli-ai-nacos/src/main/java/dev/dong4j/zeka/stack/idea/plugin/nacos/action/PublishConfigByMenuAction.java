package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.entity.ConfigFile;
import dev.dong4j.zeka.stack.idea.plugin.nacos.icons.NacosIcons;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.ConfigDialogUtil;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

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
            NacosIcons.NACOS_16
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile == null) {
            NotificationUtil.showError(project, NacosBundle.message("error.no.file"));
            return;
        }

        NacosClient client = getNacosClient(project);
        if (client == null) {
            return;
        }

        ConfigFile configFile = ConfigFile.fromFileName(virtualFile.getName(), "public");
        if (configFile == null) {
            NotificationUtil.showWarning(project, NacosBundle.message("error.nacos.not.configured"));
            return;
        }

        byte[] bytes = virtualFile.contentsToByteArray();
        configFile.setContent(new String(bytes, virtualFile.getCharset() != null ? virtualFile.getCharset() : StandardCharsets.UTF_8));
        ConfigFile confirmed = ConfigDialogUtil.promptConfig(project, configFile);
        if (confirmed == null) {
            return;
        }

        boolean success = client.publishConfig(
            confirmed.getNamespace(),
            confirmed.getGroup(),
            confirmed.getDataId(),
            confirmed.getContent(),
            confirmed.getType()
                                              );
        if (success) {
            NotificationUtil.showInfo(
                project,
                NacosBundle.message("notification.publish.success",
                                    confirmed.getDataId(),
                                    confirmed.getNamespace(),
                                    confirmed.getGroup())
                                     );
        } else {
            NotificationUtil.showError(project, NacosBundle.message("error.nacos.connection.failed"));
        }

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