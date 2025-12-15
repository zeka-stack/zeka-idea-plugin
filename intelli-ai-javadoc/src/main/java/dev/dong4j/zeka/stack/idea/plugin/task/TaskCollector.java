package dev.dong4j.zeka.stack.idea.plugin.task;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.AiCodePreprocessor;

/**
 * 任务收集器类
 * <p>
 * 负责从 PSI 元素中收集文档生成任务, 支持从方法, 字段, 类, 文件和目录等多个维度收集需要生成文档的任务.
 * 根据设置状态判断是否为元素生成文档, 并支持过滤已存在文档的元素.
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @email mailto:zeka.stack@gmail.com
 * @date 2025.11.30
 * @since 1.0.0
 */
public class TaskCollector {

    /** 项目实例，用于持有和管理当前操作的项目信息 */
    private final Project project;
    /** 用户设置状态对象，用于存储和管理应用的配置和用户偏好设置 */
    private final SettingsState settings;

    /**
     * 初始化任务收集器
     * <p>
     * 通过传入的项目对象初始化任务收集器，设置项目引用和配置状态实例
     *
     * @param project 项目对象，不能为空
     */
    public TaskCollector(@NotNull Project project) {
        this.project = project;
        this.settings = SettingsState.getInstance();
    }

    /**
     * 从单个 PSI 元素收集任务
     * <p>用于智能定位后只为特定元素生成文档
     *
     * <p>根据元素类型采取不同的处理策略：
     * <ul>
     *   <li>PsiMethod：为方法创建任务（区分普通方法和测试方法）</li>
     *   <li>PsiField：为字段创建任务</li>
     *   <li>PsiClass：为类及其所有成员创建任务</li>
     *   <li>PsiFile：为整个文件创建任务</li>
     * </ul>
     *
     * <p>处理流程：
     * <ol>
     *   <li>检查用户配置是否启用相应类型的文档生成</li>
     *   <li>检查元素是否已有文档（根据配置决定是否跳过）</li>
     *   <li>创建相应的 DocumentationTask 对象</li>
     * </ol>
     *
     * @param element PSI 元素（可以是 PsiMethod、PsiField、PsiClass）
     * @return 任务列表（单个元素或该元素包含的所有子元素）
     * @see #collectFromClass(PsiClass, List)
     * @see #collectFromFile(PsiFile)
     */
    @SuppressWarnings("D")
    @NotNull
    public List<DocumentationTask> collectFromElement(@NotNull PsiElement element) {
        List<DocumentationTask> tasks = new ArrayList<>();

        // Java 元素处理
        if (element instanceof PsiMethod method) {
            // 为单个方法生成
            if (settings.generateForMethod && shouldGenerateForElement(method)) {
                DocumentationTask.TaskType type = isTestMethod(method)
                                                  ? DocumentationTask.TaskType.TEST_METHOD
                                                  : DocumentationTask.TaskType.METHOD;
                tasks.add(createTask(method, type));
            }
        } else if (element instanceof PsiField field) {
            // 为单个字段生成
            if (settings.generateForField && shouldGenerateForElement(field)) {
                tasks.add(createTask(field, DocumentationTask.TaskType.FIELD));
            }
        } else if (element instanceof PsiClass psiClass) {
            // 为类及其所有成员生成
            // collectFromClass(psiClass, tasks);
            // 2025.11.08 只为类本身生成
            if (settings.generateForClass && shouldGenerateForElement(psiClass)) {
                tasks.add(createTask(psiClass, DocumentationTask.TaskType.CLASS));
            }
        }
        // Kotlin 元素处理
        else if (element instanceof KtNamedFunction function) {
            // 为单个 Kotlin 函数生成
            if (settings.generateForMethod && shouldGenerateForElement(function)) {
                DocumentationTask.TaskType type = isKotlinTestMethod(function)
                                                  ? DocumentationTask.TaskType.TEST_METHOD
                                                  : DocumentationTask.TaskType.METHOD;
                tasks.add(createTask(function, type));
            }
        } else if (element instanceof KtProperty property) {
            // 为单个 Kotlin 属性生成
            if (settings.generateForField && shouldGenerateForElement(property)) {
                tasks.add(createTask(property, DocumentationTask.TaskType.FIELD));
            }
        } else if (element instanceof KtClassOrObject ktClass) {
            // 为 Kotlin 类/对象生成
            if (settings.generateForClass && shouldGenerateForElement(ktClass)) {
                tasks.add(createTask(ktClass, DocumentationTask.TaskType.CLASS));
            }
        } else if (element instanceof PsiFile) {
            // 为整个文件生成
            return collectFromFile((PsiFile) element);
        }

        return tasks;
    }

