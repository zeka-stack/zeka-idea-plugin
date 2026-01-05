package dev.dong4j.zeka.stack.idea.plugin.changelog.context;

import com.intellij.diff.fragments.LineFragment;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiThrowStatement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.DocumentUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Java 语法上下文解析器 (依赖 com.intellij.java) */
public class JavaPsiContextResolver implements LanguageContextResolver {
    /**
     * 判断文件是否为 Java 语言文件
     * <p> 通过检查文件扩展名是否为 "java" 来判断该文件是否属于 Java 语言.
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
     * <p> 根据文件路径, 行号和其他信息, 尝试解析出该位置对应的类, 方法或字段名称.
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

    /**
     * 基于 PSI 生成语义摘要
     * <p> 该实现不会替代 diff, 而是将变更归因到方法 / 类级别, 输出简短语义总结.
     *
     * @param project       项目实例, 不能为 null
     * @param file          文件对象, 不能为 null
     * @param beforeContent 修改前的文件内容
     * @param afterContent  修改后的文件内容
     * @param fragments     变更片段列表, 用于定位具体修改位置
     * @return 包含变更语义总结的字符串, 若无变更或发生错误则返回 null
     */
    @Override
    public @Nullable String resolveSemanticSummary(@NotNull Project project,
                                                   @NotNull VirtualFile file,
                                                   @NotNull String beforeContent,
                                                   @NotNull String afterContent,
                                                   @NotNull List<LineFragment> fragments) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (project.isDisposed() || fragments.isEmpty()) {
                return null;
            }
            PsiJavaFile beforeFile = createPsiFile(project, file.getName(), beforeContent);
            PsiJavaFile afterFile = createPsiFile(project, file.getName(), afterContent);
            if (beforeFile == null || afterFile == null) {
                return null;
            }

            SemanticCounters counters = new SemanticCounters();
            List<String> details = new ArrayList<>();
            Set<String> processedMethods = new HashSet<>();
            Set<String> processedFields = new HashSet<>();
            Set<String> processedClasses = new HashSet<>();

