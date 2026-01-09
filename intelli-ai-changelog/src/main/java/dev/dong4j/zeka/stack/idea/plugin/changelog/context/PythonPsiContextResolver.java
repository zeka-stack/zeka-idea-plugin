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
    /** Python 文件的扩展名 */
    private static final String PY_EXT = "py";

    /** Python PSI 关键类名 */
    private static final String PY_FILE = "com.jetbrains.python.psi.PyFile";
    /** Python 类 PSI 的全限定类名 */
    private static final String PY_CLASS = "com.jetbrains.python.psi.PyClass";
    /** Python 函数 PSI 类名全限定路径 */
    private static final String PY_FUNCTION = "com.jetbrains.python.psi.PyFunction";
    /** Python PSI 中的属性或变量声明表达式类名 */
    private static final String PY_TARGET = "com.jetbrains.python.psi.PyTargetExpression";
    /** Python if 语句 PSI 类名 */
    private static final String PY_IF = "com.jetbrains.python.psi.PyIfStatement";
    /** Python for 循环语句 PSI 类名 */
    private static final String PY_FOR = "com.jetbrains.python.psi.PyForStatement";
    /** Python 循环语句节点类名 */
    private static final String PY_WHILE = "com.jetbrains.python.psi.PyWhileStatement";
    /** Python 返回语句 PSI 类全限定名 */
    private static final String PY_RETURN = "com.jetbrains.python.psi.PyReturnStatement";
    /** Python 抛出语句类名 */
    private static final String PY_RAISE = "com.jetbrains.python.psi.PyRaiseStatement";
    /** Python 函数调用表达式类全限定名 */
    private static final String PY_CALL = "com.jetbrains.python.psi.PyCallExpression";

    /**
     * 判断是否支持指定文件的语法上下文解析
     * <p> 通过文件扩展名判断是否为 Python 文件 (.py), 支持大小写不敏感比较
     *
     * @param file 要判断的虚拟文件, 不能为 null
     * @return 如果文件扩展名为 "py"(不区分大小写), 则返回 true, 否则返回 false
     */
    @Override
    public boolean supports(@NotNull VirtualFile file) {
        return PY_EXT.equalsIgnoreCase(file.getExtension());
    }

    /**
     * 根据文件和光标位置解析上下文标识符
     * <p> 通过分析指定文件中光标所在行的代码元素, 向上查找最近的函数, 类或属性声明, 并返回其完整标识符 (如类名 #函数名, 属性名等).
     * <p> 支持的标识符格式:
     * <ul>
     *   <li> 函数标识符: 若找到函数, 返回格式为 <code> 类名 #函数名 </code> 或仅函数名 (无类时)</li>
     *   <li> 属性标识符: 若找到属性, 返回格式为 <code> 类名 #属性名 </code> 或仅属性名 (无类时)</li>
     *   <li> 类标识符: 若未找到函数或属性, 仅返回类名 </li>
     * </ul>
     * <p> 若未找到任何有效标识符或文件不支持, 则返回 null.
     *
     * @param file          要解析的虚拟文件, 不能为 null
     * @param preferredLine 优先查找的行号, 若无效则使用 fallbackLine
     * @param fallbackLine  备用行号, 当 preferredLine 无效时使用
     * @return 上下文标识符字符串, 如 "ClassName#methodName", 或 null(未找到有效标识符)
     */
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

    /**
     * 解析并返回文件中的主要符号名称
     * <p> 在指定的项目和虚拟文件中查找第一个声明的类或函数, 并返回其名称.
     *
     * @param project 项目实例, 不能为 null
     * @param file    虚拟文件, 表示要解析的 Python 文件, 不能为 null
     * @return 返回文件中第一个声明的类或函数的名称, 如果未找到有效名称则返回 null
     */
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

    /**
     * 解析语义变更摘要, 用于比较代码前后版本的语义变化
     * <p> 该方法通过分析指定文件在前后内容中的变更, 生成语义变更总结, 包括类, 属性, 函数等层面的变化.
     *
     * @param project       项目对象, 用于获取 PSI 文件和执行读写操作
     * @param file          要解析的文件对象
     * @param beforeContent 前版本文件内容
     * @param afterContent  后版本文件内容
     * @param fragments     行片段列表, 包含需要分析的代码行范围
     * @return 语义变更摘要字符串, 如果未检测到变更则返回 null
     * @since 1.0
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

    /**
     * 根据文件名和内容创建 PsiFile 对象
     * <p> 该方法用于根据指定的文件名和内容创建一个 PsiFile 实例, 并检查其是否为 Python 文件.
     *
     * @param project  项目对象, 不能为 null
     * @param fileName 文件名, 不能为 null
     * @param content  文件内容, 不能为 null
     * @return 如果文件是 Python 文件, 则返回对应的 PsiFile 对象; 否则返回 null
     */
    @Nullable
    private PsiFile createPsiFile(@NotNull Project project, @NotNull String fileName, @NotNull String content) {
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        PsiFile psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, fileType, content, System.currentTimeMillis(), false);
        return isPythonFile(psiFile) ? psiFile : null;
    }

    /**
     * 判断给定的 PsiFile 是否为 Python 文件
     * <p> 通过检查文件的类名是否与预定义的 Python 文件类名一致来判断
     *
     * @param file 要检查的 PsiFile 对象, 可能为 null
     * @return 如果是 Python 文件返回 true, 否则返回 false
     */
    private boolean isPythonFile(@Nullable PsiFile file) {
        return file != null && PY_FILE.equals(file.getClass().getName());
    }

    /**
     * 查找文件中的第一个类或函数声明
     * <p> 遍历文件的子元素, 找到第一个类或函数声明并返回对应的 PsiElement 对象
     *
     * @param file 要查找的 Psi 文件
     * @return 第一个类或函数声明的 PsiElement 对象, 如果没有找到则返回 null
     */
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

    /**
     * 在给定的 Python 文件内容中查找指定行号的函数定义
     * <p> 通过查找指定行的元素, 并向上查找其父节点, 确定该行是否属于一个函数定义
     *
     * @param file    PsiFile 对象, 表示 Python 文件
     * @param content 文件内容字符串
     * @param line    目标行号, 从 0 开始计数
     * @return 如果找到函数定义, 则返回对应的 PsiElement; 否则返回 null
     */
    @Nullable
    private PsiElement findFunctionAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, PY_FUNCTION) : null;
    }

    /**
     * 根据指定行号在 Python 文件中查找类元素
     * <p> 通过行号定位文件中的元素, 然后向上查找其父节点是否为类节点 (PyClass)
     *
     * @param file    Python 文件对象, 不能为 null
     * @param content 文件内容字符串, 不能为 null
     * @param line    目标行号, 必须大于等于 0
     * @return 如果找到对应元素且其父节点为类, 则返回该类元素; 否则返回 null
     */
    @Nullable
    private PsiElement findClassAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, PY_CLASS) : null;
    }

    /**
     * 在指定行查找字段元素
     * <p> 通过文件内容和行号定位到具体的 PSI 元素, 并尝试向上查找类型为 PyTargetExpression 的字段元素
     *
     * @param file    PSI 文件对象
     * @param content 文件内容字符串
     * @param line    要查找的行号 (从 0 开始)
     * @return 查找到的字段元素, 若未找到则返回 null
     */
    @Nullable
    private PsiElement findFieldAtLine(@NotNull PsiFile file, @NotNull String content, int line) {
        PsiElement element = findElementAtLine(file, content, line);
        return element != null ? findParentByClassName(element, PY_TARGET) : null;
    }

    /**
     * 根据行号在内容中查找对应的 PSI 元素
     * <p> 通过指定行号计算起始偏移量, 并在 PSI 文件中查找该偏移位置对应的元素
     * <p> 使用示例:
     * <pre>{@code
     * PsiElement element = findElementAtLine(psiFile, content, 5);
     * }</pre>
     *
     * @param file    PSI 文件对象, 不能为 null
     * @param content 文件内容字符串, 不能为 null
     * @param line    目标行号, 必须大于等于 0
     * @return 对应偏移位置的 PSI 元素, 如果行号无效或偏移量计算失败则返回 null
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
     * 根据元素类名查找其父级元素
     * <p> 从当前元素开始向上遍历, 查找第一个类名为指定值的父级元素.
     *
     * @param element   当前元素, 不能为 null
     * @param className 要查找的父级元素的类名, 不能为 null
     * @return 如果找到匹配的父级元素则返回, 否则返回 null
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
     * <p> 从给定的文本内容中计算指定行的起始偏移量. 如果指定的行号超出文本范围, 则返回 - 1.
     *
     * @param content 文本内容
     * @param line    行号, 从 0 开始计数
     * @return 指定行的起始偏移量, 如果行号超出范围则返回 - 1
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
     * 构建函数的签名字符串
     * <p> 通过获取函数名称和参数列表, 拼接成标准的函数签名格式
     *
     * @param function 要构建签名的函数元素 (PsiElement)
     * @return 函数签名字符串, 格式为 "函数名 (参数列表)", 如果无函数名则返回仅参数列表
     */
    @NotNull
    private String buildFunctionSignature(@NotNull PsiElement function) {
        String name = getName(function);
        String params = getParameterListText(function);
        return name != null ? name + params : params;
    }

    /**
     * 根据函数元素构建唯一函数键
     * <p> 通过查找函数的所属类并拼接类名与函数签名, 生成用于标识函数的唯一键
     * <p> 若函数无所属类, 则仅返回函数签名
     *
     * @param function 函数元素, 不能为空
     * @return 构建的函数键, 格式为 "类名 #函数签名" 或仅函数签名
     */
    @NotNull
    private String buildFunctionKey(@NotNull PsiElement function) {
        PsiElement clazz = findParentByClassName(function, PY_CLASS);
        String className = clazz != null ? getName(clazz) : null;
        String sig = buildFunctionSignature(function);
        return className != null ? className + "#" + sig : sig;
    }

    /**
     * 构建类的关键字标识符
     * <p> 根据给定的 PsiElement 获取类的名称, 如果名称为空, 则返回 "AnonymousClass"
     *
     * @param clazz 要构建关键字的 PsiElement 对象
     * @return 类的名称, 如果名称为空则返回 "AnonymousClass"
     */
    @NotNull
    private String buildClassKey(@NotNull PsiElement clazz) {
        String name = getName(clazz);
        return name != null ? name : "AnonymousClass";
    }

    /**
     * 根据字段元素构建字段键名
     * <p> 从字段元素中获取名称, 若名称为空则返回默认的匿名字段标识符 "anonymousField"
     *
     * @param field 字段元素, 不能为 null
     * @return 字段的键名, 若名称为空则返回 "anonymousField"
     */
    @NotNull
    private String buildFieldKey(@NotNull PsiElement field) {
        String name = getName(field);
        return name != null ? name : "anonymousField";
    }

    /**
     * 判断两个函数的签名是否发生变化
     * <p> 通过比较两个函数的签名字符串, 判断其是否发生改变. 若签名不同, 则返回 true.
     *
     * @param beforeFunc 修改前的函数元素
     * @param afterFunc  修改后的函数元素
     * @return 如果函数签名发生变化, 返回 true; 否则返回 false
     */
    private boolean isSignatureChanged(@NotNull PsiElement beforeFunc, @NotNull PsiElement afterFunc) {
        return !buildFunctionSignature(beforeFunc).equals(buildFunctionSignature(afterFunc));
    }

    /**
     * 检查函数体是否发生变化
     * <p> 比较修改前后两个函数的文本内容, 判断函数实现是否发生了改变
     *
     * @param beforeFunc 修改前的函数 PSI 元素, 不能为 null
     * @param afterFunc  修改后的函数 PSI 元素, 不能为 null
     * @return 如果函数体文本内容不同则返回 true, 否则返回 false
     */
    private boolean isBodyChanged(@NotNull PsiElement beforeFunc, @NotNull PsiElement afterFunc) {
        String beforeText = invokeText(beforeFunc);
        String afterText = invokeText(afterFunc);
        return beforeText != null && afterText != null && !beforeText.equals(afterText);
    }

    /**
     * 判断两个函数的行为是否发生变化
     * <p> 通过比较两个函数中控制流语句的数量来判断行为是否发生变化
     * <p> 比较的控制流语句包括:if 语句,for 循环,while 循环,return 语句,raise 语句和函数调用
     *
     * @param beforeFunc 修改前的函数 PSI 元素, 不能为 null
     * @param afterFunc  修改后的函数 PSI 元素, 不能为 null
     * @return 如果控制流语句的数量发生变化则返回 true, 否则返回 false
     */
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

    /**
     * 判断两个函数是否为重构变更
     * <p>通过比较两个函数的文本内容 (去除空白字符后) 是否完全相同来判断是否属于重构变更.
     * 如果文本内容仅在格式上有差异但逻辑一致, 则认为是重构而非行为变更.
     *
     * @param beforeFunc 变更前的函数元素
     * @param afterFunc  变更后的函数元素
     * @return 如果两个函数的逻辑内容相同, 仅格式不同, 则返回 true, 表示是重构; 否则返回 false
     */
    private boolean isRefactorChange(@NotNull PsiElement beforeFunc, @NotNull PsiElement afterFunc) {
        String beforeText = invokeText(beforeFunc);
        String afterText = invokeText(afterFunc);
        if (beforeText == null || afterText == null) {
            return false;
        }
        return beforeText.replaceAll("\\s+", "").equals(afterText.replaceAll("\\s+", ""));
    }

    /**
     * 递归统计指定类名的 PSI 元素数量
     * <p> 从给定的根节点开始, 遍历所有子节点, 统计其类名与指定名称匹配的元素总数
     *
     * @param root      要遍历的根 PSI 元素
     * @param className 要统计的目标类名 (全限定类名)
     * @return 匹配类名的元素总数
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
     * 判断给定的名称是否为公共 API 名称
     * <p> 根据命名规范, 判断一个名称是否可以被认为是公共 API 的一部分.
     * 公共 API 名称的第一个字符不是下划线, 并且是一个字母.
     *
     * @param name 要检查的名称
     * @return 如果名称符合公共 API 的命名规范, 则返回 true; 否则返回 false
     */
    private boolean isPublicApi(@NotNull String name) {
        if (name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        return first != '_' && Character.isLetter(first);
    }

    /**
     * 获取指定 PSI 元素的名称
     * <p> 通过反射调用 PSI 元素的 getName 方法获取其名称, 如果返回值不是字符串类型, 则返回 null
     *
     * @param element PSI 元素对象, 不能为空
     * @return 元素的名称, 如果名称不是字符串类型或为空, 则返回 null
     */
    @Nullable
    private String getName(@NotNull PsiElement element) {
        Object name = invoke(element, "getName");
        return name instanceof String ? (String) name : null;
    }

    /**
     * 获取函数参数列表的文本表示
     * <p> 通过反射调用函数元素的 getParameterList 方法, 再获取其文本表示, 若参数为空则返回 "()".
     * <p> 使用示例:
     * <pre>{@code
     * String paramText = getParameterListText(myFunctionElement);
     * // 若参数列表为空, 返回 "()", 否则返回如 "(arg1, arg2)" 的字符串
     * }</pre>
     *
     * @param function 函数元素, 不能为 null
     * @return 参数列表的文本表示, 若参数为空则返回 "()", 若反射失败则返回 null
     */
    @NotNull
    private String getParameterListText(@NotNull PsiElement function) {
        Object params = invoke(function, "getParameterList");
        String text = params != null ? invokeText(params) : null;
        return text != null ? text : "()";
    }

    /**
     * 通过反射调用目标对象的方法
     * <p> 根据指定的方法名在目标对象上查找并调用对应方法, 若调用失败则返回 null
     * <p> 示例:
     * <pre>{@code
     * Object result = invoke(myObject, "getUserName");
     * }</pre>
     *
     * @param target     目标对象, 不能为 null
     * @param methodName 方法名, 不能为 null
     * @return 方法调用结果, 若调用失败或方法不存在则返回 null
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
     * 获取指定对象的文本内容
     * <p> 调用目标对象的 getText 方法并返回其文本内容, 若方法调用失败或返回值不是字符串类型, 则返回 null.
     *
     * @param target 目标对象, 不能为 null
     * @return 目标对象的文本内容, 若获取失败则返回 null
     */
    @Nullable
    private String invokeText(@NotNull Object target) {
        Object text = invoke(target, "getText");
        return text instanceof String ? (String) text : null;
    }

    /**
     * 构建语义变更总结信息
     * <p> 根据传入的变更计数器和详细变更列表, 生成结构化的语义变更总结字符串.
     *
     * @param counters 变更计数器对象, 包含各类变更的数量统计
     * @param details  详细的变更信息列表, 用于展示具体变更内容
     * @return 格式化后的语义变更总结字符串
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
     * 语义变更计数器类
     * <p> 用于统计代码在不同维度上的语义变化数量, 包括 API 签名变更, 实现变更, 行为变更, 重构变更, 类变更和字段变更等
     * <p> 该类通常用于版本对比或代码质量分析工具中, 帮助判断代码变更的类型和影响范围
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2026.01.09
     * @since 1.0.0
     */
    private static class SemanticCounters {
        /** API 签名变更次数 */
        int apiSignatureChanges;
        /**
         * 实现变更计数
         * <p> 表示实现层面的变更次数
         */
        int implementationChanges;
        /**
         * 行为变化计数
         * <p> 表示在当前语义分析中行为发生变化的次数
         */
        int behaviorChanges;
        /** 重构相关变更计数 */
        int refactorChanges;
        /** 类变更数量 */
        int classChanges;
        /** 记录字段变更的数量 */
        int fieldChanges;

        /**
         * 检查当前对象是否为空
         * <p> 判断所有的变更计数器是否都为 0, 如果都是 0 则表示没有发生任何变更, 返回 true; 否则返回 false.
         *
         * @return 如果所有变更计数器均为 0, 则返回 true, 表示当前对象为空; 否则返回 false
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
