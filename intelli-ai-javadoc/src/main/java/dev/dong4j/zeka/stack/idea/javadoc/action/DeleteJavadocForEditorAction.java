package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.javadoc.util.PsiElementLocator;
import lombok.extern.slf4j.Slf4j;

/**
 * 删除 Javadoc 编辑器动作类
 * <p>
 * 该类用于在编辑器中删除当前光标位置的元素的 Javadoc 注释.
 * 支持通过右键菜单触发.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.6.0
 */
@Slf4j
public class DeleteJavadocForEditorAction extends AbstractDeleteJavadocAction {

    /**
     * 处理动作事件
     *
     * @param e 动作事件对象
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project == null || project.isDisposed() || editor == null || psiFile == null) {
            return;
        }

        // 检查配置是否允许删除
        SettingsState settings = SettingsState.getInstance();
        if (!settings.allowDeleteJavadoc) {
            NotificationUtil.showInfo(project, JavadocBundle.message("notification.delete.javadoc.not.enabled"));
            return;
        }

        // 检查文件类型
        if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
            return;
        }

        // 检查是否支持 Kotlin
        if (psiFile instanceof KtFile) {
            if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
                return;
            }
        }

        // 定位元素
        PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElementAtOffset(
            psiFile, editor.getCaretModel().getOffset());

        if (locateResult == null) {
            NotificationUtil.showInfo(project, JavadocBundle.message("notification.no.element.to.delete"));
            return;
        }

        PsiElement element = locateResult.element();

        // 删除注释
        boolean deleted = deletionService.deleteJavadoc(project, element);
        if (deleted) {
            NotificationUtil.showInfo(project, JavadocBundle.message("notification.delete.javadoc.success"));
        } else {
            NotificationUtil.showInfo(project, JavadocBundle.message("notification.no.javadoc.to.delete"));
        }
    }

    /**
     * 更新操作的呈现信息
     *
     * @param e 事件对象
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 检查项目状态
        if (project == null || project.isDisposed()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // 检查配置是否允许删除
        SettingsState settings = SettingsState.getInstance();
        boolean enabled = settings.allowDeleteJavadoc && editor != null && psiFile != null;

        // 检查文件类型
        if (enabled) {
            boolean isSupportedFile = psiFile instanceof PsiJavaFile || psiFile instanceof KtFile;
            if (psiFile instanceof KtFile) {
                isSupportedFile = settings.isLanguageSupported(PluginContents.KOTLIN);
            }
            enabled = enabled && isSupportedFile;
        }

        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(enabled);
        e.getPresentation().setText(JavadocBundle.message("action.delete.javadoc"));
        e.getPresentation().setDescription(JavadocBundle.message("action.delete.javadoc.description"));
    }

    /**
     * 获取用于更新操作的线程类型
     *
     * @return ActionUpdateThread.BGT 后台线程
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}

