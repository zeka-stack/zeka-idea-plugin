# 基于 PSI 优化提示词上下文

## 一、背景

### 1.1 问题提出

在 AI 生成 Commit Message 的场景中，我们面临一个核心问题：**如何让 AI 更准确地理解代码变更的语义？**

传统的文本 diff（Git diff）虽然能够完整地展示代码变更，但对于 AI 来说，存在以下局限性：

- **缺乏结构语义**：AI 只能看到行级别的增删改，无法理解这是"接口变更"还是"实现调整"
- **噪音干扰**：格式化、重命名等非语义变更会干扰 AI 的判断
- **上下文缺失**：无法识别变更影响的范围（public API vs 内部实现）

### 1.2 设计原则

经过深入分析，我们确立了以下设计原则：

> **Diff 用 Git，理解用 PSI，表达交给 AI。**

这意味着：

- ✅ **Git diff 作为唯一的事实来源**：保持与 Git 提交的一致性，不重复造轮子
- ✅ **PSI 作为语义增强器**：不替代 diff，而是为 diff 添加结构级语义标签
- ✅ **AI 负责最终表达**：将结构化的语义信息作为上下文提供给 AI，提升生成质量

### 1.3 核心价值

通过 PSI 语义分析，我们能够：

1. **提升 AI 输出质量**：让 AI 理解"接口变更"与"实现调整"的区别
2. **支持风险分析**：识别 breaking changes、API 变更等高风险变更
3. **优化提示词结构**：将语义信息结构化，便于 AI 理解和处理
4. **扩展性设计**：支持多语言（Java、Kotlin、XML 等）的语义分析

## 二、方案设计

### 2.1 架构概览

```
┌─────────────┐
│  Git Diff   │ ← 唯一 diff 来源（文本级）
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ PSI 分析层  │ ← 语义增强（结构级）
│ - 接口变更  │
│ - 实现调整  │
│ - 行为变化  │
│ - 重构识别  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ Prompt 构建 │ ← 结构化上下文
│ - diff      │
│ - semantic  │
│ - metadata  │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  AI 生成    │ ← 高质量输出
└─────────────┘
```

### 2.2 核心接口设计

#### 2.2.1 LanguageContextResolver

语言上下文解析器的核心接口，定义了多语言支持的统一规范：

```java
public interface LanguageContextResolver {
    /**
     * 判断是否支持该文件类型
     */
    boolean supports(@NotNull VirtualFile file);

    /**
     * 解析变更位置的上下文信息（类名、方法名等）
     */
    @Nullable
    String resolveContext(@NotNull VirtualFile file,
                         int preferredLine,
                         int fallbackLine);

    /**
     * 解析文件的主要符号名称（如类名）
     */
    @Nullable
    String resolvePrimarySymbolName(@NotNull Project project,
                                   @NotNull VirtualFile file);

    /**
     * 解析基于 PSI 的语义摘要
     * 核心方法：将 diff 升级为结构语义 diff
     */
    @Nullable
    String resolveSemanticSummary(@NotNull Project project,
                                 @NotNull VirtualFile file,
                                 @NotNull String beforeContent,
                                 @NotNull String afterContent,
                                 @NotNull List<LineFragment> fragments);
}
```

#### 2.2.2 ContextResolverRegistry

解析器注册表，负责管理和调度不同语言的解析器：

- **内置解析器**：Java、XML
- **动态加载**：通过反射检查 Java 插件是否安装
- **SPI 扩展**：支持通过 ServiceLoader 扩展新的语言解析器

### 2.3 语义分析模型

#### 2.3.1 变更类型分类

我们将代码变更分为以下几个语义类别：

| 类别       | 说明                          | 示例                    |
|----------|-----------------------------|-----------------------|
| **接口变更** | public API 签名变化             | 方法参数类型变化、返回值变化        |
| **实现调整** | 方法体内部逻辑变化，但签名未变             | 优化算法、修复 bug           |
| **行为变化** | 控制流变化（if/return/throw 数量变化） | 新增条件分支、异常处理           |
| **重构**   | 结构性调整，无行为变化                 | 方法拆分、变量重命名            |
| **类变更**  | 类签名、继承关系变化                  | extends/implements 变化 |
| **字段变更** | 字段定义、默认值变化                  | public 字段新增/删除        |
| **注解变更** | 类/方法注解变化                    | @RequestMapping 参数变化  |

#### 2.3.2 判断逻辑

**接口变更 vs 实现调整**

```java
// 接口变更判断
boolean isPublicApi = method.hasModifierProperty(PsiModifier.PUBLIC)
                   || method.hasModifierProperty(PsiModifier.PROTECTED);

boolean signatureChanged = !oldMethod.getSignature(PsiSubstitutor.EMPTY)
                              .equals(newMethod.getSignature(PsiSubstitutor.EMPTY));

// 实现调整判断
boolean bodyChanged = !oldMethod.getBody().getText()
                         .equals(newMethod.getBody().getText());
```

