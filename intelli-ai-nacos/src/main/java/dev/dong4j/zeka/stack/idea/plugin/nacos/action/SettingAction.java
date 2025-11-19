package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.icons.NacosIcons;
import dev.dong4j.zeka.stack.idea.plugin.nacos.settings.NacosSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

/**
 * 打开设置 Action
 * 用于打开 Nacos 插件设置页面
 *
 * @author dong4j
 * @since 1.0.0
 */
public class SettingAction extends AbstractNacosAction {

    public SettingAction() {
        super(
            NacosBundle.message("action.nacos.settings.title"),
            NacosBundle.message("action.nacos.settings.description"),
            NacosIcons.NACOS_16
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        // 打开 Nacos 设置页面
        ShowSettingsUtil.getInstance().showSettingsDialog(project, NacosSettingsConfigurable.class);
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return true; // 设置页面总是可用
    }
}