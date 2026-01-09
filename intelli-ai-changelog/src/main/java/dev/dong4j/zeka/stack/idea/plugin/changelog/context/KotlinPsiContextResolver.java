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
import java.util.Locale;
import java.util.Set;

/**
 * Kotlin 语法上下文解析器（基于 PSI + 反射）
 * <p> 该实现避免直接依赖 Kotlin PSI 类，通过类名与反射访问关键属性，
 * 仅在 Kotlin 插件可用时启用，适配多语言环境。
 *
 * @author dong4j
 */
public class KotlinPsiContextResolver implements LanguageContextResolver {
    /** Kotlin 文件扩展名 */
    private static final String KOTLIN_EXT = "kt";
    /** Kotlin 脚本文件扩展名 */
    private static final String KOTLIN_SCRIPT_EXT = "kts";

    /** Kotlin PSI 关键类名 */
    private static final String KT_FILE = "org.jetbrains.kotlin.psi.KtFile";
    /** Kotlin */
    private static final String KT_FUNCTION = "org.jetbrains.kotlin.psi.KtNamedFunction";
    /** Kotlin 属性节点的 PSI 类名 */
    private static final String KT_PROPERTY = "org.jetbrains.kotlin.psi.KtProperty";
    /** Kotlin 类或对象 PSI 元素类名 */
    private static final String KT_CLASS = "org.jetbrains.kotlin.psi.KtClassOrObject";
    /** Kotlin 中的 if 表达式节点类名 */
    private static final String KT_IF = "org.jetbrains.kotlin.psi.KtIfExpression";
    /**
     * KtWhenExpression 的类名常量
     * <p> 表示 Kotlin 语言中的 when 表达式对应的 PSI 元素类名
     */
    private static final String KT_WHEN = "org.jetbrains.kotlin.psi.KtWhenExpression";
    /**
     * Kotlin 返回表达式的 PSI 类名称
     * <p> 用于在 PSI 树中查找和识别 Kotlin 返回语句
     */
    private static final String KT_RETURN = "org.jetbrains.kotlin.psi.KtReturnExpression";
    /** KtThrowExpression 类名字符串常量 */
    private static final String KT_THROW = "org.jetbrains.kotlin.psi.KtThrowExpression";
    /** KtCallExpression 类名字符串, 用于反射访问 Kotlin 调用表达式节点 */
    private static final String KT_CALL = "org.jetbrains.kotlin.psi.KtCallExpression";

    /**
     * 判断是否支持给定的虚拟文件
     * <p>检查文件扩展名是否为 Kotlin 文件(.kt 或 .kts), 以确定是否支持解析该文件
     *
     * @param file 虚拟文件对象, 不能为 null
     * @return 如果是 Kotlin 文件 (扩展名为 .kt 或 .kts) 则返回 true, 否则返回 false
     */
    @Override
    public boolean supports(@NotNull VirtualFile file) {
        String ext = file.getExtension();
        return KOTLIN_EXT.equalsIgnoreCase(ext) || KOTLIN_SCRIPT_EXT.equalsIgnoreCase(ext);
    }

