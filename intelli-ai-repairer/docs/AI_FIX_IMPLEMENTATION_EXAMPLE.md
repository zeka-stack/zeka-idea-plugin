# Checkstyle AI Fix 实现示例

## 1. 扩展点接口定义（需要添加到 checkstyle-idea）

### CheckstyleQuickFixProvider.java

```java
package org.infernus.idea.checkstyle.checker;

import com.intellij.codeInspection.LocalQuickFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 扩展接口：允许其他插件为 Checkstyle 违规问题提供自定义的 QuickFix
 * 
 * @since 26.0.0
 */
public interface CheckstyleQuickFixProvider {
    
    /**
     * 为指定的 Problem 提供额外的 QuickFix
     * 
     * @param problem Checkstyle 违规问题
     * @return QuickFix 数组，可以为空或 null。如果返回 null，表示不提供修复。
     */
    @Nullable
    LocalQuickFix[] getQuickFixes(@NotNull Problem problem);
}
```

### Problem.java 修改

```java
// 在 Problem.java 的 quickFixes 方法中添加扩展支持
private LocalQuickFix[] quickFixes(final String sourceCheck) {
    List<LocalQuickFix> fixes = new ArrayList<>();
    
    // 原有的 suppress fix
    if (sourceCheck != null) {
        fixes.add(new SuppressForCheckstyleFix(sourceCheck));
    }
    
    // 通过扩展点获取额外的 Fix
    try {
        Project project = target.getProject();
        if (project != null && !project.isDisposed()) {
            ExtensionPointName<CheckstyleQuickFixProvider> extensionPointName =
                ExtensionPointName.create("org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider");
            
            for (CheckstyleQuickFixProvider provider : extensionPointName.getExtensions(project)) {
                LocalQuickFix[] providedFixes = provider.getQuickFixes(this);
                if (providedFixes != null && providedFixes.length > 0) {
                    fixes.addAll(Arrays.asList(providedFixes));
                }
            }
        }
    } catch (Exception e) {
        // 扩展点不可用，忽略错误（向后兼容）
        LOG.debug("Failed to load CheckstyleQuickFixProvider extensions", e);
    }
    
    return fixes.isEmpty() ? null : fixes.toArray(new LocalQuickFix[0]);
}
```

### plugin.xml 扩展点注册

```xml
<!-- checkstyle-idea 的 plugin.xml -->
<extensionPoint name="checkstyleQuickFixProvider" 
                qualifiedName="org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider"
                area="IDEA_PROJECT"/>
```

---

## 2. AI 插件实现

### AICheckstyleQuickFixProvider.java

```java
package dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.diagnostic.Logger;
import org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider;
import org.infernus.idea.checkstyle.checker.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 为 Checkstyle 违规问题提供 AI 自动修复功能
 */
public class AICheckstyleQuickFixProvider implements CheckstyleQuickFixProvider {
    
    private static final Logger LOG = Logger.getInstance(AICheckstyleQuickFixProvider.class);
    
    @Override
    @Nullable
    public LocalQuickFix[] getQuickFixes(@NotNull Problem problem) {
        // 检查 IntelliAI Engine 是否可用
        if (!isIntelliAIEngineAvailable()) {
            return null;
        }
        
        // 检查是否支持自动修复（某些问题可能不适合 AI 修复）
        if (!isFixable(problem)) {
            return null;
        }
        
        try {
            return new LocalQuickFix[]{
                new AICheckstyleFix(problem)
            };
        } catch (Exception e) {
            LOG.warn("Failed to create AI Checkstyle fix", e);
            return null;
        }
    }
    
    /**
     * 检查 IntelliAI Engine 是否可用
     */
    private boolean isIntelliAIEngineAvailable() {
        try {
            // 检查插件是否已安装
            PluginManagerCore.getPlugin(PluginId.getId("dev.dong4j.zeka.stack.idea.plugin.common.ai"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 判断问题是否适合 AI 修复
     */
    private boolean isFixable(@NotNull Problem problem) {
        // 某些类型的问题可能不适合 AI 修复
        // 例如：需要人工判断的问题、过于复杂的问题等
        
        String sourceName = problem.sourceName();
        if (sourceName == null) {
            return false;
        }
        
        // 可以根据 sourceName 过滤
        // 例如：只修复格式类问题，不修复逻辑问题
        return true;
    }
}
```

