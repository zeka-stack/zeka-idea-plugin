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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import dev.dong4j.zeka.stack.idea.plugin.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.util.AiCodePreprocessor;

/**
 * 任务收集器
 *
 * <p>从文件、目录中收集需要生成文档的代码元素。
 * 作为文档生成流程的第一步，负责识别和收集所有需要处理的代码元素，
 * 并根据用户配置和代码状态创建相应的文档生成任务。
 *
 * <p>核心功能：
 * <ul>
 *   <li>从不同来源收集任务（元素、文件、目录）</li>
 *   <li>根据配置过滤需要处理的元素</li>
 *   <li>创建 DocumentationTask 对象</li>
 *   <li>支持递归处理内部类和目录结构</li>
 * </ul>
 *
 * <p>收集策略：
 * <ul>
 *   <li>单个元素：只处理指定的元素</li>
 *   <li>类：处理类及其所有成员</li>
 *   <li>文件：处理文件中的所有类、方法、字段</li>
 *   <li>目录：递归处理目录中的所有 Java 文件</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
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
    @NotNull
    public List<DocumentationTask> collectFromElement(@NotNull PsiElement element) {
        List<DocumentationTask> tasks = new ArrayList<>();

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
            collectFromClass(psiClass, tasks);
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
     * @param psiFile PSI 文件对象
     * @return 文档生成任务列表
     * @see JavaRecursiveElementVisitor
     */
    @NotNull
    public List<DocumentationTask> collectFromFile(@NotNull PsiFile psiFile) {
        List<DocumentationTask> tasks = new ArrayList<>();

        if (!(psiFile instanceof PsiJavaFile)) {
            return tasks;
        }

        psiFile.accept(new JavaRecursiveElementVisitor() {
            /**
             * 访问类元素并根据配置决定是否生成文档任务
             * <p>
             * 当访问到类元素时，若配置启用类文档生成且该类满足生成条件，则创建一个类文档生成任务并添加到任务列表中。
             *
             * @param aClass 被访问的类元素
             */
            @Override
            public void visitClass(@NotNull PsiClass aClass) {
                super.visitClass(aClass);

                if (settings.generateForClass && shouldGenerateForElement(aClass)) {
                    tasks.add(createTask(aClass, DocumentationTask.TaskType.CLASS));
                }
            }

            /**
             * 处理方法节点，根据配置决定是否生成文档任务
             * <p>
             * 遍历方法节点，若配置允许为方法生成文档且满足条件，则创建文档任务并添加到任务列表中
             *
             * @param method 被访问的方法节点
             */
            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                super.visitMethod(method);

                if (settings.generateForMethod && shouldGenerateForElement(method)) {
                    DocumentationTask.TaskType type = isTestMethod(method)
                                                      ? DocumentationTask.TaskType.TEST_METHOD
                                                      : DocumentationTask.TaskType.METHOD;
                    tasks.add(createTask(method, type));
                }
            }

            /**
             * 处理字段元素，根据配置决定是否生成文档任务
             * <p>
             * 当启用字段文档生成且字段满足条件时，创建并添加文档生成任务
             *
             * @param field 被访问的字段元素
             */
            @Override
            public void visitField(@NotNull PsiField field) {
                super.visitField(field);

                if (settings.generateForField && shouldGenerateForElement(field)) {
                    tasks.add(createTask(field, DocumentationTask.TaskType.FIELD));
                }
            }
        });

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
     * 获取元素的代码时，会包含已有的 JavaDoc 注释（如果存在）。
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
        // 获取代码，包含已有的 JavaDoc 注释
        String code = getCodeWithComment(element);

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

        return new DocumentationTask(element, code, type, filePath);
    }

    /**
     * 获取元素的代码，包含已有的 JavaDoc 注释
     *
     * <p>通过 element.getText() 方法获取元素的完整文本表示，
     * 包括已有的 JavaDoc 注释。这样可以为 AI 提供上下文信息，
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
            if (element instanceof PsiClass) {
                reformatCode = AiCodePreprocessor.preprocess(optimizeClassCode(reformat.getText()));
            }
            // 方法或字段级别的代码使用 AiCodePreprocessor 进行压缩
            if (element instanceof PsiMethod || element instanceof PsiField) {
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
     *   <li>保留 JavaDoc 注释（/** 注释）</li>
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
        final int MAX_LINES = settings.maxClassCodeLines;

        for (String line : lines) {
            // 如果已经达到最大行数，停止处理
            if (lineCount >= MAX_LINES) {
                optimized.append("\n// ... (代码已截取，超过 ").append(MAX_LINES).append(" 行)");
                break;
            }

            String trimmedLine = line.trim();

            // 跳过空行
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // 跳过单行注释（// 注释），但保留 JavaDoc 注释（/** 注释）
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
     *       <li>检查元素是否已有 JavaDoc 注释</li>
     *       <li>如果已有注释返回 false（跳过），否则返回 true（生成）</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param element PSI 元素
     * @return 如果应该生成文档返回 true，否则返回 false
     * @see SettingsState#overrideExisting
     */
    private boolean shouldGenerateForElement(@NotNull PsiElement element) {
        // 如果配置为覆盖已有注释，总是生成
        if (settings.overrideExisting) {
            return true;
        }

        // 如果配置为跳过已有文档（默认），检查是否已有文档
        if (element instanceof PsiDocCommentOwner) {
            PsiDocComment docComment = ((PsiDocCommentOwner) element).getDocComment();
            return docComment == null;
        }

        return true;
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
     * 判断是否为 Java 文件
     *
     * <p>通过文件扩展名判断是否为 Java 文件。
     * 不区分大小写，支持 .java 扩展名。
     *
     * @param file 虚拟文件对象
     * @return 如果是 Java 文件返回 true，否则返回 false
     */
    private boolean isJavaFile(@NotNull VirtualFile file) {
        return "java".equalsIgnoreCase(file.getExtension());
    }
}

