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
    /** Go 语言文件的扩展名 */
    private static final String GO_EXT = "go";

    /** Go PSI 关键类名 */
    private static final String GO_FILE = "com.goide.psi.GoFile";
    /** Go 语言函数或方法声明的 PSI 类全限定名 */
    private static final String GO_FUNCTION = "com.goide.psi.GoFunctionOrMethodDeclaration";
    /** Go 函数声明类名 */
    private static final String GO_FUNCTION_DECL = "com.goide.psi.GoFunctionDeclaration";
    /** Go 方法声明类名 */
    private static final String GO_METHOD_DECL = "com.goide.psi.GoMethodDeclaration";
    /**
     * 表示 Go 语言中的类型规范类名.
     *
     * @see com.goide.psi.GoTypeSpec
     */
    private static final String GO_TYPE_SPEC = "com.goide.psi.GoTypeSpec";
    /**
     * 表示 Go 语言中的字段声明类名
     *
     * @see com.goide.psi.GoFieldDeclaration
     */
    private static final String GO_FIELD_DECL = "com.goide.psi.GoFieldDeclaration";
    /** Go 语言中 if 语句的 PSI 类名 */
    private static final String GO_IF = "com.goide.psi.GoIfStatement";
    /** Go 语言中 for 循环语句的 PSI 类名 */
    private static final String GO_FOR = "com.goide.psi.GoForStatement";
    /** Go 语言 switch 语句对应的 PSI 类全限定名 */
    private static final String GO_SWITCH = "com.goide.psi.GoSwitchStatement";
    /** GoReturnStatement 对应的 PSI 类全限定名 */
    private static final String GO_RETURN = "com.goide.psi.GoReturnStatement";
    /** Go 语法中调用表达式的 PSI 类名 */
    private static final String GO_CALL = "com.goide.psi.GoCallExpr";

    /**
     * 判断是否支持指定文件的上下文解析
     * <p> 检查文件扩展名是否为 Go 语言扩展名 ("go"), 用于标识当前解析器是否适用于该文件
     *
     * @param file 要判断的虚拟文件, 不能为 null
     * @return 如果文件扩展名为 "go"(不区分大小写), 则返回 true, 否则返回 false
     */
    @Override
    public boolean supports(@NotNull VirtualFile file) {
        return GO_EXT.equalsIgnoreCase(file.getExtension());
    }

    /**
     * 解析并返回指定文件中某一行的上下文信息
     * <p> 根据给定的文件, 首选行号和回退行号, 确定该位置所属的语法结构上下文 (如函数, 类型或字段)
     *
     * @param file          文件对象
     * @param preferredLine 首选行号, 如果无效则使用 fallbackLine
     * @param fallbackLine  回退行号, 在 preferredLine 无效时使用
     * @return 返回当前行所在语法元素的上下文标识符, 格式为 "类型名 #方法签名" 或 "类型名 #字段名" 等, 如果无法解析则返回 null
     */
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

    /**
     * 获取文件中的主要符号名称
     * <p> 解析指定文件的 PSI 结构, 找到其主要声明 (如函数, 类型或字段), 并返回该符号的名称.
     *
     * @param project 项目对象, 用于获取 PSI 文件和执行读操作
     * @param file    要解析的文件对象
     * @return 文件中的主要符号名称, 若未找到或解析失败则返回 null
     */
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

    /**
     * 解析语义摘要, 用于比较代码变更的语义信息
     * <p> 该方法通过分析文件前后内容的差异, 识别出类型, 字段和函数的变更情况, 并生成语义总结.
     *
     * @param project       项目对象, 用于获取 PSI 文件和执行读操作
     * @param file          要解析的文件对象
     * @param beforeContent 文件修改前的内容
     * @param afterContent  文件修改后的内容
     * @param fragments     需要分析的代码片段列表
     * @return 包含变更语义的总结字符串, 若无变更则返回 null
     *
     *     <pre>{@code
     *         String summary = resolver.resolveSemanticSummary(project, file, beforeContent, afterContent, fragments);
     *         if (summary != null) {*     System.out.println("变更总结:" + summary);
     *         }
     *         }</pre>
     */
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

    /**
     * 根据文件名和内容创建 PSI 文件
     * <p> 通过指定的项目, 文件名和内容, 使用 PSI 文件工厂创建一个临时的 PSI 文件对象, 用于后续的语法分析.
     * <p> 仅当创建的文件为 Go 语言文件时才返回, 否则返回 null.
     * <p> 使用示例:
     * <pre>{@code
     * PsiFile goFile = createPsiFile(project, "example.go", "package main\nfunc main() {}");
     * }</pre>
     *
     * @param project  项目对象, 不能为 null
     * @param fileName 文件名, 不能为 null
     * @param content  文件内容, 不能为 null
     * @return 创建的 PSI 文件对象, 如果内容不是 Go 文件则返回 null
     */
    @Nullable
    private PsiFile createPsiFile(@NotNull Project project, @NotNull String fileName, @NotNull String content) {
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        PsiFile psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, fileType, content, System.currentTimeMillis(), false);
        return isGoFile(psiFile) ? psiFile : null;
    }

    /**
     * 判断指定的 PsiFile 是否为 Go 语言文件
     * <p> 通过检查 PsiFile 的类名是否与预定义的 Go PSI 文件类名一致来判断
     *
     * @param file 要检查的 PsiFile 对象, 可能为 null
     * @return 如果是 Go 语言文件返回 true, 否则返回 false
     */
    private boolean isGoFile(@Nullable PsiFile file) {
        return file != null && GO_FILE.equals(file.getClass().getName());
    }

    /**
     * 查找文件中的第一个声明元素
     * <p>遍历文件的子元素, 寻找第一个符合条件的声明 (函数, 方法或类型定义) 并返回
     *
     * @param file 要检查的 PSI 文件
     * @return 符合条件的第一个声明元素, 如果没有找到则返回 null
     */
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

    /**
     * 在给定的内容中查找指定行上的函数声明
     * <p> 通过在指定行查找元素, 并向上寻找父节点以找到函数声明
     *
     * @param file    包含内容的 Psi 文件
     * @param content 文件的内容字符串
     * @param line    要查找的行号
     * @return 找到的函数声明对应的 Psi 元素, 如果未找到则返回 null
     */
    @Nullable
    private PsiElement findFunctionAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, GO_FUNCTION) : null;
    }

    /**
     * 根据指定行号在 Go 文件中查找类型声明元素
     * <p> 通过行号定位文件中的元素, 然后向上查找其父节点是否为类型声明 (GoTypeSpec)
     *
     * @param file    Go 文件对象, 不能为 null
     * @param content 文件内容字符串, 不能为 null
     * @param line    目标行号, 必须 >= 0
     * @return 如果找到且其父节点为类型声明, 则返回该类型声明元素; 否则返回 null
     */
    @Nullable
    private PsiElement findTypeAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, GO_TYPE_SPEC) : null;
    }

    /**
     * 根据指定行号查找对应的字段声明
     * <p> 在给定的 PsiFile 中, 根据内容和行号定位到对应的字段声明元素.
     *
     * @param file    目标 PsiFile 文件
     * @param content 文件内容字符串
     * @param line    要查找的行号 (从 0 开始)
     * @return 如果找到对应的字段声明, 则返回该元素; 否则返回 null
     */
    @Nullable
    private PsiElement findFieldAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, GO_FIELD_DECL) : null;
    }

    /**
     * 在指定行号处查找对应的 PSI 元素
     * <p> 根据给定的文件内容和行号, 计算该行起始偏移量, 并返回该偏移位置处的 PSI 元素.
     *
     * @param file    目标 PSI 文件
     * @param content 文件内容字符串
     * @param line    要查找的行号 (从 0 开始)
     * @return 对应行的 PSI 元素, 如果未找到则返回 null
     */
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

    /**
     * 根据类名查找指定元素的父级元素
     * <p> 从指定元素开始向上遍历其父元素链, 直到找到类名匹配的父元素为止
     * <p> 匹配规则: 通过反射比较元素的类全名与指定类名是否相等
     *
     * @param element   起始元素, 不能为 null
     * @param className 要匹配的类全名, 不能为 null
     * @return 找到的第一个匹配类名的父元素, 如果未找到则返回 null
     */
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

    /**
     * 获取指定行在文本中的起始偏移量
     * <p> 从给定的文本内容中找到指定行的起始位置, 返回该行的字符偏移量.
     * <p> 第一行的行号为 0.
     *
     * @param content 文本内容
     * @param line    行号, 从 0 开始计数
     * @return 指定行的起始偏移量, 如果行号超出文本范围则返回 - 1
     */
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

    /**
     * 构建函数签名字符串
     * <p> 根据函数元素获取其名称和签名文本, 组合成完整的函数签名表示
     * <p> 若名称为空, 则返回签名文本 (若签名文本为空则返回 "func"); 若签名文本为空, 则仅返回名称
     * <p> 示例:
     * <pre>{@code
     * // 若函数名为 "foo", 签名为空, 则返回 "foo"
     * // 若函数名为 null, 签名文本为 "(int, string)", 则返回 "(int, string)"
     * // 若函数名为 "bar", 签名文本为 "(int, string)", 则返回 "bar(int, string)"
     * }</pre>
     *
     * @param function 函数元素, 不能为 null
     * @return 构建后的函数签名字符串, 可能为 "func", 名称, 或名称 + 签名组合
     */
    @NotNull
    private String buildFunctionSignature(@NotNull PsiElement function) {
        String name = getName(function);
        String signature = getSignatureText(function);
        if (name == null) {
            return signature.isEmpty() ? "func" : signature;
        }
        return signature.isEmpty() ? name : name + signature;
    }

    /**
     * 构建函数的唯一标识键
     * <p> 根据函数元素生成唯一的标识字符串, 如果函数属于某个类型, 则返回 "类型名 #函数签名" 格式, 否则只返回函数签名
     *
     * @param function 函数 PSI 元素, 不能为 null
     * @return 函数的唯一标识键, 格式为 "类型名 #函数签名"(属于某类型时) 或 "函数签名"(顶级函数时), 不会返回 null
     */
    @NotNull
    private String buildFunctionKey(@NotNull PsiElement function) {
        String name = buildFunctionSignature(function);
        PsiElement typeSpec = findParentByClassName(function, GO_TYPE_SPEC);
        String typeName = typeSpec != null ? getName(typeSpec) : null;
        return typeName != null ? typeName + "#" + name : name;
    }

    /**
     * 构建类型的关键字
     * <p> 根据给定的类型规范 Psi 元素, 提取其名称作为类型关键字. 如果名称为空, 则返回 "AnonymousType" 表示匿名类型.
     *
     * @param typeSpec 类型规范的 Psi 元素
     * @return 类型名称, 如果名称为空则返回 "AnonymousType"
     */
    @NotNull
    private String buildTypeKey(@NotNull PsiElement typeSpec) {
        String name = getName(typeSpec);
        return name != null ? name : "AnonymousType";
    }

    /**
     * 构建字段键
     * <p> 获取字段的名称作为标识键, 如果字段没有名称则返回 "anonymousField"
     *
     * @param field PSI 元素, 不能为 null
     * @return 字段键, 字段名称或 "anonymousField"
     */
    @NotNull
    private String buildFieldKey(@NotNull PsiElement field) {
        String name = getFieldName(field);
        return name != null ? name : "anonymousField";
    }

    /**
     * 获取 PSI 元素的名称
     * <p> 通过反射调用 {@code getName()} 方法获取元素名称, 如果返回值不是字符串则返回 null
     *
     * @param element 要获取名称的 PSI 元素
     * @return 元素名称, 若无法获取或类型不匹配则返回 null
     */
    @Nullable
    private String getName(@NotNull PsiElement element) {
        Object name = invoke(element, "getName");
        return name instanceof String ? (String) name : null;
    }

    /**
     * 获取字段名
     * <p> 通过反射调用 PsiElement 的 getName 方法, 获取字段的名称.
     *
     * @param field 字段元素, 不能为 null
     * @return 字段名称, 如果获取失败则返回 null
     */
    @Nullable
    private String getFieldName(@NotNull PsiElement field) {
        Object id = invoke(field, "getName");
        if (id instanceof String) {
            return (String) id;
        }
        return null;
    }

    /**
     * 获取函数的签名文本
     * <p> 通过反射调用 PsiElement 的 getSignature 方法, 获取函数的签名字符串表示.
     *
     * @param function 函数元素, 不能为 null
     * @return 函数的签名文本, 如果未找到则返回空字符串
     */
    @NotNull
    private String getSignatureText(@NotNull PsiElement function) {
        Object signature = invoke(function, "getSignature");
        String text = signature != null ? invokeText(signature) : null;
        return text != null ? text : "";
    }

    /**
     * 判断两个函数的签名是否发生变化
     * <p> 通过比较两个函数的签名字符串是否相等来判断签名是否变更
     * <p> 签名包括函数名和参数列表等结构信息, 但不包括函数体内容
     *
     * @param beforeFunction 旧的函数元素, 不能为 null
     * @param afterFunction  新的函数元素, 不能为 null
     * @return 如果两个函数的签名不相等则返回 true, 表示签名已变更; 否则返回 false
     */
    private boolean isSignatureChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        return !buildFunctionSignature(beforeFunction).equals(buildFunctionSignature(afterFunction));
    }

    /**
     * 判断函数体内容是否发生变化
     * <p> 通过比较两个函数的文本内容来判断其函数体是否相同.
     *
     * @param beforeFunction 旧版本函数元素
     * @param afterFunction  新版本函数元素
     * @return 如果函数体内容不同则返回 true, 否则返回 false
     */
    private boolean isBodyChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeText = invokeText(beforeFunction);
        String afterText = invokeText(afterFunction);
        return beforeText != null && afterText != null && !beforeText.equals(afterText);
    }

    /**
     * 判断两个函数的行为是否发生变化
     * <p> 通过比较两个函数中的控制结构节点数量来判断行为是否发生变化
     *
     * @param beforeFunction 修改前的函数元素
     * @param afterFunction  修改后的函数元素
     * @return 如果行为发生变化, 返回 true; 否则返回 false
     * @since 1.0
     */
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

    /**
     * 判断两个函数是否发生了重构调整
     * <p> 通过去除空白字符后比较两个函数的文本内容来判断是否发生了重构调整.
     * 如果去除空白字符后的文本相同, 则认为是重构调整.
     *
     * @param beforeFunction 源函数
     * @param afterFunction  目标函数
     * @return 如果两个函数在去除空白字符后文本相同, 则返回 true, 表示发生了重构调整; 否则返回 false
     */
    private boolean isRefactorChange(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeText = invokeText(beforeFunction);
        String afterText = invokeText(afterFunction);
        if (beforeText == null || afterText == null) {
            return false;
        }
        return beforeText.replaceAll("\\s+", "").equals(afterText.replaceAll("\\s+", ""));
    }

    /**
     * 统计指定类名的子节点数量
     * <p> 递归遍历 PSI 元素的子节点, 统计与给定类名匹配的节点总数.
     *
     * @param root      PSI 元素的根节点
     * @param className 要匹配的类名
     * @return 匹配的节点数量
     */
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

    /**
     * 判断名称是否为导出标识 (首字母大写)
     * <p> 在 Go 语言中, 导出标识的首字母必须大写, 该方法用于判断给定名称是否符合导出规则
     *
     * @param name 待判断的名称, 不能为空
     * @return 如果名称非空且首字母为大写, 则返回 true, 否则返回 false
     */
    private boolean isExported(@NotNull String name) {
        if (name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        return Character.isUpperCase(first);
    }

    /**
     * 调用指定对象的指定方法并返回结果
     * <p> 通过反射机制获取目标对象的指定方法并执行, 若方法调用过程中发生异常, 则返回 null.
     *
     * @param target     目标对象, 不能为 null
     * @param methodName 方法名称, 不能为 null
     * @return 方法调用的结果, 若方法调用失败或抛出异常则返回 null
     */
    @Nullable
    private Object invoke(@NotNull Object target, @NotNull String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 调用目标对象的 getText 方法并返回结果字符串
     * <p> 通过反射调用指定对象的 {@code getText} 方法, 并检查返回值是否为字符串类型.
     *
     * @param target 需要调用方法的目标对象, 不能为 null
     * @return 返回调用 {@code getText} 方法的结果, 如果结果不是字符串或调用失败则返回 null
     */
    @Nullable
    private String invokeText(@NotNull Object target) {
        Object text = invoke(target, "getText");
        return text instanceof String ? (String) text : null;
    }

    /**
     * 生成变更语义总结文本
     * <p>根据统计计数器和变更细节列表, 构建结构化的语义变更摘要, 用于展示代码变更的类型和数量.
     * <p>输出格式包含变更分类 (如接口层, 类型, 字段, 实现层, 行为, 重构) 及前 5 条细节变更记录.
     * <p>示例输出:
     * <pre>{@code
     * 变更语义总结:
     * - 接口层:2 处对外签名变更
     * - 类型:1 处类型结构变更
     * - 细节:
     * - 类型变更: MyStruct#Field1
     * - 导出字段变更: MyStruct#ExportedField
     * }</pre>
     *
     * @param counters 变更统计计数器对象, 包含各类变更数量
     * @param details  变更细节列表, 最多展示前 5 条
     * @return 格式化后的语义变更总结文本, 若无变更则返回空字符串
     */
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

    /**
     * 语义计数器类
     * <p> 用于记录和跟踪代码变更的不同类型, 包括 API 签名变化, 实现变化, 行为变化, 重构变化, 类变化和字段变化
     * <p> 通过判断是否为空来确定是否存在任何类型的变更
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.09
     * @since 1.0.0
     */
    private static class SemanticCounters {
        /** API 签名变更计数器, 用于记录接口签名相关的更改次数 */
        int apiSignatureChanges;
        /** 实现层面的变更数量 */
        int implementationChanges;
        /** 行为变更计数, 用于统计语义层面的行为修改次数 */
        int behaviorChanges;
        /** refactorChanges 表示重构引起的代码变更数量 */
        int refactorChanges;
        /** 表示类级别的变更次数 */
        int classChanges;
        /** 字段变更数量 */
        int fieldChanges;

        /**
         * 判断语义计数器是否为空
         * <p>当所有语义变更计数器 (包括 API 签名变更, 实现变更, 行为变更, 重构变更, 类变更, 字段变更) 均为 0 时, 返回 true
         *
         * @return 如果所有变更计数器均为 0, 则返回 true, 否则返回 false
         */
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
