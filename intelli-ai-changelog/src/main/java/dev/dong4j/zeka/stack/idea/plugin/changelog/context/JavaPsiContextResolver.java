package dev.dong4j.zeka.stack.idea.plugin.changelog.context;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Java 语法上下文解析器 (依赖 com.intellij.java) */
public class JavaPsiContextResolver implements LanguageContextResolver {
    /**
     * 判断文件是否为 Java 语言文件
     * <p> 通过检查文件扩展名是否为 "java" 来判断该文件是否属于 Java 语言
     *
     * @param file 待检测的虚拟文件对象
     * @return 如果文件扩展名为 "java"(不区分大小写), 则返回 true; 否则返回 false
     */
    @Override
    public boolean supports(@NotNull VirtualFile file) {
        return "java".equalsIgnoreCase(file.getExtension());
    }

    /**
     * 解析给定文件的上下文信息
     * <p> 根据文件路径, 行号和其他信息, 尝试解析出该位置对应的类, 方法或字段名称
     *
     * @param file          要解析的文件
     * @param preferredLine 优先使用的行号
     * @param fallbackLine  备用行号, 当 preferredLine 无效时使用
     * @return 解析出的上下文信息, 可能是类名, 方法签名或字段名, 如果无法解析则返回 null
     *     <p>
     *     解析过程如下:
     *     1. 获取项目实例并检查项目是否可用
     *     2. 将文件转换为 PsiFile 对象
     *     3. 检查 PsiFile 是否为 Java 文件
     *     4. 获取文件的 Document 对象
     *     5. 计算指定行的偏移量
     *     6. 查找该偏移量处的 PsiElement
     *     7. 根据 PsiElement 查找其父类, 方法或字段
     *     8. 返回相应的类名, 方法签名或字段名
     */
    @Override
    public @Nullable String resolveContext(@NotNull VirtualFile file, int preferredLine, int fallbackLine) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            Project project = ProjectLocator.getInstance().guessProjectForFile(file);
            if (project == null || project.isDisposed()) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!(psiFile instanceof PsiJavaFile)) {
                return null;
            }
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                return null;
            }
            int lineCount = document.getLineCount();
            int line = preferredLine >= 0 && preferredLine < lineCount ? preferredLine : fallbackLine;
            if (line < 0 || line >= lineCount) {
                return null;
            }
            int offset = DocumentUtil.getLineStartOffset(line, document);
            PsiElement element = psiFile.findElementAt(offset);
            if (element == null) {
                return null;
            }
            PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
            PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
            PsiField field = PsiTreeUtil.getParentOfType(element, PsiField.class, false);

            String className = psiClass != null ? psiClass.getName() : null;
            if (method != null) {
                String methodSig = method.getName() + method.getParameterList().getText();
                return className != null ? className + "#" + methodSig : methodSig;
            }
            if (field != null) {
                return className != null ? className + "#" + field.getName() : field.getName();
            }
            return className != null && !className.isEmpty() ? className : null;
        });
    }

    /**
     * 解析并返回指定文件的主符号名称
     * <p> 该方法用于获取给定文件中第一个类的名称, 作为该文件的主符号名称.
     *
     * @param project 项目实例, 不能为 null
     * @param file    文件对象, 不能为 null
     * @return 文件中第一个类的名称, 如果文件不包含类或名称为空则返回 null
     */
    @Override
    public @Nullable String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile file) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!(psiFile instanceof PsiJavaFile)) {
                return null;
            }
            PsiClass[] classes = ((PsiJavaFile) psiFile).getClasses();
            if (classes.length == 0) {
                return null;
            }
            String name = classes[0].getName();
            return name != null && !name.isEmpty() ? name : null;
        });
    }
}
