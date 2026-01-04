package dev.dong4j.zeka.stack.idea.plugin.changelog.context;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.DocumentUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** XML 语法上下文解析器 */
public class XmlContextResolver implements LanguageContextResolver {
    /**
     * 判断给定文件是否支持 XML 语法上下文解析
     * <p> 该方法用于检查文件扩展名是否为 "xml"(不区分大小写), 以确定是否需要解析其 XML 上下文
     *
     * @param file 要检查的文件对象
     * @return 如果文件扩展名为 "xml", 返回 true; 否则返回 false
     */
    @Override
    public boolean supports(@NotNull VirtualFile file) {
        return "xml".equalsIgnoreCase(file.getExtension());
    }

    /**
     * 解析当前光标位置所在的 XML 标签名称
     * <p> 根据指定文件和行号定位到具体的 XML 标签, 并返回该标签的名称.
     * <p> 如果指定的行号超出文件范围, 则使用备选行号; 如果文件或项目无效, 则返回 null.
     *
     * @param file          要解析的虚拟文件, 不能为 null
     * @param preferredLine 偏好行号, 用于定位光标位置, 如果无效则使用 fallbackLine
     * @param fallbackLine  备选行号, 当 preferredLine 无效时使用
     * @return 当前行对应的 XML 标签名称, 如果无法解析或标签不存在则返回 null
     */
    @Override
    public @Nullable String resolveContext(@NotNull VirtualFile file, int preferredLine, int fallbackLine) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            Project project = ProjectLocator.getInstance().guessProjectForFile(file);
            if (project == null || project.isDisposed()) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!(psiFile instanceof XmlFile)) {
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
            XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class, false);
            if (tag == null) {
                return null;
            }
            return tag.getName();
        });
    }

    /**
     * 解析主符号名称
     * <p> 根据指定的项目和文件查找 XML 文件的根标签名称作为主符号名称
     * <p> 如果文件不是 XML 文件或没有根标签, 则返回 null
     *
     * @param project 项目对象, 不能为 null
     * @param file    文件对象, 不能为 null
     * @return XML 文件的根标签名称, 如果文件不是 XML 文件或没有根标签则返回 null
     */
    @Override
    public @Nullable String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile file) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!(psiFile instanceof XmlFile)) {
                return null;
            }
            XmlTag rootTag = ((XmlFile) psiFile).getRootTag();
            return rootTag != null ? rootTag.getName() : null;
        });
    }
}
