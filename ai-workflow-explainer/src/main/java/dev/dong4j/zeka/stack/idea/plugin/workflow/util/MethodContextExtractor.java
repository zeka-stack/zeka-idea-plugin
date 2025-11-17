package dev.dong4j.zeka.stack.idea.plugin.workflow.util;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;

import org.jetbrains.annotations.NotNull;

import dev.dong4j.zeka.stack.idea.plugin.workflow.model.ClassInfo;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.MethodInfo;

/**
 * 方法上下文提取器
 *
 * @author dong4j
 * @version 1.0.0
 */
public final class MethodContextExtractor {
    private MethodContextExtractor() {
        // 工具类，禁止实例化
    }

    /**
     * 提取类信息
     *
     * @param psiClass PSI 类
     * @return 类信息
     */
    @NotNull
    public static ClassInfo extractClassInfo(@NotNull PsiClass psiClass) {
        ClassInfo classInfo = new ClassInfo();
        classInfo.name = psiClass.getName() != null ? psiClass.getName() : "";
        classInfo.qualifiedName = psiClass.getQualifiedName() != null ? psiClass.getQualifiedName() : "";

        // 提取包名
        PsiClass containingClass = psiClass.getContainingClass();
        if (containingClass != null) {
            classInfo.packageName = containingClass.getQualifiedName() != null
                                    ? containingClass.getQualifiedName()
                                    : "";
        } else {
            String qualifiedName = classInfo.qualifiedName;
            if (qualifiedName.contains(".")) {
                classInfo.packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
            } else {
                classInfo.packageName = "";
            }
        }

        classInfo.annotations = PSIUtil.getAnnotations(psiClass);
        classInfo.docComment = PSIUtil.getDocComment(psiClass);

        return classInfo;
    }

    /**
     * 提取方法信息
     *
     * @param psiMethod PSI 方法
     * @return 方法信息
     */
    @NotNull
    public static MethodInfo extractMethodInfo(@NotNull PsiMethod psiMethod) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.name = psiMethod.getName();
        methodInfo.signature = PSIUtil.getMethodSignature(psiMethod);

        // 提取类信息
        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass != null) {
            methodInfo.className = containingClass.getName() != null ? containingClass.getName() : "";
            methodInfo.qualifiedClassName = containingClass.getQualifiedName() != null
                                            ? containingClass.getQualifiedName()
                                            : "";
        }

        // 提取返回类型
        PsiType returnType = psiMethod.getReturnType();
        if (returnType != null) {
            methodInfo.returnType = returnType.getPresentableText();
        }

        // 提取参数信息
        com.intellij.psi.PsiParameterList parameterList = psiMethod.getParameterList();
        PsiParameter[] parameters = parameterList.getParameters();
        for (PsiParameter parameter : parameters) {
            MethodInfo.ParameterInfo paramInfo = new MethodInfo.ParameterInfo();
            paramInfo.name = parameter.getName();
            paramInfo.type = parameter.getType().getPresentableText();
            methodInfo.parameters.add(paramInfo);
        }

        // 提取注解
        methodInfo.annotations = PSIUtil.getAnnotations(psiMethod);

        // 提取文档注释
        methodInfo.docComment = PSIUtil.getDocComment(psiMethod);

        // 提取方法体摘要（简化版，只提取关键步骤）
        extractMethodBodySummary(psiMethod, methodInfo);

        return methodInfo;
    }

    /**
     * 提取方法体摘要
     * <p>
     * 简化版实现：提取方法中的方法调用作为关键步骤
     *
     * @param psiMethod  PSI 方法
     * @param methodInfo 方法信息
     */
    private static void extractMethodBodySummary(@NotNull PsiMethod psiMethod, @NotNull MethodInfo methodInfo) {
        com.intellij.psi.PsiCodeBlock body = psiMethod.getBody();
        if (body == null) {
            return;
        }

        body.accept(new com.intellij.psi.JavaRecursiveElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull com.intellij.psi.PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                PsiMethod calledMethod = expression.resolveMethod();
                if (calledMethod != null) {
                    PsiClass containingClass = calledMethod.getContainingClass();
                    if (containingClass != null) {
                        String className = containingClass.getName();
                        String methodName = calledMethod.getName();
                        if (className != null) {
                            methodInfo.bodySummary.add(className + "." + methodName + "()");
                        }
                    }
                }
            }
        });
    }
}