**重构 vs 行为变化**

```java
// 行为变化判断：控制流结构变化
int oldIfCount = PsiTreeUtil.findChildrenOfType(oldBody, PsiIfStatement.class).size();
int newIfCount = PsiTreeUtil.findChildrenOfType(newBody, PsiIfStatement.class).size();
boolean behaviorChanged = oldIfCount != newIfCount
                      || oldReturnCount != newReturnCount
                      || oldThrowCount != newThrowCount;

// 重构判断：仅排版/命名变化
String beforeText = beforeBody.getText().replaceAll("\\s+", "");
String afterText = afterBody.getText().replaceAll("\\s+", "");
boolean isRefactor = beforeText.equals(afterText);
```

## 三、实现细节

### 3.1 JavaPsiContextResolver 实现

#### 3.1.1 核心流程

```java
@Override
public @Nullable String resolveSemanticSummary(...) {
    // 1. 创建变更前后的 PSI 文件快照
    PsiJavaFile beforeFile = createPsiFile(project, fileName, beforeContent);
    PsiJavaFile afterFile = createPsiFile(project, fileName, afterContent);

    // 2. 遍历 diff 片段，定位变更位置
    for (LineFragment fragment : fragments) {
        // 3. 通过行号定位 PSI 元素（方法/字段/类）
        PsiMethod beforeMethod = findMethodAtLine(beforeFile, beforeContent, fragment.getStartLine1());
        PsiMethod afterMethod = findMethodAtLine(afterFile, afterContent, fragment.getStartLine2());

        // 4. 分析变更类型并统计
        if (isSignatureChanged(beforeMethod, afterMethod)) {
            counters.apiSignatureChanges++;
        } else if (isBehaviorChanged(beforeMethod.getBody(), afterMethod.getBody())) {
            counters.behaviorChanges++;
        } else if (isRefactorChange(...)) {
            counters.refactorChanges++;
        }
    }

    // 5. 生成结构化语义摘要
    return buildSummary(counters, details);
}
```

#### 3.1.2 行号定位 PSI 元素

关键实现：通过 diff 行号反查 PSI 元素

```java
private PsiMethod findMethodAtLine(PsiJavaFile psiFile, String content, int line) {
    // 计算行号对应的文本偏移量
    int offset = lineStartOffset(content, line);

    // 查找该位置的 PSI 元素
    PsiElement element = psiFile.findElementAt(offset);

    // 向上查找包含该元素的方法节点
    return PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
}
```

#### 3.1.3 语义摘要输出格式

```text
变更语义总结:
- 接口层：1 个 public API 方法签名变更
- 实现层：2 个方法内部实现调整
- 行为：1 处行为变化
- 重构：1 处结构调整（无明显行为变化）
- 细节：
  - public 方法签名变更: UserService#login(UserDTO)
  - 实现调整: UserService#getUserInfo()
  - 行为变化: UserService#checkPermission()
```

### 3.2 ContextResolverRegistry 实现

#### 3.2.1 解析器加载策略

```java
private static List<LanguageContextResolver> getResolvers() {
    List<LanguageContextResolver> list = new ArrayList<>();

    // 1. 内置解析器（始终可用）
    list.add(new XmlContextResolver());

    // 2. 条件加载 Java 解析器（需要 Java 插件）
    LanguageContextResolver javaResolver = loadJavaResolverIfAvailable();
    if (javaResolver != null) {
        list.add(javaResolver);
    }

    // 3. SPI 扩展（支持外部插件扩展）
    loadResolversFromSpi(list);

    return Collections.unmodifiableList(list);
}
```

#### 3.2.2 Java 解析器的条件加载

```java
private static LanguageContextResolver loadJavaResolverIfAvailable() {
    // 检查 Java 插件是否安装
    if (!PluginManagerCore.isPluginInstalled(PluginId.getId("com.intellij.java"))) {
        return null;
    }

    // 通过反射加载（避免编译时依赖）
    try {
        Class<?> resolverClass = Class.forName(JAVA_RESOLVER_CLASS);
        Object instance = resolverClass.getDeclaredConstructor().newInstance();
        if (instance instanceof LanguageContextResolver resolver) {
            return resolver;
        }
    } catch (Throwable ignored) {
        // 加载失败时静默处理，不影响其他功能
    }
    return null;
}
```

### 3.3 集成到 Prompt 构建流程

#### 3.3.1 在 CodeDiffUtil 中调用

