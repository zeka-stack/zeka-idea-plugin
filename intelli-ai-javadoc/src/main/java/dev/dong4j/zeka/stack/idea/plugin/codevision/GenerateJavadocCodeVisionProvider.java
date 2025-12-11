package dev.dong4j.zeka.stack.idea.plugin.codevision;

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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import dev.dong4j.zeka.stack.idea.plugin.PluginContents;
import dev.dong4j.zeka.stack.idea.plugin.action.AbstractGenerateJavaDocAction;
import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.task.DocumentationTask;
import dev.dong4j.zeka.stack.idea.plugin.task.TaskCollector;
import dev.dong4j.zeka.stack.idea.plugin.util.JavadocBundle;
import dev.dong4j.zeka.stack.idea.plugin.util.PsiElementLocator;
import icons.AIJicons;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import lombok.Getter;
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
 *   <li>已实现的 {@link dev.dong4j.zeka.stack.idea.plugin.action.GenerateJavadocIntentionAction}
 *       提供了更稳定的替代实现</li>
 * </ul>
 *
 * @author zeka.stack.team
 * @version 1.0.0
 * @see com.intellij.codeInsight.codeVision.CodeVisionProvider
 * @see dev.dong4j.zeka.stack.idea.plugin.action.GenerateJavadocIntentionAction
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
     * <p>
     * 该方法返回固定字符串 {@code GenerateJavadocCodeVisionProvider}, 用于标识当前实现
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
     * 该方法返回固定字符串 {@code "Generate Javadoc Code Vision"}, 并保证返回值不为 {@code null}.
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
     * 该方法返回一个 {@link CodeVisionRelativeOrdering} 的列表, 表示相对排序信息. 当前实现返回一个空列表.
     *
     * @return 代码视图相对排序列表, 永不为 {@code null}
     */
    @Override
    public @NotNull List<CodeVisionRelativeOrdering> getRelativeOrderings() {
        return Lists.newArrayList();
    }

    /**
     * 计算当前编辑器的 Code Vision 条目.
     * <p>
     * 根据编辑器所在项目的设置, 若开启了生成 Javadoc 提示, 则在项目中查找对应的 Java 或 Kotlin 文件,
     * 并收集可显示的 Code Vision 条目. 收集过程在智能模式下同步执行, 最终返回 {@link CodeVisionState.Ready}
     * 包含所有条目的列表. 若项目已释放, 设置未开启或文件类型不支持, 则返回空列表.
     *
     * @param editor 当前编辑器实例
     * @param data   传入的 Unit 对象 (此方法不使用该参数)
     * @return 包含收集到的 Code Vision 条目的 {@link CodeVisionState.Ready} 实例
     */
    @SuppressWarnings("D")
    @Override
    public @NotNull CodeVisionState computeCodeVision(@NotNull Editor editor, Unit data) {
        Project project = editor.getProject();
        if (project == null || project.isDisposed()) {
            return new CodeVisionState.Ready(Collections.emptyList());
        }

        // 检查设置：是否显示 Code Vision
        SettingsState settings = SettingsState.getInstance();
        if (!settings.showGenerateJavadocHint) {
            return new CodeVisionState.Ready(Collections.emptyList());
        }

        // 使用线程安全的集合来收集条目
        ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entriesQueue = new ConcurrentLinkedQueue<>();

        // 使用 NonBlockingReadAction 等待索引就绪（推荐替代已弃用的 DumbService.runReadActionInSmartMode）
        ReadAction.nonBlocking(() -> {
            PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getVirtualFile());

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

        // 将队列转换为列表
        List<Pair<TextRange, CodeVisionEntry>> entries = new ArrayList<>(entriesQueue);

        // 转换 Pair 列表为 IntelliJ Platform API 需要的格式
        // 注意：CodeVisionState.Ready 接受 List<Pair<TextRange, CodeVisionEntry>>
        // 但由于 Java 没有标准的 Pair，我们需要使用 kotlin.Pair 或者创建兼容的列表
        // 实际上，Kotlin 的 Pair 在 Java 中可以作为元组使用
        // 但 IntelliJ Platform 的 API 可能期望特定的类型
        // 让我们直接传递 entries，让 API 处理类型转换
        List<kotlin.Pair<TextRange, CodeVisionEntry>> kotlinPairs = new ArrayList<>();
        for (Pair<TextRange, CodeVisionEntry> entry : entries) {
            kotlinPairs.add(new kotlin.Pair<>(entry.getFirst(), entry.getSecond()));
        }
        return new CodeVisionState.Ready(kotlinPairs);
    }

    /**
     * 根据当前项目设置, 收集 Java 文件中的类, 方法, 字段的 CodeVisionEntry 并添加到 {@code entries} 队列.
     * <p>
     * 该方法首先检查是否支持 Java 语言; 若不支持则直接返回. 若 {@link SettingsState#generateForClass} 为 {@code true},
     * 则遍历文件中的所有类并根据 {@link GenerateJavadocCodeVisionProvider#shouldShowHintElement(PsiElement, SettingsState)} 判断是否需要生成类级别的提示.
     * 接着再次遍历所有类, 对每个类根据 {@link SettingsState#generateForMethod} 与 {@link SettingsState#generateForField}
     * 的配置分别收集方法和字段级别的提示信息. 所有生成的 {@link CodeVisionEntry} 都会与对应的文本范围一起
     * 添加到 {@code entries} 队列中.
     * <p>
     * <b>性能优化：</b>
     * <ul>
     *   <li>使用并行流处理多个类，加快收集速度</li>
     *   <li>使用并行流处理类内的方法和字段</li>
     *   <li>使用线程安全的队列收集结果</li>
     * </ul>
     *
     * @param javaFile 当前要分析的 Java 文件
     * @param entries  用于收集生成的 {@link CodeVisionEntry} 与其文本范围的线程安全队列
     * @param project  当前项目上下文
     * @param settings 当前设置状态
     */
    @SuppressWarnings("D")
    private void collectJavaEntries(@NotNull PsiJavaFile javaFile,
                                    @NotNull ConcurrentLinkedQueue<Pair<TextRange, CodeVisionEntry>> entries,
                                    @NotNull Project project,
                                    SettingsState settings) {

        // 检查是否支持 Java 语言
        if (!settings.isLanguageSupported("java")) {
            return;
        }

        // 获取所有类（只获取一次，避免重复遍历）
        PsiClass[] allClasses = PsiTreeUtil.getChildrenOfType(javaFile, PsiClass.class);
        if (allClasses == null || allClasses.length == 0) {
            return;
        }

        // 并行处理类级别的 Code Vision 条目
        // 注意：并行流在后台线程执行，需要 ReadAction 保护 PSI 访问
        if (settings.generateForClass) {
            Stream.of(allClasses)
                .parallel()
                .forEach(psiClass -> {
                    ReadAction.run(() -> {
                        if (shouldShowHintElement(psiClass, settings)) {
                            entries.add(createCodeVisionEntry(psiClass, project, settings));
                        }
                    });
                });
        }

        // 并行处理类内的方法和字段
        // 注意：并行流在后台线程执行，需要 ReadAction 保护 PSI 访问
        Stream.of(allClasses)
            .parallel()
            .forEach(psiClass -> {
                ReadAction.run(() -> {
                    // 收集方法
                    if (settings.generateForMethod) {
                        PsiMethod[] methods = psiClass.getMethods();
                        if (methods.length > 0) {
                            Stream.of(methods)
                                .forEach(method -> {
                                    ReadAction.run(() -> {
                                        if (shouldShowHintElement(method, settings)) {
                                            entries.add(createCodeVisionEntry(method, project, settings));
                                        }
                                    });
                                });
                        }
                    }

                    // 收集字段
                    if (settings.generateForField) {
                        PsiField[] fields = psiClass.getFields();
                        if (fields.length > 0) {
                            Stream.of(fields)
                                .forEach(field -> {
                                    ReadAction.run(() -> {
                                        if (shouldShowHintElement(field, settings)) {
                                            entries.add(createCodeVisionEntry(field, project, settings));
                                        }
                                    });
                                });
                        }
                    }
                });
            });
    }

    /**
     * 根据当前项目设置收集 Kotlin 文件中的 CodeVision 条目.
     * <p>
     * 该方法首先检查 Kotlin 语言是否被启用; 若未启用则直接返回. 随后使用 {@link KtTreeVisitorVoid}
     * 递归遍历整个 Kotlin 文件, 收集所有类、函数和属性, 然后使用并行流处理这些元素,
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
     * <b>性能优化：</b>
     * <ul>
     *   <li>先收集所有元素到列表（单次遍历）</li>
     *   <li>使用并行流处理收集到的元素，加快处理速度</li>
     *   <li>使用线程安全的队列收集结果</li>
     *   <li>与 Java 版本的并行处理方式保持一致</li>
     * </ul>
     *
     * @param ktFile   需要分析的 Kotlin 文件
     * @param entries  用于收集生成的 CodeVision 条目的线程安全队列, 方法会向其中追加条目
     * @param project  当前项目上下文
     * @param settings 当前设置状态
     */
    @SuppressWarnings("D")
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

            @Override
            public void visitProperty(@NotNull KtProperty property) {
                super.visitProperty(property);
                allProperties.add(property);
            }
        });

        // 并行处理类级别的 Code Vision 条目
        // 注意：并行流在后台线程执行，需要 ReadAction 保护 PSI 访问
        if (settings.generateForClass && !allClasses.isEmpty()) {
            allClasses.parallelStream()
                .forEach(ktClass -> {
                    ReadAction.run(() -> {
                        if (shouldShowHintElement(ktClass, settings)) {
                            entries.add(createCodeVisionEntry(ktClass, project, settings));
                        }
                    });
                });
        }

        // 并行处理函数级别的 Code Vision 条目（包括顶层函数和类内函数）
        // 注意：并行流在后台线程执行，需要 ReadAction 保护 PSI 访问
        if (settings.generateForMethod && !allFunctions.isEmpty()) {
            allFunctions.parallelStream()
                .forEach(function -> {
                    ReadAction.run(() -> {
                        if (shouldShowHintElement(function, settings)) {
                            entries.add(createCodeVisionEntry(function, project, settings));
                        }
                    });
                });
        }

        // 并行处理属性级别的 Code Vision 条目（包括顶层属性和类内属性）
        // 注意：并行流在后台线程执行，需要 ReadAction 保护 PSI 访问
        if (settings.generateForField && !allProperties.isEmpty()) {
            allProperties.parallelStream()
                .forEach(property -> {
                    ReadAction.run(() -> {
                        if (shouldShowHintElement(property, settings)) {
                            entries.add(createCodeVisionEntry(property, project, settings));
                        }
                    });
                });
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
        TextRange textRange = element.getTextRange();
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

        return Pair.create(textRange, entry);
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
     * 检查是否适用于项目
     *
     * @param project 项目
     * @return 总是返回 true
     */
    @Override
    public boolean isAvailableFor(@NotNull Project project) {
        return true;
    }

    @Override
    public Unit precomputeOnUiThread(@NotNull Editor editor) {
        // 不需要预计算
        return null;
    }

    /**
     * 泛型不可变键值对容器
     * <p>
     * 该类用于存储一对相关联的对象, 提供对键 (first) 和值 (second) 的只读访问. 通过 {@code create} 静态工厂方法可以方便地创建实例, 避免显式调用构造函数.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email "mailto:zeka.stack@gmail.com"
     * @date 2025.12.11
     * @since 1.0.0
     */
    @Getter
    public static class Pair<K, V> {
        private final K first;
        private final V second;

        private Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        public static <K, V> Pair<K, V> create(K first, V second) {
            return new Pair<>(first, second);
        }

    }

}

