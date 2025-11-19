package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * Nacos 帮助 Action
 * 用于打开 Nacos 插件帮助文档
 *
 * @author dong4j
 * @since 1.0.0
 */
public class NacosHelpAction extends AbstractNacosAction {

    private static final String HELP_URL = "https://nacos.io/en-us/docs/open-api.html";

    public NacosHelpAction() {
        super(
            NacosBundle.message("action.nacos.help.title"),
            NacosBundle.message("action.nacos.help.description"),
            AllIcons.Actions.Help
             );
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) throws Exception {
        BrowserUtil.browse(HELP_URL);
        NotificationUtil.showInfo(project, NacosBundle.message("success.action.executed", "Help"));
    }

    @Override
    protected boolean isAvailable(@NotNull Project project) {
        return true; // 帮助功能总是可用
    }
}