    /**
     * 从类收集任务（包括类本身和所有成员）
     *
     * <p>递归处理类及其所有成员，包括内部类。
     * 根据用户配置决定是否为类、方法、字段生成文档。
     *
     * <p>处理顺序：
     * <ol>
     *   <li>类本身</li>
     *   <li>所有方法（区分普通方法和测试方法）</li>
     *   <li>所有字段</li>
     *   <li>所有内部类（递归处理）</li>
     * </ol>
     *
     * <p>配置检查：
     * <ul>
     *   <li>generateForClass：是否为类生成文档</li>
     *   <li>generateForMethod：是否为方法生成文档</li>
     *   <li>generateForField：是否为字段生成文档</li>
     *   <li>overrideExisting：是否覆盖已有注释（false=跳过，true=覆盖）</li>
     * </ul>
     *
     * @param psiClass PSI 类对象
     * @param tasks    任务列表，用于收集创建的任务
     */
    @SuppressWarnings("D")
    private void collectFromClass(@NotNull PsiClass psiClass, @NotNull List<DocumentationTask> tasks) {
        // 为类本身生成
        if (settings.generateForClass && shouldGenerateForElement(psiClass)) {
            tasks.add(createTask(psiClass, DocumentationTask.TaskType.CLASS));
        }

        // 为类的所有方法生成
        if (settings.generateForMethod) {
            for (PsiMethod method : psiClass.getMethods()) {
                if (shouldGenerateForElement(method)) {
                    DocumentationTask.TaskType type = isTestMethod(method)
                                                      ? DocumentationTask.TaskType.TEST_METHOD
                                                      : DocumentationTask.TaskType.METHOD;
                    tasks.add(createTask(method, type));
                }
            }
        }

        // 为类的所有字段生成
        if (settings.generateForField) {
            for (PsiField field : psiClass.getFields()) {
                if (shouldGenerateForElement(field)) {
                    tasks.add(createTask(field, DocumentationTask.TaskType.FIELD));
                }
            }
        }

        // 为内部类生成
        for (PsiClass innerClass : psiClass.getInnerClasses()) {
            collectFromClass(innerClass, tasks);
        }
    }

