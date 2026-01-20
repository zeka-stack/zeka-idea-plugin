# Checkstyle-IDEA AI 自动修复扩展方案分析

## 1. 插件架构分析

### 1.1 核心组件

#### CheckStyleInspection (检查入口)

- **位置**: `org.infernus.idea.checkstyle.CheckStyleInspection`
- **功能**: 实现 `LocalInspectionTool`，在编辑器中实时检查代码
- **关键方法**: `checkFile()` → 返回 `ProblemDescriptor[]`

#### Problem (违规信息封装)

- **位置**: `org.infernus.idea.checkstyle.checker.Problem`
- **数据结构** (record):
  ```java
  record Problem(
      @NotNull PsiElement target,      // 违规的元素
      @NotNull String message,          // 违规消息
      @NotNull SeverityLevel severityLevel,  // 严重级别
      int line,                        // 行号
      int column,                      // 列号
      String sourceName,               // 检查名称（如 "MissingJavadocMethod"）
      boolean afterEndOfLine,
      boolean suppressErrors
  )
  ```
- **关键方法**: `toProblemDescriptor()` → 转换为 IDEA 的 `ProblemDescriptor`

#### QuickFix 机制

- **当前实现**: `Problem.quickFixes()` 只返回 `SuppressForCheckstyleFix`
- **SuppressForCheckstyleFix**: 添加 `@SuppressWarnings("checkstyle:xxx")` 注解

### 1.2 数据流

```
文件修改
  ↓
CheckStyleInspection.checkFile()
  ↓
inspectFile() → 调用 Checker 扫描
  ↓
返回 List<Problem>
  ↓
Problem.toProblemDescriptor()
  ↓
返回 ProblemDescriptor[] (包含 LocalQuickFix[])
  ↓
IDEA 显示在编辑器中
```

## 2. 扩展方案设计

### 2.1 方案一：修改 checkstyle-idea 插件（不推荐）

**优点**:

- 直接集成，无需额外插件
- 可以完全控制 QuickFix 的生成

**缺点**:

- 需要维护 fork 版本
- 每次 checkstyle-idea 更新都需要合并
- 违反开闭原则

**实现**:

```java
// 修改 Problem.java
private LocalQuickFix[] quickFixes(final String sourceCheck) {
    List<LocalQuickFix> fixes = new ArrayList<>();

    // 原有的 suppress fix
    if (sourceCheck != null) {
        fixes.add(new SuppressForCheckstyleFix(sourceCheck));
    }

    // 添加 AI Fix（需要检测是否安装了 IntelliAI Engine）
    if (isIntelliAIEngineAvailable()) {
        fixes.add(new AICheckstyleFix(this));
    }

    return fixes.toArray(new LocalQuickFix[0]);
}
```

### 2.2 方案二：通过 Extension Point 扩展（推荐⭐）

**优点**:

- 不需要修改 checkstyle-idea 源码
- 通过标准的 IDEA Extension Point 机制
- 可以独立开发和维护

**实现步骤**:

#### Step 1: 在 checkstyle-idea 中添加扩展接口

```java
// 在 checkstyle-idea 插件中添加
package org.infernus.idea.checkstyle.checker;

public interface CheckstyleQuickFixProvider {
    /**
     * 为指定的 Problem 提供额外的 QuickFix
     *
     * @param problem Checkstyle 违规问题
     * @return QuickFix 数组，可以为空或 null
     */
    @Nullable
    LocalQuickFix[] getQuickFixes(@NotNull Problem problem);
}
```

#### Step 2: 在 plugin.xml 中注册扩展点

```xml
<!-- checkstyle-idea 的 plugin.xml -->
<extensions defaultExtensionNs="com.intellij">
    <!-- 其他扩展 -->

    <!-- 新增：QuickFix 提供者扩展点 -->
    <extensionPoint name="checkstyleQuickFixProvider"
                    interface="org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider"
                    area="IDEA_PROJECT"/>
</extensions>
```

#### Step 3: 修改 Problem.java 使用扩展点

