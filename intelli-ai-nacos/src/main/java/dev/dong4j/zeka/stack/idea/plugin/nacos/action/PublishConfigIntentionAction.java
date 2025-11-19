package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

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
        if (project == null || editor == null || psiFile == null) {
            return false;
        }

        // 只在 YAML 文件中可用
        String fileName = psiFile.getName();
        return fileName.startsWith("application") &&
               (fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) {
        // TODO: 实现发布配置逻辑
        // 1. 解析当前文件
        // 2. 提取配置信息
        // 3. 显示发布对话框
        // 4. 发布到 Nacos

        // 暂时显示通知
        //NotificationUtil.showInfo(project, NacosBundle.message("success.action.executed", "Publish Config"));
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}