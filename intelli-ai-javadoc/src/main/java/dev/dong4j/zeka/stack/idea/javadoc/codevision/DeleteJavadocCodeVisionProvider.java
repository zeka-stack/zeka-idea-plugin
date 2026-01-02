package dev.dong4j.zeka.stack.idea.javadoc.codevision;

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind;
import com.intellij.codeInsight.codeVision.CodeVisionEntry;
import com.intellij.codeInsight.codeVision.CodeVisionProvider;
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering;
import com.intellij.codeInsight.codeVision.CodeVisionState;
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
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
import dev.dong4j.zeka.stack.idea.javadoc.service.JavadocDeletionService;
import dev.dong4j.zeka.stack.idea.javadoc.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.javadoc.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.javadoc.util.NotificationUtil;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import lombok.extern.slf4j.Slf4j;

/**
 * 删除 Javadoc Code Vision 提供者
 * <p>
 * 该类实现了 CodeVisionProvider 接口，用于在代码上方显示"Delete Javadoc"的可点击提示。
 * 只在已有 Javadoc 注释的元素上显示删除链接。
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @since 2.6.0
 */
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class DeleteJavadocCodeVisionProvider implements CodeVisionProvider<Unit> {

    /**
     * 删除 Javadoc 操作的服务实例
     * <p>
     * 负责执行删除 Javadoc 的具体操作.
     *
     * @see JavadocDeletionService
     */
    private final JavadocDeletionService deletionService = new JavadocDeletionService();

    /**
     * 获取默认的 CodeVision 锚点类型
     *
     * @return 默认的锚点类型
     */
    @Override
    public @NotNull CodeVisionAnchorKind getDefaultAnchor() {
        return CodeVisionAnchorKind.Right;
    }

    /**
     * 获取提供者的唯一标识符
     *
     * @return 提供者 ID
     */
    @NotNull
    @Override
    public String getId() {
        return "DeleteJavadocCodeVisionProvider";
    }

    /**
     * 获取提供者的名称
     *
     * @return 提供者名称
     */
    @NotNull
    @Override
    public String getName() {
        return JavadocBundle.message("codevision.delete.javadoc.provider.name");
    }

    /**
     * 计算 Code Vision 条目
     *
     * @param editor 编辑器实例
     * @return Code Vision 状态对象, 包含收集到的条目列表
     */
    @Override
    public @NotNull CodeVisionState computeCodeVision(@NotNull Editor editor, Unit data) {
        try {
            // 检查项目状态
            Project project = editor.getProject();
            if (project == null || project.isDisposed() || editor.isDisposed()) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            // 检查配置是否允许删除
            SettingsState settings = SettingsState.getInstance();
            if (!settings.allowDeleteJavadoc) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            VirtualFile virtualFile = editor.getVirtualFile();
            if (virtualFile == null) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            // // 检查文件是否在项目内 todo-dong4j : (2025.12.17 10:45) [慢操作 会报异常]
            // ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
            // if (!fileIndex.isInSource(virtualFile)) {
            //     return new CodeVisionState.Ready(Collections.emptyList());
            // }
            //
            // // 检查文件是否在 jar 中
            // if (virtualFile.getFileSystem() instanceof JarFileSystem) {
            //     return new CodeVisionState.Ready(Collections.emptyList());
            // }
            //
            // // 检查文件是否在本地文件系统
            // if (!(virtualFile.getFileSystem() instanceof LocalFileSystem)) {
            //     return new CodeVisionState.Ready(Collections.emptyList());
            // }

            // 使用线程安全的队列收集条目
            ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entriesQueue = new ConcurrentLinkedQueue<>();

            try {
                ReadAction.nonBlocking(() -> {
                    PsiFile psiFile = PsiUtil.getPsiFile(project, virtualFile);

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
                log.debug("计算 Code Vision 时被取消", e);
                throw e;
            } catch (Exception ex) {
                log.debug("计算 Code Vision 时发生异常", ex);
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            // 再次检查项目状态
            if (project.isDisposed() || editor.isDisposed()) {
                return new CodeVisionState.Ready(Collections.emptyList());
            }

            // 将队列转换为列表
            List<Pair<TextRange, CodeVisionEntry>> entries = new ArrayList<>(entriesQueue);
            return new CodeVisionState.Ready(entries);
        } catch (Exception e) {
            log.debug("计算 Code Vision 时发生未预期的异常", e);
            return new CodeVisionState.Ready(Collections.emptyList());
        }
    }

    /**
     * 收集 Java 文件中有 Javadoc 的元素
     * <p>
     * <b>⚠️ 重要:</b> 修复了使用 parallelStream 导致的 IDEA 卡死问题.
     * 在 CodeVision 计算中不能使用并行流, 因为会与 IDEA 的写锁机制冲突.
     *
     * @param javaFile Java 文件
     * @param entries  条目队列
     * @param project  项目
     * @param settings 设置
     */
    private void collectJavaEntries(@NotNull PsiJavaFile javaFile,
                                    @NotNull ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entries,
                                    @NotNull Project project,
                                    SettingsState settings) {
        // 检查是否支持 Java 语言
        if (!settings.isLanguageSupported(PluginContents.JAVA)) {
            return;
        }

        // 获取所有类
        Collection<PsiClass> allClassesCollection = PsiTreeUtil.findChildrenOfType(javaFile, PsiClass.class);
        if (allClassesCollection.isEmpty()) {
            return;
        }
        PsiClass[] allClasses = allClassesCollection.toArray(new PsiClass[0]);

        // 限制最大处理数量，防止处理过多元素导致卡顿
        final int MAX_CLASSES = 50;
        final int MAX_METHODS_PER_CLASS = 100;
        final int MAX_FIELDS_PER_CLASS = 100;

        // 串行处理类
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForClass) {
            int classCount = 0;
            for (PsiClass psiClass : allClasses) {
                if (classCount >= MAX_CLASSES || project.isDisposed()) {
                    break;
                }
                if (psiClass != null && psiClass.isValid()) {
                    try {
                        if (JavadocDeletionService.hasDocComment(psiClass)) {
                            entries.add(createCodeVisionEntry(psiClass, project));
                        }
                    } catch (Exception e) {
                        log.debug("处理类时发生异常", e);
                    }
                }
                classCount++;
            }
        }

        // 串行处理方法
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForMethod) {
            int classCount = 0;
            for (PsiClass psiClass : allClasses) {
                if (classCount >= MAX_CLASSES || project.isDisposed()) {
                    break;
                }
                if (psiClass == null || !psiClass.isValid()) {
                    classCount++;
                    continue;
                }

                try {
                    PsiMethod[] methods = psiClass.getMethods();
                    int methodCount = 0;
                    for (PsiMethod method : methods) {
                        if (methodCount >= MAX_METHODS_PER_CLASS) {
                            break;
                        }
                        if (method != null && method.isValid()) {
                            try {
                                if (JavadocDeletionService.hasDocComment(method)) {
                                    entries.add(createCodeVisionEntry(method, project));
                                }
                            } catch (Exception e) {
                                log.debug("处理方法时发生异常", e);
                            }
                        }
                        methodCount++;
                    }
                } catch (Exception e) {
                    log.debug("处理类方法时发生异常", e);
                }
                classCount++;
            }
        }

        // 串行处理字段
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForField) {
            int classCount = 0;
            for (PsiClass psiClass : allClasses) {
                if (classCount >= MAX_CLASSES || project.isDisposed()) {
                    break;
                }
                if (psiClass == null || !psiClass.isValid()) {
                    classCount++;
                    continue;
                }

                try {
                    PsiField[] fields = psiClass.getFields();
                    int fieldCount = 0;
                    for (PsiField field : fields) {
                        if (fieldCount >= MAX_FIELDS_PER_CLASS) {
                            break;
                        }
                        if (field != null && field.isValid()) {
                            try {
                                if (JavadocDeletionService.hasDocComment(field)) {
                                    entries.add(createCodeVisionEntry(field, project));
                                }
                            } catch (Exception e) {
                                log.debug("处理字段时发生异常", e);
                            }
                        }
                        fieldCount++;
                    }
                } catch (Exception e) {
                    log.debug("处理类字段时发生异常", e);
                }
                classCount++;
            }
        }
    }

    /**
     * 收集 Kotlin 文件中有 KDoc 的元素
     * <p>
     * <b>⚠️ 重要：</b> 修复了使用 parallelStream 导致的 IDEA 卡死问题。
     * 在 CodeVision 计算中不能使用并行流，因为会与 IDEA 的写锁机制冲突。
     *
     * @param ktFile   Kotlin 文件
     * @param entries  条目队列
     * @param project  项目
     * @param settings 设置
     */
    private void collectKotlinEntries(@NotNull KtFile ktFile,
                                      @NotNull ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entries,
                                      @NotNull Project project,
                                      SettingsState settings) {
        // 检查是否支持 Kotlin 语言
        if (!settings.isLanguageSupported(PluginContents.KOTLIN)) {
            return;
        }

        List<KtClassOrObject> allClasses = new ArrayList<>();
        List<KtNamedFunction> allFunctions = new ArrayList<>();
        List<KtProperty> allProperties = new ArrayList<>();

        // 使用 KtTreeVisitorVoid 递归遍历整个文件
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
             * 访问属性节点
             * <p> 此方法在访问到属性节点时被调用, 并将该属性节点添加到 allProperties 列表中
             *
             * @param property 被访问的属性节点, 不能为 null
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

        // 串行处理类
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForClass && !allClasses.isEmpty()) {
            int classCount = 0;
            for (KtClassOrObject ktClass : allClasses) {
                if (classCount >= MAX_CLASSES || project.isDisposed()) {
                    break;
                }
                if (ktClass != null && ktClass.isValid()) {
                    try {
                        if (JavadocDeletionService.hasDocComment(ktClass)) {
                            entries.add(createCodeVisionEntry(ktClass, project));
                        }
                    } catch (Exception e) {
                        log.debug("处理 Kotlin 类时发生异常", e);
                    }
                }
                classCount++;
            }
        }

        // 串行处理函数
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForMethod && !allFunctions.isEmpty()) {
            int functionCount = 0;
            for (KtNamedFunction function : allFunctions) {
                if (functionCount >= MAX_FUNCTIONS || project.isDisposed()) {
                    break;
                }
                if (function != null && function.isValid()) {
                    try {
                        if (JavadocDeletionService.hasDocComment(function)) {
                            entries.add(createCodeVisionEntry(function, project));
                        }
                    } catch (Exception e) {
                        log.debug("处理 Kotlin 函数时发生异常", e);
                    }
                }
                functionCount++;
            }
        }

        // 串行处理属性
        // 注意：已在 ReadAction.nonBlocking() 中执行，无需嵌套 ReadAction.run()
        if (settings.generateForField && !allProperties.isEmpty()) {
            int propertyCount = 0;
            for (KtProperty property : allProperties) {
                if (propertyCount >= MAX_PROPERTIES || project.isDisposed()) {
                    break;
                }
                if (property != null && property.isValid()) {
                    try {
                        if (JavadocDeletionService.hasDocComment(property)) {
                            entries.add(createCodeVisionEntry(property, project));
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
     * 创建 Code Vision 条目
     *
     * @param element 元素
     * @param project 项目
     * @return 条目对
     */
    @NotNull
    private Pair<TextRange, CodeVisionEntry> createCodeVisionEntry(@NotNull PsiElement element,
                                                                   @NotNull Project project) {
        // 获取文本范围
        TextRange textRange = element.getTextRange();
        if (textRange == null) {
            PsiFile containingFile = element.getContainingFile();
            if (containingFile != null) {
                textRange = new TextRange(0, 0);
            } else {
                textRange = TextRange.EMPTY_RANGE;
            }
        }

        String text = JavadocBundle.message("codevision.delete.javadoc");

        ClickableTextCodeVisionEntry entry = new ClickableTextCodeVisionEntry(
            text,
            getId(),
            createClickHandler(element, project),
            null, // 不使用图标，使用表情符号代替
            text,
            JavadocBundle.message("codevision.delete.javadoc.tooltip"),
            Collections.emptyList()
        );

        return new Pair<>(textRange, entry);
    }

    /**
     * 创建点击处理器
     *
     * @param element 元素
     * @param project 项目
     * @return 点击处理器函数
     */
    @NotNull
    private Function2<MouseEvent, Editor, Unit> createClickHandler(@NotNull PsiElement element,
                                                                   @NotNull Project project) {
        return (event, editor) -> {
            boolean deleted = deletionService.deleteJavadoc(project, element);
            if (!deleted) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    NotificationUtil.showInfo(project, JavadocBundle.message("notification.no.javadoc.to.delete"));
                });
            }
            return Unit.INSTANCE;
        };
    }

    /**
     * 判断是否适用于指定的项目
     *
     * @param project 项目
     * @return 如果项目有效且启用了删除功能，返回 true
     */
    @Override
    public boolean isAvailableFor(@NotNull Project project) {
        if (project.isDisposed()) {
            return false;
        }

        SettingsState settings = SettingsState.getInstance();
        return settings.allowDeleteJavadoc;
    }

    @NotNull
    @Override
    public List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return List.of();
    }

    @Override
    public Unit precomputeOnUiThread(@NotNull Editor editor) {
        return null;
    }
}

