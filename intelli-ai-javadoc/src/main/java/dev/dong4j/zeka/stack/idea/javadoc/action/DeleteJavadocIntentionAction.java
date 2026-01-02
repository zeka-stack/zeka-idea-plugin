package dev.dong4j.zeka.stack.idea.javadoc.action;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.IncorrectOperationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;

import javax.swing.Icon;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.service.JavadocDeletionService;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.javadoc.util.PsiElementLocator;
import icons.AIJicons;
import lombok.extern.slf4j.Slf4j;

/**
 * 删除 Javadoc 意图动作类
 * <p>
 * 该类实现了 IDEA 插件中的意图动作功能, 用于删除 Java/Kotlin 代码元素的 Javadoc/KDoc 注释.
 * 继承自 PsiElementBaseIntentionAction 并实现 Iconable 接口.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.6.0
 */
@Slf4j
public class DeleteJavadocIntentionAction extends PsiElementBaseIntentionAction implements Iconable {

    /**
     * 删除 Javadoc 服务实例
     * <p>
     * 用于执行删除 Java/Kotlin 代码元素中 Javadoc/KDoc 注释的操作
     *
     * @see JavadocDeletionService
     */
    private final JavadocDeletionService deletionService = new JavadocDeletionService();

    /**
     * 获取删除 Javadoc 的文本内容
     *
     * @return 删除 Javadoc 的文本内容
     */
    @NotNull
    @Override
    public String getText() {
        return JavadocBundle.message("action.delete.javadoc");
    }

    /**
     * 意图在 'description.html' 旁必须有 'before.*.template' 和 'after.*.template'
     * 获取插件的家族名称
     *
     * @return 插件家族名称
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return PluginContents.PLUGIN_NAME;
    }

    /**
     * 获取图标
     *
     * @param flags 图标标志
     * @return 图标对象
     */
    @Override
    public Icon getIcon(int flags) {
        return AIJicons.AIJ_16;
    }

    /**
     * 判断当前意图操作是否可用
     *
     * @param project 当前项目
     * @param editor  当前编辑器实例
     * @param element 当前光标所在的 PSI 元素
     * @return 如果可用返回 true
     */
    @SuppressWarnings("D")
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        // 如果处于预览模式，则直接返回 false
        if (IntentionPreviewUtils.isPreviewElement(element)) {
            return false;
        }

        // 检查配置是否允许删除
        SettingsState settings = SettingsState.getInstance();
        if (!settings.allowDeleteJavadoc) {
            return false;
        }

        PsiFile file = element.getContainingFile();

        // 1. 必须是 Java 或 Kotlin 文件
        if (!(file instanceof PsiJavaFile) && !(file instanceof KtFile)) {
            return false;
        }

        // 检查是否支持 Kotlin
        if (file instanceof KtFile) {
            if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
                return false;
            }
        }

        // 2. 定位元素
        PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElementAtOffset(
            file, editor.getCaretModel().getOffset());

        if (locateResult == null) {
            return false;
        }

        // 3. 如果是整个文件，不在 Intention 中显示
        if (locateResult.type() == PsiElementLocator.LocateType.FILE) {
            return false;
        }

        // 4. 检查元素是否有文档注释
        PsiElement locatedElement = locateResult.element();

        // 检查 Java 元素的文档注释
        if (locatedElement instanceof PsiDocCommentOwner docOwner) {
            return docOwner.getDocComment() != null;
        }

        // 检查 Kotlin 元素的 KDoc
        if (locatedElement instanceof KtClassOrObject ktClass) {
            return ktClass.getDocComment() != null;
        }
        if (locatedElement instanceof KtNamedFunction ktFunction) {
            return ktFunction.getDocComment() != null;
        }
        if (locatedElement instanceof KtProperty ktProperty) {
            return ktProperty.getDocComment() != null;
        }

        return false;
    }

    /**
     * 执行指定的意图操作, 用于删除当前元素的 Javadoc/KDoc 注释
     *
     * @param project 项目对象
     * @param editor  编辑器对象
     * @param element 要处理的 PsiElement 对象
     * @throws IncorrectOperationException 如果执行删除操作时发生错误
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element)
        throws IncorrectOperationException {

        // 如果处于预览模式，则直接返回
        if (IntentionPreviewUtils.isPreviewElement(element)) {
            return;
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
            return;
        }

        // 定位元素
        PsiElementLocator.LocateResult locateResult = PsiElementLocator.locateElementAtOffset(
            file, editor.getCaretModel().getOffset());

        if (locateResult == null) {
            return;
        }

        PsiElement locatedElement = locateResult.element();

        // 删除注释
        boolean deleted = deletionService.deleteJavadoc(project, locatedElement);
        if (deleted) {
            NotificationUtil.showInfo(project, JavadocBundle.message("notification.delete.javadoc.success"));
        } else {
            NotificationUtil.showInfo(project, JavadocBundle.message("notification.no.javadoc.to.delete"));
        }
    }
}

