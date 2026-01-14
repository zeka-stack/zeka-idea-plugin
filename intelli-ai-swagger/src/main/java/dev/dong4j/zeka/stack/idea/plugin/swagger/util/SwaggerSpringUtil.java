package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Spring MVC 注解识别工具
 */
public final class SwaggerSpringUtil {

    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
        "org.springframework.stereotype.Controller",
        "org.springframework.web.bind.annotation.RestController"
                                                                    );

    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
        "org.springframework.web.bind.annotation.RequestMapping",
        "org.springframework.web.bind.annotation.GetMapping",
        "org.springframework.web.bind.annotation.PostMapping",
        "org.springframework.web.bind.annotation.PutMapping",
        "org.springframework.web.bind.annotation.DeleteMapping",
        "org.springframework.web.bind.annotation.PatchMapping"
                                                                 );

    private SwaggerSpringUtil() {
    }

    public static boolean isSpringControllerMethod(@NotNull PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null || !hasControllerAnnotation(containingClass)) {
            return false;
        }
        return hasMappingAnnotation(method) || hasMappingAnnotation(containingClass);
    }

    public static boolean hasMappingAnnotation(@NotNull PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && MAPPING_ANNOTATIONS.contains(qualifiedName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasControllerAnnotation(@NotNull PsiClass psiClass) {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && CONTROLLER_ANNOTATIONS.contains(qualifiedName)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static String collectMappingAnnotations(@NotNull PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName != null && MAPPING_ANNOTATIONS.contains(qualifiedName)) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(annotation.getText());
            }
        }
        return sb.toString();
    }
}
