# Javadoc 单行注释格式化功能实现方案

## 功能概述

实现 Javadoc 单行注释的自动格式化功能，当 Javadoc 注释只有一行内容时，自动格式化为单行格式，而不是多行格式。

### 需求描述

**当前格式**：

```java
/**
 * One-line comment
 */
public abstract String sampleMethod2();
```

**期望格式**：

```java
/** One-line comment */
public abstract String sampleMethod2();
```

## 实现方案

### 方案选择

经过分析，有两种实现方案：

1. **方案一：配置 CodeStyleSettings**
    - 通过编程方式修改 `CodeStyleSettings` 中的 Javadoc 格式化选项
    - 优点：利用 IDE 内置格式化机制
    - 缺点：可能影响全局格式化设置，需要临时修改和恢复

2. **方案二：后处理格式化结果**（推荐）
    - 在格式化完成后，检测 Javadoc 是否为单行注释
    - 如果是单行注释，将其压缩为单行格式
    - 优点：不影响全局设置，实现简单，可控性强
    - 缺点：需要手动处理格式转换

**选择方案二**，原因：

- 不影响用户的代码格式化设置
- 实现简单，维护成本低
- 可以精确控制格式化逻辑

### 技术细节

#### 1. 单行注释检测逻辑

检测 Javadoc 注释是否为单行注释的标准：

- 注释内容只有一行（不包括开始标记 `/**` 和结束标记 `*/`）
- 注释内容不包含 `@param`、`@return`、`@throws` 等标签
- 注释内容长度合理（不超过一定长度限制，如 80 字符）

#### 2. 格式转换逻辑

如果检测到是单行注释，执行以下转换：

1. 提取注释内容（去除 `/**`、`*/`、`*`、空白字符）
2. 压缩为单行格式：`/** 注释内容 */`
3. 替换原始的多行格式

#### 3. 实现位置

在 `DocumentationInserterHelper.insertDocumentation()` 方法中，格式化完成后进行后处理：

```java
// 7. 格式化插入的 Javadoc
CodeStyleManager.getInstance(project).reformatText(psiFile, lineStartPosition, endPosition);

// 8. 后处理：如果是单行注释，压缩为单行格式
compressSingleLineJavaDoc(psiFile, element, document);
```

### 涉及的文件

1. **DocumentationInserterHelper.java**
    - 添加单行注释检测和压缩逻辑
    - 在格式化后调用压缩方法

2. **JavaDocFormatter.java**（可选）
    - 可以添加单行注释格式化的辅助方法
    - 或者创建新的工具类

### 实现步骤

1. 创建 `JavaDocSingleLineFormatter` 工具类
    - 提供 `isSingleLineComment()` 方法检测是否为单行注释
    - 提供 `compressToSingleLine()` 方法压缩为单行格式

2. 修改 `DocumentationInserterHelper.java`
    - 在 `insertDocumentation()` 方法中添加后处理逻辑
    - 调用单行注释压缩方法

3. 添加单元测试（可选）
    - 测试单行注释检测逻辑
    - 测试格式转换逻辑

## 注意事项

1. **性能考虑**
    - 只在格式化完成后执行一次检测和转换
    - 使用高效的字符串处理方法

2. **边界情况处理**
    - 空注释：`/** */` 保持原样
    - 只有标签的注释：不压缩为单行
    - 超长单行注释：可能需要考虑是否压缩（可配置）

3. **兼容性**
    - 不影响多行注释的格式化
    - 不影响包含标签的注释
    - 不影响其他代码的格式化

## 测试计划

### 测试用例 1：基本单行注释

- 输入：`/** One-line comment */`
- 期望：保持单行格式

### 测试用例 2：多行单行注释（需要压缩）

- 输入：
  ```java
  /**
   * One-line comment
   */
  ```
- 期望：压缩为 `/** One-line comment */`

### 测试用例 3：多行注释（不压缩）

- 输入：
  ```java
  /**
   * Multi-line comment
   * with multiple lines
   */
  ```
- 期望：保持多行格式

### 测试用例 4：包含标签的注释（不压缩）

- 输入：
  ```java
  /**
   * Comment with @param tag
   * @param name parameter name
   */
  ```
- 期望：保持多行格式

## 后续优化

1. 添加配置选项，允许用户选择是否启用单行注释压缩
2. 添加长度限制配置，超过指定长度的单行注释不压缩
3. 支持其他注释风格（如 KDOC）

