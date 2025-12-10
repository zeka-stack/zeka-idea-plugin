# TokenCounter 工具类使用说明

## 📖 概述

`TokenCounter` 是一个用于估算文本 Token 数量的工具类，主要用于 AI API 调用的成本估算和上下文限制检查。

## ✨ 特性

- **零依赖**：基于统计规律进行估算，无需额外依赖库
- **中英文支持**：自动识别中英文并采用不同的计算规则
- **代码识别**：针对代码文本提供专门的估算方法
- **详细统计**：提供字符数、单词数、中英文比例等详细信息
- **高性能**：处理 1000 行文本 < 100ms

## 🎯 估算规则

基于 OpenAI GPT 系列模型的统计规律：

| 文本类型 | 估算规则               |
|------|--------------------|
| 英文文本 | 约 4 字符 = 1 token   |
| 中文文本 | 约 1.5 字符 = 1 token |
| 代码文本 | 约 3.5 字符 = 1 token |
| 英文单词 | 平均 1.3 token/word  |

> 注意：这是估算值，与实际 token 数量的误差约 ±20%

## 📝 使用示例

### 1. 快速估算

```java
// 估算普通文本
String text = "Hello, World! 你好，世界！";
int tokens = TokenCounter.estimateTokens(text);
System.out.println("估算 tokens: " + tokens);

// 估算代码文本
String code = "public class Test { }";
int codeTokens = TokenCounter.estimateCodeTokens(code);
System.out.println("代码 tokens: " + codeTokens);
```

### 2. 获取详细统计

```java
String text = "这是一个使用 AI 生成 Javadoc 的插件。\n" +
              "It supports multiple AI providers.";

TokenStats stats = TokenCounter.analyze(text);

System.out.println("估算 tokens: " + stats.getEstimatedTokens());
System.out.println("总字符数: " + stats.getTotalChars());
System.out.println("中文字符数: " + stats.getChineseChars());
System.out.println("英文单词数: " + stats.getEnglishWords());
System.out.println("行数: " + stats.getLines());
System.out.println("中文占比: " + String.format("%.1f%%", stats.getChineseRatio() * 100));
System.out.println("是否为代码: " + stats.isProbablyCode());
System.out.println("平均每行 tokens: " + String.format("%.1f", stats.getAvgTokensPerLine()));

// 输出完整统计信息
System.out.println(stats.toString());
```

### 3. 检查是否超过限制

```java
String text = "很长的文本...";
int maxTokens = 4096; // GPT-3.5 的上下文限制

if (TokenCounter.exceedsLimit(text, maxTokens)) {
    System.out.println("文本超过了 " + maxTokens + " token 限制");
    // 截断文本
    String truncated = TokenCounter.truncateToTokenLimit(text, maxTokens);
    System.out.println("截断后的文本: " + truncated);
}
```

### 4. 计算多个文本的总 Token 数

```java
String systemPrompt = "你是一个专业的 Java 文档生成助手。";
String userPrompt = "请为以下方法生成 Javadoc 注释：";
String code = "public void test() { }";

int totalTokens = TokenCounter.estimateTotalTokens(systemPrompt, userPrompt, code);
System.out.println("总 tokens: " + totalTokens);
```

### 5. 截断文本以符合限制

```java
String longText = "这是一个很长的文本，需要截断到指定的 token 数量。" +
                  "截断会尽量在完整的句子或单词处进行...";

// 截断到最多 50 tokens（会预留 10% 余量）
String truncated = TokenCounter.truncateToTokenLimit(longText, 50);
System.out.println(truncated);
// 输出: "这是一个很长的文本，需要截断到指定的 token 数量..."
```

## 🔧 在插件中的应用

### 1. 在 AI 服务调用前检查

```java
public class AIServiceProvider {
    private static final int MAX_CONTEXT_TOKENS = 4096;

    public String generateDocumentation(String code) {
        // 检查代码 token 数量
        int codeTokens = TokenCounter.estimateCodeTokens(code);

        if (codeTokens > MAX_CONTEXT_TOKENS * 0.8) {
            // 代码太长，需要截断或分段处理
            throw new AIServiceException("代码太长，超过了上下文限制");
        }

        // 调用 AI API
        return callAIAPI(code);
    }
}
```

