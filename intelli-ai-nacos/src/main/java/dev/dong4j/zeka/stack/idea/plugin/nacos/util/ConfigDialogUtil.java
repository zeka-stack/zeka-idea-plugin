package dev.dong4j.zeka.stack.idea.plugin.nacos.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.nacos.entity.ConfigFile;

/**
 * 配置对话框工具类
 *
 * @author dong4j
 * @since 1.0.0
 */
public final class ConfigDialogUtil {
    private ConfigDialogUtil() {
    }

    /**
     * 弹出对话框让用户确认 namespace/group/dataId
     *
     * @param project    项目
     * @param configFile 默认配置
     * @return 用户确认后的配置, 取消时返回 null
     */
    @Nullable
    public static ConfigFile promptConfig(@NotNull Project project, @NotNull ConfigFile configFile) {
        String namespace = Messages.showInputDialog(
            project,
            NacosBundle.message("dialog.publish.namespace"),
            NacosBundle.message("dialog.publish.title"),
            null,
            configFile.getNamespace(),
            null
                                                   );
        if (namespace == null) {
            return null;
        }

        String group = Messages.showInputDialog(
            project,
            NacosBundle.message("dialog.publish.group"),
            NacosBundle.message("dialog.publish.title"),
            null,
            configFile.getGroup(),
            null
                                               );
        if (group == null) {
            return null;
        }

        String dataId = Messages.showInputDialog(
            project,
            NacosBundle.message("dialog.publish.dataId"),
            NacosBundle.message("dialog.publish.title"),
            null,
            configFile.getDataId(),
            null
                                                );
        if (dataId == null) {
            return null;
        }

        configFile.setNamespace(namespace);
        configFile.setGroup(group);
        configFile.setDataId(dataId);
        return configFile;
    }
}