    /**
     * 解析并返回指定 Kotlin 文件的代码上下文标识
     * <p> 在读取操作中定位指定行号的 PSI 元素, 查找其所属的函数, 属性或类,
     * 并返回相应的上下文标识字符串
     * <p> 返回格式:
     * <ul>
     *   <li> 函数:{@code 类名 #方法签名}</li>
     *   <li> 属性:{@code 类名 #属性名}</li>
     *   <li> 类:{@code 类名}</li>
     * </ul>
     *
     * @param file          目标虚拟文件, 不能为 null
     * @param preferredLine 首选行号 (0 索引), 超出范围时使用 fallbackLine
     * @param fallbackLine  备用行号, 当 preferredLine 无效时使用
     * @return 代码上下文标识字符串, 无法解析时返回 null
     * @see LanguageContextResolver#resolveContext
     */
    @Override
    public @Nullable String resolveContext(@NotNull VirtualFile file, int preferredLine, int fallbackLine) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            Project project = ProjectLocator.getInstance().guessProjectForFile(file);
            if (project == null || project.isDisposed() || DumbService.isDumb(project)) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!isKotlinFile(psiFile)) {
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

            PsiElement function = findParentByClassName(element, KT_FUNCTION);
            PsiElement property = findParentByClassName(element, KT_PROPERTY);
            PsiElement clazz = findParentByClassName(element, KT_CLASS);

            String className = clazz != null ? getName(clazz) : null;
            if (function != null) {
                String methodSig = buildFunctionSignature(function);
                return className != null ? className + "#" + methodSig : methodSig;
            }
            if (property != null) {
                String fieldName = getName(property);
                return className != null && fieldName != null ? className + "#" + fieldName : fieldName;
            }
            return className != null && !className.isEmpty() ? className : null;
        });
    }

    /**
     * 根据项目和文件解析主符号名称
     * <p>在读取操作中, 通过查找文件中的第一个声明节点 (类, 函数或属性) 并提取其名称作为主符号名.
     * <p>仅在 Kotlin 文件中有效, 且项目未被销毁, 非哑状态时执行.
     *
     * @param project 项目对象, 不能为 null
     * @param file    文件对象, 不能为 null
     * @return 主符号名称, 如果未找到有效声明或名称为空则返回 null
     */
    @Override
    public @Nullable String resolvePrimarySymbolName(@NotNull Project project, @NotNull VirtualFile file) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (project.isDisposed() || DumbService.isDumb(project)) {
                return null;
            }
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (!isKotlinFile(psiFile)) {
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
     * 根据前后代码内容和行片段分析语义变更摘要
     * <p> 通过对比前后代码内容, 结合行片段信息, 识别并统计 Kotlin 代码中类, 方法, 属性等元素的语义变更情况.
     * <p> 支持的变更类型包括: 类签名变更, 注解变更, 方法签名变更, 可见性变更, 返回值类型变更, 方法体行为变化, 重构调整, 属性默认值变化等.
     * <p> 示例:
     * <pre>{@code
     * String summary = resolveSemanticSummary(project, file, beforeContent, afterContent, fragments);
     * }</pre>
     *
     * @param project       项目对象, 用于获取 PSI 文件和执行读取操作
     * @param file          文件对象, 用于获取文件名和上下文
     * @param beforeContent 对比前的代码内容
     * @param afterContent  对比后的代码内容
     * @param fragments     行片段列表, 每个片段包含前后行号, 用于定位变更位置
     * @return 语义变更摘要字符串, 包含变更类型和详细描述; 若无变更或不支持语言则返回 null
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
            if (!isKotlinFile(beforeFile) || !isKotlinFile(afterFile)) {
                return null;
            }

            SemanticCounters counters = new SemanticCounters();
            List<String> details = new ArrayList<>();
            Set<String> processedFunctions = new HashSet<>();
            Set<String> processedProperties = new HashSet<>();
            Set<String> processedClasses = new HashSet<>();

            for (LineFragment fragment : fragments) {
                PsiElement beforeFunction = findFunctionAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterFunction = findFunctionAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiElement beforeProperty = findPropertyAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterProperty = findPropertyAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiElement beforeClass = findClassAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiElement afterClass = findClassAtLine(afterFile, afterContent, fragment.getStartLine2());

                if (beforeFunction == null && afterFunction == null) {
                    if (beforeProperty != null || afterProperty != null) {
                        PsiElement property = afterProperty != null ? afterProperty : beforeProperty;
                        String propertyKey = buildPropertyKey(property);
                        if (processedProperties.add(propertyKey)) {
                            counters.fieldChanges++;
                            if (isPublicApi(property)) {
                                counters.apiSignatureChanges++;
                                details.add("public 属性变更: " + propertyKey);
                            } else {
                                details.add("属性变更: " + propertyKey);
                            }
                            if (beforeProperty != null && afterProperty != null
                                && isPropertyInitializerChanged(beforeProperty, afterProperty)) {
                                counters.implementationChanges++;
                                details.add("属性默认值变化: " + propertyKey);
                            }
                        }
                    } else if (beforeClass != null || afterClass != null) {
                        PsiElement clazz = afterClass != null ? afterClass : beforeClass;
                        String classKey = buildClassKey(clazz);
                        if (processedClasses.add(classKey)) {
                            if (beforeClass != null && afterClass != null) {
                                if (isClassSignatureChanged(beforeClass, afterClass)) {
                                    counters.classChanges++;
                                    details.add("类签名变更: " + classKey);
                                }
                                if (isAnnotationChanged(beforeClass, afterClass)) {
                                    counters.annotationChanges++;
                                    details.add("类注解变更: " + classKey);
                                }
                            } else {
                                counters.classChanges++;
                                details.add("类新增/删除: " + classKey);
                            }
                        }
                    }
                    continue;
                }

                PsiElement primary = afterFunction != null ? afterFunction : beforeFunction;
                String methodKey = buildFunctionKey(primary);
                if (!processedFunctions.add(methodKey)) {
                    continue;
                }

                if (beforeFunction != null && afterFunction != null) {
                    if (isPublicApi(afterFunction) && isSignatureChanged(beforeFunction, afterFunction)) {
                        counters.apiSignatureChanges++;
                        details.add("public 方法签名变更: " + methodKey);
                        continue;
                    }
                    if (isVisibilityChanged(beforeFunction, afterFunction)) {
                        counters.apiSignatureChanges++;
                        details.add("方法可见性变更: " + methodKey);
                    }
                    if (isReturnTypeChanged(beforeFunction, afterFunction)) {
                        counters.apiSignatureChanges++;
                        details.add("返回值类型变更: " + methodKey);
                    }
                    if (isBodyChanged(beforeFunction, afterFunction)) {
                        if (isBehaviorChanged(beforeFunction, afterFunction)) {
                            counters.behaviorChanges++;
                            details.add("行为变化: " + methodKey);
                        } else if (isRefactorChange(beforeFunction, afterFunction)) {
                            counters.refactorChanges++;
                            details.add("重构调整: " + methodKey);
                        } else {
                            counters.implementationChanges++;
                            details.add("实现调整: " + methodKey);
                        }
                    }
                } else {
                    if (isPublicApi(primary)) {
                        counters.apiSignatureChanges++;
                        details.add("public 方法新增/删除: " + methodKey);
                    } else {
                        counters.implementationChanges++;
                        details.add("方法新增/删除: " + methodKey);
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
     * 创建 Kotlin PSI 文件
     * <p> 仅在 Kotlin 插件存在时返回 KtFile，否则返回 null。
     */
    @Nullable
    private PsiFile createPsiFile(@NotNull Project project,
                                  @NotNull String fileName,
                                  @NotNull String content) {
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        PsiFile psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, fileType, content, System.currentTimeMillis(), false);
        return isKotlinFile(psiFile) ? psiFile : null;
    }

    /**
     * 判断是否为 Kotlin 文件
     */
    private boolean isKotlinFile(@Nullable PsiFile file) {
        return file != null && KT_FILE.equals(file.getClass().getName());
    }

    /**
     * 在 PSI 树中查找第一个声明节点
     */
    @Nullable
    private PsiElement findFirstDeclaration(@NotNull PsiFile file) {
        for (PsiElement child : file.getChildren()) {
            if (KT_CLASS.equals(child.getClass().getName())
                || KT_FUNCTION.equals(child.getClass().getName())
                || KT_PROPERTY.equals(child.getClass().getName())) {
                return child;
            }
        }
        return null;
    }

    /**
     * 根据行号定位函数节点
     */
    @Nullable
    private PsiElement findFunctionAtLine(@NotNull PsiFile file,
                                          @NotNull String content,
                                          int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, KT_FUNCTION) : null;
    }

    /**
     * 根据行号定位属性节点
     */
    @Nullable
    private PsiElement findPropertyAtLine(@NotNull PsiFile file,
                                          @NotNull String content,
                                          int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, KT_PROPERTY) : null;
    }

    /**
     * 根据行号定位类节点
     */
    @Nullable
    private PsiElement findClassAtLine(@NotNull PsiFile file,
                                       @NotNull String content,
                                       int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, KT_CLASS) : null;
    }

    /**
     * 根据行号定位 PSI 元素
     */
    @Nullable
    private PsiElement findElementAtLine(@NotNull PsiFile file,
                                         @NotNull String content,
                                         int line) {
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
     * 通过类名向上查找父节点
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
     * 计算指定行的起始偏移
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
     * 构建函数签名文本
     */
    @NotNull
    private String buildFunctionSignature(@NotNull PsiElement function) {
        String name = getName(function);
        String params = getParameterListText(function);
        return name != null ? name + params : params;
    }

    /**
     * 构建函数标识
     */
    @NotNull
    private String buildFunctionKey(@NotNull PsiElement function) {
        PsiElement clazz = findParentByClassName(function, KT_CLASS);
        String className = clazz != null ? getName(clazz) : null;
        String sig = buildFunctionSignature(function);
        return className != null ? className + "#" + sig : sig;
    }

    /**
     * 构建属性标识
     */
    @NotNull
    private String buildPropertyKey(@NotNull PsiElement property) {
        PsiElement clazz = findParentByClassName(property, KT_CLASS);
        String className = clazz != null ? getName(clazz) : null;
        String name = getName(property);
        return className != null ? className + "#" + name : name;
    }

    /**
     * 构建类标识
     */
    @NotNull
    private String buildClassKey(@NotNull PsiElement clazz) {
        String name = getName(clazz);
        return name != null ? name : "AnonymousClass";
    }

    /**
     * 判断是否为对外 API
     */
    private boolean isPublicApi(@NotNull PsiElement element) {
        String visibility = getVisibility(element);
        return "public".equals(visibility) || "protected".equals(visibility);
    }

    /**
     * 判断方法签名是否变化
     */
    private boolean isSignatureChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeSig = buildFunctionSignature(beforeFunction) + ":" + getReturnTypeText(beforeFunction);
        String afterSig = buildFunctionSignature(afterFunction) + ":" + getReturnTypeText(afterFunction);
        return !beforeSig.equals(afterSig);
    }

    /**
     * 判断可见性是否变化
     */
    private boolean isVisibilityChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        return !getVisibility(beforeFunction).equals(getVisibility(afterFunction));
    }

    /**
     * 判断返回类型是否变化
     */
    private boolean isReturnTypeChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        return !getReturnTypeText(beforeFunction).equals(getReturnTypeText(afterFunction));
    }

    /**
     * 判断方法体是否变化
     */
    private boolean isBodyChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeBody = getBodyText(beforeFunction);
        String afterBody = getBodyText(afterFunction);
        return beforeBody != null && afterBody != null && !beforeBody.equals(afterBody);
    }

    /**
     * 判断是否发生行为变化
     */
    private boolean isBehaviorChanged(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeBody = getBodyText(beforeFunction);
        String afterBody = getBodyText(afterFunction);
        if (beforeBody == null || afterBody == null) {
            return false;
        }
        int beforeIf = countNodesByClassName(beforeFunction, KT_IF);
        int afterIf = countNodesByClassName(afterFunction, KT_IF);
        int beforeWhen = countNodesByClassName(beforeFunction, KT_WHEN);
        int afterWhen = countNodesByClassName(afterFunction, KT_WHEN);
        int beforeReturn = countNodesByClassName(beforeFunction, KT_RETURN);
        int afterReturn = countNodesByClassName(afterFunction, KT_RETURN);
        int beforeThrow = countNodesByClassName(beforeFunction, KT_THROW);
        int afterThrow = countNodesByClassName(afterFunction, KT_THROW);
        int beforeCall = countNodesByClassName(beforeFunction, KT_CALL);
        int afterCall = countNodesByClassName(afterFunction, KT_CALL);
        return beforeIf != afterIf
               || beforeWhen != afterWhen
               || beforeReturn != afterReturn
               || beforeThrow != afterThrow
               || beforeCall != afterCall;
    }

    /**
     * 判断是否为重构调整（仅排版/细节变化）
     */
    private boolean isRefactorChange(@NotNull PsiElement beforeFunction, @NotNull PsiElement afterFunction) {
        String beforeBody = getBodyText(beforeFunction);
        String afterBody = getBodyText(afterFunction);
        if (beforeBody == null || afterBody == null) {
            return false;
        }
        return beforeBody.replaceAll("\\s+", "").equals(afterBody.replaceAll("\\s+", ""));
    }

    /**
     * 判断类签名变化（可见性/继承）
     */
    private boolean isClassSignatureChanged(@NotNull PsiElement beforeClass, @NotNull PsiElement afterClass) {
        if (!getVisibility(beforeClass).equals(getVisibility(afterClass))) {
            return true;
        }
        String beforeSupers = getSuperTypeListText(beforeClass);
        String afterSupers = getSuperTypeListText(afterClass);
        return !beforeSupers.equals(afterSupers);
    }

    /**
     * 判断注解变化
     */
    private boolean isAnnotationChanged(@NotNull PsiElement beforeClass, @NotNull PsiElement afterClass) {
        return !getAnnotationText(beforeClass).equals(getAnnotationText(afterClass));
    }

    /**
     * 判断属性默认值变化
     */
    private boolean isPropertyInitializerChanged(@NotNull PsiElement beforeProperty, @NotNull PsiElement afterProperty) {
        String beforeInit = getPropertyInitializerText(beforeProperty);
        String afterInit = getPropertyInitializerText(afterProperty);
        return !beforeInit.equals(afterInit);
    }

    /**
     * 统计子节点中指定类名的数量
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
     * 获取函数体文本
     */
    @Nullable
    private String getBodyText(@NotNull PsiElement function) {
        Object body = invoke(function, "getBodyExpression");
        return body != null ? invokeText(body) : null;
    }

    /**
     * 获取参数列表文本
     */
    @NotNull
    private String getParameterListText(@NotNull PsiElement function) {
        Object params = invoke(function, "getValueParameterList");
        String text = params != null ? invokeText(params) : null;
        return text != null ? text : "()";
    }

    /**
     * 获取函数返回类型文本
     */
    @NotNull
    private String getReturnTypeText(@NotNull PsiElement function) {
        Object typeRef = invoke(function, "getTypeReference");
        String text = typeRef != null ? invokeText(typeRef) : null;
        return text != null ? text : "";
    }

    /**
     * 获取可见性标识
     */
    @NotNull
    private String getVisibility(@NotNull PsiElement element) {
        String modifiers = getModifierText(element);
        if (modifiers.contains("private")) {
            return "private";
        }
        if (modifiers.contains("protected")) {
            return "protected";
        }
        if (modifiers.contains("internal")) {
            return "internal";
        }
        return "public";
    }

    /**
     * 获取修饰符文本
     */
    @NotNull
    private String getModifierText(@NotNull PsiElement element) {
        Object modifierList = invoke(element, "getModifierList");
        String text = modifierList != null ? invokeText(modifierList) : null;
        return text != null ? text.toLowerCase(Locale.ROOT) : "";
    }

    /**
     * 获取注解文本
     */
    @NotNull
    private String getAnnotationText(@NotNull PsiElement element) {
        Object modifierList = invoke(element, "getModifierList");
        String text = modifierList != null ? invokeText(modifierList) : null;
        return text != null ? text : "";
    }

    /**
     * 获取类继承列表文本
     */
    @NotNull
    private String getSuperTypeListText(@NotNull PsiElement element) {
        Object list = invoke(element, "getSuperTypeList");
        String text = list != null ? invokeText(list) : null;
        return text != null ? text : "";
    }

    /**
     * 获取属性初始化表达式文本
     */
    @NotNull
    private String getPropertyInitializerText(@NotNull PsiElement property) {
        Object initializer = invoke(property, "getInitializer");
        String text = initializer != null ? invokeText(initializer) : null;
        return text != null ? text : "";
    }

    /**
     * 获取声明名称
     */
    @Nullable
    private String getName(@NotNull PsiElement element) {
        Object name = invoke(element, "getName");
        return name instanceof String ? (String) name : null;
    }

    /**
     * 统一反射调用
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
     * 获取 PSI 元素文本
     */
    @Nullable
    private String invokeText(@NotNull Object target) {
        Object text = invoke(target, "getText");
        return text instanceof String ? (String) text : null;
    }

    /**
     * 输出语义摘要
     */
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
        if (counters.annotationChanges > 0) {
            summary.append("- 注解：").append(counters.annotationChanges).append(" 处注解变化\n");
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
     * 语义统计计数器
     */
    private static class SemanticCounters {
        /** API 签名变更计数器, 记录接口签名相关的语义变化次数 */
        int apiSignatureChanges;
        /** 实现层面的变更计数 */
        int implementationChanges;
        /** 表示行为变更的计数器 */
        int behaviorChanges;
        /** 重构变更计数, 记录代码结构上的修改次数 */
        int refactorChanges;
        /** 类变更计数器, 用于统计类级别的代码变更数量 */
        int classChanges;
        /**
         * 字段变更计数
         * <p> 表示在语义分析中检测到的字段变更次数
         */
        int fieldChanges;
        /** 注解变更数量 */
        int annotationChanges;

        /**
         * 判断当前对象是否为空
         * <p> 检查所有计数器是否均为 0, 若全部为 0 则返回 true, 表示没有发生任何变化
         *
         * @return 如果所有计数器均为 0, 则返回 true, 否则返回 false
         */
        boolean isEmpty() {
            return apiSignatureChanges == 0
                   && implementationChanges == 0
                   && behaviorChanges == 0
                   && refactorChanges == 0
                   && classChanges == 0
                   && fieldChanges == 0
                   && annotationChanges == 0;
        }
    }
}
