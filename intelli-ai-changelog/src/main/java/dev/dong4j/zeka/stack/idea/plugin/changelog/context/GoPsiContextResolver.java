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
 * Go 语法上下文解析器（基于 PSI + 反射）
 * <p> 通过反射调用 Go PSI，避免直接依赖 Go 插件类。
 */
public class GoPsiContextResolver implements LanguageContextResolver {
    private static final String GO_EXT = "go";

    /** Go PSI 关键类名 */
    private static final String GO_FILE = "com.goide.psi.GoFile";
    private static final String GO_FUNCTION = "com.goide.psi.GoFunctionOrMethodDeclaration";
    private static final String GO_FUNCTION_DECL = "com.goide.psi.GoFunctionDeclaration";
    private static final String GO_METHOD_DECL = "com.goide.psi.GoMethodDeclaration";
    private static final String GO_TYPE_SPEC = "com.goide.psi.GoTypeSpec";
    private static final String GO_FIELD_DECL = "com.goide.psi.GoFieldDeclaration";
    private static final String GO_IF = "com.goide.psi.GoIfStatement";
    private static final String GO_FOR = "com.goide.psi.GoForStatement";
    private static final String GO_SWITCH = "com.goide.psi.GoSwitchStatement";
    private static final String GO_RETURN = "com.goide.psi.GoReturnStatement";
    private static final String GO_CALL = "com.goide.psi.GoCallExpr";

    @Override
    public boolean supports(@NotNull VirtualFile file) {
        return GO_EXT.equalsIgnoreCase(file.getExtension());
    }

