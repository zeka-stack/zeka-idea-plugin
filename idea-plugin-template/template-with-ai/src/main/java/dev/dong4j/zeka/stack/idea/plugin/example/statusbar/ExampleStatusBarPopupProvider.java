package dev.dong4j.zeka.stack.idea.plugin.example.statusbar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIProviderType;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.statusbar.AIStatusBarPopupProvider;
import dev.dong4j.zeka.stack.idea.plugin.common.util.AIProviderUtils;
import dev.dong4j.zeka.stack.idea.plugin.example.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.example.settings.ExampleSettingsConfigurable;
import dev.dong4j.zeka.stack.idea.plugin.example.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.example.util.ExampleBundle;
import icons.AICommonIcons;
import lombok.extern.slf4j.Slf4j;

/**
 * Example Status Bar Popup Provider
 *
 * @author dong4j
 * @date 2026-01-02 03:45:24
 * @version hello.world
 * @since hello.world
 */
@Slf4j
public class ExampleStatusBarPopupProvider implements AIStatusBarPopupProvider {
    private static final Key<Boolean> SELECTED_KEY = Key.create("selected");

    @Override
    public @NotNull String getGroupName() {
        return ExampleBundle.message("statusbar.provider.popup.title");
    }

    @Override
    public @NotNull ActionGroup createActionGroup(@NotNull Project project, @NotNull DataContext context) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (!AIProviderUtils.hasAIProvider(project, PluginContents.PLUGIN_NAME)) {
            group.add(new OpenSettingsAction(project));
            return group;
        }

        List<AIProviderConfig> providers = AIProviderUtils.getProviders();
        group.add(new ProviderSelectionActionGroup(project, providers));
        group.add(Separator.create(ExampleBundle.message("statusbar.quick.settings.title")));
        group.add(new OpenSettingsAction(project));
        return group;
    }

    @NotNull
    private AIProviderType getCurrentProviderType() {
        SettingsState settings = SettingsState.getInstance();
        return settings.providerConfig != null ? settings.providerConfig.providerType : AIProviderType.QIANWEN;
    }

    private void switchDefaultProvider(@NotNull AIProviderType providerType, @NotNull AIProviderConfig config) {
        SettingsState settings = SettingsState.getInstance();
        settings.providerConfig = config;
        AIProviderSettings globalSettings = AIProviderSettings.getInstance();
        globalSettings.updateDefaultProviderConfig(providerType, config);
    }

    private class SwitchProviderAction extends AnAction {
        private final Project project;
        private final AIProviderConfig config;

        SwitchProviderAction(@NotNull Project project, @NotNull AIProviderConfig config) {
            super(config.modelName);
            this.project = project;
            this.config = config;
            if (config.providerType != null) {
                getTemplatePresentation().setIcon(AICommonIcons.getProviderIcon(config.providerType));
            }
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (!AIProviderUtils.hasAIProvider(project, config, PluginContents.PLUGIN_NAME)) {
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                if (project.isDisposed()) {
                    return;
                }
                try {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        AIProviderConfig copy = config.copy();
                        copy.providerType = config.providerType;
                        switchDefaultProvider(config.providerType, copy);
                    });
                } catch (Exception exception) {
                    log.error("切换默认服务商失败", exception);
                }
            }, ModalityState.defaultModalityState());
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            AIProviderType currentType = getCurrentProviderType();
            boolean isSelected = config != null && config.providerType == currentType;
            e.getPresentation().putClientProperty(SELECTED_KEY, isSelected);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }

    private class ProviderSelectionActionGroup extends DefaultActionGroup {
        ProviderSelectionActionGroup(@NotNull Project project, List<AIProviderConfig> providers) {
            super(ExampleBundle.message("statusbar.provider.selection.title"), true);
            if (providers != null) {
                for (AIProviderConfig config : providers) {
                    add(new SwitchProviderAction(project, config));
                }
            }
        }
    }

    private static class OpenSettingsAction extends AnAction {
        private final Project project;

        OpenSettingsAction(@NotNull Project project) {
            super(ExampleBundle.message("statusbar.open.settings"));
            this.project = project;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (project.isDisposed()) {
                return;
            }
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ExampleSettingsConfigurable.class);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.BGT;
        }
    }
}