    /**
     * 从 PSI 文件收集任务（通用方法）
     *
     * <p>使用 JavaRecursiveElementVisitor 递归遍历文件中的所有元素，
     * 为符合条件的类、方法、字段创建文档生成任务。
     *
     * <p>遍历流程：
     * <ol>
     *   <li>检查文件是否为 Java 文件</li>
     *   <li>使用 visitor 模式遍历所有元素</li>
     *   <li>使用提供的判断条件决定是否为元素创建任务</li>
     * </ol>
     *
     * <p>Visitor 处理：
     * <ul>
     *   <li>visitClass：处理类元素</li>
     *   <li>visitMethod：处理方法元素（区分普通方法和测试方法）</li>
     *   <li>visitField：处理字段元素</li>
     * </ul>
     *
     * @param psiFile          PSI 文件对象
     * @param elementPredicate 元素判断条件，用于决定是否为元素生成文档任务
     * @return 文档生成任务列表
     * @see JavaRecursiveElementVisitor
     */
    @SuppressWarnings("D")
    @NotNull
    private List<DocumentationTask> collectFromFileInternal(@NotNull PsiFile psiFile,
                                                            @NotNull Predicate<PsiElement> elementPredicate) {
        List<DocumentationTask> tasks = new ArrayList<>();

        // 处理 Java 文件
        if (psiFile instanceof PsiJavaFile) {
            psiFile.accept(new JavaRecursiveElementVisitor() {
                /**
                 * 访问类元素并根据配置和判断条件决定是否生成文档任务
                 *
                 * @param aClass 被访问的类元素
                 */
                @Override
                public void visitClass(@NotNull PsiClass aClass) {
                    super.visitClass(aClass);

                    if (settings.generateForClass && elementPredicate.test(aClass)) {
                        tasks.add(createTask(aClass, DocumentationTask.TaskType.CLASS));
                    }
                }

                /**
                 * 处理方法节点，根据配置和判断条件决定是否生成文档任务
                 *
                 * @param method 被访问的方法节点
                 */
                @Override
                public void visitMethod(@NotNull PsiMethod method) {
                    super.visitMethod(method);

                    if (settings.generateForMethod && elementPredicate.test(method)) {
                        DocumentationTask.TaskType type = isTestMethod(method)
                                                          ? DocumentationTask.TaskType.TEST_METHOD
                                                          : DocumentationTask.TaskType.METHOD;
                        tasks.add(createTask(method, type));
                    }
                }

                /**
                 * 处理字段元素，根据配置和判断条件决定是否生成文档任务
                 *
                 * @param field 被访问的字段元素
                 */
                @Override
                public void visitField(@NotNull PsiField field) {
                    super.visitField(field);

                    if (settings.generateForField && elementPredicate.test(field)) {
                        tasks.add(createTask(field, DocumentationTask.TaskType.FIELD));
                    }
                }
            });
        }
        // 处理 Kotlin 文件
        else if (psiFile instanceof KtFile ktFile) {
            // 检查是否支持 Kotlin
            if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
                return tasks;
            }

            ktFile.accept(new KtTreeVisitorVoid() {
                @Override
                public void visitClassOrObject(@NotNull KtClassOrObject classOrObject) {
                    super.visitClassOrObject(classOrObject);

                    if (settings.generateForClass && elementPredicate.test(classOrObject)) {
                        tasks.add(createTask(classOrObject, DocumentationTask.TaskType.CLASS));
                    }
                }

                @Override
                public void visitNamedFunction(@NotNull KtNamedFunction function) {
                    super.visitNamedFunction(function);

                    if (settings.generateForMethod && elementPredicate.test(function)) {
                        DocumentationTask.TaskType type = isKotlinTestMethod(function)
                                                          ? DocumentationTask.TaskType.TEST_METHOD
                                                          : DocumentationTask.TaskType.METHOD;
                        tasks.add(createTask(function, type));
                    }
                }

                @Override
                public void visitProperty(@NotNull KtProperty property) {
                    super.visitProperty(property);

                    if (settings.generateForField && elementPredicate.test(property)) {
                        tasks.add(createTask(property, DocumentationTask.TaskType.FIELD));
                    }
                }
            });
        }

