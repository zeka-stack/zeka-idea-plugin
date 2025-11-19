package dev.dong4j.zeka.stack.idea.plugin.nacos.action;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.nacos.util.NacosBundle;

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
        return project != null && editor != null && psiFile != null;

        // 在任何文件中都可用，但主要用于配置文件
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) {
        // TODO: 实现对比配置逻辑
        // 1. 获取当前编辑器中的配置信息
        // 2. 从 Nacos 拉取远程配置
        // 3. 使用 IntelliJ Diff 工具显示差异

        // 暂时显示通知
        //NotificationUtil.showInfo(project, NacosBundle.message("success.action.executed", "Compare Config"));
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}