package dev.dong4j.zeka.stack.idea.plugin.changelog.context;

import com.intellij.diff.fragments.LineFragment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.util.DocumentUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Python 语法上下文解析器（基于 PSI + 反射）
 * <p> 通过反射访问 Py PSI，避免强依赖 Python 插件类。
 */
public class PythonPsiContextResolver implements LanguageContextResolver {
    private static final String PY_EXT = "py";

    /** Python PSI 关键类名 */
    private static final String PY_FILE = "com.jetbrains.python.psi.PyFile";
    private static final String PY_CLASS = "com.jetbrains.python.psi.PyClass";
    private static final String PY_FUNCTION = "com.jetbrains.python.psi.PyFunction";
    private static final String PY_TARGET = "com.jetbrains.python.psi.PyTargetExpression";
    private static final String PY_IF = "com.jetbrains.python.psi.PyIfStatement";
    private static final String PY_FOR = "com.jetbrains.python.psi.PyForStatement";
    private static final String PY_WHILE = "com.jetbrains.python.psi.PyWhileStatement";
    private static final String PY_RETURN = "com.jetbrains.python.psi.PyReturnStatement";
    private static final String PY_RAISE = "com.jetbrains.python.psi.PyRaiseStatement";
    private static final String PY_CALL = "com.jetbrains.python.psi.PyCallExpression";

    @Override
    public boolean supports(@NotNull VirtualFile file) {
        return PY_EXT.equalsIgnoreCase(file.getExtension());
    }

