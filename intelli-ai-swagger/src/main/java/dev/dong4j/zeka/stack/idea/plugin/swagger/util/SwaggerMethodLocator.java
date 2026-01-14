package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Swagger 方法定位工具
 */
public final class SwaggerMethodLocator {

    private SwaggerMethodLocator() {
    }

    @Nullable
    public static PsiMethod findTargetMethod(@NotNull PsiFile psiFile,
                                             @Nullable Editor editor,
                                             @Nullable PsiElement element) {
        if (element instanceof PsiMethod) {
            return (PsiMethod) element;
        }

        PsiElement target = element;
        if (target == null && editor != null) {
            int offset = editor.getCaretModel().getOffset();
            target = psiFile.findElementAt(offset);
        }

        if (target == null) {
            return null;
        }

        return PsiTreeUtil.getParentOfType(target, PsiMethod.class, false);
    }
}