    @Override
    public @Nullable String resolveContext(@NotNull VirtualFile file, int preferredLine, int fallbackLine) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            Project project = ProjectLocator.getInstance().guessProjectForFile(file);
            if (project == null || project.isDisposed() || DumbService.isDumb(project)) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!isGoFile(psiFile)) {
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

            PsiElement function = findParentByClassName(element, GO_FUNCTION);
            PsiElement typeSpec = findParentByClassName(element, GO_TYPE_SPEC);
            PsiElement field = findParentByClassName(element, GO_FIELD_DECL);

            String typeName = typeSpec != null ? getName(typeSpec) : null;
            if (function != null) {
                String funcSig = buildFunctionSignature(function);
                return typeName != null ? typeName + "#" + funcSig : funcSig;
            }
            if (field != null) {
                String fieldName = getFieldName(field);
                return typeName != null && fieldName != null ? typeName + "#" + fieldName : fieldName;
            }
            return typeName != null && !typeName.isEmpty() ? typeName : null;
        });
    }

    @Override
    public @Nullable String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile file) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (project.isDisposed() || DumbService.isDumb(project)) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!isGoFile(psiFile)) {
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
            if (!isGoFile(beforeFile) || !isGoFile(afterFile)) {
                return null;
            }

            SemanticCounters counters = new SemanticCounters();
            List<String> details = new ArrayList<>();
            Set<String> processedFunctions = new HashSet<>();
            Set<String> processedTypes = new HashSet<>();
            Set<String> processedFields = new HashSet<>();

            for (LineFragment fragment : fragments) {
                PsiElement beforeFunc = findFunctionAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterFunc = findFunctionAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiElement beforeType = findTypeAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterType = findTypeAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiElement beforeField = findFieldAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterField = findFieldAtLine(afterFile, afterContent, fragment.getStartLine2());

                if (beforeFunc == null && afterFunc == null) {
                    if (beforeType != null || afterType != null) {
                        PsiElement type = afterType != null ? afterType : beforeType;
                        String typeKey = buildTypeKey(type);
                        if (processedTypes.add(typeKey)) {
                            counters.classChanges++;
                            details.add("类型变更: " + typeKey);
                        }
                    }
                    if (beforeField != null || afterField != null) {
                        PsiElement field = afterField != null ? afterField : beforeField;
                        String fieldKey = buildFieldKey(field);
                        if (processedFields.add(fieldKey)) {
                            counters.fieldChanges++;
                            if (isExported(fieldKey)) {
                                counters.apiSignatureChanges++;
                                details.add("导出字段变更: " + fieldKey);
                            } else {
                                details.add("字段变更: " + fieldKey);
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
                    if (isExported(funcKey) && isSignatureChanged(beforeFunc, afterFunc)) {
                        counters.apiSignatureChanges++;
                        details.add("导出函数签名变更: " + funcKey);
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
                    if (isExported(funcKey)) {
                        counters.apiSignatureChanges++;
                        details.add("导出函数新增/删除: " + funcKey);
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
        return isGoFile(psiFile) ? psiFile : null;
    }

    private boolean isGoFile(@Nullable PsiFile file) {
        return file != null && GO_FILE.equals(file.getClass().getName());
    }

    @Nullable
    private PsiElement findFirstDeclaration(@NotNull PsiFile file) {
        for (PsiElement child : file.getChildren()) {
            String name = child.getClass().getName();
            if (GO_FUNCTION.equals(name) || GO_FUNCTION_DECL.equals(name) || GO_METHOD_DECL.equals(name) || GO_TYPE_SPEC.equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    private PsiElement findFunctionAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, GO_FUNCTION) : null;
    }

    @Nullable
    private PsiElement findTypeAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, GO_TYPE_SPEC) : null;
    }

    @Nullable
    private PsiElement findFieldAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, GO_FIELD_DECL) : null;
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
        String signature = getSignatureText(function);
        if (name == null) {
            return signature.isEmpty() ? "func" : signature;
        }
        return signature.isEmpty() ? name : name + signature;
    }

    @NotNull
    private String buildFunctionKey(@NotNull PsiElement function) {
        String name = buildFunctionSignature(function);
        PsiElement typeSpec = findParentByClassName(function, GO_TYPE_SPEC);
        String typeName = typeSpec != null ? getName(typeSpec) : null;
        return typeName != null ? typeName + "#" + name : name;
    }

    @NotNull
    private String buildTypeKey(@NotNull PsiElement typeSpec) {
        String name = getName(typeSpec);
        return name != null ? name : "AnonymousType";
    }

    @NotNull
    private String buildFieldKey(@NotNull PsiElement field) {
        String name = getFieldName(field);
        return name != null ? name : "anonymousField";
    }

    @Nullable
    private String getName(@NotNull PsiElement element) {
        Object name = invoke(element, "getName");
        return name instanceof String ? (String) name : null;
    }

    @Nullable
    private String getFieldName(@NotNull PsiElement field) {
        Object id = invoke(field, "getName");
        if (id instanceof String) {
            return (String) id;
        }
        return null;
    }

    @NotNull
    private String getSignatureText(@NotNull PsiElement function) {
        Object signature = invoke(function, "getSignature");
        String text = signature != null ? invokeText(signature) : null;
        return text != null ? text : "";
    }

    private boolean isSignatureChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        return !buildFunctionSignature(beforeFunction).equals(buildFunctionSignature(afterFunction));
    }

    private boolean isBodyChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeText = invokeText(beforeFunction);
        String afterText = invokeText(afterFunction);
        return beforeText != null && afterText != null && !beforeText.equals(afterText);
    }

    private boolean isBehaviorChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        int beforeIf = countNodesByClassName(beforeFunction, GO_IF);
        int afterIf = countNodesByClassName(afterFunction, GO_IF);
        int beforeFor = countNodesByClassName(beforeFunction, GO_FOR);
        int afterFor = countNodesByClassName(afterFunction, GO_FOR);
        int beforeSwitch = countNodesByClassName(beforeFunction, GO_SWITCH);
        int afterSwitch = countNodesByClassName(afterFunction, GO_SWITCH);
        int beforeReturn = countNodesByClassName(beforeFunction, GO_RETURN);
        int afterReturn = countNodesByClassName(afterFunction, GO_RETURN);
        int beforeCall = countNodesByClassName(beforeFunction, GO_CALL);
        int afterCall = countNodesByClassName(afterFunction, GO_CALL);
        return beforeIf != afterIf
               || beforeFor != afterFor
               || beforeSwitch != afterSwitch
               || beforeReturn != afterReturn
               || beforeCall != afterCall;
    }

    private boolean isRefactorChange(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeText = invokeText(beforeFunction);
        String afterText = invokeText(afterFunction);
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

    private boolean isExported(@NotNull String name) {
        if (name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        return Character.isUpperCase(first);
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
            summary.append("- 类型：").append(counters.classChanges).append(" 处类型结构变更\n");
        }
        if (counters.fieldChanges > 0) {
            summary.append("- 字段：").append(counters.fieldChanges).append(" 处字段变更\n");
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
