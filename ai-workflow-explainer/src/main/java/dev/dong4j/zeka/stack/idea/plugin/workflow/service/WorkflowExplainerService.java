package dev.dong4j.zeka.stack.idea.plugin.workflow.service;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIChatRequest;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIServiceException;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIService;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.service.AIServiceImpl;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderConfig;
import dev.dong4j.zeka.stack.idea.plugin.common.config.AIProviderSettings;
import dev.dong4j.zeka.stack.idea.plugin.workflow.model.WorkflowContext;
import dev.dong4j.zeka.stack.idea.plugin.workflow.ui.WorkflowResultToolWindow;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.JSONSerializer;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.MethodContextExtractor;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.PSIUtil;
import dev.dong4j.zeka.stack.idea.plugin.workflow.util.WorkflowBundle;

/**
 * 工作流解释服务
 *
 * @author dong4j
 * @version 1.0.0
 */
public class WorkflowExplainerService {
    private final Project project;
    private final CallGraphBuilder callGraphBuilder;

    public WorkflowExplainerService(@NotNull Project project) {
        this.project = project;
        this.callGraphBuilder = new CallGraphBuilder(project);
    }

    /**
     * 分析工作流并生成说明
     * <p>
     * 注意：PSI 操作必须在 ReadAction 中执行
     *
     * @param psiFile PSI 文件
     * @param caretOffset 光标位置偏移量
     * @return AI 生成的说明文本
     * @throws Exception 当分析或 AI 调用失败时抛出
     */
    /**
     * 工作流分析结果
     */
    public static class WorkflowResult {
        /** AI 生成的 Markdown 结果 */
        @NotNull
        public String markdown;
        /** 目标方法签名（用于生成文件名） */
        @NotNull
        public String methodSignature;

        public WorkflowResult(@NotNull String markdown, @NotNull String methodSignature) {
            this.markdown = markdown;
            this.methodSignature = methodSignature;
        }
    }