### AICheckstyleFix.java

```java
package dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.context.FixContext;
import dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.context.FixContextBuilder;
import dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.service.AICodeFixService;
import org.infernus.idea.checkstyle.checker.Problem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 使用 AI 自动修复 Checkstyle 违规问题
 */
public class AICheckstyleFix implements LocalQuickFix {
    
    private static final Logger LOG = Logger.getInstance(AICheckstyleFix.class);
    private final Problem problem;
    
    public AICheckstyleFix(@NotNull Problem problem) {
        this.problem = problem;
    }
    
    @NotNull
    @Override
    public String getFamilyName() {
        return "Fix with AI";
    }
    
    @NotNull
    @Override
    public String getName() {
        return "Fix with AI";
    }
    
    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        // 在后台任务中执行 AI 修复
        new Task.Backgroundable(project, "Fixing with AI...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                
                try {
                    // 1. 收集上下文信息
                    indicator.setText("Collecting context...");
                    FixContext context = collectContext(project, descriptor);
                    
                    if (context == null) {
                        showError(project, "Failed to collect context");
                        return;
                    }
                    
                    // 2. 调用 AI 服务生成修复代码
                    indicator.setText("Generating fix with AI...");
                    AICodeFixService fixService = AICodeFixService.getInstance(project);
                    String fixedCode = fixService.generateFix(context);
                    
                    if (fixedCode == null || fixedCode.isEmpty()) {
                        showError(project, "AI failed to generate fix");
                        return;
                    }
                    
                    // 3. 应用修复
                    indicator.setText("Applying fix...");
                    ApplicationManager.getApplication().invokeLater(() -> {
                        applyFixToFile(project, descriptor, context, fixedCode);
                    });
                    
                } catch (Exception e) {
                    LOG.error("Failed to apply AI fix", e);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        showError(project, "Failed to apply AI fix: " + e.getMessage());
                    });
                }
            }
        }.queue();
    }
    
    /**
     * 收集修复所需的上下文信息
     */
    private FixContext collectContext(@NotNull Project project, 
                                     @NotNull ProblemDescriptor descriptor) {
        try {
            PsiElement element = descriptor.getPsiElement();
            PsiFile file = element.getContainingFile();
            
            // 确定需要修复的范围
            PsiElement targetElement = determineTargetElement(element);
            if (targetElement == null) {
                return null;
            }
            
            // 获取周围代码上下文
            String surroundingCode = getSurroundingCode(targetElement);
            String fileContent = file.getText();
            
            return FixContextBuilder.builder()
                .problem(problem)
                .file(file)
                .targetElement(targetElement)
                .violationMessage(problem.message())
                .violationSource(problem.sourceName())
                .line(problem.line())
                .column(problem.column())
                .surroundingCode(surroundingCode)
                .fileContent(fileContent)
                .build();
                
        } catch (Exception e) {
            LOG.error("Failed to collect context", e);
            return null;
        }
    }
    
    /**
     * 确定需要修复的目标元素
     * 根据问题类型，可能是方法、类、字段等
     */
    private PsiElement determineTargetElement(@NotNull PsiElement element) {
        // 尝试向上查找更大的作用域
        // 例如：如果问题是方法内的，返回整个方法
        
        // 对于 Java 文件
        if (element.getLanguage().isKindOf(JavaLanguage.INSTANCE)) {
            // 尝试查找方法
            PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
            if (method != null) {
                return method;
            }
            
            // 尝试查找类
            PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);
            if (clazz != null) {
                return clazz;
            }
        }
        
        // 默认返回元素本身
        return element;
    }
    
    /**
     * 获取周围代码上下文
     */
    private String getSurroundingCode(@NotNull PsiElement element) {
        // 获取元素的文本内容
        String elementText = element.getText();
        
        // 如果需要更多上下文，可以获取前后的代码
        PsiFile file = element.getContainingFile();
        int elementStart = element.getTextRange().getStartOffset();
        int elementEnd = element.getTextRange().getEndOffset();
        
        // 获取前后各 5 行的代码
        int contextLines = 5;
        Document document = PsiDocumentManager.getInstance(element.getProject())
            .getDocument(file);
        if (document != null) {
            int startLine = document.getLineNumber(elementStart);
            int endLine = document.getLineNumber(elementEnd);
            
            int contextStartLine = Math.max(0, startLine - contextLines);
            int contextEndLine = Math.min(document.getLineCount() - 1, endLine + contextLines);
            
            int contextStartOffset = document.getLineStartOffset(contextStartLine);
            int contextEndOffset = document.getLineEndOffset(contextEndLine);
            
            return document.getText(new TextRange(contextStartOffset, contextEndOffset));
        }
        
        return elementText;
    }
    
    /**
     * 应用修复到文件
     */
    private void applyFixToFile(@NotNull Project project,
                                @NotNull ProblemDescriptor descriptor,
                                @NotNull FixContext context,
                                @NotNull String fixedCode) {
        WriteCommandAction.runWriteCommandAction(project, "Apply AI Fix", null, () -> {
            try {
                PsiElement targetElement = context.targetElement();
                
                // 解析修复后的代码
                PsiElement newElement = createPsiElementFromCode(
                    project, 
                    context.file(), 
                    fixedCode
                );
                
                if (newElement != null) {
                    // 替换元素
                    targetElement.replace(newElement);
                    
                    // 格式化代码
                    CodeStyleManager codeStyleManager = 
                        CodeStyleManager.getInstance(project);
                    codeStyleManager.reformat(targetElement);
                }
                
            } catch (Exception e) {
                LOG.error("Failed to apply fix to file", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 从代码字符串创建 PsiElement
     */
    private PsiElement createPsiElementFromCode(@NotNull Project project,
                                                @NotNull PsiFile file,
                                                @NotNull String code) {
        // 使用 JavaParserFacade 解析代码
        // 这需要根据具体的元素类型来处理
        // ...
        return null;
    }
    
    private void showError(@NotNull Project project, @NotNull String message) {
        // 显示错误通知
        // ...
    }
}
```