            for (LineFragment fragment : fragments) {
                PsiMethod beforeMethod = findMethodAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiMethod afterMethod = findMethodAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiField beforeField = findFieldAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiField afterField = findFieldAtLine(afterFile, afterContent, fragment.getStartLine2());
                PsiClass beforeClass = findClassAtLine(beforeFile, beforeContent, fragment.getStartLine1());
                PsiClass afterClass = findClassAtLine(afterFile, afterContent, fragment.getStartLine2());

                if (beforeMethod == null && afterMethod == null) {
                    if (beforeField != null || afterField != null) {
                        PsiField primaryField = afterField != null ? afterField : beforeField;
                        String fieldKey = buildFieldKey(primaryField);
                        if (processedFields.add(fieldKey)) {
                            boolean isApi = primaryField != null && isPublicApi(primaryField);
                            counters.fieldChanges++;
                            if (isApi) {
                                counters.apiSignatureChanges++;
                                details.add("public 字段变更: " + fieldKey);
                            } else {
                                details.add("字段变更: " + fieldKey);
                            }
                            if (beforeField != null && afterField != null && isFieldInitializerChanged(beforeField, afterField)) {
                                counters.implementationChanges++;
                                details.add("字段默认值变化: " + fieldKey);
                            }
                        }
                    } else if (beforeClass != null || afterClass != null) {
                        PsiClass primaryClass = afterClass != null ? afterClass : beforeClass;
                        String classKey = buildClassKey(primaryClass);
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
                PsiMethod primary = afterMethod != null ? afterMethod : beforeMethod;
                String methodKey = buildMethodKey(primary);
                if (!processedMethods.add(methodKey)) {
                    continue;
                }

                if (beforeMethod != null && afterMethod != null) {
                    if (isPublicApi(afterMethod) && isSignatureChanged(beforeMethod, afterMethod)) {
                        counters.apiSignatureChanges++;
                        details.add("public 方法签名变更: " + methodKey);
                        continue;
                    }
                    if (isVisibilityChanged(beforeMethod, afterMethod)) {
                        counters.apiSignatureChanges++;
                        details.add("方法可见性变更: " + methodKey);
                    }
                    if (isReturnTypeChanged(beforeMethod, afterMethod)) {
                        counters.apiSignatureChanges++;
                        details.add("返回值类型变更: " + methodKey);
                    }
                    if (isThrowsChanged(beforeMethod, afterMethod)) {
                        counters.apiSignatureChanges++;
                        details.add("throws 声明变更: " + methodKey);
                    }
                    if (isBodyChanged(beforeMethod, afterMethod)) {
                        if (isBehaviorChanged(beforeMethod.getBody(), afterMethod.getBody())) {
                            counters.behaviorChanges++;
                            details.add("行为变化: " + methodKey);
                        } else if (isRefactorChange(beforeMethod.getBody(), afterMethod.getBody())) {
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
     * 创建 PsiJavaFile(基于文本快照)
     * <p>
     * 该方法根据给定的项目, 文件名和内容创建一个 PsiJavaFile 对象.
     *
     * @param project  项目实例, 不能为空
     * @param fileName 文件名, 不能为空
     * @param content  文件内容, 不能为空
     * @return 创建的 PsiJavaFile 对象, 如果创建失败则返回 null
     */
    @Nullable
    private PsiJavaFile createPsiFile(@NotNull Project project,
                                      @NotNull String fileName,
                                      @NotNull String content) {
        PsiFile psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText(fileName, JavaFileType.INSTANCE, content, System.currentTimeMillis(), false);
        return psiFile instanceof PsiJavaFile javaFile ? javaFile : null;
    }

    /**
     * 根据行号定位方法节点
     *
     * @param psiFile 要解析的 PsiJavaFile 对象
     * @param content 文件内容
     * @param line    行号
     * @return 如果在指定行找到方法节点, 则返回该方法; 否则返回 null
     */
    @Nullable
    private PsiMethod findMethodAtLine(@NotNull PsiJavaFile psiFile,
                                       @NotNull String content,
                                       int line) {
        if (line < 0) {
            return null;
        }
        int offset = lineStartOffset(content, line);
        if (offset < 0) {
            return null;
        }
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? PsiTreeUtil.getParentOfType(element, PsiMethod.class, false) : null;
    }

    /**
     * 根据行号定位字段节点
     * <p> 通过计算指定行号对应的文本偏移量, 在 PsiJavaFile 中查找该位置的元素, 并返回其父级字段节点 (如果存在).
     *
     * @param psiFile 要搜索的 Java 文件对象, 不能为空
     * @param content 文件内容字符串, 不能为空
     * @param line    行号, 必须为非负整数
     * @return 找到的字段节点, 如果未找到或参数无效则返回 null
     */
    @Nullable
    private PsiField findFieldAtLine(@NotNull PsiJavaFile psiFile,
                                     @NotNull String content,
                                     int line) {
        if (line < 0) {
            return null;
        }
        int offset = lineStartOffset(content, line);
        if (offset < 0) {
            return null;
        }
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? PsiTreeUtil.getParentOfType(element, PsiField.class, false) : null;
    }

    /**
     * 根据行号定位类节点
     * <p> 通过计算指定行号对应的文本偏移量, 在 Psi 文件中查找该位置的元素, 并返回其父类节点 (如果存在).
     *
     * @param psiFile 要搜索的 PsiJavaFile 对象, 不能为空
     * @param content 文件内容字符串, 不能为空
     * @param line    行号, 必须大于等于 0
     * @return 如果找到对应的类节点, 则返回该节点; 否则返回 null
     */
    @Nullable
    private PsiClass findClassAtLine(@NotNull PsiJavaFile psiFile,
                                     @NotNull String content,
                                     int line) {
        if (line < 0) {
            return null;
        }
        int offset = lineStartOffset(content, line);
        if (offset < 0) {
            return null;
        }
        PsiElement element = psiFile.findElementAt(offset);
        return element != null ? PsiTreeUtil.getParentOfType(element, PsiClass.class, false) : null;
    }

    /**
     * 将行号转换为文本偏移
     * <p> 根据指定的行号计算其在文本内容中的起始偏移位置
     *
     * @param content 要查找的文本内容
     * @param line    目标行号 (从 0 开始)
     * @return 如果行号有效, 则返回该行在文本中的起始偏移; 否则返回 -1
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
            char ch = content.charAt(offset++);
            if (ch == '\n') {
                currentLine++;
            }
        }
        return currentLine == line ? offset : -1;
    }

    /**
     * 判断方法是否属于公共 API
     *
     * @param method 要判断的方法对象
     * @return 如果方法是 public 或 protected 修饰, 则返回 true, 否则返回 false
     */
    private boolean isPublicApi(@NotNull PsiMethod method) {
        return method.hasModifierProperty(PsiModifier.PUBLIC)
               || method.hasModifierProperty(PsiModifier.PROTECTED);
    }

    /**
     * 判断字段是否属于对外 API
     * <p> 如果字段具有 public 或 protected 修饰符, 则认为是对外 API
     *
     * @param field 字段对象, 不能为空
     * @return 如果字段具有 public 或 protected 修饰符, 则返回 true; 否则返回 false
     */
    private boolean isPublicApi(@NotNull PsiField field) {
        return field.hasModifierProperty(PsiModifier.PUBLIC)
               || field.hasModifierProperty(PsiModifier.PROTECTED);
    }

    /**
     * 判断方法可见性是否发生变化
     * <p> 比较两个方法的可见性 (public, protected, package-private, private), 如果可见性不同则返回 true.
     *
     * @param beforeMethod 原始方法对象
     * @param afterMethod  修改后的方法对象
     * @return 如果方法可见性发生变化, 返回 true; 否则返回 false
     */
    private boolean isVisibilityChanged(@NotNull PsiMethod beforeMethod, @NotNull PsiMethod afterMethod) {
        return !getVisibility(beforeMethod).equals(getVisibility(afterMethod));
    }

    /**
     * 获取方法的可见性文本表示
     * <p> 根据方法的修饰符属性返回对应的可见性字符串 </p>
     * <p> 支持的可见性包括:public,protected,private 和 package-private</p>
     *
     * @param method 方法对象, 不能为空
     * @return 方法的可见性文本, 如 "public","protected","private" 或 "package-private"
     */
    @NotNull
    private String getVisibility(@NotNull PsiMethod method) {
        if (method.hasModifierProperty(PsiModifier.PUBLIC)) {
            return "public";
        }
        if (method.hasModifierProperty(PsiModifier.PROTECTED)) {
            return "protected";
        }
        if (method.hasModifierProperty(PsiModifier.PRIVATE)) {
            return "private";
        }
        return "package-private";
    }

    /**
     * 判断方法返回类型是否发生变化
     *
     * @param beforeMethod 方法变更前的 PsiMethod 对象
     * @param afterMethod  方法变更后的 PsiMethod 对象
     * @return 如果两个方法的返回类型不同, 则返回 true; 否则返回 false
     */
    private boolean isReturnTypeChanged(@NotNull PsiMethod beforeMethod, @NotNull PsiMethod afterMethod) {
        String beforeType = beforeMethod.getReturnType() != null ? beforeMethod.getReturnType().getPresentableText() : "";
        String afterType = afterMethod.getReturnType() != null ? afterMethod.getReturnType().getPresentableText() : "";
        return !beforeType.equals(afterType);
    }

    /**
     * 判断方法的 throws 声明是否发生变化
     *
     * @param beforeMethod 变更前的方法对象
     * @param afterMethod  变更后的方法对象
     * @return 如果两个方法的 throws 声明不同, 则返回 true; 否则返回 false
     */
    private boolean isThrowsChanged(@NotNull PsiMethod beforeMethod, @NotNull PsiMethod afterMethod) {
        return !beforeMethod.getThrowsList().getText().equals(afterMethod.getThrowsList().getText());
    }

    /**
     * 判断方法签名是否发生变化
     *
     * @param beforeMethod 比较之前的方法
     * @param afterMethod  比较之后的方法
     * @return 如果方法签名发生变化, 返回 true; 否则返回 false
     */
    private boolean isSignatureChanged(@NotNull PsiMethod beforeMethod, @NotNull PsiMethod afterMethod) {
        return !beforeMethod.getSignature(PsiSubstitutor.EMPTY).equals(afterMethod.getSignature(PsiSubstitutor.EMPTY));
    }

    /**
     * 判断方法体是否发生变化
     *
     * @param beforeMethod 被比较的方法对象
     * @param afterMethod  用于比较的方法对象
     * @return 如果两个方法体的文本表示不同, 则返回 true, 否则返回 false
     */
    private boolean isBodyChanged(@NotNull PsiMethod beforeMethod, @NotNull PsiMethod afterMethod) {
        PsiCodeBlock beforeBody = beforeMethod.getBody();
        PsiCodeBlock afterBody = afterMethod.getBody();
        if (beforeBody == null || afterBody == null) {
            return false;
        }
        return !beforeBody.getText().equals(afterBody.getText());
    }

    /**
     * 判断是否为明显行为变化 (if/return/throw 数量变化)
     *
     * @param beforeBody 原始方法体
     * @param afterBody  修改后的方法体
     * @return 如果 if,return,throw 或方法调用数量发生变化, 则返回 true, 否则返回 false
     */
    private boolean isBehaviorChanged(@Nullable PsiCodeBlock beforeBody, @Nullable PsiCodeBlock afterBody) {
        if (beforeBody == null || afterBody == null) {
            return false;
        }
        int beforeIf = PsiTreeUtil.findChildrenOfType(beforeBody, PsiIfStatement.class).size();
        int afterIf = PsiTreeUtil.findChildrenOfType(afterBody, PsiIfStatement.class).size();
        int beforeReturn = PsiTreeUtil.findChildrenOfType(beforeBody, PsiReturnStatement.class).size();
        int afterReturn = PsiTreeUtil.findChildrenOfType(afterBody, PsiReturnStatement.class).size();
        int beforeThrow = PsiTreeUtil.findChildrenOfType(beforeBody, PsiThrowStatement.class).size();
        int afterThrow = PsiTreeUtil.findChildrenOfType(afterBody, PsiThrowStatement.class).size();
        int beforeCalls = PsiTreeUtil.findChildrenOfType(beforeBody, PsiMethodCallExpression.class).size();
        int afterCalls = PsiTreeUtil.findChildrenOfType(afterBody, PsiMethodCallExpression.class).size();
        return beforeIf != afterIf
               || beforeReturn != afterReturn
               || beforeThrow != afterThrow
               || beforeCalls != afterCalls;
    }

    /**
     * 判断是否为结构性重构 (仅排版 / 命名等, 不改变控制流)
     *
     * @param beforeBody 旧的方法体代码块, 可能为 null
     * @param afterBody  新的方法体代码块, 可能为 null
     * @return 如果两个代码块在移除所有空白字符后内容完全相同, 则返回 true, 表示为结构性重构; 否则返回 false
     */
    private boolean isRefactorChange(@Nullable PsiCodeBlock beforeBody, @Nullable PsiCodeBlock afterBody) {
        if (beforeBody == null || afterBody == null) {
            return false;
        }
        String beforeText = beforeBody.getText().replaceAll("\\s+", "");
        String afterText = afterBody.getText().replaceAll("\\s+", "");
        return beforeText.equals(afterText);
    }

    /**
     * 构建方法标识
     *
     * @param method 要构建标识的方法对象
     * @return 方法的完整标识符, 格式为 "类名 #方法名 + 参数列表", 如果无包含类则返回 "方法名 + 参数列表"
     */
    @NotNull
    private String buildMethodKey(@NotNull PsiMethod method) {
        PsiClass psiClass = method.getContainingClass();
        String className = psiClass != null ? psiClass.getName() : null;
        String methodSig = method.getName() + method.getParameterList().getText();
        return className != null ? className + "#" + methodSig : methodSig;
    }

    /**
     * 构建字段标识
     * <p> 根据字段所在的类和字段名称生成唯一的标识符, 格式为 "类名 #字段名" 或仅返回字段名.
     *
     * @param field 字段对象, 不能为空
     * @return 字段的唯一标识符, 如果字段所属类存在则返回 "类名 #字段名", 否则返回字段名
     */
    @NotNull
    private String buildFieldKey(@NotNull PsiField field) {
        PsiClass psiClass = field.getContainingClass();
        String className = psiClass != null ? psiClass.getName() : null;
        String fieldName = field.getName();
        return className != null ? className + "#" + fieldName : fieldName;
    }

    /**
     * 构建类标识
     * <p> 根据给定的 PsiClass 对象, 返回其名称作为标识. 如果类名为 null, 则返回 "AnonymousClass".
     *
     * @param psiClass 类对象, 不能为 null
     * @return 类名称, 若不存在则返回 "AnonymousClass"
     */
    @NotNull
    private String buildClassKey(@NotNull PsiClass psiClass) {
        String name = psiClass.getName();
        return name != null ? name : "AnonymousClass";
    }

    /**
     * 判断字段默认值是否变化
     *
     * @param beforeField 变更前的字段对象
     * @param afterField  变更后的字段对象
     * @return 如果字段默认值发生变化则返回 true, 否则返回 false
     */
    private boolean isFieldInitializerChanged(@NotNull PsiField beforeField, @NotNull PsiField afterField) {
        String beforeInit = beforeField.getInitializer() != null ? beforeField.getInitializer().getText() : "";
        String afterInit = afterField.getInitializer() != null ? afterField.getInitializer().getText() : "";
        return !beforeInit.equals(afterInit);
    }

    /**
     * 判断类签名是否发生变化 (包括可见性, 继承列表 extends 和实现列表 implements)
     *
     * @param beforeClass 比较之前的类
     * @param afterClass  比较之后的类
     * @return 如果类的签名发生变化, 返回 true; 否则返回 false
     */
    private boolean isClassSignatureChanged(@NotNull PsiClass beforeClass, @NotNull PsiClass afterClass) {
        if (!getVisibility(beforeClass.getModifierList()).equals(getVisibility(afterClass.getModifierList()))) {
            return true;
        }
        String beforeExtends = beforeClass.getExtendsList() != null ? beforeClass.getExtendsList().getText() : "";
        String afterExtends = afterClass.getExtendsList() != null ? afterClass.getExtendsList().getText() : "";
        if (!beforeExtends.equals(afterExtends)) {
            return true;
        }
        String beforeImplements = beforeClass.getImplementsList() != null ? beforeClass.getImplementsList().getText() : "";
        String afterImplements = afterClass.getImplementsList() != null ? afterClass.getImplementsList().getText() : "";
        return !beforeImplements.equals(afterImplements);
    }

    /**
     * 判断类注解是否发生变化
     *
     * @param beforeClass 变更前的类节点
     * @param afterClass  变更后的类节点
     * @return 如果类的注解内容发生变化, 则返回 true; 否则返回 false
     */
    private boolean isAnnotationChanged(@NotNull PsiClass beforeClass, @NotNull PsiClass afterClass) {
        String beforeAnno = buildAnnotationKey(beforeClass.getModifierList());
        String afterAnno = buildAnnotationKey(afterClass.getModifierList());
        return !beforeAnno.equals(afterAnno);
    }

    /**
     * 提取修饰符列表中的可见性
     *
     * @param modifierList 修饰符列表对象, 可能为 null
     * @return 返回可见性字符串, 可能值为 "public", "protected", "private", "package-private"
     */
    @NotNull
    private String getVisibility(@Nullable PsiModifierList modifierList) {
        if (modifierList == null) {
            return "package-private";
        }
        if (modifierList.hasModifierProperty(PsiModifier.PUBLIC)) {
            return "public";
        }
        if (modifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
            return "protected";
        }
        if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
            return "private";
        }
        return "package-private";
    }

    /**
     * 汇总注解列表文本
     *
     * @param modifierList 修饰符列表对象, 可能为 null
     * @return 修饰符列表的文本表示, 如果为 null 则返回空字符串
     */
    @NotNull
    private String buildAnnotationKey(@Nullable PsiModifierList modifierList) {
        if (modifierList == null) {
            return "";
        }
        return modifierList.getText();
    }

    /**
     * 输出语义摘要文本
     *
     * @param counters 用于统计各类变更的计数器对象
     * @param details  包含具体变更细节的字符串列表
     * @return 格式化的语义变更总结字符串, 包含不同层级的变更分类和示例详情
     */
    @NotNull
    private String buildSummary(@NotNull SemanticCounters counters, @NotNull List<String> details) {
        StringBuilder summary = new StringBuilder();
        summary.append("变更语义总结:\n");
        if (counters.apiSignatureChanges > 0) {
            summary.append("- 接口层：").append(counters.apiSignatureChanges).append(" 个 public API 方法签名变更\n");
        }
        if (counters.classChanges > 0) {
            summary.append("- 类级别：").append(counters.classChanges).append(" 处类结构变更\n");
        }
        if (counters.fieldChanges > 0) {
            summary.append("- 字段：").append(counters.fieldChanges).append(" 处字段变更\n");
        }
        if (counters.annotationChanges > 0) {
            summary.append("- 注解：").append(counters.annotationChanges).append(" 处注解变化\n");
        }
        if (counters.implementationChanges > 0) {
            summary.append("- 实现层：").append(counters.implementationChanges).append(" 个方法内部实现调整\n");
        }
        if (counters.behaviorChanges > 0) {
            summary.append("- 行为：").append(counters.behaviorChanges).append(" 处行为变化\n");
        }
        if (counters.refactorChanges > 0) {
            summary.append("- 重构：").append(counters.refactorChanges).append(" 处结构调整（无明显行为变化）\n");
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

    /** 语义统计计数器 */
    private static class SemanticCounters {
        /** API 签名变更次数 */
        int apiSignatureChanges;
        /** 实现层面的变更计数 */
        int implementationChanges;
        /** 行为变更计数 */
        int behaviorChanges;
        /** 重构变更计数器, 用于统计代码重构相关的变更数量 */
        int refactorChanges;
        /** 类变更计数器, 用于统计类级别的变更数量 */
        int classChanges;
        /** 字段变更计数 */
        int fieldChanges;
        /** 注解变更计数, 记录类或方法上注解的增删改数量 */
        int annotationChanges;

        /**
         * 判断语义统计计数器是否为空
         * <p>当所有变更计数 (包括 API 签名变更, 实现变更, 行为变更, 重构变更, 类变更, 字段变更, 注解变更) 均为 0 时返回 true, 否则返回 false
         *
         * @return 如果所有变更计数都为 0 则返回 true, 否则返回 false
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
