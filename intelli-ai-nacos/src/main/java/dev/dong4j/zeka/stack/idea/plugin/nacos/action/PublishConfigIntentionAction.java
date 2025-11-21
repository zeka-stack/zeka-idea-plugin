package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.entity.ConfigFile;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.ConfigDialogUtil;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * 发布配置意图 Action
 * 用于在 YAML 文件中通过意图操作发布配置到 Nacos
 *
 * @author dong4j
 * @since 1.0.0
 */
public class PublishConfigIntentionAction implements IntentionAction {

    @Override
    public @NotNull String getText() {
        return NacosBundle.message("intention.nacos.publish.config.text");
    }

    @Override
    public @NotNull String getFamilyName() {
        return NacosBundle.message("intention.nacos.publish.config.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        if (editor == null || psiFile == null) {
            return false;
        }

        // 只在 YAML 文件中可用
        String fileName = psiFile.getName();
        return fileName.startsWith("application") &&
               (fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) {
        NacosClient client = NacosClientUtils.getDefaultClient();
        if (client == null) {
            NotificationUtil.showError(project, NacosBundle.message("error.nacos.not.configured"));
            return;
        }

        ConfigFile configFile = ConfigFile.fromFileName(psiFile.getName(), "public");
        if (configFile == null) {
            NotificationUtil.showWarning(project, NacosBundle.message("error.no.file"));
            return;
        }
        configFile.setContent(psiFile.getText());

        ConfigFile confirmed = ConfigDialogUtil.promptConfig(project, configFile);
        if (confirmed == null) {
            return;
        }

        try {
            boolean success = client.publishConfig(
                confirmed.getNamespace(),
                confirmed.getGroup(),
                confirmed.getDataId(),
                confirmed.getContent(),
                confirmed.getType()
                                                  );
            if (success) {
                NotificationUtil.showInfo(project,
                                          NacosBundle.message("notification.publish.success",
                                                              confirmed.getDataId(),
                                                              confirmed.getNamespace(),
                                                              confirmed.getGroup()));
            } else {
                NotificationUtil.showError(project, NacosBundle.message("error.nacos.connection.failed"));
            }
        } catch (Exception ex) {
            NotificationUtil.showError(project, NacosBundle.message("error.general", ex.getMessage()));
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}