### 2. 显示统计信息

```java
public class TaskExecutor {
    private void logTaskInfo(DocumentationTask task) {
        String code = task.getCode();
        TokenStats stats = TokenCounter.analyze(code);

        String message = String.format(
            "任务: %s, 代码长度: %d 字符, 估算 tokens: %d, 行数: %d",
            task.getType(),
            stats.getTotalChars(),
            stats.getEstimatedTokens(),
            stats.getLines()
        );

        JavaDocConsoleView.print(project, message);
    }
}
```

### 3. 成本估算

```java
public class CostCalculator {
    // GPT-3.5 价格：$0.0005 / 1K tokens (输入)
    private static final double PRICE_PER_1K_TOKENS = 0.0005;

    public double estimateCost(String text) {
        int tokens = TokenCounter.estimateTokens(text);
        return (tokens / 1000.0) * PRICE_PER_1K_TOKENS;
    }

    public void displayCostInfo(List<DocumentationTask> tasks) {
        int totalTokens = 0;
        for (DocumentationTask task : tasks) {
            totalTokens += TokenCounter.estimateCodeTokens(task.getCode());
        }

        double cost = (totalTokens / 1000.0) * PRICE_PER_1K_TOKENS;

        String message = String.format(
            "预计消耗: %d tokens, 约 $%.4f",
            totalTokens, cost
        );

        System.out.println(message);
    }
}
```

### 4. 优化提示词长度

```java
public class PromptOptimizer {
    public String optimizePrompt(String template, String code, int maxTokens) {
        // 估算模板的 token 数
        int templateTokens = TokenCounter.estimateTokens(template);

        // 计算代码可用的 token 数量
        int availableForCode = maxTokens - templateTokens - 100; // 预留 100 tokens

        // 如果代码太长，截断它
        if (TokenCounter.estimateCodeTokens(code) > availableForCode) {
            code = TokenCounter.truncateToTokenLimit(code, availableForCode);
        }

        return template + "\n\n" + code;
    }
}
```

## 📊 TokenStats 详细说明

`TokenStats` 类提供了详细的文本统计信息：

| 字段                | 说明                     |
|-------------------|------------------------|
| `estimatedTokens` | 估算的 token 数量           |
| `totalChars`      | 总字符数                   |
| `chineseChars`    | 中文字符数                  |
| `englishWords`    | 英文单词数                  |
| `otherChars`      | 其他字符数                  |
| `codeSymbols`     | 代码符号数（`{}[]();,.<>` 等） |
| `lines`           | 行数                     |

### 计算属性

```java
TokenStats stats = TokenCounter.analyze(text);

// 获取平均每行 token 数
double avgTokensPerLine = stats.getAvgTokensPerLine();

// 获取中文字符占比 (0-1)
double chineseRatio = stats.getChineseRatio();

// 判断是否主要是代码（代码符号占比 > 10%）
boolean isCode = stats.isProbablyCode();
```

## ⚠️ 注意事项

1. **估算值**：返回的 token 数量是估算值，与实际值可能有 ±20% 的误差
2. **不同模型**：不同的 AI 模型可能使用不同的 tokenizer，计算结果可能略有差异
3. **截断精度**：`truncateToTokenLimit` 是粗略截断，建议预留 10-20% 的余量
4. **性能考虑**：对于超大文本（> 10MB），建议分块处理

## 🔮 未来改进

如果需要更精确的 token 计算，可以考虑集成以下库：

1. **jtokkit**：OpenAI tiktoken 的 Java 实现
   ```gradle
   implementation("com.knuddels:jtokkit:1.0.0")
   ```

2. **使用方式**：
   ```java
   // 精确计算（需要 jtokkit 库）
   Encoding encoding = EncodingRegistry.getEncoding(EncodingType.CL100K_BASE);
   int exactTokens = encoding.countTokens(text);
   ```

## 📚 相关资源

- [OpenAI Token 计算器](https://platform.openai.com/tokenizer)
- [GPT Token 计算规则](https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them)
- [jtokkit GitHub](https://github.com/knuddelsgmbh/jtokkit)


