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
import com.intellij.psi.PsiReferenceExpression;
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
     * 检测光标位置的元素类型
     */
    public enum ElementType {
        METHOD_CALL("方法调用"),     // 方法调用
        METHOD_DEFINITION("方法定义"), // 方法定义
        CLASS_DEFINITION("类定义"),  // 类定义
        UNKNOWN("未知类型");           // 未知类型

        private final String displayName;

        ElementType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
         * 检测光标位置的元素类型和对应的 PSI 元素
         */
        public record ElementContext(@NotNull ElementType type, @Nullable PsiElement element) {
    }

    /**
     * 检测光标位置的元素类型
     *
     * @param psiFile PSI 文件
     * @param offset  偏移量
     * @return 元素上下文信息
     */
    @NotNull
    public static ElementContext detectElementType(@NotNull PsiFile psiFile, int offset) {
        PsiElement elementAtOffset = psiFile.findElementAt(offset);
        if (elementAtOffset == null) {
            return new ElementContext(ElementType.UNKNOWN, null);
        }

        // 1. 检测方法调用
        PsiMethodCallExpression methodCall = PsiTreeUtil.getParentOfType(elementAtOffset, PsiMethodCallExpression.class);
        if (methodCall != null) {
            // 进一步检查光标是否在方法调用的标识符上
            PsiElement methodIdentifier = getMethodCallIdentifier(methodCall);
            if (methodIdentifier != null && isElementInRange(elementAtOffset, methodIdentifier)) {
                return new ElementContext(ElementType.METHOD_CALL, methodCall);
            }
        }

        // 2. 检测方法定义
        PsiMethod method = PsiTreeUtil.getParentOfType(elementAtOffset, PsiMethod.class);
        if (method != null) {
            // 检查光标是否在方法名上
            PsiIdentifier methodNameIdentifier = method.getNameIdentifier();
            if (methodNameIdentifier != null && isElementInRange(elementAtOffset, methodNameIdentifier)) {
                return new ElementContext(ElementType.METHOD_DEFINITION, method);
            }
        }

        // 3. 检测类定义
        PsiClass psiClass = PsiTreeUtil.getParentOfType(elementAtOffset, PsiClass.class);
        if (psiClass != null) {
            // 检查光标是否在类名上
            PsiIdentifier classNameIdentifier = psiClass.getNameIdentifier();
            if (classNameIdentifier != null && isElementInRange(elementAtOffset, classNameIdentifier)) {
                return new ElementContext(ElementType.CLASS_DEFINITION, psiClass);
            }
        }

        return new ElementContext(ElementType.UNKNOWN, null);
    }

    /**
     * 获取方法调用的标识符元素
     *
     * @param methodCall 方法调用表达式
     * @return 方法标识符元素
     */
    @Nullable
    private static PsiElement getMethodCallIdentifier(@NotNull PsiMethodCallExpression methodCall) {
        PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
        return methodExpression.getReferenceNameElement();
    }

    /**
     * 检查元素是否在指定范围内
     *
     * @param element 要检查的元素
     * @param target  目标范围元素
     * @return 如果在范围内返回 true
     */
    private static boolean isElementInRange(@NotNull PsiElement element, @NotNull PsiElement target) {
        int elementOffset = element.getTextOffset();
        int targetStart = target.getTextOffset();
        int targetEnd = targetStart + target.getTextLength();
        return elementOffset >= targetStart && elementOffset <= targetEnd;
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

