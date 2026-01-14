package dev.dong4j.zeka.stack.idea.plugin.swagger.util;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Swagger 注解处理工具
 */
public final class SwaggerAnnotationUtil {

    private static final String SWAGGER_PACKAGE = "io.swagger.v3.oas.annotations";

    private static final Map<String, String> SWAGGER_ANNOTATIONS = new LinkedHashMap<>();

    static {
        SWAGGER_ANNOTATIONS.put("Operation", SWAGGER_PACKAGE + ".Operation");
        SWAGGER_ANNOTATIONS.put("Parameter", SWAGGER_PACKAGE + ".Parameter");
        SWAGGER_ANNOTATIONS.put("Parameters", SWAGGER_PACKAGE + ".Parameters");
        SWAGGER_ANNOTATIONS.put("RequestBody", SWAGGER_PACKAGE + ".parameters.RequestBody");
        SWAGGER_ANNOTATIONS.put("ApiResponse", SWAGGER_PACKAGE + ".responses.ApiResponse");
        SWAGGER_ANNOTATIONS.put("ApiResponses", SWAGGER_PACKAGE + ".responses.ApiResponses");
        SWAGGER_ANNOTATIONS.put("Schema", SWAGGER_PACKAGE + ".media.Schema");
        SWAGGER_ANNOTATIONS.put("Content", SWAGGER_PACKAGE + ".media.Content");
        SWAGGER_ANNOTATIONS.put("ArraySchema", SWAGGER_PACKAGE + ".media.ArraySchema");
        SWAGGER_ANNOTATIONS.put("ExampleObject", SWAGGER_PACKAGE + ".media.ExampleObject");
        SWAGGER_ANNOTATIONS.put("Header", SWAGGER_PACKAGE + ".headers.Header");
        SWAGGER_ANNOTATIONS.put("Tag", SWAGGER_PACKAGE + ".tags.Tag");
        SWAGGER_ANNOTATIONS.put("Tags", SWAGGER_PACKAGE + ".tags.Tags");
        SWAGGER_ANNOTATIONS.put("SecurityRequirement", SWAGGER_PACKAGE + ".security.SecurityRequirement");
        SWAGGER_ANNOTATIONS.put("SecurityRequirements", SWAGGER_PACKAGE + ".security.SecurityRequirements");
        SWAGGER_ANNOTATIONS.put("Extension", SWAGGER_PACKAGE + ".extensions.Extension");
        SWAGGER_ANNOTATIONS.put("Extensions", SWAGGER_PACKAGE + ".extensions.Extensions");
    }

    private SwaggerAnnotationUtil() {
    }

    public static boolean hasSwaggerAnnotations(@NotNull PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return false;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (isSwaggerAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSwaggerAnnotation(@NotNull PsiAnnotation annotation) {
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName != null) {
            return qualifiedName.startsWith(SWAGGER_PACKAGE);
        }
        String name = annotation.getNameReferenceElement() != null
                      ? annotation.getNameReferenceElement().getText()
                      : null;
        return name != null && SWAGGER_ANNOTATIONS.containsKey(name);
    }

    @NotNull
    public static String qualifySwaggerAnnotations(@NotNull String annotationText) {
        String qualified = annotationText;
        for (Map.Entry<String, String> entry : SWAGGER_ANNOTATIONS.entrySet()) {
            String simpleName = entry.getKey();
            String fqcn = entry.getValue();
            qualified = qualified.replaceAll("@" + simpleName + "\\b", "@" + fqcn);
        }
        return qualified;
    }

    public static void deleteSwaggerAnnotations(@NotNull PsiModifierListOwner owner) {
        PsiModifierList modifierList = owner.getModifierList();
        if (modifierList == null) {
            return;
        }
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (isSwaggerAnnotation(annotation)) {
                annotation.delete();
            }
        }
    }
}