### FixContext.java

```java
package dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.context;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.infernus.idea.checkstyle.checker.Problem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 修复上下文信息
 */
public record FixContext(
    @NotNull Problem problem,
    @NotNull PsiFile file,
    @NotNull PsiElement targetElement,
    @NotNull String violationMessage,
    @Nullable String violationSource,
    int line,
    int column,
    @NotNull String surroundingCode,
    @NotNull String fileContent
) {
    public static FixContextBuilder builder() {
        return new FixContextBuilder();
    }
    
    public static class FixContextBuilder {
        private Problem problem;
        private PsiFile file;
        private PsiElement targetElement;
        private String violationMessage;
        private String violationSource;
        private int line;
        private int column;
        private String surroundingCode;
        private String fileContent;
        
        public FixContextBuilder problem(@NotNull Problem problem) {
            this.problem = problem;
            return this;
        }
        
        public FixContextBuilder file(@NotNull PsiFile file) {
            this.file = file;
            return this;
        }
        
        public FixContextBuilder targetElement(@NotNull PsiElement targetElement) {
            this.targetElement = targetElement;
            return this;
        }
        
        public FixContextBuilder violationMessage(@NotNull String violationMessage) {
            this.violationMessage = violationMessage;
            return this;
        }
        
        public FixContextBuilder violationSource(@Nullable String violationSource) {
            this.violationSource = violationSource;
            return this;
        }
        
        public FixContextBuilder line(int line) {
            this.line = line;
            return this;
        }
        
        public FixContextBuilder column(int column) {
            this.column = column;
            return this;
        }
        
        public FixContextBuilder surroundingCode(@NotNull String surroundingCode) {
            this.surroundingCode = surroundingCode;
            return this;
        }
        
        public FixContextBuilder fileContent(@NotNull String fileContent) {
            this.fileContent = fileContent;
            return this;
        }
        
        public FixContext build() {
            return new FixContext(
                problem,
                file,
                targetElement,
                violationMessage,
                violationSource,
                line,
                column,
                surroundingCode,
                fileContent
            );
        }
    }
}
```

