# Commit Message 生成提示优化方案

## 背景说明

在 IntelliJ IDEA 插件中，当用户触发自动生成 Git Commit Message 功能时，由于 AI 服务响应需要一定时间（通常 3-10
秒），用户界面会出现一段等待期。如果在此期间没有任何视觉反馈，用户可能会认为插件卡死或功能失效，从而影响用户体验和信任度。

### 问题场景

- **用户操作**：用户在 Git 提交面板中点击"生成提交消息"按钮
- **等待时间**：AI 服务处理代码变更并生成提交消息需要 3-10 秒
- **用户感知**：如果界面没有任何变化，用户会感到困惑和不安
- **影响**：降低用户对插件功能的信任度，可能导致用户重复点击或取消操作

### 设计目标

1. **可感知性**：让用户明确知道系统正在处理请求
2. **专业性**：符合 IntelliJ IDEA 插件的 UX 设计规范
3. **非侵入性**：不打断用户的正常操作流程
4. **信息性**：通过阶段化提示让用户了解处理进度

## 解决方案设计

### 核心思路

采用**阶段化文案轮播 + 打字机效果 + 光标闪烁**的组合方案，在提交消息输入框中显示动态提示，模拟 AI 思考过程，让用户感知到系统正在工作。

### 方案特点

- ✅ **阶段化提示**：将生成过程拆分为用户可理解的阶段（分析 → 思考 → 生成）
- ✅ **打字机效果**：逐字显示提示文本，模拟真实输出过程
- ✅ **光标闪烁**：在提示文本后显示闪烁光标，增强"正在处理"的视觉反馈
- ✅ **平滑过渡**：当 AI 开始返回内容时，平滑切换到实际内容，避免闪烁

## 实现细节

### 技术架构

实现位于 `CommitMessageGenerator` 类中的 `TypingIndicator` 内部类，采用以下技术：

- **定时器**：使用 IntelliJ Platform 的 `Alarm` 类在 Swing 线程中调度动画更新
- **状态管理**：通过原子变量管理动画状态，确保线程安全
- **生命周期管理**：支持通过 `Disposable` 自动清理资源

### 阶段划分

根据 AI 生成流程，将提示分为三个阶段：

1. **分析阶段** (`analyzingText`)
    - 中文：`🔍 分析代码变更中...`
    - 英文：`🔍 Analyzing code changes...`
    - 时机：生成任务启动时立即显示

2. **思考阶段** (`thinkingText`)
    - 中文：`🧠 理解修改意图...`
    - 英文：`🧠 Understanding intent...`
    - 时机：当检测到 AI 返回思考内容时触发

3. **草稿阶段** (`draftingText`)
    - 中文：`📝 生成提交描述`
    - 英文：`📝 Generating commit description`
    - 时机：思考阶段完成后自动进入

### 动画效果

#### 打字机效果

- **逐字输出**：每个字符延迟 45ms 显示（`TYPING_TEXT_DELAY_MS`）
- **行间停顿**：阶段切换时停顿 1000ms（`TYPING_LINE_PAUSE_MS`）
- **换行处理**：多行提示之间自动添加换行符

#### 光标闪烁

- **闪烁间隔**：每 500ms 切换一次（`TYPING_CURSOR_DELAY_MS`）
- **光标样式**：`_💤`（下划线 + 睡眠表情）
- **显示逻辑**：偶数索引显示光标，奇数索引隐藏光标

### 关键代码实现

#### 启动打字指示器

```java
private TypingIndicator startTypingIndicator(
    @Nullable CommitMessageI commitMessageControl,
    @Nullable ChangelogToolWindowService.ChangelogOutputSession outputSession) {
    Disposable parentDisposable = commitMessageControl instanceof Disposable
        ? (Disposable) commitMessageControl : null;
    TypingIndicator indicator = new TypingIndicator(
        commitMessageControl, outputSession, parentDisposable);
    if (commitMessageControl != null || outputSession != null) {
        indicator.start();
    }
    return indicator;
}
```

#### 逐字输出实现

```java
private void typeLine(@NotNull String line, @NotNull Runnable onComplete) {
    int[] index = {0};
    alarm.addRequest(new Runnable() {
        @Override
        public void run() {
            if (stopped.get()) {
                return;
            }
            if (index[0] == 0 && !typedHint.isEmpty()) {
                typedHint.append('\n');
            }
            if (index[0] < line.length()) {
                typedHint.append(line.charAt(index[0]));
                index[0]++;
                updateHintText();
                alarm.addRequest(this, TYPING_TEXT_DELAY_MS);
            } else {
                updateHintText();
                onComplete.run();
            }
        }
    }, TYPING_TEXT_DELAY_MS);
}
```

