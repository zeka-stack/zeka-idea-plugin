package dev.dong4j.zeka.stack.idea.plugin.workflow.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.common.exception.NoProviderdException;
import dev.dong4j.zeka.stack.idea.plugin.workflow.ai.TracerAIResponseListener;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.ClassRelationshipContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.MethodCallerChainContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.WorkflowContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.WorkflowType;
import dev.dong4j.zeka.stack.idea.plugin.workflow.settings.SettingsState;
import dev.dong4j.zeka.stack.idea.plugin.workflow.ui.WorkflowResultToolWindow;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.JSONSerializer;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.MethodContextExtractor;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.NotificationUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.PSIUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;

/**
 * 工作流解释服务
 *
 * @author dong4j
 * @version 1.0.0
 */
public class WorkflowExplainerService {
    /**
     * 当前操作的项目对象
     * <p>
     * 该字段用于存储与当前操作相关的项目信息, 不可变
     */
    private final Project project;
    /**
     * 用于构建调用图的工具
     * <p>
     * 该实例负责分析和生成代码中的调用关系图谱
     *
     * @see CallGraphBuilder
     */
    private final CallGraphBuilder callGraphBuilder;

    /**
     * 初始化 WorkflowExplainerService 实例
     * <p>
     * 使用指定的项目信息创建服务实例, 并初始化调用图构建器.
     *
     * @param project 项目对象, 用于提供上下文信息
     * @throws NullPointerException 如果传入的 project 参数为 null, 将抛出异常
     * @since 1.0
     */
    public WorkflowExplainerService(@NotNull Project project) {
        this.project = project;
        this.callGraphBuilder = new CallGraphBuilder(project);
    }

    /**
     * 工作流结果类
     * <p>
     * 用于封装工作流执行后的结果信息, 包含以 Markdown 格式展示的描述内容以及方法签名, 便于在日志或展示中使用.
     *
     * @author zeka.stack.team
     * @version 1.0.0
     * @email mailto:zeka.stack@gmail.com
     * @date 2025.12.02
     * @since 1.0.0
     */
    public static class WorkflowResult {
        /**
         * 用于存储或处理的 Markdown 格式内容
         * <p>
         * 该字段表示以 Markdown 语法编写的文本数据, 通常用于富文本展示或编辑.
         *
         * @see org.apache.commons.lang3.Validate#notNull(Object)
         */
        @NotNull
        public String markdown;
        /**
         * 方法签名, 表示该方法的名称和参数类型
         *
         * @NotNull 注解表明该字段不允许为 null
         */
        @NotNull
        public String methodSignature;

        /**
         * 构造一个新的 WorkflowResult 对象
         * <p>
         * 该方法用于初始化 WorkflowResult 实例的 markdown 和 methodSignature 字段.
         *
         * @param markdown        用于描述工作流的 Markdown 格式文本
         * @param methodSignature 方法的签名信息
         * @since 1.0
         */
        public WorkflowResult(@NotNull String markdown, @NotNull String methodSignature) {
            this.markdown = markdown;
            this.methodSignature = methodSignature;
        }
    }