        return tasks;
    }

    /**
     * 从 PSI 文件收集任务
     *
     * <p>使用 JavaRecursiveElementVisitor 递归遍历文件中的所有元素，
     * 为符合条件的类、方法、字段创建文档生成任务。
     *
     * <p>遍历流程：
     * <ol>
     *   <li>检查文件是否为 Java 文件</li>
     *   <li>使用 visitor 模式遍历所有元素</li>
     *   <li>为每个符合条件的元素创建任务</li>
     * </ol>
     *
     * <p>Visitor 处理：
     * <ul>
     *   <li>visitClass：处理类元素</li>
     *   <li>visitMethod：处理方法元素（区分普通方法和测试方法）</li>
     *   <li>visitField：处理字段元素</li>
     * </ul>
     *
     * <p>判断条件：
     * <ul>
     *   <li>根据用户配置（generateForClass、generateForMethod、generateForField）决定是否处理</li>
     *   <li>根据 overrideExisting 配置决定是否覆盖已有文档</li>
     * </ul>
     *
     * @param psiFile PSI 文件对象
     * @return 文档生成任务列表
     * @see #collectMissingJavaDocFromFile(PsiFile)
     * @see JavaRecursiveElementVisitor
     */
    @NotNull
    public List<DocumentationTask> collectFromFile(@NotNull PsiFile psiFile) {
        return collectFromFileInternal(psiFile, this::shouldGenerateForElement);
    }

    /**
     * 从 PSI 文件收集缺失 Javadoc 的任务
     *
     * <p>专门用于 Git 提交场景，只收集没有 Javadoc 的元素，
     * 忽略所有配置（generateForClass、generateForMethod、generateForField、overrideExisting），
     * 强制检查所有类型的元素，确保只为缺失文档的元素生成任务。
     *
     * <p>与 collectFromFile 的区别：
     * <ul>
     *   <li>强制只收集没有 Javadoc 的元素</li>
     *   <li>忽略所有配置（generateForClass、generateForMethod、generateForField、overrideExisting）</li>
     *   <li>强制检查类、方法、字段所有类型的元素</li>
     *   <li>适用于提交前的文档检查场景</li>
     * </ul>
     *
     * <p>遍历流程：
     * <ol>
     *   <li>检查文件是否为 Java 文件</li>
     *   <li>使用 visitor 模式遍历所有元素</li>
     *   <li>强制检查所有类型的元素（类、方法、字段）</li>
     *   <li>只收集没有 Javadoc 的元素</li>
     * </ol>
     *
     * @param psiFile PSI 文件对象
     * @return 缺失 Javadoc 的文档生成任务列表
     * @see #collectFromFile(PsiFile)
     * @see JavaRecursiveElementVisitor
     */
    @SuppressWarnings("D")
    @NotNull
    public List<DocumentationTask> collectMissingJavaDocFromFile(@NotNull PsiFile psiFile) {
        List<DocumentationTask> tasks = new ArrayList<>();

        // 处理 Java 文件
        if (psiFile instanceof PsiJavaFile) {
            // 专门用于 Git 提交场景，强制检查所有类型的元素，不受配置影响
            psiFile.accept(new JavaRecursiveElementVisitor() {
                /**
                 * 访问类元素，强制检查所有类（不受 generateForClass 配置影响）
                 *
                 * @param aClass 被访问的类元素
                 */
                @Override
                public void visitClass(@NotNull PsiClass aClass) {
                    super.visitClass(aClass);

                    // 强制检查类，不受 generateForClass 配置影响
                    if (hasNoJavaDoc(aClass)) {
                        tasks.add(createTask(aClass, DocumentationTask.TaskType.CLASS));
                    }
                }

                /**
                 * 处理方法节点，强制检查所有方法（不受 generateForMethod 配置影响）
                 *
                 * @param method 被访问的方法节点
                 */
                @Override
                public void visitMethod(@NotNull PsiMethod method) {
                    super.visitMethod(method);

                    // 强制检查方法，不受 generateForMethod 配置影响
                    if (hasNoJavaDoc(method)) {
                        DocumentationTask.TaskType type = isTestMethod(method)
                                                          ? DocumentationTask.TaskType.TEST_METHOD
                                                          : DocumentationTask.TaskType.METHOD;
                        tasks.add(createTask(method, type));
                    }
                }

                /**
                 * 处理字段元素，强制检查所有字段（不受 generateForField 配置影响）
                 *
                 * @param field 被访问的字段元素
                 */
                @Override
                public void visitField(@NotNull PsiField field) {
                    super.visitField(field);

                    // 强制检查字段，不受 generateForField 配置影响
                    if (hasNoJavaDoc(field)) {
                        tasks.add(createTask(field, DocumentationTask.TaskType.FIELD));
                    }
                }
            });
        }
        // 处理 Kotlin 文件
        else if (psiFile instanceof KtFile ktFile) {
            // 检查是否支持 Kotlin
            if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
                return tasks;
            }

            // 专门用于 Git 提交场景，强制检查所有类型的元素，不受配置影响
            ktFile.accept(new KtTreeVisitorVoid() {
                @Override
                public void visitClassOrObject(@NotNull KtClassOrObject classOrObject) {
                    super.visitClassOrObject(classOrObject);

                    // 强制检查类，不受 generateForClass 配置影响
                    if (hasNoJavaDoc(classOrObject)) {
                        tasks.add(createTask(classOrObject, DocumentationTask.TaskType.CLASS));
                    }
                }

                @Override
                public void visitNamedFunction(@NotNull KtNamedFunction function) {
                    super.visitNamedFunction(function);

                    // 强制检查方法，不受 generateForMethod 配置影响
                    if (hasNoJavaDoc(function)) {
                        DocumentationTask.TaskType type = isKotlinTestMethod(function)
                                                          ? DocumentationTask.TaskType.TEST_METHOD
                                                          : DocumentationTask.TaskType.METHOD;
                        tasks.add(createTask(function, type));
                    }
                }

                @Override
                public void visitProperty(@NotNull KtProperty property) {
                    super.visitProperty(property);

                    // 强制检查字段，不受 generateForField 配置影响
                    if (hasNoJavaDoc(property)) {
                        tasks.add(createTask(property, DocumentationTask.TaskType.FIELD));
                    }
                }
            });
        }

        return tasks;
    }

    /**
     * 从虚拟文件收集任务
     *
     * <p>将虚拟文件转换为 PSI 文件，然后收集其中的文档生成任务。
     * 主要用于处理项目视图中的文件选择。
     *
     * <p>处理流程：
     * <ol>
     *   <li>通过 PsiManager 将 VirtualFile 转换为 PsiFile</li>
     *   <li>调用 collectFromFile 方法收集任务</li>
     * </ol>
     *
     * <p>异常处理：
     * <ul>
     *   <li>如果无法转换为 PsiFile，返回空列表</li>
     * </ul>
     *
     * @param virtualFile 虚拟文件对象
     * @return 文档生成任务列表
     * @see #collectFromFile(PsiFile)
     */
    @NotNull
    public List<DocumentationTask> collectFromVirtualFile(@NotNull VirtualFile virtualFile) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile == null) {
            return new ArrayList<>();
        }
        return collectFromFile(psiFile);
    }

    /**
     * 从目录递归收集任务
     *
     * <p>递归遍历目录中的所有文件和子目录，
     * 为其中的 Java 文件收集文档生成任务。
     * 主要用于处理项目视图中的目录选择。
     *
     * <p>遍历策略：
     * <ul>
     *   <li>递归处理子目录</li>
     *   <li>只处理 Java 文件</li>
     *   <li>合并所有文件的任务</li>
     * </ul>
     *
     * @param directory 虚拟目录对象
     * @return 文档生成任务列表
     * @see #collectFromDirectoryRecursive(VirtualFile, List)
     */
    @NotNull
    public List<DocumentationTask> collectFromDirectory(@NotNull VirtualFile directory) {
        List<DocumentationTask> tasks = new ArrayList<>();
        collectFromDirectoryRecursive(directory, tasks);
        return tasks;
    }

    /**
     * 递归收集指定目录下的Java文件对应的文档任务
     * <p>
     * 该方法会递归遍历指定目录下的所有子目录和文件，对于每个Java文件，调用
     * collectFromVirtualFile 方法收集文档任务，并将结果添加到任务列表中。
     * <p>
     * 使用 VfsUtilCore.visitChildrenRecursively() 来安全地遍历目录结构，
     * 避免因循环符号链接导致的无限递归问题。
     *
     * @param directory 要收集的目录对象
     * @param tasks     用于存储收集到的文档任务的列表
     */
    private void collectFromDirectoryRecursive(@NotNull VirtualFile directory,
                                               @NotNull List<DocumentationTask> tasks) {
        if (!directory.isDirectory()) {
            return;
        }

        VfsUtilCore.visitChildrenRecursively(directory, new VirtualFileVisitor<Void>() {
            /**
             * 访问指定的文件并收集相关任务
             * <p>
             * 如果文件不是目录且是 Java 文件, 则从该文件中收集任务并添加到任务列表中.
             *
             * @param file 要访问的虚拟文件
             * @return 总是返回 true, 表示继续访问其他文件
             */
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory() && isJavaFile(file)) {
                    tasks.addAll(collectFromVirtualFile(file));
                }
                return true;
            }
        });
    }

    /**
     * 创建任务
     *
     * <p>创建 DocumentationTask 对象，包含处理所需的全部信息。
     * 获取元素的代码时，会包含已有的 Javadoc 注释（如果存在）。
     * 这样 AI 可以看到当前的注释，从而生成更准确的新注释或改进现有注释。
     *
     * <p>任务包含的信息：
     * <ul>
     *   <li>PSI 元素：用于后续的文档插入</li>
     *   <li>代码内容：包含现有注释，供 AI 分析</li>
     *   <li>任务类型：决定使用的 Prompt 模板</li>
     *   <li>文件路径：用于进度显示和日志记录</li>
     * </ul>
     *
     * @param element PSI 元素
     * @param type    任务类型
     * @return 创建的文档生成任务
     * @see DocumentationTask
     */
    @NotNull
    private DocumentationTask createTask(@NotNull PsiElement element,
                                         @NotNull DocumentationTask.TaskType type) {
        // 获取代码，包含已有的 Javadoc 注释
        String code = getCodeWithComment(element);

        // 构建上下文信息（当前仅提供类级别代码片段，后续可扩展）
        GenerationContext context = settings.enableGenerationContext
                                    ? buildGenerationContext(element)
                                    : GenerationContext.empty();

        // 获取文件路径，处理 VirtualFile 为 null 的情况（例如 Scratch 文件）
        PsiFile containingFile = element.getContainingFile();
        String filePath;
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile != null) {
            filePath = virtualFile.getPath();
        } else {
            // 如果 VirtualFile 为 null，使用文件名作为标识
            filePath = containingFile.getName();
        }

        return new DocumentationTask(element, code, type, filePath, context);
    }

    /**
     * 基于 PSI 元素构建文档生成上下文。
     * <p>
     * 当前实现：
     * <ul>
     *   <li>如果元素本身是 Java {@link PsiClass} 或 Kotlin {@link KtClassOrObject}，则使用其自身代码作为上下文</li>
     *   <li>否则查找最近的 Java {@link PsiClass} 或 Kotlin {@link KtClassOrObject} 作为所属类</li>
     *   <li>截取该类/对象代码的前 500 行作为类级别上下文片段</li>
     *   <li>如果找不到所属类，则返回 null</li>
     * </ul>
     *
     * @param element 当前任务对应的 PSI 元素
     * @return 上下文对象，如果无法构建则返回 null
     */
    @NotNull
    private GenerationContext buildGenerationContext(@NotNull PsiElement element) {
        // 元素本身就是 Java 类
        if (element instanceof PsiClass) {
            return GenerationContext.empty();
        }
        // 元素本身就是 Kotlin 类/对象
        if (element instanceof KtClassOrObject) {
            return GenerationContext.empty();
        }

        // 优先查找最近的 Java 类
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass != null) {
            // 先压缩代码
            String reformatCode = AiCodePreprocessor.preprocess(optimizeClassCode(psiClass.getText()));
            String snippet = limitLines(reformatCode);
            return GenerationContext.ofClassCode(snippet);
        }

        // 再查找最近的 Kotlin 类/对象
        KtClassOrObject ktClass = PsiTreeUtil.getParentOfType(element, KtClassOrObject.class);
        if (ktClass != null) {
            // 先压缩代码
            String reformatCode = AiCodePreprocessor.preprocess(optimizeClassCode(ktClass.getText()));
            String snippet = limitLines(reformatCode);
            return GenerationContext.ofClassCode(snippet);
        }

        // 没有找到合适的类级上下文
        return GenerationContext.empty();
    }

    /**
     * 将多行字符串限制在指定的最大行数内。
     *
     * @param text 原始文本
     * @return 截断后的文本，如果原始文本行数不足则返回原文
     */
    @NotNull
    private String limitLines(@NotNull String text) {
        if (text.isEmpty() || settings.maxClassCodeLines <= 0) {
            return text;
        }
        String[] lines = text.split("\n", -1);
        if (lines.length <= settings.maxClassCodeLines) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < settings.maxClassCodeLines; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取元素的代码，包含已有的 Javadoc 注释
     *
     * <p>通过 element.getText() 方法获取元素的完整文本表示，
     * 包括已有的 Javadoc 注释。这样可以为 AI 提供上下文信息，
     * 有助于生成更准确和一致的文档。
     *
     * <p>对于类级别的代码，会进行优化以减少 token 消耗：
     * <ul>
     *   <li>删除多余的空格、注释和换行</li>
     *   <li>保留必要的空格以维持代码可读性</li>
     *   <li>如果优化后超过 1000 行，会进行截取</li>
     * </ul>
     *
     * <p>对于方法和字段级别的代码，如果启用了代码压缩，会进行压缩处理：
     * <ul>
     *   <li>删除所有注释（Javadoc、块注释、单行注释）</li>
     *   <li>删除多余空格和空行</li>
     *   <li>缩进压缩到最小层级（每层 1 个空格）</li>
     * </ul>
     *
     * <p>设计考虑：
     * <ul>
     *   <li>保持代码的完整性</li>
     *   <li>提供现有注释作为参考</li>
     *   <li>避免重复添加注释</li>
     *   <li>优化 token 使用效率</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 包含注释的完整代码
     */
    @NotNull
    private String getCodeWithComment(@NotNull PsiElement element) {
        String originalCode = element.getText();

        if (!settings.enableCodeCompression) {
            // 未启用代码压缩时返回原始代码
            return originalCode;
        }

        String reformatCode = "";
        // 不同的 IDEA 版本可能会出现格式化异常
        try {
            // 格式化副本, 不能直接操作原始的 PSI 元素, 因为该方法在 read-action 中调用, 所以拷贝一个副本来执行格式化
            final PsiElement reformat = CodeStyleManager.getInstance(project).reformat(element.copy());

            // 类级别的代码使用 optimizeClassCode 方法
            if (element instanceof PsiClass || element instanceof KtClassOrObject) {
                reformatCode = AiCodePreprocessor.preprocess(optimizeClassCode(reformat.getText()));
            }
            // 方法或字段级别的代码使用 AiCodePreprocessor 进行压缩
            if (element instanceof PsiMethod || element instanceof PsiField ||
                element instanceof KtNamedFunction || element instanceof KtProperty) {
                reformatCode = AiCodePreprocessor.preprocess(reformat.getText());
            }
        } catch (Exception e) {
            return originalCode;
        }
        return StringUtil.isEmpty(reformatCode) ? originalCode : reformatCode;
    }

    /**
     * 优化类级别的代码以减少 token 消耗
     *
     * <p>对类代码进行以下优化：
     * <ul>
     *   <li>删除多余的空行和空白字符</li>
     *   <li>删除单行注释（// 注释）</li>
     *   <li>保留 Javadoc 注释（/** 注释）</li>
     *   <li>保留必要的空格以维持代码结构</li>
     *   <li>如果超过 1000 行，截取前 1000 行</li>
     * </ul>
     *
     * @param originalCode 原始类代码
     * @return 优化后的代码
     */
    @NotNull
    private String optimizeClassCode(@NotNull String originalCode) {
        if (originalCode.trim().isEmpty()) {
            return originalCode;
        }

        StringBuilder optimized = new StringBuilder();
        String[] lines = originalCode.split("\n");
        int lineCount = 0;
        final int maxLines = settings.maxClassCodeLines;

        for (String line : lines) {
            // 如果已经达到最大行数，停止处理
            if (lineCount >= maxLines) {
                optimized.append("\n// ... (代码已截取，超过 ").append(maxLines).append(" 行)");
                break;
            }

            String trimmedLine = line.trim();

            // 跳过空行
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // 跳过单行注释（// 注释），但保留 Javadoc 注释（/** 注释）
            if (trimmedLine.startsWith("//") && !trimmedLine.startsWith("/**")) {
                continue;
            }

            // 保留这行代码
            optimized.append(line).append("\n");
            lineCount++;
        }

        String result = optimized.toString();

        // 如果结果为空，返回原始代码
        if (result.trim().isEmpty()) {
            return originalCode;
        }

        return result;
    }

    /**
     * 判断元素是否没有 Javadoc/KDoc 注释
     *
     * <p>专门用于检查元素是否缺失文档注释（Javadoc 或 KDoc），
     * 忽略 overrideExisting 配置，只检查元素是否已有文档。
     *
     * <p>检查逻辑：
     * <ul>
     *   <li>Java 元素：如果元素支持文档注释（PsiDocCommentOwner），检查是否有 Javadoc</li>
     *   <li>Kotlin 元素：检查是否有 KDoc 注释</li>
     *   <li>如果没有文档注释返回 true，否则返回 false</li>
     *   <li>如果元素不支持文档注释，返回 true（允许处理）</li>
     * </ul>
     *
     * @param element PSI 元素
     * @return 如果元素没有文档注释返回 true，否则返回 false
     * @see #shouldGenerateForElement(PsiElement)
     */
    private boolean hasNoJavaDoc(@NotNull PsiElement element) {
        // Java 元素检查
        if (element instanceof PsiDocCommentOwner) {
            PsiDocComment docComment = ((PsiDocCommentOwner) element).getDocComment();
            return docComment == null;
        }
        // Kotlin 元素检查 - 直接检查具体的 Kotlin 元素类型
        if (element instanceof KtClassOrObject) {
            KDoc docComment = ((KtDeclaration) element).getDocComment();
            return docComment == null;
        }
        if (element instanceof KtNamedFunction) {
            KDoc docComment = ((KtNamedFunction) element).getDocComment();
            return docComment == null;
        }
        if (element instanceof KtProperty) {
            KDoc docComment = ((KtProperty) element).getDocComment();
            return docComment == null;
        }
        return true;
    }

    /**
     * 判断是否应该为元素生成文档
     *
     * <p>根据用户配置决定是否为指定元素生成文档。
     * 主要检查 overrideExisting 配置项，如果为 false（默认）则跳过已有文档的元素。
     *
     * <p>检查逻辑：
     * <ol>
     *   <li>如果 overrideExisting 为 true，总是返回 true（覆盖已有注释）</li>
     *   <li>如果 overrideExisting 为 false（默认）且元素支持文档：
     *     <ul>
     *       <li>检查元素是否已有 Javadoc 注释</li>
     *       <li>如果已有注释返回 false（跳过），否则返回 true（生成）</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param element PSI 元素
     * @return 如果应该生成文档返回 true，否则返回 false
     * @see SettingsState#overrideExisting
     * @see #hasNoJavaDoc(PsiElement)
     */
    private boolean shouldGenerateForElement(@NotNull PsiElement element) {
        // 如果配置为覆盖已有注释，总是生成
        if (settings.overrideExisting) {
            return true;
        }

        // 如果不允许覆盖已有注释，检查是否存在文档
        return hasNoJavaDoc(element);
    }

    /**
     * 判断是否为测试方法
     *
     * <p>检查方法是否被 JUnit 4 或 JUnit 5 的 @Test 注解标记。
     * 用于区分普通方法和测试方法，以便使用不同的 Prompt 模板。
     *
     * <p>支持的注解：
     * <ul>
     *   <li>org.junit.Test (JUnit 4)</li>
     *   <li>org.junit.jupiter.api.Test (JUnit 5)</li>
     * </ul>
     *
     * @param method PSI 方法对象
     * @return 如果是测试方法返回 true，否则返回 false
     */
    private boolean isTestMethod(@NotNull PsiMethod method) {
        PsiAnnotation junit4 = method.getModifierList().findAnnotation("org.junit.Test");
        PsiAnnotation junit5 = method.getModifierList().findAnnotation("org.junit.jupiter.api.Test");
        return junit4 != null || junit5 != null;
    }

    /**
     * 判断是否为 Kotlin 测试函数
     *
     * <p>检查 Kotlin 函数是否被 JUnit 4 或 JUnit 5 的 @Test 注解标记。
     * 用于区分普通函数和测试函数，以便使用不同的 Prompt 模板。
     *
     * <p>支持的注解：
     * <ul>
     *   <li>org.junit.Test (JUnit 4)</li>
     *   <li>org.junit.jupiter.api.Test (JUnit 5)</li>
     * </ul>
     *
     * @param function Kotlin 函数对象
     * @return 如果是测试函数返回 true，否则返回 false
     */
    private boolean isKotlinTestMethod(@NotNull KtNamedFunction function) {
        for (org.jetbrains.kotlin.psi.KtAnnotationEntry entry : function.getAnnotationEntries()) {
            // 获取注解的短名称
            org.jetbrains.kotlin.name.Name shortName = entry.getShortName();
            if (shortName != null && "Test".equals(shortName.asString())) {
                // 检查注解文本中是否包含 JUnit 包名
                String entryText = entry.getText();
                if (entryText != null) {
                    // 检查是否包含 JUnit 4 或 JUnit 5 的 Test 注解
                    if (entryText.contains("org.junit.Test") ||
                        entryText.contains("org.junit.jupiter.api.Test") ||
                        entryText.contains("junit.Test") ||
                        entryText.contains("jupiter.api.Test")) {
                        return true;
                    }
                }
                // 如果注解名是 Test，也认为是测试方法（大多数情况下）
                // 因为 Kotlin 中通常使用 @Test 注解
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为 Java 或 Kotlin 文件
     *
     * <p>通过文件扩展名判断是否为 Java 或 Kotlin 文件。
     * 不区分大小写，支持 .java 和 .kt 扩展名。
     *
     * @param file 虚拟文件对象
     * @return 如果是 Java 或 Kotlin 文件返回 true，否则返回 false
     */
    private boolean isJavaFile(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        return PluginContents.JAVA.equalsIgnoreCase(extension) || PluginContents.KOTLIN_EXTENSION.equalsIgnoreCase(extension);
    }
}