### AICodeFixService.java

```java
package dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.service;

import dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.context.FixContext;
import dev.dong4j.zeka.stack.idea.plugin.common.ai.AIService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AI 代码修复服务
 */
public class AICodeFixService {
    
    private final AIService aiService;
    
    public AICodeFixService(@NotNull AIService aiService) {
        this.aiService = aiService;
    }
    
    public static AICodeFixService getInstance(@NotNull com.intellij.openapi.project.Project project) {
        // 获取 AIService 实例
        AIService aiService = project.getService(AIService.class);
        return new AICodeFixService(aiService);
    }
    
    /**
     * 生成修复代码
     */
    @Nullable
    public String generateFix(@NotNull FixContext context) {
        // 构建提示词
        String prompt = buildFixPrompt(context);
        
        // 调用 AI 服务
        return aiService.generateCode(prompt);
    }
    
    /**
     * 构建修复提示词
     */
    @NotNull
    private String buildFixPrompt(@NotNull FixContext context) {
        return String.format("""
            你是一个 Java 代码修复专家。请修复以下 Checkstyle 违规问题。
            
            **违规信息**:
            - 检查规则: %s
            - 违规消息: %s
            - 位置: 第 %d 行，第 %d 列
            
            **当前代码**:
            ```java
            %s
            ```
            
            **要求**:
            1. 修复 Checkstyle 违规问题
            2. 保持代码的功能不变
            3. 只返回修复后的代码片段，不要包含解释
            4. 保持代码风格一致
            5. 只修复指定的问题，不要改动其他部分
            
            **修复后的代码**:
            """,
            context.violationSource() != null ? context.violationSource() : "Unknown",
            context.violationMessage(),
            context.line(),
            context.column(),
            context.surroundingCode()
        );
    }
}
```

### plugin.xml 注册

```xml
<!-- 你的 AI 插件的 plugin.xml -->
<idea-plugin>
    <id>dev.dong4j.zeka.stack.idea.plugin.checkstyle.ai</id>
    <name>IntelliAI Checkstyle</name>
    
    <depends>com.intellij.modules.platform</depends>
    <depends>com.intellij.modules.java</depends>
    <depends>CheckStyle-IDEA</depends>
    <depends>dev.dong4j.zeka.stack.idea.plugin.common.ai</depends>
    
    <extensions defaultExtensionNs="CheckStyle-IDEA">
        <checkstyleQuickFixProvider 
            implementation="dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.AICheckstyleQuickFixProvider"/>
    </extensions>
</idea-plugin>
```

## 3. 使用流程

1. **用户编辑代码** → Checkstyle Inspection 检测到违规
2. **显示问题** → IDEA 显示 ProblemDescriptor，包含 QuickFix 列表
3. **用户选择 "Fix with AI"** → 触发 `AICheckstyleFix.applyFix()`
4. **收集上下文** → 收集代码上下文、违规信息等
5. **调用 AI** → 使用 IntelliAI Engine 生成修复代码
6. **应用修复** → 使用 WriteCommandAction 替换代码
7. **格式化** → 自动格式化修复后的代码

## 4. 注意事项

1. **错误处理**: AI 可能失败，需要友好的错误提示
2. **进度显示**: AI 调用较慢，需要显示进度条
3. **代码解析**: 从 AI 返回的代码字符串解析为 PsiElement 需要仔细处理
4. **撤销支持**: IDEA 会自动提供撤销功能
5. **批量修复**: 未来可以支持批量修复多个问题