#### 光标闪烁实现

```java
private void scheduleCursorBlink() {
    alarm.addRequest(() -> {
        if (stopped.get()) {
            return;
        }
        String base = typedHint.toString();
        String text = dotIndex % 2 == 0 ? base + cursorText : base;
        setCommitMessageText(text, commitMessageControl);
        if (outputSession != null) {
            outputSession.setText(text);
        }
        dotIndex++;
        scheduleCursorBlink();
    }, TYPING_CURSOR_DELAY_MS);
}
```

### 状态同步

#### 与 AI 流式响应集成

当 AI 开始返回实际内容时，需要立即停止提示动画并切换到实际内容：

```java
@Override
public void onChunk(@NotNull String chunk) {
    if (state.cancelled.get()) {
        return;
    }
    // 检测到内容开始返回时，停止提示动画
    if (!chunk.isBlank() && contentStarted.compareAndSet(false, true)) {
        typingIndicator.stop();
    }
    buffer.append(chunk);
    // 实时更新提交消息文本
    ApplicationManager.getApplication().invokeLater(() -> {
        if (setCommitMessageText(buffer.toString(), commitMessageControl)) {
            updated.set(true);
        }
    });
}
```

#### 思考阶段检测

当 AI 返回思考内容时，触发思考阶段提示：

```java
@Override
public void onThinkingChunk(@NotNull String chunk) {
    if (state.cancelled.get()) {
        return;
    }
    if (!chunk.isBlank()) {
        typingIndicator.startThinkingStage();
    }
}
```

## 技术要点

### 线程安全

- **Swing 线程**：所有 UI 更新必须在 Swing 线程（EDT）中执行
- **定时器调度**：使用 `Alarm.ThreadToUse.SWING_THREAD` 确保回调在正确线程
- **原子变量**：使用 `AtomicBoolean` 管理状态，避免竞态条件

### 资源管理

- **自动清理**：如果 `commitMessageControl` 实现了 `Disposable`，定时器会自动清理
- **手动清理**：否则需要在任务完成时手动调用 `Disposer.dispose(alarm)`
- **取消机制**：支持通过 `stop()` 方法立即停止动画

### 性能优化

- **延迟调度**：使用递归调度而非循环，避免占用线程
- **状态检查**：每次回调都检查停止状态，及时退出
- **最小更新**：只在内容变化时更新 UI，减少不必要的重绘

## 用户体验优化

### 设计原则

1. **符合 JetBrains UX 规范**
    - 使用语义化文本而非无语义 Spinner
    - 避免花哨动画，保持专业感
    - 不打断用户视线焦点

2. **信息层次**
    - 阶段化提示让用户了解处理进度
    - Emoji 图标增强视觉识别度
    - 简洁文案避免信息过载

3. **平滑过渡**
    - 从提示到实际内容的切换无闪烁
    - 支持实时流式输出，内容逐步显示

### 不推荐的方案

根据 JetBrains 官方 UX 建议，以下方案不推荐使用：

- ❌ **真进度条**：AI 推理时间不可预测，进度条容易卡住
- ❌ **Toast/Balloon 通知**：提交时用户视线在 Commit 面板，不应抢焦点
- ❌ **固定文本**：单一的"正在处理..."无法传达进度信息

## 配置参数

当前实现中的关键参数：

| 参数                       | 值      | 说明           |
|--------------------------|--------|--------------|
| `TYPING_TEXT_DELAY_MS`   | 45ms   | 打字机效果每个字符的延迟 |
| `TYPING_LINE_PAUSE_MS`   | 1000ms | 阶段切换时的停顿时间   |
| `TYPING_CURSOR_DELAY_MS` | 500ms  | 光标闪烁的切换间隔    |

这些参数可以根据实际体验效果进行调整，建议通过配置项暴露给用户（可选）。

## 国际化支持

所有提示文本都通过 `ChangelogBundle` 进行国际化：

- **中文资源**：`ChangelogBundle_zh_CN.properties`
- **英文资源**：`ChangelogBundle.properties`

支持的语言可以通过添加对应的资源文件进行扩展。

