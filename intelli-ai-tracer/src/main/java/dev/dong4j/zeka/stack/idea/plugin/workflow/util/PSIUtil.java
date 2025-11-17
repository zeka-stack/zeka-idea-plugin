package dev.dong4j.zeka.stack.idea.plugin.workflow.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI 工具类
 *
 * @author dong4j
 * @version 1.0.0
 */
public final class PSIUtil {
    private PSIUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 获取光标位置的 PSI 元素
     *
     * @param project 项目对象
     * @return PSI 元素，如果无法获取则返回 null
     */
    @Nullable
    public static PsiElement getElementAtCaret(@NotNull Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return null;
        }

        PsiFile psiFile = com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile == null) {
            return null;
        }

        return psiFile.findElementAt(editor.getCaretModel().getOffset());
    }

    /**
     * 获取光标位置的方法调用表达式
     *
     * @param project 项目对象
     * @return 方法调用表达式，如果光标不在方法调用上则返回 null
     */
    @Nullable
    public static PsiMethodCallExpression getMethodCallAtCaret(@NotNull Project project) {
        PsiElement elementAtCaret = getElementAtCaret(project);
        if (elementAtCaret == null) {
            return null;
        }

        // 如果当前元素是标识符，尝试获取父元素
        if (elementAtCaret instanceof PsiIdentifier) {
            PsiElement parent = elementAtCaret.getParent();
            if (parent instanceof PsiMethodCallExpression) {
                return (PsiMethodCallExpression) parent;
            }
        }

        // 向上查找方法调用表达式
        return PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethodCallExpression.class);
    }

    /**
     * 根据偏移量获取方法调用表达式
     *
     * @param psiFile PSI 文件
     * @param offset  偏移量
     * @return 方法调用表达式，如果不在方法调用上则返回 null
     */
    @Nullable
    public static PsiMethodCallExpression getMethodCallAtOffset(@NotNull PsiFile psiFile, int offset) {
        PsiElement elementAtOffset = psiFile.findElementAt(offset);
        if (elementAtOffset == null) {
            return null;
        }

        // 如果当前元素是标识符，尝试获取父元素
        if (elementAtOffset instanceof PsiIdentifier) {
            PsiElement parent = elementAtOffset.getParent();
            if (parent instanceof PsiMethodCallExpression) {
                return (PsiMethodCallExpression) parent;
            }
        }

        // 向上查找方法调用表达式
        return PsiTreeUtil.getParentOfType(elementAtOffset, PsiMethodCallExpression.class);
    }

    /**
     * 获取当前所在的方法
     *
     * @param project 项目对象
     * @return 当前方法，如果不在方法中则返回 null
     */
    @Nullable
    public static PsiMethod getCurrentMethod(@NotNull Project project) {
        PsiElement elementAtCaret = getElementAtCaret(project);
        if (elementAtCaret == null) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
    }

    /**
     * 根据偏移量获取方法
     *
     * @param psiFile PSI 文件
     * @param offset  偏移量
     * @return 方法，如果不在方法中则返回 null
     */
    @Nullable
    public static PsiMethod getMethodAtOffset(@NotNull PsiFile psiFile, int offset) {
        PsiElement elementAtOffset = psiFile.findElementAt(offset);
        if (elementAtOffset == null) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(elementAtOffset, PsiMethod.class);
    }

    /**
     * 获取当前所在的类
     *
     * @param project 项目对象
     * @return 当前类，如果不在类中则返回 null
     */
    @Nullable
    public static PsiClass getCurrentClass(@NotNull Project project) {
        PsiElement elementAtCaret = getElementAtCaret(project);
        if (elementAtCaret == null) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(elementAtCaret, PsiClass.class);
    }

    /**
     * 根据偏移量获取类
     *
     * @param psiFile PSI 文件
     * @param offset  偏移量
     * @return 类，如果不在类中则返回 null
     */
    @Nullable
    public static PsiClass getClassAtOffset(@NotNull PsiFile psiFile, int offset) {
        PsiElement elementAtOffset = psiFile.findElementAt(offset);
        if (elementAtOffset == null) {
            return null;
        }
        return PsiTreeUtil.getParentOfType(elementAtOffset, PsiClass.class);
    }

    /**
     * 获取方法的签名字符串
     *
     * @param method 方法
     * @return 方法签名
     */
    @NotNull
    public static String getMethodSignature(@NotNull PsiMethod method) {
        StringBuilder sb = new StringBuilder();

        // 修饰符
        if (method.hasModifierProperty("public")) {
            sb.append("public ");
        } else if (method.hasModifierProperty("protected")) {
            sb.append("protected ");
        } else if (method.hasModifierProperty("private")) {
            sb.append("private ");
        }

        // 返回类型
        PsiType returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(returnType.getPresentableText()).append(" ");
        }

        // 方法名
        sb.append(method.getName());

        // 参数列表
        sb.append("(");
        PsiParameterList parameterList = method.getParameterList();
        PsiParameter[] parameters = parameterList.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            PsiParameter parameter = parameters[i];
            sb.append(parameter.getType().getPresentableText()).append(" ").append(parameter.getName());
        }
        sb.append(")");

        return sb.toString();
    }

    /**
     * 获取注解列表
     *
     * @param owner 拥有注解的元素
     * @return 注解名称列表
     */
    @NotNull
    public static java.util.List<String> getAnnotations(@NotNull PsiModifierListOwner owner) {
        java.util.List<String> annotations = new java.util.ArrayList<>();
        com.intellij.psi.PsiModifierList modifierList = owner.getModifierList();
        if (modifierList != null) {
            com.intellij.psi.PsiAnnotation[] psiAnnotations = modifierList.getAnnotations();
            for (com.intellij.psi.PsiAnnotation annotation : psiAnnotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName != null) {
                    annotations.add("@" + qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1));
                }
            }
        }
        return annotations;
    }

    /**
     * 获取文档注释
     *
     * @param owner 拥有注释的元素
     * @return 文档注释内容，如果没有则返回 null
     */
    @Nullable
    public static String getDocComment(@NotNull PsiModifierListOwner owner) {
        if (owner instanceof PsiDocCommentOwner docOwner) {
            PsiDocComment docComment = docOwner.getDocComment();
            if (docComment != null) {
                return docComment.getText();
            }
        }
        return null;
    }
}

