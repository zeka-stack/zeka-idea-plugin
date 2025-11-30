package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 任务元素辅助类
 * <p>
 * 提供与 PSI 元素相关的辅助方法，包括获取元素完整路径、构建类名、获取任务类型表情符号等。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.12.01
 * @since 1.0.0
 */
public final class TaskElementHelper {

    private TaskElementHelper() {
        // 工具类，禁止实例化
    }

    /**
     * 获取元素的完整类路径（import 路径）
     * <p>
     * 根据元素类型返回对应的完整类路径，用于在进度显示中展示。
     * <ul>
     *   <li>如果是类/接口/枚举：直接返回类的全路径（如 com.example.MyClass）</li>
     *   <li>如果是方法/字段等在类内部的元素：使用点号拼接（如 com.example.MyClass.methodName）</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 完整类路径，如果无法获取则返回元素名称
     */
    @SuppressWarnings("D")
    @NotNull
    public static String getElementQualifiedName(PsiElement element) {
        if (element == null) {
            return "UnknownElement";
        }
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            // 1. 如果是类/接口/枚举，直接返回类的全路径
            if (element instanceof PsiClass psiClass) {
                String qualifiedName = psiClass.getQualifiedName();
                if (qualifiedName != null) {
                    return qualifiedName;
                }
                String className = psiClass.getName();
                return Objects.requireNonNullElseGet(className, () -> psiClass.getClass().getSimpleName());
                // 匿名类或特殊情况下，使用类类型作为后备
            }

            // 2. 如果是方法，使用点号拼接：类全路径.方法名
            if (element instanceof PsiMethod method) {
                PsiClass containingClass = PsiTreeUtil.getParentOfType(method, PsiClass.class);
                String methodName = method.getName();
                final String className = buildClassName(containingClass, methodName);
                return Objects.requireNonNullElse(className, methodName);
            }

            // 3. 如果是字段，使用点号拼接：类全路径.字段名
            if (element instanceof PsiField field) {
                PsiClass containingClass = PsiTreeUtil.getParentOfType(field, PsiClass.class);
                String fieldName = field.getName();
                final String className = buildClassName(containingClass, fieldName);
                return Objects.requireNonNullElse(className, fieldName);
            }

            // 4. 如果是文件，尝试获取文件中的第一个类
            if (element instanceof PsiFile) {
                if (element instanceof PsiJavaFile javaFile) {
                    PsiClass[] classes = javaFile.getClasses();
                    if (classes.length > 0) {
                        String qualifiedName = classes[0].getQualifiedName();
                        if (qualifiedName != null) {
                            return qualifiedName;
                        }
                        String className = classes[0].getName();
                        if (className != null) {
                            return className;
                        }
                    }
                }
                PsiFile containingFile = element.getContainingFile();
                if (containingFile != null) {
                    return containingFile.getName();
                }
                // 最后的后备方案
                return "UnknownFile";
            }

            // 5. 其他情况，尝试查找包含的类
            PsiClass containingClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (containingClass != null) {
                String className = containingClass.getQualifiedName();
                if (className != null) {
                    String elementName = element.getClass().getSimpleName();
                    return className + "." + elementName;
                }
                String simpleClassName = containingClass.getName();
                if (simpleClassName != null) {
                    String elementName = element.getClass().getSimpleName();
                    return simpleClassName + "." + elementName;
                }
            }

            // 最后的后备方案：使用元素类型名称
            String elementTypeName = element.getClass().getSimpleName();
            if (!elementTypeName.isEmpty()) {
                return elementTypeName;
            }
            // 如果所有方法都失败，返回默认值
            return "UnknownElement";
        });
    }

    /**
     * 构建带有类名的元素名称字符串
     * <p>
     * 根据指定的类和元素名称, 构建形如 "类名. 元素名" 的字符串. 如果类名或元素名为空, 则返回 null.
     *
     * @param containingClass 包含该元素的类对象
     * @param element         元素名称
     * @return 构建后的字符串, 格式为 "类名. 元素名", 若类或元素为空则返回 null
     */
    @Nullable
    public static String buildClassName(PsiClass containingClass, String element) {
        if (containingClass != null && element != null) {
            String className = containingClass.getQualifiedName();
            if (className != null) {
                return className + "." + element;
            }
            // 如果类没有全路径，使用类名
            String classSimpleName = containingClass.getName();
            if (classSimpleName != null) {
                return classSimpleName + "." + element;
            }
        }
        return null;
    }

    /**
     * 获取任务类型的表情符号
     *
     * @param type 任务类型
     * @return 表情符号
     */
    @NotNull
    public static String getTaskTypeEmoji(@NotNull DocumentationTask.TaskType type) {
        return switch (type) {
            case CLASS -> "📦";
            case METHOD -> "⚙️";
            case TEST_METHOD -> "🧪";
            case FIELD -> "📝";
            case INTERFACE -> "🔌";
            case ENUM -> "🔢";
        };
    }
}