    @Override
    public @Nullable String resolveContext(@NotNull VirtualFile file, int preferredLine, int fallbackLine) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            Project project = ProjectLocator.getInstance().guessProjectForFile(file);
            if (project == null || project.isDisposed() || DumbService.isDumb(project)) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!isPythonFile(psiFile)) {
                return null;
            }
            var document = FileDocumentManager.getInstance().getDocument(file);
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

            PsiElement function = findParentByClassName(element, PY_FUNCTION);
            PsiElement clazz = findParentByClassName(element, PY_CLASS);
            PsiElement field = findParentByClassName(element, PY_TARGET);

            String className = clazz != null ? getName(clazz) : null;
            if (function != null) {
                String funcSig = buildFunctionSignature(function);
                return className != null ? className + "#" + funcSig : funcSig;
            }
            if (field != null) {
                String fieldName = getName(field);
                return className != null && fieldName != null ? className + "#" + fieldName : fieldName;
            }
            return className != null && !className.isEmpty() ? className : null;
        });
    }

    @Override
    public @Nullable String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile file) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (project.isDisposed() || DumbService.isDumb(project)) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!isPythonFile(psiFile)) {
                return null;
            }
            PsiElement primary = findFirstDeclaration(psiFile);
            if (primary == null) {
                return null;
            }
            String name = getName(primary);
            return name != null && !name.isEmpty() ? name : null;
        });
    }

    @Override
    public @Nullable String resolveSemanticSummary(@NotNull Project project,
                                                   @NotNull VirtualFile file,
                                                   @NotNull String beforeContent,
                                                   @NotNull String afterContent,
                                                   @NotNull List<LineFragment> fragments) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (project.isDisposed() || DumbService.isDumb(project) || fragments.isEmpty()) {
                return null;
            }
            PsiFile beforeFile = createPsiFile(project, file.getName(), beforeContent);
            PsiFile afterFile = createPsiFile(project, file.getName(), afterContent);
            if (!isPythonFile(beforeFile) || !isPythonFile(afterFile)) {
                return null;
            }

            SemanticCounters counters = new SemanticCounters();
            List<String> details = new ArrayList<>();
            Set<String> processedFunctions = new HashSet<>();
            Set<String> processedClasses = new HashSet<>();
            Set<String> processedFields = new HashSet<>();

            for (LineFragment fragment : fragments) {
                PsiElement beforeFunc = findFunctionAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterFunc = findFunctionAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiElement beforeClass = findClassAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterClass = findClassAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiElement beforeField = findFieldAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterField = findFieldAtLine(afterFile, afterContent, fragment.getStartLine2());

                if (beforeFunc == null && afterFunc == null) {
                    if (beforeClass != null || afterClass != null) {
                        PsiElement clazz = afterClass != null ? afterClass : beforeClass;
                        String classKey = buildClassKey(clazz);
                        if (processedClasses.add(classKey)) {
                            counters.classChanges++;
                            details.add("类变更: " + classKey);
                        }
                    }
                    if (beforeField != null || afterField != null) {
                        PsiElement field = afterField != null ? afterField : beforeField;
                        String fieldKey = buildFieldKey(field);
                        if (processedFields.add(fieldKey)) {
                            counters.fieldChanges++;
                            if (isPublicApi(fieldKey)) {
                                counters.apiSignatureChanges++;
                                details.add("公开属性变更: " + fieldKey);
                            } else {
                                details.add("属性变更: " + fieldKey);
                            }
                        }
                    }
                    continue;
                }

                PsiElement primary = afterFunc != null ? afterFunc : beforeFunc;
                String funcKey = buildFunctionKey(primary);
                if (!processedFunctions.add(funcKey)) {
                    continue;
                }

                if (beforeFunc != null && afterFunc != null) {
                    if (isPublicApi(funcKey) && isSignatureChanged(beforeFunc, afterFunc)) {
                        counters.apiSignatureChanges++;
                        details.add("公开函数签名变更: " + funcKey);
                        continue;
                    }
                    if (isBodyChanged(beforeFunc, afterFunc)) {
                        if (isBehaviorChanged(beforeFunc, afterFunc)) {
                            counters.behaviorChanges++;
                            details.add("行为变化: " + funcKey);
                        } else if (isRefactorChange(beforeFunc, afterFunc)) {
                            counters.refactorChanges++;
                            details.add("重构调整: " + funcKey);
                        } else {
                            counters.implementationChanges++;
                            details.add("实现调整: " + funcKey);
                        }
                    }
                } else {
                    if (isPublicApi(funcKey)) {
                        counters.apiSignatureChanges++;
                        details.add("公开函数新增/删除: " + funcKey);
                    } else {
                        counters.implementationChanges++;
                        details.add("函数新增/删除: " + funcKey);
                    }
                }
            }

            if (counters.isEmpty()) {
                return null;
            }
            return buildSummary(counters, details);
        });
    }

    @Nullable
    private PsiFile createPsiFile(@NotNull Project project, @NotNull String fileName, @NotNull String content) {
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        PsiFile psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, fileType, content, System.currentTimeMillis(), false);
        return isPythonFile(psiFile) ? psiFile : null;
    }

    private boolean isPythonFile(@Nullable PsiFile file) {
        return file != null && PY_FILE.equals(file.getClass().getName());
    }

    @Nullable
    private PsiElement findFirstDeclaration(@NotNull PsiFile file) {
        for (PsiElement child : file.getChildren()) {
            String name = child.getClass().getName();
            if (PY_CLASS.equals(name) || PY_FUNCTION.equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    private PsiElement findFunctionAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, PY_FUNCTION) : null;
    }

    @Nullable
    private PsiElement findClassAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, PY_CLASS) : null;
    }

    @Nullable
    private PsiElement findFieldAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, PY_TARGET) : null;
    }

    @Nullable
    private PsiElement findElementAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        if (line < 0) {
            return null;
        }
        int offset = lineStartOffset(content, line);
        if (offset < 0) {
            return null;
        }
        return file.findElementAt(offset);
    }

    @Nullable
    private PsiElement findParentByClassName(@NotNull PsiElement element, @NotNull String className) {
        PsiElement current = element;
        while (current != null) {
            if (className.equals(current.getClass().getName())) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private int lineStartOffset(@NotNull String content, int line) {
        if (line == 0) {
            return 0;
        }
        int currentLine = 0;
        int offset = 0;
        int length = content.length();
        while (offset < length) {
            if (currentLine == line) {
                return offset;
            }
            if (content.charAt(offset++) == '\n') {
                currentLine++;
            }
        }
        return currentLine == line ? offset : -1;
    }

    @NotNull
    private String buildFunctionSignature(@NotNull PsiElement function) {
        String name = getName(function);
        String params = getParameterListText(function);
        return name != null ? name + params : params;
    }

    @NotNull
    private String buildFunctionKey(@NotNull PsiElement function) {
        PsiElement clazz = findParentByClassName(function, PY_CLASS);
        String className = clazz != null ? getName(clazz) : null;
        String sig = buildFunctionSignature(function);
        return className != null ? className + "#" + sig : sig;
    }

    @NotNull
    private String buildClassKey(@NotNull PsiElement clazz) {
        String name = getName(clazz);
        return name != null ? name : "AnonymousClass";
    }

    @NotNull
    private String buildFieldKey(@NotNull PsiElement field) {
        String name = getName(field);
        return name != null ? name : "anonymousField";
    }

    private boolean isSignatureChanged(@NotNull PsiElement beforeFunc, @NotNull PsiElement afterFunc) {
        return !buildFunctionSignature(beforeFunc).equals(buildFunctionSignature(afterFunc));
    }

    private boolean isBodyChanged(@NotNull PsiElement beforeFunc, @NotNull PsiElement afterFunc) {
        String beforeText = invokeText(beforeFunc);
        String afterText = invokeText(afterFunc);
        return beforeText != null && afterText != null && !beforeText.equals(afterText);
    }

    private boolean isBehaviorChanged(@NotNull PsiElement beforeFunc, @NotNull PsiElement afterFunc) {
        int beforeIf = countNodesByClassName(beforeFunc, PY_IF);
        int afterIf = countNodesByClassName(afterFunc, PY_IF);
        int beforeFor = countNodesByClassName(beforeFunc, PY_FOR);
        int afterFor = countNodesByClassName(afterFunc, PY_FOR);
        int beforeWhile = countNodesByClassName(beforeFunc, PY_WHILE);
        int afterWhile = countNodesByClassName(afterFunc, PY_WHILE);
        int beforeReturn = countNodesByClassName(beforeFunc, PY_RETURN);
        int afterReturn = countNodesByClassName(afterFunc, PY_RETURN);
        int beforeRaise = countNodesByClassName(beforeFunc, PY_RAISE);
        int afterRaise = countNodesByClassName(afterFunc, PY_RAISE);
        int beforeCall = countNodesByClassName(beforeFunc, PY_CALL);
        int afterCall = countNodesByClassName(afterFunc, PY_CALL);
        return beforeIf != afterIf
               || beforeFor != afterFor
               || beforeWhile != afterWhile
               || beforeReturn != afterReturn
               || beforeRaise != afterRaise
               || beforeCall != afterCall;
    }

    private boolean isRefactorChange(@NotNull PsiElement beforeFunc, @NotNull PsiElement afterFunc) {
        String beforeText = invokeText(beforeFunc);
        String afterText = invokeText(afterFunc);
        if (beforeText == null || afterText == null) {
            return false;
        }
        return beforeText.replaceAll("\\s+", "").equals(afterText.replaceAll("\\s+", ""));
    }

    private int countNodesByClassName(@NotNull PsiElement root, @NotNull String className) {
        int count = 0;
        for (PsiElement child : root.getChildren()) {
            if (className.equals(child.getClass().getName())) {
                count++;
            }
            count += countNodesByClassName(child, className);
        }
        return count;
    }

    private boolean isPublicApi(@NotNull String name) {
        if (name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        return first != '_' && Character.isLetter(first);
    }

    @Nullable
    private String getName(@NotNull PsiElement element) {
        Object name = invoke(element, "getName");
        return name instanceof String ? (String) name : null;
    }

    @NotNull
    private String getParameterListText(@NotNull PsiElement function) {
        Object params = invoke(function, "getParameterList");
        String text = params != null ? invokeText(params) : null;
        return text != null ? text : "()";
    }

    @Nullable
    private Object invoke(@NotNull Object target, @NotNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private String invokeText(@NotNull Object target) {
        Object text = invoke(target, "getText");
        return text instanceof String ? (String) text : null;
    }

    @NotNull
    private String buildSummary(@NotNull SemanticCounters counters, @NotNull List<String> details) {
        StringBuilder summary = new StringBuilder();
        summary.append("变更语义总结:\n");
        if (counters.apiSignatureChanges > 0) {
            summary.append("- 接口层：").append(counters.apiSignatureChanges).append(" 处对外签名变更\n");
        }
        if (counters.classChanges > 0) {
            summary.append("- 类级别：").append(counters.classChanges).append(" 处类结构变更\n");
        }
        if (counters.fieldChanges > 0) {
            summary.append("- 属性：").append(counters.fieldChanges).append(" 处属性变更\n");
        }
        if (counters.implementationChanges > 0) {
            summary.append("- 实现层：").append(counters.implementationChanges).append(" 处实现调整\n");
        }
        if (counters.behaviorChanges > 0) {
            summary.append("- 行为：").append(counters.behaviorChanges).append(" 处行为变化\n");
        }
        if (counters.refactorChanges > 0) {
            summary.append("- 重构：").append(counters.refactorChanges).append(" 处结构调整\n");
        }
        if (!details.isEmpty()) {
            summary.append("- 细节：\n");
            int limit = Math.min(details.size(), 5);
            for (int i = 0; i < limit; i++) {
                summary.append("  - ").append(details.get(i)).append("\n");
            }
        }
        return summary.toString().trim();
    }

    private static class SemanticCounters {
        int apiSignatureChanges;
        int implementationChanges;
        int behaviorChanges;
        int refactorChanges;
        int classChanges;
        int fieldChanges;

        boolean isEmpty() {
            return apiSignatureChanges == 0
                   && implementationChanges == 0
                   && behaviorChanges == 0
                   && refactorChanges == 0
                   && classChanges == 0
                   && fieldChanges == 0;
        }
    }
}