```java
private static String resolveSemanticSummary(VirtualFile virtualFile, DiffResult diffResult) {
    Project project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
    if (project == null || project.isDisposed()) {
        return null;
    }

    // 调用注册表解析语义摘要
    return ContextResolverRegistry.resolveSemanticSummary(
        project,
        virtualFile,
        diffResult.beforeContent(),
        diffResult.afterContent(),
        diffResult.fragments()
    );
}
```

#### 3.3.2 在 ChangelogPromptBuilder 中嵌入

语义摘要作为 `semantic_summary` 字段嵌入到 JSON 上下文中：

```json
{
  "changes": [
    {
      "path": "UserService.java",
      "full_diff_content": "...",
      "semantic_summary": "变更语义总结:\n- 接口层：1 个 public API 方法签名变更\n..."
    }
  ]
}
```

### 3.4 线程安全与性能优化

#### 3.4.1 ReadAction 保护

所有 PSI 操作都在 `ReadAction` 中执行，确保线程安全：

```java
return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
    // PSI 操作
    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    // ...
});
```

#### 3.4.2 DumbMode 检查

在索引未完成时（DumbMode），跳过 PSI 分析：

```java
if (DumbService.isDumb(project)) {
    return null; // 降级为纯文本 diff
}
```

#### 3.4.3 去重处理

使用 `Set<String>` 避免重复分析同一个方法/字段：

```java
Set<String> processedMethods = new HashSet<>();
String methodKey = buildMethodKey(method);
if (!processedMethods.add(methodKey)) {
    continue; // 已处理过，跳过
}
```

## 四、使用示例

### 4.1 典型场景

**场景 1：接口变更**

```java
// 变更前
public User login(UserDTO dto) { ... }

// 变更后
public User login(UserCommand command) { ... }
```

**语义摘要输出：**

```
变更语义总结:
- 接口层：1 个 public API 方法签名变更
- 细节：
  - public 方法签名变更: UserService#login(UserDTO) → login(UserCommand)
```

**场景 2：行为变化**

```java
// 变更前
public boolean checkPermission(User user) {
    return user.isAdmin();
}

// 变更后
public boolean checkPermission(User user) {
    if (user.isAdmin()) {
        return true;
    }
    return user.hasRole("editor"); // 新增条件分支
}
```

**语义摘要输出：**

```
变更语义总结:
- 行为：1 处行为变化
- 细节：
  - 行为变化: UserService#checkPermission() 新增条件分支
```

**场景 3：重构**

```java
// 变更前
public void process() {
    // 长方法实现
}

// 变更后
public void process() {
    validate();
    execute();
    cleanup();
}

private void validate() { ... }
private void execute() { ... }
private void cleanup() { ... }
```

**语义摘要输出：**

```
变更语义总结:
- 重构：1 处结构调整（无明显行为变化）
- 细节：
  - 重构调整: UserService#process() 方法拆分
```

### 4.2 AI Prompt 增强效果

**增强前（仅文本 diff）：**

```
文件: UserService.java
变更内容:
- public User login(UserDTO dto)
+ public User login(UserCommand command)
```

**增强后（包含语义摘要）：**

```
文件: UserService.java
变更内容:
- public User login(UserDTO dto)
+ public User login(UserCommand command)

变更语义总结:
- 接口层：1 个 public API 方法签名变更
- 细节：
  - public 方法签名变更: UserService#login(UserDTO) → login(UserCommand)
```

AI 能够更准确地识别这是一个 **breaking change**，需要更新调用方。

## 五、扩展性设计

### 5.1 添加新语言支持

#### 5.1.1 实现 LanguageContextResolver

以 Kotlin 为例：

```java
public class KotlinPsiContextResolver implements LanguageContextResolver {
    @Override
    public boolean supports(@NotNull VirtualFile file) {
        return "kt".equalsIgnoreCase(file.getExtension());
    }

    @Override
    public @Nullable String resolveSemanticSummary(...) {
        // Kotlin 特定的 PSI 分析逻辑
        // 使用 KtClass, KtFunction 等 Kotlin PSI API
    }
}
```

#### 5.1.2 通过 SPI 注册

在 `META-INF/services/dev.dong4j.zeka.stack.idea.plugin.changelog.context.LanguageContextResolver` 文件中添加：

```
dev.dong4j.zeka.stack.idea.plugin.changelog.context.KotlinPsiContextResolver
```

### 5.2 自定义语义规则

可以在 `JavaPsiContextResolver` 中扩展更细粒度的判断逻辑：

```java
// 示例：识别 Spring 注解变更
private boolean isSpringAnnotationChanged(PsiClass beforeClass, PsiClass afterClass) {
    String beforeMapping = extractRequestMapping(beforeClass);
    String afterMapping = extractRequestMapping(afterClass);
    return !beforeMapping.equals(afterMapping);
}
```