    /**
     * 分析工作流并生成说明（两阶段写入）
     * <p>
     * 阶段1：在调用 AI 之前，创建 scratch 文件并写入元数据
     * 阶段2：在调用 AI 之后，追加 AI 结果到文件
     *
     * @param psiFile     PSI 文件
     * @param caretOffset 光标位置偏移量
     * @throws Exception 当分析或 AI 调用失败时抛出
     */
    public void explainWorkflow(@NotNull PsiFile psiFile, int caretOffset) throws Exception {
        // PSI 操作必须在 ReadAction 中执行
        WorkflowAnalysisData analysisData = com.intellij.openapi.application.ReadAction.compute(() -> {
            try {
                // 1. 获取光标位置的方法调用
                PsiMethodCallExpression methodCall = PSIUtil.getMethodCallAtOffset(psiFile, caretOffset);
                if (methodCall == null) {
                    throw new RuntimeException(WorkflowBundle.message("error.no.method.call"));
                }

                // 2. 解析被调用的方法
                PsiMethod targetMethod = methodCall.resolveMethod();
                if (targetMethod == null) {
                    throw new RuntimeException(WorkflowBundle.message("error.cannot.resolve.method"));
                }

                // 3. 获取当前上下文
                PsiMethod currentMethod = PSIUtil.getMethodAtOffset(psiFile, caretOffset);
                PsiClass currentClass = PSIUtil.getClassAtOffset(psiFile, caretOffset);

                // 4. 构建工作流上下文
                WorkflowContext context = buildWorkflowContext(targetMethod, currentMethod, currentClass);

                // 5. 生成 JSON
                String json = JSONSerializer.toJson(context);

                // 6. 获取方法签名（用于生成文件名）
                String methodSignature = PSIUtil.getMethodSignature(targetMethod);

                // 7. 获取代码位置信息（用于生成代码链接）
                CodeLocation codeLocation = getCodeLocation(psiFile, methodCall);

                return new WorkflowAnalysisData(json, methodSignature, codeLocation, psiFile);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // 阶段1：创建 scratch 文件并写入元数据
        createScratchFileAndWriteMetadata(analysisData);

        // 阶段2：调用 AI 生成说明（在 ReadAction 外部执行）
        String aiMarkdown = callAI(analysisData.json);

        // 阶段3：追加 AI 结果到文件
        appendAIResult(aiMarkdown);
    }

    /**
         * 代码位置信息
         */
        private record CodeLocation(String filePath, int line, int column) {
    }

    /**
         * 工作流分析数据（内部类）
         */
        private record WorkflowAnalysisData(String json, String methodSignature, CodeLocation codeLocation, PsiFile psiFile) {
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
        String fileName = generateFileName(analysisData.methodSignature);

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
        sb.append("# 工作流分析结果\n\n");

        // 代码链接
        CodeLocation location = analysisData.codeLocation;
        if (location.line > 0) {
            // 使用 java:// 协议来引用代码文件（file:// 无法被拦截）
            // 格式：java://文件路径:行号:列号
            String codeLink = String.format("java://%s:%d:%d", location.filePath, location.line, location.column);
            String methodName = analysisData.methodSignature;
            sb.append("## 分析的代码\n\n");
            sb.append(String.format("[%s](%s)\n\n", methodName, codeLink));
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
     * 生成文件名（基于方法签名）
     *
     * @param methodSignature 方法签名
     * @return 文件名
     */
    @NotNull
    private String generateFileName(@NotNull String methodSignature) {
        // 清理方法签名，生成安全的文件名
        String fileName = methodSignature
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
     * 构建工作流上下文
     *
     * @param targetMethod  目标方法
     * @param currentMethod 当前方法
     * @param currentClass  当前类
     * @return 工作流上下文
     */
    @NotNull
    private WorkflowContext buildWorkflowContext(@NotNull PsiMethod targetMethod,
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
     * 调用 AI 生成说明
     * <p>
     * 注意：此方法在 ReadAction 外部执行，可以执行网络请求
     *
     * @param json 上下文 JSON
     * @return AI 生成的说明
     * @throws Exception 当 AI 调用失败时抛出
     */
    @NotNull
    private String callAI(@NotNull String json) throws Exception {
        // 获取 AI 配置
        AIProviderSettings settings = AIProviderSettings.getInstance();
        java.util.List<AIProviderConfig> verifiedProviders = settings.getVerifiedProviders();

        if (verifiedProviders.isEmpty()) {
            throw new Exception(WorkflowBundle.message("error.ai.provider.not.configured"));
        }

        // 使用第一个已验证的提供商
        AIProviderConfig config = verifiedProviders.get(0);

        // 构建 Prompt
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(json);

        // 创建 AI 请求
        AIChatRequest request = new AIChatRequest(systemPrompt, userPrompt);

        // 调用 AI 服务
        AIService aiService = AIServiceImpl.getInstance();
        try {
            return aiService.generateContent(project, request, config, null);
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
        return "你是一名资深的系统架构师和技术分析师。\n" +
               "请根据提供的方法上下文信息，深入分析该方法的业务流程，并生成一份详细的技术说明文档。\n" +
               "\n" +
               "要求：\n" +
               "1. **调用链分析**：\n" +
               "   - 首先分析调用者（callers）：谁调用了这个方法，调用者的职责是什么\n" +
               "   - 然后分析被调用者（callees）：这个方法调用了哪些其他方法，每个被调用方法的职责是什么\n" +
               "   - 理解完整的调用链路，从入口到出口\n" +
               "\n" +
               "2. **绘制时序图**：\n" +
               "   - 使用 Mermaid 语法绘制完整的调用时序图（sequenceDiagram）\n" +
               "   - 展示从调用者到目标方法，再到被调用者的完整调用流程\n" +
               "   - 标注关键的方法调用和参数传递\n" +
               "\n" +
               "3. **详细技术说明**：\n" +
               "   - 按照调用链的顺序，逐个解释每个环节的作用：\n" +
               "     a) 首先说明调用者如何触发这个方法（调用场景和触发条件）\n" +
               "     b) 然后详细解释目标方法的核心逻辑和职责\n" +
               "     c) 接着逐个说明目标方法调用的每个子方法的作用和意义\n" +
               "     d) 最后说明整个调用链的返回流程和数据流转\n" +
               "   - 每个环节都要说明：方法的作用、参数的含义、返回值的作用、在整个流程中的位置\n" +
               "   - 说明要具体、详细，不要泛泛而谈\n" +
               "\n" +
               "4. **总结说明**：\n" +
               "   - 在详细解释之后，提供一段总结性的说明\n" +
               "   - 总结该方法在整个系统中的定位和作用\n" +
               "   - 说明该方法解决的核心问题和业务价值\n" +
               "\n" +
               "5. **注意事项**：\n" +
               "   - 只基于提供的上下文信息进行分析，不要编造不存在的细节\n" +
               "   - 如果某些信息缺失，明确说明哪些信息无法确定\n" +
               "   - 使用专业但易懂的技术语言";
    }

    /**
     * 构建用户提示词
     *
     * @param json 上下文 JSON
     * @return 用户提示词
     */
    @NotNull
    private String buildUserPrompt(@NotNull String json) {
        return "以下是方法上下文（JSON）：\n\n" + json + "\n\n" +
               "请按照以下格式输出：\n" +
               "\n" +
               "## 调用时序图\n" +
               "\n" +
               "```mermaid\n" +
               "sequenceDiagram\n" +
               "    ...\n" +
               "```\n" +
               "\n" +
               "## 技术说明\n" +
               "\n" +
               "### 调用链分析\n" +
               "\n" +
               "#### 1. 调用者分析\n" +
               "（详细说明谁调用了这个方法，调用场景和触发条件）\n" +
               "\n" +
               "#### 2. 目标方法核心逻辑\n" +
               "（详细说明目标方法的作用、参数含义、核心处理逻辑）\n" +
               "\n" +
               "#### 3. 被调用方法分析\n" +
               "（逐个详细说明目标方法调用的每个子方法的作用、参数、返回值及其在整个流程中的意义）\n" +
               "\n" +
               "#### 4. 返回流程分析\n" +
               "（说明数据如何从被调用方法返回到调用者，整个调用链的返回流程）\n" +
               "\n" +
               "### 总结\n" +
               "\n" +
               "（总结该方法在整个系统中的定位、作用、解决的核心问题和业务价值）";
    }
}

