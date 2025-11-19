package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClient;
import dev.dong4j.zeka.stack.idea.plugin.nacos.client.NacosClientUtils;
import dev.dong4j.zeka.stack.idea.plugin.nacos.entity.ConfigFile;
import dev.dong4j.zeka.stack.idea.plugin.nacos.service.CompareConfigService;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.ConfigDialogUtil;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;
import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NotificationUtil;

/**
 * 对比配置意图 Action
 * 用于在配置编辑器中通过意图操作对比本地和远程配置
 *
 * @author dong4j
 * @since 1.0.0
 */
public class CompareConfigIntentionAction implements IntentionAction {

    @Override
    public @NotNull String getText() {
        return NacosBundle.message("intention.nacos.compare.config.text");
    }

    @Override
    public @NotNull String getFamilyName() {
        return NacosBundle.message("intention.nacos.compare.config.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
        if (project == null || editor == null || psiFile == null) {
            return false;
        }
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
        ConfigFile confirmed = ConfigDialogUtil.promptConfig(project, configFile);
        if (confirmed == null) {
            return;
        }

        try {
            String remote = client.getConfig(confirmed.getNamespace(), confirmed.getGroup(), confirmed.getDataId());
            CompareConfigService.getInstance(project).compareConfigurations(
                project,
                psiFile.getText(),
                remote != null ? remote : "",
                confirmed.getDataId()
                                                                           );
        } catch (Exception ex) {
            NotificationUtil.showError(project, NacosBundle.message("error.general", ex.getMessage()));
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}