```java
// 修改 Problem.java 的 quickFixes 方法
private LocalQuickFix[] quickFixes(final String sourceCheck) {
    List<LocalQuickFix> fixes = new ArrayList<>();

    // 原有的 suppress fix
    if (sourceCheck != null) {
        fixes.add(new SuppressForCheckstyleFix(sourceCheck));
    }

    // 通过扩展点获取额外的 Fix
    Project project = target.getProject();
    if (project != null && !project.isDisposed()) {
        ExtensionPoint<CheckstyleQuickFixProvider> extensionPoint =
            ExtensionPointName.create("org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider");

        for (CheckstyleQuickFixProvider provider : extensionPoint.getExtensions(project)) {
            LocalQuickFix[] providedFixes = provider.getQuickFixes(this);
            if (providedFixes != null) {
                fixes.addAll(Arrays.asList(providedFixes));
            }
        }
    }

    return fixes.isEmpty() ? null : fixes.toArray(new LocalQuickFix[0]);
}
```

#### Step 4: 在 AI 插件中实现扩展

```java
// 在你的 AI 插件中
package dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix;

import org.infernus.idea.checkstyle.checker.CheckstyleQuickFixProvider;
import org.infernus.idea.checkstyle.checker.Problem;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.extensions.Extension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AICheckstyleQuickFixProvider implements CheckstyleQuickFixProvider {

    @Override
    @Nullable
    public LocalQuickFix[] getQuickFixes(@NotNull Problem problem) {
        // 检查 IntelliAI Engine 是否可用
        if (!isIntelliAIEngineAvailable()) {
            return null;
        }

        // 返回 AI Fix
        return new LocalQuickFix[]{
            new AICheckstyleFix(problem)
        };
    }

    private boolean isIntelliAIEngineAvailable() {
        // 检查插件是否已安装
        // ...
    }
}
```

#### Step 5: 在你的插件中注册扩展

```xml
<!-- 你的 AI 插件的 plugin.xml -->
<extensions defaultExtensionNs="com.intellij">
    <extensions defaultExtensionNs="CheckStyle-IDEA">
        <checkstyleQuickFixProvider
            implementation="dev.dong4j.zeka.stack.idea.plugin.checkstyle.fix.AICheckstyleQuickFixProvider"/>
    </extensions>
</extensions>
```

### 2.3 方案三：通过 ProblemDescriptor 注入（备选方案）

如果无法修改 checkstyle-idea，可以通过监听或后处理的方式注入 Fix：

```java
// 在你的插件中
public class CheckstyleProblemDescriptorEnhancer {

    @PostConstruct
    public void enhanceProblemDescriptors(@NotNull Project project) {
        // 监听 ProblemDescriptor 的创建
        // 找到 Checkstyle 相关的 ProblemDescriptor
        // 动态添加 AI Fix
    }
}
```

**缺点**: 实现复杂，可能不稳定

## 3. AI Fix 实现设计

### 3.1 AICheckstyleFix 类结构

```java
public class AICheckstyleFix implements LocalQuickFix {

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
    public void applyFix(@NotNull Project project,
                        @NotNull ProblemDescriptor descriptor) {
        // 1. 收集上下文信息
        FixContext context = collectContext(project, descriptor);

        // 2. 调用 AI 服务
        String fixedCode = callAIService(context);

        // 3. 应用修复
        applyFixToFile(project, descriptor, fixedCode);
    }

    private FixContext collectContext(@NotNull Project project,
                                     @NotNull ProblemDescriptor descriptor) {
        PsiFile file = descriptor.getPsiElement().getContainingFile();
        PsiElement element = descriptor.getPsiElement();

        return FixContext.builder()
            .file(file)
            .element(element)
            .problem(problem)
            .violationMessage(problem.message())
            .violationSource(problem.sourceName())
            .line(problem.line())
            .column(problem.column())
            .surroundingCode(getSurroundingCode(element))
            .build();
    }

    private String callAIService(@NotNull FixContext context) {
        // 构建提示词
        String prompt = buildFixPrompt(context);

        // 调用 IntelliAI Engine
        AIService aiService = getAIService();
        return aiService.generateCode(prompt);
    }

    private void applyFixToFile(@NotNull Project project,
                                @NotNull ProblemDescriptor descriptor,
                                @NotNull String fixedCode) {
        // 使用 WriteAction 应用修复
        WriteAction.run(() -> {
            PsiElement element = descriptor.getPsiElement();
            // 替换代码
            element.replace(/* 新代码 */);
        });
    }
}
```

