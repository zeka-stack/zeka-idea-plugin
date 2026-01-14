package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger Prompt 构建工具
 */
public final class SwaggerPromptBuilder {

    private SwaggerPromptBuilder() {
    }

    @NotNull
    public static String buildMethodContext(@NotNull PsiMethod method) {
        StringBuilder sb = new StringBuilder(512);
        PsiClass containingClass = method.getContainingClass();
        if (containingClass != null) {
            sb.append("Class: ").append(containingClass.getQualifiedName()).append("\n");
            String classMappings = SwaggerSpringUtil.collectMappingAnnotations(containingClass);
            if (!classMappings.isBlank()) {
                sb.append("Class mappings:\n").append(classMappings).append("\n");
            }
        }

        String methodMappings = SwaggerSpringUtil.collectMappingAnnotations(method);
        if (!methodMappings.isBlank()) {
            sb.append("Method mappings:\n").append(methodMappings).append("\n");
        }

        sb.append("Method signature:\n").append(buildMethodSignature(method)).append("\n");

        sb.append("Parameters:\n").append(buildParameters(method)).append("\n");
        sb.append("Return:\n").append(resolveReturnType(method));
        return sb.toString().trim();
    }

    @NotNull
    private static String buildMethodSignature(@NotNull PsiMethod method) {
        String modifiers = buildModifiers(method);
        String returnType = resolveReturnType(method);
        String parameters = buildParametersInline(method);
        String throwsText = buildThrows(method);
        String signature = String.format("%s %s %s(%s)%s",
                                         modifiers,
                                         returnType,
                                         method.getName(),
                                         parameters,
                                         throwsText);
        return signature.replaceAll("\\s+", " ").trim() + " { }";
    }

    @NotNull
    private static String buildModifiers(@NotNull PsiMethod method) {
        List<String> modifiers = new ArrayList<>();
        addModifierIfPresent(modifiers, method, PsiModifier.PUBLIC);
        addModifierIfPresent(modifiers, method, PsiModifier.PROTECTED);
        addModifierIfPresent(modifiers, method, PsiModifier.PRIVATE);
        addModifierIfPresent(modifiers, method, PsiModifier.ABSTRACT);
        addModifierIfPresent(modifiers, method, PsiModifier.STATIC);
        addModifierIfPresent(modifiers, method, PsiModifier.FINAL);
        addModifierIfPresent(modifiers, method, PsiModifier.SYNCHRONIZED);
        addModifierIfPresent(modifiers, method, PsiModifier.DEFAULT);
        return String.join(" ", modifiers).trim();
    }

    private static void addModifierIfPresent(@NotNull List<String> modifiers,
                                             @NotNull PsiMethod method,
                                             @NotNull String modifier) {
        if (method.hasModifierProperty(modifier)) {
            modifiers.add(modifier);
        }
    }

    @NotNull
    private static String buildParameters(@NotNull PsiMethod method) {
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.isEmpty()) {
            return "- (none)";
        }
        StringBuilder sb = new StringBuilder();
        for (PsiParameter parameter : parameterList.getParameters()) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("- ").append(buildParameterSignature(parameter));
        }
        return sb.toString();
    }

    @NotNull
    private static String buildParametersInline(@NotNull PsiMethod method) {
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (PsiParameter parameter : parameterList.getParameters()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(buildParameterSignature(parameter));
        }
        return sb.toString();
    }

    @NotNull
    private static String buildParameterSignature(@NotNull PsiParameter parameter) {
        StringBuilder sb = new StringBuilder();
        for (PsiAnnotation annotation : parameter.getAnnotations()) {
            if (SwaggerAnnotationUtil.isSwaggerAnnotation(annotation)) {
                continue;
            }
            sb.append(annotation.getText()).append(" ");
        }
        sb.append(parameter.getType().getCanonicalText())
            .append(" ")
            .append(parameter.getName());
        return sb.toString().trim();
    }

    @NotNull
    private static String buildThrows(@NotNull PsiMethod method) {
        com.intellij.psi.PsiClassType[] throwsTypes = method.getThrowsList().getReferencedTypes();
        if (throwsTypes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" throws ");
        for (int i = 0; i < throwsTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(throwsTypes[i].getCanonicalText());
        }
        return sb.toString();
    }

    @NotNull
    private static String resolveReturnType(@NotNull PsiMethod method) {
        PsiType returnType = method.getReturnType();
        return returnType != null ? returnType.getCanonicalText() : "void";
    }
}