    /**
     * 解释当前光标位置的工作流
     * <p>
     * 该方法用于分析指定文件中光标所在位置的代码元素类型, 并根据类型执行相应的工作流分析逻辑.
     * 如果没有可用的 AI 提供者配置, 将显示错误通知并返回.
     *
     * @param psiFile     当前打开的 Psi 文件, 表示正在分析的源代码文件
     * @param caretOffset 光标在文件中的偏移位置, 用于定位当前分析的代码元素
     * @throws Exception 在调用 AI 分析或处理过程中发生异常时抛出
     */
    public void explainWorkflow(@NotNull PsiFile psiFile, int caretOffset) throws Exception {
        // PSI 操作必须在 ReadAction 中执行
        WorkflowAnalysisData analysisData = com.intellij.openapi.application.ReadAction.compute(() -> {
            try {
                // 1. 检测元素类型
                PSIUtil.ElementContext elementContext = PSIUtil.detectElementType(psiFile, caretOffset);

                if (elementContext.type() == PSIUtil.ElementType.UNKNOWN) {
                    throw new RuntimeException(WorkflowBundle.message("error.unsupported.element"));
                }

                // 2. 根据类型生成不同的工作流
                switch (elementContext.type()) {
                    case METHOD_CALL:
                        return analyzeMethodCallWorkflow(psiFile, caretOffset, (PsiMethodCallExpression) elementContext.element());
                    case METHOD_DEFINITION:
                        return analyzeMethodCallerChain(psiFile, caretOffset, (PsiMethod) elementContext.element());
                    case CLASS_DEFINITION:
                        return analyzeClassRelationship(psiFile, caretOffset, (PsiClass) elementContext.element());
                    default:
                        throw new RuntimeException(WorkflowBundle.message("error.unsupported.element"));
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            // 阶段1：创建 scratch 文件并写入元数据
            createScratchFileAndWriteMetadata(analysisData);

            // 阶段2：调用 AI 生成说明（在 ReadAction 外部执行）
            String aiMarkdown = callAI(analysisData.json, analysisData.workflowType);

            // 阶段3：追加 AI 结果到文件
            appendAIResult(aiMarkdown);
        } catch (NoProviderdException e) {
            Notification notification = new Notification(NotificationUtil.NOTIFICATION_GROUP_ID,
                                                         WorkflowBundle.message("notification.error.title"),
                                                         WorkflowBundle.message("settings.ai.provider.no.available.warning"),
                                                         NotificationType.ERROR);
            // 添加设置动作
            NotificationUtil.addOpenConfigurablePanelAction(notification, project);
        } catch (Exception e) {
            throw new RuntimeException("Failed to explain workflow", e);
        }
    }

    /**
     * 代码位置信息
     */
    private record CodeLocation(String filePath, int line, int column) {
    }

    /**
     * 工作流分析数据（内部类）
     */
    private record WorkflowAnalysisData(String json, String signature, CodeLocation codeLocation, PsiFile psiFile,
                                        WorkflowType workflowType) {
    }

    /**
     * 获取代码位置信息（行号和列号）
     *
     * @param psiFile PSI 文件
     * @param element PSI 元素
     * @return 代码位置信息
     */
    @NotNull
    private CodeLocation getCodeLocation(@NotNull PsiFile psiFile, @NotNull PsiElement element) {
        VirtualFile virtualFile = psiFile.getVirtualFile();
        String filePath = virtualFile != null ? virtualFile.getPath() : "";

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document != null) {
            int offset = element.getTextOffset();
            int line = document.getLineNumber(offset);
            int column = offset - document.getLineStartOffset(line);
            return new CodeLocation(filePath, line + 1, column + 1); // 行号和列号从1开始
        }

        return new CodeLocation(filePath, 0, 0);
    }

    /**
     * 创建 scratch 文件并写入元数据（阶段1）
     *
     * @param analysisData 分析数据
     * @return Scratch 文件的 VirtualFile
     */
    @NotNull
    private VirtualFile createScratchFileAndWriteMetadata(@NotNull WorkflowAnalysisData analysisData) {
        // 生成文件名
        String fileName = generateFileName(analysisData.signature);

        // 构建元数据 Markdown
        String metadata = buildMetadataMarkdown(analysisData, fileName);

        // 创建 scratch 文件并写入元数据
        WorkflowResultToolWindow toolWindow = WorkflowResultToolWindow.getInstance(project);
        return toolWindow.createScratchFileWithMetadata(metadata, fileName);
    }

    /**
     * 构建元数据 Markdown
     *
     * @param analysisData 分析数据
     * @param fileName     文件名（用于生成文件链接）
     * @return 元数据 Markdown 内容
     */
    @NotNull
    private String buildMetadataMarkdown(@NotNull WorkflowAnalysisData analysisData, @NotNull String fileName) {
        StringBuilder sb = new StringBuilder();

        // 标题
        sb.append("# ").append(analysisData.workflowType.getDisplayName()).append("分析结果\n\n");

        // 代码链接
        CodeLocation location = analysisData.codeLocation;
        if (location.line > 0) {
            // 使用 java:// 协议来引用代码文件（file:// 无法被拦截）
            // 格式：java://文件路径:行号:列号
            String codeLink = String.format("java://%s:%d:%d", location.filePath, location.line, location.column);
            String signature = analysisData.signature;
            sb.append("## 分析的代码\n\n");
            sb.append(String.format("[%s](%s)\n\n", signature, codeLink));
        }

        // Scratch 文件链接
        // 使用 scratch:// 协议来引用 scratch 文件
        String scratchFileLink = String.format("scratch://%s", fileName);
        sb.append("## 临时文件\n\n");
        sb.append(String.format("[%s](%s)\n\n", fileName, scratchFileLink));

        // JSON 数据
        sb.append("## 分析上下文（JSON）\n\n");
        sb.append("```json\n");
        sb.append(analysisData.json);
        sb.append("\n```\n\n");

        return sb.toString();
    }

    /**
     * 生成文件名（基于签名）
     *
     * @param signature 签名（方法签名或类名）
     * @return 文件名
     */
    @NotNull
    private String generateFileName(@NotNull String signature) {
        // 清理签名，生成安全的文件名
        String fileName = signature
            .replaceAll("[<>]", "") // 移除泛型符号
            .replaceAll("\\s+", "_") // 空格替换为下划线
            .replaceAll("[^a-zA-Z0-9._-]", "_"); // 其他特殊字符替换为下划线

        // 如果文件名太长，截取
        if (fileName.length() > 200) {
            fileName = fileName.substring(0, 200);
        }

        // 确保以 .md 结尾
        if (!fileName.endsWith(".md")) {
            fileName += ".md";
        }

        return fileName;
    }

    /**
     * 追加 AI 结果到文件（阶段2）
     *
     * @param aiMarkdown AI 生成的 Markdown
     */
    private void appendAIResult(@NotNull String aiMarkdown) {
        WorkflowResultToolWindow toolWindow = WorkflowResultToolWindow.getInstance(project);
        toolWindow.appendAIResult(aiMarkdown);
    }

    /**
     * 分析方法调用工作流（当前功能）
     *
     * @param psiFile     PSI 文件
     * @param caretOffset 光标偏移量
     * @param methodCall  方法调用表达式
     * @return 工作流分析数据
     */
    @NotNull
    private WorkflowAnalysisData analyzeMethodCallWorkflow(@NotNull PsiFile psiFile,
                                                           int caretOffset,
                                                           @NotNull PsiMethodCallExpression methodCall) {
        // 当前的实现逻辑
        PsiMethod targetMethod = methodCall.resolveMethod();
        if (targetMethod == null) {
            throw new RuntimeException(WorkflowBundle.message("error.cannot.resolve.method"));
        }

        PsiMethod currentMethod = PSIUtil.getMethodAtOffset(psiFile, caretOffset);
        PsiClass currentClass = PSIUtil.getClassAtOffset(psiFile, caretOffset);

        WorkflowContext context = buildMethodCallWorkflowContext(targetMethod, currentMethod, currentClass);
        String json = JSONSerializer.toJson(context);
        String methodSignature = PSIUtil.getMethodSignature(targetMethod);
        CodeLocation codeLocation = getCodeLocation(psiFile, methodCall);

        return new WorkflowAnalysisData(json, methodSignature, codeLocation, psiFile, WorkflowType.METHOD_CALL_FLOW);
    }

    /**
     * 分析方法调用链（谁调用了这个方法）
     *
     * @param psiFile     PSI 文件
     * @param caretOffset 光标偏移量
     * @param method      方法定义
     * @return 工作流分析数据
     */
    @NotNull
    private WorkflowAnalysisData analyzeMethodCallerChain(@NotNull PsiFile psiFile,
                                                          int caretOffset,
                                                          @NotNull PsiMethod method) {
        // 构建方法调用链上下文
        MethodCallerChainContext context = callGraphBuilder.findMethodCallerChain(method);

        // 设置项目信息
        context.project.name = project.getName();

        String json = JSONSerializer.toJson(context);
        String methodSignature = PSIUtil.getMethodSignature(method);
        CodeLocation codeLocation = getCodeLocation(psiFile, method);

        return new WorkflowAnalysisData(json, methodSignature, codeLocation, psiFile, WorkflowType.METHOD_CALLER_CHAIN);
    }

    /**
     * 分析类关系链
     *
     * @param psiFile     PSI 文件
     * @param caretOffset 光标偏移量
     * @param psiClass    类定义
     * @return 工作流分析数据
     */
    @NotNull
    private WorkflowAnalysisData analyzeClassRelationship(@NotNull PsiFile psiFile,
                                                          int caretOffset,
                                                          @NotNull PsiClass psiClass) {
        // 构建类关系上下文
        ClassRelationshipContext context = buildClassRelationshipContext(psiClass);
        String json = JSONSerializer.toJson(context);
        String className = psiClass.getName() != null ? psiClass.getName() : "UnknownClass";
        CodeLocation codeLocation = getCodeLocation(psiFile, psiClass);

        return new WorkflowAnalysisData(json, className, codeLocation, psiFile, WorkflowType.CLASS_RELATIONSHIP);
    }

    /**
     * 构建方法调用工作流上下文
     *
     * @param targetMethod  目标方法
     * @param currentMethod 当前方法
     * @param currentClass  当前类
     * @return 工作流上下文
     */
    @NotNull
    private WorkflowContext buildMethodCallWorkflowContext(@NotNull PsiMethod targetMethod,
                                                           @Nullable PsiMethod currentMethod,
                                                           @Nullable PsiClass currentClass) {
        WorkflowContext context = new WorkflowContext();

        // 设置项目信息
        context.project.name = project.getName();

        // 设置当前类信息
        if (currentClass != null) {
            context.currentClass = MethodContextExtractor.extractClassInfo(currentClass);
        }

        // 设置当前方法信息（使用目标方法）
        context.currentMethod = MethodContextExtractor.extractMethodInfo(targetMethod);

        // 如果当前方法存在且不是目标方法，则添加到调用者列表
        if (currentMethod != null && !currentMethod.equals(targetMethod)) {
            context.callers.add(MethodContextExtractor.extractMethodInfo(currentMethod));
        }

        // 查找调用者
        context.callers.addAll(callGraphBuilder.findCallers(targetMethod));

        // 查找被调用者
        context.callees.addAll(callGraphBuilder.findCallees(targetMethod));

        return context;
    }

    /**
     * 构建类关系上下文
     *
     * @param psiClass 目标类
     * @return 类关系上下文
     */
    @NotNull
    private ClassRelationshipContext buildClassRelationshipContext(@NotNull PsiClass psiClass) {
        ClassRelationshipContext context = new ClassRelationshipContext();

        // 设置项目信息
        context.project.name = project.getName();

        // 设置目标类信息
        context.targetClass = MethodContextExtractor.extractClassInfo(psiClass);

        // 查找继承关系
        context.inheritance = callGraphBuilder.findClassInheritance(psiClass);

        // 查找依赖关系
        context.dependencies = callGraphBuilder.findClassDependencies(psiClass);

        // 查找内部类
        PsiClass[] innerClasses = psiClass.getInnerClasses();
        for (PsiClass innerClass : innerClasses) {
            context.innerClasses.add(MethodContextExtractor.extractClassInfo(innerClass));
        }

        return context;
    }

    /**
     * 调用 AI 生成说明
     * <p>
     * 注意：此方法在 ReadAction 外部执行，可以执行网络请求
     *
     * @param json         上下文 JSON
     * @param workflowType 工作流类型
     * @return AI 生成的说明
     * @throws Exception 当 AI 调用失败时抛出
     */
    @NotNull
    private String callAI(@NotNull String json, @NotNull WorkflowType workflowType) throws Exception {
        // 获取 AI 配置
        AIProviderConfig config = SettingsState.getInstance().providerConfig;
        if (config == null) {
            throw new NoProviderdException();
        }

        // 构建 Prompt
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(json, workflowType);

        // 创建 AI 请求
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

        // 检查是否启用详细日志
        boolean verboseLogging = AIProviderSettings.getInstance().verboseLogging;
        AIResponseListener listener = verboseLogging ? new TracerAIResponseListener(project) : null;

        // 调用 AI 服务
        AIService aiService = AIServiceImpl.getInstance();
        try {
            return aiService.generateContent(project, request, config, listener);
        } catch (AIServiceException e) {
            throw new Exception(WorkflowBundle.message("error.ai.service.failed", e.getMessage()));
        }
    }

    /**
     * 构建系统提示词
     *
     * @return 系统提示词
     */
    @NotNull
    private String buildSystemPrompt() {
        SettingsState settings = SettingsState.getInstance();
        String prompt = settings.systemPrompt;
        if (StringUtil.isEmptyOrSpaces(prompt)) {
            prompt = SettingsState.getDefaultSystemPrompt();
        }
        return prompt;
    }

    /**
     * 构建用户提示词（支持不同工作流类型）
     *
     * @param json         上下文 JSON
     * @param workflowType 工作流类型
     * @return 用户提示词
     */
    @NotNull
    private String buildUserPrompt(@NotNull String json, @NotNull WorkflowType workflowType) {
        SettingsState settings = SettingsState.getInstance();
        String template;

        switch (workflowType) {
            case METHOD_CALL_FLOW:
                template = settings.methodCallTemplate;
                if (StringUtil.isEmptyOrSpaces(template)) {
                    template = SettingsState.getDefaultMethodCallTemplate();
                }
                break;
            case METHOD_CALLER_CHAIN:
                template = settings.methodCallerChainTemplate;
                if (StringUtil.isEmptyOrSpaces(template)) {
                    template = SettingsState.getDefaultMethodCallerChainTemplate();
                }
                break;
            case CLASS_RELATIONSHIP:
                template = settings.classRelationshipTemplate;
                if (StringUtil.isEmptyOrSpaces(template)) {
                    template = SettingsState.getDefaultClassRelationshipTemplate();
                }
                break;
            default:
                template = settings.workflowTemplate;
                if (StringUtil.isEmptyOrSpaces(template)) {
                    template = SettingsState.getDefaultWorkflowTemplate();
                }
        }

        if (template.contains(SettingsState.CONTEXT_PLACEHOLDER)) {
            return template.replace(SettingsState.CONTEXT_PLACEHOLDER, json);
        }
        return template + "\n\n" + json;
    }

}