### 3.2 提示词设计

```java
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

        **修复后的代码**:
        """,
        context.violationSource(),
        context.violationMessage(),
        context.line(),
        context.column(),
        context.surroundingCode()
    );
}
```

### 3.3 上下文收集

```java
public class FixContext {
    private final PsiFile file;
    private final PsiElement element;
    private final Problem problem;
    private final String violationMessage;
    private final String violationSource;
    private final int line;
    private final int column;
    private final String surroundingCode;
    private final String fileContent;  // 可选：整个文件内容

    // 获取周围代码上下文
    private String getSurroundingCode(PsiElement element) {
        // 获取元素前后的代码（例如：前后各 5 行）
        // 或者获取整个方法/类的代码
    }
}
```

## 4. 实现建议

### 4.1 推荐方案

**方案二（Extension Point）** 是最佳选择，因为：

1. ✅ 符合开闭原则
2. ✅ 不需要维护 fork
3. ✅ 可以通过 PR 贡献到 checkstyle-idea 项目
4. ✅ 其他插件也可以扩展

### 4.2 实施步骤

#### Phase 1: 向 checkstyle-idea 贡献扩展点

1. Fork checkstyle-idea 项目
2. 添加 `CheckstyleQuickFixProvider` 接口
3. 修改 `Problem.quickFixes()` 支持扩展点
4. 提交 PR

#### Phase 2: 实现 AI Fix 插件

1. 创建新插件模块（如 `intelli-ai-checkstyle`）
2. 实现 `CheckstyleQuickFixProvider`
3. 实现 `AICheckstyleFix`
4. 集成 IntelliAI Engine

#### Phase 3: 测试和优化

1. 测试各种 Checkstyle 规则
2. 优化提示词
3. 处理边界情况
4. 性能优化

### 4.3 注意事项

1. **依赖管理**: AI 插件需要依赖 checkstyle-idea 插件
   ```xml
   <depends>CheckStyle-IDEA</depends>
   ```

2. **错误处理**: AI 服务可能失败，需要有降级方案
    - 显示错误提示
    - 提供重试机制

3. **性能考虑**:
    - AI 调用可能较慢，需要显示进度
    - 考虑批量修复多个问题

4. **用户体验**:
    - 修复前显示预览
    - 支持撤销操作
    - 记录修复历史

## 5. 扩展接口设计（详细）

如果 checkstyle-idea 不愿意添加扩展点，可以考虑：

### 5.1 最小化修改方案

只需要在 `Problem.quickFixes()` 中添加一行代码检查是否有扩展类：

```java
// 使用反射或 ServiceLoader 查找扩展实现
private LocalQuickFix[] quickFixes(final String sourceCheck) {
    List<LocalQuickFix> fixes = new ArrayList<>();

    if (sourceCheck != null) {
        fixes.add(new SuppressForCheckstyleFix(sourceCheck));
    }

    // 尝试加载外部扩展（可选）
    fixes.addAll(loadExternalFixes());

    return fixes.isEmpty() ? null : fixes.toArray(new LocalQuickFix[0]);
}

private List<LocalQuickFix> loadExternalFixes() {
    // 使用 ServiceLoader 加载
    // 或者通过反射查找特定类
}
```

这样 checkstyle-idea 只需要很小的改动，主要是向后兼容的增强。

## 6. 总结

- **最佳方案**: 通过 Extension Point 扩展（方案二）
- **备选方案**: 如果无法贡献代码，考虑 ServiceLoader 机制
- **实现重点**: AI Fix 的提示词设计和上下文收集
- **用户体验**: 预览、撤销、批量修复等功能
