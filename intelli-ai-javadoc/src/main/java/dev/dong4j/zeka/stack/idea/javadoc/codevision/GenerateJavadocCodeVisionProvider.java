package dev.dong4j.zeka.stack.idea.javadoc.codevision;

import com.google.common.collect.Lists;

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind;
import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.codeInsight.codeVision.CodeVisionProvider;
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
import com.intellij.codeInsight.codeVision.CodeVisionState;
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import dev.dong4j.zeka.stack.idea.javadoc.PluginContents;
import dev.dong4j.zeka.stack.idea.javadoc.action.AbstractGenerateJavaDocAction;
import dev.dong4j.zeka.stack.idea.javadoc.action.GenerateJavadocIntentionAction;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.javadoc.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.PsiElementLocator;
import icons.AIJicons;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import lombok.extern.slf4j.Slf4j;

/**
 * 生成 Javadoc Code Vision 提供者
 * <p>
 * 该类实现了 CodeVisionProvider 接口，用于在代码上方显示"Generate Javadoc"的可点击提示。
 * 类似于 IDEA 中的"x usages"提示，会在没有 Javadoc 的代码元素上显示可点击的链接。
 *
 * <p><b>⚠️ 重要提示：</b>
 * <ul>
 *   <li>CodeVisionProvider API 被标记为 {@code @ApiStatus.Experimental}，属于实验性 API</li>
 *   <li>该 API 可能在未来的 IntelliJ Platform 版本中发生变化或被移除</li>
 *   <li>如果 API 发生重大变化，需要相应更新代码</li>
 *   <li>目前该 API 在 IntelliJ IDEA 2022.3+ 版本中可用，但稳定性不保证</li>
 * </ul>
 *
 * <p><b>替代方案：</b>
 * <ul>
 *   <li>如果 Code Vision 功能不可用，用户仍可使用 Intention Action (Alt+Enter)</li>
 *   <li>已实现的 {@link GenerateJavadocIntentionAction}
 *       提供了更稳定的替代实现</li>
 * </ul>
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @see com.intellij.codeInsight.codeVision.CodeVisionProvider
 * @see GenerateJavadocIntentionAction
 * @since 2.6.0
 */
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class GenerateJavadocCodeVisionProvider implements CodeVisionProvider<Unit> {
    /**
     * 获取默认的 CodeVision 锚点类型
     * <p>
     * 该方法返回 CodeVision 的默认锚点位置, 默认值为 {@link CodeVisionAnchorKind#Right}.
     *
     * @return 默认的锚点类型
     */
    @Override
    public @NotNull CodeVisionAnchorKind getDefaultAnchor() {
        return CodeVisionAnchorKind.Right;
    }

    /**
     * 获取此实现的唯一标识符
     * <p> 该方法返回固定字符串 {@code "GenerateJavadocCodeVisionProvider"}, 用于标识当前实现
     *
     * @return 该实现的唯一标识符
     */
    @Override
    public @NotNull String getId() {
        return "GenerateJavadocCodeVisionProvider";
    }

    /**
     * 获取名称
     * <p>
     * 该方法返回固定字符串 "Generate Javadoc Code Vision", 并保证返回值不为 {@code null}.
     *
     * @return 名称字符串
     */
    @Override
    public @NotNull String getName() {
        return "Generate Javadoc Code Vision";
    }

    /**
     * 获取相对排序列表
     * <p>
     * 该方法返回一个 {@link CodeVisionRelativeOrdering} 的列表, 表示相对排序信息.
     * 当前实现返回一个空列表.
     *
     * @return 代码视图的相对排序列表, 永不为 null
     */
    @Override
    public @NotNull List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return Lists.newArrayList();
    }

    /**
     * 计算当前编辑器的 Code Vision 条目.
     * <p>
     * 根据编辑器所在项目的设置, 若开启了生成 Javadoc 提示, 则在项目中查找对应的 Java 或 Kotlin 文件,
     * 并收集可显示的 Code Vision 条目. 收集过程在智能模式下同步执行, 最终返回{@link CodeVisionState.Ready}
     * 包含所有条目的列表. 若项目已释放, 设置未开启或文件类型不支持, 则返回空列表.
     *
     * @param editor 当前编辑器实例
     * @param data   从 {@link #precomputeOnUiThread} 返回的 Unit 对象, 如果为 null 表示早期检查失败
     * @return 包含收集到的 Code Vision 条目的 {@link CodeVisionState.Ready} 实例
     */
    @SuppressWarnings("D")
    @Override
    public @NotNull CodeVisionState computeCodeVision(@NotNull Editor editor, Unit data) {
        try {
            // 如果 precomputeOnUiThread 返回 null，说明快速检查失败，直接返回空列表
            if (data == null) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            Project project = editor.getProject();
            if (project == null || project.isDisposed()) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            // 获取文件（已经在 precomputeOnUiThread 中检查过文件系统类型，这里再次获取用于后续处理）
            VirtualFile virtualFile = editor.getVirtualFile();
            if (virtualFile == null || editor.isDisposed()) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            SettingsState settings = SettingsState.getInstance();

            // 使用线程安全的集合来收集条目
            ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entriesQueue = new ConcurrentLinkedQueue<>();

            // 使用 NonBlockingReadAction 等待索引就绪（推荐替代已弃用的 DumbService.runReadActionInSmartMode）
            try {
                ReadAction.nonBlocking(() -> {
                    // 再次检查项目状态（在 ReadAction 内部）
                    if (project.isDisposed() || editor.isDisposed()) {
                        return null;
                    }

                    // 检查文件是否在项目内（必须在 ReadAction 中执行）
                    // 注意：ProjectFileIndex.isInProject() 需要 ReadAction 保护
                    if (!isFileInProject(project, virtualFile)) {
                        return null;
                    }

                    PsiFile psiFile = PsiUtil.getPsiFile(project, virtualFile);

                    // 只支持 Java 和 Kotlin 文件
                    if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
                        return null;
                    }

                    // 检查是否支持 Kotlin
                    if (psiFile instanceof KtFile) {
                        if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
                            return null;
                        }
                    }

                    // 处理 Java 文件
                    if (psiFile instanceof PsiJavaFile) {
                        collectJavaEntries((PsiJavaFile) psiFile, entriesQueue, project, settings);
                    }

                    // 处理 Kotlin 文件
                    if (psiFile instanceof KtFile) {
                        collectKotlinEntries((KtFile) psiFile, entriesQueue, project, settings);
                    }

                    return null;
                }).inSmartMode(project).executeSynchronously();
            } catch (ProcessCanceledException e) {
                // 如果 ReadAction 被取消（例如项目被 disposed），返回空列表
                log.debug("计算 Code Vision 时被取消", e);
                // ProcessCanceledException 必须抛出, 不能捕获处理
                throw e;
            } catch (Exception ex) {
                // 如果 ReadAction 执行失败（例如项目被 disposed），返回空列表
                log.debug("计算 Code Vision 时发生异常", ex);
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            // 再次检查项目状态（在执行后）
            if (project.isDisposed() || editor.isDisposed()) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            // 将队列转换为列表
            List<Pair<TextRange, CodeVisionEntry>> entries = new ArrayList<>(entriesQueue);
            return new CodeVisionState.Ready(entries);
        } catch (Exception e) {
            // 捕获所有异常，避免影响 Code Vision 系统
            log.debug("计算 Code Vision 时发生未预期的异常", e);
            return new CodeVisionState.Ready(Collections.emptyList());
        }
    }

    /**
     * 根据当前项目设置, 收集 Java 文件中的类, 方法和字段的 CodeVision 条目, 并添加到指定的线程安全队列中.
     * <p>
     * 该方法首先检查是否支持 Java 语言; 若不支持则直接返回. 若 {@link SettingsState#generateForClass} 为 {@code true},
     * 则遍历文件中的所有类, 并根据 {@link GenerateJavadocCodeVisionProvider#shouldShowHintElement(PsiElement, SettingsState)} 判断是否需要生成类级别的提示.
     * 接着再次遍历所有类, 对每个类根据 {@link SettingsState#generateForMethod} 与 {@link SettingsState#generateForField} 的配置分别收集方法和字段级别的提示信息.
     * 所有生成的 {@link CodeVisionEntry} 都会与对应的文本范围一起添加到 {@code entries} 队列中.
     * <p>
     * <b> 性能优化说明:</b>
     * <ul>
     * <li> 使用串行循环处理, 避免 parallelStream 导致的死锁问题 </li>
     * <li> 限制最大处理数量, 防止处理过多元素导致卡顿 </li>
     * <li> 已在 ReadAction.nonBlocking() 中执行, 无需嵌套 ReadAction</li>
     * </ul>
     * <p>
     * <b>⚠️ 重要:</b> 修复了使用 parallelStream 导致的 IDEA 卡死问题.
     * 在 CodeVision 计算中不能使用并行流, 因为会与 IDEA 的写锁机制冲突.
     *
     * @param javaFile 当前要分析的 Java 文件
     * @param entries  用于收集生成的 {@link CodeVisionEntry} 与其文本范围的线程安全队列
     * @param project  当前项目上下文
     * @param settings 当前设置状态
     */
    @SuppressWarnings( {"D", "DuplicatedCode"})
    private void collectJavaEntries(@NotNull PsiJavaFile javaFile,
                                    @NotNull ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entries,
                                    @NotNull Project project,
                                    SettingsState settings) {

        // 检查是否支持 Java 语言
        if (!settings.isLanguageSupported(PluginContents.JAVA)) {
            return;
        }

        // 获取所有类（包括内部类，递归查找）
        // 使用 findChildrenOfType 而不是 getChildrenOfType，以支持内部类
        Collection<PsiClass> allClassesCollection = PsiTreeUtil.findChildrenOfType(javaFile, PsiClass.class);
        if (allClassesCollection.isEmpty()) {
            return;
        }
        PsiClass[] allClasses = allClassesCollection.toArray(new PsiClass[0]);

        // 限制最大处理数量，防止处理过多元素导致卡顿
        final int MAX_CLASSES = 50;
        final int MAX_METHODS_PER_CLASS = 100;
        final int MAX_FIELDS_PER_CLASS = 100;

        // 串行处理类级别的 Code Vision 条目
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForClass) {
            int classCount = 0;
            for (PsiClass psiClass : allClasses) {
                if (classCount >= MAX_CLASSES || project.isDisposed()) {
                    break;
                }
                // 检查元素有效性
                if (psiClass != null && psiClass.isValid()) {
                    try {
                        if (shouldShowHintElement(psiClass, settings)) {
                            entries.add(createCodeVisionEntry(psiClass, project, settings));
                        }
                    } catch (Exception e) {
                        log.debug("处理类时发生异常", e);
                    }
                }
                classCount++;
            }
        }

        // 串行处理类内的方法和字段
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        int classCount = 0;
        for (PsiClass psiClass : allClasses) {
            if (classCount >= MAX_CLASSES || project.isDisposed()) {
                break;
            }
            // 检查元素有效性
            if (psiClass == null || !psiClass.isValid()) {
                classCount++;
                continue;
            }

            try {
                // 收集方法
                if (settings.generateForMethod) {
                    PsiMethod[] methods = psiClass.getMethods();
                    int methodCount = 0;
                    for (PsiMethod method : methods) {
                        if (methodCount >= MAX_METHODS_PER_CLASS) {
                            break;
                        }
                        if (method != null && method.isValid()) {
                            try {
                                if (shouldShowHintElement(method, settings)) {
                                    entries.add(createCodeVisionEntry(method, project, settings));
                                }
                            } catch (Exception e) {
                                log.debug("处理方法时发生异常", e);
                            }
                        }
                        methodCount++;
                    }
                }

                // 收集字段
                if (settings.generateForField) {
                    PsiField[] fields = psiClass.getFields();
                    int fieldCount = 0;
                    for (PsiField field : fields) {
                        if (fieldCount >= MAX_FIELDS_PER_CLASS) {
                            break;
                        }
                        if (field != null && field.isValid()) {
                            try {
                                if (shouldShowHintElement(field, settings)) {
                                    entries.add(createCodeVisionEntry(field, project, settings));
                                }
                            } catch (Exception e) {
                                log.debug("处理字段时发生异常", e);
                            }
                        }
                        fieldCount++;
                    }
                }
            } catch (Exception e) {
                log.debug("处理类的方法和字段时发生异常", e);
            }
            classCount++;
        }
    }

    /**
     * 根据当前项目设置收集 Kotlin 文件中的 CodeVision 条目.
     * <p>
     * 该方法首先检查 Kotlin 语言是否被启用; 若未启用则直接返回. 随后使用 {@link KtTreeVisitorVoid}
     * 递归遍历整个 Kotlin 文件, 收集所有类、函数和属性, 然后串行处理这些元素,
     * 根据 {@link SettingsState} 的配置决定是否为各种类型的元素生成 CodeVision 条目.
     * <p>
     * <b>遍历范围：</b>
     * <ul>
     *   <li>顶层函数和属性（不在类内部的）</li>
     *   <li>类内部的函数和属性</li>
     *   <li>嵌套类内部的函数和属性</li>
     *   <li>所有层级的类和对象声明</li>
     * </ul>
     * <p>
     * <b>性能优化说明：</b>
     * <ul>
     *   <li>先收集所有元素到列表（单次遍历）</li>
     *   <li>使用串行循环处理，避免 parallelStream 导致的死锁问题</li>
     *   <li>限制最大处理数量，防止处理过多元素导致卡顿</li>
     *   <li>已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction</li>
     * </ul>
     * <p>
     * <b>⚠️ 重要：</b> 修复了使用 parallelStream 导致的 IDEA 卡死问题。
     * 在 CodeVision 计算中不能使用并行流，因为会与 IDEA 的写锁机制冲突。
     *
     * @param ktFile   需要分析的 Kotlin 文件
     * @param entries  用于收集生成的 CodeVision 条目的线程安全队列, 方法会向其中追加条目
     * @param project  当前项目上下文
     * @param settings 当前设置状态
     */
    @SuppressWarnings( {"D", "DuplicatedCode"})
    private void collectKotlinEntries(@NotNull KtFile ktFile,
                                      @NotNull ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entries,
                                      @NotNull Project project, SettingsState settings) {
        // 检查是否支持 Kotlin 语言
        if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
            return;
        }

        // 收集所有元素到列表
        List<KtClassOrObject> allClasses = new ArrayList<>();
        List<KtNamedFunction> allFunctions = new ArrayList<>();
        List<KtProperty> allProperties = new ArrayList<>();

        // 使用 KtTreeVisitorVoid 递归遍历整个文件，收集所有元素
        // 这样可以获取到所有层级的函数和属性，而不仅仅是类的直接子节点
        ktFile.accept(new KtTreeVisitorVoid() {
            /**
             * 访问类或对象声明
             * <p> 在遍历 Kotlin 代码结构时, 处理类或对象声明节点. 将找到的类或对象添加到全局类列表中
             *
             * @param classOrObject 类或对象声明节点, 不能为空
             */
            @Override
            public void visitClassOrObject(@NotNull KtClassOrObject classOrObject) {
                super.visitClassOrObject(classOrObject);
                allClasses.add(classOrObject);
            }

            @Override
            public void visitNamedFunction(@NotNull KtNamedFunction function) {
                super.visitNamedFunction(function);
                allFunctions.add(function);
            }

            /**
             * 访问属性节点并将其添加到属性列表中
             * <p> 此方法用于遍历 Kotlin 语法树中的属性节点, 并将该属性对象添加到内部维护的属性列表中.
             *
             * @param property 被访问的属性节点, 不能为空
             */
            @Override
            public void visitProperty(@NotNull KtProperty property) {
                super.visitProperty(property);
                allProperties.add(property);
            }
        });

        // 限制最大处理数量，防止处理过多元素导致卡顿
        final int MAX_CLASSES = 50;
        final int MAX_FUNCTIONS = 200;
        final int MAX_PROPERTIES = 200;

        // 串行处理类级别的 Code Vision 条目
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForClass && !allClasses.isEmpty()) {
            int classCount = 0;
            for (KtClassOrObject ktClass : allClasses) {
                if (classCount >= MAX_CLASSES || project.isDisposed()) {
                    break;
                }
                // 检查元素有效性
                if (ktClass != null && ktClass.isValid()) {
                    try {
                        if (shouldShowHintElement(ktClass, settings)) {
                            entries.add(createCodeVisionEntry(ktClass, project, settings));
                        }
                    } catch (Exception e) {
                        log.debug("处理 Kotlin 类时发生异常", e);
                    }
                }
                classCount++;
            }
        }

        // 串行处理函数级别的 Code Vision 条目（包括顶层函数和类内函数）
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForMethod && !allFunctions.isEmpty()) {
            int functionCount = 0;
            for (KtNamedFunction function : allFunctions) {
                if (functionCount >= MAX_FUNCTIONS || project.isDisposed()) {
                    break;
                }
                // 检查元素有效性
                if (function != null && function.isValid()) {
                    try {
                        if (shouldShowHintElement(function, settings)) {
                            entries.add(createCodeVisionEntry(function, project, settings));
                        }
                    } catch (Exception e) {
                        log.debug("处理 Kotlin 函数时发生异常", e);
                    }
                }
                functionCount++;
            }
        }

        // 串行处理属性级别的 Code Vision 条目（包括顶层属性和类内属性）
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForField && !allProperties.isEmpty()) {
            int propertyCount = 0;
            for (KtProperty property : allProperties) {
                if (propertyCount >= MAX_PROPERTIES || project.isDisposed()) {
                    break;
                }
                // 检查元素有效性
                if (property != null && property.isValid()) {
                    try {
                        if (shouldShowHintElement(property, settings)) {
                            entries.add(createCodeVisionEntry(property, project, settings));
                        }
                    } catch (Exception e) {
                        log.debug("处理 Kotlin 属性时发生异常", e);
                    }
                }
                propertyCount++;
            }
        }
    }

    /**
     * 判断是否应该显示提示（Kotlin 元素）
     * <p>
     * 检查条件：
     * <ol>
     *   <li>如果允许覆盖已有注释（overrideExisting = true），总是显示</li>
     *   <li>如果不允许覆盖，只在元素没有 KDoc 时显示</li>
     * </ol>
     *
     * @param element  元素
     * @param settings 设置
     * @return 如果应该显示返回 true
     */
    private boolean shouldShowHintElement(@NotNull PsiElement element, @NotNull SettingsState settings) {
        // 如果允许覆盖，总是显示
        if (settings.overrideExisting) {
            return true;
        }

        // 使用工具类检查是否已有 Javadoc/KDoc
        return !PsiElementLocator.hasJavaDoc(element);
    }

    /**
     * 创建 Code Vision 条目（Java 和 Kotlin 元素通用）
     * <p>
     * 该方法统一处理 Java 和 Kotlin 元素的 Code Vision 条目创建，
     * 因为两者的创建逻辑完全相同。
     *
     * @param element  元素（可以是 Java 或 Kotlin 元素）
     * @param project  项目
     * @param settings 当前设置状态
     * @return 条目对（TextRange 和 CodeVisionEntry）
     */
    @NotNull
    private Pair<TextRange, CodeVisionEntry> createCodeVisionEntry(@NotNull PsiElement element,
                                                                   @NotNull Project project,
                                                                   SettingsState settings) {
        // 获取文本范围，如果为 null 则使用空范围
        TextRange textRange = element.getTextRange();
        if (textRange == null) {
            // 如果无法获取文本范围，使用元素所在文件的起始位置
            PsiFile containingFile = element.getContainingFile();
            if (containingFile != null) {
                textRange = new TextRange(0, 0);
            } else {
                // 如果连文件都无法获取，返回空范围
                textRange = TextRange.EMPTY_RANGE;
            }
        }

        String text = JavadocBundle.message("codevision.generate.javadoc");
        if (settings.overrideExisting && PsiElementLocator.hasJavaDoc(element)) {
            text = JavadocBundle.message("codevision.override.javadoc");
        }

        ClickableTextCodeVisionEntry entry = new ClickableTextCodeVisionEntry(
            text,
            getId(),
            createClickHandler(element, project),
            AIJicons.AIJ_16,
            text,
            JavadocBundle.message("codevision.generate.javadoc.tooltip"),
            Collections.emptyList()
        );

        return new Pair<>(textRange, entry);
    }

    /**
     * 创建点击处理器
     *
     * @param element 元素
     * @param project 项目
     * @return 点击处理器函数（Kotlin 函数类型）
     */
    @NotNull
    private Function2<MouseEvent, Editor, Unit> createClickHandler(@NotNull PsiElement element,
                                                                   @NotNull Project project) {
        return (event, editor) -> {
            PsiFile psiFile = element.getContainingFile();
            if (psiFile == null) {
                return Unit.INSTANCE;
            }

            // 使用基础 Action 的处理逻辑
            AbstractGenerateJavaDocAction baseAction = new AbstractGenerateJavaDocAction() {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    // 不需要实现，因为直接调用 process 方法
                }
            };

            // 创建一个临时的 AnActionEvent 来触发生成
            // 由于我们需要精确定位到元素，我们需要手动定位编辑器光标到元素位置
            // 或者直接调用 TaskCollector 收集该元素的任务
            ApplicationManager.getApplication().executeOnPooledThread(
                () -> ReadAction.run(
                    () -> {
                        // 收集该元素的任务
                        TaskCollector collector = new TaskCollector(project);
                        List<DocumentationTask> tasks = collector.collectFromElement(element);

                        // 生成文档
                        String targetDescription = PsiElementLocator.getElementDescription(element);

                        ApplicationManager.getApplication().invokeLater(
                            () -> baseAction.generateDocumentation(project, tasks, targetDescription));
                    }));

            return Unit.INSTANCE;
        };
    }

    /**
     * 判断是否适用于指定的项目
     * <p>
     * 该方法检查以下条件：
     * <ol>
     *   <li>项目是否有效（未 disposed）</li>
     *   <li>设置中是否启用了 Code Vision 提示</li>
     * </ol>
     * <p>
     * 注意：该方法只检查项目级别的可用性，不检查具体文件。
     * 文件级别的检查（如是否在项目内、是否在 jar 中等）在 {@link #computeCodeVision} 中进行。
     *
     * @param project 目标项目
     * @return 如果项目有效且启用了 Code Vision 提示，返回 true；否则返回 false
     */
    @Override
    public boolean isAvailableFor(@NotNull Project project) {
        // 检查项目是否有效
        if (project.isDisposed()) {
            return false;
        }

        // 检查设置：是否显示 Code Vision
        SettingsState settings = SettingsState.getInstance();
        return settings.showGenerateJavadocHint;
    }

    /**
     * 检查文件是否在项目内（排除 jar 中的源码）
     * <p>
     * 该方法检查以下条件：
     * <ol>
     *   <li>文件是否在 JarFileSystem 中（jar 中的源码）</li>
     *   <li>文件是否在本地文件系统中（不是 jar 中的文件）</li>
     *   <li>文件是否在项目的源码根目录或资源根目录中</li>
     * </ol>
     * <p>
     * <b>重要：</b>该方法必须在 ReadAction 中调用，因为 {@link ProjectFileIndex#isInProject(VirtualFile)}
     * 需要访问项目文件索引，必须在 ReadAction 中执行。
     *
     * @param project     项目对象
     * @param virtualFile 虚拟文件
     * @return 如果文件在项目内且不是 jar 中的源码，返回 true；否则返回 false
     */
    private boolean isFileInProject(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // 检查文件是否在 jar 中（jar 中的源码不应该生成 Code Vision）
        if (virtualFile.getFileSystem() instanceof JarFileSystem) {
            return false;
        }

        // 检查文件是否在本地文件系统中
        if (!(virtualFile.getFileSystem() instanceof LocalFileSystem)) {
            return false;
        }

        // 检查文件是否在项目的源码根目录或资源根目录中
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        return fileIndex.isInProject(virtualFile);
    }

    /**
     * 在 UI 线程上预计算操作
     * <p>
     * 该方法在 UI 线程上执行快速检查，避免在后台线程中执行不必要的计算。
     * 检查结果通过返回值传递给 {@link #computeCodeVision} 方法。
     * <p>
     * 检查内容（仅快速检查，不涉及慢操作）：
     * <ol>
     *   <li>项目是否有效（未 disposed）</li>
     *   <li>编辑器是否有效</li>
     *   <li>文件系统类型检查（排除 jar 中的源码）</li>
     * </ol>
     * <p>
     * 注意：文件是否在项目内的详细检查（使用 ProjectFileIndex）是慢操作，
     * 不能在 EDT 上执行，因此移到 {@link #computeCodeVision} 的后台线程中。
     * <p>
     * 如果检查失败，返回 {@code null}，{@link #computeCodeVision} 将提前返回空列表。
     *
     * @param editor 编辑器实例, 用于操作或获取编辑相关数据
     * @return 如果快速检查通过返回 {@link Unit#INSTANCE}，否则返回 {@code null}
     */
    @Override
    public Unit precomputeOnUiThread(@NotNull Editor editor) {
        // 检查项目是否有效
        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            return null;
        }

        SettingsState settings = SettingsState.getInstance();
        if (!settings.showGenerateJavadocHint) {
            return null;
        }

        // 检查编辑器是否有效
        VirtualFile virtualFile = editor.getVirtualFile();
        if (virtualFile == null || editor.isDisposed()) {
            return null;
        }

        // 快速检查：文件系统类型（排除 jar 中的源码）
        // 注意：这是快速检查，不涉及慢操作
        if (virtualFile.getFileSystem() instanceof JarFileSystem) {
            return null;
        }

        // 快速检查：只处理本地文件系统
        if (!(virtualFile.getFileSystem() instanceof LocalFileSystem)) {
            return null;
        }

        // 快速检查通过，返回 Unit.INSTANCE 表示可以继续处理
        // 详细的文件索引检查将在 computeCodeVision 的后台线程中执行
        return Unit.INSTANCE;
    }

}

